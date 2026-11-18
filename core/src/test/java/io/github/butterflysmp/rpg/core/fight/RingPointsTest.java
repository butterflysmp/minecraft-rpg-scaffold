package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F4, THE RING'S POINTS (PLAN-dragon-fight.md section 6 step F4): pure, so the count per redraw is pinned here. */
class RingPointsTest {

    static RingLook look() {
        return new RingLook("dust", 170, 0, 255, 1.6, 1.0, List.of(0.2, 1.2, 2.2), 4);
    }

    @Test
    void aRadius48RingIs302PointsAtEachOfThreeHeights() {
        // 2 x pi x 48 = 301.59 blocks round, one point a block or closer: ceil gives 302.
        assertEquals(302, RingPoints.pointsPerHeight(48.0, look()));
        assertEquals(906, RingPoints.pointsPerRedraw(48.0, look()));
        assertEquals(906, RingPoints.points(new Ring(0, 0, 48.0), 64.0, look()).size());
    }

    @Test
    void everyPointLiesOnTheRingAtItsHeight() {
        Ring ring = new Ring(10.0, -5.0, 12.0);
        List<double[]> pts = RingPoints.points(ring, 64.0, look());
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
        List<double[]> pts = RingPoints.points(new Ring(0, 0, 48.0), 0.0, look());
        double worst = 0.0;
        for (int i = 1; i < 302; i++) {
            worst = Math.max(worst, Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[2] - pts.get(i - 1)[2]));
        }
        assertTrue(worst <= 1.0, "chord " + worst + " is within the 1-block spacing");
        assertTrue(worst > 0.99, "and not needlessly dense: " + worst);
    }

    @Test
    void aClosedRingDrawsNothing() {
        assertEquals(0, RingPoints.points(new Ring(0, 0, 0.0), 64.0, look()).size());
        assertEquals(0, RingPoints.pointsPerRedraw(0.0, look()));
    }

    @Test
    void theLookRefusesNonsense() {
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 256, 0, 0, 1.6, 1.0, List.of(0.2), 4), "a colour channel past 255");
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 0.0, List.of(0.2), 4), "a zero spacing");
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 1.0, List.of(), 4), "no height at all");
        assertThrows(IllegalArgumentException.class,
                () -> new RingLook("dust", 1, 0, 0, 1.6, 1.0, List.of(0.2), 0), "a zero redraw period");
    }
}
