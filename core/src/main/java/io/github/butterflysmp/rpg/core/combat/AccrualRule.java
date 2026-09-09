package io.github.butterflysmp.rpg.core.combat;

/**
 * Whether a hit's element may ACCRUE its declared status, or merely wears the element for display.
 *
 * <p>The third of the same species on this port, and it exists for the reason the other two do:
 * {@link CritState} and {@link DefenseRule} each replaced a bare {@code boolean} because adjacent
 * transposable flags compile in either order and mean opposite things swapped. A reader looking for
 * one of the three should find all three here.
 *
 * <h2>IT REPLACES A GUARD MADE OF ABSENCE, AND THAT IS THE POINT</h2>
 *
 * Scorch's own burn tick used to reach the damage port through the element-LESS arity, so it had no
 * element to pass and therefore could not accrue more scorch. That worked, and it cost the burn its
 * glyph: a fire burn drew an unmarked number beside the marked hit that lit it.
 *
 * <p><b>The naive fix would have been a second nullable String</b> -- {@code element} for accrual
 * beside {@code displayElement} for the glyph. Two adjacent nullable strings, transposable, in a
 * codebase that has {@code DamageSignatureTest} precisely because of that class. Swapping them would
 * silently re-enable the loop AND drop the glyph: the two worst outcomes available, from one typo,
 * with no compile error. So the element stays single and the RULE becomes a named value.
 *
 * <h2>WHAT THIS BUYS BEYOND THE GLYPH: THE GUARD BECOMES ASSERTABLE</h2>
 *
 * <b>You cannot assert on a null that means something.</b> While the burn carried no element, the
 * loop guard's only witness was behavioural -- "a burn tick accrues no stacks" -- and that row could
 * not tell the guard from an ordinary registry miss, because a null element misses the registry
 * anyway. It passed either way.
 *
 * <p>Now the burn HAS an element, the registry hits, and {@link #INERT} is the only thing that can
 * produce the observation. The row goes from ambiguous to precisely targeted, which is what a named
 * value buys over an absent one.
 *
 * <h2>TWO REASONS FOR "NO ACCRUAL", AND THEY MUST STAY DISTINGUISHABLE</h2>
 *
 * Same outcome, different facts, and {@code ElementAccrual.forHit} returns empty for each by a
 * different route. Collapsing them into "the accrual guard" is how one of them gets deleted:
 *
 * <ul>
 *   <li><b>A null element</b> means the hit HAS no element -- the vanilla-damage boundary (fall,
 *       drowning, lava), a thorns reflect, the dev damage and apply commands. The short arities keep
 *       meaning exactly this, and it is CORRECT for every one of their callers. <b>Do not retrofit
 *       {@link #INERT} onto them to look tidy: lava genuinely has no element.</b></li>
 *   <li><b>{@link #INERT}</b> means the hit HAS an element and must not accrue -- scorch's own burn
 *       tick, and nothing else today.</li>
 * </ul>
 */
public enum AccrualRule {

    /**
     * The hit's element accrues whatever status it declares. Every payload-driven hit, and the sweep
     * rider -- a swept bystander is taking the same weapon's fire, so it burns like the primary did.
     */
    ACCRUES,

    /**
     * The hit wears its element for DISPLAY only and accrues nothing.
     *
     * <p><b>Scorch's own burn passes this, and it is the loop guard.</b> Without it a burn tick would
     * accrue stacks from itself: each period would re-apply the status, refresh the window and add to
     * the count, and the clock would never run out. That failure is a RUNAWAY rather than a wrong
     * number, which is worth knowing when mutating it -- against a fake sink it presents as a hang,
     * and a hang gets blamed on the harness rather than on the change. Bound the assertion to a fixed
     * number of periods so the mutation reddens on a count instead of on termination.
     */
    INERT;

    /** True for {@link #ACCRUES}. For the one site that branches on it. */
    public boolean accrues() {
        return this == ACCRUES;
    }
}
