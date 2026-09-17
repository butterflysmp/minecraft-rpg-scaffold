package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings screen's geometry: nine choosers, two buttons, filler everywhere else.
 *
 * <p>{@code SettingsMenu} itself needs a live {@code Player} and {@code Bukkit.createInventory}, so
 * it is boot-gate-only -- {@code GATE-nexus.md}'s slice 4b rows. The layout is pure and is tested
 * here, the same trade {@code NexusMenuLayout} and {@code RecipeBrowserLayout} make.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body, not
 * predicted here.
 */
class SettingsMenuLayoutTest {

    @Test
    void theButtonSlotsArePINNEDToTheirLITERALS_notNamedSymbolically() {
        // THE LITERALS AS LITERALS, for the reason NexusMenuLayoutTest carries three scars from: a
        // constant that is only ever named symbolically has no guard at all, because a mutation
        // moves the code and the expectation together.
        assertEquals(48, SettingsMenuLayout.BACK_SLOT,
                "Back is 48, immediately left of Close -- Ben's ruling. It was 45 (the first slot "
                        + "of the chrome row) until he moved it onto the cell the recipe browser "
                        + "already used");
        assertEquals(49, SettingsMenuLayout.CLOSE_SLOT,
                "Close is 49 -- the SAME slot the crafting menu and the hub use, so the one button "
                        + "a player has learned does not move between our screens");
        assertEquals(9, SettingsMenuLayout.FIRST_SLOT_CHOOSER,
                "the choosers start at menu 9 -- row 2, the top-left of the mirrored inventory");
        assertEquals(54, SettingsMenuLayout.SIZE, "six rows");
        assertEquals(36, SettingsMenuLayout.MAIN_INVENTORY_SIZE, "hotbar and storage, no armour");
        assertEquals(9, SettingsMenuLayout.HOTBAR_SIZE, "nine, the bottom ROW of the picker");
    }

    @Test
    void theChoosersAreTHIRTYSIXContiguousCells_theInventoryMirrored() {
        // THE MAPPING IS STILL THE WHOLE DESIGN -- what changed is what it mirrors. This row used to
        // assert NINE choosers in hotbar order, on the argument that the screen was choosing a
        // HOTBAR slot and the hotbar is nine in a row. The screen now chooses an INVENTORY slot, so
        // the picker is the inventory.
        assertEquals(36, SettingsMenuLayout.SLOT_CHOOSERS.size(), "the 36 main cells");
        for (int i = 0; i < 36; i++) {
            assertEquals(9 + i, SettingsMenuLayout.SLOT_CHOOSERS.get(i),
                    "chooser list entry " + i + " is menu slot " + (9 + i));
        }
        // FOUR FULL ROWS, 9..44 -- rows 2 through 5, with row 1 and the chrome row clear.
        assertEquals(1, SettingsMenuLayout.SLOT_CHOOSERS.get(0) / 9, "the block starts in row 2");
        assertEquals(4, SettingsMenuLayout.SLOT_CHOOSERS.get(35) / 9, "and ends in row 5");
        assertTrue(SettingsMenuLayout.SLOT_CHOOSERS.get(35) < SettingsMenuLayout.BOTTOM_ROW_START,
                "the last chooser must stay clear of the chrome row");
    }

