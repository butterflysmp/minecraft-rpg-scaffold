package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * Where a cast starts, which way it points, and which way is the shooter's RIGHT. In Paper this is
 * the player's eye location, look direction and yaw-derived right, but core neither knows nor
 * cares.
 *
 * Replaces the already-resolved (target, impactPoint) pair that AbilityService
 * used to be handed. Resolving an aim into a target is the job of CastExecutor,
 * because doing so touches the world and must happen on the owning region thread.
 *
 * The direction is normalised on construction, so every consumer can treat it as
 * a unit vector. A zero direction stays zero rather than becoming NaN.
 *
 * <h2>*** WHY `right` IS CARRIED AND NOT DERIVED ***</h2>
 *
 * <p>A cast that builds a pattern in the shooter's VIEW PLANE -- {@code SpreadPattern}, today the
 * only one -- needs a second axis, and <b>the direction alone does not contain it.</b> The
 * shooter's right depends only on YAW, and at pitch +/-90 both horizontal components of the
 * direction are zero, so the yaw is unrecoverable from it. The information has to come in from
 * outside or be lost.
 *
 * <p>It is perpendicular to the direction by construction rather than by assertion: a yaw-derived
 * right is horizontal, and its dot with the look vector cancels to exactly zero at every pitch.
 * Nothing here re-orthogonalises, and {@code SpreadPattern} normalises what it is handed.
 */
public record Aim(Vec3 origin, Vec3 direction, Vec3 right) {

    public Aim {
        direction = direction.normalize();
    }

    /**
     * An aim with no view-plane basis -- <b>every cast shape that reads only the direction</b>, and
     * every test fixture that is not exercising a spread.
     *
     * <p><b>*** THIS IS A TRAPDOOR AND IT IS DOCUMENTED AS ONE. ***</b> The right is derived from
     * the direction's HORIZONTAL component, which is correct at every pitch except the two where
     * it matters: at pitch +/-90 the horizontal component is zero and <b>this yields a ZERO right
     * vector.</b>
     *
     * <p>That is deliberate and it is the honest answer -- the yaw genuinely is not in the
     * direction, so inventing an axis here would manufacture a roll nobody chose and hide the loss.
     * <b>A zero is loud downstream; an invented axis is silent.</b> {@code SpreadPattern} refuses a
     * zero basis by name and says this constructor is where it came from.
     *
     * <p><b>Production must not reach this constructor, and a test enforces that rather than a
     * sentence.</b> {@code AimWiringSignatureTest} scans the construction sites in {@code paper}
     * and requires each to pass a right vector. <b>This rung stays for tests; the guard is what
     * keeps it safe to keep.</b>
     *
     * <p>The horizontal derivation itself: rotating the horizontal facing a quarter turn, matching
     * the yaw convention {@code DashAim} already uses ({@code forward = (-sin, 0, cos)},
     * {@code left = (cos, 0, sin)}), so a right derived here and a right derived from a Bukkit yaw
     * agree wherever both exist.
     */
    public Aim(Vec3 origin, Vec3 direction) {
        this(origin, direction, horizontalRight(direction));
    }

    /** The point {@code distance} blocks along the aim. */
    public Vec3 pointAt(double distance) {
        return origin.add(direction.scale(distance));
    }

    /**
     * The same aim pointed a new way, keeping the origin and the shooter's right.
     *
     * <p>Exists because {@code CastExecutor.executeFan} rotates an aim per arrow and <b>must carry
     * the basis across</b>; rebuilding with the two-argument constructor would silently re-derive
     * the right from the rotated direction and throw the real one away.
     */
    public Aim pointing(Vec3 newDirection) {
        return new Aim(origin, newDirection, right);
    }

    /**
     * The horizontal quarter-turn, and ZERO for a vertical look. Static so the constructor can
     * delegate to it before {@code this} exists.
     */
    private static Vec3 horizontalRight(Vec3 direction) {
        // (-z, 0, x) is (x, 0, z) turned a quarter turn, and it is ZERO exactly when the look is
        // vertical -- which is the case the javadoc above is about.
        return new Vec3(-direction.z(), 0, direction.x()).normalize();
    }
}
