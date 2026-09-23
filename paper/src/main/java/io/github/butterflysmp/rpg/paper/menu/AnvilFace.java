package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.Affordability;
import io.github.butterflysmp.rpg.core.anvil.AnvilDecision;
import io.github.butterflysmp.rpg.core.anvil.AnvilVerdict;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

/**
 * What the anvil's bar and preview cell SHOW, as a pure function of the verdict.
 *
 * <p>Separated from {@code AnvilMenu} for the reason {@code GrindstoneButton} is: this is the
 * decidable part. A {@link Material} is a plain enum that loads without a server, so the whole
 * mapping is unit-testable and the boot gate only has to confirm it reached the screen.
 *
 * <h2>THE PALETTE IS THE GRINDSTONE'S RULE, APPLIED RATHER THAN RE-CHOSEN</h2>
 *
 * <b>YELLOW resolves itself if you WAIT. GRAY and RED resolve only if you ACT. LIME is ready.</b>
 *
 * <h2>*** YELLOW ARRIVED IN 13b, AND IT IS THE PALETTE RULE FIRING RATHER THAN A STATE ADDED ***</h2>
 *
 * <p>This javadoc read <i>"There is no YELLOW here … Do not add a fourth state to fill the palette
 * out"</i>, and the reason it gave was exact: <b>13a had no arming delay and no clock, so the one
 * colour that means "wait" had nothing to name.</b> 13b gives the screen a clock, so it does.
 *
 * <p><b>The instruction it carried still stands, and this is not a breach of it.</b> A state must
 * not be invented to use up a colour. {@code ARMING} was not: the countdown exists first, and YELLOW
 * follows from the rule because {@code GrindstoneButton.ARMING} is the only other user of that pane
 * in the whole plugin. <b>The sentence is rewritten rather than deleted, because the argument is
 * what made adding the state safe.</b>
 *
 * <pre>
 *   READY          LIME     a legal, priced transfer
 *   ARMING         YELLOW   the delay is running
 *   REFUSED        RED      these are the wrong things
 *   CANNOT_AFFORD  RED      right items, not enough XP
 *   EMPTY          GRAY     you have not put anything in yet
 * </pre>
 *
 * <b>FIVE STATES, FOUR COLOURS.</b> No row may assert that the two counts are equal.
 *
 * <h2>GRAY FOR EMPTY, NOT RED -- AND THE COLLAPSE THIS AVOIDS IS ALREADY RECORDED</h2>
 *
 * <b>Red means something is WRONG, and an empty slot is not wrong.</b> Collapsing them would make
 * red do two jobs -- <i>"these are the wrong items"</i> and <i>"you have not started"</i> -- which
 * is the collapse {@code GATE-nexus.md} Row 28 exists to record, and which
 * {@code GrindstoneButton} split into two colours AND two texts after fixing it halfway once.
 *
 * <h2>*** CANNOT_AFFORD IS RED TOO, AND THE BRIEF SAID OTHERWISE ***</h2>
 *
 * 13b's brief asked for <i>"a colour DISTINCT FROM REFUSED"</i>, on the argument that "wrong items"
 * and "not enough XP" have different fixes. <b>Ben withdrew it: <i>"ON THE MERITS I AGREE AND I WAS
 * WRONG."</i></b>
 *
 * <p>The reason is this screen's own established design. <b>Three refusal arms already share RED and
 * are told apart by their sentences</b> -- the colour says <i>something is wrong</i>, the SENTENCE
 * says which thing -- so a fourth refusal in its own colour would make the screen contradict itself
 * about what a colour means. Row 28's defect was two causes indistinguishable in BOTH surfaces at
 * once; here they differ in the surface that carries the detail.
 *
 * <p><b>Recorded because the brief is the durable artefact and it says the opposite.</b> Anyone
 * reading it and this file together would otherwise conclude one of them is a bug.
 *
 * <h2>{@link #paneFor} IS A LOOKUP, NOT A DECISION</h2>
 *
 * <b>{@link #stateOf} computes; the bar and the preview cell both read the result.</b> If the bar
 * derived its own colour, a gate row reading both surfaces would be checking one expression against
 * a different one -- {@code EnchantCost.clampPower}'s stated reason, and {@code GATE-crafting.md}'s
 * Q23 is the row that exists because two surfaces on one screen disagreed about a colour.
 */
final class AnvilFace {

    private AnvilFace() {}

