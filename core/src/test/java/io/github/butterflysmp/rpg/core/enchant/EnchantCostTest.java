package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.xp.XpCurve;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The price of an enchant, pinned across the whole grid rather than at a sample.
 *
 * THE GRID IS THE POINT. A discount written with doubles instead of integers is wrong in exactly
 * ONE of the twelve level-and-power cells asserted here, and a different one depending on which
 * double form is used. Assert one power, or one level, and a reimplementation ships green while
 * charging a point more or less than the menu printed beside it. The two forms and where each
 * breaks are recorded on the tests that catch them.
 *
 * Every case here is a guard whose absence is a real, shippable bug: a price that does not match the
 * number on the cell, a discount that rounds against the player, a power that clamps the wrong way
 * and pays the player instead of charging them, or a fourth enchant level shipping with no price.
 */
class EnchantCostTest {

    @Test
    void thePricesAreTheBanksOfLevelSixteenTwentyFiveAndForty() {
        // The derivation, executable. 352/910/2920 are asserted as literals AND as the curve's own
        // answer, so moving either one alone is caught -- a re-tuned BASE_LEVELS that forgot to move
        // the literals, or a curve that drifted while the literals stayed.
        assertEquals(352, EnchantCost.basePoints(1));
        assertEquals(910, EnchantCost.basePoints(2));
        assertEquals(2920, EnchantCost.basePoints(3));
        assertEquals(XpCurve.totalForLevel(16), EnchantCost.basePoints(1));
        assertEquals(XpCurve.totalForLevel(25), EnchantCost.basePoints(2));
        assertEquals(XpCurve.totalForLevel(40), EnchantCost.basePoints(3));
    }

    @Test
    void aFourthEnchantLevelCannotShipWithoutAPrice() {
        // EnchantState decides how far an enchant can go; this decides what each step costs. If the
        // model grows a level and the price table does not, xpPoints throws on a click that the menu
        // has already told the player is available -- so the two are pinned together here rather
        // than discovered in front of someone.
        assertEquals(EnchantState.MAX_LEVEL, EnchantCost.maxPricedLevel(),
                "every level the model allows must have a price");
    }

    @Test
    void withNoShelvesEachRungCostsItsFullBank() {
        assertEquals(352, EnchantCost.xpPoints(1, 0));
        assertEquals(910, EnchantCost.xpPoints(2, 0));
        assertEquals(2920, EnchantCost.xpPoints(3, 0));
        // 4182 to take one enchant all the way, which is what boot row 12 spends.
        assertEquals(4182, EnchantCost.xpPoints(1, 0) + EnchantCost.xpPoints(2, 0)
                + EnchantCost.xpPoints(3, 0));
    }

    @Test
    void aFullRingTakesExactlyThirtyPercentOffEveryRung() {
        assertEquals(246, EnchantCost.xpPoints(1, 30));
        assertEquals(637, EnchantCost.xpPoints(2, 30));
        assertEquals(2044, EnchantCost.xpPoints(3, 30), "the cell that catches a double discount");
        assertEquals(2927, EnchantCost.xpPoints(1, 30) + EnchantCost.xpPoints(2, 30)
                + EnchantCost.xpPoints(3, 30));

        // Exactly 70%, with no rounding slack at all -- 2044 * 100 == 2920 * 70. This is the whole
        // reason the cost model is in points: pricing the LEVEL count instead would have charged
        // XpCurve.totalForLevel(28) == 1186 here, which is 59% off, not 30%.
        assertEquals(2920 * 70, 2044 * 100, "III at full power is 70% of III at none");
        assertEquals(1186, XpCurve.totalForLevel(28), "what the discarded level model would have charged");

        // Mutation, RUN: rewrite as (int)(base * (1 - p/100.0)) -> 1 red, and ONLY here:
        // "the cell that catches a double discount ==> expected: <2044> but was: <2043>".
        // I and II stay green at this power and that is CORRECT, not a missed mutation --
        // 1 - 30/100.0 is the same double as 0.7, and 910 * 0.7 lands inside half a ULP of 637 so
        // the multiply rounds back up, while 2920 * 0.7 does not. One wrong cell out of nine.
    }

    @Test
    void theDiscountFloorsSoItNeverRoundsAgainstThePlayer() {
        // ONE shelf. 910 * 0.99 is 900.9, and the player pays 900.
        assertEquals(348, EnchantCost.xpPoints(1, 1));
        assertEquals(900, EnchantCost.xpPoints(2, 1), "900.9 floors to 900, it does not round to 901");
        assertEquals(2890, EnchantCost.xpPoints(3, 1));

        // Mutation, RUN: rewrite as Math.round(base * (1 - p/100.0)) -> 2 red, and NEITHER of them
        // at power 30: "900.9 floors to 900, it does not round to 901 ==> expected: <900> but was:
        // <901>", and ten shelves at I "expected: <316> but was: <317>". The (int) form breaks only
        // at power 30 and this one only away from it, so neither power catches both.
    }

