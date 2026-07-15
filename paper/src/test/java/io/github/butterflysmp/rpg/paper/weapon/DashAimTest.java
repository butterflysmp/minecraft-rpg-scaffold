package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The dash direction is always horizontal. The case that matters is the stationary fallback:
 * with no movement keys it must be a W-equivalent FORWARD dash on the ground plane, NOT the
 * look direction -- so a player looking up and casting cannot launch straight up. Pitch never
 * enters {@link DashAim#directionFromInput} (it takes only yaw), and these lock the
 * consequence: a zero Y in every case. A mutation that lets a vertical component through --
 * e.g. reintroducing the pitched look direction -- makes Y nonzero and reddens.
 */
class DashAimTest {

    /** No movement keys held: the stationary fallback. */
    private static Vec3 stationary(double yaw) {
        return DashAim.directionFromInput(yaw, false, false, false, false);
    }

    @Test
    void stationaryFallbackIsHorizontalNeverVertical() {
        for (double yaw : new double[] {0, 30, 45, 90, 135, 180, -45, -90, 270}) {
            assertEquals(0.0, stationary(yaw).y(), 1e-9,
                    "a standing dash is flat on the ground -- no vertical launch (yaw " + yaw + ")");
        }
    }

    @Test
    void stationaryFallbackIsWEquivalentForward() {
        // Bukkit yaw 0 faces +Z; pressing nothing dashes forward = +Z, exactly as W would.
        Vec3 d = stationary(0);
        assertEquals(0.0, d.x(), 1e-9);
        assertEquals(0.0, d.y(), 1e-9);
        assertEquals(1.0, d.z(), 1e-9, "no keys -> a forward (W-equivalent) dash along facing");
    }

    @Test
    void strafeLeftIsHorizontalAndPerpendicularToFacing() {
        // Facing +Z (yaw 0), strafe-left only -> the player's left is +X, flat on the ground.
        Vec3 d = DashAim.directionFromInput(0, false, false, true, false);
        assertEquals(1.0, d.x(), 1e-9);
        assertEquals(0.0, d.y(), 1e-9, "strafe is a ground-plane step, never vertical");
        assertEquals(0.0, d.z(), 1e-9);
    }

    @Test
    void movingDiagonalStaysHorizontal() {
        // forward + strafe-left at an arbitrary yaw -> still flat.
        assertEquals(0.0, DashAim.directionFromInput(37, true, false, true, false).y(), 1e-9);
    }

    // --- Rekindle's reverse-facing dash: horizontal, and the exact opposite of facing. ---

    @Test
    void reverseFacingIsHorizontalNeverVertical() {
        for (double yaw : new double[] {0, 30, 45, 90, 135, 180, -45, -90, 270}) {
            assertEquals(0.0, DashAim.reverseFacing(yaw).y(), 1e-9,
                    "a reverse dash is flat on the ground -- no down-and-back (yaw " + yaw + ")");
        }
    }

    @Test
    void reverseFacingIsExactlyOppositeTheForwardDash() {
        // Yaw 0 faces +Z, so the forward dash is +Z and the reverse is -Z.
        Vec3 atZero = DashAim.reverseFacing(0);
        assertEquals(0.0, atZero.x(), 1e-9);
        assertEquals(0.0, atZero.y(), 1e-9);
        assertEquals(-1.0, atZero.z(), 1e-9, "yaw 0 faces +Z, so a reverse dash goes -Z");

        // Yaw 90 faces -X, so the forward dash is -X and the reverse is +X. A mutation that
        // forgets to negate would return the forward direction and redden here.
        Vec3 atNinety = DashAim.reverseFacing(90);
        assertEquals(1.0, atNinety.x(), 1e-9, "yaw 90 faces -X, so a reverse dash goes +X");
        assertEquals(0.0, atNinety.y(), 1e-9);
        assertEquals(0.0, atNinety.z(), 1e-9);
    }
}
