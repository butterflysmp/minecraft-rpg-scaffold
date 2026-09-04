package io.github.butterflysmp.rpg.core.ability;

/** How the ability reaches its target. */
public sealed interface CastSpec {
    record Self() implements CastSpec {}
    record Melee(double reach, double arcDegrees) implements CastSpec {}
    record Ray(double range) implements CastSpec {}
    /**
     * A body arcing under gravity until it hits something. {@code trail} is a visual id presented
     * at the projectile's live position once per tick of flight, or null for a bare projectile
     * that leaves nothing.
     *
     * <p>{@code trail} is OPTIONAL and defaults to absent, which is what makes it not a change to
     * the weapons that do not ask for one: {@code hunters_bow} and {@code ember_staff} specify no
     * trail, get null, and behave byte-identically to before this field existed. The field is here
     * because {@link io.github.butterflysmp.rpg.core.combat.ProjectileFlight} has ALWAYS presented
     * a trail every step -- there was simply no way for the schema to produce anything but null,
     * and the call site's {@code // a bare projectile leaves no trail} read as a decision when it
     * was the only value available.
     */
    record Projectile(double speed, double gravity, int maxLifetimeTicks, String trail)
            implements CastSpec {

        /**
         * A projectile with no trail -- every call site that predates the field, and both dev
         * weapons. The same optional-argument ladder {@code AbilityDefinition} uses for its
         * authored description: the convenience constructor drops the tail.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks) {
            this(speed, gravity, maxLifetimeTicks, null);
        }
    }

    /**
     * Which way a dash sends the caster. The concrete direction VECTOR is still resolved
     * outside core (it reads a Bukkit player); this only names the RULE, so the same
     * resolver can produce either without core learning what a Player is.
     */
    enum DashDirection {
        /** Ember Step: the way the player is moving (WASD), or facing when stationary. */
        MOVEMENT_ELSE_FORWARD,
        /** Rekindle: the reverse of facing, ALWAYS -- a straight backpedal, ignoring WASD. */
        REVERSE_FACING
    }

    /**
     * Moves the caster. A one-shot velocity impulse of magnitude {@code speed} along a
     * direction resolved outside core (per {@code direction}), plus a small upward
     * {@code lift}, carried by physics. {@code distance} is the INTENDED length -- the
     * swept line used to find who the dash passes through, independent of the ballistic path
     * the caster actually takes. The concrete vector is deliberately NOT a field: core stays
     * Bukkit-free; only the {@code direction} MODE is declared here.
     *
     * {@code lift} exists because a purely horizontal ground impulse is eaten by Minecraft's
     * first-tick ground friction and barely travels; a touch of up arcs the caster off the
     * floor so the horizontal velocity carries. It is a tuning number -- dialed in the yml
     * against {@code speed} until a flat-ground dash reads the intended distance.
     */
    record Dash(double distance, double speed, double lift, DashDirection direction) implements CastSpec {}
}
