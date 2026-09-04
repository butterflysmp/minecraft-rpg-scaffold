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
     *
     * <p>{@code item} is a material id RENDERED AS THE PROJECTILE'S BODY -- a real item entity
     * driven to the positions this flight computes, un-pickup-able, removed when the bolt resolves.
     * Also optional, and absent for the same two dev weapons.
     *
     * <p>The two are independent on purpose. A trail without a body is what the Flint Staff shipped
     * as one slice earlier; a body without a trail is a silent thrown rock. Neither implies the
     * other, so neither defaults from the other.
     */
    record Projectile(double speed, double gravity, int maxLifetimeTicks, String trail, String item)
            implements CastSpec {

        /**
         * A projectile with a trail but NO RENDERED BODY -- what the Flint Staff was between the
         * trail landing and the body landing, and what any projectile that wants particles without
         * an entity still is.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks, String trail) {
            this(speed, gravity, maxLifetimeTicks, trail, null);
        }

        /**
         * A projectile with neither -- every call site that predates both fields, and both dev
         * weapons. The same optional-argument ladder {@code AbilityDefinition} uses for its
         * authored description: each convenience constructor drops the TAIL, never a middle field,
         * so a reader counting arguments never has to work out which one was omitted.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks) {
            this(speed, gravity, maxLifetimeTicks, null, null);
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