    @Test
    void tenShelvesTakeATenthOffEveryRung() {
        // A middle of the range, so the grid is not just its two ends.
        assertEquals(316, EnchantCost.xpPoints(1, 10));
        assertEquals(819, EnchantCost.xpPoints(2, 10));
        assertEquals(2628, EnchantCost.xpPoints(3, 10));
    }

    @Test
    void thePriceIsNeverMoreThanTheExactDiscountAndNeverAWholePointLess() {
        // The floor property, stated independently of the expression that implements it: the charge
        // sits in [exact - 1, exact]. A rounding-to-nearest implementation breaks the upper bound;
        // a double that drifts low breaks the lower one.
        int checked = 0;
        for (int level = 1; level <= EnchantCost.maxPricedLevel(); level++) {
            for (int power = 0; power <= EnchantCost.MAX_POWER; power++) {
                double exact = EnchantCost.basePoints(level) * (1.0 - power / 100.0);
                int charged = EnchantCost.xpPoints(level, power);
                assertTrue(charged <= Math.ceil(exact), "level " + level + " at power " + power
                        + " charged " + charged + " against an exact " + exact);
                assertTrue(charged > exact - 1.0, "level " + level + " at power " + power
                        + " charged " + charged + ", more than a point under " + exact);
                checked++;
            }
        }
        assertEquals(93, checked, "the property has to have actually run");
    }

    @Test
    void moreShelvesNeverCostMore() {
        int checked = 0;
        for (int level = 1; level <= EnchantCost.maxPricedLevel(); level++) {
            int previous = Integer.MAX_VALUE;
            for (int power = 0; power <= EnchantCost.MAX_POWER; power++) {
                int charged = EnchantCost.xpPoints(level, power);
                assertTrue(charged <= previous, "power " + power + " cost more than power " + (power - 1));
                previous = charged;
                checked++;
            }
        }
        assertEquals(93, checked, "the property has to have actually run");
    }

    @Test
    void aPowerOutsideTheRangeClampsRatherThanPayingThePlayer() {
        // THE ONE THAT MATTERS MOST. Unclamped, a power of 200 gives a factor of -100 and the price
        // goes NEGATIVE -- at which point the affordability check passes trivially and the deduction
        // ADDS to the wallet. A bookshelf scan is data from the world, so it fails safe in both
        // directions rather than being trusted.
        assertEquals(EnchantCost.xpPoints(3, 0), EnchantCost.xpPoints(3, -5), "below zero is no discount");
        assertEquals(EnchantCost.xpPoints(3, 0), EnchantCost.xpPoints(3, Integer.MIN_VALUE));
        assertEquals(EnchantCost.xpPoints(3, 30), EnchantCost.xpPoints(3, 200), "past the cap is the cap");
        assertEquals(EnchantCost.xpPoints(3, 30), EnchantCost.xpPoints(3, Integer.MAX_VALUE));
        assertEquals(0, EnchantCost.clampPower(-1));
        assertEquals(30, EnchantCost.clampPower(31));
        // Mutation, RUN: drop the upper clamp -> 2 red, "past the cap is the cap ==> expected: <2044>
        // but was: <-2920>". A NEGATIVE price -- the check passes trivially and the deduction pays.
        // Mutation, RUN: drop the lower clamp -> 1 red, "below zero is no discount ==> expected:
        // <2920> but was: <3066>". The two halves bound different ends and are not redundant.
    }

    @Test
    void noDiscountEverMakesAnEnchantFree() {
        // The floor of the whole domain is 246, at I with a full ring. A price of zero would make the
        // affordability check vacuous and the table a button rather than a cost.
        int cheapest = Integer.MAX_VALUE;
        for (int level = 1; level <= EnchantCost.maxPricedLevel(); level++) {
            for (int power = -50; power <= 200; power++) {
                cheapest = Math.min(cheapest, EnchantCost.xpPoints(level, power));
            }
        }
        assertEquals(246, cheapest, "nothing anywhere in the domain is cheaper than this");
        assertTrue(cheapest > 0);
    }

    @Test
    void aLevelNothingCanReachHasNoPriceAndSaysSo() {
        // Refused loudly rather than clamped: no click can ask for these, so one arriving is a bug in
        // the caller and silently pricing it as I or III would hide that.
        assertThrows(IllegalArgumentException.class, () -> EnchantCost.xpPoints(0, 0));
        assertThrows(IllegalArgumentException.class, () -> EnchantCost.xpPoints(4, 0));
        assertThrows(IllegalArgumentException.class, () -> EnchantCost.xpPoints(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> EnchantCost.xpPoints(Integer.MAX_VALUE, 30));
        // Mutation, RUN: BASE_POINTS[targetLevel] instead of - 1 -> 5 failures and 3 ERRORS. Every rung
        // is priced one too high ("expected: <352> but was: <910>") and III walks off the end of the
        // array. It reddens in the grid tests above rather than here, which is the point of both.
    }
}
