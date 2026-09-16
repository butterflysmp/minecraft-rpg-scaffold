package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.Material;

/**
 * What the strip button shows, as a pure function of the tray and the clock.
 *
 * <p>Separated from {@code GrindstoneMenu} for the reason {@code GridClickIntent} is: this is the
 * decidable part. A {@code Material} is a plain enum that loads without a server, so the whole
 * state machine is unit-testable and the boot gate only has to confirm it reached the screen.
 *
 * <h2>THE PALETTE IS A RULE, NOT THREE CHOICES</h2>
 *
 * <b>YELLOW is a state that resolves itself if you WAIT. GRAY is a state that resolves only if you
 * ACT. LIME is ready.</b> That sentence decides the colour of the next state anyone adds to this
 * screen without another dialog.
 *
 * <p><b>Two GRAY states, two different texts, and that is {@code GATE-nexus.md} Row 28's rule
 * applied within one colour.</b> An empty tray and a tray of already-stripped weapons are both
 * player-resolvable -- so both are gray -- but the remedies differ ("put something in" against
 * "these are the wrong weapons"), so the text differs. Row 28 failed because one colour served a
 * pending load and a failed one; collapsing these two would be the same defect one level down.
 *
 * <h2>PRECEDENCE: NOTHING, THEN ARMING, THEN READY</h2>
 *
 * <b>An empty tray shows gray whatever the clock says.</b> A lock on an action with nothing to lose
 * is not information the player needs, and a freshly opened menu would otherwise be both "nothing
 * to strip" and "arming" at once.
 */
final class GrindstoneButton {

    private GrindstoneButton() {}

    /** Three seconds, re-armed by every mutation of the tray. */
    static final int ARM_TICKS = 60;

    /** Twice a second. Fast enough that a countdown reads as one, slow enough to be cheap. */
    static final int PERIOD_TICKS = 10;

    private static final int TICKS_PER_SECOND = 20;

    /**
     * Which of the four faces the button is wearing.
     *
     * <p>The two {@code NOTHING_} arms are distinct <b>values</b>, not one arm with a branch inside
     * it, so a test can assert that they differ rather than inspecting strings for a substring.
     */
    enum State { NOTHING_EMPTY, NOTHING_STRIPPED, ARMING, READY }

    /** What to render: the state, its colour, and the line the player reads. */
    record Face(State state, Material material, String text) {}

    /**
     * The button, from the tray and the clock.
     *
     * @param trayEmpty      is there nothing at all in the tray?
     * @param strippable     how many items would actually CHANGE -- not the tray size.
     * @param refund         the refund for the whole tray, from the same expression that grants it.
     * @param remainingTicks ticks left on the arming delay; {@code <= 0} means armed.
     */
    static Face faceFor(boolean trayEmpty, int strippable, int refund, int remainingTicks) {
        // PRECEDENCE, and the order of these branches IS the ruling.
        if (strippable <= 0) {
            return trayEmpty
                    ? new Face(State.NOTHING_EMPTY, Material.GRAY_DYE, "Add weapons to strip")
                    : new Face(State.NOTHING_STRIPPED, Material.GRAY_DYE,
                            "These have nothing to strip");
        }
        if (remainingTicks > 0) {
            return new Face(State.ARMING, Material.YELLOW_DYE,
                    "Arming... " + secondsRemaining(remainingTicks));
        }
        // THE COUNT IS THE COUNT THE REFUND CAME FROM. A fourteen-item tray with nine enchanted
        // reads "Strip 9 weapons" -- printing fourteen beside the refund invites the player to
        // divide one by the other.
        return new Face(State.READY, Material.LIME_DYE,
                "Strip " + strippable + (strippable == 1 ? " weapon" : " weapons")
                        + " -- +" + refund + " XP");
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
     * sound -- and that does NOT violate Row 14's "no-op with feedback, not silence" ruling,
     * because <b>the button IS the feedback and the player is clicking it.</b> Row 14's case was a
     * button that looked identical whether it worked or not; this one never does.
     *
     * <p>It is also why neither silence is spammable: twenty clicks on a gray button produce twenty
     * nothings, where twenty chat lines would be twenty lines.
     */
    static boolean acts(Face face) {
        return face.state() == State.READY;
    }
}
