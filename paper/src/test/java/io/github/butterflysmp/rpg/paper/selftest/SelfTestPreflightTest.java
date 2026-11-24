package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PREFLIGHT ({@link SelfTestPreflight}): before any row of a run that declares one, the ground the scenario declares
 * is measured, and a bad spot runs NO row. The boot of {@code e774d4b8} read 255 blocks of WATER in EC0's floor and ran
 * every row on it anyway; EC0 only described the spot. What he is told is plain words naming what is wrong and where,
 * then what to do; the log carries the counts either way.
 */
class SelfTestPreflightTest {

    private static final SelfTestPreflight.Box BOX = new SelfTestPreflight.Box(0, -6, 32, 10);

    /** One cell's reading: its material's name, and (floor only) its kind. */
    private record Cell(String name, SelfTestPreflight.Floor kind) {}

    private static final Cell AIR = new Cell("air", null);
    private static final Cell GROUND = new Cell("grass_block", SelfTestPreflight.Floor.SOLID);

    /** The whole box, every column read: two body cells (feet, head) and the floor under them. */
    private static SelfTestPreflight.Tally measure(BiFunction<Integer, Integer, Cell> feet,
                                                  BiFunction<Integer, Integer, Cell> head,
                                                  BiFunction<Integer, Integer, Cell> floor) {
        SelfTestPreflight.Tally t = new SelfTestPreflight.Tally(BOX);
        for (int dx = BOX.x1(); dx <= BOX.x2(); dx++) {
            for (int dz = BOX.z1(); dz <= BOX.z2(); dz++) {
                Cell f = feet.apply(dx, dz);
                Cell h = head.apply(dx, dz);
                Cell g = floor.apply(dx, dz);
                t.body(dx, dz, f.name().equals("air"), f.name());
                t.body(dx, dz, h.name().equals("air"), h.name());
                t.floor(dx, dz, g.kind(), g.name());
            }
        }
        return t;
    }

    @Test
    void theBoxIsFourWholeNumbersEastFromEastToSouthFromSouthTo() {
        assertEquals(BOX, SelfTestPreflight.parse("0 -6 32 10").orElseThrow());
        assertTrue(SelfTestPreflight.parse("0 -6 32").isEmpty(), "three numbers");
        assertTrue(SelfTestPreflight.parse("32 -6 0 10").isEmpty(), "east backwards");
        assertTrue(SelfTestPreflight.parse("0 10 32 -6").isEmpty(), "south backwards");
        assertTrue(SelfTestPreflight.parse("0 -6 32 x").isEmpty(), "not a number");
        assertTrue(SelfTestPreflight.parse("0 -6 65 10").isEmpty(), "beyond 64");
    }

    @Test
    void aClearSpotPassesAndSaysSoWithItsCounts() {
        SelfTestPreflight.Tally t = measure((x, z) -> AIR, (x, z) -> AIR, (x, z) -> GROUND);
        assertTrue(t.ok());
        // 33 x 17 = 561 columns: 1122 body cells, 561 floor cells.
        assertEquals("PREFLIGHT ok box=0..32 east -6..10 south air=1122/1122 floor solid=561/561", t.logLine());
        assertEquals(List.of(), t.tell());
    }

    @Test
    void waterInTheFloorFailsAndIsNamedWithWhereItIs() {
        // The seat's own example, 12 blocks of water: here 3 to 8 east, 2 to 3 to his right (6 x 2).
        Cell water = new Cell("water", SelfTestPreflight.Floor.LIQUID);
        SelfTestPreflight.Tally t = measure((x, z) -> AIR, (x, z) -> AIR,
                (x, z) -> x >= 3 && x <= 8 && z >= 2 && z <= 3 ? water : GROUND);
        assertFalse(t.ok());
        assertEquals(List.of(
                "The selftest cannot start here: 12 floor blocks are water or another liquid (water), from 3 to 8 blocks"
                        + " east, 2 to 3 to your right.",
                "Move to solid flat ground and type the command again."), t.tell());
        assertEquals("PREFLIGHT fail box=0..32 east -6..10 south air=1122/1122 floor solid=549/561 liquid=12 hole=0"
                + " soft=0", t.logLine());
    }

    @Test
    void eachKindOfBadGroundIsItsOwnPartInOneLine() {
        SelfTestPreflight.Tally t = measure(
                (x, z) -> x == 5 && z == -3 ? new Cell("short_grass", null) : AIR,
                (x, z) -> x == 6 && z == 0 ? new Cell("oak_leaves", null) : AIR,
                (x, z) -> x == 10 && z == 0 ? new Cell("air", SelfTestPreflight.Floor.HOLE)
                        : x == 0 && z == -6 ? new Cell("snow", SelfTestPreflight.Floor.SOFT) : GROUND);
        List<String> tell = t.tell();
        assertEquals(2, tell.size(), "one or two lines");
        // The parts in the order the measure meets them: body, then the floor's kinds (liquid, hole, soft).
        assertEquals("The selftest cannot start here: 2 blocks at foot or head height are not air (short_grass), from 5"
                + " to 6 blocks east, from 3 to your left to your line; 1 floor block is a hole (air), 10 blocks east, on"
                + " your line; 1 floor block is not solid (snow), 0 blocks east, 6 to your left.", tell.get(0));
        assertEquals("PREFLIGHT fail box=0..32 east -6..10 south air=1120/1122 floor solid=559/561 liquid=0 hole=1"
                + " soft=1", t.logLine());
    }

    @Test
    void theSideReadsLeftAndRightAsHeFacesEast() {
        assertEquals("on your line", SelfTestPreflight.side(0, 0));
        assertEquals("4 to your right", SelfTestPreflight.side(4, 4));
        assertEquals("2 to 4 to your right", SelfTestPreflight.side(2, 4));
        assertEquals("3 to your left", SelfTestPreflight.side(-3, -3));
        assertEquals("1 to 3 to your left", SelfTestPreflight.side(-3, -1));
        assertEquals("from 6 to your left to 10 to your right", SelfTestPreflight.side(-6, 10));
        assertEquals("from your line to 2 to your right", SelfTestPreflight.side(0, 2));
        assertEquals("from 5 to your left to your line", SelfTestPreflight.side(-5, 0));
    }

    @Test
    void theFloorKindIsSolidOnlyWhenNotAirNotLiquidNotWaterloggedAndSolid() {
        assertEquals(SelfTestPreflight.Floor.SOLID, SelfTestPreflight.floor(false, false, true));
        assertEquals(SelfTestPreflight.Floor.HOLE, SelfTestPreflight.floor(true, false, false));
        assertEquals(SelfTestPreflight.Floor.LIQUID, SelfTestPreflight.floor(false, true, false));
        // A waterlogged slab is solid AND wet: the wet wins.
        assertEquals(SelfTestPreflight.Floor.LIQUID, SelfTestPreflight.floor(false, true, true));
        assertEquals(SelfTestPreflight.Floor.SOFT, SelfTestPreflight.floor(false, false, false));
    }
}
