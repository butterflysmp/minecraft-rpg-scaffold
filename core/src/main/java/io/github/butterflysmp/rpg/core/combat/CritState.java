package io.github.butterflysmp.rpg.core.combat;

/**
 * Whether a hit CRIT, and by how much.
 *
 * <p>Replaced a bare {@code boolean wasCrit} on the damage port, because two adjacent transposable
 * flags compile in either order and mean opposite things swapped. It is the first of three of the
 * same species, beside {@link DefenseRule} and {@link AccrualRule}.
 *
 * <h2>IT NOW CARRIES THE MULTIPLIER, AND {@code isCrit()} IS DERIVED FROM IT</h2>
 *
 * <b>The precedent is {@code Caster.crit()}, not an invention here:</b> that method derives the fact
 * from the frozen multiplier <i>"rather than carried beside it, so the two cannot disagree about one
 * swing"</i>. This is the same shape one layer down. Strictly GREATER than {@link Crit#NO_CRIT}, for
 * the same reason it gives: a crit whose bonus resolved to 0 multiplies by exactly 1.0 and changed
 * nothing, so there is no hit to celebrate.
 *
 * <p><b>WHY THE MULTIPLIER HAD TO TRAVEL.</b> The scorch cap is the hit's DECLARED magnitude, and
 * the accrual site only has the RESOLVED one -- post-charge, post-crit. It needs the crit taken back
 * out, and the alternatives were worse: a seventh port parameter carrying the crit-free figure would
 * have put two adjacent {@code double}s on the signature, which is the exact transposable pair
 * {@code DamageSignatureTest} exists because of, and a swap would silently cap the burn at half the
 * right figure with nothing failing.
 *
 * <p><b>The site RECOVERS the declared magnitude by dividing; it is not handed it.</b> That is
 * weaker than being told, and it is worth saying so where it happens. The upgrade trigger is
 * written down too: the day a SECOND consumer needs a crit-free magnitude, the division stops being
 * a local convenience and a {@code HitAmount(total, preCrit)} record -- the symmetric twin of
 * {@code DamageOutcome} -- is owed.
 *
 * <h2>THE DISPLAY SEAM TAKES A BOOLEAN AGAIN, AND THAT IS NOT A REGRESSION</h2>
 *
 * {@code DamageNumberText} was moved from {@code boolean} to this type on vocabulary grounds, when
 * this type WAS a bare fact. It has moved back, because the type now means <b>the crit FACTOR</b>
 * and the display seam genuinely does not have one: {@code HealthChange} carries a bit, and
 * fabricating a multiplier to satisfy a signature would be the "absence is not a neutral value"
 * error in a new costume -- a made-up number that reads as real and could be divided by.
 *
 * <p>So there is no {@code CRIT} constant and no {@code of(boolean)}: every {@code CritState} in the
 * system carries a multiplier somebody actually resolved.
 *
 * @param multiplier what the hit was multiplied by. {@link Crit#NO_CRIT} (exactly 1.0) for a normal
 *                   hit, and the resolved factor for a crit. Never below {@code NO_CRIT} -- a
 *                   negative or zero bonus resolves to {@code NO_CRIT} rather than shrinking a hit.
 */
public record CritState(double multiplier) {

    /** A hit that did not crit: multiplied by exactly one. */
    public static final CritState NORMAL = new CritState(Crit.NO_CRIT);

    public CritState {
        if (multiplier < Crit.NO_CRIT) {
            throw new IllegalArgumentException(
                    "A crit multiplier below " + Crit.NO_CRIT + " would SHRINK the hit, which is not"
                            + " what a crit is -- got " + multiplier + ". Crit.multiplier already"
                            + " floors a non-positive bonus at NO_CRIT, so reaching here means a"
                            + " caller computed one some other way.");
        }
    }

    /**
     * The state for a resolved multiplier. {@link Crit#NO_CRIT} yields {@link #NORMAL}, so a crit
     * whose bonus resolved to nothing is not treated as a crit anywhere downstream.
     */
    public static CritState of(double multiplier) {
        return multiplier > Crit.NO_CRIT ? new CritState(multiplier) : NORMAL;
    }

    /** True when the hit was actually multiplied. DERIVED, so it cannot disagree with the factor. */
    public boolean isCrit() {
        return multiplier > Crit.NO_CRIT;
    }
}
