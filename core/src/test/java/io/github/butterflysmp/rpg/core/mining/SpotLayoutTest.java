package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vein size's own refusals, and the spot layout's two rules: every 5 x 5 box lies wholly on
 * the patch (centre at least {@code EDGE_MARGIN_PX} = 2 from each edge), and no two boxes overlap
 * (centres at Chebyshev distance at least {@code SPOT_SPACING_PX} = 5). Both values are the seat's
 * rulings of 2026-11-13 (PLAN-mining.md section 2, ruling 6).
 *
 * <p><b>The sweep checks the rules with ITS OWN arithmetic</b>, not by calling the production
 * helpers, so a mutation in a helper cannot certify itself.
 */
class SpotLayoutTest {

    /** The Small vein, as ruled: 3 wide, 2 high, 2 to 3 spots (Ben, 2026-11-13: "yes"). */
    static final VeinSize SMALL = new VeinSize("small", 3, 2, 2, 3);

    // --- VeinSize -------------------------------------------------------------------------------

    @Test
    void theSmallVeinIs48By32PixelsAndGuaranteesTwentySpots() {
        assertEquals(48, SMALL.widthPx());
        assertEquals(32, SMALL.heightPx());
        // The WORST-CASE guarantee (the seat's F-A, 2026-11-13), not the perfect tiling (9 x 6 = 54,
        // which a random maximal packing can fall far short of). Centre area 44 x 28; each centre
        // blocks a 9 x 9 square of centres: ceil(44 / 9) x ceil(28 / 9) = 5 x 4.
        assertEquals(20, SMALL.guaranteedSpots());
    }

