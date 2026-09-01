package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The slot-click decision, pinned across the WHOLE axis rather than the cases that happen to matter.
 *
 * <p>{@code MenuRouting} itself needs an {@code InventoryClickEvent}, a {@code Player} and a live
 * {@code Inventory}, none of which exist without a server, and the project has no MockBukkit. This
 * is the part that is decidable: {@code InventoryAction} and {@code ClickType} are plain enums that
 * load without a server, exactly as {@code VanillaHealPolicyTest} relies on for {@code RegainReason}.
 *
 * <p>The first two tests are the ones that survive a Paper upgrade. {@code InventoryAction} is
 * Bukkit's enum and grows in Minecraft drops -- the six {@code *_BUNDLE} constants on this build are
 * recent additions. A new constant lands in REFUSE by construction, and this asserts that rather
 * than hoping.
 *
 * <p>Each test names the mutation it forces red.
 */
class GridClickIntentTest {

    /** Every action that is un-cancelled or performed for a STACKING slot. Everything else refuses. */
    private static final Set<InventoryAction> STACKING_MOVERS = Set.of(
            InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE,
            InventoryAction.SWAP_WITH_CURSOR);

    // ---------------------------------------------------------------- the axis

    @Test
    void everyInventoryActionLandsSomewhereNamedForBothPolicies() {
        // The coverage claim: no constant falls off the end into null or an exception. A new Bukkit
        // constant reaches this loop the day it exists.
        for (InventoryAction action : InventoryAction.values()) {
            for (SlotPolicy policy : SlotPolicy.values()) {
                for (ClickType click : ClickType.values()) {
                    for (boolean restingEmpty : new boolean[]{true, false}) {
                        for (boolean similar : new boolean[]{true, false}) {
                            for (boolean accepted : new boolean[]{true, false}) {
                                assertNotNull(
                                        GridClickIntent.of(action, click, policy, restingEmpty,
                                                similar, accepted),
                                        action + "/" + policy + "/" + click + " decided nothing");
                            }
                        }
                    }
                }
            }
        }
        // Mutation: give `of` a path that returns null -> reddens naming the constant.
    }

    @Test
    void anActionNobodyHasHeardOfYetIsREFUSED() {
        // The whitelist property, stated as a property rather than a list. Anything outside the
        // named movers must refuse, whatever the surrounding state says.
        for (InventoryAction action : InventoryAction.values()) {
            if (STACKING_MOVERS.contains(action)) continue;
            assertEquals(GridClickIntent.REFUSE,
                    GridClickIntent.of(action, ClickType.LEFT, SlotPolicy.STACKING,
                            false, true, true),
                    action + " is not a named mover and must refuse");
        }
        // Mutation: turn INBOUND/OUTBOUND into "everything except a denylist" -> a bundle action
        // becomes permitted -> reddens. This is the ANY_BUT_SHIELD shape, caught mechanically.
    }

    // ---------------------------------------------------- EXCLUSIVE must not move

    @Test
    void EXCLUSIVEIsExactlyTodaysRuleAndTheEnchantSlotDoesNotMove() {
        // Put one whole stack in, take one whole stack out, LEFT only, empty slot only.
        assertEquals(GridClickIntent.PERMIT, exclusive(InventoryAction.PLACE_ALL, ClickType.LEFT, true, true));
        assertEquals(GridClickIntent.PERMIT, exclusive(InventoryAction.PICKUP_ALL, ClickType.LEFT, false, false));

        // Occupancy is the router's gate: vanilla MERGES onto a matching stack, so a place onto an
        // occupied EXCLUSIVE slot must refuse or the slot ends up holding two.
        assertEquals(GridClickIntent.REFUSE, exclusive(InventoryAction.PLACE_ALL, ClickType.LEFT, false, true));

        // acceptsInput said no.
        assertEquals(GridClickIntent.REFUSE, exclusive(InventoryAction.PLACE_ALL, ClickType.LEFT, true, false));

        // Half a weapon is a state nothing downstream is written for.
        assertEquals(GridClickIntent.REFUSE, exclusive(InventoryAction.PLACE_ONE, ClickType.RIGHT, true, true));
        assertEquals(GridClickIntent.REFUSE, exclusive(InventoryAction.PICKUP_HALF, ClickType.RIGHT, false, false));

        // And the swap the grid now performs stays refused here.
        assertEquals(GridClickIntent.REFUSE, exclusive(InventoryAction.SWAP_WITH_CURSOR, ClickType.LEFT, false, true));
        // Mutation: let EXCLUSIVE fall through to `stacking` -> the last three reddens.
    }

