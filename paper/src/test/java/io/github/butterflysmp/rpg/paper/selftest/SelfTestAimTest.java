package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The AIM verb's ray, cast from the player's EYE along the rotation {@link SelfTestAim} computes, passes through the
 * target's hitbox at every distance the scenarios stage. Exact ray, exact box: no lenience, which every real consumer
 * ({@code /rpg apply}'s 0.3, a projectile's own box) only adds to.
 *
 * <p>Vanilla's sizes, as the scenarios meet them: the player's eye at 1.62; a zombie 0.6 wide and 1.95 tall with its
 * eye at 1.74; a wither skeleton (the Knell) 0.7 wide and 2.4 tall with its eye at 2.1. Read from the pinned jar's
 * {@code EntityType} static initialiser ({@code sized} and {@code eyeHeight}), Paper 26.1.2 build 74, 2026-10-14.
 */
class SelfTestAimTest {

    private static final double PLAYER_EYE = 1.62;

    @Test
    void theRayMeetsAZombieStraightAheadAt2And3And4And8() {
        for (double d : new double[] {2, 3, 4, 8}) {
            assertTrue(hits(d, 0, 0.6, 1.95, 1.74), "zombie at d=" + d);
        }
    }

    @Test
    void theRayMeetsAWitherSkeletonAt2And3And4And8() {
        for (double d : new double[] {2, 3, 4, 8}) {
            assertTrue(hits(d, 0, 0.7, 2.4, 2.1), "wither skeleton at d=" + d);
        }
    }

    @Test
    void theRayMeetsAZombieOffTheAxis() {
        // WS3's neighbour sits at ~4 ~ ~2; the yaw must turn as well as the pitch.
        assertTrue(hits(4, 2, 0.6, 1.95, 1.74));
        assertTrue(hits(3.2, 3.2, 0.6, 1.95, 1.74));
    }

    @Test
    void theBootsMissIsTheFeetAnchor() {
        // The positive control: the 2026-10-14 boot logged pitch=-12.271 at d=8. That is the rotation from the
        // FEET (eye height 0) to the zombie's eye -- and from the eye, cast along it, it passes over the zombie.
        SelfTestAim.Rotation fromFeet = SelfTestAim.at(0, 0, 0, 0, 8, 1.74, 0);
        assertEquals(-12.271, fromFeet.pitch(), 0.0005);
        assertFalse(rayHitsBox(PLAYER_EYE, fromFeet, 8, 0, 0.6, 1.95));
    }

    @Test
    void yawFollowsBukkitsConvention() {
        assertEquals(-90f, SelfTestAim.at(0, 0, 0, PLAYER_EYE, 5, PLAYER_EYE, 0).yaw(), 1e-4);   // +X
        assertEquals(0f, SelfTestAim.at(0, 0, 0, PLAYER_EYE, 0, PLAYER_EYE, 5).yaw(), 1e-4);     // +Z
        assertEquals(0f, SelfTestAim.at(0, 0, 0, PLAYER_EYE, 5, PLAYER_EYE, 0).pitch(), 1e-4);   // level
    }

    /** A target standing at (dx, 0, dz) from the player's feet. */
    private static boolean hits(double dx, double dz, double width, double height, double eye) {
        SelfTestAim.Rotation r = SelfTestAim.at(0, 0, 0, PLAYER_EYE, dx, eye, dz);
        return rayHitsBox(PLAYER_EYE, r, dx, dz, width, height);
    }

    /** Slab test: the ray from (0, eyeY, 0) along Bukkit's direction for {@code r}, against the target's box. */
    private static boolean rayHitsBox(double eyeY, SelfTestAim.Rotation r, double dx, double dz,
                                      double width, double height) {
        double yaw = Math.toRadians(r.yaw()), pitch = Math.toRadians(r.pitch());
        double[] dir = {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
        double[] origin = {0, eyeY, 0};
        double half = width / 2;
        double[] min = {dx - half, 0, dz - half};
        double[] max = {dx + half, height, dz + half};
        double near = 0, far = 64;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dir[i]) < 1e-12) {
                if (origin[i] < min[i] || origin[i] > max[i]) return false;
                continue;
            }
            double t1 = (min[i] - origin[i]) / dir[i], t2 = (max[i] - origin[i]) / dir[i];
            near = Math.max(near, Math.min(t1, t2));
            far = Math.min(far, Math.max(t1, t2));
            if (near > far) return false;
        }
        return true;
    }
}
