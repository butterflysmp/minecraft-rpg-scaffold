package io.github.butterflysmp.rpg.core.combat;

/**
 * Whether a particular hit is subject to {@link Defense}, as a TYPE rather than as a boolean.
 *
 * <h2>WHY THIS IS NOT A {@code boolean}, AND IT IS NOT A STYLE PREFERENCE</h2>
 *
 * <b>This parameter's wiring is the one claim in the scorch slice that CANNOT BE WITNESSED IN GAME,
 * and it will stay that way.</b> The gate row written for it ({@code GATE-scorch-slice-1.md}'s
 * {@code S5}) needs two targets of the same max differing only in armour. Defense is player-only --
 * {@code reconcileDefenseModifiers} has exactly one production caller, {@code PlayerHealthSystem},
 * on a scan of WORN GEAR, and {@code MobDefinition} carries no defense field at all -- so the row
 * needs a second player, and nothing but another player can scorch a player. There is no second
 * account. The row is unreachable, not deferred.
 *
 * <p>What that row uniquely covered was never the arithmetic. The arithmetic has witnesses:
 * {@code CombatantStatsTest.bypassesDefenseSkipsTheCurveENTIRELYRatherThanReducingItsCut} proves the
 * curve is skipped, and {@code FakeWorld} records the flag arriving at the sink. <b>What had no
 * witness was the PAPER WIRING passing the right value</b>, and it was written as:
 *
 * <pre>
 *   handle().applyDamage(amount, applierId, false, true);   // two bare positional booleans
 * </pre>
 *
 * <b>Transposed, that still compiles, and it silently means "this was a crit, and Defense applies".</b>
 * A percent-of-max burn quietly trimmed by armour is no longer percent-of-max damage -- it is
 * ordinary damage wearing a percentage, which defeats the one property the shape was chosen for.
 *
 * <p><b>Named constants would NOT have fixed this.</b> {@code applyDamage(amount, id, NOT_A_CRIT,
 * BYPASSES_DEFENSE)} reads better and compiles exactly as happily when the two are swapped, because
 * both are still {@code boolean}. Only a distinct TYPE makes the transposition a compile error, which
 * is the whole requirement: <b>a claim that cannot be witnessed should be made impossible to get
 * wrong, not left to a row that will never be ticked.</b>
 *
 * <p>This is the mutation-unexpressibility move ({@code NEXT.md}, "A MUTATION THAT CANNOT BE
 * EXPRESSED IS A PROPERTY THE TYPE ENFORCES") arriving from the other side. There, making a defect
 * unexpressible was the BETTER of two options. Here it is the ONLY one, because the witness is
 * unreachable rather than merely expensive.
 *
 * <h2>The name is the standing question's, not scorch's</h2>
 *
 * {@code NEXT.md}'s open item is <i>"which causes should {@code Defense} touch?"</i>, and drowning's
 * deferred rule ("10% of max health, REGARDLESS of defense") needs this same seam. Scorch is the
 * first CONSUMER, not the owner -- so this is named for the property, exactly as the boolean it
 * replaces was.
 */
public enum DefenseRule {

    /** {@link Defense#applyDefense} takes its cut. The default for every ordinary hit. */
    APPLIES,

    /**
     * {@link Defense} is skipped ENTIRELY -- not reduced, not partially applied.
     *
     * <p>Armour still reaches a bypassing DoT, through stack ACCRUAL rather than through mitigation:
     * fewer points landed is fewer stacks, so armour DELAYS scorch rather than blunting it.
     */
    BYPASSED
}
