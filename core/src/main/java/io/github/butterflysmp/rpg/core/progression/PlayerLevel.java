package io.github.butterflysmp.rpg.core.progression;

import java.util.OptionalLong;

/**
 * The player level curve: lifetime XP in, level out.
 *
 * <p>Pure, zero dependencies, and the whole of the progression mechanism. The impure half -- reading
 * a profile, hooking an event -- lives in {@code paper/}.
 *
 * <h2>*** STORE THE TOTAL, DERIVE THE LEVEL. NEVER WRITE A LEVEL TO A PROFILE. ***</h2>
 *
 * <b>The profile holds ONE number: lifetime XP.</b> Every level in the game is computed from it on
 * read, through {@link #levelFor}.
 *
 * <p><b>THIS IS THE ONE THING THE PREDECESSOR PROJECT GOT WRONG, AND IT HAS A CLASS NAMED AFTER THE
 * CONSEQUENCE.</b> It stored the level, shipped a curve, retuned the curve, and then needed
 * {@code XpCurveMigration} to convert everyone's stored progress. <b>With the total stored, a retune
 * costs nothing</b> -- the same lifetime XP simply lands on a different level -- and there is no
 * migration to write, to test, or to get wrong.
 *
 * <p><b>If you find yourself writing a level to the profile, stop.</b> The level is a VIEW.
 *
 * <h2>LIFETIME XP IS MONOTONIC. NOTHING EVER DECREASES IT.</h2>
 *
 * It counts what a player has earned, not what they hold. <b>Spending at the enchant table does not
 * reduce it and a grindstone refund does not raise it</b> -- see {@code PlayerLevelListener} for why
 * that falls out of the hook rather than being guarded for.
 *
 * <h2>A {@code long}, AND THE REASON IS THE WORD "LIFETIME" RATHER THAN THE SIZE OF THE CURVE</h2>
 *
 * <b>The level-99 total is 11,642,250, which fits an {@code int} with about 184x headroom</b> --
 * measured, not assumed. So the curve alone would not justify the type.
 *
 * <p><b>What justifies it is that this value does not stop at the cap.</b> Reaching level 99 does
 * not stop a player earning, and lifetime XP keeps accumulating for as long as the server lives.
 * An {@code int} would hold about 184 times the level-99 total and then <b>overflow silently into a
 * negative number</b>, which {@link #levelFor} would read as level 1.
 *
 * <p>Recorded this way because the obvious justification -- <i>"11.6 million needs a long"</i> -- is
 * <b>false</b>, and a type defended by a wrong reason is one the next person narrows.
 */
public final class PlayerLevel {

    private PlayerLevel() {}

    /** The cap. There is no level 100 and no XP requirement beyond 99. */
    public static final int MAX_LEVEL = 99;

    /**
     * XP to advance FROM this index TO the next level. Index 0 is unused; 1..98 are the rungs.
     *
     * <h2>HAND-AUTHORED. DO NOT REGENERATE IT FROM A FORMULA.</h2>
     *
     * The predecessor's comment records that it was generated from a two-segment exponential and
     * then <b>rounded to the nearest 10</b> -- so no closed form reproduces these exact numbers, and
     * a "simplification" that replaced the table with the formula would move every threshold in the
     * game by a few XP while looking tidier.
     *
     * <p>The three anchors, verified against the source table rather than quoted: {@code [1] = 1000},
     * {@code [60] = 150000}, {@code [98] = 400000}.
     */
    private static final long[] XP_TO_NEXT = {
            0L,
            1000L, 1090L, 1190L, 1290L, 1400L, 1530L, 1660L, 1810L, 1970L, 2150L,
            2340L, 2550L, 2770L, 3020L, 3280L, 3570L, 3890L, 4240L, 4610L, 5020L,
            5470L, 5950L, 6480L, 7050L, 7680L, 8360L, 9100L, 9900L, 10780L, 11740L,
            12780L, 13910L, 15140L, 16490L, 17950L, 19540L, 21270L, 23160L, 25210L, 27440L,
            29880L, 32520L, 35410L, 38540L, 41960L, 45680L, 49730L, 54140L, 58940L, 64160L,
            69850L, 76040L, 82780L, 90110L, 98100L, 106800L, 116260L, 126570L, 137790L, 150000L,
            153920L, 157950L, 162080L, 166310L, 170660L, 175130L, 179700L, 184400L, 189230L, 194170L,
            199250L, 204460L, 209810L, 215290L, 220920L, 226700L, 232620L, 238710L, 244950L, 251350L,
            257930L, 264670L, 271590L, 278690L, 285980L, 293460L, 301130L, 309000L, 317080L, 325370L,
            333880L, 342610L, 351570L, 360760L, 370200L, 379870L, 389810L, 400000L
    };

