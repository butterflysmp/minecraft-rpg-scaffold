package io.github.butterflysmp.rpg.core.ability.effect;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.List;

/**
 * A composable, declarative description of what an ability *does*.
 * Loaded from YAML. Never contains behaviour -- only data.
 *
 * Split by whether an effect needs a victim. A cast can resolve no target at
 * all (a grenade lands in an empty field), so EffectApplier must skip the
 * targeted ones. Encoding that in the type means the check happens once, and
 * the compiler -- not a reviewer -- guarantees a new effect type declares
 * which kind it is.
 *
 * Sealed so that EffectApplier's switch is exhaustive: add a new effect type
 * and the compiler tells you every place that must handle it.
 */
public sealed interface EffectSpec permits EffectSpec.Targeted, EffectSpec.Untargeted {

    /** Needs a victim. Skipped entirely when the cast resolved no target. */
    sealed interface Targeted extends EffectSpec
            permits Damage, Heal, Knockback, Status {}

    /** Acts on the world rather than a victim. Always runs. */
    sealed interface Untargeted extends EffectSpec
            permits Area, Burst, DelayedBurst, ThrowEmbers, Visual {}

    /** element is a content id ("fire", "kinetic", ...) -- pure identity, validated at boot; never null. */
    record Damage(double amount, String element) implements Targeted {}

    record Heal(double amount) implements Targeted {}

    record Knockback(double strength) implements Targeted {}

    /** e.g. "scorch", "slow", "suppress" -- resolved by the status registry. */
    record Status(String statusId, int durationTicks, int amplifier) implements Targeted {}

    /**
     * Applies nested effects to everything in radius ONCE, on the frame it is applied.
     * An explosion's splash, as against Area's lingering field.
     *
     * Applied inline, never scheduled. The Paper adapter clamps a zero delay up to one
     * tick, so scheduling this would put the splash a frame behind the bang -- and it is
     * a bang: the whole point is that it lands with the visual, not after it.
     *
     * Nested effects are Targeted only, for the same reason Area's are: it forbids a
     * Burst of Bursts, and a Visual that would present once per victim.
     */
    record Burst(double radius, List<Targeted> effects) implements Untargeted {

        public Burst {
            if (radius <= 0) {
                throw new IllegalArgumentException("Effect 'burst' radius must be > 0, got: " + radius);
            }
            effects = List.copyOf(effects);
        }
    }

    /**
     * Applies nested effects to everything in radius, repeatedly.
     *
     * Nested effects are Targeted only, which forbids two things by construction:
     * a nested Area (an area spawning areas every pulse is an unbounded
     * fan-out of scheduled tasks) and a nested Visual (it would present at the
     * area's origin once per combatant per pulse, not at each victim).
     */
    record Area(double radius, int durationTicks, int tickInterval,
                List<Targeted> effects) implements Untargeted {

        public Area {
            // A tickInterval of 0 makes EffectApplier.tickArea reschedule itself forever
            // at zero delay: next = elapsed + 0 never exceeds durationTicks. On a server
            // that is an unbounded task storm, not a hang you would notice in a test.
            // Checked here rather than in AbilityLoader so every caller is defended; the
            // loader's catch(RuntimeException) still turns it into a named, skipped file.
            if (tickInterval <= 0) {
                throw new IllegalArgumentException("Effect 'area' tick_interval must be > 0, got: "
                        + tickInterval);
            }
            if (radius <= 0) {
                throw new IllegalArgumentException("Effect 'area' radius must be > 0, got: " + radius);
            }
            effects = List.copyOf(effects);
        }
    }

    /** Presentation only. Delegated to CombatWorld.present. */
    record Visual(String visualId) implements Untargeted {}

    /**
     * A blast that goes off later. Plants a display-only {@code markerId} at the point it is
     * applied, then after {@code fuseTicks} detonates {@code burst} there (mob-only) and
     * removes the marker. A pure timer -- it fires whether or not anything is near, with no
     * contact detection; the marker only shows where.
     *
     * The general mechanic behind Rekindle's ember, and reusable for any mine / trap /
     * delayed explosion, the same general-mechanic / specific-content split as the rest of
     * the effect grammar. Marker lifetime is exactly the fuse: its removal is tied to the same
     * scheduled task that fires the burst, so display and detonation can never diverge.
     */
    record DelayedBurst(String markerId, int fuseTicks, Burst burst) implements Untargeted {

        public DelayedBurst {
            if (fuseTicks < 1) {
                throw new IllegalArgumentException(
                        "Effect 'delayed_burst' fuse_ticks must be >= 1, got: " + fuseTicks);
            }
        }
    }

    /**
     * Launches a fan of arcing projectiles from the caster and runs {@code onImpact} where
     * each one lands. Directions are the caster's facing rotated by each of
     * {@code anglesDegrees}, horizontally; each projectile flies with {@code gravity} and a
     * touch of {@code launchLift} so it arcs, and its impact (a body, a wall, or lifetime
     * expiry) fires {@code onImpact} through the ordinary effect path -- exactly the grenade's
     * impact-fires-an-effect route, just with a scheduling effect (a {@link DelayedBurst}) in
     * the list instead of an immediate one.
     *
     * The embers arc: it reuses the shared projectile flight loop, not a copy of it. A wall
     * in the way stops an ember short and fires {@code onImpact} AT the wall, not past it.
     *
     * {@code trail} is a visual id left along each ember's arc (a flame trail so the throw
     * reads as a thrown ember, not a bare item), or null for no trail.
     */
    record ThrowEmbers(List<Double> anglesDegrees, double speed, double gravity,
                       double launchLift, int maxLifetimeTicks, String trail,
                       List<EffectSpec> onImpact) implements Untargeted {

        public ThrowEmbers {
            if (anglesDegrees.isEmpty()) {
                throw new IllegalArgumentException("Effect 'throw_embers' needs at least one angle");
            }
            if (maxLifetimeTicks < 1) {
                throw new IllegalArgumentException(
                        "Effect 'throw_embers' max_lifetime_ticks must be >= 1, got: " + maxLifetimeTicks);
            }
            anglesDegrees = List.copyOf(anglesDegrees);
            onImpact = List.copyOf(onImpact);
        }

        /**
         * The launch direction of each ember: {@code facing} rotated horizontally by each
         * angle. Pure geometry, so a mutation to the rotation -- or to how an angle is applied
         * -- reddens a unit test rather than surfacing only in a boot.
         */
        public static List<Vec3> fan(Vec3 facing, List<Double> anglesDegrees) {
            return anglesDegrees.stream().map(facing::rotateAboutY).toList();
        }
    }
}
