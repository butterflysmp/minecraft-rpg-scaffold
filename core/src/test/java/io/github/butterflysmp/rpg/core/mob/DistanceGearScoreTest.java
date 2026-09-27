package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three curves (M3, M11, M12), the circle (M19) and the cap (M20).
 *
 * <p>Every expected value below was produced by EXECUTING the expression on 2026-09-27, not by
 * working it out by hand (memory: never predict floating point). The "not N" figures in the cap rows
 * are what the same curve returns with the clamp removed; they are why each row can tell a missing
 * clamp from a working one.
 *
 * <p><b>The End's origin is NOT tested here.</b> This function only ever sees a distance; the (0, 0)
 * origin (M23) is paper's {@code MobOrigin}, and its witness is gate row G7.
 */
class DistanceGearScoreTest {

    // --- the three curves at the ruled points --------------------------------------------------

    @Test
    void eachWorldStartsAtItsRuledValue() {
        assertEquals(100, DistanceGearScore.of(MobDimension.OVERWORLD, 0));
        assertEquals(200, DistanceGearScore.of(MobDimension.NETHER, 0), "M11. Mutation: 200 -> 100 reddens");
        assertEquals(300, DistanceGearScore.of(MobDimension.END, 0), "M12. Mutation: 300 -> 100 reddens");
    }

    @Test
    void every2500BlocksIsPlus100OnTheNormalRate() {
        assertEquals(200, DistanceGearScore.of(MobDimension.OVERWORLD, 2_500));
        assertEquals(400, DistanceGearScore.of(MobDimension.END, 2_500));
    }

    @Test
    void theNetherGrowsEightTimesFaster() {
        // 500 blocks is below the cap, so this is the row that proves the 8x rate; the 2,500-block
        // row below is capped and cannot. Mutation: rate 8 -> 1 gives 220 -> reddens.
        assertEquals(360, DistanceGearScore.of(MobDimension.NETHER, 500));
    }

    // --- the cap, on both sides, in every world (M20) -----------------------------------------

    @Test
    void theOverworldCapsAt500From10000Blocks() {
        assertEquals(499, DistanceGearScore.of(MobDimension.OVERWORLD, 9_975));
        assertEquals(500, DistanceGearScore.of(MobDimension.OVERWORLD, 10_000));
        assertEquals(500, DistanceGearScore.of(MobDimension.OVERWORLD, 12_000),
                "M20: 500, NOT 580 -- the uncapped curve's value, and gate row G19's");
    }

    @Test
    void theEndCapsAt500From5000Blocks() {
        assertEquals(499, DistanceGearScore.of(MobDimension.END, 4_975));
        assertEquals(500, DistanceGearScore.of(MobDimension.END, 5_000));
        assertEquals(500, DistanceGearScore.of(MobDimension.END, 6_000), "500, not 540");
    }

    @Test
    void theNetherCapsAt500From937AndAHalfBlocks() {
        assertEquals(488, DistanceGearScore.of(MobDimension.NETHER, 900));
        assertEquals(500, DistanceGearScore.of(MobDimension.NETHER, 937.5));
        assertEquals(500, DistanceGearScore.of(MobDimension.NETHER, 2_500),
                "500, NOT 1,000 -- the uncapped Nether at 2,500 blocks");
    }

    @Test
    void theCapHoldsAtTheWorldBorder() {
        assertEquals(500, DistanceGearScore.of(MobDimension.OVERWORLD, 30_000_000));
        assertEquals(500, DistanceGearScore.of(MobDimension.NETHER, 30_000_000));
    }

    // --- rounding (the seat's default) ---------------------------------------------------------

    @Test
    void aHalfRoundsUp() {
        // 12.5 / 25 = 0.5 exactly, so 100.5 -> 101. Mutation: round -> floor gives 100 -> reddens,
        // and this is the only row that can see it.
        assertEquals(101, DistanceGearScore.of(MobDimension.OVERWORLD, 12.5));
        assertEquals(100, DistanceGearScore.of(MobDimension.OVERWORLD, 12.4));
    }

    // --- fail soft -----------------------------------------------------------------------------

    @Test
    void aNanOrNegativeDistanceFallsBackToTheCurvesStartNeverToZero() {
        // Math.round(NaN) is 0: without the guard this would seed a GS 0 mob.
        assertEquals(100, DistanceGearScore.of(MobDimension.OVERWORLD, Double.NaN));
        assertEquals(200, DistanceGearScore.of(MobDimension.NETHER, -5));
    }

    // --- the circle (M19) ----------------------------------------------------------------------

    @Test
    void distanceIsHorizontalAndEuclidean() {
        assertEquals(5.0, DistanceGearScore.horizontalDistance(3, 4, 0, 0), 1e-12, "a circle, not max(|x|,|z|) = 4");
        assertEquals(5.0, DistanceGearScore.horizontalDistance(103, -46, 100, -50), 1e-12, "measured from the origin");
    }

    // --- the seam and the stored-value guard ---------------------------------------------------

    @Test
    void theDistanceSourceIsTheCurve() {
        assertEquals(DistanceGearScore.of(MobDimension.END, 1_234),
                GearScoreSource.DISTANCE.gearScoreFor(MobDimension.END, 1_234));
    }

    @Test
    void aStoredScoreIsTrustedOnlyInsideOneToTheCap() {
        assertTrue(MobGearScore.isValidStored(1));
        assertTrue(MobGearScore.isValidStored(500));
        assertFalse(MobGearScore.isValidStored(0), "gate row G15's value");
        assertFalse(MobGearScore.isValidStored(501));
        assertFalse(MobGearScore.isValidStored(900), "gate row G15b's value");
    }
}
