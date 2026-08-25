package io.github.butterflysmp.rpg.core.enchant;

/**
 * The TEXT of an enchant's tooltip line. No colour, no Adventure, no Bukkit.
 *
 * Same split as {@code WeaponLoreLines} and {@code WeaponLore}: the string logic lives here so it
 * runs in the two-second unit-test loop, and {@code EnchantLore} adds only the grey and the italic.
 * A roman numeral is exactly the kind of thing that is embarrassing to get wrong and expensive to
 * check by booting a server and hovering an item.
 */
public final class EnchantLoreLines {

    private EnchantLoreLines() {}

    private static final String[] NUMERALS = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    };

    /**
     * {@code 1..10} to {@code "I".."X"}; 0 and negatives to {@code ""}; past the table, the arabic
     * number.
     *
     * The fallback is deliberate rather than a throw. This is a COSMETIC line, and the one thing it
     * must never do is be the reason a {@code /rpg give} fails or an item cannot render -- the same
     * instinct {@code WeaponLore.elementLine} follows when an element id is missing. A level past X
     * cannot occur while {@link EnchantState#MAX_LEVEL} is 3; it renders as "11" rather than
     * crashing if that ever stops being true.
     */
    public static String romanNumeral(int level) {
        if (level <= 0) return "";
        if (level < NUMERALS.length) return NUMERALS[level];
        return String.valueOf(level);
    }

    /**
     * An active enchant's line: {@code "Unbreaking III"}.
     *
     * <p>The numeral is OMITTED when the enchant's maximum level is 1, which is vanilla's own rule
     * -- Mending renders as "Mending", never "Mending I", because a level on a single-level enchant
     * is noise that implies a II exists. Driven by the enchant's declared {@code max_level} rather
     * than by the level being 1, so an enchant that CAN reach III still reads "Unbreaking I" at its
     * lowest level, which is the information the player actually needs.
     */
    public static String label(String displayName, int level, int maxLevel) {
        if (maxLevel <= 1) return displayName;
        String numeral = romanNumeral(level);
        return numeral.isEmpty() ? displayName : displayName + " " + numeral;
    }
}