    @Test
    void theRefusedBoundIsTheWorstCaseGuaranteeOnBothSides() {
        assertEquals(20, new VeinSize("s", 3, 2, 1, 20).maxSpots(), "20 is accepted on a 3 x 2");
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 2, 1, 21));
        // 1 x 1: centre area 12 x 12, ceil(12 / 9) = 2 per axis.
        assertEquals(4, new VeinSize("s", 1, 1, 1, 4).guaranteedSpots());
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 1, 1, 1, 5));
        // A wide strip: 5 x 1 is 80 x 16 pixels, centre area 76 x 12: ceil(76/9) = 9, ceil(12/9) = 2.
        assertEquals(18, new VeinSize("s", 5, 1, 1, 18).guaranteedSpots());
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 5, 1, 1, 19));
        // Mutation: ceil -> floor (4 x 3 = 12 on the 3 x 2) -> "20 is accepted" reddens.
        // Mutation: the perfect-tiling count (54) -> 21 is accepted -> reddens.
    }

    @Test
    void aDenseVeinAtTheGuaranteedMaximumStillRollsAndObeysTheRules() {
        // The bound promises a free legal position exists; rejection sampling must still FIND it.
        // This probes that at the worst count the bound allows, on 300 fixed seeds.
        VeinSize dense = new VeinSize("dense", 3, 2, 20, 20);
        for (long seed = 0; seed < 300; seed++) {
            List<Spot> spots = SpotLayout.roll(dense, new SplittableRandom(seed)).spots();
            assertEquals(20, spots.size());
            for (int i = 0; i < spots.size(); i++) {
                for (int j = i + 1; j < spots.size(); j++) {
                    Spot a = spots.get(i), b = spots.get(j);
                    assertTrue(Math.max(Math.abs(a.cu() - b.cu()), Math.abs(a.cv() - b.cv())) >= 5,
                            "seed " + seed);
                }
            }
        }
    }

    @Test
    void aVeinSizeRefusesWhatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new VeinSize(" ", 3, 2, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 0, 2, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 2, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 2, 3, 2));
        // Above the worst-case guarantee (both sides are in the row below this one).
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 1, 1, 1, 5));
        // Mutation: drop the capacity refusal -> the last line reddens.
    }

    // --- The rules, on hand-made layouts ---------------------------------------------------------

    @Test
    void theEdgeRuleAcceptsTwoAndWidthMinusThreeAndRefusesOneBeyond() {
        assertTrue(SpotLayout.fitsEdge(new Spot(2, 2), SMALL));
        assertTrue(SpotLayout.fitsEdge(new Spot(45, 29), SMALL), "48 - 3 and 32 - 3");
        assertFalse(SpotLayout.fitsEdge(new Spot(1, 10), SMALL));
        assertFalse(SpotLayout.fitsEdge(new Spot(46, 10), SMALL));
        assertFalse(SpotLayout.fitsEdge(new Spot(10, 1), SMALL));
        assertFalse(SpotLayout.fitsEdge(new Spot(10, 30), SMALL));
        // Mutation: EDGE_MARGIN_PX 2 -> 1 -> (1, 10) is accepted -> reddens.
    }

    @Test
    void theSpacingRuleIsChebyshevFive() {
        assertTrue(SpotLayout.farEnough(new Spot(10, 10), List.of(new Spot(15, 10))));
        assertTrue(SpotLayout.farEnough(new Spot(10, 10), List.of(new Spot(15, 14))), "dx 5 is enough");
        assertTrue(SpotLayout.farEnough(new Spot(10, 10), List.of(new Spot(13, 15))), "dy 5 is enough");
        assertFalse(SpotLayout.farEnough(new Spot(10, 10), List.of(new Spot(14, 14))), "dx 4, dy 4");
        assertFalse(SpotLayout.farEnough(new Spot(10, 10), List.of(new Spot(14, 10))));
        // Mutation: Chebyshev -> Manhattan (dx + dy >= 5) -> (14, 14) passes -> reddens.
        // Mutation: SPOT_SPACING_PX 5 -> 4 -> (14, 10) passes -> reddens.
    }

    // --- The roll -------------------------------------------------------------------------------

    @Test
    void tenThousandSeededRollsOfTheSmallVeinAllObeyBothRules() {
        Set<Integer> counts = new HashSet<>();
        int minU = Integer.MAX_VALUE, maxU = Integer.MIN_VALUE;
        for (long seed = 0; seed < 10_000; seed++) {
            SpotLayout layout = SpotLayout.roll(SMALL, new SplittableRandom(seed));
            List<Spot> spots = layout.spots();
            counts.add(spots.size());
            assertTrue(spots.size() >= 2 && spots.size() <= 3, "seed " + seed + ": " + spots);
            for (int i = 0; i < spots.size(); i++) {
                Spot a = spots.get(i);
                // Independent arithmetic: the box a.cu-2 .. a.cu+2 must lie in 0 .. 47.
                assertTrue(a.cu() - 2 >= 0 && a.cu() + 2 <= 47, "seed " + seed + " u: " + a);
                assertTrue(a.cv() - 2 >= 0 && a.cv() + 2 <= 31, "seed " + seed + " v: " + a);
                minU = Math.min(minU, a.cu());
                maxU = Math.max(maxU, a.cu());
                for (int j = i + 1; j < spots.size(); j++) {
                    Spot b = spots.get(j);
                    int cheb = Math.max(Math.abs(a.cu() - b.cu()), Math.abs(a.cv() - b.cv()));
                    assertTrue(cheb >= 5, "seed " + seed + ": " + a + " and " + b + " overlap");
                }
            }
        }
        assertEquals(Set.of(2, 3), counts, "both ruled counts occur");
        assertEquals(2, minU, "the edge value is reachable, so the margin is not wider than ruled");
        assertEquals(45, maxU);
    }

    @Test
    void tenThousandSeededRollsOfEachS2SizeAllObeyBothRules() {
        // S2: the three new sizes (design doc, relayed). Each is far under its worst-case guarantee
        // (medium 5 <= 45, large 7 <= 60, extra_large 9 <= 84; executed in PLAN 5.12), so the sampler
        // must never throw.
        for (VeinSize size : List.of(MiningFixtures.medium(), MiningFixtures.large(), MiningFixtures.extraLarge())) {
            Set<Integer> counts = new HashSet<>();
            for (long seed = 0; seed < 10_000; seed++) {
                List<Spot> spots = SpotLayout.roll(size, new SplittableRandom(seed)).spots();
                counts.add(spots.size());
                for (int i = 0; i < spots.size(); i++) {
                    Spot a = spots.get(i);
                    assertTrue(a.cu() - 2 >= 0 && a.cu() + 2 <= size.width() * 16 - 1, size.id() + " seed " + seed + " u: " + a);
                    assertTrue(a.cv() - 2 >= 0 && a.cv() + 2 <= size.height() * 16 - 1, size.id() + " seed " + seed + " v: " + a);
                    for (int j = i + 1; j < spots.size(); j++) {
                        Spot b = spots.get(j);
                        assertTrue(Math.max(Math.abs(a.cu() - b.cu()), Math.abs(a.cv() - b.cv())) >= 5,
                                size.id() + " seed " + seed + ": " + a + " and " + b);
                    }
                }
            }
            assertEquals(Set.of(size.minSpots(), size.maxSpots()), counts, size.id());
        }
    }

    @Test
    void theS2SizesGuaranteeTheirExecutedCeilings() {
        assertEquals(45, MiningFixtures.medium().guaranteedSpots());
        assertEquals(60, MiningFixtures.large().guaranteedSpots());
        assertEquals(84, MiningFixtures.extraLarge().guaranteedSpots());
    }

    @Test
    void theCountComesFromTheRangeInclusive() {
        // count first, then (u, v) per spot.
        assertEquals(3, SpotLayout.roll(SMALL, ScriptedRandom.of(3, 2, 2, 20, 20, 40, 2)).spots().size());
        assertEquals(2, SpotLayout.roll(SMALL, ScriptedRandom.of(2, 2, 2, 20, 20)).spots().size());
    }

    @Test
    void theRevealOrderIsTheRollOrder() {
        SpotLayout layout = SpotLayout.roll(SMALL, ScriptedRandom.of(2, 40, 7, 3, 20));
        assertEquals(List.of(new Spot(40, 7), new Spot(3, 20)), layout.spots());
    }

    @Test
    void aSpotThatCannotBePlacedFailsLoudlyInsteadOfLooping() {
        // A generator that always answers the smallest value puts every candidate at (2, 2): the
        // second spot can never be far enough from the first.
        assertThrows(IllegalStateException.class, () -> SpotLayout.roll(SMALL, ScriptedRandom.alwaysMin()));
    }
}
