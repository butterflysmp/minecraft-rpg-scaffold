package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.Material;

/**
 * What the strip button shows, as a pure function of the tray and the clock.
 *
 * <p>Separated from {@code GrindstoneMenu} for the reason {@code GridClickIntent} is: this is the
 * decidable part. A {@code Material} is a plain enum that loads without a server, so the whole
 * state machine is unit-testable and the boot gate only has to confirm it reached the screen.
 *
 * <h2>THE PALETTE IS A RULE, NOT FOUR CHOICES</h2>
 *
 * <b>YELLOW resolves itself if you WAIT. GRAY and RED resolve only if you ACT. LIME is ready.</b>
 * That sentence decides the colour of the next state anyone adds to this screen without another
 * dialog.
 *
 * <p><b>The two act-states are one rule and two causes</b>, which is why they share the axis and not
 * the colour: gray means <i>put something in</i>, red means <i>these are the wrong things</i>.
 *
 * <h2>FOUR STATES, FOUR COLOURS -- AND THE TWO "NOTHING" ARMS NOW SEPARATE BY COLOUR TOO</h2>
 *
 * <pre>
 *   READY              LIME         ready to strip
 *   ARMING             YELLOW       the delay is running
 *   NOTHING_STRIPPED   RED          the tray holds items with nothing to strip
 *   NOTHING_EMPTY      GRAY         the tray is empty
 * </pre>
 *
 * <h2>GRAY, AND IT WAS LIGHT GRAY FOR ONE PR. BEN RULED IT BACK.</h2>
 *
 * <b>Ben asked for grey twice; the second time it shipped as LIGHT grey and he asked again.</b> The
 * code did what the brief said both times -- <b>this is an overturned ruling, not a defect</b>.
 *
 * <p><b>THE ARGUMENT THAT LOST, KEPT BECAUSE THE NEXT PERSON WILL MAKE IT AGAIN.</b> It was that
 * {@code MenuIcons.EMPTY_SUGGESTION} is already {@code LIGHT_GRAY_STAINED_GLASS_PANE} and its own
 * javadoc reads <i>"Not a second filler -- a cell that is waiting to hold something"</i> -- an empty
 * tray being, on that reading, the same statement. <b>That precedent is real and it is not what was
 * asked for.</b> Anyone who finds {@code EMPTY_SUGGESTION} and reaches the same conclusion should
 * know it has already been reached and overruled by the person looking at the screen.
 *
 * <p><b>The chrome is BLACK and stays black.</b> {@code MenuIcons.FILLER} is
 * {@code BLACK_STAINED_GLASS_PANE} on every screen in this plugin, and changing it at one slot would
 * make one screen's furniture differ from every other screen's.
 *
 * <pre>
 *   gray  nothing      yellow  wait
 *   lime  go           red     wrong
 * </pre>
 *
 * <b>GRAY FOR EMPTY, NOT RED. Ben's ruling, and the reason is what keeps red meaningful:</b> red
 * means something is WRONG, and an empty tray is not wrong. It also stops red doing two jobs --
 * <i>"these are the wrong items"</i> and <i>"you have not started"</i> -- which is the collapse
 * {@code GATE-nexus.md} Row 28 exists to record.
 *
 * <p><b>These two used to be one colour with two texts, and that was Row 28's remedy applied
 * HALFWAY.</b> Row 28's fix changed the colour as well as the text. Now they differ in both.
 *
 * <h2>*** DO NOT ADD AN "INCOMPATIBLE ITEM" STATE. IT CANNOT HAPPEN. ***</h2>
 *
 * Red has <b>exactly one reachable cause</b>: gear with nothing left to strip.
 * {@code GrindstoneMenu.acceptsInput} refuses anything that is not weapon, shield, armor or tool,
 * <b>so an incompatible item never reaches the tray</b> and an arm written for it would be a branch
 * nothing can enter. That is the dead-guard shape this project has removed before -- and unlike a
 * dead guard, it would also be advertised to the player as a state the screen can be in.
 *
 * <h2>PRECEDENCE: NOTHING, THEN ARMING, THEN READY</h2>
 *
 * <b>An empty tray shows gray whatever the clock says.</b> A lock on an action with nothing to lose
 * is not information the player needs, and a freshly opened menu would otherwise be both "nothing
 * to strip" and "arming" at once.
 *
 * <h2>THE BAR AND THE BUTTON ARE ONE STATE MACHINE WITH TWO RENDERERS</h2>
 *
 * This class computes the state; <b>{@link #paneFor} and {@link #dyeFor} are lookups, not
 * decisions.</b> The bar computes nothing.
 *
 * <p><b>If the bar derived its own colour, a gate row reading both surfaces would be checking one
 * expression against a different one</b> -- {@code EnchantCost.clampPower}'s stated reason, and
 * {@code GATE-crafting.md}'s Q23 is the row that exists because two surfaces on one screen
 * disagreed about a colour.
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

    /**
     * What to render: the state and the line the player reads.
     *
     * <p><b>NO COLOUR IS STORED HERE.</b> Both surfaces derive theirs from {@link #state()} through
     * {@link #dyeFor} and {@link #paneFor}, so the button and the bar cannot disagree about which
     * state they are showing -- there is only one.
     */
    record Face(State state, String text) {

        /** The button's icon material. */
        Material material() {
            return dyeFor(state);
        }
    }

    /** The BUTTON's colour for a state. A lookup, not a decision. */
    static Material dyeFor(State state) {
        return switch (state) {
            case READY -> Material.LIME_DYE;
            case ARMING -> Material.YELLOW_DYE;
            case NOTHING_STRIPPED -> Material.RED_DYE;
            case NOTHING_EMPTY -> Material.GRAY_DYE;
        };
    }

    /**
     * The STATUS BAR's pane for a state. A lookup, not a decision.
     *
     * <p>Exhaustive switch with <b>no default arm</b>, the form every {@code SlotPolicy} consumer
     * uses: a fifth state added to {@link State} fails to compile here rather than rendering as
     * whatever the default happened to be.
     */
    static Material paneFor(State state) {
        return switch (state) {
            case READY -> Material.LIME_STAINED_GLASS_PANE;
            case ARMING -> Material.YELLOW_STAINED_GLASS_PANE;
            case NOTHING_STRIPPED -> Material.RED_STAINED_GLASS_PANE;
            case NOTHING_EMPTY -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

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
                    ? new Face(State.NOTHING_EMPTY, "Add items to strip")
                    : new Face(State.NOTHING_STRIPPED, "These have nothing to strip");
        }
        if (remainingTicks > 0) {
            return new Face(State.ARMING, "Arming... " + secondsRemaining(remainingTicks));
        }
        // THE COUNT IS THE COUNT THE REFUND CAME FROM. A fourteen-item tray with nine enchanted
        // reads "Strip 9 items" -- printing fourteen beside the refund invites the player to
        // divide one by the other.
        //
        // "ITEMS", NOT "WEAPONS", AND THE SCREEN HAD ALREADY CHOSEN. The tray admits weapons,
        // shields, armor AND tools -- acceptsInput enumerates all four and the info lore promises
        // all four -- so a button reading "Strip 9 weapons" tells a player stripping shields that
        // the screen is doing something else. One screen, two vocabularies, with the narrower one
        // on the cell carrying the number.
        //
        // Measured before changing it: this was the ONLY player-facing string on the screen using
        // "weapon" as a category. "One item at a time.", "Strips EVERY enchant from every item
        // here." and "Stripped N items for R XP." were already neutral, so the neutral noun was
        // the screen's and the button was the one place that had not got it.
        return new Face(State.READY,
                "Strip " + strippable + (strippable == 1 ? " item" : " items")
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
     * <p>It is also why neither silence is spammable: twenty clicks on a non-LIME button produce twenty
     * nothings, where twenty chat lines would be twenty lines.
     */
    static boolean acts(Face face) {
        return face.state() == State.READY;
    }
}
