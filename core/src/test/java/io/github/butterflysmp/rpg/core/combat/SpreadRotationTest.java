package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RING ROTATES (the seat's ruling 1, 2026-11-22, the Breacher rework): {@code rotation_degrees}, counter-clockwise
 * from the shooter's RIGHT toward his UP, as phi already runs, DEFAULT 0. Ben: four pellets, "a triangle shape with the
 * point up", one on the aim. A ring of three at 0, 120 and 240 (today's) points RIGHT; rotated 90 it is 90, 210 and 330:
 * point UP, two below, left and right.
 *
 * <p>The basis is the one a level shot facing +x has: forward (1, 0, 0), the shooter's right (0, 0, 1), so his up is
 * {@code right x forward} = (0, 1, 0), as {@code SpreadPattern} builds it.
 */
class SpreadRotationTest {

    private static final Vec3 FORWARD = new Vec3(1, 0, 0);
    private static final Vec3 RIGHT = new Vec3(0, 0, 1);
    private static final double EPS = 1e-9;

    /** A RING OF THREE ROTATED 90: one pellet straight up, two below, one each side, mirror images. */
    @Test
    void aRingOfThreeAtNinetyPointsUp() {
        List<Vec3> d = SpreadPattern.directionsFor(FORWARD, RIGHT, new CastSpec.Spread(4, 3.0, 90.0));
        assertEquals(4, d.size(), "the aim and a ring of three");
        assertEquals(FORWARD, d.get(0), "the aim pellet first, as before");
        Vec3 top = d.get(1);
        assertEquals(0.0, top.z(), EPS, "the first ring pellet has no lateral offset: straight up");
        assertTrue(top.y() > 0, "and it is ABOVE the aim: " + top);
        Vec3 a = d.get(2);
        Vec3 b = d.get(3);
        assertTrue(a.y() < 0 && b.y() < 0, "the other two are BELOW the aim: " + a + " " + b);
        assertTrue(a.z() * b.z() < 0, "one each side of it: " + a + " " + b);
        assertEquals(a.y(), b.y(), EPS, "at the same height");
        assertEquals(-a.z(), b.z(), EPS, "mirror images left and right");
        // The triangle's geometry: each lower pellet sits at half the top one's height below the aim.
        assertEquals(-top.y() / 2, a.y(), 1e-6, "sin(210) = -1/2 of sin(90)");
    }

    /** UNROTATED, TODAY'S PATTERN PELLET FOR PELLET: 0 is the default and every shipped file reads it. */
    @Test
    void atZeroItIsTodaysPatternPelletForPellet() {
        for (int count : new int[] {4, 7}) {
            for (double angle : new double[] {3.0, 12.5}) {
                List<Vec3> before = SpreadPattern.directionsFor(FORWARD, RIGHT, new CastSpec.Spread(count, angle));
                List<Vec3> zero = SpreadPattern.directionsFor(FORWARD, RIGHT, new CastSpec.Spread(count, angle, 0.0));
                assertEquals(before, zero, "count " + count + " at " + angle + ": rotation 0 changes nothing");
            }
        }
        // CONTROL: today's ring of three points RIGHT, which is why the option exists.
        List<Vec3> d = SpreadPattern.directionsFor(FORWARD, RIGHT, new CastSpec.Spread(4, 3.0));
        assertEquals(0.0, d.get(1).y(), EPS, "unrotated, the first ring pellet is level...");
        assertTrue(d.get(1).z() > 0, "...and to the shooter's RIGHT: a triangle pointing right");
    }

    /** Rotating moves WHERE each ring pellet is, never HOW FAR off the aim: every one is still exactly the angle. */
    @Test
    void rotationKeepsEveryPelletAtTheAngle() {
        double expected = Math.cos(Math.toRadians(3.0));
        for (Vec3 v : SpreadPattern.directionsFor(FORWARD, RIGHT, new CastSpec.Spread(4, 3.0, 90.0)).subList(1, 4)) {
            assertEquals(expected, v.dot(FORWARD), 1e-12, "3 degrees off the aim: " + v);
        }
    }

    @Test
    void aNonFiniteRotationIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(4, 3.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(4, 3.0, Double.POSITIVE_INFINITY));
    }
}
