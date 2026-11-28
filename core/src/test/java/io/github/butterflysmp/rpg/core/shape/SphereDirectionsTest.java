package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE DIRECTIONS A BURST'S PARTICLES ARE THROWN IN (Solar Flare's firework, the seat's N4, Route B, 2026-11-28): n unit
 * vectors spread evenly over the sphere (a Fibonacci lattice), deterministic, so a burst is the same ball every time.
 */
class SphereDirectionsTest {

    /**
     * n distinct unit vectors; every one length 1; their mean is the centre (a ball, not a cone); at the counts a burst
     * uses (60, 90) every octant is used (8 directions leave two octants empty: executed, 6).
     */
    @Test
    void nDistinctUnitDirectionsCoveringEveryOctantAroundTheCentre() {
        for (int n : new int[] {8, 60, 90}) {
            List<Vec3> dirs = SphereDirections.fibonacci(n);
            assertEquals(n, dirs.size());
            assertEquals(n, new HashSet<>(dirs).size(), "distinct");
            double mx = 0, my = 0, mz = 0;
            var octants = new HashSet<Integer>();
            for (Vec3 d : dirs) {
                assertEquals(1.0, d.length(), 1e-12);
                mx += d.x(); my += d.y(); mz += d.z();
                octants.add((d.x() >= 0 ? 1 : 0) | (d.y() >= 0 ? 2 : 0) | (d.z() >= 0 ? 4 : 0));
            }
            assertTrue(Math.abs(mx / n) < 0.05 && Math.abs(my / n) < 0.05 && Math.abs(mz / n) < 0.05,
                    n + ": the mean is " + mx / n + " " + my / n + " " + mz / n);
            if (n >= 60) assertEquals(8, octants.size(), n + " directions use every octant");   // 8 do NOT: 6, EXECUTED
        }
        assertEquals(List.of(new Vec3(0, 1, 0)), SphereDirections.fibonacci(1), "one: straight up");
        assertThrows(IllegalArgumentException.class, () -> SphereDirections.fibonacci(0));
    }
}
