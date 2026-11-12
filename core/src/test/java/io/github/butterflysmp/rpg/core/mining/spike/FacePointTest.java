package io.github.butterflysmp.rpg.core.mining.spike;

import io.github.butterflysmp.rpg.core.mining.spike.FacePoint.Aim;
import io.github.butterflysmp.rpg.core.mining.spike.FacePoint.Face;
import io.github.butterflysmp.rpg.core.mining.spike.FacePoint.Spot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacePointTest {

    private static final double EPS = 1e-9;

    @Test
    void aHitSnapsToThePixelCentreItLandsIn() {
        // South face of block (0,64,0) is the plane z=1. A hit at u=0.40 (pixel 6.4 -> 6), v=0.71
        // (pixel 11.36 -> 11). The centre is (6.5/16, 64 + 11.5/16) and sits OUTSET in front of z=1.
        Spot s = FacePoint.snap(0, 64, 0, Face.SOUTH, 0.40, 64.71, 1.0);
        assertEquals(6, s.pu());
        assertEquals(11, s.pv());
        assertEquals(6.5 / 16, s.x(), EPS);
        assertEquals(64 + 11.5 / 16, s.y(), EPS);
        assertEquals(1.0 + FacePoint.OUTSET, s.z(), EPS);
    }

    @Test
    void theCentreIsClampedSoTheFiveByFiveNeverOverhangs() {
        // Pixel 0 and pixel 15 both clamp: 2 is the lowest k with k-2 >= 0, and 13 the highest with k+2 <= 15.
        Spot low = FacePoint.snap(0, 0, 0, Face.SOUTH, 0.01, 0.01, 1.0);
        Spot high = FacePoint.snap(0, 0, 0, Face.SOUTH, 0.99, 0.99, 1.0);
        assertEquals(2, low.pu());
        assertEquals(2, low.pv());
        assertEquals(13, high.pu());
        assertEquals(13, high.pv());
    }

    @Test
    void theSpotSitsOutsideTheBlockOnEveryFace() {
        // Mutation it forces red: an inverted normal sign puts the spot INSIDE the block, where it is never seen.
        for (Face f : Face.values()) {
            Spot s = FacePoint.snap(10, 20, 30, f, 10.5, 20.5, 30.5);
            double[] c = {s.x(), s.y(), s.z()};
            double[] blockMin = {10, 20, 30};
            int axis = f.nx != 0 ? 0 : f.ny != 0 ? 1 : 2;
            int sign = f.nx + f.ny + f.nz;
            double expected = sign > 0 ? blockMin[axis] + 1 + FacePoint.OUTSET : blockMin[axis] - FacePoint.OUTSET;
            assertEquals(expected, c[axis], EPS, f.name());
        }
    }

    @Test
    void negativeCoordinatesSnapByFloorNotTruncation() {
        // Block -5 spans x in [-5, -4). A hit at x=-4.70 is local 0.30, pixel 4 (not pixel 3, which
        // (int)(-4.70*16) truncation arithmetic would give relative to the wrong origin).
        Spot s = FacePoint.snap(-5, 64, -3, Face.NORTH, -4.70, 64.5, -3.0);
        assertEquals(4, s.pu());
        assertEquals(-5 + 4.5 / 16, s.x(), EPS);
        assertEquals(-3.0 - FacePoint.OUTSET, s.z(), EPS);
    }

    @Test
    void theBoxEdgeIsInsideAndJustPastItIsOut() {
        Spot s = FacePoint.snap(0, 64, 0, Face.SOUTH, 0.5, 64.5, 1.0);   // centre (8.5/16, 64+8.5/16)
        Aim edge = FacePoint.aim(s, s.x() + 2.5 / 16, s.y() - 2.5 / 16, 1.0);
        Aim past = FacePoint.aim(s, s.x() + 2.6 / 16, s.y(), 1.0);
        assertEquals(2.5, edge.du(), 1e-9);
        assertEquals(-2.5, edge.dv(), 1e-9);
        assertTrue(edge.inBox());
        assertFalse(past.inBox());
    }

    @Test
    void theFaceCentreSitsOnTheFacePlaneByTheOutset() {
        // Block (3,70,-8). EAST is the plane x=4, WEST x=3, UP y=71, DOWN y=70, SOUTH z=-7, NORTH z=-8.
        // Mutation it forces red: using the block min on a positive face puts the patch inside the block.
        double o = 0.0005;
        assertArrayEquals(new double[]{4 + o, 70.5, -7.5}, FacePoint.centre(3, 70, -8, Face.EAST, o), EPS);
        assertArrayEquals(new double[]{3 - o, 70.5, -7.5}, FacePoint.centre(3, 70, -8, Face.WEST, o), EPS);
        assertArrayEquals(new double[]{3.5, 71 + o, -7.5}, FacePoint.centre(3, 70, -8, Face.UP, o), EPS);
        assertArrayEquals(new double[]{3.5, 70 - o, -7.5}, FacePoint.centre(3, 70, -8, Face.DOWN, o), EPS);
        assertArrayEquals(new double[]{3.5, 70.5, -7 + o}, FacePoint.centre(3, 70, -8, Face.SOUTH, o), EPS);
        assertArrayEquals(new double[]{3.5, 70.5, -8 - o}, FacePoint.centre(3, 70, -8, Face.NORTH, o), EPS);
    }

    @Test
    void theAimTextIsSignedToTwoPlaces() {
        assertEquals("+0.75,-1.25,in", new Aim(0.75, -1.25, true).text());
        assertEquals("-3.00,+0.00,out", new Aim(-3.0, 0.0, false).text());
    }

    @Test
    void eachRotationTurnsPlusZOntoTheFaceNormal() {
        // Mutation it forces red: a sign slip on any one face (e.g. EAST and WEST swapped).
        for (Face f : Face.values()) {
            float[] q = FacePoint.rotation(f);
            double norm = q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3];
            assertEquals(1.0, norm, 1e-6, f + " is not a unit quaternion");
            double[] v = rotate(q, 0, 0, 1);
            assertEquals(f.nx, v[0], 1e-6, f + " x");
            assertEquals(f.ny, v[1], 1e-6, f + " y");
            assertEquals(f.nz, v[2], 1e-6, f + " z");
        }
    }

    /** v' = q v q*, for a unit quaternion {x, y, z, w}. */
    private static double[] rotate(float[] q, double vx, double vy, double vz) {
        double x = q[0], y = q[1], z = q[2], w = q[3];
        // t = 2 * cross(q.xyz, v); v' = v + w t + cross(q.xyz, t)
        double tx = 2 * (y * vz - z * vy), ty = 2 * (z * vx - x * vz), tz = 2 * (x * vy - y * vx);
        return new double[]{
                vx + w * tx + (y * tz - z * ty),
                vy + w * ty + (z * tx - x * tz),
                vz + w * tz + (x * ty - y * tx)};
    }
}
