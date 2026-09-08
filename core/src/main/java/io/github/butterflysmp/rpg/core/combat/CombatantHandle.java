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
     * This port carries a number, a culprit, and -- since crit, and now since elements -- two
     * PRESENTATION facts.
     *
     * <p><b>ELEMENT REACHES HERE NOW, AND IS STILL NOT MATH.</b> This javadoc previously said it
     * <i>"still never reaches here"</i>, on the reasoning that an element would only regain a bearing
     * on the number if a real resistance system existed. <b>That reasoning is still correct and the
     * conclusion no longer follows</b>: the element multiplies nothing, and if it ever did that would
     * belong in core rather than in a call to the server. It rides for two things nothing downstream
     * can derive -- the damage number's GLYPH, which is a content fact only {@code ElementRegistry}
     * holds, and STACK ACCRUAL, which has to know which status the hit accrues.
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
        applyDamage(amount, sourceId, CritState.NORMAL);
    }

    /**
     * As above, stating whether the hit was a CRIT.
     *
     * @param crit for display only -- the crit multiplier is already inside {@code amount}.
     *             Applying it again here would square the crit.
     */
    default void applyDamage(double amount, UUID sourceId, CritState crit) {
        applyDamage(amount, sourceId, crit, DefenseRule.APPLIES);
    }

    /**
     * As above, stating whether this damage SKIPS the Defense curve entirely.
     *
     * <h2>THIS ARITY WEARS NO ELEMENT, AND THAT IS THE LOOP GUARD</h2>
     *
     * <b>DO NOT "TIDY" THIS INTO A DELEGATION THAT PASSES SOME DEFAULT ELEMENT.</b> It delegates with
     * {@code null} on purpose, and the whole anti-loop property of stack accrual rests on it.
     *
     * <p>Scorch's own burn tick reaches the port through here ({@code EntityScorchSink}), so it has no
     * element to pass, so it <b>cannot accrue more scorch</b>. That is the loop made
     * <i>unrepresentable</i> rather than checked: there is no flag to get wrong, no ordering to
     * preserve, and no condition a later reader has to know about. Give this arity a default element
     * and a burn begins feeding itself its own ticks -- silently, and only on a large health pool,
     * where a 5%-of-max burn is big enough to clear {@code Scorch.DAMAGE_PER_STACK}.
     *
     * <p>Every other elementless caller reaches the port here too, and each is correct rather than
     * merely unconverted: the thorns reflect (a shield returning force is not the attacker's element),
     * the vanilla-damage boundary (fall, drowning and lava have no element and inventing one would be
     * a lie), and the dev apply command (an instrument, not a hit).
     *
     * <h2>AND THE DEFENSE RULE IS THE FIRST PER-CAUSE RULE, NOT A SCORCH FEATURE</h2>
     *
     * {@code NEXT.md}'s standing question -- <i>"which causes should {@code Defense} touch?"</i> --
     * records that {@code CombatantStats.damage} applies {@code Defense.applyDefense} to every cause
     * unconditionally, and that <b>there is no route to "ignores defense" today.</b> The operator's
     * drowning rule ("10% of max health, REGARDLESS of defense or max health") has been waiting on
     * exactly this parameter; scorch is merely its first consumer.
     *
     * <p><b>Hence the name.</b> It states the PROPERTY, not the feature. A {@code boolean isScorch}
     * would have to be replaced by whatever eventually answers the standing question; {@code
     * bypassesDefense} generalises by having a second caller added, not by being redesigned. Do not
     * rename it after a status.
     *
     * <p><b>What it is NOT:</b> not vanilla's {@code bypasses_armor} tag list. {@code NEXT.md} is
     * explicit that the tag list <i>"is EVIDENCE, NOT A MANDATE"</i> -- a BALANCE list, unlike the
     * i-frame and ratchet mechanisms where diverging from vanilla produced bugs. Adopting it wholesale
     * needs the operator, per cause. This parameter makes that conversation implementable; it does not
     * pre-answer it.
     *
     * <p><b>BOTH TRAILING PARAMETERS ARE TYPES RATHER THAN BOOLEANS, AND THAT IS LOAD-BEARING.</b>
     * They were {@code boolean wasCrit, boolean bypassesDefense} -- two bare positional flags that
     * compile in either order and mean opposite things swapped. The single call site passing both
     * explicitly ({@code EntityScorchSink}) is in {@code paper/}, reachable only through a real Bukkit
     * entity, and the gate row written to witness it is UNREACHABLE: see {@link DefenseRule}'s javadoc
     * for why, and for why named constants would not have helped.
     *
     * @param defense {@link DefenseRule#BYPASSED} makes the amount land whole. Percent-of-max damage is
     *                the shape this exists for: cutting a 5%-of-max burn with armour makes it ordinary
     *                damage with extra arithmetic rather than the anti-tank tool it was specified as.
     */
    default void applyDamage(double amount, UUID sourceId, CritState crit, DefenseRule defense) {
        applyDamage(amount, sourceId, crit, defense, null);
    }

    /**
     * As above, naming the ELEMENT this damage wears.
     *
     * <p>Identity and presentation, never a factor: it multiplies nothing. Two consumers need it and
     * neither can derive it -- the damage number's glyph (content, held by {@code ElementRegistry})
     * and stack accrual (which status, and how many stacks the landed damage buys).
     *
     * <p><b>Accrual reads the POST-mitigation number, which is why it cannot happen in
     * {@code EffectApplier}.</b> The applier sits upstream of the Defense curve; what actually landed
     * is known only after {@code CombatantStats.damage}, which returns it for exactly this reason.
     * The operator's ruling is that <i>armour DELAYS scorch rather than blunting it</i>, and the
     * post-mitigation figure is the entire content of "delays".
     *
     * @param element a loaded element id ("fire", "kinetic", ...), or {@code null} for damage that
     *                wears none. Null is the honest value for every non-payload path -- see the
     *                four-argument arity above, whose elementlessness is the loop guard.
     */
    void applyDamage(double amount, UUID sourceId, CritState crit, DefenseRule defense, String element);

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

    default void applyStatus(String statusId, int durationTicks, int amplifier) {
        applyStatus(statusId, durationTicks, amplifier, null, 0.0);
    }

    /**
     * As above, naming WHO applied the status and HOW HARD the payload that carried it hits.
     *
     * <p>Both were unrepresentable before, and a status that needed either had to do without. Scorch
     * needs both: {@code applierId} is the kill credit, and {@code sourceDamage} is the cap that stops
     * a percent-of-max-health burn running away on a large health pool.
     *
     * <p><b>{@code sourceDamage} is named for what it IS, not for the one status that reads it.</b> It
     * is the payload's headline damage as {@code DamagePayload.of} resolves it -- an authored literal,
     * or the wielder's attack stat for a {@code weapon_damage} payload -- which is the same number the
     * tooltip prints. A field called {@code scorchCap} would have to be renamed by the second consumer.
     *
     * @param applierId    who to credit; may be null where nothing applied it (a dev command)
     * @param sourceDamage the payload's damage, or {@code <= 0} when the payload declares none. An
     *                     adapter must treat non-positive as UNDECLARED and substitute a conservative
     *                     constant -- <b>never as "no cap"</b>, which for a percent-of-max effect is
     *                     not a fallback but the absence of one
     */
    void applyStatus(String statusId, int durationTicks, int amplifier,
                     UUID applierId, double sourceDamage);
}
