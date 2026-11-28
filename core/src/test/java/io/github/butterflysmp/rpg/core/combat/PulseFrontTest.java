package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * M1 (Ben, 2026-11-28 via the seat: "double the speed the circle goes out"): SMOLDER'S NEW TRAVEL, 15 ticks for the
     * same 12 blocks, 0.8 a tick. The seat's distances, executed: 0 -> 0, 3 -> 4, 6 -> 8, 11.5 -> 15, 12 -> 15, 12.5 ->
     * never. Neighbours that pin the rounding: 3.2 (exactly tick 4's radius) -> 4, 3.21 -> 5, 11.2 (tick 14's) -> 14.
     */
    @Test
    void smoldersNewTravelOfFifteenTicksReachesEachDistanceOnTheExecutedTick() {
        double[][] table = {
                {0.0, 0}, {3.0, 4}, {3.2, 4}, {3.21, 5}, {6.0, 8}, {11.2, 14}, {11.5, 15}, {12.0, 15}, {12.5, -1},
        };
        for (double[] row : table) {
            assertEquals((int) row[1], PulseFront.reachTick(row[0], RADIUS, 15), "distance " + row[0]);
        }
    }

    /**
     * N3 (the seat, 2026-11-28): A TRAVEL OF 0 IS INSTANT. Every distance up to the radius reaches on tick 0 (5.9 and 6.0
     * included), one hair past it never; the radius is whole from tick 0, and 0 before it. READ BEFORE THIS ROW: 0 was
     * REFUSED by {@code CastSpec.Pulses} ("travel_ticks must be 1 or more"), and {@code radiusAt(0, r, 0)} was 0, so
     * only a body at distance 0 would have been struck; both changed here.
     */
    @Test
    void aTravelOfZeroStrikesEveryDistanceUpToTheRadiusOnTickZero() {
        double[][] table = {{0.0, 0}, {3.0, 0}, {5.9, 0}, {6.0, 0}, {6.1, -1}};
        for (double[] row : table) assertEquals((int) row[1], PulseFront.reachTick(row[0], 6.0, 0), "distance " + row[0]);
        assertEquals(6.0, PulseFront.radiusAt(0, 6.0, 0));
        assertEquals(6.0, PulseFront.radiusAt(5, 6.0, 0));
        assertEquals(0.0, PulseFront.radiusAt(-1, 6.0, 0));
    }

    // ================================================================= M2: the spiral, a drawing of the same front

    /**
     * EVERY SPIRAL POINT LIES ON AN ARM AT THE FRONT'S OWN RADIUS (M2): for every tick of the travel, each point drawn
     * between the previous tick's radius and this tick's is at angle {@code a0 + 2 pi x turns x (r / radius) + k x 120
     * degrees} for some arm k, 0.1 above the origin, and its distance from the origin is in {@code (radiusAt(t - 1),
     * radiusAt(t)]}. The tick's outermost point per arm is AT {@code radiusAt(t)}, the radius the hit rule compares.
     */
    @Test
    void everySpiralPointLiesOnAnArmBetweenTheFrontsLastRadiusAndItsNewOne() {
        Vec3 origin = new Vec3(10, 64, -5);
        for (int t = 1; t <= 15; t++) {
            double from = PulseFront.radiusAt(t - 1, RADIUS, 15);
            double to = PulseFront.radiusAt(t, RADIUS, 15);
            List<Vec3> points = PulseFront.spiralPoints(origin, from, to, 3, 1.0, RADIUS);
            double[] outermost = new double[3];
            for (Vec3 p : points) {
                double r = PulseFront.horizontal(origin, p);
                assertTrue(r > from - 1e-9 && r <= to + 1e-9, "tick " + t + ": r " + r + " outside (" + from + ", " + to + "]");
                assertEquals(64.1, p.y(), 1e-12);
                double drawn = Math.atan2(p.z() - origin.z(), p.x() - origin.x());
                int arm = -1;
                for (int k = 0; k < 3; k++) {
                    double expected = PulseFront.spiralAngle(k, 3, 1.0, r, RADIUS);
                    if (Math.abs(Math.IEEEremainder(drawn - expected, 2 * Math.PI)) < 1e-9) arm = k;
                }
                assertTrue(arm >= 0, "tick " + t + ": a point at r " + r + " lies on no arm");
                outermost[arm] = Math.max(outermost[arm], r);
            }
            for (int k = 0; k < 3; k++) assertEquals(to, outermost[k], 1e-9, "tick " + t + " arm " + k + "'s tip");
        }
    }

    /**
     * THE FORMULA ITSELF, AT FOUR POINTS (a0 = 0, due east; the angle grows from +x toward +z, which is CLOCKWISE seen
     * from above in Minecraft's axes, x east and z south): arm 0 at r 0 is 0; at r 6 (half the radius, one turn in all)
     * half a turn, pi; arm 1 is 120 degrees on; at the edge one whole turn.
     */
    @Test
    void theArmAngleIsAZeroPlusTurnsTimesTheFractionOfTheRadiusPlusTheArmsShare() {
        assertEquals(0.0, PulseFront.spiralAngle(0, 3, 1.0, 0.0, RADIUS), 1e-12);
        assertEquals(Math.PI, PulseFront.spiralAngle(0, 3, 1.0, 6.0, RADIUS), 1e-12);
        assertEquals(Math.PI + 2 * Math.PI / 3, PulseFront.spiralAngle(1, 3, 1.0, 6.0, RADIUS), 1e-12);
        assertEquals(2 * Math.PI, PulseFront.spiralAngle(0, 3, 1.0, 12.0, RADIUS), 1e-12);
    }

    /**
     * SPACED SO AN ARM READS AS A LINE (M2, CC'S CALL 0.4 blocks along the arm): no two consecutive points of one arm,
     * across the WHOLE travel, are more than {@link PulseFront#SPIRAL_SPACING_BLOCKS} plus 5% apart in a straight line;
     * and tick 0 draws the one centre point.
     */
    @Test
    void consecutivePointsOfOneArmAreNoFurtherApartThanTheSpacing() {
        Vec3 origin = Vec3.ZERO;
        assertEquals(List.of(new Vec3(0, 0.1, 0)), PulseFront.spiralPoints(origin, 0.0, 0.0, 3, 1.0, RADIUS));
        for (int k = 0; k < 3; k++) {
            Vec3 last = new Vec3(0, 0.1, 0);
            for (int t = 1; t <= 15; t++) {
                List<Vec3> points = PulseFront.spiralPoints(origin, PulseFront.radiusAt(t - 1, RADIUS, 15),
                        PulseFront.radiusAt(t, RADIUS, 15), 3, 1.0, RADIUS);
                int perArm = points.size() / 3;
                for (int i = 0; i < perArm; i++) {
                    Vec3 p = points.get(k * perArm + i);
                    assertTrue(p.subtract(last).length() <= PulseFront.SPIRAL_SPACING_BLOCKS * 1.05,
                            "arm " + k + " tick " + t + ": a gap of " + p.subtract(last).length());
                    last = p;
                }
            }
        }
    }

    /**
     * V1 (the seat, 2026-11-28; Ben: "Can we try 9 spirals"): AT NINE ARMS, ONLY THE ARM COUNT MOVES. Every point of
     * every tick lies on one of nine arms, 40 degrees apart ({@code k x 2 pi / 9}), at the front's radius band; each arm's
     * tip is AT {@code radiusAt(t)}; each arm draws as many points as an arm of three does (the spacing is per arm); and
     * no two consecutive points of one arm are more than the spacing (plus 5%) apart. a0, the hand and the turns are the
     * same function's, so arm 0 is unchanged.
     */
    @Test
    void atNineArmsEveryPointLiesOnOneOfNineArmsAndEachArmIsDrawnAsAThreeArmSpiralsArmIs() {
        Vec3 origin = new Vec3(10, 64, -5);
        Vec3[] last = new Vec3[9];
        java.util.Arrays.fill(last, new Vec3(10, 64.1, -5));
        for (int t = 1; t <= 15; t++) {
            double from = PulseFront.radiusAt(t - 1, RADIUS, 15);
            double to = PulseFront.radiusAt(t, RADIUS, 15);
            List<Vec3> points = PulseFront.spiralPoints(origin, from, to, 9, 1.0, RADIUS);
            List<Vec3> threeArms = PulseFront.spiralPoints(origin, from, to, 3, 1.0, RADIUS);
            assertEquals(3 * threeArms.size(), points.size(), "tick " + t + ": each arm as many points as at three");
            int perArm = points.size() / 9;
            for (int k = 0; k < 9; k++) {
                for (int i = 0; i < perArm; i++) {
                    Vec3 p = points.get(k * perArm + i);
                    double r = PulseFront.horizontal(origin, p);
                    assertTrue(r > from - 1e-9 && r <= to + 1e-9, "tick " + t + " arm " + k + ": r " + r);
                    double drawn = Math.atan2(p.z() - origin.z(), p.x() - origin.x());
                    double expected = PulseFront.spiralAngle(k, 9, 1.0, r, RADIUS);
                    assertTrue(Math.abs(Math.IEEEremainder(drawn - expected, 2 * Math.PI)) < 1e-9, "tick " + t + " arm " + k);
                    assertTrue(p.subtract(last[k]).length() <= PulseFront.SPIRAL_SPACING_BLOCKS * 1.05,
                            "arm " + k + " tick " + t + ": a gap of " + p.subtract(last[k]).length());
                    last[k] = p;
                }
                assertEquals(to, PulseFront.horizontal(origin, points.get(k * perArm + perArm - 1)), 1e-9, "tick " + t + " arm " + k + "'s tip");
            }
            for (int i = 0; i < perArm; i++) {
                assertEquals(threeArms.get(i), points.get(i), "tick " + t + ": arm 0 is the same at nine arms as at three");
            }
        }
        assertEquals(2 * Math.PI / 9, PulseFront.spiralAngle(1, 9, 1.0, 0.0, RADIUS), 1e-12, "the arms are 40 degrees apart");
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
