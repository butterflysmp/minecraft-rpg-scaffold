package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.Affordability;
import io.github.butterflysmp.rpg.core.anvil.AnvilDecision;
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
 * The anvil's colour: decision to state to pane.
 *
 * <p>A {@link Material} is a plain enum that loads without a server, which is what lets this run at
 * the two-second loop instead of costing a boot.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilFaceTest {

    private static AnvilDecision affordable(AnvilVerdict verdict) {
        return new AnvilDecision(verdict, Affordability.AFFORDABLE);
    }

    private static AnvilDecision priced(int cost, int wallet) {
        return AnvilDecision.of(
                new io.github.butterflysmp.rpg.core.anvil.Side.Scored(
                        TransferKey.MELEE, 140, io.github.butterflysmp.rpg.core.weapon.Rarity.RARE),
                new io.github.butterflysmp.rpg.core.anvil.Side.Scored(
                        TransferKey.MELEE, 310, io.github.butterflysmp.rpg.core.weapon.Rarity.RARE),
                wallet);
    }

    @Test
    void READYIsTheONLYStateThatShowsLIME() {
        assertEquals(AnvilFace.State.READY,
                AnvilFace.stateOf(affordable(new AnvilVerdict.Ready(310, 910))));
        assertEquals(Material.LIME_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.READY));
        assertEquals(Material.LIME_DYE, AnvilFace.dyeFor(AnvilFace.State.READY));
    }

    /**
     * *** GRAY FOR EMPTY, NOT RED, AND THE COLLAPSE IT AVOIDS IS ALREADY RECORDED. ***
     *
     * <p>Red means something is WRONG, and an empty slot is not wrong. Collapsing them would make
     * red do two jobs -- <i>"these are the wrong items"</i> and <i>"you have not started"</i> --
     * which is the collapse {@code GATE-nexus.md} Row 28 exists to record.
     */
    @Test
    void anEMPTYSlotIsGRAYAndARefusalIsRED_becauseRedMustKeepMeaningWRONG() {
        assertEquals(AnvilFace.State.EMPTY,
                AnvilFace.stateOf(affordable(new AnvilVerdict.SlotEmpty())));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.EMPTY));

        assertEquals(AnvilFace.State.REFUSED,
                AnvilFace.stateOf(affordable(new AnvilVerdict.NotScoreable())));
        assertEquals(Material.RED_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.REFUSED));

        assertNotEquals(AnvilFace.paneFor(AnvilFace.State.EMPTY),
                AnvilFace.paneFor(AnvilFace.State.REFUSED),
                "an empty screen and a refused pair must not look the same at a glance");
    }

    /**
     * *** CANNOT_AFFORD IS RED TOO, AND THE BRIEF ASKED FOR A DISTINCT COLOUR. ***
     *
     * <p>Ben withdrew that: <i>"ON THE MERITS I AGREE AND I WAS WRONG."</i> Three refusal arms
     * already share RED and are told apart by their sentences, so a fourth refusal in its own colour
     * would make this screen contradict itself about what a colour means.
     *
     * <p><b>The STATE is still its own</b>, because the sentence, the icon and the button's
     * behaviour all differ -- only the pane is shared.
     */
    @Test
    void CANNOTAFFORDIsItsOwnSTATEButSharesREDWithRefused() {
        AnvilDecision broke = priced(910, 400);
        assertEquals(AnvilFace.State.CANNOT_AFFORD, AnvilFace.stateOf(broke),
                "a Ready verdict the player cannot pay for must NOT render as READY -- the bar "
                        + "would say go on a button that does nothing");

        assertEquals(Material.RED_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.CANNOT_AFFORD));
        assertEquals(AnvilFace.paneFor(AnvilFace.State.REFUSED),
                AnvilFace.paneFor(AnvilFace.State.CANNOT_AFFORD),
                "SHARED ON PURPOSE. Both mean 'something is wrong'; the sentence says which.");

        // AND THE SAME WALLET ONE POINT HIGHER IS READY, so the row is about the boundary and not
        // about the staging.
        assertEquals(AnvilFace.State.READY, AnvilFace.stateOf(priced(910, 910)),
                "exact cost is affordable");
        // Mutation MUT13B-AFFORD: always affordable -> reddens here.
    }

    /**
     * *** YELLOW ARRIVED WITH THE CLOCK, AND IT CANNOT COME FROM THE VERDICT. ***
     *
     * <p>13a's version of this row asserted there were THREE states and that no state was yellow,
     * on the argument that the screen had no clock so <i>"the one colour that means wait has
     * nothing to name."</i> 13b gives it a clock. <b>The instruction that argument carried -- do not
     * invent a state to use up a colour -- still stands, and ARMING does not breach it: the
     * countdown exists first.</b>
     */
    @Test
    void ARMINGIsYELLOWAndIsNOTReachableFromAnyVerdict() {
        assertEquals(Material.YELLOW_STAINED_GLASS_PANE,
                AnvilFace.paneFor(AnvilFace.State.ARMING));
        assertEquals(Material.YELLOW_DYE, AnvilFace.dyeFor(AnvilFace.State.ARMING));

        // NO DECISION PRODUCES IT. Arming is a fact about the clock, not about the items, and
        // AnvilButton.faceFor is the only thing that can return it.
        int checked = 0;
        for (AnvilVerdict verdict : allVerdicts()) {
            assertNotEquals(AnvilFace.State.ARMING, AnvilFace.stateOf(affordable(verdict)),
                    verdict + " must not resolve to ARMING -- the verdict knows nothing about the "
                            + "clock");
            checked++;
        }
        assertNotEquals(AnvilFace.State.ARMING, AnvilFace.stateOf(priced(910, 400)),
                "nor does an unaffordable one");
        assertEquals(5, checked, "the sweep has to have actually run");
    }

    /**
     * *** FIVE STATES, FOUR COLOURS. THE TWO COUNTS ARE NOT EQUAL AND NO ROW MAY ASSERT THEY ARE. ***
     *
     * <p>13a's equivalent row asserted three states and three distinct panes. That equality was true
     * then and is false now, and asserting it again would force {@code CANNOT_AFFORD} into a colour
     * Ben withdrew.
     */
    @Test
    void thereAreFIVEStatesAndFOURColours_andTheMismatchIsTheRuling() {
        assertEquals(5, AnvilFace.State.values().length,
                "EMPTY, REFUSED, CANNOT_AFFORD, ARMING, READY");

        Set<Material> panes = new HashSet<>();
        Set<Material> dyes = new HashSet<>();
        int checked = 0;
        for (AnvilFace.State state : AnvilFace.State.values()) {
            panes.add(AnvilFace.paneFor(state));
            dyes.add(AnvilFace.dyeFor(state));
            checked++;
        }
        assertEquals(5, checked, "the sweep has to have actually run");
        assertEquals(4, panes.size(),
                "FOUR, not five -- REFUSED and CANNOT_AFFORD share RED by ruling: " + panes);
        assertEquals(4, dyes.size(), "and the button matches the bar: " + dyes);
    }

    /**
     * *** THE BUTTON AND THE BAR ARE TWO LOOKUPS ON ONE STATE, NOT TWO DECISIONS. ***
     *
     * <p>{@code EnchantCost.clampPower}'s reason: if the bar derived its own colour, a gate row
     * reading both surfaces would be checking one expression against a different one.
     * {@code GATE-crafting.md}'s Q23 is the row that exists because two surfaces on one screen
     * disagreed about a colour.
     */
    @Test
    void theBUTTONAndTheBARAgreeOnEveryState_becauseNeitherDecidesAnything() {
        int checked = 0;
        for (AnvilFace.State state : AnvilFace.State.values()) {
            // The two are different MATERIALS but must always be the same COLOUR FAMILY: a pane and
            // a dye of the same name. Asserted by name rather than by a mapping table, so a new
            // state cannot pair a lime dye with a red pane.
            String pane = AnvilFace.paneFor(state).name().replace("_STAINED_GLASS_PANE", "");
            String dye = AnvilFace.dyeFor(state).name().replace("_DYE", "");
            assertEquals(pane, dye, state + ": the button and the bar must show one state");
            checked++;
        }
        assertEquals(5, checked, "the sweep has to have actually run");
    }

    @Test
    void everyStateHasAWritingColour_andTheRefusalsShareItAsTheyShareThePane() {
        assertEquals(NamedTextColor.GREEN, AnvilFace.nameColorFor(AnvilFace.State.READY));
        assertEquals(NamedTextColor.YELLOW, AnvilFace.nameColorFor(AnvilFace.State.ARMING));
        assertEquals(NamedTextColor.RED, AnvilFace.nameColorFor(AnvilFace.State.REFUSED));
        assertEquals(NamedTextColor.RED, AnvilFace.nameColorFor(AnvilFace.State.CANNOT_AFFORD));
        assertEquals(NamedTextColor.GRAY, AnvilFace.nameColorFor(AnvilFace.State.EMPTY));
    }

    @Test
    void everyVERDICTAliveTodayResolvesToAState_noArmFallsThrough() {
        int checked = 0;
        for (AnvilVerdict verdict : allVerdicts()) {
            assertNotEquals(null, AnvilFace.stateOf(affordable(verdict)), verdict + " needs a face");
            checked++;
        }
        assertEquals(5, checked, "five verdict arms -- the sweep has to have actually run");
    }

    private static AnvilVerdict[] allVerdicts() {
        return new AnvilVerdict[] {
                new AnvilVerdict.Ready(310, 910),
                new AnvilVerdict.SlotEmpty(),
                new AnvilVerdict.NotScoreable(),
                new AnvilVerdict.KeyMismatch(TransferKey.ARMOR_HEAD, TransferKey.ARMOR_FEET),
                new AnvilVerdict.DonorNotHigher(260, 260),
        };
    }
}
