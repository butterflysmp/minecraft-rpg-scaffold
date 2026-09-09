package io.github.butterflysmp.rpg.core.combat.stat;

/**
 * What one call to {@link CombatantStats#damage} actually did: how much landed, and where it left the
 * target.
 *
 * <p>Both numbers exist inside {@code damage} already and were discarded. They are returned because
 * stack accrual needs each of them and can derive neither: it runs in the adapter, AFTER the call, on
 * the far side of the mitigation curve.
 *
 * <p><b>THIS CARRIES FACTS, NOT A POLICY.</b> {@code newCurrent} rather than a
 * {@code boolean stillAlive} or {@code targetDied}, because the caller's rule ("accrue only onto
 * something still standing") is legible where it is READ, and a boolean named for one consumer's
 * predicate would have to be renamed by the second. The record reports the state; the reader decides
 * what it means.
 *
 * <p><b>WHY NOT {@code reachedZero}, WHICH ALREADY EXISTS ON {@link HealthChange}.</b> That bit is
 * TRANSITION-ONLY -- {@code HealthState.damage} returns {@code before > 0.0 && current == 0.0}, so it
 * fires exactly once, which is right for a death hook and wrong here. {@code MobDeathSystem.shouldKill}
 * is {@code reachedZero() && !targetIsPlayer()}, so <b>a player at zero custom health is never killed
 * or removed</b> -- they stay tracked, alive, at the floor. A further hit on them reports
 * {@code reachedZero == false}, and a consumer keyed on the transition would treat a combatant at zero
 * health as a live target. That is D3b's shape, where a floor render overwrote a death for the same
 * reason. For a mob the difference is unreachable only because the store entry is gone by the second
 * hit -- and relying on that would be reading the mob path's cleanup ordering as a signal.
 *
 * <p><b>Both components are doubles and so transposable in principle.</b> There is one construction
 * site, the accessors are named, and {@code CombatantStatsTest} pins the two to DIFFERENT values on
 * purpose so a swapped construction reddens rather than passing on a coincidence.
 *
 * <p><b>KNOWN LIMITATION: {@link #UNTRACKED} is indistinguishable from TRACKED-AT-ZERO from the
 * return value alone.</b> Both read {@code (0.0, 0.0)}. That is a policy-shaped compromise in a
 * record that otherwise refuses policy: the honest fact for an untracked id is "there is no
 * health", which is not the number zero. It does not bite today -- the only consumer gates on
 * {@code newCurrent > 0} and both readings agree there -- and the fixes (an Optional, or a third
 * component) cost more than the confusion. Named here rather than left to be derived, because a
 * later consumer that needs to tell the two apart will get SILENCE from this type rather than a
 * compile error.
 *
 * @param dealt      the POST-MITIGATION amount that actually landed; {@code 0.0} on an untracked
 *                   combatant, where nothing happened at all
 * @param newCurrent the target's custom current health AFTER the hit. {@code 0.0} on an untracked
 *                   combatant, which reads correctly as "not standing" -- nothing to accrue onto
 */
public record DamageOutcome(double dealt, double newCurrent) {

    /** Nothing happened: the combatant is not tracked, so no damage landed and there is no health. */
    public static final DamageOutcome UNTRACKED = new DamageOutcome(0.0, 0.0);
}
