package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.xp.XpCurve;

/**
 * What a score transfer costs, in XP POINTS, keyed on the rarity of the item BEING UPGRADED.
 *
 * <h2>*** THIS IS THE FIRST CONSUMER OF {@link Rarity} AS A COST AXIS ***</h2>
 *
 * <b>Stated so the next one finds it rather than inventing a second shape.</b> Until now rarity
 * drove exactly two things -- the item name's colour and the tooltip's footer -- and nothing
 * mechanical. {@code EnchantRoll.candidateCount} records the other pending consumer and says
 * rarity-weighting there is <i>"deferred BY CHOICE rather than by impossibility"</i>. When that
 * choice is finally made, <b>this table is the precedent it inherits</b>: authored in levels,
 * flat per tier, converted through the curve.
 *
 * <p><b>It does NOT contradict {@code GearScore}'s ruling, and the distinction is worth keeping
 * straight.</b> That class says <i>"RARITY DOES NOT ENTER ANY OF THIS ... adding one would be the
 * change that has to be argued for"</i> -- and it is about POWER: an EXOTIC may roll low and a
 * COMMON high, because rarity and power are independent axes. <b>Nothing here touches power.</b>
 * This prices an operation, which is the axis {@code EnchantCost} already occupies, and
 * {@code GearScore} still has no {@link Rarity} parameter anywhere in it.
 *
 * <h2>FLAT PER TIER, IGNORING THE SIZE OF THE JUMP</h2>
 *
 * Ben's ruling. Taking a RARE from 100 to 400 costs what taking it from 100 to 101 costs. <b>The
 * alternative -- pricing the delta -- makes a player hoard donors to buy one big jump instead of
 * several small ones</b>, and turns the screen into arithmetic homework. Flat also means the number
 * on screen is stable while the player swaps donors in and out, which is what makes the readout
 * worth reading.
 *
 * <h2>AUTHORED IN LEVELS, CHARGED AND DISPLAYED IN POINTS</h2>
 *
 * Exactly {@code EnchantCost}'s split, and its javadoc carries the full argument: levels are not
 * linear, so a percentage of a LEVEL count is a different percentage at every rung. <b>Levels are
 * the INPUT to the price and never the price itself</b> -- they describe the bank of a player
 * starting from zero, which is the only point on the curve where "40 levels" is an unambiguous
 * amount of money. <b>The level figure must never be printed anywhere</b>; the screen says
 * {@code 2920 XP}.
 */
public final class AnvilCost {

    private AnvilCost() {}

    /**
     * The tuning knob, in the units a designer thinks in.
     *
     * <h2>AN EXHAUSTIVE SWITCH RATHER THAN AN ARRAY INDEXED BY {@code ordinal()}</h2>
     *
     * <p>{@code EnchantCost} indexes an array, and it is right to: its key is an enchant LEVEL, a
     * number with no enum behind it. Ours is an enum, so <b>a seventh tier must be a compile error
     * here</b> rather than an {@code ArrayIndexOutOfBoundsException} on the first EXOTIC-plus item
     * a player brings to the screen. {@link Rarity}'s own javadoc says its ordering is load-bearing
     * and that a new tier would be a compile error at every mapping site -- this is one of those
     * sites, and it only is one because of the switch.
     *
     * <p>Exposed package-private for the test, the way {@code EnchantCost.basePoints} is, <b>so the
     * derivation is asserted rather than described</b>.
     */
    static int levelFor(Rarity rarity) {
        if (rarity == null) {
            throw new IllegalArgumentException(
                    "no rarity: every GearDefinition carries one, so a null here is a programming"
                            + " error rather than bad data");
        }
        return switch (rarity) {
            case COMMON -> 10;
            case UNCOMMON -> 16;
            case RARE -> 25;
            case EPIC -> 40;
            case LEGENDARY -> 60;
            case EXOTIC -> 90;
        };
    }

    /**
     * What a transfer into an item of this rarity costs, in XP points.
     *
     * <p>Converted through {@link XpCurve#totalForLevel} exactly as {@code EnchantCost.derive} does
     * -- <b>derived at every call rather than typed into a second table</b>, so the two cannot
     * drift. {@code AnvilCostTest} asserts the six points as literals AND asserts that the
     * derivation produces them, which is what stops either half moving alone.
     *
     * <p><b>The six points are all distinct</b> (160 / 352 / 910 / 2920 / 8670 / 24045), so no
     * transposition between two tiers can hide behind two readings that happen to be equal. Three
     * of them -- 352, 910 and 2920 -- are the same figures {@code EnchantCost.BASE_POINTS} ships,
     * because 16, 25 and 40 are levels both tables author. That overlap is a coincidence of the
     * curve and <b>not</b> a shared constant; neither table reads the other.
     */
    public static int xpPoints(Rarity rarity) {
        return XpCurve.totalForLevel(levelFor(rarity));
    }
}
