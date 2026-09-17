package io.github.butterflysmp.rpg.core.progression;

import java.util.Locale;

/**
 * The plain-text half of the progression readout: level, lifetime XP, and XP to the next level.
 *
 * <p>Pure String/number formatters, no Adventure and no Bukkit, so they run in the 2-second loop --
 * the same split {@code StatsSheetLines} makes for the eight combat stats, and the labels are padded
 * to that class's {@code LABEL_WIDTH} so the two blocks line up in one tooltip.
 *
 * <h2>THOUSANDS SEPARATORS, AND {@code Locale.ROOT} RATHER THAN THE DEFAULT</h2>
 *
 * A lifetime total reaches <b>11,642,250</b> at the cap, and eight unbroken digits are unreadable.
 * <b>{@code Locale.ROOT} is explicit because the default locale is a property of the SERVER'S
 * MACHINE</b>: a German-locale JVM renders {@code %,d} as {@code 11.642.250}, so the same build
 * would print different numbers on two servers and a gate reading would not be reproducible.
 *
 * <h2>*** THERE IS NO "XP TO NEXT" LINE AT THE CAP, AND THAT IS THE WHOLE REASON
 * {@code xpToNextLevel} RETURNS AN OPTIONAL ***</h2>
 *
 * The predecessor returned {@code Long.MAX_VALUE} there and it rendered. <b>The tempting fix is a
 * placeholder value on the same line</b> -- {@code "To Next    MAX"} -- and it is the same defect
 * wearing better clothes: the line's subject is an AMOUNT REMAINING, and there is no amount
 * remaining, so any value in that column is a lie about a quantity rather than a note about a state.
 *
 * <p><b>So the line is ABSENT at 99 and the marker goes on the LEVEL line instead</b>, where it is
 * a fact about the level rather than a number in a column that should be empty.
 *
 * <p>This is not the "a conditional line is not an optional line" case that {@code NexusStatsLore}
 * warns about for the quiver pair. That line is conditional on <b>what the player is holding</b> and
 * must not be dropped for tidiness; this one is absent because <b>the quantity does not exist</b>.
 */
public final class PlayerLevelLines {

    private PlayerLevelLines() {}

    /**
     * <b>THERE IS NO {@code label()} AND NO {@code LABEL_WIDTH} HERE, DELIBERATELY.</b> Callers pad
     * with {@code StatsSheetLines.label}, exactly as {@code StatsSheet} does for the eight combat
     * labels, so the shared column has <b>one owner</b>.
     *
     * <p>The first draft copied the padder and added a test asserting the two widths agreed. That
     * is the {@code PAINTED_SLOTS} lesson inverted: <b>two lists checked against each other, where
     * one list used twice removes the defect class instead of watching for it.</b>
     */
    public static final String LEVEL_LABEL = "Level";
    public static final String LIFETIME_LABEL = "Lifetime XP";
    public static final String TO_NEXT_LABEL = "To Next";

    /**
     * What a level-99 player's level line carries instead of a bare number.
     *
     * <p><b>Parenthesised and on the LEVEL line by design</b> -- see the class javadoc. Putting it
     * in the "To Next" column would restate the predecessor's {@code Long.MAX_VALUE} defect as a
     * word.
     */
    public static final String MAX_MARKER = "(MAX)";

    /** A whole number with thousands separators: {@code 11642250} reads {@code "11,642,250"}. */
    public static String amount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** The level, with {@link #MAX_MARKER} appended at the cap: {@code "13"}, {@code "99 (MAX)"}. */
    public static String level(long lifetimeXp) {
        int level = PlayerLevel.levelFor(lifetimeXp);
        return PlayerLevel.isMaxed(lifetimeXp) ? level + " " + MAX_MARKER : String.valueOf(level);
    }

    /** Total XP ever earned. Not "current XP" -- nothing spends it; {@code PlayerLevel} says why. */
    public static String lifetime(long lifetimeXp) {
        return amount(Math.max(0L, lifetimeXp));
    }

    /**
     * XP remaining to the next level.
     *
     * @throws IllegalStateException at the cap, where there is no next level. <b>Loud rather than a
     *         placeholder</b>: the caller must not render this line at all there, and
     *         {@code PlayerLevel.isMaxed} is how it asks. A returned string would let a caller print
     *         the line by accident, which is the exact failure the optional was introduced to stop.
     */
    public static String toNext(long lifetimeXp) {
        return amount(PlayerLevel.xpToNextLevel(lifetimeXp).orElseThrow(() ->
                new IllegalStateException("no next level at the cap; ask PlayerLevel.isMaxed first")));
    }
}
