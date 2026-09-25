package io.github.butterflysmp.rpg.core.equipment;

import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Gesture;
import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Input;
import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Outcome;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Outcome.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Equipment screen's armour rules, over the full grid in BOTH game modes. Each rule names the
 * vanilla code it reproduces (see {@link ArmorPlacement}), and each block names the mutation it must
 * redden.
 */
class ArmorPlacementTest {

    private static final boolean[] MODES = {false, true};   // survival, creative

    private static Outcome decide(Gesture g, ArmorSlot target, ArmorSlot itemSlot, boolean allowed,
                                  boolean bound, boolean creative, int amount, boolean occupied,
                                  boolean star) {
        return ArmorPlacement.decide(new Input(target, g, Optional.ofNullable(itemSlot), allowed, bound,
                creative, amount, occupied, star));
    }

    /** A plain, fitting, unbound, single incoming item, in the given mode. */
    private static Outcome plain(Gesture g, boolean occupied, boolean creative) {
        return decide(g, ArmorSlot.HEAD, ArmorSlot.HEAD, true, false, creative, 1, occupied, false);
    }

    // --- Rule 1: fit (LivingEntity.isEquippableInSlot) --------------------------------------------

    @Test
    void rule1_theItemMustFitTHISSlot_everySlotPairBothModes() {
        for (boolean creative : MODES) {
            for (ArmorSlot target : ArmorSlot.values()) {
                for (ArmorSlot item : ArmorSlot.values()) {
                    Outcome place = decide(Gesture.PLACE, target, item, true, false, creative, 1, false, false);
                    Outcome in = decide(Gesture.SHIFT_IN, target, item, true, false, creative, 1, false, false);
                    if (item == target) {
                        assertEquals(PLACE_ONE, place, target + "<-" + item);
                        assertEquals(SHIFT_IN_ONE, in, target + "<-" + item);
                    } else {
                        assertEquals(REFUSE_WRONG_SLOT, place, target + "<-" + item);
                        assertEquals(REFUSE_WRONG_SLOT, in, target + "<-" + item);
                    }
                }
            }
        }
        // Mutation: delete the fit check (the itemSlot != target arm) -> reddens.
    }

    @Test
    void rule1_anItemWithNoArmourSlotFitsNoArmourSlot() {
        for (boolean creative : MODES) {
            for (ArmorSlot target : ArmorSlot.values()) {
                assertEquals(REFUSE_WRONG_SLOT, decide(Gesture.PLACE, target, null, true, false, creative, 1, false, false));
                assertEquals(REFUSE_WRONG_SLOT, decide(Gesture.SWAP, target, null, true, false, creative, 1, true, false));
            }
        }
    }

    @Test
    void rule1_allowedEntitiesThatExcludeThePlayerAreRefused() {
        for (boolean creative : MODES) {
            assertEquals(REFUSE_NOT_FOR_PLAYER, decide(Gesture.PLACE, ArmorSlot.HEAD, ArmorSlot.HEAD, false, false, creative, 1, false, false));
            assertEquals(REFUSE_NOT_FOR_PLAYER, decide(Gesture.SWAP, ArmorSlot.HEAD, ArmorSlot.HEAD, false, false, creative, 1, true, false));
            assertEquals(REFUSE_NOT_FOR_PLAYER, decide(Gesture.SHIFT_IN, ArmorSlot.HEAD, ArmorSlot.HEAD, false, false, creative, 1, false, false));
        }
    }

    // --- Rule 2: binding (ArmorSlot.mayPickup: !isCreative && PREVENT_ARMOR_CHANGE) --------------

    @Test
    void rule2_aBoundItemCannotLeaveInSurvival_butCanInCreative() {
        for (Gesture leaving : new Gesture[] {Gesture.TAKE, Gesture.SHIFT_OUT, Gesture.SWAP}) {
            Outcome survival = decide(leaving, ArmorSlot.HEAD, ArmorSlot.HEAD, true, true, false, 1, true, false);
            Outcome creative = decide(leaving, ArmorSlot.HEAD, ArmorSlot.HEAD, true, true, true, 1, true, false);
            assertEquals(REFUSE_BOUND, survival, leaving + " survival");
            assertEquals(switch (leaving) { case TAKE -> TAKE; case SHIFT_OUT -> SHIFT_OUT; default -> SWAP; },
                    creative, leaving + " creative");
        }
        // Mutation: delete the binding arm (boundRefusal -> false) -> reddens.
    }

