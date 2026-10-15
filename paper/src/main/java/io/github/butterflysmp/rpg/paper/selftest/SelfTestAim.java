package io.github.butterflysmp.rpg.paper.selftest;

/**
 * The AIM verb's arithmetic: the yaw and pitch that point the player's EYE at the target's eye.
 *
 * <p><b>Why this is Java and not {@code tp @s ~ ~ ~ facing entity <e> eyes}.</b> That command's {@code eyes} is the
 * TARGET's anchor only. The player's own anchor is the command source's, and a player's source stack is built with
 * {@code Anchor.FEET} (javap, Paper 26.1.2 build 74: {@code CommandSourceStack}'s 9-argument constructor,
 * {@code LookAt$LookAtEntity.perform}, {@code Entity.lookAt}). So the rotation was computed from the FEET, and every
 * look ray -- which starts at the eyes -- ran parallel to the intended one, 1.62 blocks higher: over a zombie at 8
 * blocks. The 2026-10-14 boot logged {@code pitch=-12.271}; from the eye it is -0.859.
 *
 * <p>No Bukkit type here, so {@code SelfTestAimTest} proves the ray meets the hitbox without a server.
 */
final class SelfTestAim {

    private SelfTestAim() {}

    /** Bukkit's convention: yaw 0 faces +Z and -90 faces +X; pitch is positive looking DOWN. */
    record Rotation(float yaw, float pitch) {}

    /** The rotation from the eye ({@code feetY + eyeHeight}) to the target's eye point. */
    static Rotation at(double feetX, double feetY, double feetZ, double eyeHeight,
                       double targetX, double targetEyeY, double targetZ) {
        double dx = targetX - feetX;
        double dy = targetEyeY - (feetY + eyeHeight);
        double dz = targetZ - feetZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Rotation(yaw, pitch);
    }
}
