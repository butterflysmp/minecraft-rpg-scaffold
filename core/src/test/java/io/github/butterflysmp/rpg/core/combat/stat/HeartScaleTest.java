package io.github.butterflysmp.rpg.core.combat.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two-tier heart scale is pure math, so it is pinned exactly here rather than boot-witnessed.
 * The values are the design's worked examples; the tier boundary at max=100 and the fractional fill
 * are the edges that a naive linear scale gets wrong. Each names the mutation it forces red.
 */
class HeartScaleTest {

    private static final double EPS = 1e-9;

    @Test
    void heartCountFollowsTheTwoTierScaleAtTheDesignExamples() {
        assertEquals(10, HeartScale.heartCount(100), "100 HP -> 10 hearts (first tier, 10 HP each)");
        assertEquals(12, HeartScale.heartCount(250), "250 -> 12 (10 + ceil(150/100))");
        assertEquals(13, HeartScale.heartCount(400), "400 -> 13 (10 + ceil(300/100))");
        assertEquals(19, HeartScale.heartCount(1000), "1000 -> 19 (10 + ceil(900/100))");
        // Mutation: a single linear HP-per-heart -> 400 gives 40 hearts, past the vanilla cap -> reddens.
    }

    @Test
    void theTierBoundaryIsAtExactlyOneHundred() {
        assertEquals(10, HeartScale.heartCount(100), "100 sits in the low tier: ceil(100/10) = 10");
        assertEquals(11, HeartScale.heartCount(101), "one HP over the boundary adds a high-tier heart");
        assertEquals(11, HeartScale.heartCount(200), "200 -> 10 + ceil(100/100) = 11");
        // Mutation: use < instead of <= at the boundary -> 100 takes the high-tier branch (still 10 here,
        // but 100.0 edge cases drift) -> the off-by-one shows at the boundary -> reddens.
    }

    @Test
    void smallAndZeroMaxDoNotUnderflow() {
        assertEquals(1, HeartScale.heartCount(5), "5 HP still shows a (partial) heart: ceil(0.5) = 1");
        assertEquals(1, HeartScale.heartCount(10), "10 -> 1 heart");
        assertEquals(0, HeartScale.heartCount(0), "no max -> no hearts, not a crash");
        // Mutation: drop the max<=0 guard -> ceil(0/10)=0 is fine, but a negative max yields negatives -> reddens.
    }

    @Test
    void fillIsAPercentageOfTheHeartCount() {
        assertEquals(6.5, HeartScale.filledHearts(200, 400), EPS, "200/400 -> 0.5 * 13 = 6.5 hearts filled");
        assertEquals(13.0, HeartScale.filledHearts(400, 400), EPS, "full -> every heart filled");
        assertEquals(3.25, HeartScale.filledHearts(100, 400), EPS, "100/400 -> 0.25 * 13 = 3.25");
        assertEquals(0.0, HeartScale.filledHearts(0, 400), EPS, "empty -> nothing filled");
        // Mutation: fill from HP directly (current/10) instead of percentage*count -> 200 -> 20 hearts -> reddens.
    }

    @Test
    void fillDividesByMaxSafelyAtZero() {
        assertEquals(0.0, HeartScale.filledHearts(0, 0), EPS, "zero max fills nothing rather than dividing by zero");
        // Mutation: drop the max<=0 guard in filledHearts -> 0/0 = NaN -> reddens.
    }

    // --- The inverse: vanilla health POINTS back into custom HP -----------------------------------

    @Test
    void aVanillaHealTranslatesPROPORTIONALLYAndNotOneForOne() {
        assertEquals(20.0, HeartScale.customFromHealthPoints(4, 100), EPS,
                "a 4-point potion is two hearts of a 10-heart bar -> 20% of a 100 max");
        assertEquals(5.0, HeartScale.customFromHealthPoints(1, 100), EPS, "half a heart of ten -> 5%");
        assertEquals(100.0, HeartScale.customFromHealthPoints(20, 100), EPS, "a full 10-heart bar -> the whole max");
        assertEquals(400.0, HeartScale.customFromHealthPoints(26, 400), EPS,
                "and a full 13-heart bar at max 400 -> 400, so the scale holds in the high tier");
        // The 4-point potion is worth 20 HP at max 100 and 61.53846153846154 at max 400 -- executed,
        // not derived. One-for-one would make every potion in the game worthless the moment Growth
        // raised someone's ceiling, which is the failure this arc exists to fix.
        // Mutation: return healthPoints unchanged (a 1:1 translation) -> the first row gives 4 -> reddens.
    }

    @Test
    void theInverseUsesPOINTSNotHEARTSSoItUndoesWhatTheRendererDID() {
        // HeartBarRenderer doubles filledHearts on the way out; this halves on the way in. The
        // round trip is the only thing that pins the factor of two, because a hearts-based inverse
        // is off by exactly 2x and otherwise looks entirely reasonable.
        assertRoundTrip(50, 100);
        assertRoundTrip(200, 400);
        assertRoundTrip(33, 100);
        assertRoundTrip(777, 1000);
        // Mutation: drop the * 2 from the divisor -> every round trip doubles -> reddens.
    }

    private static void assertRoundTrip(double current, double max) {
        double points = HeartScale.filledHearts(current, max) * 2.0;
        assertEquals(current, HeartScale.customFromHealthPoints(points, max), EPS,
                current + "/" + max + " out to " + points + " vanilla points and back");
    }

    @Test
    void theInverseRefusesANonPositiveMaxOrAmountRatherThanDividingOrHealingBACKWARDS() {
        assertEquals(0.0, HeartScale.customFromHealthPoints(4, 0), EPS,
                "no bar to translate onto, and heartCount(0) is 0 -- do not divide by it");
        assertEquals(0.0, HeartScale.customFromHealthPoints(4, -100), EPS, "nor a negative one");
        assertEquals(0.0, HeartScale.customFromHealthPoints(-4, 100), EPS,
                "a negative amount must not become a heal -- EntityRegainHealthEvent's amount is settable");
        assertEquals(0.0, HeartScale.customFromHealthPoints(0, 100), EPS, "and zero is zero");
        // Mutation: drop the healthPoints <= 0 guard -> the negative row returns -20 and a
        // rerouted heal starts damaging -> reddens.
    }
}
