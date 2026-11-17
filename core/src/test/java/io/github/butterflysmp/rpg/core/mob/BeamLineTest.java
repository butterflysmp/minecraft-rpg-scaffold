package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Knell's beam's line (PLAN-dragon-mobs.md step R5): a segment from the Knell's EYE along the locked direction,
 * tested against a body's box GROWN by the tolerance on every side (cfde822: "only if that player's box grown by 1.0
 * still meets the segment").
 *
 * <p><b>THE QUANTITIES:</b> the tolerance is BLOCKS added to each face of the box; the gap is the smallest such growth
 * at which the segment meets the box. <b>Every coordinate below is a sum of powers of two</b> (0.25, 1.25, 2^-20), so
 * each sum and difference is exact in a double and no row passes or fails on a rounding accident.
 */
class BeamLineTest {

    private static final double TOL = 1.0;
    /** 2^-20 blocks: the "just" in just inside and just outside. */
    private static final double JUST = 1.0 / (1 << 20);

    /** A line along +x at height 1, from x = -8, 16 blocks long. */
    private static final BeamLine ALONG_X = new BeamLine(new Vec3(-8, 1, 0), new Vec3(1, 0, 0), 16);

    /** A body 0.5 wide (half-width 0.25) and 2 tall, its feet at (0, 0, z): the line passes through its height. */
    private static BeamBox bodyAtZ(double z) {
        return BeamBox.standing(new Vec3(0, 0, z), 0.5, 2.0);
    }

    // --- the tolerance's edge ---------------------------------------------------------------------------------------

    @Test
    void aBodyTheLinePassesThroughIsMetWithGapZero() {
        assertTrue(ALONG_X.meets(bodyAtZ(0), 0.0));
        assertEquals(0.0, ALONG_X.gap(bodyAtZ(0)), 1e-9);
    }

    @Test
    void theToleranceWidensEveryFaceByItsBlocks_insideTheEdge() {
        // The body's near face is at z - 0.25: 1.25 - 0.25 = 1.0 from the line, minus 2^-20, so the gap is just
        // under the tolerance.
        BeamBox b = bodyAtZ(1.25 - JUST);
        assertTrue(ALONG_X.meets(b, TOL));
        assertEquals(1.0 - JUST, ALONG_X.gap(b), 1e-9);
    }

    @Test
    void theToleranceEdgeItselfIsInside() {
        BeamBox b = bodyAtZ(1.25);   // gap exactly 1.0
        assertTrue(ALONG_X.meets(b, TOL), "a box grown by 1.0 that touches the segment meets it (inclusive)");
        assertEquals(1.0, ALONG_X.gap(b), 1e-9);
    }

    @Test
    void justOutsideTheToleranceIsMissed() {
        BeamBox b = bodyAtZ(1.25 + JUST);
        assertFalse(ALONG_X.meets(b, TOL));
        assertEquals(1.0 + JUST, ALONG_X.gap(b), 1e-9);
    }

    @Test
    void withNoToleranceTheSameBodyIsMissed() {
        assertFalse(ALONG_X.meets(bodyAtZ(1.25 - JUST), 0.0), "the tolerance, not the body, is what met it");
    }

    @Test
    void theGapIsTheLargestAxisDistance_soADiagonalOffsetIsNotEuclidean() {
        // The body's feet at y = 2, so its bottom face is 1.0 above the line (which runs at y = 1), and its centre at
        // z = 1.25, so its near face is 1.0 off in z (1.25 - 0.25). Euclidean, the nearest corner is sqrt(2) away.
        BeamBox b = BeamBox.standing(new Vec3(0, 2.0, 1.25), 0.5, 2.0);
        // Grown by 1.0 on every side, the box reaches down to y = 1 and in to z = 0: its edge touches the line.
        assertTrue(ALONG_X.meets(b, TOL), "growing every face by 1.0 covers a corner 1.0 off on two axes");
        assertEquals(1.0, ALONG_X.gap(b), 1e-9, "not sqrt(2): the grown box is a box, not a capsule");
    }

    // --- the segment's ends ------------------------------------------------------------------------------------------

    @Test
    void aBodyBeyondTheSegmentsEndIsMissedAndOneAtTheEndIsMet() {
        // The line ends at x = 8. A body whose near face is at x = 9 + 2^-20 is more than 1 beyond the end.
        BeamBox past = BeamBox.standing(new Vec3(9.25 + JUST, 0, 0), 0.5, 2.0);
        assertFalse(ALONG_X.meets(past, TOL));
        BeamBox atEdge = BeamBox.standing(new Vec3(9.25, 0, 0), 0.5, 2.0);   // near face exactly 1.0 past the end
        assertTrue(ALONG_X.meets(atEdge, TOL));
    }

    @Test
    void aBodyBehindTheOriginIsMissed() {
        BeamBox behind = BeamBox.standing(new Vec3(-10, 0, 0), 0.5, 2.0);   // far face at -9.75: 1.75 behind
        assertFalse(ALONG_X.meets(behind, TOL));
    }

    // --- a block stops it ------------------------------------------------------------------------------------------

    @Test
    void aClippedLineDoesNotReachABodyBeyondTheBlock() {
        BeamBox target = bodyAtZ(0);   // at x = 0, 8 blocks along
        assertTrue(ALONG_X.meets(target, TOL));
        BeamLine clipped = ALONG_X.clip(4.0);   // a block 4 blocks along: the segment ends at x = -4
        assertEquals(4.0, clipped.length(), 0.0);
        assertFalse(clipped.meets(target, TOL), "the box's near face is at x = -0.25, 3.75 past the clip");
    }

    @Test
    void clippingNeverLengthensTheLineNorMakesItNegative() {
        assertEquals(16.0, ALONG_X.clip(40.0).length(), 0.0);
        assertEquals(0.0, ALONG_X.clip(-3.0).length(), 0.0);
    }

    // --- aiming -----------------------------------------------------------------------------------------------------

    @Test
    void aimedRunsFromTheOriginThroughThePointForTheGivenLength() {
        BeamLine l = BeamLine.aimed(new Vec3(8, 3, 0), new Vec3(0, 3, 0), 40);
        assertEquals(new Vec3(-1, 0, 0), l.direction());
        assertEquals(40.0, l.length(), 0.0);
        assertEquals(new Vec3(-32, 3, 0), l.end(), "it runs on past the aim point to the range");
    }

    @Test
    void aimingAtTheOriginItselfIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> BeamLine.aimed(Vec3.ZERO, Vec3.ZERO, 40));
    }

    @Test
    void theNearestPointIsClampedToTheSegment() {
        assertEquals(new Vec3(3, 1, 0), ALONG_X.nearestTo(new Vec3(3, 7, 5)));
        assertEquals(new Vec3(-8, 1, 0), ALONG_X.nearestTo(new Vec3(-20, 0, 0)), "before the origin: the origin");
        assertEquals(new Vec3(8, 1, 0), ALONG_X.nearestTo(new Vec3(30, 0, 0)), "past the end: the end");
    }
}
