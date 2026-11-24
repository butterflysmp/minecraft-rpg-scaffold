package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PAD'S SHAPE ({@link SelfTestPadShape}; Ben's ruling A and the seat's P2-P3, 2026-11-23): pure, so every cell is a
 * unit row. An interior x -2..45, z -13..13 from the stand point; a one-block stone floor under all of it; a barrier RIM
 * round it from floor level to 4 above; air above the floor inside. Coordinates here are blocks from the stand point's
 * column, with {@code dy} 0 the FLOOR's level (his feet stand at dy 1).
 */
class SelfTestPadShapeTest {

    @Test
    void theInteriorFloorAndRimHaveTheirCounts() {
        Map<SelfTestPadShape.Kind, Integer> n = new EnumMap<>(SelfTestPadShape.Kind.class);
        SelfTestPadShape.forEachCell((dx, dy, dz, kind) -> n.merge(kind, 1, Integer::sum));
        // Interior 48 x 27 = 1296 columns: 1296 floor, 1296 x 4 = 5184 air. Rim: 50 x 29 - 1296 = 154 columns x 5 = 770.
        assertEquals(1296, n.get(SelfTestPadShape.Kind.FLOOR));
        assertEquals(5184, n.get(SelfTestPadShape.Kind.AIR));
        assertEquals(770, n.get(SelfTestPadShape.Kind.RIM));
        assertEquals(7250, SelfTestPadShape.CELLS);
    }

    @Test
    void eachCellHasOneKindAndNothingOutsideThePadHasAny() {
        assertEquals(SelfTestPadShape.Kind.FLOOR, SelfTestPadShape.kindAt(0, 0, 0), "under the stand point");
        assertEquals(SelfTestPadShape.Kind.AIR, SelfTestPadShape.kindAt(0, 1, 0), "his feet");
        assertEquals(SelfTestPadShape.Kind.AIR, SelfTestPadShape.kindAt(45, 4, 13), "the far corner, the top air layer");
        assertEquals(SelfTestPadShape.Kind.RIM, SelfTestPadShape.kindAt(-3, 0, 0), "the rim at floor level, west");
        assertEquals(SelfTestPadShape.Kind.RIM, SelfTestPadShape.kindAt(46, 4, 14), "the rim's top corner");
        assertNull(SelfTestPadShape.kindAt(0, 5, 0), "above the rim's height: not a pad cell");
        assertNull(SelfTestPadShape.kindAt(0, -1, 0), "under the floor: not a pad cell");
        assertNull(SelfTestPadShape.kindAt(47, 0, 0), "beyond the rim: not a pad cell");
        assertNull(SelfTestPadShape.kindAt(0, 0, -15), "beyond the rim: not a pad cell");
    }

    /**
     * THE FALL HE CANNOT TAKE: at floor level and each of the four levels above, every interior cell's eight neighbours
     * are interior or RIM. None is open air, so nothing in the interior can step, slide or be knocked off an edge.
     */
    @Test
    void noInteriorCellIsBesideOpenAirAtFloorLevelOrTheFourAbove() {
        int checked = 0;
        for (int dx = SelfTestPadShape.X1; dx <= SelfTestPadShape.X2; dx++) {
            for (int dz = SelfTestPadShape.Z1; dz <= SelfTestPadShape.Z2; dz++) {
                for (int dy = 0; dy <= SelfTestPadShape.RIM_HEIGHT; dy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            if (ox == 0 && oz == 0) continue;
                            int nx = dx + ox;
                            int nz = dz + oz;
                            boolean interior = SelfTestPadShape.inInterior(nx, nz);
                            assertTrue(interior || SelfTestPadShape.kindAt(nx, dy, nz) == SelfTestPadShape.Kind.RIM,
                                    "(" + dx + "," + dy + "," + dz + ") has open air at (" + nx + "," + dy + "," + nz + ")");
                            checked++;
                        }
                    }
                }
            }
        }
        assertEquals(1296 * 5 * 8, checked);
    }

    @Test
    void theStandPointIsTwoHundredEastOfSpawnAtSpawnsZWithTheFloorAtY200() {
        assertEquals(new SelfTestPadShape.Stand(212, 200, -37), SelfTestPadShape.stand(12, -37));
        assertEquals(201, SelfTestPadShape.stand(12, -37).feetY());
    }

    @Test
    void theInteriorHoldsEnchantColumnsGroundWithMargin() {
        // GATE-enchant-columns' preflight box, 0..32 east and -6..10 south: inside, two blocks or more from each edge.
        assertTrue(SelfTestPadShape.inInterior(0, -6) && SelfTestPadShape.inInterior(32, 10));
        assertTrue(SelfTestPadShape.inInterior(-2, -13) && SelfTestPadShape.inInterior(45, 13), "the interior's corners");
        assertTrue(!SelfTestPadShape.inInterior(-3, 0) && !SelfTestPadShape.inInterior(0, 14), "the rim is not interior");
    }

    @Test
    void aRightCellIsOkAnEmptyFloorOrRimCellIsPlacedAndAnyOtherWrongCellIsRepaired() {
        assertEquals(SelfTestPadShape.Verdict.OK, SelfTestPadShape.verdict(SelfTestPadShape.Kind.FLOOR, false, true));
        assertEquals(SelfTestPadShape.Verdict.OK, SelfTestPadShape.verdict(SelfTestPadShape.Kind.AIR, true, true));
        assertEquals(SelfTestPadShape.Verdict.PLACED, SelfTestPadShape.verdict(SelfTestPadShape.Kind.FLOOR, true, false));
        assertEquals(SelfTestPadShape.Verdict.PLACED, SelfTestPadShape.verdict(SelfTestPadShape.Kind.RIM, true, false));
        assertEquals(SelfTestPadShape.Verdict.REPAIRED, SelfTestPadShape.verdict(SelfTestPadShape.Kind.FLOOR, false, false),
                "dirt where stone should be");
        assertEquals(SelfTestPadShape.Verdict.REPAIRED, SelfTestPadShape.verdict(SelfTestPadShape.Kind.AIR, false, false),
                "a block where air should be");
    }

    @Test
    void thePadLineNamesWhereAndWhatWasDone() {
        assertEquals("PAD world=world stand=212,201,-37 floorY=200 interior=-2..45 east -13..13 south cells=7250"
                        + " placed=2066 repaired=0 ok=5184",
                SelfTestPadShape.line("world", SelfTestPadShape.stand(12, -37), new SelfTestPadShape.Tally(2066, 0, 5184)));
    }

    @Test
    void theChunksSpanTheRimToo() {
        // Stand x 212: the rim from 209 to 258 is chunks 13..16; z -37: the rim from -51 to -23 is chunks -4..-2.
        assertEquals(java.util.List.of(13, -4, 16, -2),
                java.util.Arrays.stream(SelfTestPadShape.chunks(SelfTestPadShape.stand(12, -37))).boxed().toList());
    }

    @Test
    void theFortyBlocksOfAirAboveTheFloorAreInsideTheOverworldsBuildHeight() {
        // READ from the server jar (run/versions/26.1.2/paper-26.1.2.jar, build 74):
        // data/minecraft/dimension_type/overworld.json, min_y -64, height 384: the top block is Y 319.
        int top = -64 + 384 - 1;
        assertTrue(SelfTestPadShape.FLOOR_Y + 40 <= top, "floor " + SelfTestPadShape.FLOOR_Y + " + 40 against " + top);
    }
}
