package io.github.butterflysmp.rpg.core.ability;

/** How the ability reaches its target. */
public sealed interface CastSpec {
    record Self() implements CastSpec {}
    record Melee(double reach, double arcDegrees) implements CastSpec {}
    record Ray(double range) implements CastSpec {}
    record Projectile(double speed, double gravity, int maxLifetimeTicks) implements CastSpec {}

    /**
     * Moves the caster. A one-shot velocity impulse of magnitude {@code speed} along a
     * direction resolved outside core (WASD movement, or look when stationary), carried by
     * physics. {@code distance} is the INTENDED length -- the swept line used to find who the
     * dash passes through, independent of the ballistic path the caster actually takes.
     * Direction is deliberately NOT a field: core stays direction-agnostic and Bukkit-free.
     */
    record Dash(double distance, double speed) implements CastSpec {}
}
