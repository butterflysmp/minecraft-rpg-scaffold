package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Turns YAML into {@link ArmorDefinition}. The only class that knows the armor schema.
 *
 * <h2>ONE FILE, FOUR DEFINITIONS -- the one place armor diverges from every other loader</h2>
 *
 * Every other content type is one file, one id, id taken from the filename. Armor is authored one
 * file per MATERIAL TIER, each expanding to the four slot pieces. That is deliberate: rarity and
 * flavour are genuinely per-tier properties, and twenty-four files repeating them four times each
 * would put the same fact in four places and invite three of them to drift.
 *
 * <p><b>Ids come from each piece's {@code material} token, not from the filename</b> --
 * {@code diamond_helmet}, {@code golden_chestplate}, {@code leather_boots}. The filename names the
 * tier and is not an id at all. The material token is already unique across the roster, is already
 * the name a player would guess at {@code /rpg give}, and derives correctly for the tiers whose
 * display names are irregular (leather's pieces are Cap, Tunic and Pants, so a name-derived id
 * would produce {@code leather_cap} for an item whose material is {@code leather_helmet}).
 *
 * <p>Fails soft, and LOUDLY about the size of the failure: a malformed tier file is logged, NAMED,
 * and skipped -- and the warning says it took FOUR pieces with it, because one bad file silently
 * costing a quarter of the roster is exactly the kind of loss that reads as "armor is a bit thin"
 * rather than as a defect. The refusals live in {@link ArmorDefinition}'s constructor, so the schema
 * knows how to READ a file and the model knows what is legal.
 *
 * <p><b>The {@code defense} value this reads is DISPLAY-ONLY.</b> It feeds the tooltip line and
 * nothing else; the Defense a worn piece actually contributes is read off vanilla by
 * {@code DefenseModifierItems}. {@code ArmorConsistency} is what checks the two agree, and it runs
 * at boot precisely because nothing in this file can.
 */
public final class ArmorLoader {

    /** The four slot keys a tier file's {@code pieces:} map must be keyed by. */
    static final String PIECES = "pieces";

    private final Logger log;

    public ArmorLoader(Logger log) {
        this.log = log;
    }

    public ArmorRegistry loadAll(File armorDir) {
        ArmorRegistry registry = new ArmorRegistry();
        File[] files = armorDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skippedFiles = 0;
        int skippedPieces = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                for (ArmorDefinition piece : parseTier(tierOf(f), yaml)) {
                    registry.register(piece);
                }
            } catch (RuntimeException ex) {
                skippedFiles++;
                skippedPieces += ArmorSlot.values().length;
                // Naming the piece count is the point of this message. "Skipping malformed armor
                // 'diamond.yml'" understates the loss by a factor of four, and a roster that is
                // four pieces short does not look broken from in-game -- it looks like a tier
                // nobody has authored yet.
                log.warning("Skipping malformed armor tier '" + f.getName() + "': " + ex.getMessage()
                        + " -- this file defines " + ArmorSlot.values().length
                        + " pieces, and ALL of them are now missing.");
            }
        }
        if (skippedFiles > 0) {
            log.warning(skippedFiles + " armor tier file(s) were skipped, costing " + skippedPieces
                    + " pieces. The server is still running, but those pieces cannot be minted.");
        }
        return registry;
    }

    /** The tier name is the filename: diamond.yml -> diamond. Used in messages, never as an id. */
    private static String tierOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    /**
     * One tier file to its four pieces.
     *
     * <p>Throws on the FIRST bad piece rather than skipping it, so a tier is all-or-nothing. A
     * partially-loaded tier -- three slots present, one silently absent -- is the worst of the
     * available outcomes: a player finds three quarters of a set and no log line explains the gap.
     * Whole-file refusal makes the loss loud and the message actionable.
     */
    private List<ArmorDefinition> parseTier(String tier, ConfigurationSection s) {
        Rarity rarity = rarity(s.getString("rarity", "common"), tier);

        // A scalar where a list belongs is silently ignored by getStringList, which is how a
        // one-line flavor: disappears with no explanation. Warn, exactly as the other loaders do.
        if (s.isString("flavor")) {
            log.warning("Armor tier '" + tier + "' has a scalar 'flavor:'; it must be a YAML list "
                    + "(one '- ' item per line). Ignoring it.");
        }
        List<String> flavor = s.getStringList("flavor");

        ConfigurationSection pieces = s.getConfigurationSection(PIECES);
        if (pieces == null) {
            throw new IllegalArgumentException("armor tier '" + tier + "' has no '" + PIECES
                    + ":' section; it must name all " + ArmorSlot.values().length + " slots");
        }

        // Walked over the SLOT AXIS rather than over the file's own keys, deliberately. Iterating
        // the YAML would silently accept a file that defines three slots, and would accept a typo'd
        // key ('foot:') as a fourth piece nobody asked for. Walking the enum means a missing slot is
        // a named refusal and an unknown key is simply never read.
        List<ArmorDefinition> parsed = new java.util.ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            String key = slot.name().toLowerCase(java.util.Locale.ROOT);
            ConfigurationSection piece = pieces.getConfigurationSection(key);
            if (piece == null) {
                throw new IllegalArgumentException("armor tier '" + tier + "' is missing its '"
                        + key + "' piece");
            }
            parsed.add(parsePiece(tier, slot, piece, rarity, flavor));
        }
        return parsed;
    }

    private ArmorDefinition parsePiece(String tier, ArmorSlot slot, ConfigurationSection s,
                                       Rarity rarity, List<String> flavor) {
        // The material is REQUIRED and has no default, unlike every other loader's material key.
        // It cannot have one: it is also the id, so a default would give two tiers the same id and
        // the second would be refused by the registry as a duplicate -- a confusing way to report a
        // missing key.
        String material = s.getString("material");
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("armor tier '" + tier + "' piece '"
                    + slot.name().toLowerCase(java.util.Locale.ROOT)
                    + "' has no 'material:'; it is also the piece's id, so it cannot be defaulted");
        }

        String displayName = s.getString("display_name", material);

        // Defaults to 0, the way the weapon loader's attack_damage and the shield loader's block_dr
        // do: an absent stat is a stat of zero and the tooltip says so, rather than the loader
        // inventing a number the file never asked for. ArmorConsistency will then report the
        // mismatch against vanilla loudly at boot, which is the better place to catch it.
        double defense = s.getDouble("defense", 0.0);

        // THE CLAIM IS READ FROM THIS SLOT'S SECTION, never from the tier file's top level.
        //
        // One tier file yields FOUR definitions, so a file-level key would have all four claiming
        // the same item -- the index would see a four-way contest, drop the result, and ALL ARMOR
        // WOULD SILENTLY STOP MINTING. Worse, the boot warning would name four armor definitions
        // sharing a craft_result, which reads as a content authoring error and sends whoever
        // investigates into the yml rather than into this method.
        //
        // `s` is the slot's own section, the same one `material` above came from.
        Optional<String> craftResult = Optional.ofNullable(s.getString("craft_result"));

        return new ArmorDefinition(material, displayName, rarity, material, slot, defense, flavor,
                craftResult);
    }

    /** Unknown rarity throws, which the caller turns into a named, skipped file. */
    private static Rarity rarity(String name, String tier) {
        Rarity rarity = Rarity.fromName(name);
        if (rarity == null) {
            throw new IllegalArgumentException("armor tier '" + tier + "' has unknown rarity '"
                    + name + "'");
        }
        return rarity;
    }
}