    /** Which of the five faces the screen is wearing. */
    enum State { EMPTY, REFUSED, CANNOT_AFFORD, ARMING, READY }

    /**
     * The face for a whole decision -- the verdict AND whether it can be paid for.
     *
     * <p><b>It takes the decision rather than the verdict, and that is the affordability half
     * arriving.</b> A {@code Ready} verdict the player cannot pay for must not render as READY: the
     * bar would say "go" on a button that does nothing.
     *
     * <p><b>An exhaustive switch over the sealed {@code AnvilVerdict} with NO default arm</b>, so a
     * sixth outcome fails to compile here rather than rendering as whatever the default happened to
     * be. That is the whole reason {@code AnvilVerdict} is sealed.
     *
     * <p><b>{@code ARMING} is deliberately not reachable from here.</b> It is a fact about the
     * clock, not about the items, and it is applied by {@code AnvilButton.faceFor}.
     */
    static State stateOf(AnvilDecision decision) {
        if (decision.affordability() instanceof Affordability.CannotAfford) {
            return State.CANNOT_AFFORD;
        }
        return switch (decision.verdict()) {
            case AnvilVerdict.Ready ignored -> State.READY;
            case AnvilVerdict.SlotEmpty ignored -> State.EMPTY;
            case AnvilVerdict.NotScoreable ignored -> State.REFUSED;
            case AnvilVerdict.KeyMismatch ignored -> State.REFUSED;
            case AnvilVerdict.DonorNotHigher ignored -> State.REFUSED;
        };
    }

    /**
     * The CONFIRM BUTTON's icon for a state. A lookup, not a decision.
     *
     * <p>Paired with {@link #paneFor} exactly as {@code GrindstoneButton} pairs its two: the button
     * and the bar derive from one state, so they cannot disagree about which state they show.
     * <b>Deriving one from the other would be a second decision wearing a lookup's clothes.</b>
     */
    static Material dyeFor(State state) {
        return switch (state) {
            case READY -> Material.LIME_DYE;
            case ARMING -> Material.YELLOW_DYE;
            case REFUSED, CANNOT_AFFORD -> Material.RED_DYE;
            case EMPTY -> Material.GRAY_DYE;
        };
    }

    /**
     * The STATUS BAR's pane for a state. A lookup, not a decision.
     *
     * <p>Exhaustive switch with <b>no default arm</b>, the form every {@code SlotPolicy} consumer
     * uses.
     */
    static Material paneFor(State state) {
        return switch (state) {
            case READY -> Material.LIME_STAINED_GLASS_PANE;
            case ARMING -> Material.YELLOW_STAINED_GLASS_PANE;
            // TWO ARMS, ONE COLOUR, on purpose -- see the class javadoc. Both mean "something is
            // wrong"; the SENTENCE says which, and the two sentences differ.
            case REFUSED, CANNOT_AFFORD -> Material.RED_STAINED_GLASS_PANE;
            case EMPTY -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    /**
     * The colour the verdict's own sentence is written in, on the preview cell.
     *
     * <h2>EVERY ARM IS READ, WHICH IS WHY THIS IS A FULL LOOKUP AND NOT A TWO-ARM HELPER</h2>
     *
     * <b>READY's arm is not dead.</b> The refused, unaffordable and empty faces write the sentence
     * as the barrier's display NAME; the ready face writes it as a LORE LINE under the item's own
     * name. Different cell, same sentence, same colour lookup -- so the states stay one mapping
     * rather than a partial helper plus a literal somewhere else.
     *
     * <p><b>ARMING's arm is read too</b>, by the confirm button's own name while the countdown runs.
     *
     * <p><b>It matches {@link #paneFor} by construction, and that is the point</b>: the bar and the
     * preview cell are reading one state through two lookups, neither of which decides anything.
     * <b>It is not the SAME mapping, and it must not be collapsed into one:</b> {@code REFUSED} and
     * {@code CANNOT_AFFORD} share a PANE and differ here only in that they do not have to -- the
     * pane is a glance, the text is the detail.
     */
    static NamedTextColor nameColorFor(State state) {
        return switch (state) {
            case READY -> NamedTextColor.GREEN;
            case ARMING -> NamedTextColor.YELLOW;
            case REFUSED, CANNOT_AFFORD -> NamedTextColor.RED;
            case EMPTY -> NamedTextColor.GRAY;
        };
    }
}
