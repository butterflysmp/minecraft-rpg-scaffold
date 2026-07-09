package io.github.yourname.rpg.core.ability.effect;

import io.github.yourname.rpg.core.element.Element;
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
            permits Area, Burst, Visual {}

    record Damage(double amount, Element element) implements Targeted {}

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
}
