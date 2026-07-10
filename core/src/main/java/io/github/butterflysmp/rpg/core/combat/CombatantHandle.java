package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.UUID;

/**
 * A way to act on a combatant. Dispatches; never returns world state.
 *
 * Every method here is fire-and-forget: the Paper adapter hops each one onto the thread
 * that owns the entity. That is why nothing returns a value -- you cannot hop a thread and
 * still answer synchronously. Reads live on CombatantSnapshot, captured up front.
 */
public interface CombatantHandle {

    UUID id();

    /**
     * Deal {@code amount} damage, attributed to {@code sourceId}.
     *
     * The elemental multiplier is ALREADY APPLIED by EffectApplier before this is called
     * (Element.multiplierAgainst, against the target's shield). Do not re-add an Element
     * parameter: this port carries a number and a culprit, nothing else. An element only
     * regains meaning when a real resistance system exists, and that belongs in core, not
     * in a call to the server.
     *
     * @param sourceId who to blame -- for aggro and kill credit. Never an entity
     *                 reference: a lingering area outlives its caster, and holding one
     *                 would pin it. May resolve to nothing, in which case the adapter
     *                 deals the damage unattributed rather than lying about it.
     */
    void applyDamage(double amount, UUID sourceId);

    void applyHeal(double amount);

    void applyKnockback(Vec3 direction, double strength);

    void applyStatus(String statusId, int durationTicks, int amplifier);
}
