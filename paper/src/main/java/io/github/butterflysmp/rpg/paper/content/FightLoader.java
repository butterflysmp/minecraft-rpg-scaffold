package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.fight.FightSettings;
import io.github.butterflysmp.rpg.core.fight.RingLook;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Loads {@code content/fights/} (PLAN-dragon-fight.md steps F4 to F7). It is the only class that knows the fight schema.
 *
 * <p><b>Every key is REQUIRED, never defaulted</b>, the shape of {@code MiningContentLoader}: a missing key would
 * otherwise read as 0, and a 0 radius or a 0 damage is a fight that silently does nothing. A file with any missing or
 * malformed key is REFUSED whole, with a {@code Refusing} warning naming the key, so R0c's pattern catches it at boot;
 * the range checks are the core records' ({@link FightSettings}, {@link RingLook}), and their message is the reason.
 * The Mage's mob must be a loaded mob file and every sound a known sound, asked of the two predicates the plugin gives.
 */
public final class FightLoader {

    static final String SUPPORTED_PARTICLE = "dust";

    private final Logger log;
    private final Predicate<String> mobKnown;
    private final Predicate<String> soundKnown;

    public FightLoader(Logger log, Predicate<String> mobKnown, Predicate<String> soundKnown) {
        this.log = log;
        this.mobKnown = mobKnown;
        this.soundKnown = soundKnown;
    }

