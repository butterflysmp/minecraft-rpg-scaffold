package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings screen's geometry: <b>two buttons in the body, two in the chrome row.</b>
 *
 * <p><b>THE PICKER'S ROWS LEFT WITH THE PICKER</b>, in slice 10 --
 * {@code NexusSlotPickerLayoutTest} holds the thirty-six choosers, the mirror and the slot names.
 * What is left here is a screen of settings.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class SettingsMenuLayoutTest {

    @Test
    void theSlotsArePINNEDToTheirLITERALS_notNamedSymbolically() {
        assertEquals(48, SettingsMenuLayout.BACK_SLOT,
                "Back is 48, immediately left of Close -- unchanged by this slice");
        assertEquals(49, SettingsMenuLayout.CLOSE_SLOT,
                "Close is 49 -- the one button a player has learned, in the cell every screen uses");
        assertEquals(22, SettingsMenuLayout.SLOT_SETTING_SLOT, "open the slot picker");
        assertEquals(24, SettingsMenuLayout.TOGGLE_SETTING_SLOT, "the star on/off switch");
        assertEquals(31, SettingsMenuLayout.STONE_SLOT_SETTING_SLOT, "the Ability Stone's slot picker");
        assertEquals(33, SettingsMenuLayout.STONE_TOGGLE_SETTING_SLOT, "the Ability Stone on/off switch");
        assertEquals(54, SettingsMenuLayout.SIZE, "six rows");
    }

    @Test
    void theTwoSettingsAreSPACED_becauseTheyAreDifferentKINDSOfThing() {
        // NOT A BAND. NexusMenuLayout's three stations are a contiguous run because a player reads
        // them as one group of the same kind. These two are not: one OPENS A SCREEN, one FLIPS A
        // SWITCH. The gap at 23 says so without needing a label.
        assertEquals(2, SettingsMenuLayout.TOGGLE_SETTING_SLOT - SettingsMenuLayout.SLOT_SETTING_SLOT,
                "one clear cell between them");
        assertFalse(SettingsMenuLayout.SETTING_SLOTS.contains(23),
                "and 23 is filler, not a third setting nobody painted");

        // BOTH IN THE SAME ROW, so the gap reads as spacing rather than as two separate groups.
        assertEquals(SettingsMenuLayout.SLOT_SETTING_SLOT / 9,
                SettingsMenuLayout.TOGGLE_SETTING_SLOT / 9, "same row");
    }

    /** The stone's two settings sit DIRECTLY BELOW the star's, so the pair reads as one table. */
    @Test
    void theStonesSettingsSitDirectlyBelowTheStars() {
        assertEquals(SettingsMenuLayout.SLOT_SETTING_SLOT + 9, SettingsMenuLayout.STONE_SLOT_SETTING_SLOT);
        assertEquals(SettingsMenuLayout.TOGGLE_SETTING_SLOT + 9, SettingsMenuLayout.STONE_TOGGLE_SETTING_SLOT);
        assertFalse(SettingsMenuLayout.SETTING_SLOTS.contains(32), "and 32 is filler, like 23 above it");
        assertEquals(4, SettingsMenuLayout.SETTING_SLOTS.size());
    }

    @Test
    void theSettingsAreInTheBODYAndTheButtonsAreInTheCHROMEROW() {
        for (int setting : SettingsMenuLayout.SETTING_SLOTS) {
            assertTrue(setting < SettingsMenuLayout.BOTTOM_ROW_START,
                    "setting " + setting + " is a FEATURE and must not sit in the chrome row");
        }
        assertTrue(SettingsMenuLayout.BACK_SLOT >= SettingsMenuLayout.BOTTOM_ROW_START
                        && SettingsMenuLayout.CLOSE_SLOT >= SettingsMenuLayout.BOTTOM_ROW_START,
                "both buttons are chrome and belong in the bottom row");
        assertTrue(SettingsMenuLayout.BACK_SLOT < SettingsMenuLayout.CLOSE_SLOT,
                "Back is read before Close by a left-to-right reader");
    }

    @Test
    void theFillerEXCLUDESEveryLiveSlot_andCOVERSEverythingElse() {
        // *** THE PAINTED-HOLE GUARD. *** The hub shipped an invisible clickable cell at slot 33
        // because a slot was subtracted from the filler and painted by nothing. SETTING_SLOTS is
        // ONE LIST used by both the subtraction and the click handler, so that is unrepresentable
        // here rather than merely detectable.
        assertFalse(SettingsMenuLayout.FILLER_SLOTS.contains(SettingsMenuLayout.CLOSE_SLOT));
        assertFalse(SettingsMenuLayout.FILLER_SLOTS.contains(SettingsMenuLayout.BACK_SLOT));
        for (int setting : SettingsMenuLayout.SETTING_SLOTS) {
            assertFalse(SettingsMenuLayout.FILLER_SLOTS.contains(setting),
                    "filler must never cover the setting at slot " + setting);
        }

        assertEquals(54 - 4 - 2, SettingsMenuLayout.FILLER_SLOTS.size(),
                "every slot except the four settings and the two buttons -- 48 (two settings until the Ability Stone)");
        for (int slot = 0; slot < SettingsMenuLayout.SIZE; slot++) {
            boolean live = slot == SettingsMenuLayout.CLOSE_SLOT
                    || slot == SettingsMenuLayout.BACK_SLOT
                    || SettingsMenuLayout.SETTING_SLOTS.contains(slot);
            assertEquals(!live, SettingsMenuLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " filler membership");
        }
    }

    @Test
    void closeAndBackAgreeWithEverySCREENThisOneIsReachedFromOrOpens() {
        // THE CROSS-SCREEN CLAIM, asserted rather than left to literals happening to match. This
        // screen sits BETWEEN the hub and the picker, so it is the one place both seams meet.
        assertEquals(NexusMenuLayout.CLOSE_SLOT, SettingsMenuLayout.CLOSE_SLOT, "the hub, above");
        assertEquals(NexusSlotPickerLayout.CLOSE_SLOT, SettingsMenuLayout.CLOSE_SLOT,
                "the picker, below -- THE PAIR THE PREDECESSOR GOT WRONG, one click apart");
        assertEquals(RecipeBrowserLayout.BACK_SLOT, SettingsMenuLayout.BACK_SLOT,
                "and Back is the plugin's single answer, unchanged since #112");
    }
}
