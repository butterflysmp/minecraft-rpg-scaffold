package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * *** THE SEVEN DIRECTIONS, IN DEGREES, WHICH IS THE UNIT THE REQUIREMENT IS STATED IN. ***
 *
 * <h2>WHY THE VALUES ARE NOT DERIVED IN THE HEAD</h2>
 *
 * <p>Every expected number below was EXECUTED before it was written down. This repository has a
 * standing rule about that and it was earned: three floating-point claims made by reasoning were all
 * wrong in one pass. {@code atan2}'s quadrants and the sign of zero are exactly where that happens.
 *
 * <h2>*** NORTH AND SOUTH ARE THE NO-OP VALUES OF THIS DEFECT. THEIR PASSING IS NOT COVERAGE. ***</h2>
 *
 * <p>The defect being guarded is a body whose rotation was computed in {@code Location.setDirection}'s
 * convention instead of the projectile one. Those two conventions differ by a NEGATED YAW, which is a
 * mirror about the north-south axis: <b>north and south come out identical under both</b>, east and
 * west come out 180 degrees apart, and the diagonal comes out 90 degrees apart. So a suite that
 * checked only north and south would pass with the defect fully present.
 *
 * <p>{@link #theSetDirectionConventionAgreesOnlyOnNorthAndSouth} states that mechanically rather than
 * in a comment, because a comment saying "these two rows are hollow" is the thing this repository
 * keeps finding in place of a check. It is the MUTATION, executed: splice the other convention in and
 * exactly five of the seven rows redden.
 *
 * <p><b>AND IT IS A DIFFERENT NO-OP FROM THE PLATFORM'S OTHER MIRROR, WHICH IS WHY THIS SAYS WHICH.</b>
 * {@code AbstractArrow.tick} flips a {@code noPhysics} arrow's yaw to {@code atan2(-x, -z)} -- a
 * mirror about the EAST-WEST axis, whose no-op values are east and west and which sends a south bolt
 * NORTH. The two defects have opposite blind spots, so "south is the control" is true of one and false
 * of the other. This test is about the first; the boot rows in
 * {@code GATE-arrow-body-orientation.md} are what can see the second.
 */
class BodyRotationTest {

    /** Degrees. Generous relative to a float's precision and far tighter than any real error. */
    private static final float EPS = 1.0e-4f;

    // --- the seven rows -------------------------------------------------------------------------

    @Test
    void southIsYawZeroAndLevel() {
        BodyRotation r = BodyRotation.along(new Vec3(0, 0, 1));
        assertEquals(0.0f, r.yaw(), EPS, "south is +Z and is the projectile convention's zero yaw");
        assertEquals(0.0f, r.pitch(), EPS, "a horizontal shot is level");
    }

    @Test
    void northIsOneHundredAndEighty() {
        BodyRotation r = BodyRotation.along(new Vec3(0, 0, -1));
        assertEquals(180.0f, r.yaw(), EPS,
                "north is -Z. The value is +180 because x is POSITIVE zero here; the platform's own"
                        + " normaliseYaw maps 180 to -180, which is the same heading. A caller must not"
                        + " read anything into the sign.");
        assertEquals(0.0f, r.pitch(), EPS);
    }

    @Test
    void eastIsPlusNinety() {
        BodyRotation r = BodyRotation.along(new Vec3(1, 0, 0));
        assertEquals(90.0f, r.yaw(), EPS,
                "east is +X. setDirection's convention gives 270 for this shot -- the same heading"
                        + " mirrored, and the reason this row is staged rather than north.");
        assertEquals(0.0f, r.pitch(), EPS);
    }

    @Test
    void westIsMinusNinety() {
        BodyRotation r = BodyRotation.along(new Vec3(-1, 0, 0));
        assertEquals(-90.0f, r.yaw(), EPS);
        assertEquals(0.0f, r.pitch(), EPS);
    }

    @Test
    void straightUpIsPitchPlusNinety() {
        BodyRotation r = BodyRotation.along(new Vec3(0, 1, 0));
        assertEquals(90.0f, r.pitch(), EPS,
                "UP IS POSITIVE PITCH IN THIS CONVENTION, and it is negative in the look convention."
                        + " A vertical shot is where that sign error is largest and most visible.");
        assertEquals(0.0f, r.yaw(), EPS,
                "with no horizontal component the yaw is a roll about the body's own axis and carries"
                        + " no information. Pinned at 0 so a future change to it is deliberate.");
    }

    @Test
    void straightDownIsPitchMinusNinety() {
        BodyRotation r = BodyRotation.along(new Vec3(0, -1, 0));
        assertEquals(-90.0f, r.pitch(), EPS, "both, not one: a sign error reaches up and not down");
        assertEquals(0.0f, r.yaw(), EPS);
    }

    @Test
    void southEastIsFortyFive() {
        BodyRotation r = BodyRotation.along(new Vec3(1, 0, 1));
        assertEquals(45.0f, r.yaw(), EPS,
                "THE ONE DIRECTION THAT SEPARATES ALL THREE CANDIDATE CONVENTIONS: 45 here, 315 under"
                        + " setDirection, 225 under the platform's noPhysics flip. No two agree.");
        assertEquals(0.0f, r.pitch(), EPS);
    }

    // --- the contract rows ---------------------------------------------------------------------

    @Test
    void aZeroVelocityHasNoRotationAndSaysSoWithNull() {
        assertNull(BodyRotation.along(Vec3.ZERO),
                "null means NO ANSWER and the caller must skip the write. (0, 0) would be due south"
                        + " and level -- the exact defect this type removes -- arrived at silently.");
    }

    @Test
    void aSpeedIsNeverARotation() {
        BodyRotation unit = BodyRotation.along(new Vec3(1, 0, 1));
        BodyRotation fast = BodyRotation.along(new Vec3(2.5, 0, 2.5));

        assertEquals(unit.yaw(), fast.yaw(), EPS,
                "atan2 reads a ratio, so scaling the velocity cannot move the rotation. 2.5 is the"
                        + " Plume's authored speed, i.e. the vector this actually sees in flight.");
        assertEquals(unit.pitch(), fast.pitch(), EPS);
    }

    @Test
    void aVerticalVelocityIsNotAZeroVelocity() {
        assertEquals(90.0f, BodyRotation.along(new Vec3(0, 2.5, 0)).pitch(), EPS,
                "straight up has zero HORIZONTAL length and non-zero length. The guard is on the"
                        + " vector, not on its horizontal part, so this must not take the null path.");
    }

    // --- the mutation, executed rather than described --------------------------------------------

    /**
     * *** THE MUTATION. SPLICE {@code Location.setDirection}'S CONVENTION IN AND NAME WHAT REDDENS. ***
     *
     * <p>Five of the seven rows above redden: EAST, WEST, UP, DOWN and SOUTH-EAST. <b>NORTH and SOUTH
     * stay GREEN</b>, because the two conventions differ by a negated yaw and those are the two
     * headings a negated yaw leaves alone. That is stated as an assertion here so that the hollowness
     * of those two rows is a fact the suite knows, not a warning in a comment nobody re-reads.
     *
     * <p>{@link #lookConventionYaw} is a verbatim transcription of {@code Location.setDirection} from
     * the pinned paper-api SOURCE jar, kept in the test so the comparison is against the real rival
     * and not against a remembered version of it.
     */
    @Test
    void theSetDirectionConventionAgreesOnlyOnNorthAndSouth() {
        Vec3 south = new Vec3(0, 0, 1);
        Vec3 north = new Vec3(0, 0, -1);
        Vec3 east = new Vec3(1, 0, 0);
        Vec3 west = new Vec3(-1, 0, 0);
        Vec3 southEast = new Vec3(1, 0, 1);

        // the survivors, and the reason the staging above is not "whatever direction I was facing"
        assertEquals(BodyRotation.along(south).yaw(), lookConventionYaw(south), EPS,
                "SOUTH IS A NO-OP OF THIS DEFECT. A body staged south reads as a clean pass with the"
                        + " wrong convention fully in place.");
        assertEquals(BodyRotation.along(north).yaw(), Math.abs(lookConventionYaw(north)), EPS,
                "NORTH IS THE OTHER NO-OP: 180 and -180 are one heading, so it cannot fail either."
                        + " Compared through abs() for exactly that reason.");

        // and the five that cannot survive it
        assertNotEquals(BodyRotation.along(east).yaw(), lookConventionYaw(east),
                "east must disagree: 90 against 270");
        assertNotEquals(BodyRotation.along(west).yaw(), lookConventionYaw(west),
                "west must disagree: -90 against 90");
        assertNotEquals(BodyRotation.along(southEast).yaw(), lookConventionYaw(southEast),
                "south-east must disagree: 45 against 315");
        assertNotEquals(BodyRotation.along(new Vec3(0, 1, 0)).pitch(), lookConventionPitch(0, 1, 0),
                "up must disagree: +90 against -90");
        assertNotEquals(BodyRotation.along(new Vec3(0, -1, 0)).pitch(), lookConventionPitch(0, -1, 0),
                "down must disagree: -90 against +90");
    }

    /**
     * {@code Location.setDirection}'s yaw, transcribed from the pinned paper-api sources jar.
     * <b>Not production code and must never become any.</b> It exists so the mutation is a call
     * rather than an edit.
     */
    private static float lookConventionYaw(Vec3 v) {
        double twoPi = 2 * Math.PI;
        double theta = Math.atan2(-v.x(), v.z());
        return (float) Math.toDegrees((theta + twoPi) % twoPi);
    }

    /** {@code Location.setDirection}'s pitch, including its zero-horizontal early return. */
    private static float lookConventionPitch(double x, double y, double z) {
        if (x == 0 && z == 0) return y > 0 ? -90.0f : 90.0f;
        return (float) Math.toDegrees(Math.atan(-y / Math.sqrt(x * x + z * z)));
    }
}
