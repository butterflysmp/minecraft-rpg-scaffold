package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.Aim;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Vec3Test {

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertAll(
                () -> assertEquals(expected.x(), actual.x(), 1e-9, "x"),
                () -> assertEquals(expected.y(), actual.y(), 1e-9, "y"),
                () -> assertEquals(expected.z(), actual.z(), 1e-9, "z"));
    }

    @Test
    void subtractIsTheInverseOfAdd() {
        var a = new Vec3(1, 2, 3);
        var b = new Vec3(0.5, -2, 7);
        assertVec(a, a.add(b).subtract(b));
    }

    @Test
    void lengthOfAThreeFourFiveTriangle() {
        assertEquals(5.0, new Vec3(3, 4, 0).length(), 1e-9);
        assertEquals(25.0, new Vec3(3, 4, 0).lengthSquared(), 1e-9);
    }

    @Test
    void normalizeProducesAUnitVector() {
        assertEquals(1.0, new Vec3(3, 4, 0).normalize().length(), 1e-9);
        assertVec(new Vec3(0.6, 0.8, 0), new Vec3(3, 4, 0).normalize());
    }

    /** A player looking straight at their own feet can produce a zero vector. */
    @Test
    void normalizingZeroYieldsZeroNotNaN() {
        Vec3 normalized = Vec3.ZERO.normalize();
        assertVec(Vec3.ZERO, normalized);
        assertFalse(Double.isNaN(normalized.x()));
    }

    @Test
    void dotOfPerpendicularVectorsIsZero() {
        assertEquals(0.0, new Vec3(1, 0, 0).dot(new Vec3(0, 1, 0)), 1e-9);
        assertEquals(1.0, new Vec3(1, 0, 0).dot(new Vec3(1, 0, 0)), 1e-9);
        assertEquals(-1.0, new Vec3(1, 0, 0).dot(new Vec3(-1, 0, 0)), 1e-9);
    }

    @Test
    void aimNormalisesItsDirection() {
        var aim = new Aim(new Vec3(10, 64, 10), new Vec3(0, 0, 5));
        assertVec(new Vec3(0, 0, 1), aim.direction());
    }

    @Test
    void aimPointAtWalksAlongTheDirection() {
        var aim = new Aim(new Vec3(10, 64, 10), new Vec3(0, 0, 5));
        assertVec(new Vec3(10, 64, 13), aim.pointAt(3));
        assertVec(aim.origin(), aim.pointAt(0));
    }

    @Test
    void aimWithZeroDirectionDoesNotProduceNaN() {
        var aim = new Aim(new Vec3(1, 2, 3), Vec3.ZERO);
        assertVec(new Vec3(1, 2, 3), aim.pointAt(10));
    }

    // --- cross, and the degeneracy that is the whole reason it exists --------------------------

    /**
     * THE RIGHT-HANDED CONVENTION, PINNED AT THE AXES.
     *
     * <p>Three rows rather than one, because {@code x cross y = z} alone is satisfied by a
     * left-handed implementation with two components swapped. The cyclic triple pins the sign of
     * every component.
     */
    @Test
    void crossIsRightHanded() {
        assertVec(new Vec3(0, 0, 1), new Vec3(1, 0, 0).cross(new Vec3(0, 1, 0)));
        assertVec(new Vec3(1, 0, 0), new Vec3(0, 1, 0).cross(new Vec3(0, 0, 1)));
        assertVec(new Vec3(0, 1, 0), new Vec3(0, 0, 1).cross(new Vec3(1, 0, 0)));
    }

    /** It anti-commutes, which is the property a swapped call site would break silently. */
    @Test
    void crossAntiCommutes() {
        Vec3 a = new Vec3(2, -3, 7);
        Vec3 b = new Vec3(-5, 11, 1);
        assertVec(a.cross(b).negate(), b.cross(a));
    }

    /** The result is perpendicular to both inputs, at values no two of which are equal. */
    @Test
    void crossIsPerpendicularToBothInputs() {
        Vec3 a = new Vec3(2, -3, 7);
        Vec3 b = new Vec3(-5, 11, 1);
        Vec3 c = a.cross(b);
        assertEquals(0.0, c.dot(a), 1e-9, "not perpendicular to a");
        assertEquals(0.0, c.dot(b), 1e-9, "not perpendicular to b");
    }

    /**
     * *** PARALLEL INPUTS GIVE ZERO, AND THIS IS THE HAZARD THE WHOLE SPREAD DESIGN IS BUILT
     * AROUND. ***
     *
     * <p>Crossing a look vector with world up is the obvious way to build a view-plane basis, and
     * it returns exactly this at pitch +/-90 -- where the look vector IS world up. Not an
     * exception, not a NaN: a zero vector, which normalises to a zero vector and collapses a ring
     * onto its aim.
     *
     * <p>Pinned here so the property is stated where {@code cross} lives, rather than only being
     * implied by the pole rows in {@code SpreadPatternTest}.
     */
    @Test
    void crossOfParallelVectorsIsZeroWhichIsTheDegeneracyThatMattersHere() {
        assertVec(Vec3.ZERO, new Vec3(0, 1, 0).cross(new Vec3(0, 1, 0)));
        assertVec(Vec3.ZERO, new Vec3(0, 1, 0).cross(new Vec3(0, 5, 0)));
        assertVec(Vec3.ZERO, new Vec3(0, 1, 0).cross(new Vec3(0, -1, 0)));
    }

    /**
     * IT IS NOT NORMALISED, DELIBERATELY -- the length carries information.
     *
     * <p>For two unit vectors the length is {@code sin(angle between)}, so a caller can tell a
     * well-conditioned basis from a nearly-degenerate one by reading it. Normalising inside
     * {@code cross} would throw that away, and this row is what stops a later "tidy-up" doing so.
     */
    @Test
    void crossIsNotNormalisedBecauseItsLengthIsSinOfTheAngle() {
        Vec3 c = new Vec3(3, 0, 0).cross(new Vec3(0, 4, 0));
        assertEquals(12.0, c.length(), 1e-9, "3 x 4, not a unit vector");

        double thirty = Math.toRadians(30);
        Vec3 unitAngle = new Vec3(1, 0, 0)
                .cross(new Vec3(Math.cos(thirty), Math.sin(thirty), 0));
        assertEquals(Math.sin(thirty), unitAngle.length(), 1e-9, "sin(30) for two unit vectors");
    }

    // --- Aim's third component -----------------------------------------------------------------

    /**
     * THE TWO-ARGUMENT CONVENIENCE DERIVES THE RIGHT HORIZONTALLY, AND AGREES WITH THE YAW FORM.
     *
     * <p>Facing south (+Z) the shooter's right is west ({@code -X}). This is the rung every test
     * fixture and every non-spread cast uses, so it has to be correct wherever it is defined at
     * all -- it is only at the pole that it cannot be.
     */
    @Test
    void theTwoArgumentAimDerivesAHorizontalRight() {
        assertVec(new Vec3(-1, 0, 0), new Aim(Vec3.ZERO, new Vec3(0, 0, 1)).right());
        assertVec(new Vec3(0, 0, 1), new Aim(Vec3.ZERO, new Vec3(1, 0, 0)).right());
    }

    /**
     * IT STAYS CORRECT AT A STEEP PITCH, WHICH IS WHY THE POLE IS THE ONLY PROBLEM.
     *
     * <p>At 60 degrees up, facing south, the right is still due west. A reader who knows the
     * constructor "breaks at steep angles" would guard the wrong thing -- the failure is AT the
     * pole and nowhere near it.
     */
    @Test
    void theTwoArgumentAimIsStillCorrectAtASteepPitch() {
        double p = Math.toRadians(60);
        var aim = new Aim(Vec3.ZERO, new Vec3(0, Math.sin(p), Math.cos(p)));
        assertVec(new Vec3(-1, 0, 0), aim.right());
    }

    /**
     * *** AND AT THE POLE IT YIELDS ZERO, WHICH IS THE HONEST ANSWER AND A DOCUMENTED TRAPDOOR. ***
     *
     * <p>The yaw genuinely is not recoverable from a vertical look vector, so inventing an axis
     * here would manufacture a roll nobody chose and hide the loss. A zero is loud downstream:
     * {@code SpreadPattern} refuses it by name and says this constructor is where it came from.
     *
     * <p><b>This row is what makes that trapdoor a measured fact rather than a warning</b>, and it
     * is the reason {@code AimWiringSignatureTest} exists to keep production off this rung.
     */
    @Test
    void theTwoArgumentAimYieldsAZeroRightAtThePole() {
        assertVec(Vec3.ZERO, new Aim(Vec3.ZERO, new Vec3(0, 1, 0)).right());
        assertVec(Vec3.ZERO, new Aim(Vec3.ZERO, new Vec3(0, -1, 0)).right());
    }

    /**
     * {@code pointing} CARRIES THE BASIS ACROSS, WHICH IS WHAT A FAN NEEDS.
     *
     * <p>{@code CastExecutor.executeFan} re-aims per arrow. Rebuilding with the two-argument
     * constructor would silently RE-DERIVE the right from the rotated direction and discard the
     * real one -- correct at every pitch but the two that matter, which is the signature of this
     * whole class of defect.
     *
     * <p>Staged with a right that the horizontal derivation would NOT reproduce, so the row can
     * tell "carried" from "recomputed and happened to match".
     */
    @Test
    void pointingKeepsTheOriginAndTheShootersRight() {
        Vec3 origin = new Vec3(10, 64, 10);
        Vec3 oddRight = new Vec3(0, 0, 1);
        var aim = new Aim(origin, new Vec3(0, 0, 1), oddRight);

        var turned = aim.pointing(new Vec3(1, 0, 0));

        assertVec(new Vec3(1, 0, 0), turned.direction());
        assertVec(origin, turned.origin());
        // Not assertVec: this is the row's actual claim, so it carries the message that says what
        // went wrong. assertVec takes no message, deliberately -- it is the file's terse helper.
        assertEquals(oddRight, turned.right(),
                "the basis was re-derived from the new direction instead of carried");
    }
}
