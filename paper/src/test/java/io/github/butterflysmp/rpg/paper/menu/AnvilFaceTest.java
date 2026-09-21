package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.AnvilVerdict;
import io.github.butterflysmp.rpg.core.anvil.TransferKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The anvil's colour: verdict to state to pane.
 *
 * <p>A {@link Material} is a plain enum that loads without a server, which is what lets this run at
 * the two-second loop instead of costing a boot.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilFaceTest {

    @Test
    void READYIsTheONLYVerdictThatShowsLIME() {
        assertEquals(AnvilFace.State.READY,
                AnvilFace.stateOf(new AnvilVerdict.Ready(310, 910)));
        assertEquals(Material.LIME_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.READY));
    }

    /**
     * *** GRAY FOR EMPTY, NOT RED, AND THE COLLAPSE IT AVOIDS IS ALREADY RECORDED. ***
     *
     * <p>Red means something is WRONG, and an empty slot is not wrong. Collapsing them would make
     * red do two jobs -- <i>"these are the wrong items"</i> and <i>"you have not started"</i> --
     * which is the collapse {@code GATE-nexus.md} Row 28 exists to record, and which
     * {@code GrindstoneButton} fixed halfway once before splitting both the colour AND the text.
     */
    @Test
    void anEMPTYSlotIsGRAYAndARefusalIsRED_becauseRedMustKeepMeaningWRONG() {
        assertEquals(AnvilFace.State.EMPTY,
                AnvilFace.stateOf(new AnvilVerdict.SlotEmpty()));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.EMPTY));

        assertEquals(AnvilFace.State.REFUSED,
                AnvilFace.stateOf(new AnvilVerdict.NotScoreable()));
        assertEquals(Material.RED_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.REFUSED));

        assertNotEquals(AnvilFace.paneFor(AnvilFace.State.EMPTY),
                AnvilFace.paneFor(AnvilFace.State.REFUSED),
                "an empty screen and a refused pair must not look the same at a glance -- the bar "
                        + "is the only thing either says without a hover");
    }

    @Test
    void allTHREERefusalsShowRED_butTheirSENTENCESAreWhatTellThemApart() {
        // THE COLOUR COLLAPSES AND THE TEXT DOES NOT, and that is the correct split rather than the
        // Row 28 defect: the bar says "something is wrong" at a glance, and the preview cell says
        // WHICH thing. Row 28's defect was two causes indistinguishable in BOTH surfaces at once.
        assertEquals(AnvilFace.State.REFUSED,
                AnvilFace.stateOf(new AnvilVerdict.NotScoreable()));
        assertEquals(AnvilFace.State.REFUSED, AnvilFace.stateOf(
                new AnvilVerdict.KeyMismatch(TransferKey.ARMOR_HEAD, TransferKey.ARMOR_FEET)));
        assertEquals(AnvilFace.State.REFUSED,
                AnvilFace.stateOf(new AnvilVerdict.DonorNotHigher(260, 260)));

        Set<String> sentences = new HashSet<>();
        sentences.add(new AnvilVerdict.NotScoreable().sentence());
        sentences.add(new AnvilVerdict.KeyMismatch(
                TransferKey.ARMOR_HEAD, TransferKey.ARMOR_FEET).sentence());
        sentences.add(new AnvilVerdict.DonorNotHigher(260, 260).sentence());
        assertEquals(3, sentences.size(),
                "three red faces, three different explanations: " + sentences);
    }

    /**
     * *** THERE IS NO YELLOW, AND THAT IS THE PALETTE RULE WORKING RATHER THAN A GAP. ***
     *
     * <p>{@code GrindstoneButton}'s rule: <b>YELLOW resolves itself if you WAIT. GRAY and RED
     * resolve only if you ACT. LIME is ready.</b> This screen has no clock and no arming delay, so
     * nothing about it resolves by waiting and the one colour that means "wait" has nothing to name.
     * <b>Do not add a fourth state to fill the palette out.</b>
     */
    @Test
    void thereIsNOYellowState_becauseNothingOnThisScreenResolvesByWAITING() {
        assertEquals(3, AnvilFace.State.values().length,
                "READY, REFUSED, EMPTY. A fourth state here is a claim that something on this "
                        + "screen changes on its own, and nothing does.");

        Set<Material> panes = new HashSet<>();
        int checked = 0;
        for (AnvilFace.State state : AnvilFace.State.values()) {
            Material pane = AnvilFace.paneFor(state);
            assertNotEquals(Material.YELLOW_STAINED_GLASS_PANE, pane,
                    state + " must not be yellow -- yellow means WAIT, and there is no waiting here");
            panes.add(pane);
            checked++;
        }
        assertEquals(3, checked, "the sweep has to have actually run");
        assertEquals(3, panes.size(), "three states, three colours: " + panes);
    }

    /**
     * *** EVERY VERDICT ARM MAPS TO A STATE, WHICH IS WHAT THE SEALED INTERFACE BUYS. ***
     *
     * <p>{@code stateOf} switches with no default arm, so a sixth {@code AnvilVerdict} is a compile
     * error here rather than a face nobody drew. No test can assert a compile error, so this row
     * asserts the thing that makes it possible: that every arm alive today resolves.
     */
    @Test
    void everyVERDICTAliveTodayResolvesToAState_noArmFallsThrough() {
        AnvilVerdict[] all = {
                new AnvilVerdict.Ready(310, 910),
                new AnvilVerdict.SlotEmpty(),
                new AnvilVerdict.NotScoreable(),
                new AnvilVerdict.KeyMismatch(TransferKey.MELEE, TransferKey.MAGE),
                new AnvilVerdict.DonorNotHigher(260, 140),
        };
        int checked = 0;
        for (AnvilVerdict verdict : all) {
            AnvilFace.State state = AnvilFace.stateOf(verdict);
            assertNotEquals(null, state, verdict + " must have a face");
            checked++;
        }
        assertEquals(5, checked, "five verdict arms -- the sweep has to have actually run");
    }

    /**
     * *** THE NAME COLOUR AND THE PANE COLOUR ARE TWO LOOKUPS ON ONE STATE, NOT TWO DECISIONS. ***
     *
     * <p>{@code EnchantCost.clampPower}'s reason: if the bar derived its own colour, a gate row
     * reading both surfaces would be checking one expression against a different one --
     * {@code GATE-crafting.md}'s Q23 is the row that exists because two surfaces on one screen
     * disagreed about a colour.
     */
    @Test
    void theNAMEColourIsASecondLookupOnTheSAMEState_soTheBarAndThePreviewCannotDisagree() {
        assertEquals(NamedTextColor.GREEN, AnvilFace.nameColorFor(AnvilFace.State.READY));
        assertEquals(NamedTextColor.RED, AnvilFace.nameColorFor(AnvilFace.State.REFUSED));
        assertEquals(NamedTextColor.GRAY, AnvilFace.nameColorFor(AnvilFace.State.EMPTY));

        Set<NamedTextColor> colours = new HashSet<>();
        int checked = 0;
        for (AnvilFace.State state : AnvilFace.State.values()) {
            colours.add(AnvilFace.nameColorFor(state));
            checked++;
        }
        assertEquals(3, checked, "the sweep has to have actually run");
        assertEquals(3, colours.size(),
                "three states, three text colours -- two sharing one would make the sentence say "
                        + "less than the bar beside it: " + colours);
    }
}
