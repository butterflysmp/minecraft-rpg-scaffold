package io.github.butterflysmp.rpg.paper.menu;

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
 * <p><b>There is no YELLOW here, and that is the rule working rather than a gap.</b> This screen has
 * no arming delay and no clock -- nothing about it resolves by waiting -- so the one colour that
 * means "wait" has nothing to name. <b>Do not add a fourth state to fill the palette out.</b>
 *
 * <pre>
 *   READY     LIME    a legal, priced transfer
 *   REFUSED   RED     these are the wrong things
 *   EMPTY     GRAY    you have not put anything in yet
 * </pre>
 *
 * <h2>GRAY FOR EMPTY, NOT RED -- AND THE COLLAPSE THIS AVOIDS IS ALREADY RECORDED</h2>
 *
 * <b>Red means something is WRONG, and an empty slot is not wrong.</b> Collapsing them would make
 * red do two jobs -- <i>"these are the wrong items"</i> and <i>"you have not started"</i> -- which
 * is the collapse {@code GATE-nexus.md} Row 28 exists to record, and which
 * {@code GrindstoneButton} split into two colours AND two texts after fixing it halfway once.
 *
 * <p><b>Three of the four verdict arms share RED, and that is not the same collapse.</b> The colour
 * says <i>something is wrong</i>; the SENTENCE, which the verdict authors itself, says which thing.
 * Row 28's defect was two causes that were indistinguishable in both surfaces at once.
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

    /** Which of the three faces the screen is wearing. */
    enum State { EMPTY, REFUSED, READY }

    /**
     * The face for a verdict.
     *
     * <p><b>An exhaustive switch over the sealed {@code AnvilVerdict} with NO default arm</b>, so a
     * fifth outcome fails to compile here rather than rendering as whatever the default happened to
     * be. That is the whole reason {@code AnvilVerdict} is sealed.
     */
    static State stateOf(AnvilVerdict verdict) {
        return switch (verdict) {
            case AnvilVerdict.Ready ignored -> State.READY;
            case AnvilVerdict.SlotEmpty ignored -> State.EMPTY;
            case AnvilVerdict.NotScoreable ignored -> State.REFUSED;
            case AnvilVerdict.KeyMismatch ignored -> State.REFUSED;
            case AnvilVerdict.DonorNotHigher ignored -> State.REFUSED;
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
            case REFUSED -> Material.RED_STAINED_GLASS_PANE;
            case EMPTY -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    /**
     * The colour the verdict's own sentence is written in, on the preview cell.
     *
     * <h2>ALL THREE ARMS ARE READ, WHICH IS WHY THIS IS A THREE-STATE LOOKUP AND NOT A TWO</h2>
     *
     * <b>READY's arm is not dead.</b> The refused and empty faces write the sentence as the
     * barrier's display NAME; the ready face writes it as a LORE LINE under the item's own name.
     * Different cell, same sentence, same colour lookup -- so the three states stay one mapping
     * rather than a two-arm helper plus a literal somewhere else.
     *
     * <p><b>It matches {@link #paneFor} by construction, and that is the point</b>: the bar and the
     * preview cell are reading one state through two lookups, neither of which decides anything.
     */
    static NamedTextColor nameColorFor(State state) {
        return switch (state) {
            case READY -> NamedTextColor.GREEN;
            case REFUSED -> NamedTextColor.RED;
            case EMPTY -> NamedTextColor.GRAY;
        };
    }
}
