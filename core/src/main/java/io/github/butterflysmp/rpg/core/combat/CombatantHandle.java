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
     * This port carries a number, a culprit, and -- since crit -- one PRESENTATION fact. Element is
     * still identity, not math, and still never reaches here: an element would only regain a bearing
     * on the number if a real resistance system existed, and that would belong in core, not in a call
     * to the server.
     *
     * <p>{@code wasCrit} is a deliberate widening of "a number and a culprit", and it earns its place
     * by being underivable downstream rather than by being convenient. The crit multiplier is rolled
     * once per cast and frozen on the caster, on the DEALER's thread; the damage is then hopped onto
     * the TARGET's thread and lands a tick later, where the amount alone cannot say whether it was
     * doubled. It changes no arithmetic -- the multiplier is already inside {@code amount} -- and
     * exists so the damage number can be styled and the crit particle can fire.
     *
     * @param sourceId who to blame -- for aggro and kill credit. Never an entity
     *                 reference: a lingering area outlives its caster, and holding one
     *                 would pin it. May resolve to nothing, in which case the adapter
     *                 deals the damage unattributed rather than lying about it.
     */
    default void applyDamage(double amount, UUID sourceId) {
        applyDamage(amount, sourceId, false);
    }

    /**
     * As above, stating whether the hit was a CRIT.
     *
     * @param wasCrit for display only -- the crit multiplier is already inside {@code amount}.
     *                Applying it again here would square the crit.
     */
    void applyDamage(double amount, UUID sourceId, boolean wasCrit);

    /**
     * Raise the target's health by {@code amount}, capped at its max by the implementation.
     *
     * <p><b>No {@code sourceId}, unlike {@link #applyDamage}</b>, so an implementation has nobody to
     * credit and attributes the heal to the target. Nothing reads a heal's dealer today. Widening
     * this is what a heal-credit feature -- a support archetype's contribution, a heal popup -- would
     * have to start with.
     */
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