    /** Every {@code *.yml} in the directory, by file id. A refused file is absent and named. */
    public Map<String, FightContent> loadAll(File dir) {
        Map<String, FightContent> out = new LinkedHashMap<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return out;
        Arrays.sort(files);
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - ".yml".length());
            try {
                FightContent fight = parse(id, YamlConfiguration.loadConfiguration(f));
                out.put(id, fight);
                log.info(loadedLine(fight));
            } catch (RuntimeException ex) {
                log.warning("Refusing fight '" + id + "' in " + f.getName() + ": " + ex.getMessage());
            }
        }
        return out;
    }

    /** The startup line, one per fight: every number, so a boot reads what was loaded rather than what was meant. */
    static String loadedLine(FightContent f) {
        FightSettings s = f.settings();
        RingLook l = f.look();
        return String.format(Locale.ROOT,
                "Loaded fight '%s': border %.1f, mage ring %.1f, v_fast %.2f, v_slow %.2f, e_kill %.2f, "
                        + "%d crystals every %d ticks, a strike every %d ticks from %.1f; ring %s (%d,%d,%d) size %.2f "
                        + "every %.2f blocks at %s, row shift %.2f, turn %.2f/s %s, every %d ticks; mage '%s' refused %s, "
                        + "landing lightning %s; "
                        + "crystals at %.1f, %.1f up, lit %s; participants within %.1f; arena at (%d, %d) floor y %d, "
                        + "disc %d x %d, %d pillars %d wide on %.1f to +%d, time %d, entry (%.1f, %.1f), environment %s, all down %d s; "
                        + "dragon '%s' "
                        + "Mages below %s, win return %d ticks, bar %s, bite %.1f, wing %.1f, breath %.1f/s, one contact hit per %d ticks; "
                        + "revive within %.2f blocks for %d s at %.2f of max, %s, label '%s'",
                f.id(), s.borderRadiusBlocks(), s.mageRingRadiusBlocks(), s.fastCloseBlocksPerSecond(),
                s.slowCloseBlocksPerSecond(), s.killExpansionBlocks(), s.crystalCount(), s.crystalIntervalTicks(),
                s.strikeIntervalTicks(), s.firstStrikeDamage(), l.particleKey(), l.red(), l.green(), l.blue(),
                l.dustSize(), l.pointSpacingBlocks(), l.heightOffsetsBlocks(), l.rowShiftBlocks(),
                l.turnBlocksPerSecond(), l.alternateTurns() ? "alternating" : "together", l.redrawPeriodTicks(),
                f.mageMob(), f.refusedCue().sound(), f.mageLandingLightning() ? "on" : "off",
                f.crystalRingRadiusBlocks(), f.crystalHeightBlocks(), f.litCue().sound(), f.participantRadiusBlocks(),
                f.arena().shape().centreX(), f.arena().shape().centreZ(), f.arena().shape().floorY(),
                f.arena().shape().discRadius(), f.arena().shape().discThickness(), f.arena().shape().pillarCount(),
                f.arena().shape().pillarWidth(), f.arena().shape().pillarRingRadius(),
                f.arena().shape().pillarTopAboveFloor(), f.arena().timeOfDayTicks(), f.arena().entryDxBlocks(),
                f.arena().entryDzBlocks(), f.arena().environment().fileName(), f.arena().allDownSeconds(), f.dragon().mob(), f.dragon().mageThresholds(), f.dragon().winReturnTicks(),
                f.dragon().bossBarColor(), f.dragon().attacks().bite(), f.dragon().attacks().wing(),
                f.dragon().attacks().breathPerSecond(), f.dragon().attacks().contactHitIntervalTicks(),
                f.revive().radiusBlocks(), f.revive().seconds(), f.revive().healthFraction(),
                f.revive().maxRevives().isEmpty() ? "no limit" : "at most " + f.revive().maxRevives().getAsInt(),
                f.revive().label());
    }

    FightContent parse(String id, ConfigurationSection root) {
        ConfigurationSection s = section(root, "settings");
        FightSettings settings = new FightSettings(
                number(s, "border_radius_blocks"),
                number(s, "mage_ring_radius_blocks"),
                number(s, "fast_close_blocks_per_second"),
                number(s, "slow_close_blocks_per_second"),
                number(s, "kill_expansion_blocks"),
                whole(s, "crystal_count"),
                whole(s, "crystal_interval_ticks"),
                whole(s, "strike_interval_ticks"),
                number(s, "first_strike_damage"));
        ConfigurationSection r = section(root, "ring_look");
        String particle = r.getString("particle");
        if (particle == null) throw new IllegalArgumentException("ring_look has no 'particle:'");
        if (!SUPPORTED_PARTICLE.equals(particle)) {
            throw new IllegalArgumentException("ring_look.particle '" + particle + "' is not supported; only '"
                    + SUPPORTED_PARTICLE + "' is");
        }
        List<?> colour = r.getList("color");
        if (colour == null || colour.size() != 3 || !colour.stream().allMatch(c -> c instanceof Integer)) {
            throw new IllegalArgumentException("ring_look needs 'color:' as three whole numbers [r, g, b]"
                    + (r.contains("color") ? ", got '" + r.get("color") + "'" : ", and it is missing"));
        }
        List<?> heights = r.getList("height_offsets_blocks");
        if (heights == null || heights.isEmpty() || !heights.stream().allMatch(h -> h instanceof Number)) {
            throw new IllegalArgumentException("ring_look needs 'height_offsets_blocks:' as a list of numbers"
                    + (r.contains("height_offsets_blocks") ? ", got '" + r.get("height_offsets_blocks") + "'"
                    : ", and it is missing"));
        }
        RingLook look = new RingLook(particle,
                (Integer) colour.get(0), (Integer) colour.get(1), (Integer) colour.get(2),
                number(r, "dust_size"),
                number(r, "point_spacing_blocks"),
                heights.stream().map(h -> ((Number) h).doubleValue()).toList(),
                number(r, "row_shift_blocks"),
                number(r, "turn_blocks_per_second"),
                flag(r, "alternate_turns"),
                whole(r, "redraw_period_ticks"));
        ConfigurationSection m = section(root, "mage");
        String mob = m.getString("mob");
        if (mob == null) throw new IllegalArgumentException("mage has no 'mob:'");
        if (!mobKnown.test(mob)) {
            throw new IllegalArgumentException("mage.mob '" + mob + "' names no loaded mob file (content/mobs/)");
        }
        FightContent.Cue refused = cue(m, "refused");
        boolean landingLightning = flag(m, "landing_lightning");
        ConfigurationSection c = section(root, "crystals");
        double crystalRadius = positive(c, "ring_radius_blocks");
        double crystalHeight = number(c, "height_above_ground_blocks");
        if (!Double.isFinite(crystalHeight)) {
            throw new IllegalArgumentException("crystals.height_above_ground_blocks must be finite: " + crystalHeight);
        }
        FightContent.Cue lit = cue(c, "lit");
        ConfigurationSection fx = section(root, "fixture");
        double reach = positive(fx, "participant_radius_blocks");
        return new FightContent(id, settings, look, mob, refused, crystalRadius, crystalHeight, lit, reach,
                arena(section(root, "arena"), settings), dragon(section(root, "dragon")), landingLightning,
                revive(section(root, "revive")));
    }

    /**
     * {@code revive:} (step F9e). The radius above 0; the seconds a whole number of at least 1; the fraction above 0
     * and at most 1; {@code max_revives} optional (absent is unlimited), a whole number of 0 or more when present; the
     * label a non-blank string.
     */
    private FightContent.Revive revive(ConfigurationSection r) {
        double radius = positive(r, "revive_radius_blocks");
        int seconds = whole(r, "revive_seconds");
        if (seconds < 1) throw new IllegalArgumentException("revive.revive_seconds must be 1 or more, got " + seconds);
        double fraction = number(r, "revive_health_fraction");
        if (!(fraction > 0.0) || fraction > 1.0) {
            throw new IllegalArgumentException("revive.revive_health_fraction must be above 0 and at most 1, got "
                    + fraction);
        }
        java.util.OptionalInt max = java.util.OptionalInt.empty();
        if (r.contains("max_revives")) {
            int m = whole(r, "max_revives");
            if (m < 0) throw new IllegalArgumentException("revive.max_revives must be 0 or more, got " + m);
            max = java.util.OptionalInt.of(m);
        }
        String label = r.getString("label");
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("revive needs a 'label:'" + (r.contains("label") ? ", got a blank one"
                    : ", and it is missing"));
        }
        return new FightContent.Revive(radius, seconds, fraction, max, label);
    }

    /**
     * {@code dragon:}, the arena fight's dragon (step F9). Its mob must be a loaded mob file; its thresholds are checked
     * by the queue that will run them ({@code DragonCrossings}), so the file and the fight cannot disagree about what is
     * legal; the win's delay is a whole number of ticks, 0 or more; the boss bar's colour an Adventure colour name.
     */
    private FightContent.Dragon dragon(ConfigurationSection d) {
        String mob = d.getString("mob");
        if (mob == null) throw new IllegalArgumentException("dragon has no 'mob:'");
        if (!mobKnown.test(mob)) {
            throw new IllegalArgumentException("dragon.mob '" + mob + "' names no loaded mob file (content/mobs/)");
        }
        List<?> raw = d.getList("mage_thresholds");
        if (raw == null || !raw.stream().allMatch(t -> t instanceof Number)) {
            throw new IllegalArgumentException("dragon needs 'mage_thresholds:' as a list of numbers"
                    + (d.contains("mage_thresholds") ? ", got '" + d.get("mage_thresholds") + "'" : ", and it is missing"));
        }
        List<Double> thresholds = raw.stream().map(t -> ((Number) t).doubleValue()).toList();
        try {
            new io.github.butterflysmp.rpg.core.fight.DragonCrossings(thresholds);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("dragon.mage_thresholds " + thresholds + ": " + ex.getMessage());
        }
        int win = whole(d, "win_return_ticks");
        if (win < 0) throw new IllegalArgumentException("dragon.win_return_ticks must be 0 or more: " + win);
        String colour = d.getString("boss_bar_color");
        if (colour == null || net.kyori.adventure.bossbar.BossBar.Color.NAMES.value(colour) == null) {
            throw new IllegalArgumentException("dragon.boss_bar_color must be one of "
                    + net.kyori.adventure.bossbar.BossBar.Color.NAMES.keys() + ", got '" + colour + "'");
        }
        io.github.butterflysmp.rpg.core.fight.DragonAttacks attacks;
        try {
            attacks = new io.github.butterflysmp.rpg.core.fight.DragonAttacks(
                    number(d, "bite"), number(d, "wing"), number(d, "breath_per_second"), whole(d, "contact_hit_interval_ticks"));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("dragon attacks: " + ex.getMessage());
        }
        return new FightContent.Dragon(mob, thresholds, win, colour, attacks);
    }

    /**
     * {@code arena:}, the fight's own world and its generated stand-in arena (step F8b). The shape's own checks are the
     * range checks (a pillar off the disc is refused with its reason). Crystal spot n stands on pillar n, so the
     * crystal count must be the pillar count.
     */
    private static FightContent.Arena arena(ConfigurationSection a, FightSettings settings) {
        io.github.butterflysmp.rpg.core.fight.ArenaShape shape = new io.github.butterflysmp.rpg.core.fight.ArenaShape(
                whole(a, "centre_x"), whole(a, "centre_z"), whole(a, "floor_y"),
                whole(a, "disc_radius_blocks"), whole(a, "disc_thickness_blocks"), whole(a, "pillar_count"),
                positive(a, "pillar_ring_radius_blocks"), whole(a, "pillar_width_blocks"),
                whole(a, "pillar_top_above_floor_blocks"));
        if (shape.pillarCount() != settings.crystalCount()) {
            throw new IllegalArgumentException("arena: crystal spot n stands on pillar n, but crystal_count "
                    + settings.crystalCount() + " is not pillar_count " + shape.pillarCount());
        }
        int time = whole(a, "time_of_day_ticks");
        if (time < 0 || time >= 24000) {
            throw new IllegalArgumentException("arena.time_of_day_ticks must be 0 to 23999: " + time);
        }
        List<?> entry = a.getList("entry_offset_blocks");
        if (entry == null || entry.size() != 2 || !entry.stream().allMatch(e -> e instanceof Number)) {
            throw new IllegalArgumentException("arena needs 'entry_offset_blocks:' as [dx, dz]"
                    + (a.contains("entry_offset_blocks") ? ", got '" + a.get("entry_offset_blocks") + "'"
                    : ", and it is missing"));
        }
        double dx = ((Number) entry.get(0)).doubleValue();
        double dz = ((Number) entry.get(1)).doubleValue();
        if (!shape.isFloorColumn(shape.centreX() + (int) Math.floor(dx), shape.centreZ() + (int) Math.floor(dz))) {
            throw new IllegalArgumentException("arena.entry_offset_blocks [" + dx + ", " + dz + "] is off the disc");
        }
        String env = a.getString("environment");
        FightContent.Environment environment;
        if ("end".equals(env)) environment = FightContent.Environment.END;
        else if ("normal".equals(env)) environment = FightContent.Environment.NORMAL;
        else throw new IllegalArgumentException("arena.environment must be 'end' or 'normal'"
                + (env == null ? ", and it is missing" : ", got '" + env + "'"));
        int allDown = whole(a, "all_down_seconds");
        if (allDown < 0) throw new IllegalArgumentException("arena.all_down_seconds must be 0 or more, got " + allDown);
        return new FightContent.Arena(shape, time, dx, dz, environment, allDown);
    }

    /** {@code <prefix>_sound}, {@code <prefix>_volume}, {@code <prefix>_pitch}; the sound must be known. */
    private FightContent.Cue cue(ConfigurationSection s, String prefix) {
        String sound = s.getString(prefix + "_sound");
        if (sound == null) throw new IllegalArgumentException(s.getName() + " has no '" + prefix + "_sound:'");
        if (!soundKnown.test(sound)) {
            throw new IllegalArgumentException(s.getName() + "." + prefix + "_sound '" + sound + "' is not a known sound");
        }
        double volume = positive(s, prefix + "_volume");
        double pitch = positive(s, prefix + "_pitch");
        return new FightContent.Cue(sound, (float) volume, (float) pitch);
    }

    private static ConfigurationSection section(ConfigurationSection s, String key) {
        ConfigurationSection out = s.getConfigurationSection(key);
        if (out == null) {
            throw new IllegalArgumentException("no '" + key + ":' section"
                    + (s.contains(key) ? " (it is a scalar; it needs indented keys)" : ""));
        }
        return out;
    }

    private static double positive(ConfigurationSection s, String key) {
        double v = number(s, key);
        if (!(v > 0.0) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(s.getName() + "." + key + " must be finite and > 0: " + v);
        }
        return v;
    }

    /** A required number, whole or not; refused by name when missing or not a number. */
    private static double number(ConfigurationSection s, String key) {
        Object v = s.get(key);
        if (!(v instanceof Number n)) {
            throw new IllegalArgumentException(s.getName() + " needs a number '" + key + ":'"
                    + (s.contains(key) ? ", got '" + v + "'" : ", and it is missing"));
        }
        return n.doubleValue();
    }

    /** A required {@code true} or {@code false}; refused by name when missing or anything else. */
    private static boolean flag(ConfigurationSection s, String key) {
        if (!s.isBoolean(key)) {
            throw new IllegalArgumentException(s.getName() + " needs 'true' or 'false' for '" + key + ":'"
                    + (s.contains(key) ? ", got '" + s.get(key) + "'" : ", and it is missing"));
        }
        return s.getBoolean(key);
    }

    /** A required whole number; refused by name when missing or not an integer. */
    private static int whole(ConfigurationSection s, String key) {
        if (!s.isInt(key)) {
            throw new IllegalArgumentException(s.getName() + " needs a whole number '" + key + ":'"
                    + (s.contains(key) ? ", got '" + s.get(key) + "'" : ", and it is missing"));
        }
        return s.getInt(key);
    }
}
