package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M25 (Ben, 2026-09-28): the overworld is GS 100 and the Nether GS 200, with no distance term; the End is
 * 300 within 1,000 blocks of (0, 0), then +100 per 10,000 blocks. The End curve being continuous, rounded
 * and clamped at 500 is the seat's default, not a ruling.
 *
 * <p>The distances are chosen so no two readings collide: 6,000 and 11,000 give 350 and 400, which a
 * missing offset (360, 410) or the old /25 rate (500, 500) cannot reproduce.
 */
class DimensionGearScoreTest {

    // --- the overworld and the Nether: flat -----------------------------------------------------------

    @Test
    void theOverworldIsOneHundredAtEveryDistance() {
        assertEquals(100, DimensionGearScore.of(MobDimension.OVERWORLD, 0));
        assertEquals(100, DimensionGearScore.of(MobDimension.OVERWORLD, 100_000),
                "M25: no distance term. Mutation: a distance term back in the overworld -> reddens");
        assertEquals(100, DimensionGearScore.of(MobDimension.OVERWORLD, 30_000_000), "the world border");
    }

    @Test
    void theNetherIsTwoHundredAtEveryDistance() {
        assertEquals(200, DimensionGearScore.of(MobDimension.NETHER, 0));
        assertEquals(200, DimensionGearScore.of(MobDimension.NETHER, 100_000), "M25: no distance term");
    }

    // --- the End: flat to 1,000, then +100 per 10,000, capped ---------------------------------------

    @Test
    void theEndIsThreeHundredUpToTheOneThousandBlockEdge() {
        assertEquals(300, DimensionGearScore.of(MobDimension.END, 0));
        assertEquals(300, DimensionGearScore.of(MobDimension.END, 500));
        assertEquals(300, DimensionGearScore.of(MobDimension.END, 1_000),
                "the edge. Mutation: drop the 1,000 offset -> 310 -> reddens");
    }

    @Test
    void theEndClimbsOneHundredPerTenThousandBlocksPastTheEdge() {
        // The seat's worked points.
        assertEquals(350, DimensionGearScore.of(MobDimension.END, 6_000),
                "Mutation: /25 in the End -> 500; the offset dropped -> 360");
        assertEquals(400, DimensionGearScore.of(MobDimension.END, 11_000));
        assertEquals(500, DimensionGearScore.of(MobDimension.END, 21_000), "the cap, reached exactly");
    }

    @Test
    void theEndIsCappedAtFiveHundredBeyond() {
        assertEquals(500, DimensionGearScore.of(MobDimension.END, 21_001));
        assertEquals(500, DimensionGearScore.of(MobDimension.END, 50_000), "uncapped this would be 790");
        assertEquals(500, DimensionGearScore.of(MobDimension.END, 30_000_000));
    }

    @Test
    void theEndRoundsToAWholeNumber() {
        // 50 blocks past the edge is +0.5; Math.round takes a half up. 49 blocks is +0.49, which stays 300.
        assertEquals(301, DimensionGearScore.of(MobDimension.END, 1_050));
        assertEquals(300, DimensionGearScore.of(MobDimension.END, 1_049));
    }

    @Test
    void aNanOrNegativeDistanceFallsBackToTheStartNeverToZero() {
        // A GS 0 mob has zero health and is born dead, so a caller bug must never produce one.
        assertEquals(300, DimensionGearScore.of(MobDimension.END, Double.NaN));
        assertEquals(300, DimensionGearScore.of(MobDimension.END, -5));
        assertEquals(100, DimensionGearScore.of(MobDimension.OVERWORLD, Double.NaN));
    }

    // --- the circle (M19), still used by the End ----------------------------------------------------

    @Test
    void distanceIsHorizontalEuclidean() {
        assertEquals(5.0, DimensionGearScore.horizontalDistance(3, 4, 0, 0), 1e-12, "a 3-4-5 triangle");
        assertEquals(5.0, DimensionGearScore.horizontalDistance(103, -46, 100, -50), 1e-12, "from an origin");
    }

    @Test
    void theBlanketSourceIsThisCurve() {
        // M3's seam stays; BLANKET is its one implementation, and it is exactly the M25 curve.
        for (double d : new double[] {0, 1_000, 6_000, 21_000}) {
            for (MobDimension dim : MobDimension.values()) {
                assertEquals(DimensionGearScore.of(dim, d), GearScoreSource.BLANKET.gearScoreFor(dim, d), dim + " " + d);
            }
        }
    }
}
