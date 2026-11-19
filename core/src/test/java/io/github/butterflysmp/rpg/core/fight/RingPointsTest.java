package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F4, THE RING'S POINTS (PLAN-dragon-fight.md section 6 step F4): pure, so the count per redraw is pinned here. */
class RingPointsTest {

    static RingLook look() {
        return new RingLook("dust", 170, 0, 255, 1.6, 1.0, List.of(0.2, 1.2, 2.2), 0.0, 0.0, false, 4);
    }

    @Test
    void aRadius48RingIs302PointsAtEachOfThreeHeights() {
        // 2 x pi x 48 = 301.59 blocks round, one point a block or closer: ceil gives 302.
        assertEquals(302, RingPoints.pointsPerHeight(48.0, look()));
        assertEquals(906, RingPoints.pointsPerRedraw(48.0, look()));
        assertEquals(906, RingPoints.points(new Ring(0, 0, 48.0), 64.0, look(), 0L).size());
    }

    @Test
    void everyPointLiesOnTheRingAtItsHeight() {
        Ring ring = new Ring(10.0, -5.0, 12.0);
        List<double[]> pts = RingPoints.points(ring, 64.0, look(), 0L);
        int perHeight = RingPoints.pointsPerHeight(12.0, look());
        for (int i = 0; i < pts.size(); i++) {
            double[] p = pts.get(i);
            assertEquals(12.0, Math.hypot(p[0] - 10.0, p[2] + 5.0), 1e-9, "point " + i + " is on the ring");
            double expectedY = 64.0 + look().heightOffsetsBlocks().get(i / perHeight);
            assertEquals(expectedY, p[1], 1e-9, "point " + i + " is at its height");
        }
    }

