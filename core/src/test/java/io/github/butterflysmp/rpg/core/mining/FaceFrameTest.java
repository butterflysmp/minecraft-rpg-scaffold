package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall frame: which blocks a slot covers, and where a look ray lands in patch pixels.
 *
 * <p>Every case stages a point whose {@code u} and {@code v} DIFFER (28 against 12, 20 against
 * 12), so a swap of the two axes, or a sign flip on the right axis, cannot pass by coincidence.
 * The anchor is the bottom-centre block as the player sees the face (PLAN-mining.md 5.1).
 */
class FaceFrameTest {

    private static final double EPS = 1e-9;

    private static FaceFrame small(WallFace face) {
        return new FaceFrame(10, 64, 20, face, 3, 2);
    }

    private static PatchPoint hit(FaceFrame frame, double ex, double ey, double ez,
                                  double dx, double dy, double dz) {
        Optional<PatchPoint> p = frame.worldToPatch(ex, ey, ez, dx, dy, dz);
        assertTrue(p.isPresent(), "the ray should land on the plane");
        return p.get();
    }

    @Test
    void northFaceRightIsMinusXAndThePlaneIsTheBlocksLowZ() {
        // The player stands NORTH of the wall, facing +Z (south). West (-X) is
        // on their right, so the patch's left edge is the footprint's high-X edge, x = 12.
        PatchPoint p = hit(small(WallFace.NORTH), 10.25, 64.75, 17.0, 0, 0, 1);
        assertEquals(28.0, p.u(), EPS, "u = (12 - 10.25) * 16");
        assertEquals(12.0, p.v(), EPS, "v = (64.75 - 64) * 16");
        // Mutation: right axis +X for NORTH -> u reads -28 -> reddens.
    }

    @Test
    void southFaceRightIsPlusXAndThePlaneIsTheBlocksHighZ() {
        PatchPoint p = hit(small(WallFace.SOUTH), 10.25, 64.75, 24.0, 0, 0, -1);
        assertEquals(20.0, p.u(), EPS, "left edge x = 9, u = (10.25 - 9) * 16");
        assertEquals(12.0, p.v(), EPS);
    }

    @Test
    void eastFaceRightIsMinusZ() {
        PatchPoint p = hit(small(WallFace.EAST), 14.0, 64.75, 20.25, -1, 0, 0);
        assertEquals(28.0, p.u(), EPS, "left edge z = 22, u = (22 - 20.25) * 16");
        assertEquals(12.0, p.v(), EPS);
    }

    @Test
    void westFaceRightIsPlusZ() {
        PatchPoint p = hit(small(WallFace.WEST), 6.0, 64.75, 20.25, 1, 0, 0);
        assertEquals(20.0, p.u(), EPS, "left edge z = 19, u = (20.25 - 19) * 16");
        assertEquals(12.0, p.v(), EPS);
    }

    @Test
    void anObliqueRayLandsWhereTheLineMeetsThePlane() {
        // From (12, 66, 18) along (-0.5, -0.5, 1): t = 2 reaches z = 20 at (11, 65, 20).
        PatchPoint p = hit(small(WallFace.NORTH), 12.0, 66.0, 18.0, -0.5, -0.5, 1.0);
        assertEquals(16.0, p.u(), EPS, "u = (12 - 11) * 16");
        assertEquals(16.0, p.v(), EPS, "v = (65 - 64) * 16");
        // Mutation: drop the t scaling (use the direction as a displacement) -> reddens.
    }

    @Test
    void aRayPointingAwayFromTheWallLandsNowhere() {
        assertTrue(small(WallFace.NORTH).worldToPatch(10.5, 65, 17, 0, 0, -1).isEmpty());
    }

    @Test
    void aRayParallelToTheWallLandsNowhere() {
        assertTrue(small(WallFace.NORTH).worldToPatch(10.5, 65, 17, 1, 0, 0).isEmpty());
    }

    @Test
    void aWallBehindTheEyeIsNotHit() {
        // The eye is past the plane (z = 23 > 20) and looks further +Z: the line meets the plane
        // only at a negative t, behind the eye.
        assertTrue(small(WallFace.NORTH).worldToPatch(10.5, 65, 23, 0, 0, 1).isEmpty());
        // Mutation: accept t <= 0 -> a point is returned -> reddens.
    }

    @Test
    void theFootprintIsTheAnchorAndOneEitherSideTwoHigh() {
        List<BlockPos> blocks = small(WallFace.NORTH).footprint();
        assertEquals(List.of(
                new BlockPos(11, 64, 20), new BlockPos(10, 64, 20), new BlockPos(9, 64, 20),
                new BlockPos(11, 65, 20), new BlockPos(10, 65, 20), new BlockPos(9, 65, 20)),
                blocks, "row by row from the bottom, each row left to right as the player sees it");
    }

    @Test
    void anEastFootprintRunsAlongZ() {
        List<BlockPos> blocks = small(WallFace.EAST).footprint();
        assertEquals(List.of(
                new BlockPos(10, 64, 21), new BlockPos(10, 64, 20), new BlockPos(10, 64, 19),
                new BlockPos(10, 65, 21), new BlockPos(10, 65, 20), new BlockPos(10, 65, 19)),
                blocks);
    }
}
