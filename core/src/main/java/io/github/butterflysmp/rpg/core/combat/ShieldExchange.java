package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.enchant.Thorns;

/**
 * Both halves of a blocked hit, decided together from ONE input: what reaches the victim, and what
 * goes back to the attacker.
 *
 * <h2>Why this exists, and it is a TESTABILITY decision rather than a tidiness one</h2>
 *
 * Thorns's single load-bearing rule is that it reflects a fraction of the PRE-MITIGATION blow --
 * the attacker's raw stat, before the block fraction and before armor. The natural way to write the
 * rider is to reduce a local and then reflect, and reflecting off the reduced local instead is a
 * silent wrong answer: at the shipped numbers it turns 4.5 into 2.25 with nothing red anywhere.
 *
 * <p>That mistake lives in {@code RpgListeners.onMobMeleeAttack}, which <b>cannot be unit-tested</b>
 * -- it needs a live {@code Player}, a live {@code LivingEntity} and a real {@code BLOCKING}
 * modifier, and no listener test exists or can. A pure {@code Thorns.reflected} test cannot help:
 * it can pin the arithmetic but not say WHICH value the rider passed in.
 *
 * <p>So the CHOICE is moved here, where a test can reach it. {@link #of} takes the raw stat once and
 * returns both numbers, so the rider never holds a reduced value that could be mis-passed -- the
 * reduction happens INSIDE. A mutation that reflects off the reduced figure now reddens
 * {@code ./mvnw -pl core test} in two seconds instead of surviving to a boot gate.
 *
 * <p><b>What this does NOT make testable</b>, stated so the coverage is not overclaimed: the ORDER in
 * which the rider deals the two numbers, and the inline {@code Regions.requireOwned} throw that makes
 * "reflect last" the safe placement. Those stay in the rider and stay boot-gated. This class closes
 * the value-selection trap only.
 *
 * <p>The composition itself is not new arithmetic -- {@link Shield#applyBlock} and
 * {@link Thorns#reflected} are unchanged and still individually tested. This only fixes what they
 * are both fed.
 */
public record ShieldExchange(double applied, double reflected) {

    /**
     * Resolve one incoming hit against a shield.
     *
     * @param preMitigation the attacker's raw attack stat -- before the block AND before the victim's
     *                      armor, which lands a thread-hop later in {@code CombatantStats.damage}.
     *                      <b>Both</b> returned values are derived from this one input; that is the
     *                      whole point of the type.
     * @param blocked       vanilla's raised-AND-frontal-AND-in-arc verdict, already resolved. False
     *                      means the hit lands whole and nothing comes back, which is also what a
     *                      broken, untagged or dangling shield produces.
     * @param effectiveDr   the shield's block fraction with Bulwark already composed and clamped.
     * @param reflectPercent Thorns's summed percentage, 0 when the shield carries none.
     */
    public static ShieldExchange of(double preMitigation, boolean blocked,
                                    double effectiveDr, double reflectPercent) {
        if (!blocked) {
            // No block, no reflect -- and the hit passes undiminished. One arm, so "not blocked"
            // cannot accidentally reflect: the broken-shield gate and the hit-from-behind case both
            // arrive here, and neither should send anything back.
            return new ShieldExchange(preMitigation, Thorns.NONE);
        }
        return new ShieldExchange(
                Shield.applyBlock(preMitigation, effectiveDr),
                Thorns.reflected(preMitigation, reflectPercent));
    }
}
