package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The slot picker's geometry: thirty-six mirrored choosers, two buttons, filler everywhere else.
 *
 * <p><b>MOVED FROM {@code SettingsMenuLayoutTest} WITH THE CHOOSERS THEMSELVES</b>, in slice 10.
 * Settings is a screen of buttons now; these rows are about the picker, so they live beside it.
 *
 * <p>{@code NexusSlotPickerMenu} needs a live {@code Player} and {@code Bukkit.createInventory}, so
 * it is boot-gate-only. The layout is pure and is tested here.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class NexusSlotPickerLayoutTest {

    @Test
    void theButtonSlotsArePINNEDToTheirLITERALS_notNamedSymbolically() {
        // THE LITERALS AS LITERALS: a constant only ever named symbolically has no guard at all,
        // because a mutation moves the code and the expectation together.
        assertEquals(48, NexusSlotPickerLayout.BACK_SLOT,
                "Back is 48 -- and the PREDECESSOR PUT ITS PICKER'S BACK AT 49, which is our "
                        + "CLOSE. A player who had learned one screen would have closed the other");
        assertEquals(49, NexusSlotPickerLayout.CLOSE_SLOT,
                "Close is 49, the same cell as every other screen in this plugin");
        assertEquals(9, NexusSlotPickerLayout.FIRST_SLOT_CHOOSER);
        assertEquals(54, NexusSlotPickerLayout.SIZE, "six rows");
        assertEquals(36, NexusSlotPickerLayout.MAIN_INVENTORY_SIZE, "hotbar and storage, no armour");
        assertEquals(9, NexusSlotPickerLayout.HOTBAR_SIZE);
    }

    @Test
    void theCHROMEAgreesWithSettings_whichIsTheScreenOneClickUP() {
        // *** THE PREDECESSOR'S DEFECT, ASSERTED AGAINST. *** Its picker used back 49 / close 53
        // while its own settings screen used 48/49, so CLOSE MOVED between two screens a player
        // reaches in sequence. Ours cannot: both are read from the same pair of literals here.
        assertEquals(SettingsMenuLayout.BACK_SLOT, NexusSlotPickerLayout.BACK_SLOT,
                "the picker's Back sits where the settings screen's Back sits");
        assertEquals(SettingsMenuLayout.CLOSE_SLOT, NexusSlotPickerLayout.CLOSE_SLOT,
                "and so does Close -- the one button that must never move");
        assertEquals(NexusMenuLayout.CLOSE_SLOT, NexusSlotPickerLayout.CLOSE_SLOT,
                "and the hub agrees too, two screens up");
        assertTrue(NexusSlotPickerLayout.BACK_SLOT != NexusSlotPickerLayout.CLOSE_SLOT,
                "Back and Close are different intents and must be different cells");
    }

    @Test
    void theChoosersAreTHIRTYSIXContiguousCells_theInventoryMirrored() {
        assertEquals(36, NexusSlotPickerLayout.SLOT_CHOOSERS.size(), "the 36 main cells");
        for (int i = 0; i < 36; i++) {
            assertEquals(9 + i, NexusSlotPickerLayout.SLOT_CHOOSERS.get(i),
                    "chooser list entry " + i + " is menu slot " + (9 + i));
        }
        assertEquals(1, NexusSlotPickerLayout.SLOT_CHOOSERS.get(0) / 9, "the block starts in row 2");
        assertEquals(4, NexusSlotPickerLayout.SLOT_CHOOSERS.get(35) / 9, "and ends in row 5");
        assertTrue(NexusSlotPickerLayout.SLOT_CHOOSERS.get(35) < NexusSlotPickerLayout.BOTTOM_ROW_START,
                "the last chooser must stay clear of the chrome row");
    }

    @Test
    void chooserForIsTheMIRROR_identityForStorageAndTheHotbarOnTheBottomRow() {
        // *** ALMOST THE IDENTITY, WHICH IS THE RISK. ***
        //   menu  9..35 -> inventory  9..35   IDENTITY -- 27 of 36
        //   menu 36..44 -> inventory  0..8    the hotbar, on the bottom row
        // A plausible off-by-nine passes every case staged in the identity half, so the hotbar half
        // and BOTH seams are asserted individually rather than by a loop alone.
        assertEquals(OptionalInt.of(9), NexusSlotPickerLayout.chooserFor(9), "first storage cell");
        assertEquals(OptionalInt.of(35), NexusSlotPickerLayout.chooserFor(35), "last storage cell");
        assertEquals(OptionalInt.of(0), NexusSlotPickerLayout.chooserFor(36), "THE SEAM: menu 36 is hotbar 1");
        assertEquals(OptionalInt.of(8), NexusSlotPickerLayout.chooserFor(44), "last hotbar cell");

        // A BIJECTION ONTO 0..35 -- every inventory slot reachable, none twice. Without this, a
        // mapping that skipped one cell and doubled another satisfies all four above.
        boolean[] seen = new boolean[36];
        for (int menuSlot : NexusSlotPickerLayout.SLOT_CHOOSERS) {
            int inv = NexusSlotPickerLayout.chooserFor(menuSlot).orElseThrow();
            assertFalse(seen[inv], "inventory slot " + inv + " is reachable from two choosers");
            seen[inv] = true;
        }
        for (int inv = 0; inv < 36; inv++) {
            assertTrue(seen[inv], "inventory slot " + inv + " is reachable from no chooser");
        }

        // THE BOUNDARIES: an off-by-one here makes a filler pane writable or a chooser inert.
        assertEquals(OptionalInt.empty(), NexusSlotPickerLayout.chooserFor(8), "one before the block");
        assertEquals(OptionalInt.empty(), NexusSlotPickerLayout.chooserFor(45), "one after the block");
        assertEquals(OptionalInt.empty(), NexusSlotPickerLayout.chooserFor(0));
        assertEquals(OptionalInt.empty(),
                NexusSlotPickerLayout.chooserFor(NexusSlotPickerLayout.CLOSE_SLOT),
                "Close must never resolve to a chooser -- it would silently move the star");
        assertEquals(OptionalInt.empty(),
                NexusSlotPickerLayout.chooserFor(NexusSlotPickerLayout.BACK_SLOT), "nor Back");
        assertEquals(OptionalInt.empty(), NexusSlotPickerLayout.chooserFor(-1),
                "an OptionalInt rather than -1 is exactly so a sentinel cannot read as slot -1");
    }

    @Test
    void theFillerEXCLUDESEveryLiveSlot_soNoneCanBePaintedOver() {
        assertFalse(NexusSlotPickerLayout.FILLER_SLOTS.contains(NexusSlotPickerLayout.CLOSE_SLOT),
                "filler must never cover Close -- the screen would be unclosable except with Esc");
        assertFalse(NexusSlotPickerLayout.FILLER_SLOTS.contains(NexusSlotPickerLayout.BACK_SLOT),
                "nor Back");
        for (int chooser : NexusSlotPickerLayout.SLOT_CHOOSERS) {
            assertFalse(NexusSlotPickerLayout.FILLER_SLOTS.contains(chooser),
                    "filler must never cover chooser at slot " + chooser);
        }

        // AND IT COVERS EVERYTHING ELSE -- without this the assertions above are equally consistent
        // with FILLER_SLOTS being empty and the whole screen rendering blank.
        assertEquals(54 - 36 - 2, NexusSlotPickerLayout.FILLER_SLOTS.size(),
                "every slot except the 36 choosers and the two buttons -- 16");
        for (int slot = 0; slot < NexusSlotPickerLayout.SIZE; slot++) {
            boolean live = slot == NexusSlotPickerLayout.CLOSE_SLOT
                    || slot == NexusSlotPickerLayout.BACK_SLOT
                    || NexusSlotPickerLayout.SLOT_CHOOSERS.contains(slot);
            assertEquals(!live, NexusSlotPickerLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " filler membership");
        }
    }

    @Test
    void slotNamesReadAsAnINVENTORY_notAsAGridOfNumbers() {
        // *** KEPT FROM #117 VERBATIM, AND IT IS WHY THE SCREEN READS AS THE PLAYER'S OWN BAG. ***
        // The hotbar is numbered the way their keyboard numbers it, and storage restarts rather
        // than continuing to 36 -- "slot 28" means nothing to anyone.
        assertEquals("Hotbar 1", NexusSlotPickerLayout.slotName(0));
        assertEquals("Hotbar 9", NexusSlotPickerLayout.slotName(8), "the default slot");
        assertEquals("Row 1, slot 1", NexusSlotPickerLayout.slotName(9), "THE SEAM: storage starts");
        assertEquals("Row 1, slot 9", NexusSlotPickerLayout.slotName(17));
        assertEquals("Row 2, slot 1", NexusSlotPickerLayout.slotName(18));
        assertEquals("Row 3, slot 9", NexusSlotPickerLayout.slotName(35), "the last storage cell");

        // NO NAME REPEATS across the 36 cells -- two cells with one name is a picker that cannot
        // say where the star is.
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int slot = 0; slot < 36; slot++) {
            assertTrue(names.add(NexusSlotPickerLayout.slotName(slot)),
                    "duplicate name at inventory slot " + slot);
        }
        assertEquals(36, names.size(), "and the sweep actually ran");
    }
}
