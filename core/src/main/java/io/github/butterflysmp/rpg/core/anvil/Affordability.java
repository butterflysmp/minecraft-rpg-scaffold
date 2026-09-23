package io.github.butterflysmp.rpg.core.anvil;

/**
 * Whether the player can pay for a transfer the anvil has already ruled legal.
 *
 * <h2>*** A SECOND CORE FUNCTION, NOT A WIDER {@code evaluate} ***</h2>
 *
 * Ben's ruling. {@code AnvilTransfer.evaluate} does <b>not</b> gain a wallet parameter: a signature
 * change invalidates every incremental result, and slice 13a's fifteen rows are the incremental
 * result. Affordability is layered on instead, by {@link AnvilDecision}.
 *
 * <p><b>It is a separate question from the verdict, not a sixth refusal arm.</b> The verdict answers
 * <i>may these two items trade</i> — a fact about the items. This answers <i>can this player pay for
 * it right now</i> — a fact about the player, and the only input to the decision that is not an
 * input SLOT. That difference is why the wallet can change with no gesture on the screen, and it is
 * the whole reason the confirm click re-evaluates.
 *
 * <h2>THE WALLET IS IN POINTS, ALWAYS</h2>
 *
 * Never levels. {@code XpCurve}'s own javadoc: <i>"level 40 is 2920 points, level 20 is 550, so half
 * the levels is not half the money. Anything that charges a player has to work in points or it
 * charges a different price at every level while looking like it charges one."</i>
 */
public sealed interface Affordability {

    /**
     * The player can pay.
     *
     * <p><b>Also the answer for a verdict that is not {@code Ready} at all</b>, and that is
     * deliberate rather than a fudge: it means <i>affordability is not the reason this is refused</i>.
     * A refused pair has no price, so reporting it as unaffordable would make the screen name the
     * wrong problem. {@link AnvilDecision#actionable()} requires BOTH halves, so nothing acts on
     * this value alone.
     */
    record Affordable() implements Affordability {}

    /**
     * The transfer is legal and the player is short.
     *
     * <p>Carries BOTH numbers, as {@code AnvilVerdict.DonorNotHigher} does, so the sentence can say
     * how far short rather than leaving the player to open two screens and subtract.
     */
    record CannotAfford(int xpPoints, int wallet) implements Affordability {

        /**
         * <b>The register is {@code EnchantMenu}'s, reused rather than invented.</b> That screen
         * already says <i>"&lt;name&gt; costs &lt;cost&gt; XP; you have &lt;wallet&gt;."</i> for exactly
         * this fact, and a second phrasing for one fact is two accounts of one rule.
         */
        public String sentence() {
            return "This transfer costs " + xpPoints + " XP; you have " + wallet + ".";
        }
    }

    /** Allocation-free {@link Affordable}: the common answer, once per repaint. */
    Affordability AFFORDABLE = new Affordable();

    /**
     * Can this wallet pay this price?
     *
     * <p><b>Exact cost is AFFORDABLE.</b> {@code wallet >= cost}, not {@code >}: a player with
     * precisely the price may spend it and end at zero, which is what every shop in every game
     * means by affordable. The boundary is pinned by a row, because an off-by-one here refuses the
     * purchase a player has saved exactly enough for.
     *
     * @param xpPoints     the price, from {@code AnvilCost} via {@code AnvilVerdict.Ready}
     * @param walletPoints the player's whole bank, from {@code XpCurve.totalPoints}
     */
    static Affordability of(int xpPoints, int walletPoints) {
        return walletPoints >= xpPoints
                ? AFFORDABLE
                : new CannotAfford(xpPoints, walletPoints);
    }
}