    /** Cumulative XP needed to REACH each level. {@code [1] = 0} -- level 1 is where everyone starts. */
    private static final long[] TOTAL_FOR_LEVEL = cumulative();

    private static long[] cumulative() {
        long[] totals = new long[MAX_LEVEL + 1];
        for (int level = 2; level <= MAX_LEVEL; level++) {
            totals[level] = totals[level - 1] + XP_TO_NEXT[level - 1];
        }
        return totals;
    }

    /**
     * The level this much lifetime XP buys.
     *
     * <p>Clamps at both ends: negative or zero is level 1, and anything at or beyond the level-99
     * total is 99. <b>Never throws</b> -- this reads a number off disk that may have been
     * hand-edited, and a progression display is not a place to take out a join handler.
     */
    public static int levelFor(long lifetimeXp) {
        if (lifetimeXp <= 0) return 1;

        // Linear rather than binary: 99 entries, and this runs on a menu open rather than per tick.
        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (lifetimeXp >= TOTAL_FOR_LEVEL[level]) return level;
        }
        return 1;
    }

    /**
     * Cumulative XP needed to reach {@code level} from nothing.
     *
     * @throws IllegalArgumentException for a level outside {@code 1..99}. Unlike {@link #levelFor},
     *         this takes a LEVEL -- which is always computed, never stored -- so an out-of-range
     *         value is a programming error rather than bad data, and it is refused loudly. Same
     *         asymmetry {@code EnchantCost.xpPoints} draws against its clamping power argument.
     */
    public static long totalForLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "no total for player level " + level + "; levels are 1.." + MAX_LEVEL);
        }
        return TOTAL_FOR_LEVEL[level];
    }

    /**
     * How much more lifetime XP until the next level, or EMPTY at the cap.
     *
     * <h2>AN {@code OptionalLong} RATHER THAN A SENTINEL, AND THAT IS A DELIBERATE DEPARTURE</h2>
     *
     * <b>The predecessor returned {@code Long.MAX_VALUE} at the cap.</b> That is a number, so it
     * renders -- <i>"9223372036854775807 XP to next level"</i> -- and every caller has to remember
     * not to print it. <b>An empty optional cannot be printed by accident</b>; the maxed case has to
     * be handled to compile.
     *
     * <p>Same reasoning as {@code SettingsMenuLayout.chooserFor} returning an {@code OptionalInt}
     * rather than {@code -1}: a sentinel that is a valid value of its own type is a defect waiting
     * for the one caller who forgets.
     */
    public static OptionalLong xpToNextLevel(long lifetimeXp) {
        int level = levelFor(lifetimeXp);
        if (level >= MAX_LEVEL) return OptionalLong.empty();
        return OptionalLong.of(TOTAL_FOR_LEVEL[level + 1] - Math.max(0, lifetimeXp));
    }

    /** How far into the current level this much XP is. Zero exactly at a level boundary. */
    public static long intoCurrentLevel(long lifetimeXp) {
        return Math.max(0, lifetimeXp) - TOTAL_FOR_LEVEL[levelFor(lifetimeXp)];
    }

    /** Is this player at the cap? The one question a display must ask before formatting a total. */
    public static boolean isMaxed(long lifetimeXp) {
        return levelFor(lifetimeXp) >= MAX_LEVEL;
    }

    /**
     * Add to a lifetime total without wrapping.
     *
     * <h2>ONE EXPRESSION, BECAUSE THERE ARE TWO WRITERS AND THE WRONG ANSWER IS SILENT</h2>
     *
     * {@code PlayerLevelListener}'s orb hook and {@code /rpg playerxp add} both do this, and a
     * plain {@code +} that overflows produces a NEGATIVE total, which {@link #levelFor} reads as
     * <b>level 1</b>. The worst failure available to a progression number is total loss wearing a
     * new player's face, so it is written once rather than twice.
     *
     * <p>Unreachable in play -- the level-99 total is 11,642,250 -- but {@code set} can put any
     * {@code long} in the profile, and a hand-edited file reaches this with no setter in between.
     */
    public static long plus(long lifetimeXp, long amount) {
        long sum = lifetimeXp + amount;
        // Overflow in a same-sign addition is exactly "the result moved the wrong way".
        if (amount > 0 && sum < lifetimeXp) return Long.MAX_VALUE;
        if (amount < 0 && sum > lifetimeXp) return Long.MIN_VALUE;
        return sum;
    }
}