    @Test
    void chooserForIsTheMIRROR_identityForStorageAndTheHotbarOnTheBottomRow() {
        // *** THE ONLY ARITHMETIC ON THIS SCREEN, AND IT IS ALMOST THE IDENTITY, WHICH IS THE RISK.
        //
        //   menu  9..35  ->  inventory  9..35   IDENTITY -- 27 of 36
        //   menu 36..44  ->  inventory  0..8    the hotbar, on the bottom row
        //
        // A plausible off-by-nine passes every case staged in the identity half, so the hotbar half
        // and BOTH seams are asserted individually rather than by a loop alone.
        assertEquals(OptionalInt.of(9), SettingsMenuLayout.chooserFor(9), "first storage cell");
        assertEquals(OptionalInt.of(35), SettingsMenuLayout.chooserFor(35), "last storage cell");
        assertEquals(OptionalInt.of(0), SettingsMenuLayout.chooserFor(36), "THE SEAM: menu 36 is hotbar 1");
        assertEquals(OptionalInt.of(8), SettingsMenuLayout.chooserFor(44), "last hotbar cell");

        // AND IT IS A BIJECTION ONTO 0..35 -- every inventory slot reachable, none twice. Without
        // this, a mapping that skipped one cell and doubled another satisfies all four above.
        boolean[] seen = new boolean[36];
        for (int menuSlot : SettingsMenuLayout.SLOT_CHOOSERS) {
            int inv = SettingsMenuLayout.chooserFor(menuSlot).orElseThrow();
            assertFalse(seen[inv], "inventory slot " + inv + " is reachable from two choosers");
            seen[inv] = true;
        }
        for (int inv = 0; inv < 36; inv++) {
            assertTrue(seen[inv], "inventory slot " + inv + " is reachable from no chooser");
        }

        // THE BOUNDARIES, because an off-by-one here makes a filler pane writable or a chooser inert.
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(8), "one before the block");
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(45), "one after the block");
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(0));
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(SettingsMenuLayout.CLOSE_SLOT),
                "Close must never resolve to a chooser -- it would silently move the star");
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(SettingsMenuLayout.BACK_SLOT),
                "nor Back");
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(-1),
                "an OptionalInt rather than -1 is exactly so a sentinel cannot be read as slot -1");
    }

    @Test
    void theFillerEXCLUDESEveryLiveSlot_soNoneCanBePaintedOver() {
        assertFalse(SettingsMenuLayout.FILLER_SLOTS.contains(SettingsMenuLayout.CLOSE_SLOT),
                "filler must never cover Close -- the screen would be unclosable except with Esc");
        assertFalse(SettingsMenuLayout.FILLER_SLOTS.contains(SettingsMenuLayout.BACK_SLOT),
                "nor Back");
        for (int chooser : SettingsMenuLayout.SLOT_CHOOSERS) {
            assertFalse(SettingsMenuLayout.FILLER_SLOTS.contains(chooser),
                    "filler must never cover chooser at slot " + chooser);
        }

        // AND IT COVERS EVERYTHING ELSE -- without this the assertions above are equally consistent
        // with FILLER_SLOTS being empty and the whole screen rendering blank.
        assertEquals(54 - 36 - 2, SettingsMenuLayout.FILLER_SLOTS.size(),
                "every slot except the 36 choosers and the two buttons -- 16");
        for (int slot = 0; slot < SettingsMenuLayout.SIZE; slot++) {
            boolean live = slot == SettingsMenuLayout.CLOSE_SLOT
                    || slot == SettingsMenuLayout.BACK_SLOT
                    || SettingsMenuLayout.SLOT_CHOOSERS.contains(slot);
            assertEquals(!live, SettingsMenuLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " filler membership");
        }
    }

    @Test
    void theChoosersAreInTheBODYAndTheButtonsAreInTheCHROMEROW() {
        // The hub's standing layout rule, applied to the second screen that has to obey it:
        // chrome in the bottom row, features in the body. Without this, "18" and "45" are two
        // numbers someone picked and nothing objects when a third screen picks differently.
        for (int chooser : SettingsMenuLayout.SLOT_CHOOSERS) {
            assertTrue(chooser < SettingsMenuLayout.BOTTOM_ROW_START,
                    "chooser " + chooser + " is a FEATURE and must not sit in the chrome row");
        }
        assertTrue(SettingsMenuLayout.BACK_SLOT >= SettingsMenuLayout.BOTTOM_ROW_START
                        && SettingsMenuLayout.CLOSE_SLOT >= SettingsMenuLayout.BOTTOM_ROW_START,
                "both buttons are chrome and belong in the bottom row");
        assertTrue(SettingsMenuLayout.BACK_SLOT < SettingsMenuLayout.CLOSE_SLOT,
                "Back is read before Close by a left-to-right reader, which is the order a player "
                        + "meets the two intents in");
    }

    @Test
    void closeAgreesWithTheOtherScreensAndBackDoesNOTCollideWithIt() {
        // THE CROSS-SCREEN CLAIM, asserted rather than left to two literals happening to match.
        // CraftingMenuLayout records that the enchant table deliberately did NOT move with it, so
        // agreement here is a decision and not an accident.
        assertEquals(CraftingMenuLayout.CLOSE_SLOT, SettingsMenuLayout.CLOSE_SLOT,
                "Close is the one button that must not move between screens");
        assertEquals(NexusMenuLayout.CLOSE_SLOT, SettingsMenuLayout.CLOSE_SLOT,
                "and the hub agrees too -- this screen is reached from it");
        assertTrue(SettingsMenuLayout.BACK_SLOT != SettingsMenuLayout.CLOSE_SLOT,
                "Back and Close are different intents and must be different slots");

        // THE BACK BUTTONS CONVERGED, AND THAT IS A DECISION RATHER THAN TWO LITERALS MATCHING.
        // RecipeBrowserLayout.BACK_SLOT was already 48 when Ben moved this one there, so the plugin
        // now has ONE answer for where a back button lives. Asserted so the two cannot drift apart
        // silently -- the same reason Close is asserted against the other screens above.
        assertEquals(RecipeBrowserLayout.BACK_SLOT, SettingsMenuLayout.BACK_SLOT,
                "both back buttons in the plugin sit in the same cell -- the next screen that needs "
                        + "one has a single answer to copy rather than two to choose between");
    }
}
