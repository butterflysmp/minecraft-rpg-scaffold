package io.github.butterflysmp.rpg.core.anvil;

/**
 * Everything the anvil has decided about the pair in front of it, as one comparable value.
 *
 * <h2>*** ONE PRODUCER, CALLED TWICE ***</h2>
 *
 * Ben's ruling, and it is the reason this type exists rather than two loose values. <b>The decision
 * {@code paper} RENDERS and the decision it RECOMPUTES at the moment of the click must come from
 * this one function.</b> If the rendered value were built by one path and the recomputed value by
 * another, a mismatch would no longer mean <i>something changed</i> — it would mean <i>the two paths
 * disagree</i>, which is a different fact wearing the same face, and the confirm handler could not
 * tell them apart.
 *
 * <h2>*** IT CARRIES ONLY DECISION-BEARING FIELDS ***</h2>
 *
 * The verdict arm and its data, and the affordability. <b>No rendered text, no tick counts, nothing
 * incidental.</b> A record, so equality is structural and nothing in it can differ for a reason that
 * is not a change in the decision.
 *
 * <p><b>*** DO NOT COMPARE RENDERED TEXT INSTEAD. ***</b> {@code GrindstoneMenu} keeps a
 * {@code lastConfirmText} beside the code this slice adds, and it is a <b>repaint-suppression
 * cache</b>, not a decision. <b>Two different decisions format identically</b> — the same target
 * rarity means the same cost line, so a donor swapped for one with a different score renders the
 * same string while producing a different result. A text comparison passes that as "unchanged" and
 * applies the wrong score: a false negative on the only irreversible action in the feature.
 *
 * @param verdict       what {@code AnvilTransfer} ruled about the two items
 * @param affordability whether the player can pay, for a verdict that has a price
 */
public record AnvilDecision(AnvilVerdict verdict, Affordability affordability) {

    public AnvilDecision {
        if (verdict == null || affordability == null) {
            throw new IllegalArgumentException(
                    "a decision is both halves; a null here means the producer was bypassed");
        }
    }

    /**
     * Decide everything, from the two slots and the wallet.
     *
     * <p><b>{@code AnvilTransfer.evaluate} is called, not reimplemented</b> — its fifteen rows
     * remain the guard on the verdict, and this method adds exactly one fact on top.
     *
     * @param walletPoints the player's whole bank in POINTS, from {@code XpCurve.totalPoints}
     */
    public static AnvilDecision of(Side target, Side donor, int walletPoints) {
        AnvilVerdict verdict = AnvilTransfer.evaluate(target, donor);
        // ONLY A READY VERDICT HAS A PRICE. Asking whether a refused pair is affordable would make
        // the screen name the wrong problem -- see Affordability.Affordable.
        Affordability affordability = verdict instanceof AnvilVerdict.Ready ready
                ? Affordability.of(ready.xpPoints(), walletPoints)
                : Affordability.AFFORDABLE;
        return new AnvilDecision(verdict, affordability);
    }

    /**
     * May a confirm click act on this decision?
     *
     * <h2>BOTH HALVES, AND ASKING ONLY ONE IS THE DEFECT</h2>
     *
     * The same shape as {@code GearScore.carriesScore}: two independent refusals composed into one
     * door. A {@code Ready} verdict the player cannot pay for is not actionable, and an
     * {@code Affordable} answer on a refused pair means nothing on its own.
     *
     * <p><b>It is also the transition predicate the arming deadline watches.</b> The deadline resets
     * whenever this goes from {@code false} to {@code true} — a decision that becomes ACTIONABLE
     * starts the clock from zero — and <b>never when it goes the other way</b>, because a decision
     * that becomes refused needs no arm; it is already refused.
     */
    public boolean actionable() {
        return verdict instanceof AnvilVerdict.Ready
                && affordability instanceof Affordability.Affordable;
    }
}