    @Test
    void rule2_bindingOnlyStopsLeaving_notArriving() {
        for (boolean creative : MODES) {
            assertEquals(PLACE_ONE, decide(Gesture.PLACE, ArmorSlot.HEAD, ArmorSlot.HEAD, true, true, creative, 1, false, false),
                    "restingBound is irrelevant to an empty slot");
        }
    }

    // --- Rule 4: cursor swap (AbstractContainerMenu.doClick pickup path) --------------------------

    @Test
    void rule4_aSingleFittingItemSwaps_aStackDoesNot_aMisfitStaysOnTheCursor() {
        for (boolean creative : MODES) {
            assertEquals(SWAP, plain(Gesture.SWAP, true, creative));
            assertEquals(REFUSE_STACK, decide(Gesture.SWAP, ArmorSlot.HEAD, ArmorSlot.HEAD, true, false, creative, 2, true, false),
                    "vanilla: carried.getCount() > slot.getMaxStackSize (1) -> no swap");
            assertEquals(REFUSE_WRONG_SLOT, decide(Gesture.SWAP, ArmorSlot.HEAD, ArmorSlot.CHEST, true, false, creative, 1, true, false));
        }
    }

    @Test
    void rule4_takeAndPlaceOnlyActOnTheStateTheyName() {
        for (boolean creative : MODES) {
            assertEquals(TAKE, plain(Gesture.TAKE, true, creative));
            assertEquals(NOTHING, plain(Gesture.TAKE, false, creative));
            assertEquals(NOTHING, plain(Gesture.PLACE, true, creative));
            assertEquals(NOTHING, plain(Gesture.SWAP, false, creative));
            assertEquals(SHIFT_OUT, plain(Gesture.SHIFT_OUT, true, creative));
            assertEquals(NOTHING, plain(Gesture.SHIFT_OUT, false, creative));
        }
    }

    // --- Rule 5: shift-in (InventoryMenu.quickMoveStack), and the one deliberate divergence -----

    @Test
    void rule5_shiftInGoesToAnEmptyMatchingSlot_andAnOccupiedOneIsREFUSED() {
        for (boolean creative : MODES) {
            assertEquals(SHIFT_IN_ONE, plain(Gesture.SHIFT_IN, false, creative));
            assertEquals(REFUSE_OCCUPIED, plain(Gesture.SHIFT_IN, true, creative),
                    "the deliberate divergence: vanilla hops main<->hotbar; we move nothing");
        }
    }

    // --- Rule 6: one from a stack (ArmorSlot.getMaxStackSize == 1) --------------------------------

    @Test
    void rule6_aStackPlacesOrShiftsOne() {
        for (boolean creative : MODES) {
            assertEquals(PLACE_ONE, decide(Gesture.PLACE, ArmorSlot.HEAD, ArmorSlot.HEAD, true, false, creative, 16, false, false));
            assertEquals(SHIFT_IN_ONE, decide(Gesture.SHIFT_IN, ArmorSlot.HEAD, ArmorSlot.HEAD, true, false, creative, 16, false, false));
        }
    }

    // --- Rule 7: the Nexus star never enters -------------------------------------------------------

    @Test
    void rule7_theStarIsRefusedOnEveryIncomingGesture() {
        for (boolean creative : MODES) {
            for (Gesture g : new Gesture[] {Gesture.PLACE, Gesture.SWAP, Gesture.SHIFT_IN}) {
                boolean occupied = g == Gesture.SWAP;
                assertEquals(REFUSE_STAR, decide(g, ArmorSlot.HEAD, ArmorSlot.HEAD, true, false, creative, 1, occupied, true), g.name());
            }
        }
    }

    @Test
    void theDecisionIsTotal_everyInputGetsAnOutcome() {
        int n = 0;
        for (Gesture g : Gesture.values())
            for (boolean occupied : MODES)
                for (boolean bound : MODES)
                    for (boolean creative : MODES)
                        for (boolean star : MODES) {
                            decide(g, ArmorSlot.CHEST, ArmorSlot.CHEST, true, bound, creative, 1, occupied, star);
                            n++;
                        }
        assertEquals(Gesture.values().length * 16, n);
    }
}
