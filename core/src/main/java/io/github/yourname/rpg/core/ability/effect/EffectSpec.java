package io.github.yourname.rpg.core.ability.effect;

import io.github.yourname.rpg.core.element.Element;
import java.util.List;

/**
 * A composable, declarative description of what an ability *does*.
 * Loaded from YAML. Never contains behaviour -- only data.
 *
 * Sealed so that EffectApplier's switch is exhaustive: add a new effect type
 * and the compiler tells you every place that must handle it.
 */
public sealed interface EffectSpec {

    record Damage(double amount, Element element) implements EffectSpec {}

    record Heal(double amount) implements EffectSpec {}

    record Knockback(double strength) implements EffectSpec {}

    /** e.g. "scorch", "slow", "suppress" -- resolved by the status registry. */
    record Status(String statusId, int durationTicks, int amplifier) implements EffectSpec {}

    /** Applies nested effects to everything in radius, repeatedly. */
    record Area(double radius, int durationTicks, int tickInterval,
                List<EffectSpec> effects) implements EffectSpec {}

    /** Presentation only. Delegated to CombatWorld.present. */
    record Visual(String visualId) implements EffectSpec {}
}
