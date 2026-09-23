package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * The yaw and pitch a rendered projectile body must wear to point along a velocity, in DEGREES.
 *
 * <h2>WHY THIS IS IN {@code core} AT ALL</h2>
 *
 * <p>It is two {@code atan2} calls and no platform types, and {@code CombatWorld}'s adapter is the
 * only caller. Putting it here is the seam rule doing its job: the arithmetic gets unit rows in the
 * unit the requirement is stated in, and {@code paper} is left with one call and one write.
 *
 * <h2>*** THE CONVENTION, AND IT IS NOT THE ONE {@code Location.setDirection} WRITES ***</h2>
 *
 * <pre>
 *   yaw   = toDegrees(atan2(x, z))
 *   pitch = toDegrees(atan2(y, sqrt(x*x + z*z)))
 * </pre>
 *
 * <p><b>The witness is {@code Projectile.shoot}, and it is a witness rather than an argument.</b>
 * Every vanilla bow arrow is spawned through it, it writes exactly the two lines above into
 * {@code setYRot}/{@code setXRot}, and a bow arrow looks right on the frame it appears. Read from the
 * pinned build's bytecode, not remembered.
 *
 * <p><b>{@code Location.setDirection} writes a DIFFERENT convention</b> --
 * {@code atan2(-x, z)} and {@code atan(-y/h)}, the entity-LOOK convention a player's head uses. It is
 * the yaw negated and the pitch negated, so east and west come out backwards and up and down come out
 * inverted, while <b>north and south are unaffected.</b> Anything that routes a projectile body's
 * rotation through a {@code Location} is therefore wrong on five of seven directions and right on the
 * two a tester is most likely to face. {@code BodyRotationTest} pins that difference mechanically.
 *
 * <h2>NULL MEANS "NO ANSWER", AND IT IS NOT {@code (0, 0)}</h2>
 *
 * <p>{@link #along} returns {@code null} for a zero velocity. <b>{@code (0, 0)} would be a silent
 * wrong answer</b> -- it is due south and level, which is precisely the defect this type exists to
 * remove -- and a thrown exception would push a guard into every caller for a case that has no
 * meaning. The caller's contract is to SKIP THE WRITE and leave whatever rotation the body has.
 *
 * <h2>WHAT THIS TYPE CANNOT FIX, STATED HERE BECAUSE THE NEXT READER WILL ASSUME IT DOES</h2>
 *
 * <p>A body's rotation is also recomputed by the platform every tick, and on the pinned build
 * {@code AbstractArrow.tick} derives the yaw as {@code atan2(-x, -z)} -- reversed -- for a
 * {@code noPhysics} arrow, which every bolt body is. Writing the value from here does not stop that;
 * it makes each write correct and lets the platform erode it until the next one. The account, the
 * measured tick order and the boot rows are in {@code GATE-arrow-body-orientation.md}. <b>This is the
 * pointer; that is the account.</b>
 */
public record BodyRotation(float yaw, float pitch) {

    /**
     * The rotation that points along {@code velocity}, or {@code null} if it has no direction.
     *
     * <p>Scale-invariant: {@code atan2} reads the ratio, so a velocity and its multiple give the same
     * answer. A speed is never a rotation.
     */
    public static BodyRotation along(Vec3 velocity) {
        if (velocity.lengthSquared() == 0) return null;

        double horizontal = Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());
        return new BodyRotation(
                (float) Math.toDegrees(Math.atan2(velocity.x(), velocity.z())),
                (float) Math.toDegrees(Math.atan2(velocity.y(), horizontal)));
    }
}
