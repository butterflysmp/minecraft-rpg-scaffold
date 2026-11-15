package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobBody;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobStages;
import io.github.butterflysmp.rpg.core.mob.StageBody;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Turns YAML into {@link MobDefinition}. The only class that knows the mob schema.
 *
 * A mob's id is its filename minus .yml, as with every other content type. Fails soft: a malformed
 * file is logged, NAMED, and skipped, and every other mob still loads -- so one bad file cannot take
 * the server's mob content down with it.
 *
 * {@code base_entity} has no default on purpose. Rarity defaults to common and material defaults to a
 * sword because a wrong guess there is cosmetic; a wrong guess at which creature to spawn is not. A
 * missing value throws, which turns it into a named, skipped file at boot -- the same treatment a
 * weapon's missing {@code class} gets, and for the same reason.
 */
public final class MobLoader {

    private final Logger log;
    private final Predicate<String> elementExists;

    /**
     * @param elementExists whether an id names a loaded element ({@code ElementRegistry}); asked of a mob's
     *                      {@code element}, so an unknown one is a named, skipped file at load rather than a mob
     *                      that silently has no element. Elements load before mobs (RpgPlugin's content order).
     */
    public MobLoader(Logger log, Predicate<String> elementExists) {
        this.log = log;
        this.elementExists = elementExists;
    }

    public MobRegistry loadAll(File mobsDir) {
        MobRegistry registry = new MobRegistry();
        File[] files = mobsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed mob '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " mob file(s) were skipped. The server is still running, "
                    + "but that mob cannot be spawned.");
        }
        return registry;
    }

    /** The id is the filename: knell.yml -> knell. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    /**
     * MobDefinition rejects a blank base_entity, a blank display name and a non-positive max_health --
     * caught by loadAll, named, skipped. Whether the base_entity NAMES A REAL living entity is not
     * knowable here (it needs the Bukkit registry, which needs a running server), so ContentValidator
     * checks that at boot instead, the same way a dangling visual_id is checked.
     */
    private MobDefinition parse(String id, ConfigurationSection s) {
        String element = s.getString("element");
        if (element != null && !elementExists.test(element)) {
            throw new IllegalArgumentException("element '" + element + "' names no element in content/elements");
        }
        ConfigurationSection staged = s.getConfigurationSection("stages");
        if (staged != null) {
            // Two sources for one number is refused: a staged mob's health and body live in its stages only.
            for (String key : List.of("max_health", "scale", "speed")) {
                if (s.contains(key)) {
                    throw new IllegalArgumentException("'" + key + "' is authored beside 'stages'; a staged mob's"
                            + " body and health live in its stages only");
                }
            }
            MobStages stages = new MobStages(staged.getInt("count", 0),
                    stageBody(staged, "first"), stageBody(staged, "last"));
            return new MobDefinition(id, s.getString("base_entity"), s.getString("display_name", id),
                    stages.first().maxHealth(), MobBody.VANILLA, element, stages);
        }
        return new MobDefinition(
                id,
                s.getString("base_entity"),
                s.getString("display_name", id),
                s.getDouble("max_health", 0.0),
                new MobBody(optional(s, "scale"), optional(s, "speed")),
                element,
                null);
    }

    /** One stage's body: height in blocks, a speed multiplier, and the authored health. All three required. */
    private static StageBody stageBody(ConfigurationSection staged, String which) {
        ConfigurationSection b = staged.getConfigurationSection(which);
        if (b == null) throw new IllegalArgumentException("stages." + which + " is required");
        for (String key : List.of("height", "speed", "max_health")) {
            if (!b.contains(key)) throw new IllegalArgumentException("stages." + which + "." + key + " is required");
        }
        return new StageBody(b.getDouble("height"), b.getDouble("speed"), b.getDouble("max_health"));
    }

    /** An optional number: absent means "leave the vanilla value alone", never 0 or 1. */
    private static OptionalDouble optional(ConfigurationSection s, String key) {
        return s.contains(key) ? OptionalDouble.of(s.getDouble(key)) : OptionalDouble.empty();
    }
}
