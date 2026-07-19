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
            permits Damage, Heal, Knockback, Status, WeaponDamage {}

    /** Acts on the world rather than a victim. Always runs. */
    sealed interface Untargeted extends EffectSpec
            permits Area, Burst, ThrowEmbers, Visual {}

    /** element is a content id ("fire", "kinetic", ...) -- pure identity, validated at boot; never null. */
    record Damage(double amount, String element) implements Targeted {}

    /**
     * Deals the CASTER'S attack-damage stat (base + modifiers), resolved at hit time -- not a literal
     * amount. This is the basic melee hit: a weapon's declared attack_damage flows into the caster's
     * ATTACK_DAMAGE stat (a MAIN_HAND modifier), and this reads it back, so the swing and the tooltip
     * share one source of truth. element carries identity exactly like {@link Damage}'s (flavour + kit
     * gating), never a multiplier. Costed/ranged payloads keep the literal {@link Damage}.
     */
    record WeaponDamage(String element) implements Targeted {}

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
     * Throws a fan of REAL items from the caster, then runs ONE per-tick loop per ember: each
     * tick it draws {@code trail} at the item's LIVE position and counts the fuse down, and when
     * the fuse reaches zero it detonates {@code burst} (mob-only) right there and removes the
     * item. The thrown item IS the marker: vanilla physics flies and lands it, so there is no
     * landing detection, no separate marker, and no resting-item pop. This is the old Blast
     * Fungus mechanism.
     *
     * The per-tick loop earns two things at once. The {@code trail} is a clean particle LINE:
     * one flame per tick at where the item actually is, so the item's own motion draws the arc
     * (as opposed to the old scattered trail, or a computed flight path that no longer exists).
     * And because the loop re-enters the item's region every tick to read its position, the
     * detonation runs on the region that owns the item -- region-correct on Folia, exactly as
     * ProjectileFlight re-enters each tick for the grenade.
     *
     * Directions are the caster's facing rotated horizontally by each of {@code anglesDegrees};
     * each item launches at {@code speed} with a touch of {@code launchLift} so it arcs.
     * {@code itemId} is the material thrown (e.g. blaze_powder). {@code visual} is presented at
     * the detonation point when the fuse fires (a boom/flash), or null for none. {@code trail}
     * is the per-tick flight particle id, or null for a bare item.
     *
     * {@code speed} is a real launch velocity (blocks/tick), interpreted by the adapter's
     * physical item -- NOT the old virtual per-tick flight step, so it lives on a different
     * scale and is tuned by feel on the content loop.
     */
    record ThrowEmbers(List<Double> anglesDegrees, double speed, double launchLift,
                       String itemId, int fuseTicks, Burst burst, String visual, String trail)
            implements Untargeted {

        public ThrowEmbers {
            if (anglesDegrees.isEmpty()) {
                throw new IllegalArgumentException("Effect 'throw_embers' needs at least one angle");
            }
            if (fuseTicks < 1) {
                throw new IllegalArgumentException(
                        "Effect 'throw_embers' fuse_ticks must be >= 1, got: " + fuseTicks);
            }
            anglesDegrees = List.copyOf(anglesDegrees);
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
