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
            case "wither" -> new StatusDefinition.Wither(id, immuneTypes(id, s));
            default -> throw new IllegalArgumentException("Unknown status kind: " + kind);
        };
    }

    /**
     * {@code immune:}, a list of entity type keys, lowercased; absent means nobody is immune. A key with
     * a namespace or a space is refused as a named, skipped file -- it could never match the bare type
     * key the check compares against, so it would read as a ruling and protect nothing.
     *
     * <p><b>A WELL-FORMED KEY THAT NAMES NO ENTITY TYPE IS REFUSED, AND THE REST OF THE LIST LOADS</b> (the
     * seat, 2026-10-01). {@code wither_skelton} would make every wither skeleton silently NON-immune while
     * the file reads as a ruling. It is checked against {@link #KNOWN_ENTITY_TYPES}, the pinned API's
     * {@code EntityType} list, and refused with a {@code Refusing} WARN naming the file and the key, so
     * R0c's {@code 'Refusing'} pattern catches it at boot. The rest of the list still loads: one typo
     * must not strip the immunity the correct keys rule.
     */
    private java.util.Set<String> immuneTypes(String id, ConfigurationSection s) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (String raw : s.getStringList("immune")) {
            String key = raw.toLowerCase(Locale.ROOT).trim();
            if (!key.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("Invalid immune entity type '" + raw
                        + "'; expected a bare type key like wither_skeleton");
            }
            if (!KNOWN_ENTITY_TYPES.contains(key)) {
                log.warning("Refusing immune entity type '" + raw + "' in status '" + id + ".yml': no entity"
                        + " type has that key, so it would protect nothing. The rest of the list loads.");
                continue;
            }
            out.add(key);
        }
        return out;
    }

    /**
     * Every entity type key the pinned API knows ({@code zombie}, {@code wither_skeleton}), read from
     * {@link org.bukkit.entity.EntityType} -- the enum the jar ships, which mirrors the entity-type
     * registry of the same build. Read without a server: the enum's static initialiser and
     * {@code getKey()} touch no {@code Bukkit} call (checked with {@code javap -c} on build 74).
     * {@code UNKNOWN} has no key and is skipped, since its {@code getKey()} throws.
     */
    static final java.util.Set<String> KNOWN_ENTITY_TYPES = java.util.Arrays.stream(
                    org.bukkit.entity.EntityType.values())
            .filter(t -> t != org.bukkit.entity.EntityType.UNKNOWN)
            .map(t -> t.getKey().getKey())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

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
