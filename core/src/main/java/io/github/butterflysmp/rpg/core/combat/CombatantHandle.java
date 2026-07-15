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
     * This port carries a number and a culprit, nothing else -- element is identity, not
     * math, and never reaches here. An element would only regain a bearing on the number if
     * a real resistance system existed, and that would belong in core, not in a call to the
     * server.
     *
     * @param sourceId who to blame -- for aggro and kill credit. Never an entity
     *                 reference: a lingering area outlives its caster, and holding one
     *                 would pin it. May resolve to nothing, in which case the adapter
     *                 deals the damage unattributed rather than lying about it.
     */
    void applyDamage(double amount, UUID sourceId);

    void applyHeal(double amount);

    void applyKnockback(Vec3 direction, double strength);

    /**
     * Set the combatant's velocity outright to {@code velocity} -- a self-propelled impulse,
     * the mechanism behind a dash. Distinct from {@link #applyKnockback} on purpose:
     * knockback is additive and points away from an impact, whereas this REPLACES velocity so
     * a dash goes a controlled distance regardless of the momentum the caster already carried.
     */
    void applyImpulse(Vec3 velocity);

    void applyStatus(String statusId, int durationTicks, int amplifier);
}
