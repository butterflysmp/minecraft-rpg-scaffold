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
