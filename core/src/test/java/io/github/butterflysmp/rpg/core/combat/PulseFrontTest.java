package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * THE PULSE'S PURE DECISIONS AS TABLES (Smolder, the seat's Z1, Z2, Z7, 2026-11-27): when each pulse fires, how far the
 * ring has travelled, and the tick it reaches a body. Smolder's own numbers: radius 12, travel 30 ticks (0.4 blocks a
 * tick), interval 60, count 4. Every expected value here was EXECUTED, never predicted: the reach table is the assertion
 * of record for the gate's SM2 hit ticks.
 */
class PulseFrontTest {

    private static final double RADIUS = 12.0;
    private static final int TRAVEL = 30;

    /** WHEN: the cast tick plus (n - 1) x 60. Four pulses, from a cast at tick 1000. */
    @Test
    void eachPulseFiresAtTheCastTickPlusItsIntervals() {
        assertEquals(1000, PulseFront.pulseTick(1000, 1, 60));
        assertEquals(1060, PulseFront.pulseTick(1000, 2, 60));
        assertEquals(1120, PulseFront.pulseTick(1000, 3, 60));
        assertEquals(1180, PulseFront.pulseTick(1000, 4, 60));
        assertThrows(IllegalArgumentException.class, () -> PulseFront.pulseTick(1000, 0, 60));
    }

    /** HOW FAR: 0 at tick 0, radius x t / travel between, the radius from the travel's end on. */
    @Test
    void theRingGrowsLinearlyFromZeroToTheRadiusOverItsTravel() {
        assertEquals(0.0, PulseFront.radiusAt(0, RADIUS, TRAVEL));
        assertEquals(0.4, PulseFront.radiusAt(1, RADIUS, TRAVEL), 1e-12);
        assertEquals(6.0, PulseFront.radiusAt(15, RADIUS, TRAVEL));
        assertEquals(12.0, PulseFront.radiusAt(30, RADIUS, TRAVEL));
        assertEquals(12.0, PulseFront.radiusAt(31, RADIUS, TRAVEL));
    }

    /**
     * WHEN THE RING REACHES A BODY, the gate's SM2 distances and their neighbours: 3 -> 8, 4 (his wolf, never struck by
     * the side, but its tick named) -> 10, 6 -> 15, 11.5 -> 29, 12 -> 30, 12.5 -> never. And the edges: 0 -> 0, a hair
     * over one step -> 2. A body EXACTLY on a step (6.0 at t 15, 4.0 at t 10) is struck on that step: {@code >=}.
     */
    @Test
    void theReachTickIsTheFirstWholeTickTheRingIsAtOrBeyondTheDistance() {
        double[][] table = {
                {0.0, 0}, {0.4, 1}, {0.41, 2}, {3.0, 8}, {4.0, 10}, {6.0, 15}, {11.5, 29}, {12.0, 30}, {12.5, -1},
        };
        for (double[] row : table) {
            assertEquals((int) row[1], PulseFront.reachTick(row[0], RADIUS, TRAVEL), "distance " + row[0]);
        }
        assertEquals(-1, PulseFront.reachTick(Double.NaN, RADIUS, TRAVEL), "not a number: never");
    }

    /** The ONE radius function: for every tick, a body standing exactly at the drawn radius is struck on that tick. */
    @Test
    void aBodyStandingOnTheDrawnRingIsStruckOnThatVeryTick() {
        for (int t = 0; t <= TRAVEL; t++) {
            double drawn = PulseFront.radiusAt(t, RADIUS, TRAVEL);
            int reach = PulseFront.reachTick(drawn, RADIUS, TRAVEL);
            // tick 0 and tick 1 both draw what they draw; a body at 0 is struck at 0, at 0.4 at 1, and so on
            assertEquals(t, reach, "tick " + t + " draws " + drawn);
        }
    }

    /** The distance is HORIZONTAL: a height difference changes nothing. */
    @Test
    void theDistanceIsHorizontalFeetToFeet() {
        assertEquals(5.0, PulseFront.horizontal(new Vec3(0, 64, 0), new Vec3(3, 70, 4)), 1e-12);
    }

    /** The drawn ring: one centre point at radius 0; at 12 blocks 101 points, every one 12 from the centre, 0.1 up. */
    @Test
    void theRingIsDrawnRoundTheOriginAtGroundLevel() {
        Vec3 origin = new Vec3(10, 64, -5);
        assertEquals(List.of(new Vec3(10, 64.1, -5)), PulseFront.ringPoints(origin, 0));
        List<Vec3> ring = PulseFront.ringPoints(origin, 12);
        assertEquals(101, ring.size());
        for (Vec3 p : ring) {
            assertEquals(12.0, PulseFront.horizontal(origin, p), 1e-9);
            assertEquals(64.1, p.y(), 1e-12);
        }
        assertEquals(8, PulseFront.ringPoints(origin, 0.4).size(), "a small ring is drawn with at least 8");
    }
}
