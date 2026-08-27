package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.xp.XpCurve;

/**
 * What an enchant costs, in XP POINTS, after the bookshelf discount.
 *
 * <p>Beside {@link Unbreaking} and {@link DamageEnchants} because it is the same kind of thing: a
 * pure mechanism the table drives, with the impure half -- reading a player's wallet, counting
 * blocks -- left at the call site.
 *
 * <p><b>Points, not levels, and the difference is the whole design.</b> An earlier version of this
 * priced in levels and discounted the level COUNT: 40 levels for III, less 30% is 28 levels. But
 * levels are not linear -- 40 levels is 2920 points and 28 levels is 1186 -- so that "30% discount"
 * was really 59% off, and a different percentage at every rung. Discounting the POINTS is the only
 * way the number on the bookshelf readout means what it says.
 *
 * <p><b>Integer arithmetic, and here it genuinely matters.</b> Write the discount as
 * {@code (int)(base * (1 - power/100.0))} and III at full power charges 2043 against 2044 -- a point
 * less than the cell printed. Measured, and only at III: {@code 1 - 30/100.0} is the same double as
 * {@code 0.7}, and multiplying it by 910 lands close enough to 637 to round back up, while 2920 does
 * not. One wrong cell out of nine, which is why the test asserts the whole grid and not a sample.
 * {@code base * (100 - power) / 100} has no such question in it, and it FLOORS, so the rounding is
 * always the player's.
 */
public final class EnchantCost {

    private EnchantCost() {}

    /**
     * The tuning knob, in the units a designer thinks in: reaching I costs what a player banks
     * getting to level 16 from nothing, II what they bank reaching 25, III reaching 40.
     *
     * <p>A uniform system knob rather than per-enchant content -- the same call {@code MAX_ENERGY}
     * is. Every enchant costs the same to reach a given level; what differs is what the level buys.
     *
     * <p><b>Levels are the INPUT to the price, never the price itself.</b> They describe the bank of
     * a player starting from zero, which is the only point on the curve where "16 levels" is an
     * unambiguous amount of money. Past that it is 352 points and nothing else.
     */
    private static final int[] BASE_LEVELS = {16, 25, 40};

    /**
     * {@code {352, 910, 2920}} -- and derived rather than typed, so the derivation cannot go stale.
     * The test asserts both the literals and the derivation, so moving either alone is caught.
     */
    private static final int[] BASE_POINTS = derive(BASE_LEVELS);

    /**
     * One bookshelf is one power is one percent off, and thirty is the ceiling.
     *
     * <p>THE one definition. {@code BookshelfPower} caps its count here so the readout cannot print
     * 32/30, and {@link #clampPower} clamps here so the function stays total for any caller. Two
     * uses, not two constants -- the predecessor project had a scan capped in one file against a
     * maximum declared in another, and its comment still said 20 while the constant said 30.
     */
    public static final int MAX_POWER = 30;

    /**
     * The power actually applied, which is also the percentage the readout prints.
     *
     * <p>Public so the number shown and the number charged come out of one expression. The same
     * reasoning that makes {@code Unbreaking.consumeChance} public: if the printed value and the
     * applied value came from two expressions, the boot gate would be checking one against itself.
     */
    public static int clampPower(int power) {
        return Math.max(0, Math.min(MAX_POWER, power));
    }

    /**
     * What it costs to take an enchant TO {@code targetLevel}, at this much bookshelf power.
     *
     * <p>Each rung is priced on its own -- there is no bundle rate -- so a player who stops at I has
     * not overpaid for it.
     *
     * <p><b>{@code targetLevel} throws where {@code bookshelfPower} clamps</b>, and the asymmetry is
     * the reachable-surface rule. Power arrives from a scan of the world: it is data, it fails safe
     * (never negative, never free), and clamping is the answer. A target level outside 1..3 cannot
     * be produced by any click -- UNLOCK asks for 1, and LEVEL_UP only fires below the cap -- so one
     * is a programming error and gets refused loudly, exactly as {@code EnchantMenuLayout.rawSlotFor}
     * refuses a slot outside its grid.
     *
     * @throws IllegalArgumentException if {@code targetLevel} is not a level an enchant can reach.
     */
    public static int xpPoints(int targetLevel, int bookshelfPower) {
        if (targetLevel < 1 || targetLevel > BASE_POINTS.length) {
            throw new IllegalArgumentException("no price for enchant level " + targetLevel
                    + "; levels are 1.." + BASE_POINTS.length);
        }
        // Integer throughout. The numerator is never negative -- power clamps at 30, so the factor is
        // at least 70 -- which makes this division a floor rather than a truncation-towards-zero, and
        // the floor is in the player's favour.
        return BASE_POINTS[targetLevel - 1] * (100 - clampPower(bookshelfPower)) / 100;
    }

    /** The highest level anything can be priced for. Held equal to the model's own cap by a test. */
    public static int maxPricedLevel() {
        return BASE_POINTS.length;
    }

    /** Exposed for the test, so the derivation is asserted rather than described. */
    static int basePoints(int targetLevel) {
        return BASE_POINTS[targetLevel - 1];
    }

    private static int[] derive(int[] levels) {
        int[] points = new int[levels.length];
        for (int i = 0; i < levels.length; i++) points[i] = XpCurve.totalForLevel(levels[i]);
        return points;
    }
}
