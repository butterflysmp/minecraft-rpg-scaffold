package io.github.butterflysmp.rpg.core.xp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vanilla's XP curve, pinned in both directions.
 *
 * Every case here guards a real, shippable bug: a price derived from the wrong total, a purchase
 * that silently loses a point to float truncation, a band boundary one level out so every level in
 * a range is wrong, or an overflow that turns an enormous wallet negative and makes everything free.
 *
 * The two assertions that carry the most weight are properties rather than examples.
 * {@code theBarSizeIsExactlyTheGapBetweenTwoTotals} pins BOTH piecewise functions against each other
 * at EVERY band boundary at once -- it is the only thing standing between this class and a
 * plausible-looking curve that is wrong in the middle of a range. And
 * {@code aTotalSurvivesTheRoundTripThroughLevelAndBar} is what says a purchase cannot leak points.
 */
class XpCurveTest {

    @Test
    void theCumulativeTotalMatchesVanillaAtEveryBandAndBoundary() {
        // Read off vanilla, not off this implementation. 352, 910 and 2920 are additionally the three
        // numbers EnchantCost derives its prices from, so a drift here silently re-prices the table.
        assertEquals(0, XpCurve.totalForLevel(0));
        assertEquals(7, XpCurve.totalForLevel(1));
        assertEquals(315, XpCurve.totalForLevel(15));
        assertEquals(352, XpCurve.totalForLevel(16), "level 16 is the unlock price");
        assertEquals(394, XpCurve.totalForLevel(17), "first level of the middle band");
        assertEquals(910, XpCurve.totalForLevel(25), "level 25 is the II price");
        assertEquals(1395, XpCurve.totalForLevel(30));
        assertEquals(1507, XpCurve.totalForLevel(31), "last level of the middle band");
        assertEquals(1628, XpCurve.totalForLevel(32), "first level of the top band");
        assertEquals(2920, XpCurve.totalForLevel(40), "level 40 is the III price");
        assertEquals(4267, XpCurve.totalForLevel(46));
        // BOTH mutations predicted for this test were WRONG, and the runs are recorded rather than
        // the predictions, because a comment claiming a guard the code does not have is worse than
        // no comment at all.
        //
        // Predicted: cumulative band l <= 16 -> l <= 15 reddens here. IT DOES NOT. Nor does 14, nor
        // 29, nor 30 on the second band. Vanilla's parabolas intersect at CONSECUTIVE INTEGER PAIRS
        // -- 15 and 16, 30 and 31 -- so any branch inside {14,15,16} or {29,30,31} is the identical
        // function. Measured: 13, 17, 28 and 32 each redden (l <= 17 gives "first level of the
        // middle band ==> expected: <394> but was: <391>"). A one-off here is not a bug.
        //
        // Predicted: rewriting the middle band as (long)(2.5*l*l - 40.5*l + 360) loses the cancelling
        // half at odd levels. IT DOES NOT -- all 11 stayed green. 2.5 and 40.5 are exact binary
        // fractions and these magnitudes sit far inside a double's mantissa, so nothing rounds. The
        // integer form is a no-doubles-in-this-class choice, not a correctness fix. The place a
        // double genuinely bites is EnchantCost's discount, where it is pinned deliberately.
    }

    @Test
    void theBarSizeMatchesVanillaAtEveryBandAndBoundary() {
        // The bands here are 15/16 and 30/31 -- deliberately NOT the 16/17 and 31/32 above.
        assertEquals(7, XpCurve.pointsToNextLevel(0));
        assertEquals(37, XpCurve.pointsToNextLevel(15), "last level of the low band");
        assertEquals(42, XpCurve.pointsToNextLevel(16), "first level of the middle band");
        assertEquals(112, XpCurve.pointsToNextLevel(30), "last level of the middle band");
        assertEquals(121, XpCurve.pointsToNextLevel(31), "first level of the top band");
        // Mutation, RUN: bar band l <= 15 -> l <= 16 -> 5 red, "first level of the middle band ==>
        // expected: <42> but was: <39>", plus the consistency property and the round trip. The bar
        // bands intersect at a SINGLE point each (15, 30) where the cumulative bands intersect at a
        // pair, which is exactly why a one-off is caught here and is not caught above.
        // Mutation, RUN: bar band l <= 30 -> l <= 31 -> 3 red, expected <121> but was <117>.
    }

