package io.github.yourname.rpg.paper.content;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Turns YAML into VisualDefinition. The only class that knows the visual schema.
 *
 * A visual's id is its filename minus .yml. Visuals are referenced by ability
 * content, so renaming a file breaks those references -- ContentValidator is
 * what catches that at startup, and this convention is only safe because it exists.
 *
 * Fails soft, like AbilityLoader: a malformed file is logged, named, and skipped,
 * and every other visual still loads.
 *
 * Resolves Particle here, at load time, because Particle is a plain enum and needs
 * no server. Sound is registry-backed and cannot be, so its key's *syntax* is
 * checked here and its *existence* at startup.
 */
public final class VisualLoader {

    private final Logger log;

    public VisualLoader(Logger log) {
        this.log = log;
    }

    public VisualRegistry loadAll(File visualsDir) {
        VisualRegistry registry = new VisualRegistry();
        File[] files = visualsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed visual '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " visual file(s) were skipped. The server is still running, "
                    + "but that content is not loaded.");
        }
        return registry;
    }

    /** The id is the filename: solar_detonation.yml -> solar_detonation. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private VisualDefinition parse(String id, ConfigurationSection s) {
        List<Map<?, ?>> raw = s.getMapList("steps");
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Visual is missing a non-empty 'steps' list");
        }
        List<VisualSpec> steps = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            steps.add(parseStep(m));
        }
        return new VisualDefinition(id, steps);
    }

    private VisualSpec parseStep(Map<?, ?> m) {
        Object rawType = m.get("type");
        if (rawType == null) throw new IllegalArgumentException("Visual step is missing its 'type' field");
        String type = String.valueOf(rawType).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "particle" -> new VisualSpec.Particles(
                    particle(str(m, type, "particle")),
                    (int) numOr(m, type, "count", 10),
                    numOr(m, type, "spread", 0.0));
            case "sound" -> {
                String key = str(m, type, "key");
                yield new VisualSpec.Sound(key, soundKey(key),
                        (float) numOr(m, type, "volume", 1.0),
                        (float) numOr(m, type, "pitch", 1.0));
            }
            default -> throw new IllegalArgumentException("Unknown visual step type: " + type);
        };
    }

    /**
     * Particle.valueOf catches a typo, but not a particle that needs a data object:
     * valueOf("DUST") succeeds and spawnParticle(DUST, ...) then throws on the first
     * cast, because DUST wants a DustOptions. getDataType() == Void.class is what
     * "takes no data object" means. The schema has nowhere to put that data yet, so
     * say so at load rather than at the first cast.
     */
    private static Particle particle(String raw) {
        Particle p;
        try {
            p = Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown particle: " + raw);
        }
        if (p.getDataType() != Void.class) {
            throw new IllegalArgumentException("Particle '" + raw + "' requires a data object ("
                    + p.getDataType().getSimpleName() + "); the visual schema cannot supply one yet");
        }
        return p;
    }

    /**
     * fromString returns null on an invalid key; NamespacedKey.minecraft would throw.
     * Either way this is a content mistake, and a returned null lets it be reported
     * with the file's name like every other one.
     */
    private static NamespacedKey soundKey(String key) {
        NamespacedKey parsed = NamespacedKey.fromString(key);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid sound key '" + key
                    + "'; expected a lowercase vanilla key like entity.blaze.shoot");
        }
        return parsed;
    }

    private static double num(Map<?, ?> m, String type, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Visual step '" + type + "' is missing field: " + k);
        if (!(v instanceof Number n)) {
            throw new IllegalArgumentException(
                    "Visual step '" + type + "' field '" + k + "' must be a number, got: " + v);
        }
        return n.doubleValue();
    }

    private static double numOr(Map<?, ?> m, String type, String k, double fallback) {
        return m.get(k) == null ? fallback : num(m, type, k);
    }

    private static String str(Map<?, ?> m, String type, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Visual step '" + type + "' is missing field: " + k);
        return String.valueOf(v);
    }
}
