package io.github.butterflysmp.rpg.core.ability;

/** How the ability reaches its target. */
public sealed interface CastSpec {
    record Self() implements CastSpec {}
    record Melee(double reach, double arcDegrees) implements CastSpec {}
    record Ray(double range) implements CastSpec {}
    record Projectile(double speed, double gravity, int maxLifetimeTicks) implements CastSpec {}

    /**
     * Moves the caster. A one-shot velocity impulse of magnitude {@code speed} along a
     * direction resolved outside core (WASD movement, or look when stationary), plus a small
     * upward {@code lift}, carried by physics. {@code distance} is the INTENDED length -- the
     * swept line used to find who the dash passes through, independent of the ballistic path
     * the caster actually takes. Direction is deliberately NOT a field: core stays
     * direction-agnostic and Bukkit-free.
     *
     * {@code lift} exists because a purely horizontal ground impulse is eaten by Minecraft's
     * first-tick ground friction and barely travels; a touch of up arcs the caster off the
     * floor so the horizontal velocity carries. It is a tuning number -- dialed in the yml
     * against {@code speed} until a flat-ground dash reads the intended distance.
     */
    record Dash(double distance, double speed, double lift) implements CastSpec {}
}