    @Test
    void theBarSizeIsExactlyTheGapBetweenTwoTotals() {
        // THE LOAD-BEARING PROPERTY. Two piecewise functions with DIFFERENT band boundaries have to
        // agree at every single level, and no set of hand-picked examples can promise that. Move
        // either boundary by one and this reddens at exactly the level that moved.
        int checked = 0;
        for (int level = 0; level <= 5000; level++) {
            assertEquals(XpCurve.totalForLevel(level + 1) - XpCurve.totalForLevel(level),
                    XpCurve.pointsToNextLevel(level),
                    "the bar at level " + level + " must be the gap to the next total");
            checked++;
        }
        // A loop that ran zero times is a green test that checked nothing -- said out loud rather
        // than trusted, because a bad bound reports success having executed nothing.
        assertEquals(5001, checked, "the property has to have actually run");
    }

    @Test
    void theTotalOnlyEverGoesUp() {
        // A curve that dips would let a player LOSE wallet by gaining a level. Cheap to assert, and
        // it catches a sign error the anchors above can miss between them.
        int previous = -1;
        for (int level = 0; level <= 5000; level++) {
            int total = XpCurve.totalForLevel(level);
            assertTrue(total > previous, "level " + level + " must bank more than the level below it");
            previous = total;
        }
    }

    @Test
    void aWalletIsTheLevelsBankPlusHoweverFarIntoTheBarTheyAre() {
        assertEquals(352, XpCurve.totalPoints(16, 0.0), "an empty bar is exactly the level's bank");
        assertEquals(352 + 21, XpCurve.totalPoints(16, 0.5), "half of level 16's 42-point bar");
        assertEquals(394, XpCurve.totalPoints(16, 1.0), "a full bar is the next level's bank");
        assertEquals(0, XpCurve.totalPoints(0, 0.0));
        // Mutation, RUN: drop the bar term from totalPoints -> 4 red, "half of level 16's 42-point bar ==>
        // expected: <373> but was: <352>".
    }

    @Test
    void aCorruptLevelOrBarReadsAsASmallerWalletRatherThanALargerOne() {
        // Both fail towards refusing a purchase. A wallet that read HIGHER than it is would let a
        // player buy something they cannot afford and land the deduction below zero.
        assertEquals(0, XpCurve.totalPoints(-5, 0.0), "a negative level is no wallet at all");
        assertEquals(352, XpCurve.totalPoints(16, -1.0), "a negative bar does not subtract");
        assertEquals(394, XpCurve.totalPoints(16, 5.0), "an over-full bar does not add");
        // Mutation, RUN: drop the progress clamp -> 1 red, "a negative bar does not subtract ==> expected:
        // <352> but was: <310>" -- a corrupt bar SUBTRACTING from a wallet.
        // Mutation, RUN: drop the LOWER half of the level clamp -> 1 red, "a negative level is no wallet at
        // all ==> expected: <0> but was: <-5>". The two halves of that clamp bound different ends and
        // are not redundant: the upper half is pinned by the overflow test below.
    }

    @Test
    void anAbsurdLevelStaysPositiveInsteadOfOverflowingIntoAFreeShop() {
        // /xp set @s 2000000000 levels is a command an operator can type, and Player.getLevel() is an
        // int. Unclamped, 9 * l * l overflows and the wallet goes NEGATIVE -- at which point every
        // price is affordable and every deduction adds. The clamp is what makes that unreachable.
        assertTrue(XpCurve.totalForLevel(XpCurve.MAX_LEVEL) > 0);
        assertEquals(XpCurve.totalForLevel(XpCurve.MAX_LEVEL), XpCurve.totalForLevel(Integer.MAX_VALUE),
                "past the cap the curve stops moving rather than wrapping");
        assertTrue(XpCurve.totalPoints(Integer.MAX_VALUE, 1.0) > 0, "and the wallet stays positive");
        // Mutation, RUN: compute totalForLevel in int rather than long -> 2 red. 9 * l * l overflows long
        // before the result would, so totalForLevel(MAX_LEVEL) goes negative and the round-trip at
        // level 20000 collapses: "expected: <20000> but was: <0>".
        // Mutation, RUN: drop the MAX_LEVEL cap -> 1 red, "past the cap the curve stops moving rather
        // than wrapping ==> expected: <2147407943> but was: <1073744211>".
    }