    @Test
    void EXCLUSIVERefusesEveryNonLeftClick() {
        // The LEFT-only rule, over the whole ClickType axis rather than the two buttons a reader
        // thinks of. NUMBER_KEY and SWAP_OFFHAND are intercepted upstream by type, but if that
        // interception is ever moved this is what still refuses them here.
        for (ClickType click : ClickType.values()) {
            if (click == ClickType.LEFT) continue;
            assertEquals(GridClickIntent.REFUSE,
                    exclusive(InventoryAction.PLACE_ALL, click, true, true),
                    click + " must not place into an EXCLUSIVE slot");
        }
        // Mutation: drop the `click != LEFT` guard -> reddens on SHIFT_LEFT.
    }

    // ------------------------------------------------------------- STACKING

    @Test
    void aStackingSlotPlacesIntoEmptyMergesOntoSimilarAndSwapsOnDissimilar() {
        // The three arms of the one decision, in the order a player meets them.
        assertEquals(GridClickIntent.PERMIT,
                stacking(InventoryAction.PLACE_ALL, true, false, true));
        assertEquals(GridClickIntent.MERGE_ALL,
                stacking(InventoryAction.PLACE_ALL, false, true, true));
        assertEquals(GridClickIntent.SWAP,
                stacking(InventoryAction.SWAP_WITH_CURSOR, false, false, true));

        // Merge and swap are ONE decision: if the similarity test inverts, BOTH of the last two
        // move, which is what makes this pair worth asserting together.
        // Mutation: similarity test always-true -> the SWAP row reddens.
        // Mutation: similarity test always-false -> the MERGE_ALL row reddens.
    }

    @Test
    void rightClickPlacesOneAndTakesHalf() {
        assertEquals(GridClickIntent.PERMIT,
                stacking(InventoryAction.PLACE_ONE, true, false, true));
        assertEquals(GridClickIntent.MERGE_ONE,
                stacking(InventoryAction.PLACE_ONE, false, true, true));
        assertEquals(GridClickIntent.PERMIT,
                stacking(InventoryAction.PICKUP_HALF, false, false, true));
        // Mutation: collapse MERGE_ONE into MERGE_ALL -> a right-click dumps the whole cursor stack
        // onto the resting one -> reddens.
    }

    @Test
    void PLACE_SOMEIsAMergeBecauseThatIsWhatVanillaCallsAPartialTopUp() {
        // The overflow case arrives under its own action name. Treating it as anything but a merge
        // is how a 64-onto-40 place loses the 40 that could not fit.
        assertEquals(GridClickIntent.MERGE_ALL,
                stacking(InventoryAction.PLACE_SOME, false, true, true));
        // Mutation: drop PLACE_SOME from INBOUND -> reddens.
    }

    // ----------------------------------------------------- the acceptsInput gate

    @Test
    void acceptedGatesEveryInboundArmAndNoOutboundOne() {
        // THE reason `accepted` is a parameter rather than a call inside the router. Slice 1's grid
        // admits everything, so a mutation of the live acceptsInput call could not redden anything
        // -- the gate would have no witness at all. Here it has one.
        for (InventoryAction action : EnumSet.of(InventoryAction.PLACE_ALL,
                InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
                InventoryAction.SWAP_WITH_CURSOR)) {
            assertEquals(GridClickIntent.REFUSE,
                    stacking(action, false, true, false),
                    action + " must refuse when acceptsInput said no");
            assertEquals(GridClickIntent.REFUSE,
                    stacking(action, true, false, false),
                    action + " must refuse when acceptsInput said no, empty slot too");
        }

        // Taking your own item back is not an admission decision, and gating it would strand an
        // item in a slot the menu had changed its mind about.
        for (InventoryAction action : EnumSet.of(InventoryAction.PICKUP_ALL,
                InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF,
                InventoryAction.PICKUP_ONE)) {
            assertEquals(GridClickIntent.PERMIT,
                    stacking(action, false, false, false),
                    action + " must still come OUT when acceptsInput says no");
        }
        // Mutation: ignore `accepted` -> the first loop reddens.
        // Mutation: gate OUTBOUND on `accepted` too -> the second loop reddens.
    }

