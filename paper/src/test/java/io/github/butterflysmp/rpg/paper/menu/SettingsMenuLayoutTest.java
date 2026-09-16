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
        assertEquals(45, SettingsMenuLayout.BACK_SLOT, "Back is the first slot of the chrome row");
        assertEquals(49, SettingsMenuLayout.CLOSE_SLOT,
                "Close is 49 -- the SAME slot the crafting menu and the hub use, so the one button "
                        + "a player has learned does not move between our screens");
        assertEquals(18, SettingsMenuLayout.FIRST_SLOT_CHOOSER, "the choosers start row 3");
        assertEquals(54, SettingsMenuLayout.SIZE, "six rows");
        assertEquals(9, SettingsMenuLayout.HOTBAR_SIZE, "nine, because the hotbar is nine");
    }

    @Test
    void theNineChoosersAreACONTIGUOUSROW_inHotbarOrder() {
        // THE MAPPING IS THE WHOLE DESIGN: chooser n is hotbar slot n, left to right, so the player
        // points rather than reads. A set would lose the order and a non-contiguous run would make
        // the screen stop looking like a hotbar.
        assertEquals(9, SettingsMenuLayout.SLOT_CHOOSERS.size());
        for (int i = 0; i < 9; i++) {
            assertEquals(18 + i, SettingsMenuLayout.SLOT_CHOOSERS.get(i),
                    "chooser " + i + " must be the " + i + "th cell of the row");
        }
        // And they are all in ONE row: 18..26 is row 3 entire. 26/9 == 18/9 == 2.
        assertEquals(SettingsMenuLayout.SLOT_CHOOSERS.get(0) / 9,
                SettingsMenuLayout.SLOT_CHOOSERS.get(8) / 9,
                "all nine in a single row -- a run that straddled a boundary would read as two "
                        + "groups, which is what CraftingMenuLayout's adjacency row guards against");
    }

    @Test
    void chooserForIsTheINVERSEOfTheSlotList_andRefusesEverythingElse() {
        for (int i = 0; i < 9; i++) {
            assertEquals(OptionalInt.of(i),
                    SettingsMenuLayout.chooserFor(SettingsMenuLayout.SLOT_CHOOSERS.get(i)),
                    "round trip for chooser " + i);
        }

        // THE BOUNDARIES, both of them, because an off-by-one here makes a filler pane writable or
        // a live chooser inert.
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(17), "one before the row");
        assertEquals(OptionalInt.empty(), SettingsMenuLayout.chooserFor(27), "one after the row");
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
        assertEquals(54 - 9 - 2, SettingsMenuLayout.FILLER_SLOTS.size(),
                "every slot except the nine choosers and the two buttons");
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
    }
}
