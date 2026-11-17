package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobBeam;
import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.MobBody;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobGrowth;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobSplit;
import io.github.butterflysmp.rpg.core.mob.MobStages;
import io.github.butterflysmp.rpg.core.mob.MobTeleport;
import io.github.butterflysmp.rpg.core.mob.MobShot;
import io.github.butterflysmp.rpg.core.mob.MobBlink;
import io.github.butterflysmp.rpg.core.mob.MobShield;
import io.github.butterflysmp.rpg.core.mob.MobPounce;
import io.github.butterflysmp.rpg.core.mob.StageBody;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private final Predicate<String> soundExists;

    /**
     * @param elementExists whether an id names a loaded element ({@code ElementRegistry}); asked of a mob's
     *                      {@code element}, so an unknown one is a named, skipped file at load rather than a mob
     *                      that silently has no element. Elements load before mobs (RpgPlugin's content order).
     * @param soundExists   whether a key names a sound event the server knows; asked of every beam sound
     *                      (the follow-up of 2026-11-17), so a typo is a named, skipped file rather than a silent beam.
     *                      Production asks {@code Registry.SOUND_EVENT}; a test passes a set.
     */
    public MobLoader(Logger log, Predicate<String> elementExists, Predicate<String> soundExists) {
        this(log, elementExists, soundExists, key -> true);
    }

    /**
     * @param materialExists whether a key names a vanilla material the server knows; asked of {@code main_hand} (an
     *                       ITEM) and of {@code shot.look.block} (a BLOCK), step R7a, so a typo is a named, skipped
     *                       file. Production asks {@code Material.matchMaterial}; the three-argument form accepts any.
     */
    public MobLoader(Logger log, Predicate<String> elementExists, Predicate<String> soundExists,
                     Predicate<String> materialExists) {
        this.log = log;
        this.elementExists = elementExists;
        this.soundExists = soundExists;
        this.materialExists = materialExists;
    }

    private final Predicate<String> materialExists;

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
                    stages.first().maxHealth(), MobBody.VANILLA, element, stages, split(s), growth(s), blast(s),
                    beam(s), teleport(s), shot(s), mainHand(s), blink(s), shield(s), mount(s), pounce(s),
                    riddenSpeed(s));
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
                null, null, null, null,
                beam(s), teleport(s), shot(s), mainHand(s), blink(s), shield(s), mount(s), pounce(s),
                    riddenSpeed(s));
    }

    /**
     * The optional {@code blink:} block (dragon-mobs step R7b), or null. Every key required when present:
     * {@code trigger_range} (BLOCKS, the half-extent of the box round its feet), {@code near} and {@code far} (BLOCKS),
     * {@code attempts}, {@code level_tolerance} (whole BLOCKS), {@code particle} (a Bukkit {@code Particle} name),
     * {@code particle_count}, {@code sounds} (a list of {@code { key, volume, pitch }}). {@link MobBlink} refuses a bad
     * number by name.
     */
    private MobBlink blink(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("blink");
        if (b == null) return null;
        for (String key : List.of("trigger_range", "near", "far", "attempts", "level_tolerance", "particle",
                "particle_count", "cooldown_ticks", "sounds")) {
            if (!b.contains(key)) throw new IllegalArgumentException("blink." + key + " is required");
        }
        // cooldown_ticks (step R8): TICKS after a blink that lands; required, never defaulted, and MobBlink refuses a
        // negative one by name.
        return new MobBlink(b.getDouble("trigger_range"), b.getDouble("near"), b.getDouble("far"), b.getInt("attempts"),
                b.getInt("level_tolerance"), soundList(b, "blink"), particle(b, "blink"), b.getInt("particle_count"),
                b.getInt("cooldown_ticks"));
    }

    /** The optional {@code mount:} (step R8): the id of the mob file this one is spawned riding, or null. */
    private static String mount(ConfigurationSection s) {
        return s.getString("mount");
    }

    /** The optional {@code ridden_speed:} (step R8): a multiplier MobDefinition refuses unless above 0, or null. */
    private static Double riddenSpeed(ConfigurationSection s) {
        return s.contains("ridden_speed") ? s.getDouble("ridden_speed") : null;
    }

    /**
     * The optional {@code pounce:} block (step R8, the Skreel's), or null. Every key required when present, nested keys
     * named with their path ({@code pounce.band.far}): {@code acquire_range}, {@code band.near}, {@code band.far},
     * {@code path_speed}, {@code windup_ticks}, {@code period_ticks}, {@code launch_vertical}, {@code timeout_ticks},
     * {@code reach}, {@code damage}, {@code knockback.horizontal}, {@code knockback.vertical}, {@code windup.particle},
     * {@code windup.particle_count}, {@code windup.every_ticks}, {@code windup.sounds}, {@code launch_sounds},
     * {@code land_sounds}. {@link MobPounce} refuses a bad number by name; every sound key is checked.
     */
    private MobPounce pounce(ConfigurationSection s) {
        ConfigurationSection p = s.getConfigurationSection("pounce");
        if (p == null) return null;
        for (String key : List.of("acquire_range", "band.near", "band.far", "path_speed", "windup_ticks", "period_ticks",
                "launch_vertical", "timeout_ticks", "reach", "damage", "knockback.horizontal", "knockback.vertical",
                "windup.particle", "windup.particle_count", "windup.every_ticks", "windup.sounds", "launch_sounds",
                "land_sounds")) {
            if (!p.contains(key)) throw new IllegalArgumentException("pounce." + key + " is required");
        }
        ConfigurationSection w = p.getConfigurationSection("windup");
        return new MobPounce(p.getDouble("acquire_range"), p.getDouble("band.near"), p.getDouble("band.far"),
                p.getDouble("path_speed"), p.getInt("windup_ticks"), p.getInt("period_ticks"),
                p.getDouble("launch_vertical"), p.getInt("timeout_ticks"), p.getDouble("reach"), p.getDouble("damage"),
                p.getDouble("knockback.horizontal"), p.getDouble("knockback.vertical"), particle(w, "pounce.windup"),
                w.getInt("particle_count"), w.getInt("every_ticks"), soundList(w, "pounce.windup"),
                soundsAt(p, "launch_sounds", "pounce"), soundsAt(p, "land_sounds", "pounce"));
    }

    /** A list of {@code { key, volume, pitch }} at {@code key} under {@code b}, each key checked as the beam's are. */
    private List<BeamSound> soundsAt(ConfigurationSection b, String key, String block) {
        List<BeamSound> out = new java.util.ArrayList<>();
        for (Map<?, ?> m : b.getMapList(key)) {
            for (String f : List.of("key", "volume", "pitch")) {
                if (!m.containsKey(f)) throw new IllegalArgumentException(block + "." + key + " entry " + m + " has no " + f);
            }
            String sound = String.valueOf(m.get("key"));
            if (!soundExists.test(sound)) {
                throw new IllegalArgumentException(block + "." + key + " key '" + sound
                        + "' names no sound event the server knows");
            }
            out.add(new BeamSound(sound, ((Number) m.get("volume")).doubleValue(), ((Number) m.get("pitch")).doubleValue()));
        }
        return out;
    }

    /**
     * The optional {@code shield:} block (dragon-mobs step R7b), or null. Every key required when present:
     * {@code item} (the off-hand ITEM key), {@code particle}, {@code particle_count}, {@code sounds}.
     */
    private MobShield shield(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("shield");
        if (b == null) return null;
        for (String key : List.of("item", "particle", "particle_count", "sounds")) {
            if (!b.contains(key)) throw new IllegalArgumentException("shield." + key + " is required");
        }
        String item = b.getString("item");
        if (!materialExists.test(item)) {
            throw new IllegalArgumentException("shield.item '" + item + "' names no item the server knows");
        }
        return new MobShield(item, soundList(b, "shield"), particle(b, "shield"), b.getInt("particle_count"));
    }

    /** A {@code sounds:} list directly under a block (blink, shield), each key checked as the beam's are. */
    private List<BeamSound> soundList(ConfigurationSection b, String block) {
        List<BeamSound> out = new java.util.ArrayList<>();
        for (Map<?, ?> m : b.getMapList("sounds")) {
            for (String f : List.of("key", "volume", "pitch")) {
                if (!m.containsKey(f)) throw new IllegalArgumentException(block + ".sounds entry " + m + " has no " + f);
            }
            String key = String.valueOf(m.get("key"));
            if (!soundExists.test(key)) {
                throw new IllegalArgumentException(block + ".sounds key '" + key + "' names no sound event the server knows");
            }
            out.add(new BeamSound(key, ((Number) m.get("volume")).doubleValue(), ((Number) m.get("pitch")).doubleValue()));
        }
        return out;
    }

    /** A Bukkit {@code Particle} name, lower case in the file; an unknown one is refused by name. */
    private static String particle(ConfigurationSection b, String block) {
        String name = b.getString("particle");
        org.bukkit.Particle p;
        try {
            p = org.bukkit.Particle.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(block + ".particle '" + name + "' names no particle");
        }
        // The spawn passes no data, and a particle that needs some (block, dust, item) throws there, on the mob's
        // thread, mid-blink or mid-absorb. Refused here instead, by name.
        if (p.getDataType() != Void.class) {
            throw new IllegalArgumentException(block + ".particle '" + name + "' needs data (" + p.getDataType().getSimpleName()
                    + "); author one that needs none");
        }
        return name;
    }

    /** The optional {@code main_hand:} ITEM key (step R7a: the Phalanx's bow), or null; an unknown one is refused. */
    private String mainHand(ConfigurationSection s) {
        String key = s.getString("main_hand");
        if (key != null && !materialExists.test(key)) {
            throw new IllegalArgumentException("main_hand '" + key + "' names no item the server knows");
        }
        return key;
    }

    /**
     * The optional {@code shot:} block (dragon-mobs step R7a), or null. Every key is required when the block is
     * present; each is named in {@link MobShot}'s javadoc with its quantity. {@code sounds.fire} and
     * {@code sounds.destroyed} are lists of {@code { key, volume, pitch }}, each key checked against the server's
     * sound events; {@code look.block} is checked against its materials. {@link MobShot} refuses a bad value by name.
     */
    private MobShot shot(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("shot");
        if (b == null) return null;
        for (String key : List.of("period_ticks", "acquire_range", "reacquire_range", "speed", "homing", "life_ticks",
                "max_distance", "tolerance", "damage", "hit_box", "look.block", "look.size", "look.glow", "look.trail.rgb",
                "look.trail.size", "look.flash.rgb", "look.flash.size", "look.flash.every_ticks", "sounds.fire",
                "sounds.destroyed")) {
            if (!b.contains(key)) throw new IllegalArgumentException("shot." + key + " is required");
        }
        String block = b.getString("look.block");
        if (!materialExists.test(block)) {
            throw new IllegalArgumentException("shot.look.block '" + block + "' names no block the server knows");
        }
        MobShot.ShotLook look = new MobShot.ShotLook(block, b.getDouble("look.size"), b.getDouble("hit_box"), rgb(b, "look.glow"),
                rgb(b, "look.trail.rgb"), b.getDouble("look.trail.size"), rgb(b, "look.flash.rgb"),
                b.getDouble("look.flash.size"), b.getInt("look.flash.every_ticks"));
        return new MobShot(b.getInt("period_ticks"), b.getDouble("acquire_range"), b.getDouble("reacquire_range"),
                b.getDouble("speed"), b.getDouble("homing"), b.getInt("life_ticks"), b.getDouble("max_distance"),
                b.getDouble("tolerance"), b.getDouble("damage"), look,
                sounds(b, "shot", "fire"), sounds(b, "shot", "destroyed"));
    }

    /** A colour authored as three whole numbers {@code [r, g, b]}, each 0 to 255, packed as {@code 0xRRGGBB}. */
    private static int rgb(ConfigurationSection b, String key) {
        List<Integer> c = b.getIntegerList(key);
        if (c.size() != 3) throw new IllegalArgumentException("shot." + key + " must be [r, g, b], was " + b.get(key));
        for (int v : c) {
            if (v < 0 || v > 255) throw new IllegalArgumentException("shot." + key + " " + c + " has a part outside 0..255");
        }
        return (c.get(0) << 16) | (c.get(1) << 8) | c.get(2);
    }

    /**
     * The optional {@code beam:} block (dragon-mobs step R5), or null. Every key is required when the block is
     * present: {@code cycle_ticks.min} and {@code .max} (TICKS between attempts), {@code telegraph_ticks} (TICKS from
     * the lock to the fire), {@code flash_at} (the telegraph TICKS the warning is drawn on), {@code range} (BLOCKS from
     * the eye), {@code aim_height} (a FRACTION of the target's height), {@code tolerance} (BLOCKS the target's box is
     * grown by), {@code damage} (authored, before gear score). {@link MobBeam} refuses a bad value by name.
     */
    /**
     * The optional {@code teleport:} block (dragon-mobs step R6), or null. Every key is required when the block is
     * present: {@code delay_ticks} (TICKS from the hit to the move), {@code arc_degrees.min} and {@code .max} (DEGREES
     * around the target), {@code spots_per_side}, {@code level_tolerance} (whole BLOCKS above or below the target's
     * feet), {@code require_sight}. {@link MobTeleport} refuses a bad value by name; {@code MobDefinition} refuses a
     * teleport with no beam, and a delay not shorter than the beam's {@code cycle_ticks.min}.
     */
    private static MobTeleport teleport(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("teleport");
        if (b == null) return null;
        for (String key : List.of("delay_ticks", "arc_degrees.min", "arc_degrees.max", "spots_per_side",
                "level_tolerance", "require_sight")) {
            if (!b.contains(key)) throw new IllegalArgumentException("teleport." + key + " is required");
        }
        return new MobTeleport(b.getInt("delay_ticks"), b.getDouble("arc_degrees.min"), b.getDouble("arc_degrees.max"),
                b.getInt("spots_per_side"), b.getInt("level_tolerance"), b.getBoolean("require_sight"));
    }

    private MobBeam beam(ConfigurationSection s) {
        ConfigurationSection b = s.getConfigurationSection("beam");
        if (b == null) return null;
        for (String key : List.of("cycle_ticks.min", "cycle_ticks.max", "telegraph_ticks", "flash_at", "range",
                "aim_height", "tolerance", "damage", "sounds.telegraph", "sounds.fire")) {
            if (!b.contains(key)) throw new IllegalArgumentException("beam." + key + " is required");
        }
        return new MobBeam(b.getInt("cycle_ticks.min"), b.getInt("cycle_ticks.max"), b.getInt("telegraph_ticks"),
                b.getIntegerList("flash_at"), b.getDouble("range"), b.getDouble("aim_height"),
                b.getDouble("tolerance"), b.getDouble("damage"), sounds(b, "beam", "telegraph"), sounds(b, "beam", "fire"));
    }

    /**
     * One of the beam's two sound lists (the follow-up of 2026-11-17): {@code sounds.telegraph} (played at the lock)
     * or {@code sounds.fire} (played at the fire), each a list of {@code { key, volume, pitch }}, every field
     * required. A key the server does not know is refused by name, so a typo is a skipped file, never a silent beam.
     */
    private List<BeamSound> sounds(ConfigurationSection beam, String block, String when) {
        List<BeamSound> out = new java.util.ArrayList<>();
        for (Map<?, ?> m : beam.getMapList("sounds." + when)) {
            for (String f : List.of("key", "volume", "pitch")) {
                if (!m.containsKey(f)) {
                    throw new IllegalArgumentException(block + ".sounds." + when + " entry " + m + " has no " + f);
                }
            }
            String key = String.valueOf(m.get("key"));
            if (!soundExists.test(key)) {
                throw new IllegalArgumentException(block + ".sounds." + when + " key '" + key
                        + "' names no sound event the server knows");
            }
            out.add(new BeamSound(key, ((Number) m.get("volume")).doubleValue(),
                    ((Number) m.get("pitch")).doubleValue()));
        }
        return out;
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
