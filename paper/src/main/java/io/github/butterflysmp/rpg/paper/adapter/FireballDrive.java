package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * The velocity to hand a FIREBALL body so that it moves exactly one computed step this tick
 * (LEGACY-B, {@code PLAN-legacy-b.md} section 5.4; Q-B3, the seat: COMPENSATE).
 *
 * <h2>WHY A FIREBALL NEEDS THIS AND AN ARROW DOES NOT</h2>
 *
 * <p>{@link PaperCombatWorld#driveMarker} sets a velocity and lets the entity's own tick move it. An
 * arrow applies its drag AFTER its move, so the next drive overwrites the drag before it matters and
 * the body lands exactly on the path. <b>A hurting projectile does the opposite.</b> Read from the
 * pinned server jar ({@code AbstractHurtingProjectile.tick}, {@code javap -c}): {@code tick} calls
 * {@code applyInertia} FIRST, and {@code applyInertia} is
 *
 * <pre>
 *   delta = (delta + normalize(delta) x accelerationPower) x inertia     then setPos(position + delta)
 *   accelerationPower = 0.1 (constructor default)    inertia = getInertia() = 0.95f (0.8f in water)
 * </pre>
 *
 * <p>So a body driven with {@code v} moves {@code normalize(v) x (|v| + 0.1) x 0.95}: the right
 * direction at the wrong speed. At the staff's 1.5 that is 1.52 a tick, 0.02 blocks ahead per tick
 * (awk, in the plan). <b>Inverting it is one line</b>: drive with {@code normalize(v) x (|v| / 0.95 -
 * 0.1)} and the body moves exactly {@code v}.
 *
 * <h2>THE TWO CONSTANTS ARE JAR FACTS WITH NO API GETTER</h2>
 *
 * <p>{@code CraftFireball.setAcceleration} sets the VELOCITY, not {@code accelerationPower}, so the
 * push has no API setter or getter in this build; it is reachable only through entity NBT, which is
 * NMS and banned here. The design therefore works WITH the push and the drag rather than switching
 * them off. If a Paper update changes either constant the body leads or lags on the right line,
 * nothing reddens, and gate row LB9 is the witness that would see it.
 *
 * <p>{@link #INERTIA} is the jar's {@code 0.95f} WIDENED to double ({@code 0.949999988079071}), which
 * is exactly what {@code f2d} hands {@code Vec3.scale}. Writing {@code 0.95} would leave a residual of
 * about 2e-8 per tick: invisible, and still not the jar's number.
 *
 * <h2>WATER IS NOT COMPENSATED</h2>
 *
 * <p>In water the inertia is {@code 0.8f}, so a compensated body over water moves slower than the
 * flight. Cosmetic only: {@code castRay} owns every hit and never consults the body.
 *
 * <h2>BELOW THE FLOOR THE LAW CANNOT BE INVERTED, AND NO SHIPPED CONTENT IS THERE</h2>
 *
 * <p>Any non-zero delta moves at least {@code 0.1 x 0.95 = 0.095} a tick, because the push is added
 * along whatever direction the delta has. A step shorter than {@link #FLOOR} has no exact drive. This
 * clamps the magnitude at zero, so such a body stalls rather than reversing (a negative magnitude
 * would point the delta backwards, and the push would then carry it backwards too). <b>Unreachable by
 * shipped content</b>: the only fireball is the Blaze King's Staff at speed 1.5 and gravity 0, whose
 * step never changes. A future slow or arcing fireball would meet it near an apex; that is recorded
 * here and not ruled.
 */
final class FireballDrive {

    private FireballDrive() {}

    /** {@code AbstractHurtingProjectile.accelerationPower}'s constructor default, read from the jar. */
    static final double ACCELERATION_POWER = 0.1;

    /** {@code AbstractHurtingProjectile.getInertia()}'s {@code 0.95f}, widened exactly as the jar does. */
    static final double INERTIA = (double) 0.95f;

    /** The shortest non-zero move a hurting projectile can make in one tick. */
    static final double FLOOR = ACCELERATION_POWER * INERTIA;

    /**
     * The velocity to set so that vanilla's {@code applyInertia} turns it back into {@code step}.
     *
     * @param step one tick's computed displacement, as {@code ProjectileFlight} hands it to the drive
     */
    static Vec3 velocityFor(Vec3 step) {
        double length = step.length();
        if (length == 0) return Vec3.ZERO;                  // normalize() below would be ZERO anyway
        double magnitude = length / INERTIA - ACCELERATION_POWER;
        if (magnitude <= 0) return Vec3.ZERO;               // below FLOOR: stall, never reverse
        return step.normalize().scale(magnitude);
    }
}
