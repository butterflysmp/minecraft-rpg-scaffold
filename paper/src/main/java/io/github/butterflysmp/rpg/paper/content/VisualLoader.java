package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.Color;
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
            case "particle" -> {
                Particle p = particle(str(m, type, "particle"));
                yield new VisualSpec.Particles(
                        p,
                        (int) numOr(m, type, "count", 10),
                        numOr(m, type, "spread", 0.0),
                        // 1.0, NOT 0.0. This is Bukkit's `extra`, and the 6-arg spawnParticle this
                        // adapter used to call already passed 1.0 (its default chain ends at
                        // dconst_1). Every visual on disk was authored against that value without
                        // anyone choosing it, so 1.0 is what "absent" has always meant here.
                        // See VisualSpec.Particles.
                        numOr(m, type, "speed", 1.0),
                        dust(m, type, p),
                        // 4.0, NOT 0.0 -- zero would be a beam that draws nothing, silently.
                        // Inert unless this visual is presented ALONG a segment.
                        numOr(m, type, "samples_per_block", VisualSpec.DEFAULT_SAMPLES_PER_BLOCK));
            }
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
     * valueOf("BLOCK") succeeds and spawnParticle(BLOCK, ...) then throws on the first
     * cast, because BLOCK wants a BlockData. getDataType() == Void.class is what
     * "takes no data object" means, and it is still the rule for every particle the
     * schema cannot supply data for -- which is all of them but one.
     *
     * <p><b>DUST IS THE ONE EXCEPTION, AND THE GATE IS OPENED BY DATA TYPE RATHER THAN BY NAME.</b>
     * Enumerated from the pinned API rather than recalled: on paper-api 26.1.2.build.74-stable,
     * eighteen particles declare a non-Void data type, spread across ten classes -- Spell, Color,
     * DustOptions, ItemStack, BlockData, Float, DustTransition, Vibration, Integer and Trail.
     * EXACTLY ONE of those classes is DustOptions, and exactly one particle carries it. So
     * comparing against {@code DustOptions.class} admits DUST and nothing else, today or after a
     * Paper bump, whereas relaxing the check to "has some data object" would silently admit
     * seventeen particles whose data the schema still cannot express. Everything else keeps
     * failing at load, by name, exactly as it did.
     */
    private static Particle particle(String raw) {
        Particle p;
        try {
            p = Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown particle: " + raw);
        }
        if (p.getDataType() != Void.class && p.getDataType() != Particle.DustOptions.class) {
            throw new IllegalArgumentException("Particle '" + raw + "' requires a data object ("
                    + p.getDataType().getSimpleName() + "); the visual schema cannot supply one yet");
        }
        return p;
    }

    /**
     * The DustOptions a DUST step needs, or null for every particle that takes no data.
     *
     * <p><b>AUTHORING colour or size on a particle that cannot use them is an ERROR, not a
     * no-op.</b> A field a file may set and the code silently drops is indistinguishable from a
     * field that works, and the author would have no way to find out. So the check runs in BOTH
     * directions: DUST without a colour fails, and a colour on FLAME fails.
     *
     * <p>{@code color} is authored as a three-item list so it reads as the call it becomes --
     * {@code [40, 90, 240]} is {@code Color.fromRGB(40, 90, 240)}. The range check is here rather
     * than left to fromRGB because fromRGB throws IllegalArgumentException with a message about
     * "Red is not between 0 and 255", which names no file; a content mistake has to arrive as a
     * NAMED, SKIPPED file like every other one.
     */
    private static Particle.DustOptions dust(Map<?, ?> m, String type, Particle p) {
        boolean takesDust = p.getDataType() == Particle.DustOptions.class;
        boolean authored = m.get("color") != null || m.get("size") != null;

        if (!takesDust) {
            if (authored) {
                throw new IllegalArgumentException("Visual step '" + type + "' sets 'color'/'size' "
                        + "on particle '" + p.name() + "', which takes no data object and would "
                        + "ignore them; remove them or use DUST");
            }
            return null;
        }

        Object raw = m.get("color");
        if (raw == null) {
            throw new IllegalArgumentException("Visual step '" + type + "' uses particle '"
                    + p.name() + "', which requires a 'color' -- author it as [r, g, b]");
        }
        if (!(raw instanceof List<?> channels) || channels.size() != 3) {
            throw new IllegalArgumentException("Visual step '" + type + "' field 'color' must be a "
                    + "list of exactly three numbers [r, g, b], got: " + raw);
        }

        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++) {
            if (!(channels.get(i) instanceof Number n)) {
                throw new IllegalArgumentException("Visual step '" + type + "' field 'color' entry "
                        + i + " must be a number, got: " + channels.get(i));
            }
            rgb[i] = n.intValue();
            if (rgb[i] < 0 || rgb[i] > 255) {
                throw new IllegalArgumentException("Visual step '" + type + "' field 'color' entry "
                        + i + " must be between 0 and 255, got: " + rgb[i]);
            }
        }

        double size = numOr(m, type, "size", 1.0);
        if (size <= 0) {
            throw new IllegalArgumentException("Visual step '" + type + "' field 'size' must be "
                    + "greater than 0, got: " + size);
        }
        return new Particle.DustOptions(Color.fromRGB(rgb[0], rgb[1], rgb[2]), (float) size);
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