    @Test
    void consecutivePointsAreAtMostTheSpacingApart() {
        List<double[]> pts = RingPoints.points(new Ring(0, 0, 48.0), 0.0, look(), 0L);
        double worst = 0.0;
        for (int i = 1; i < 302; i++) {
            worst = Math.max(worst, Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[2] - pts.get(i - 1)[2]));
        }
        assertTrue(worst <= 1.0, "chord " + worst + " is within the 1-block spacing");
        assertTrue(worst > 0.99, "and not needlessly dense: " + worst);
    }

    /** The wall Ben said yes to (eighth message): four rows 1.5 apart, each shifted round, turning. */
    static RingLook wall() {
        return new RingLook("dust", 170, 0, 255, 1.6, 1.0, List.of(0.2, 1.7, 3.2, 4.7), 0.25, 1.0, false, 4);
    }

    /**
     * The wall after the second boot: six rows 1.5 apart (Ben: "yes, maybe another row or 2"), every other row turning
     * the opposite way (Ben: "maybe alternate twists on every other row").
     */
    static RingLook sixRowWall() {
        return new RingLook("dust", 170, 0, 255, 1.6, 1.0, List.of(0.2, 1.7, 3.2, 4.7, 6.2, 7.7), 0.25, 1.0, true, 4);
    }

    @Test
    void theSixRowWallIs1812PointsAtRadius48() {
        assertEquals(1812, RingPoints.pointsPerRedraw(48.0, sixRowWall()), "302 a row, 6 rows");
        assertEquals(1812, RingPoints.points(new Ring(0, 0, 48.0), 64.0, sixRowWall(), 0L).size());
    }

    /** The signed arc a row's first point moved between two redraws, blocks, wrapped into (-C/2, C/2]. */
    private static double movedAlong(double[] before, double[] after, double r) {
        double c = 2 * Math.PI * r;
        double d = arcOf(after, r) - arcOf(before, r);
        d = ((d % c) + c) % c;
        return d > c / 2 ? d - c : d;
    }

    @Test
    void rowsZeroTwoAndFourTurnOneWayAndRowsOneThreeAndFiveTheOther() {
        int per = RingPoints.pointsPerHeight(48.0, sixRowWall());
        List<double[]> start = RingPoints.points(new Ring(0, 0, 48.0), 64.0, sixRowWall(), 0L);
        List<double[]> later = RingPoints.points(new Ring(0, 0, 48.0), 64.0, sixRowWall(), 20L);
        for (int row = 0; row < 6; row++) {
            double expected = row % 2 == 0 ? 1.0 : -1.0;
            assertEquals(expected, movedAlong(start.get(row * per), later.get(row * per), 48.0), 1e-9,
                    "row " + row + " turned " + expected + " block in 20 ticks");
            // The shift is not alternated: each row still starts its shift further round at tick 0.
            assertEquals(row * 0.25, arcOf(start.get(row * per), 48.0), 1e-9, "row " + row + "'s shift");
        }
    }

    @Test
    void theFourRowWallIs1208PointsAtRadius48() {
        assertEquals(1208, RingPoints.pointsPerRedraw(48.0, wall()), "302 a row, 4 rows");
        assertEquals(1208, RingPoints.points(new Ring(0, 0, 48.0), 64.0, wall(), 0L).size());
    }

    /** The arc from angle 0 to a point, blocks, measured anticlockwise in [0, 2 pi r). */
    private static double arcOf(double[] p, double r) {
        double a = Math.atan2(p[2], p[0]);
        if (a < 0) a += 2 * Math.PI;
        return a * r;
    }

    @Test
    void eachRowStartsTheShiftFurtherRoundThanTheRowBelow() {
        int per = RingPoints.pointsPerHeight(48.0, wall());
        List<double[]> pts = RingPoints.points(new Ring(0, 0, 48.0), 64.0, wall(), 0L);
        for (int row = 0; row < 4; row++) {
            double[] first = pts.get(row * per);
            assertEquals(row * 0.25, arcOf(first, 48.0), 1e-9, "row " + row + "'s first point");
            assertEquals(64.0 + wall().heightOffsetsBlocks().get(row), first[1], 1e-12);
            assertEquals(48.0, Math.hypot(first[0], first[2]), 1e-9, "still on the ring");
        }
    }

    @Test
    void theWholeWallTurnsAtTheSpeedAlongTheCircumference() {
        int per = RingPoints.pointsPerHeight(48.0, wall());
        List<double[]> start = RingPoints.points(new Ring(0, 0, 48.0), 64.0, wall(), 0L);
        List<double[]> second = RingPoints.points(new Ring(0, 0, 48.0), 64.0, wall(), 20L);
        for (int row = 0; row < 4; row++) {
            double moved = arcOf(second.get(row * per), 48.0) - arcOf(start.get(row * per), 48.0);
            assertEquals(1.0, moved, 1e-9, "row " + row + " turned 1.0 block in 20 ticks");
        }
        // And at a smaller radius the same speed is a larger angle: the speed is along the circumference.
        List<double[]> small = RingPoints.points(new Ring(0, 0, 12.0), 64.0, wall(), 20L);
        assertEquals(1.0, arcOf(small.get(0), 12.0), 1e-9);
    }

    @Test
    void theLookRefusesANonFiniteShiftOrTurn() {
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 1.0, List.of(0.2), Double.NaN, 0.0, false, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 1.0, List.of(0.2), 0.0, Double.POSITIVE_INFINITY, false, 4));
    }

    @Test
    void aClosedRingDrawsNothing() {
        assertEquals(0, RingPoints.points(new Ring(0, 0, 0.0), 64.0, look(), 0L).size());
        assertEquals(0, RingPoints.pointsPerRedraw(0.0, look()));
    }

    @Test
    void theLookRefusesNonsense() {
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 256, 0, 0, 1.6, 1.0, List.of(0.2), 0.0, 0.0, false, 4), "a colour channel past 255");
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 0.0, List.of(0.2), 0.0, 0.0, false, 4), "a zero spacing");
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 1.0, List.of(), 0.0, 0.0, false, 4), "no height at all");
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 1.0, List.of(0.2), 0.0, 0.0, false, 0), "a zero redraw period");
    }
}
