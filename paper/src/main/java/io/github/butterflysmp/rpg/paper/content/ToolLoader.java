package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolKind;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Turns YAML into {@link ToolDefinition}. The only class that knows the tool schema.
 *
 * <h2>A FLAT LIST, and deliberately not a grid</h2>
 *
 * {@code ArmorLoader} reads a {@code pieces:} map and walks the {@link
 * io.github.butterflysmp.rpg.core.weapon.ArmorSlot} ENUM rather than the file's own keys, so a tier
 * defining three slots is a named refusal. That is right for armor and would be wrong here, and the
 * difference is not stylistic: armor genuinely IS a closed four-slot axis Minecraft decided. Tools
 * are not.
 *
 * <p><b>Shears is not one irregular in an otherwise clean grid; it is the visible edge of a much
 * larger one.</b> Of the 84 durable materials on a booted server, a tier-by-kind grid describes 24.
 * Outside it sit shears, a brush, a fishing rod, flint and steel, a mace, a carrot on a stick and
 * seven spears -- swept and recorded in {@code NEXT.md}, not guessed. A {@code tiers x kinds} file
 * shape would encode a structure fitting under a third of what this loader will eventually hold, and
 * the first irregular would need a special case in a loader designed around regularity.
 *
 * <p>So a file holds a {@code tools:} section keyed by ID, and this walks THE FILE'S OWN KEYS. That
 * is the opposite of armor's rule and correct for the opposite reason: there is no axis to walk, so
 * a "missing" tool is not a detectable concept -- nothing declares how many a file should have.
 * Shears, a mace and a fishing rod are then ordinary entries rather than exceptions.
 *
 * <h2>The key is the id AND the material</h2>
 *
 * {@code iron_pickaxe:} names both, the way an armor piece's {@code material} token is also its id.
 * One string, so the two cannot disagree, and it is already the name a player would guess at
 * {@code /rpg give}. The filename names nothing at all -- it is a container, not a tier -- which is
 * what keeps rarity a PER-ENTRY decision and stops shipping one file from silently deciding the
 * rarity curve for five more.
 *
 * <h2>A bad entry costs the ENTRY, not the file</h2>
 *
 * The other deliberate divergence from {@code ArmorLoader}, which refuses a whole tier on the first
 * bad piece. Its reason is that a PARTIAL SET is worse than none: three quarters of a set with no
 * log line reads as "a tier nobody has authored yet". Tools are not a set. A missing hoe is a
 * missing hoe, and losing four good tools to one typo is the worse outcome. So each entry is named
 * and skipped on its own, and the count is said out loud.
 *
 * <p>The refusals themselves live in {@link ToolDefinition}'s constructor, so the schema knows how
 * to READ a file and the model knows what is legal -- which is what lets a kind that disagrees with
 * its material arrive as a named, skipped entry rather than as a pickaxe whose footer says "Shovel".
 */
public final class ToolLoader {

    /** The section a tool file's entries live under. */
    static final String TOOLS = "tools";

    private final Logger log;

    public ToolLoader(Logger log) {
        this.log = log;
    }

    public ToolRegistry loadAll(File toolsDir) {
        ToolRegistry registry = new ToolRegistry();
        File[] files = toolsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skippedFiles = 0;
        int skippedEntries = 0;
        for (File f : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);

            ConfigurationSection tools = yaml.getConfigurationSection(TOOLS);
            // ZERO IS A DEFECT, NOT A QUIET NO-OP -- a file present but empty is the shape
            // CLAUDE.md names twice, and it is indistinguishable from a working read unless it is
            // said out loud. Refused at the FILE level because there is nothing else in the file to
            // salvage.
            if (tools == null || tools.getKeys(false).isEmpty()) {
                skippedFiles++;
                log.warning("Skipping tool file '" + f.getName() + "': it has no '" + TOOLS
                        + ":' entries. A tool file is a flat list of definitions keyed by material "
                        + "id; an empty one loads nothing and would otherwise say nothing.");
                continue;
            }

            for (String id : tools.getKeys(false)) {
                try {
                    ConfigurationSection entry = tools.getConfigurationSection(id);
                    if (entry == null) {
                        throw new IllegalArgumentException("tool '" + id
                                + "' is not a section; each entry needs its own indented keys");
                    }
                    registry.register(parse(id, entry));
                } catch (RuntimeException ex) {
                    skippedEntries++;
                    // ENTRY, not file. The other four in this file still load, which is the whole
                    // point of the flat list -- see the class javadoc.
                    log.warning("Skipping malformed tool '" + id + "' in '" + f.getName() + "': "
                            + ex.getMessage() + " -- the other entries in this file are unaffected.");
                }
            }
        }
        if (skippedFiles > 0 || skippedEntries > 0) {
            log.warning(skippedEntries + " tool(s) and " + skippedFiles + " tool file(s) were "
                    + "skipped. The server is still running, but those tools cannot be minted.");
        }
        return registry;
    }

    /**
     * One entry to one definition. The section key is both the id and the material.
     */
    private ToolDefinition parse(String id, ConfigurationSection s) {
        String displayName = s.getString("display_name", id);
        Rarity rarity = rarity(s.getString("rarity", "common"), id);

        // REQUIRED, with no default. A kind is what the footer noun is derived from, and the only
        // available fallback would be the generic word "Tool" on every tool in the game -- which is
        // invisible in play and permanent on every item minted before anyone notices. Refused here
        // by resolving to null and letting the record say so, the same contract ArmorLoader uses for
        // a bad slot.
        ToolKind kind = ToolKind.fromName(s.getString("kind"));

        // A scalar where a list belongs is silently ignored by getStringList, which is how a
        // one-line flavor: disappears with no explanation. Warn, exactly as the other loaders do.
        if (s.isString("flavor")) {
            log.warning("Tool '" + id + "' has a scalar 'flavor:'; it must be a YAML list "
                    + "(one '- ' item per line). Ignoring it.");
        }

        // The mint-on-craft claim. Optional, no default: opting in is a per-entry economy decision,
        // not something the loader infers. Read from THIS entry's section, never the file's top
        // level -- a file-level key would have every tool in the file claiming the same item, the
        // index would see a multi-way contest, drop the result, and every tool would silently stop
        // minting. That is the defect ArmorLoader records for its own four-piece tier files.
        Optional<String> craftResult = Optional.ofNullable(s.getString("craft_result"));

        // The id is also the material: one string, so the two cannot disagree.
        return new ToolDefinition(id, displayName, rarity, id, kind,
                s.getStringList("flavor"), craftResult);
    }

    /** Unknown rarity throws, which the caller turns into a named, skipped entry. */
    private static Rarity rarity(String name, String id) {
        Rarity rarity = Rarity.fromName(name);
        if (rarity == null) {
            throw new IllegalArgumentException("tool '" + id + "' has unknown rarity '" + name + "'");
        }
        return rarity;
    }
}
