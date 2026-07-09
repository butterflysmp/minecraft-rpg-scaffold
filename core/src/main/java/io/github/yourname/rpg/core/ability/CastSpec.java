package io.github.yourname.rpg.core.ability;

/** How the ability reaches its target. */
public sealed interface CastSpec {
    record Self() implements CastSpec {}
    record Melee(double reach, double arcDegrees) implements CastSpec {}
    record Ray(double range) implements CastSpec {}
    record Projectile(double speed, double gravity, int maxLifetimeTicks) implements CastSpec {}
}
