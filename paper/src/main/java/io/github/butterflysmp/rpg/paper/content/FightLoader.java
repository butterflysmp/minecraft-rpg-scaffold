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
import java.util.logging.Logger;

/**
 * Loads {@code content/fights/} (PLAN-dragon-fight.md step F4). It is the only class that knows the fight schema.
 *
 * <p><b>Every key is REQUIRED, never defaulted</b>, the shape of {@code MiningContentLoader}: a missing key would
 * otherwise read as 0, and a 0 radius or a 0 damage is a fight that silently does nothing. A file with any missing or
 * malformed key is REFUSED whole, with a {@code Refusing} warning naming the key, so R0c's pattern catches it at boot;
 * the range checks are the core records' ({@link FightSettings}, {@link RingLook}), and their message is the reason.
 */
public final class FightLoader {

    static final String SUPPORTED_PARTICLE = "dust";

    private final Logger log;

    public FightLoader(Logger log) {
        this.log = log;
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
                        + "every %.2f blocks at %s every %d ticks; participants within %.1f",
                f.id(), s.borderRadiusBlocks(), s.mageRingRadiusBlocks(), s.fastCloseBlocksPerSecond(),
                s.slowCloseBlocksPerSecond(), s.killExpansionBlocks(), s.crystalCount(), s.crystalIntervalTicks(),
                s.strikeIntervalTicks(), s.firstStrikeDamage(), l.particleKey(), l.red(), l.green(), l.blue(),
                l.dustSize(), l.pointSpacingBlocks(), l.heightOffsetsBlocks(), l.redrawPeriodTicks(),
                f.participantRadiusBlocks());
    }

    static FightContent parse(String id, ConfigurationSection root) {
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
                whole(r, "redraw_period_ticks"));
        ConfigurationSection fx = section(root, "fixture");
        double reach = number(fx, "participant_radius_blocks");
        if (!(reach > 0.0) || Double.isInfinite(reach)) {
            throw new IllegalArgumentException("fixture.participant_radius_blocks must be finite and > 0: " + reach);
        }
        return new FightContent(id, settings, look, reach);
    }

    private static ConfigurationSection section(ConfigurationSection s, String key) {
        ConfigurationSection out = s.getConfigurationSection(key);
        if (out == null) {
            throw new IllegalArgumentException("no '" + key + ":' section"
                    + (s.contains(key) ? " (it is a scalar; it needs indented keys)" : ""));
        }
        return out;
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

    /** A required whole number; refused by name when missing or not an integer. */
    private static int whole(ConfigurationSection s, String key) {
        if (!s.isInt(key)) {
            throw new IllegalArgumentException(s.getName() + " needs a whole number '" + key + ":'"
                    + (s.contains(key) ? ", got '" + s.get(key) + "'" : ", and it is missing"));
        }
        return s.getInt(key);
    }
}
