package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Aim;

import org.bukkit.Location;

/**
 * THE ONE PLACE A BUKKIT {@link Location} BECOMES AN {@link Aim}, including the shooter's RIGHT.
 *
 * <p>Every production aim in this plugin is built here. That is the point: the right vector is a
 * trigonometric convention, and <b>two sites computing it independently is how they come to
 * disagree</b> -- the same argument {@code check-jar.sh} makes about being the only thing that
 * decides which file is "the jar", and the one {@code QuiverSize.resolve} makes about being the
 * only place a bonus becomes a capacity.
 *
 * <h2>WHY THE RIGHT COMES FROM YAW AND NOT FROM THE LOOK VECTOR</h2>
 *
 * <p>{@code SpreadPattern} builds its ring in the shooter's view plane and needs a second axis.
 * <b>Crossing the look vector with world up gives the right answer at every pitch except the two
 * that matter</b>, where the look vector IS world up and the cross product is zero. The shooter's
 * right depends only on YAW, so it is defined everywhere -- and the yaw is not recoverable from the
 * look vector at pitch +/-90, where both horizontal components are zero.
 *
 * <p>So the yaw is read from the {@code Location}, which has it, rather than re-derived from a
 * direction that has thrown it away. <b>This is the whole reason {@link Aim} carries a third
 * component.</b>
 *
 * <p>The convention matches {@code DashAim}'s, which resolves WASD against the same frame:
 * {@code forward = (-sin, 0, cos)} and {@code left = (cos, 0, sin)}, so right is {@code -left}.
 * Bukkit yaw 0 faces +Z (south), where east (+X) is on the player's LEFT -- checked against that
 * table rather than reasoned from a compass, because the sign is easy to talk oneself into.
 *
 * <p><b>It is exactly perpendicular to the look direction at every pitch</b>, and that is
 * arithmetic rather than an approximation: a horizontal right dotted with
 * {@code (-sin y cos p, -sin p, cos y cos p)} cancels to zero identically. Nothing downstream
 * re-orthogonalises.
 */
public final class ViewAim {

    private ViewAim() {}

    /**
     * The aim a cast fired from {@code eye} starts with: the eye as origin, the look direction, and
     * the shooter's own right.
     *
     * <p>Takes the whole {@code Location} rather than a direction <b>because the yaw is the part
     * that cannot be recovered afterwards.</b> A signature taking a direction would compile, work
     * at every pitch a test is likely to stage, and lose the pole.
     */
    public static Aim of(Location eye) {
        Vec3 origin = new Vec3(eye.getX(), eye.getY(), eye.getZ());
        var direction = eye.getDirection();
        return new Aim(origin,
                new Vec3(direction.getX(), direction.getY(), direction.getZ()),
                rightOf(eye.getYaw()));
    }

    /**
     * The shooter's right, from yaw alone, as a unit vector. Never zero -- which is the property
     * the whole class exists for.
     *
     * <p>Package-private rather than public: it is the convention, and a second caller wanting it
     * almost certainly wants {@link #of} instead. Visible at all so a test can assert the sign
     * against known yaws without constructing a {@code Location}.
     */
    static Vec3 rightOf(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }
}
