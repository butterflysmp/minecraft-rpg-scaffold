package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.ChunkTraversal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure vector arithmetic. No world, no clock, no chunks -- just where the planes are. */
class ChunkTraversalTest {

    private static final double EPS = 1e-6;

    private static List<Vec3> endpoints(Vec3 origin, Vec3 direction, double range) {
        // Through Aim, because Aim normalises, and the traversal relies on a unit vector.
        Aim aim = new Aim(origin, direction);
        return ChunkTraversal.segmentEndpoints(aim.origin(), aim.direction(), range);
    }

    private static boolean onAPlane(double coordinate) {
        return Math.abs(coordinate - 16 * Math.round(coordinate / 16.0)) < EPS;
    }

    /**
     * The properties that make the whole scheme sound, asserted for every case: each
     * interior endpoint sits on a plane, distances strictly increase, no two endpoints
     * coincide, and the walk ends exactly at range.
     */
    private static void assertWellFormed(Vec3 origin, Vec3 direction, double range, List<Vec3> ends) {
        Aim aim = new Aim(origin, direction);

        assertFalse(ends.isEmpty(), "there is always a final endpoint");

        Vec3 expectedEnd = aim.pointAt(range);
        Vec3 actualEnd = ends.get(ends.size() - 1);
        assertEquals(0, actualEnd.subtract(expectedEnd).length(), 1e-9, "must end at range");

        double previousDistance = 0;
        for (int i = 0; i < ends.size(); i++) {
            double distance = ends.get(i).subtract(origin).length();
            assertTrue(distance > previousDistance + 1e-9,
                    "endpoints must strictly advance; #" + i + " did not");
            previousDistance = distance;

            if (i < ends.size() - 1) {
                Vec3 e = ends.get(i);
                assertTrue(onAPlane(e.x()) || onAPlane(e.z()),
                        "interior endpoint " + i + " is not on a chunk plane: " + e);
            }
        }
    }

    @Test
    void axisAlignedRayCrossesOnePlane() {
        var ends = endpoints(Vec3.ZERO, new Vec3(1, 0, 0), 30);

        assertWellFormed(Vec3.ZERO, new Vec3(1, 0, 0), 30, ends);
        assertEquals(2, ends.size(), "x=16 then the end at x=30");
        assertEquals(16, ends.get(0).x(), EPS);
        assertEquals(30, ends.get(1).x(), EPS);
    }

    /**
     * An origin at x=0 lies ON a plane. Travelling +x, the next crossing is 16, not 0 --
     * otherwise the first segment would have zero length and the walk would never advance.
     */
    @Test
    void anOriginOnAPlaneDoesNotProduceAZeroLengthSegment() {
        var ends = endpoints(new Vec3(32.0, 64, 5), new Vec3(1, 0, 0), 30);
        assertWellFormed(new Vec3(32.0, 64, 5), new Vec3(1, 0, 0), 30, ends);
        assertEquals(48, ends.get(0).x(), EPS, "the plane at 32 is behind us");

        var backwards = endpoints(new Vec3(32.0, 64, 5), new Vec3(-1, 0, 0), 30);
        assertWellFormed(new Vec3(32.0, 64, 5), new Vec3(-1, 0, 0), 30, backwards);
        assertEquals(16, backwards.get(0).x(), EPS, "travelling -x, the next plane is 16");
    }

    /**
     * A 45-degree ray from the origin crosses an x-plane and a z-plane at the SAME instant
     * -- it goes through the corner at (16,16), which it reaches at t = 16*sqrt(2) = 22.627
     * blocks along, not at t = 16. The two crossings must collapse to one endpoint.
     */
    @Test
    void aCornerCrossingCollapsesToASingleEndpoint() {
        var dir = new Vec3(1, 0, 1);
        var ends = endpoints(Vec3.ZERO, dir, 30);

        assertWellFormed(Vec3.ZERO, dir, 30, ends);
        assertEquals(2, ends.size(), "the corner is one endpoint, not two");
        assertEquals(16, ends.get(0).x(), EPS);
        assertEquals(16, ends.get(0).z(), EPS);
        assertEquals(16 * Math.sqrt(2), ends.get(0).length(), EPS, "the corner is 22.627 away");
    }

    /**
     * The case that matters. Both lattice examples above cross x and z at the same instant,
     * so they cannot tell a correct DDA from one that is off by a plane. From a non-lattice
     * origin the two axes cross at different distances, in a definite order.
     *
     * Verified independently, to five decimals: z at t=9.61665, then x at t=17.67767.
     */
    @Test
    void aDiagonalFromANonLatticeOriginCrossesEachAxisSeparately() {
        var origin = new Vec3(3.5, 0, 9.2);
        var dir = new Vec3(1, 0, 1);

        var ends = endpoints(origin, dir, 30);
        assertWellFormed(origin, dir, 30, ends);

        assertEquals(3, ends.size(), "z-plane, then x-plane, then the end");

        // t = 9.61665 -- z reaches 16 first, because it started at 9.2, nearer its plane.
        assertEquals(10.3, ends.get(0).x(), EPS);
        assertEquals(16.0, ends.get(0).z(), EPS);

        // t = 17.67767 -- now x reaches 16.
        assertEquals(16.0, ends.get(1).x(), EPS);
        assertEquals(21.7, ends.get(1).z(), EPS);

        // t = 30.
        assertEquals(24.7132034, ends.get(2).x(), 1e-6);
        assertEquals(30.4132034, ends.get(2).z(), 1e-6);
    }

    /** Height does not decide region ownership, so a vertical ray never leaves its column. */
    @Test
    void aVerticalRayCrossesNoPlanes() {
        var origin = new Vec3(5, 64, 5);
        var ends = endpoints(origin, new Vec3(0, 1, 0), 30);

        assertWellFormed(origin, new Vec3(0, 1, 0), 30, ends);
        assertEquals(1, ends.size(), "one segment, resolved inline, no scheduled ticks");
        assertEquals(94, ends.get(0).y(), EPS);
    }

    /** Negative coordinates and negative directions use the same planes, in reverse. */
    @Test
    void negativeCoordinatesCrossThePlanesToo() {
        var origin = new Vec3(-3, 0, -3);
        var ends = endpoints(origin, new Vec3(-1, 0, 0), 30);

        assertWellFormed(origin, new Vec3(-1, 0, 0), 30, ends);
        assertEquals(-16, ends.get(0).x(), EPS);
        assertEquals(-33, ends.get(ends.size() - 1).x(), EPS);
    }

    /** A ray shorter than its own chunk column is a single inline segment. */
    @Test
    void aShortRayStaysInOneColumn() {
        var origin = new Vec3(4, 64, 4);
        var ends = endpoints(origin, new Vec3(1, 0, 0), 5);

        assertEquals(1, ends.size());
        assertEquals(9, ends.get(0).x(), EPS);
    }

    @Test
    void columnOfIsNegativeSafe() {
        assertEquals(0, ChunkTraversal.columnOf(0.0));
        assertEquals(0, ChunkTraversal.columnOf(15.9));
        assertEquals(1, ChunkTraversal.columnOf(16.0));
        assertEquals(-1, ChunkTraversal.columnOf(-0.1));
        assertEquals(-1, ChunkTraversal.columnOf(-16.0));
        assertEquals(-2, ChunkTraversal.columnOf(-16.1));
    }
}
