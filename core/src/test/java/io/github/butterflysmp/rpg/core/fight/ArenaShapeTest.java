package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE GENERATED STAND-IN ARENA (the F8 brief, section 4 b; Ben: "Yes that's fine for now"): an end-stone disc of radius
 * 56, 3 thick, its top at y 64, and ten 3x3 obsidian pillars on a circle of radius 40, their tops 12 above the floor;
 * crystal spot n stands on pillar n. Pure, so the block counts the gate predicts are the code's, executed here.
 */
class ArenaShapeTest {

    /** The brief's numbers (SEAT PLACEHOLDERS), centred on (0, 0) of the fight's world. */
    static ArenaShape brief() {
        return new ArenaShape(0, 0, 64, 56, 3, 10, 40.0, 3, 12);
    }

    @Test
    void theDiscIsEveryColumnWithinTheRadiusThreeThick() {
        ArenaShape a = brief();
        long columns = 0;
        for (int dx = -60; dx <= 60; dx++) for (int dz = -60; dz <= 60; dz++) if (dx * dx + dz * dz <= 56 * 56) columns++;
        assertEquals(columns, a.floorColumns(), "the disc's columns: dx^2 + dz^2 <= 56^2");
        assertEquals(3 * columns, a.floorBlocks());
        assertTrue(a.isFloorColumn(56, 0) && a.isFloorColumn(0, -56), "the edge is in");
        assertFalse(a.isFloorColumn(57, 0) || a.isFloorColumn(40, 40), "beyond it is not (40,40 is 56.57 out)");
        assertEquals(62, a.floorBottomY());
        assertEquals(64, a.floorTopY());
    }

    @Test
    void tenThreeByThreePillarsOnTheRingTwelveHighAboveTheFloor() {
        ArenaShape a = brief();
        List<int[]> centres = a.pillarCentres();
        assertEquals(10, centres.size());
        Set<String> distinct = new HashSet<>();
        for (int[] c : centres) {
            double d = Math.hypot(c[0], c[1]);
            assertTrue(Math.abs(d - 40.0) <= Math.sqrt(0.5) + 1e-9, "a pillar centre is on the ring, rounded: " + d);
            distinct.add(c[0] + "," + c[1]);
        }
        assertEquals(10, distinct.size(), "ten distinct pillars");
        assertEquals(10L * 9 * 12, a.pillarBlocks(), "10 pillars x 9 columns x 12 high");
        assertEquals(76, a.pillarTopY(), "the top 12 above the floor's top at 64");
        assertEquals(a.floorBlocks() + a.pillarBlocks(), a.totalBlocks());
    }

    /** The figures GATE-fight-arena.md predicts, executed here so the gate quotes the code's numbers. */
    @Test
    void theBriefsArenaIs30615BlocksWithItsPillarsHere() {
        ArenaShape a = brief();
        assertEquals(9845, a.floorColumns());
        assertEquals(29535, a.floorBlocks());
        assertEquals(1080, a.pillarBlocks());
        assertEquals(30615, a.totalBlocks());
        List<String> centres = a.pillarCentres().stream().map(c -> c[0] + "," + c[1]).toList();
        assertEquals(List.of("40,0", "32,24", "12,38", "-12,38", "-32,24", "-40,0", "-32,-24", "-12,-38", "12,-38",
                "32,-24"), centres);
    }

    @Test
    void theFirstPillarIsDueEastAndEveryBlockTheShapeEmitsIsCounted() {
        ArenaShape a = brief();
        assertEquals(40, a.pillarCentres().get(0)[0]);
        assertEquals(0, a.pillarCentres().get(0)[1]);
        long[] counts = new long[2];
        a.forEachBlock((x, y, z, kind) -> counts[kind == ArenaShape.Kind.FLOOR ? 0 : 1]++);
        assertEquals(a.floorBlocks(), counts[0]);
        assertEquals(a.pillarBlocks(), counts[1]);
    }

    @Test
    void crystalSpotNStandsOnPillarNAndTheGroundIsTheFloorsTopPlusOne() {
        ArenaShape a = brief();
        for (int n = 1; n <= 10; n++) {
            int[] c = a.pillarCentres().get(n - 1);
            double[] spot = a.crystalSpot(n);
            assertEquals(c[0] + 0.5, spot[0], 0.0);
            assertEquals(77.0, spot[1], 0.0, "standing on the pillar's top block at 76");
            assertEquals(c[1] + 0.5, spot[2], 0.0);
        }
        assertEquals(65.0, a.groundY(), 0.0, "C's ground for the wall and the Mage: feet on the floor at 64");
        assertThrows(IllegalArgumentException.class, () -> a.crystalSpot(0));
        assertThrows(IllegalArgumentException.class, () -> a.crystalSpot(11));
    }

    @Test
    void theShapeRefusesPillarsOffTheDiscAndNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new ArenaShape(0, 0, 64, 40, 3, 10, 40.0, 3, 12),
                "a pillar at 40 + 1 hangs off a disc of 40");
        assertThrows(IllegalArgumentException.class, () -> new ArenaShape(0, 0, 64, 56, 0, 10, 40.0, 3, 12));
        assertThrows(IllegalArgumentException.class, () -> new ArenaShape(0, 0, 64, 56, 3, 10, 40.0, 2, 12),
                "an even pillar width has no centre block");
        assertThrows(IllegalArgumentException.class, () -> new ArenaShape(0, 0, 64, 56, 3, 0, 40.0, 3, 12));
        assertThrows(IllegalArgumentException.class, () -> new ArenaShape(0, 0, 64, 56, 3, 10, 40.0, 3, 0));
    }
}
