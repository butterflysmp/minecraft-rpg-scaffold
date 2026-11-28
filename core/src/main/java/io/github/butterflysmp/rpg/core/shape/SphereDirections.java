package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * DIRECTIONS SPREAD EVENLY OVER A SPHERE (Solar Flare's firework, the seat's N4, Route B, 2026-11-28): the burst's
 * particles are each thrown toward one of these, out to the burst's radius, so the ball they make is round and the same
 * every time. A Fibonacci lattice: point i of n at height {@code y = 1 - 2 (i + 0.5) / n} and azimuth {@code i x the
 * golden angle} ({@code pi (3 - sqrt 5)}). One direction is straight up.
 */
public final class SphereDirections {

    private SphereDirections() {}

    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

    /** {@code n} unit vectors over the sphere, 1 or more. */
    public static List<Vec3> fibonacci(int n) {
        if (n < 1) throw new IllegalArgumentException("a sphere needs 1 or more directions, was " + n);
        if (n == 1) return List.of(new Vec3(0, 1, 0));
        List<Vec3> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double y = 1 - 2 * (i + 0.5) / n;
            double r = Math.sqrt(1 - y * y);
            double a = GOLDEN_ANGLE * i;
            out.add(new Vec3(r * Math.cos(a), y, r * Math.sin(a)).normalize());
        }
        return out;
    }
}
