package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Turns YAML into StatusDefinition. The only class that knows the status schema.
 *
 * A status's id is its filename minus .yml, as with visuals. Fails soft, like
 * AbilityLoader: a malformed file is logged, named, and skipped.
 *
 * Never resolves a PotionEffectType. That needs Registry, which needs a server,
 * which would make this untestable. Only the key's syntax is checked here.
 */
public final class StatusLoader {

    private final Logger log;

    public StatusLoader(Logger log) {
        this.log = log;
    }

    public StatusRegistry loadAll(File statusesDir) {
        StatusRegistry registry = new StatusRegistry();
        File[] files = statusesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        java.util.List<String> retired = new java.util.ArrayList<>();
        for (File f : files) {
            if (RETIRED_FILES.contains(f.getName())) {
                retired.add(f.getName());
                continue;
            }
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed status '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " status file(s) were skipped. The server is still running, "
                    + "but that content is not loaded.");
        }
        if (!retired.isEmpty()) {
            log.warning(retired.size() + " status file(s) in " + statusesDir.getPath() + " " + retired
                    + " name DELETED content and are NOT loaded. They are stale copies (saveResource never"
                    + " deletes); they can be deleted, and --refresh-content clears them on a dev server.");
        }
        return registry;
    }

    /**
     * Files DELETED by a ruling, skipped if a stale copy survives in a data folder -- AbilityLoader's
     * RETIRED_FILES handling: named once at boot, never deleted, and not loaded.
     *
     * <p>{@code surge.yml}: ruling 22 (2026-09-26), PLAN-build-system.md -- its only user was
     * arc_surge, deleted by ruling 20.
     */
    static final java.util.Set<String> RETIRED_FILES = java.util.Set.of("surge.yml");

    /** The id is the filename: scorch.yml -> scorch. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private StatusDefinition parse(String id, ConfigurationSection s) {
        String kind = req(s, "kind").toLowerCase(Locale.ROOT);
        return switch (kind) {
            case "fire" -> new StatusDefinition.Fire(id);
            case "potion" -> new StatusDefinition.Potion(id, potionType(req(s, "potion_type")));
            case "rooted" -> new StatusDefinition.Immobilize(id, false);
            case "freeze" -> new StatusDefinition.Immobilize(id, true);
            case "soaked" -> new StatusDefinition.Soaked(id);
            case "scorch" -> new StatusDefinition.Scorch(id);
            default -> throw new IllegalArgumentException("Unknown status kind: " + kind);
        };
    }

    private static String req(ConfigurationSection s, String path) {
        String v = s.getString(path);
        if (v == null) throw new IllegalArgumentException("Missing required field: " + path);
        return v;
    }

    /**
     * fromString, not NamespacedKey.minecraft: the latter throws on 'Slowness', and
     * that exception would surface later from inside a scheduler task as a scheduler
     * error rather than as a named, skipped file. A returned null keeps it fail-soft.
     */
    private static NamespacedKey potionType(String raw) {
        NamespacedKey parsed = NamespacedKey.fromString(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid potion_type '" + raw
                    + "'; expected a lowercase key like slowness");
        }
        return parsed;
    }
}
