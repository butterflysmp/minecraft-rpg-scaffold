package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Patch pixels back to the world, for placing the displays (PLAN-mining.md 5.4), and each face's
 * display rotation.
 *
 * <p>The headline is the ROUND TRIP: a point placed by {@code patchToWorld} and looked at straight
 * along the normal must come back as the same patch point through {@code worldToPatch}. The two
 * mappings cannot then disagree about which way u runs, on any face.
 */
class FaceFrameWorldTest {

    private static final double EPS = 1e-9;

    @Test
    void patchToWorldAndWorldToPatchAgreeOnEveryFace() {
        for (WallFace face : WallFace.values()) {
            FaceFrame frame = new FaceFrame(10, 64, 20, face, 3, 2);
            double[] w = frame.patchToWorld(28.5, 12.5, 0.0);
            // Step back 3 blocks along the normal and look straight at the wall.
            double ex = w[0] + 3 * face.nx, ez = w[2] + 3 * face.nz;
            Optional<PatchPoint> back = frame.worldToPatch(ex, w[1], ez, -face.nx, 0, -face.nz);
            assertTrue(back.isPresent(), face.name());
            assertEquals(28.5, back.get().u(), EPS, face + " u");
            assertEquals(12.5, back.get().v(), EPS, face + " v");
        }
        // Mutation: drop face.rx from patchToWorld's u term -> u comes back 0 on N/S -> reddens.
    }

    @Test
    void theOutsetMovesThePointOffTheWallAlongTheNormal() {
        FaceFrame north = new FaceFrame(10, 64, 20, WallFace.NORTH, 3, 2);
        assertArrayEquals(new double[]{12 - 28.5 / 16, 64 + 12.5 / 16, 20 - 0.001},
                north.patchToWorld(28.5, 12.5, 0.001), EPS);
        FaceFrame east = new FaceFrame(10, 64, 20, WallFace.EAST, 3, 2);
        assertArrayEquals(new double[]{11 + 0.001, 64 + 12.5 / 16, 22 - 28.5 / 16},
                east.patchToWorld(28.5, 12.5, 0.001), EPS);
    }

    @Test
    void theCentreIsTheMiddleOfTheFootprintFace() {
        FaceFrame north = new FaceFrame(10, 64, 20, WallFace.NORTH, 3, 2);
        assertArrayEquals(new double[]{10.5, 65.0, 20 - 0.0005}, north.centre(0.0005), EPS,
                "3 wide around x = 10.5, 2 high from y = 64");
    }

    @Test
    void displayRotationsTurnTheModelsSouthFaceToEachWall() {
        float h = (float) Math.sqrt(0.5);
        assertArrayEquals(new float[]{0f, 0f, 0f, 1f}, WallFace.SOUTH.displayRotation(), 1e-6f);
        assertArrayEquals(new float[]{0f, 1f, 0f, 0f}, WallFace.NORTH.displayRotation(), 1e-6f);
        assertArrayEquals(new float[]{0f, h, 0f, h}, WallFace.EAST.displayRotation(), 1e-6f);
        assertArrayEquals(new float[]{0f, -h, 0f, h}, WallFace.WEST.displayRotation(), 1e-6f);
        // Each is a unit quaternion.
        for (WallFace f : WallFace.values()) {
            float[] q = f.displayRotation();
            assertEquals(1.0, q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3], 1e-6, f.name());
        }
    }
}
