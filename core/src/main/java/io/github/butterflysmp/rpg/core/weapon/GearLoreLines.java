package io.github.butterflysmp.rpg.core.weapon;

/**
 * Number formatting shared by every gear tooltip. One copy of what was four.
 *
 * <p>{@link #trimNumber} existed verbatim in {@code WeaponLore.number},
 * {@code WeaponLoreLines.trimNumber}, {@code ShieldLoreLines.trimNumber} and
 * {@code ArmorLoreLines.trimNumber} -- four identical bodies under two names, one of them in
 * {@code paper} where it had no business being, since it formats a number and needs no Bukkit.
 *
 * <p>Four copies of a formatter is not merely untidy here. Every tooltip in the game runs through
 * one of them, so a rounding change applied to three of the four would produce items that disagree
 * about how to print the same value, and no test that reads only one kind of item would see it.
 */
public final class GearLoreLines {

    private GearLoreLines() {}

    /**
     * The gear-score line's label, on every kind of scoreable gear.
     *
     * <p><b>ONE OWNER FOR A COLUMN THREE TOOLTIPS SHARE.</b> A weapon, a shield and a piece of armour
     * all render this line, so the label lives here rather than three times -- the same argument this
     * class already makes for {@link #trimNumber}, and the one {@code PlayerLevelLines} makes when it
     * deliberately has no padder of its own. Three copies would let two kinds of gear come to call the
     * same number different things, and no test that reads only one kind would ever see it.
     *
     * <p><b>IT PROMISES NOTHING ABOUT DAMAGE, DELIBERATELY.</b> A score raises a weapon's attack
     * damage and an armour piece's Defense, and on a SHIELD it raises neither -- a shield is scoreable
     * because it feeds the wearer's average, which bands their next drop. A label promising damage
     * would be false on every shield in the game; this one is true on all three kinds.
     */
    public static final String SCORE_NOUN = "Gear Score";

    /**
     * The same words with the tooltip colon, DERIVED rather than written twice.
     *
     * <p>The item tooltip needs { "Gear Score: "} and the stats head needs { "Gear Score"}
     * -- the head pads through { StatsSheetLines.label}, so a colon there would be padded INTO
     * the gap and the column would not line up with the eight combat labels beside it.
     *
     * <p><b>Two literals differing by two characters is the purest drift case there is</b>, and it
     * would present as one screen calling the number something slightly different from another. So
     * there is ONE literal and the second form is built from it -- { ResourceCost} states the
     * rule: two literals cannot drift apart if there is only one.
     */
    public static final String SCORE_LABEL = SCORE_NOUN + ": ";

    /**
     * A score as it renders: a bare whole number, {@code 340}.
     *
     * <p>Not routed through {@link #trimNumber}, and that is the one decision in this method. A score
     * is an {@code int} by construction -- {@code GearScore} clamps to integers and the PDC stores an
     * INTEGER -- so there is no {@code .0} to trim and nothing fractional to preserve. Taking a double
     * here would invite a caller to hand over a scaled damage figure by mistake and have it render as
     * though it were the score.
     */
    public static String scoreValue(int score) {
        return String.valueOf(score);
    }

    /**
     * A double as the shortest honest string: {@code 50.0 -> "50"}, {@code 12.5 -> "12.5"}.
     *
     * <p>Whole values lose the {@code .0} because content may legally author either {@code 3} or
     * {@code 3.0} and both are the same double -- printing "Defense: 3.0" on a piece worth 3 is
     * noise on every item in the game. Fractional values keep their fraction, because a genuinely
     * fractional stat is real content and truncating it would make the tooltip disagree with the
     * arithmetic.
     *
     * <p>The infinity guard is not decoration: {@code (long) Double.POSITIVE_INFINITY} is
     * {@code Long.MAX_VALUE}, so without it an infinite stat would print as 9223372036854775807
     * rather than as the obviously-broken "Infinity" that sends someone to the content file.
     */
    public static String trimNumber(double n) {
        if (n == Math.floor(n) && !Double.isInfinite(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }
}
