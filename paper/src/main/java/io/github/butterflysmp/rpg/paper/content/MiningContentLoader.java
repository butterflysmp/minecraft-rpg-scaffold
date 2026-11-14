package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mining.OreDefinition;
import io.github.butterflysmp.rpg.core.mining.OreRegistry;
import io.github.butterflysmp.rpg.core.mining.VeinSize;
import io.github.butterflysmp.rpg.core.mining.VeinSizeRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * Loads {@code content/vein_sizes/} and {@code content/ores/} (PLAN-mining.md 5.3). It is the only
 * class that knows the mining schema.
 *
 * <p><b>Every number is REQUIRED, never defaulted.</b> {@code getInt(key)} answers 0 for a missing
 * key, and 0 is a legal payout, so a typo would ship a patch that pays nothing and says nothing.
 * Each required number is read through {@link #requireInt}, which refuses a missing or non-integer
 * value by name. A malformed entry is skipped and named, the same shape as {@code ToolLoader}; the
 * others in its file still load.
 *
 * <p>The refusals that need no server live in the core records ({@link VeinSize},
 * {@link OreDefinition}). The cross-file and registry checks (does the size exist, does the item
 * or sound resolve) are {@code ContentValidator.validateMining}'s.
 */
public final class MiningContentLoader {

    static final String VEIN_SIZES = "vein_sizes";
    static final String ORES = "ores";

    private final Logger log;

    public MiningContentLoader(Logger log) {
        this.log = log;
    }

    public VeinSizeRegistry loadVeinSizes(File dir) {
        VeinSizeRegistry registry = new VeinSizeRegistry();
        walk(dir, VEIN_SIZES, "vein size", (id, s) -> registry.register(parseSize(id, s)));
        return registry;
    }

    public OreRegistry loadOres(File dir) {
        OreRegistry registry = new OreRegistry();
        walk(dir, ORES, "ore", (id, s) -> registry.register(parseOre(id, s)));
        return registry;
    }

    private void walk(File dir, String root, String noun, BiConsumer<String, ConfigurationSection> parse) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        Arrays.sort(files); // deterministic load order across filesystems
        int skippedFiles = 0;
        int skippedEntries = 0;
        for (File f : files) {
            ConfigurationSection entries = YamlConfiguration.loadConfiguration(f).getConfigurationSection(root);
            if (entries == null || entries.getKeys(false).isEmpty()) {
                skippedFiles++;
                log.warning("Skipping " + noun + " file '" + f.getName() + "': it has no '" + root
                        + ":' entries, so it loads nothing.");
                continue;
            }
            for (String id : entries.getKeys(false)) {
                try {
                    ConfigurationSection entry = entries.getConfigurationSection(id);
                    if (entry == null) {
                        throw new IllegalArgumentException(noun + " '" + id
                                + "' is not a section; each entry needs its own indented keys");
                    }
                    parse.accept(id, entry);
                } catch (RuntimeException ex) {
                    skippedEntries++;
                    log.warning("Skipping malformed " + noun + " '" + id + "' in '" + f.getName() + "': "
                            + ex.getMessage() + " -- the other entries in this file are unaffected.");
                }
            }
        }
        if (skippedFiles > 0 || skippedEntries > 0) {
            log.warning(skippedEntries + " " + noun + "(s) and " + skippedFiles + " " + noun
                    + " file(s) were skipped.");
        }
    }

    private static VeinSize parseSize(String id, ConfigurationSection s) {
        ConfigurationSection spots = requireSection(s, "spots", "vein size '" + id + "'");
        return new VeinSize(id,
                requireInt(s, "width", "vein size '" + id + "'"),
                requireInt(s, "height", "vein size '" + id + "'"),
                requireInt(spots, "min", "vein size '" + id + "' spots"),
                requireInt(spots, "max", "vein size '" + id + "' spots"));
    }

    private static OreDefinition parseOre(String id, ConfigurationSection s) {
        String where = "ore '" + id + "'";
        ConfigurationSection payout = requireSection(s, "payout", where);
        ConfigurationSection perSpot = requireSection(payout, "per_spot", where + " payout");
        ConfigurationSection bonusSection = requireSection(payout, "completion_bonus", where + " payout");
        Map<String, Integer> bonus = new LinkedHashMap<>();
        for (String size : bonusSection.getKeys(false)) {
            bonus.put(size, requireInt(bonusSection, size, where + " payout.completion_bonus"));
        }
        ConfigurationSection models = requireSection(s, "models", where);
        ConfigurationSection sounds = requireSection(s, "sounds", where);
        if (s.isString("sizes")) {
            throw new IllegalArgumentException(where + " has a scalar 'sizes:'; it must be a list, e.g. [small]");
        }
        List<String> sizes = s.getStringList("sizes");
        return new OreDefinition(id,
                s.getString("display_name"),
                payout.getString("item"),
                requireInt(perSpot, "min", where + " payout.per_spot"),
                requireInt(perSpot, "max", where + " payout.per_spot"),
                bonus,
                requireInt(s, "regrowth_ticks", where),
                sizes,
                models.getString("patch"), models.getString("spot"), models.getString("crack"),
                sounds.getString("miss"), sounds.getString("break"));
    }

    private static ConfigurationSection requireSection(ConfigurationSection s, String key, String where) {
        ConfigurationSection section = s.getConfigurationSection(key);
        if (section == null) {
            throw new IllegalArgumentException(where + " has no '" + key + ":' section"
                    + (s.contains(key) ? " (it is a scalar; it needs indented keys)" : ""));
        }
        return section;
    }

    /** A required whole number: refused by name when missing or not an integer, never defaulted. */
    private static int requireInt(ConfigurationSection s, String key, String where) {
        if (!s.isInt(key)) {
            throw new IllegalArgumentException(where + " needs a whole number '" + key + ":'"
                    + (s.contains(key) ? ", got '" + s.get(key) + "'" : ", and it is missing"));
        }
        return s.getInt(key);
    }
}
