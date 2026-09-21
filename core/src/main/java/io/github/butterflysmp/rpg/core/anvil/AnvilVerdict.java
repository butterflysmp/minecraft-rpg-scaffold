package io.github.butterflysmp.rpg.core.anvil;

/**
 * What the anvil says about the pair in front of it: one READY arm and four named REFUSALS.
 *
 * <h2>*** REFUSALS ARE A NAMED SET, NOT A BOOLEAN ***</h2>
 *
 * Ben's ruling, and it is about enumerating the AXIS rather than the cases we happen to have today:
 *
 * <pre>
 *   SlotEmpty        a slot holds nothing
 *   NotScoreable     an item carries no gear score at all
 *   KeyMismatch      two scoreable items that may not trade with each other
 *   DonorNotHigher   a legal pair where the sacrifice has nothing to give
 * </pre>
 *
 * <p>A sealed interface, so {@code paper}'s switch is exhaustive with <b>no default arm</b> and a
 * fifth outcome is a compile error at the render site rather than a face nobody drew. Same
 * discipline as {@code RefreshVerdict}, whose scan site switches the same way.
 *
 * <h2>EACH REFUSAL CARRIES ITS OWN DATA AND AUTHORS ITS OWN SENTENCE</h2>
 *
 * {@code AbilityService.CastResult}'s rule: <i>"a refusal a player can see the end of is a
 * different experience from one that just says no"</i> -- which is why {@code OnCooldown} carries
 * its remaining ticks and {@code InsufficientResource} carries required AND available.
 * {@link DonorNotHigher} carries both scores for exactly that reason, and {@link KeyMismatch}
 * carries both keys so the sentence can name the kind the player must bring.
 *
 * <p>And its companion rule: <b>a refusal that reuses another refusal's message is a bug report
 * waiting to be filed.</b> Four causes, four sentences, and {@code AnvilTransferTest} asserts the
 * NAMED arm on every row rather than merely that something refused -- a row that accepts either of
 * two refusals cannot fail for its stated reason.
 *
 * <h2>EVERY SENTENCE IS AN INSTRUCTION, NOT A COMPLAINT</h2>
 *
 * Ben's ruling. <b>{@link SlotEmpty} especially: it is the face the screen OPENS in</b>, so it is
 * the resting state rather than an error path, and it tells a first-time player what the screen is
 * for. The other three each name the one thing the player must change.
 *
 * <p>The sentences render as LORE ON THE OUTPUT SLOT rather than as chat -- they belong to the cell
 * that would have held the result, where the player is already looking.
 */
public sealed interface AnvilVerdict {

    /**
     * The line the player reads, on the output slot.
     *
     * <p>In core rather than in the menu, for {@code VaultPageGate.refusal}'s reason: the screen
     * cannot be constructed without a running server, so a sentence written there has no unit test
     * that can read it. Here every one of the five is asserted at the two-second loop.
     */
    String sentence();

    /**
     * The transfer is legal and priced.
     *
     * @param newScore the score the target would end up carrying -- the donor's, unchanged. There
     *                 is no blending and no bonus; the point of the feature is to MOVE a roll.
     * @param xpPoints what it costs, from {@link AnvilCost}. <b>13a displays this and spends
     *                 nothing.</b> The spend, the consumption of the donor and the confirm button
     *                 are 13b.
     */
    record Ready(int newScore, int xpPoints) implements AnvilVerdict {

        @Override
        public String sentence() {
            return "Sacrifice to raise this to " + newScore + ".";
        }
    }

    /**
     * One or both slots are empty.
     *
     * <p><b>It wins over {@link NotScoreable} when one slot is empty and the other holds a tool</b>,
     * and that is a decision rather than an accident of branch order: an empty slot is the screen's
     * resting state, and the player's next action -- put something in -- is the same either way.
     */
    record SlotEmpty() implements AnvilVerdict {

        @Override
        public String sentence() {
            return "Place the item to upgrade, and one to sacrifice.";
        }
    }

    /**
     * An item that carries no gear score, so there is nothing to move in either direction.
     *
     * <p>Three causes collapse here -- a tool, something that is none of ours, and an instance its
     * own definition declares unscored -- because <b>the player's next action is identical for all
     * three.</b> That is a deliberate collapse, unlike the four arms of this interface, which are
     * kept apart precisely because their remedies differ.
     */
    record NotScoreable() implements AnvilVerdict {

        @Override
        public String sentence() {
            return "Both items must be gear that carries a score.";
        }
    }

    /**
     * Two scoreable items that may not trade with each other.
     *
     * <p>Carries BOTH keys although the sentence only names the target's: the donor's is what makes
     * this arm distinguishable from every other in a test and in a log, and a refusal that cannot
     * say what it was handed sends the reader looking through the whole screen.
     */
    record KeyMismatch(TransferKey targetKey, TransferKey donorKey) implements AnvilVerdict {

        public KeyMismatch {
            if (targetKey == null || donorKey == null) {
                throw new IllegalArgumentException(
                        "a key mismatch is between two PRESENT keys; an absent one is"
                                + " NotScoreable, and TransferKey.of never produces one");
            }
        }

        /**
         * <b>Names the TARGET's kind, in the plural.</b> The player is upgrading that item and the
         * donor is what they must change, so the sentence tells them what to go and find. See
         * {@link TransferKey#plural} for why the plural form is the one that survives all eight.
         */
        @Override
        public String sentence() {
            return "Both items must be " + targetKey.plural() + ".";
        }
    }

    /**
     * A legal pair where the sacrifice has nothing to give.
     *
     * <h2>EQUAL IS REJECTED, NOT A NO-OP</h2>
     *
     * Ben's ruling. The donor must be STRICTLY higher. Accepting an equal pair would consume a
     * second item in 13b and change nothing at all -- a transaction whose only effect is a loss,
     * and one a player would only notice afterwards.
     *
     * <p>Carries both scores so the sentence can say how far short the donor is, rather than
     * leaving the player to hover two tooltips and subtract.
     */
    record DonorNotHigher(int targetScore, int donorScore) implements AnvilVerdict {

        @Override
        public String sentence() {
            return "The sacrifice must score above " + targetScore + ". This one is " + donorScore
                    + ".";
        }
    }
}
