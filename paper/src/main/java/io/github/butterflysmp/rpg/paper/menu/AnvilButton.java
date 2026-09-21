package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.AnvilDecision;
import io.github.butterflysmp.rpg.core.anvil.AnvilVerdict;

/**
 * What the confirm button shows, as a pure function of the decision and the clock.
 *
 * <p>Separated from {@code AnvilMenu} for the reason {@code GrindstoneButton} is: this is the
 * decidable part, and {@code AnvilMenu} cannot be constructed without a server. The slice before
 * last shipped two zero-kill mutations because logic sat inside a class that needed one.
 *
 * <h2>*** THE CLOCK LAYERS OVER THE DECISION; IT CANNOT COME FROM IT ***</h2>
 *
 * {@code AnvilVerdict} is sealed and {@code AnvilFace.stateOf} is an exhaustive switch with no
 * default arm, so there is no way to add an ARMING verdict and no reason to want one: <b>arming is a
 * fact about the clock, not about the items.</b> {@link #faceFor} takes both and applies a
 * precedence, exactly as {@code GrindstoneButton.faceFor} layers {@code remainingTicks} over
 * {@code strippable}.
 *
 * <h2>THE PRECEDENCE, AND THE ORDER OF THE BRANCHES IS THE RULING</h2>
 *
 * <pre>
 *   not actionable   whatever the clock says -- a refused pair shows its refusal, not a countdown
 *   arming           the delay is running
 *   ready            go
 * </pre>
 *
 * <b>A freshly opened screen would otherwise be both "nothing in it" and "arming" at once</b>, and a
 * lock on an action with nothing to lose is not information the player needs.
 */
final class AnvilButton {

    private AnvilButton() {}

    /** Three seconds, re-armed by every input change and by every arrival at an actionable face. */
    static final int ARM_TICKS = 60;

    /** Twice a second. Fast enough that a countdown reads as one, slow enough to be cheap. */
    static final int PERIOD_TICKS = 10;

    private static final int TICKS_PER_SECOND = 20;

    /**
     * What to render: the face's state and the line the player reads.
     *
     * <p><b>NO COLOUR IS STORED HERE.</b> The button and the bar both derive theirs from
     * {@link #state()} through {@code AnvilFace}, so the two surfaces cannot disagree about which
     * state they are showing -- there is only one.
     */
    record Face(AnvilFace.State state, String text) {}

    /**
     * The button, from the decision and the clock.
     *
     * @param decision       from {@code AnvilDecision.of} -- the ONE producer, so the button and the
     *                       confirm handler are reading the same computation
     * @param remainingTicks ticks left on the arming delay; {@code <= 0} means armed
     */
    static Face faceFor(AnvilDecision decision, int remainingTicks) {
        // PRECEDENCE, and the order of these branches IS the ruling.
        if (!decision.actionable()) {
            return new Face(AnvilFace.stateOf(decision), refusalText(decision));
        }
        if (remainingTicks > 0) {
            return new Face(AnvilFace.State.ARMING, "Arming... " + secondsRemaining(remainingTicks));
        }
        AnvilVerdict.Ready ready = (AnvilVerdict.Ready) decision.verdict();
        // *** THE TEXT NAMES BOTH THE RESULT AND THE PRICE, AND THAT IS NOT DECORATION. ***
        //
        // refreshConfirm's repaint suppression keys on this string, so it must be INJECTIVE over
        // everything the player must see change. A text carrying only the price would render two
        // donors of different scores identically -- and the cell would then freeze across exactly
        // the swap that changes what the player receives.
        //
        // IT IS STILL NEVER COMPARED TO DECIDE ANYTHING. The decision object is what reconciles;
        // see AnvilDecision's warning about lastConfirmText.
        return new Face(AnvilFace.State.READY,
                "Raise to " + ready.newScore() + " -- " + ready.xpPoints() + " XP");
    }

    /**
     * What a non-actionable button says.
     *
     * <p>The verdict's own sentence for a refused pair, and the affordability's for a priced one the
     * player cannot pay. <b>Both come from {@code core}</b>, which is where every player-facing
     * sentence in this feature lives.
     */
    private static String refusalText(AnvilDecision decision) {
        if (decision.affordability() instanceof
                io.github.butterflysmp.rpg.core.anvil.Affordability.CannotAfford shortfall) {
            return shortfall.sentence();
        }
        return decision.verdict().sentence();
    }

    /**
     * Whole seconds still to wait, rounded UP.
     *
     * <p>Rounded up so the last partial second still reads as "1" rather than flashing "0" at a
     * button that is not yet armed -- a zero on a locked button is the one number that would be a
     * lie.
     */
    static int secondsRemaining(int remainingTicks) {
        if (remainingTicks <= 0) return 0;
        return (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    /**
     * May a click on this face do anything?
     *
     * <p><b>Only LIME acts.</b> A click on any other face is a silent no-op -- no chat line, no
     * sound -- and that is not a violation of the "no-op with feedback" rule, because <b>the button
     * IS the feedback and the player is clicking it.</b> It is also why neither silence is
     * spammable: twenty clicks on a non-LIME button produce twenty nothings.
     *
     * <p><b>{@code AnvilMenu} asks this about the DISPLAYED face, not a recomputed one</b>, and that
     * is a deviation from {@code GrindstoneMenu} with a reason -- see that class's confirm handler.
     */
    static boolean acts(Face face) {
        return face.state() == AnvilFace.State.READY;
    }
}