    @Test
    void aTotalLandsInTheLevelWhoseBankItHasClearedAndNoFurther() {
        assertEquals(0, XpCurve.levelFor(0));
        assertEquals(0, XpCurve.levelFor(6), "one point short of level 1");
        assertEquals(1, XpCurve.levelFor(7), "exactly level 1's bank IS level 1");
        assertEquals(15, XpCurve.levelFor(351), "one point short of 16");
        assertEquals(16, XpCurve.levelFor(352));
        assertEquals(40, XpCurve.levelFor(2920));
        assertEquals(0, XpCurve.levelFor(-100), "a negative total is not a negative level");
        // Mutation, RUN: <= -> < in the loop condition -> 3 red, "exactly level 1's bank IS level 1 ==>
        // expected: <1> but was: <0>". This is the
        // exact boundary boot rows 4 to 6 land on, where a purchase must leave the player at level 0.
    }

    @Test
    void aTotalSurvivesTheRoundTripThroughLevelAndBar() {
        // THE PROPERTY THAT SAYS A PURCHASE CANNOT LEAK POINTS. The seam writes a remaining total out
        // as (level, bar) and any later read turns it back into a total; if those two disagree by one
        // the player quietly bleeds XP every time they buy something.
        int checked = 0;
        for (int total = 0; total <= 10000; total++) {
            int level = XpCurve.levelFor(total);
            assertTrue(XpCurve.totalForLevel(level) <= total, "the level is not past the total");
            assertTrue(total < XpCurve.totalForLevel(level + 1), "the level is not short of the total");
            assertEquals(total, XpCurve.totalPoints(level, XpCurve.progressFor(total)),
                    "total " + total + " must survive being written out and read back");
            checked++;
        }
        assertEquals(10001, checked, "the property has to have actually run");
        // Mutation, RUN: Math.round -> (long) truncation in totalPoints -> 2 red, "total 23 must survive
        // being written out and read back ==> expected: <23> but was: <22>", and "level 100 plus 1 of
        // 742 ==> expected: <30971> but was: <30970>". A point per purchase, silently.
    }

    @Test
    void theRoundTripAlsoHoldsWhereTheBarIsBigEnoughForFloatToMatter() {
        // The loop above tops out at level 46, where a bar is 256 points. A float carries about seven
        // digits, so the interesting case is a bar of tens of thousands -- reachable by an operator's
        // /xp set, and the case where a truncating read would miss by more than a point.
        int checked = 0;
        for (int level : new int[] {100, 1000, 5000, 20000}) {
            int base = XpCurve.totalForLevel(level);
            int bar = XpCurve.pointsToNextLevel(level);
            for (int into : new int[] {0, 1, bar / 2, bar - 1}) {
                int total = base + into;
                assertEquals(level, XpCurve.levelFor(total));
                assertEquals(total, XpCurve.totalPoints(level, XpCurve.progressFor(total)),
                        "level " + level + " plus " + into + " of " + bar);
                checked++;
            }
        }
        assertEquals(16, checked, "the property has to have actually run");
    }

    @Test
    void theBarFractionIsAlwaysSomethingSetExpWillAccept() {
        // Player.setExp THROWS outside [0,1], and a throw on this path is a crash mid-purchase.
        int checked = 0;
        for (int total = 0; total <= 10000; total++) {
            float progress = XpCurve.progressFor(total);
            assertTrue(progress >= 0.0f && progress <= 1.0f, "progress at " + total + " was " + progress);
            checked++;
        }
        assertEquals(10001, checked, "the property has to have actually run");
        assertEquals(0.0f, XpCurve.progressFor(-1), "a negative total is an empty bar, not a throw");
    }
}
