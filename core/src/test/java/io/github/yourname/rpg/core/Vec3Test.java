package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.combat.Aim;
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
}
