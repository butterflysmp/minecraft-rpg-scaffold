package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.MobBody;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobGrowth;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobSplit;
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
                    stages.first().maxHealth(), MobBody.VANILLA, element, stages, split(s), growth(s), blast(s));
        }
        if (s.contains("blast")) {
            throw new IllegalArgumentException("'blast' needs 'stages': its damage is authored per stage");
        }
        if (s.contains("split")) {
            throw new IllegalArgumentException("'split' needs 'stages': a split names the stage its children are born at");
        }
        if (s.contains("growth")) {
            throw new IllegalArgumentException("'growth' needs 'stages': growing moves a stage");
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

    /**
     * The optional {@code split:} block (PLAN-dragon-mobs.md step R2), or null. Every key is required when the block
     * is present, and {@code at} must say {@code last}: Ben ruled that only the last stage splits, and the file says
     * so in words rather than leaving it implied.
     */
    private static MobSplit split(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("split");
        if (b == null) return null;
        for (String key : List.of("at", "count", "child_stage", "spread")) {
            if (!b.contains(key)) throw new IllegalArgumentException("split." + key + " is required");
        }
        if (!"last".equals(b.getString("at"))) {
            throw new IllegalArgumentException("split.at must be 'last' (only the last stage splits), was '"
                    + b.getString("at") + "'");
        }
        return new MobSplit(b.getInt("count"), b.getInt("child_stage"), b.getDouble("spread"));
    }

    /**
     * The optional {@code growth:} block (the plan's step R4), or null. Every key is required when the block is
     * present, and {@code element} must name a loaded element, as a mob's own {@code element} must.
     */
    private MobGrowth growth(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("growth");
        if (b == null) return null;
        for (String key : List.of("element", "radius", "stages_per_death")) {
            if (!b.contains(key)) throw new IllegalArgumentException("growth." + key + " is required");
        }
        String element = b.getString("element");
        if (!elementExists.test(element)) {
            throw new IllegalArgumentException("growth.element '" + element + "' names no element in content/elements");
        }
        return new MobGrowth(element, b.getDouble("radius"), b.getInt("stages_per_death"));
    }

    /**
     * The optional {@code blast:} block (dragon-mobs step R3), or null. Every key is required when the block is
     * present: {@code reach} (blocks), {@code damage.first} and {@code damage.last} (authored damage at the first and
     * last stage, at zero distance), {@code fuse_ticks} (ticks), {@code knockback} (a multiplier of vanilla's push).
     * {@link MobBlast} refuses a bad value by name.
     */
    private static MobBlast blast(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("blast");
        if (b == null) return null;
        for (String key : List.of("reach", "damage.first", "damage.last", "fuse_ticks", "knockback")) {
            if (!b.contains(key)) throw new IllegalArgumentException("blast." + key + " is required");
        }
        return new MobBlast(b.getDouble("reach"), b.getDouble("damage.first"), b.getDouble("damage.last"),
                b.getInt("fuse_ticks"), b.getDouble("knockback"));
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
