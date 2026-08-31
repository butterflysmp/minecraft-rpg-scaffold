package io.github.butterflysmp.rpg.core.weapon;

/**
 * The plain-text half of the armor tooltip: pure String/number formatters over the content model.
 * No Adventure, no Bukkit -- lives in core so it runs in the 2-second test loop, and the paper
 * {@code ArmorLore} builder only wraps these in colour and layout. Same split, and same reason, as
 * {@link WeaponLoreLines} and {@link ShieldLoreLines}.
 *
 * <p>Describes the ITEM, never the holder, so it is mint-time safe and two players reading the same
 * helmet cannot see different numbers.
 */
public final class ArmorLoreLines {

    private ArmorLoreLines() {}

    /**
     * The stat's name, its own constant because the tooltip colours the LABEL and the VALUE
     * differently -- gray label, green number, the idiom the shield's damage-reduction line and the
     * weapon's damage line both use. Trailing space included so the caller concatenates rather than
     * remembering to.
     *
     * <p><b>"Defense", matching the action bar's {@code &#9736;} field and the stat itself</b>, not
     * "Armor" -- which is vanilla's word for the raw points and would invite a player to read the
     * number as vanilla's ~80% mitigation rather than this project's curve.
     */
    public static final String DEFENSE_LABEL = "Defense: ";

    /**
     * The stat a Growth enchant raises, as it appears on a BONUS line: {@code "+30 Max Health"}.
     *
     * <p>NO COLON and no trailing space, unlike {@link #DEFENSE_LABEL}, because it is a SUFFIX
     * rather than a prefix -- the value leads. That shape difference is not cosmetic; it follows
     * from what the two enchants actually do:
     *
     * <ul>
     *   <li>Defense is a stat the piece ALREADY HAS, so Protection edits the number in place and
     *       the line stays {@code "Defense: 17"}.
     *   <li>Max health is a stat a piece of armor has NONE of, so Growth has no total to modify. It
     *       adds a line that was not there at all.
     * </ul>
     *
     * <p><b>The same words {@code EnchantEffectLine} uses</b>, so the enchant's own description and
     * the line it produces cannot disagree. "Max Health" rather than "Health" because Growth raises
     * the CEILING and grants no current health -- equipping is headroom, never a heal.
     */
    public static final String MAX_HEALTH_LABEL = "Max Health";

    /**
     * The value half of a flat BONUS line: {@code 30 -> "+30"}.
     *
     * <p>The sign belongs to the VALUE, not the label, and that is what makes this reusable: a bonus
     * line is {@code "+N "} followed by any stat's noun, and Slice 2b's Mana Bank needs the
     * identical shape with a different one. Nothing here knows which stat it is, which is the whole
     * point -- the alternative was a {@code growthLine()} that would have needed a
     * {@code manaBankLine()} beside it a slice later.
     */
    public static String bonusValue(double points) {
        return "+" + trimNumber(points);
    }

    /** The value half: {@code 8 -> "8"}. Coloured, where {@link #DEFENSE_LABEL} is not. */
    public static String defenseValue(double defense) {
        return trimNumber(defense);
    }

    /**
     * Label and value in one string: {@code 8 -> "Defense: 8"}.
     *
     * <p>No percent conversion here, and that absence is deliberate: armor points are the stat's
     * own unit -- {@code HealthState.defense}'s javadoc records that they are kept as POINTS and
     * curved exactly once, at the point of use, because points add correctly across four slots
     * where damage-reduction fractions do not. Printing a percent on the item would be printing a
     * number the pieces cannot be added up in, and it would disagree with the {@code &#9736;} field
     * two pixels away. The shield shows a percent because ONE shield is the whole of its stat; the
     * four armor slots are not.
     *
     * <p>So the multiply-by-100 that forced {@code ShieldLoreLines} into a rounding pass never
     * happens, and whole values stay exact. The trim is still here because a content file may
     * legally author {@code 3.0}, and {@code "Defense: 3.0"} on an item that is worth 3 is noise.
     *
     * <p>Worked: {@code 0 -> "Defense: 0"}; {@code 3 -> "Defense: 3"}; {@code 8 -> "Defense: 8"}.
     */
    public static String defenseLabel(double defense) {
        return DEFENSE_LABEL + defenseValue(defense);
    }

    /**
     * The noun a piece is called in its rarity footer: {@code HEAD -> "Helmet"}.
     *
     * <p>An exhaustive switch with NO DEFAULT ARM, the same compiler-guided shape
     * {@code RarityColors} and {@code GearClassLabel} use. A fifth {@link ArmorSlot} is then a
     * compile error here until someone names it, rather than silently footering as some fallback.
     *
     * <p>These are the vanilla NOUNS, not the vanilla item names -- leather's pieces are called Cap,
     * Tunic, Pants and Boots, and those irregular names are authored per piece in the content file
     * as {@code display_name}. This is the generic word for the slot, which is what the footer
     * wants: a Leather Cap's footer reads "Common Helmet" because the footer says what KIND of gear
     * the item is, exactly as a weapon's reads "Rare Melee Weapon" rather than repeating its name.
     */
    public static String slotNoun(ArmorSlot slot) {
        return switch (slot) {
            case HEAD -> "Helmet";
            case CHEST -> "Chestplate";
            case LEGS -> "Leggings";
            case FEET -> "Boots";
        };
    }

    /** {@code 8.0 -> "8"}, {@code 2.5 -> "2.5"}. The idiom the other two lore-line classes use. */
    private static String trimNumber(double n) {
        return GearLoreLines.trimNumber(n);
    }
}