    @Test
    void whenConsultsAcceptanceSaysNoTheAnswerDoesNotDependOnAcceptance() {
        // The two methods are one source of truth or they are a bug waiting to happen. The router
        // uses consultsAcceptance to decide whether to CALL menu.acceptsInput at all -- which it
        // must not do speculatively, because EnchantMenu's version talks to the player when it
        // refuses. This asserts the contract that makes skipping the call safe: wherever
        // consultsAcceptance is false, `of` cannot tell the two worlds apart.
        for (InventoryAction action : InventoryAction.values()) {
            for (SlotPolicy policy : SlotPolicy.values()) {
                for (ClickType click : ClickType.values()) {
                    if (GridClickIntent.consultsAcceptance(action, click, policy)) continue;
                    for (boolean restingEmpty : new boolean[]{true, false}) {
                        for (boolean similar : new boolean[]{true, false}) {
                            assertEquals(
                                    GridClickIntent.of(action, click, policy, restingEmpty, similar, true),
                                    GridClickIntent.of(action, click, policy, restingEmpty, similar, false),
                                    action + "/" + policy + "/" + click
                                            + " says it ignores acceptance but does not");
                        }
                    }
                }
            }
        }
        // Mutation: drop SWAP_WITH_CURSOR from consultsAcceptance's STACKING arm -> reddens,
        // because `of` DOES gate the swap on acceptance. That divergence would mean the router
        // never asks acceptsInput before performing a swap.
    }

    @Test
    void EXCLUSIVEConsultsAcceptanceOnlyWhereItDoesToday() {
        // Today the router reaches placeAllowed for exactly one shape: LEFT + PLACE_ALL. Anything
        // wider and EnchantMenu starts talking to the player on gestures that are silent today.
        assertTrue(GridClickIntent.consultsAcceptance(
                InventoryAction.PLACE_ALL, ClickType.LEFT, SlotPolicy.EXCLUSIVE));
        assertFalse(GridClickIntent.consultsAcceptance(
                InventoryAction.PICKUP_ALL, ClickType.LEFT, SlotPolicy.EXCLUSIVE));
        assertFalse(GridClickIntent.consultsAcceptance(
                InventoryAction.PLACE_ONE, ClickType.RIGHT, SlotPolicy.EXCLUSIVE));
        assertFalse(GridClickIntent.consultsAcceptance(
                InventoryAction.PLACE_SOME, ClickType.LEFT, SlotPolicy.EXCLUSIVE));
        // Mutation: give EXCLUSIVE the STACKING arm -> the last two redden, and EnchantMenu would
        // start refusing out loud where it is silent today.
    }

    @Test
    void aSwapOntoAnEmptyOrSimilarSlotIsNotASwap() {
        // Belt and braces: vanilla produces SWAP_WITH_CURSOR for neither, so these arms exist to
        // keep a hand-built call from performing a two-write exchange against air.
        assertEquals(GridClickIntent.REFUSE,
                stacking(InventoryAction.SWAP_WITH_CURSOR, true, false, true));
        assertEquals(GridClickIntent.REFUSE,
                stacking(InventoryAction.SWAP_WITH_CURSOR, false, true, true));
        // Mutation: drop the restingEmpty arm -> the first reddens.
    }

    // ---------------------------------------------------------------- helpers

    private static GridClickIntent exclusive(InventoryAction action, ClickType click,
                                             boolean restingEmpty, boolean accepted) {
        return GridClickIntent.of(action, click, SlotPolicy.EXCLUSIVE, restingEmpty, false, accepted);
    }

    private static GridClickIntent stacking(InventoryAction action, boolean restingEmpty,
                                            boolean similar, boolean accepted) {
        ClickType click = action == InventoryAction.PLACE_ONE || action == InventoryAction.PICKUP_HALF
                ? ClickType.RIGHT : ClickType.LEFT;
        return GridClickIntent.of(action, click, SlotPolicy.STACKING, restingEmpty, similar, accepted);
    }
}
