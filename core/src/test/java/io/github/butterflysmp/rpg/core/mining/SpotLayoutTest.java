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
    void theSmallVeinIs48By32PixelsWith54Tiles() {
        assertEquals(48, SMALL.widthPx());
        assertEquals(32, SMALL.heightPx());
        assertEquals(54, SMALL.tileCapacity(), "floor(48 / 5) x floor(32 / 5) = 9 x 6");
    }

    @Test
    void aVeinSizeRefusesWhatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new VeinSize(" ", 3, 2, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 0, 2, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 2, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 3, 2, 3, 2));
        // 1 x 1 is 16 x 16 pixels: 3 x 3 = 9 tiles. Ten spots cannot be guaranteed.
        assertEquals(9, new VeinSize("s", 1, 1, 1, 9).tileCapacity());
        assertThrows(IllegalArgumentException.class, () -> new VeinSize("s", 1, 1, 1, 10));
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
