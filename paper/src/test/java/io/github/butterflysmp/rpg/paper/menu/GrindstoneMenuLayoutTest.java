package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The grindstone's geometry, pinned as LITERALS.
 *
 * <p>Following {@code EnchantMenuLayoutTest}'s reasoning: a test that recomputes the arithmetic it
 * is checking cannot fail. The fourteen tray cells are written out, so a relayout that moves both
 * the loop and the expectation consistently still reddens here.
 *
 * <p>{@code GrindstoneMenu} itself needs a live {@code Player} and {@code Bukkit.createInventory},
 * so it is boot-gate-only -- {@code GATE-nexus.md}'s slice 6 rows.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class GrindstoneMenuLayoutTest {

    /** The tray, by hand: rows 3 and 4 of the screen, columns 2 through 8. */
    private static final List<Integer> EXPECTED_TRAY = List.of(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34);

    @Test
    void theTrayIsTheFOURTEENCellsAndNothingElse() {
        assertEquals(14, GrindstoneMenuLayout.INPUT_SLOTS.size(), "fourteen cells, Ben's ruling");
        assertEquals(EXPECTED_TRAY,
                GrindstoneMenuLayout.INPUT_SLOTS.stream().sorted().toList(),
                "the tray is rows 3 and 4, columns 2-8");
        // Mutation: widen the column loop to 0..8 -> 18 cells -> both redden.
    }

    @Test
    void theTrayKeepsAMarginAtBOTHEdges() {
        // Column 1 and column 9 stay filler on both tray rows. Without this the tray runs into the
        // screen edge and the block stops reading as a block.
        for (int row = 2; row <= 3; row++) {
            int left = row * 9;
            int right = row * 9 + 8;
            assertFalse(GrindstoneMenuLayout.INPUT_SLOTS.contains(left),
                    "slot " + left + " is column 1 and must be filler");
            assertFalse(GrindstoneMenuLayout.INPUT_SLOTS.contains(right),
                    "slot " + right + " is column 9 and must be filler");
        }
        // Mutation: column loop 0..7 or 1..8 -> one edge joins the tray -> reddens.
    }

    @Test
    void theButtonSlotsArePINNEDToTheirLITERALS() {
        assertEquals(4, GrindstoneMenuLayout.INFO_SLOT, "the hint reads top-centre");
        assertEquals(40, GrindstoneMenuLayout.CONFIRM_SLOT, "row 5 centre -- under the tray");
        assertEquals(48, GrindstoneMenuLayout.BACK_SLOT, "beside Close");
        assertEquals(49, GrindstoneMenuLayout.CLOSE_SLOT, "like every other screen");
        assertEquals(54, GrindstoneMenuLayout.SIZE);
    }

    @Test
    void backAndCloseAreTheSAMEPairEveryOtherScreenUses() {
        // 48/49, and after the second set of layout rulings there is no exception left to name.
        // Asserted against the others rather than as two literals, so they cannot drift apart.
        assertEquals(CraftingMenuLayout.BACK_SLOT, GrindstoneMenuLayout.BACK_SLOT, "crafting");
        assertEquals(EnchantMenuLayout.BACK_SLOT, GrindstoneMenuLayout.BACK_SLOT, "enchanting");
        assertEquals(SettingsMenuLayout.BACK_SLOT, GrindstoneMenuLayout.BACK_SLOT, "settings");
        assertEquals(RecipeBrowserLayout.BACK_SLOT, GrindstoneMenuLayout.BACK_SLOT, "the browser");
        assertEquals(CraftingMenuLayout.CLOSE_SLOT, GrindstoneMenuLayout.CLOSE_SLOT, "and Close");
        assertEquals(GrindstoneMenuLayout.CLOSE_SLOT - 1, GrindstoneMenuLayout.BACK_SLOT,
                "Back sits immediately left of Close");
    }

    @Test
    void theCONFIRMSlotIsNOTATrayCell_andTHATISWhatStopsATickDestroyingWeapons() {
        // *** THE ROW THAT MATTERS MOST IN THIS FILE. ***
        //
        // The arming countdown repaints CONFIRM_SLOT twice a second for the menu's whole life. If
        // that cell were ever inside the tray, the repaint would overwrite a player's weapon -- up
        // to fourteen of them over a full tray, with no output slot to recover them from and no
        // undo. Unrecoverable, unlike MUTS5-BACK's invisible button, which was merely cosmetic.
        //
        // This does not prove the TICK is well-behaved; it removes the LAYOUT from the list of ways
        // it could go wrong. The other half is that the tick writes one named slot and never calls
        // render(), which only a boot row can see -- GATE-nexus slice 6, Row 42.
        assertFalse(GrindstoneMenuLayout.INPUT_SLOTS.contains(GrindstoneMenuLayout.CONFIRM_SLOT),
                "the confirm button must NEVER be a tray cell -- the countdown repaints it");
        // Mutation: CONFIRM_SLOT 40 -> 34 (the tray's last cell) -> reddens.
    }

    @Test
    void theFILLERTouchesNOTrayCellAndNOButton() {
        // A filler pane over a BUTTON is invisible until someone clicks it. A filler pane over a
        // TRAY CELL destroys an item. The second is why the tray is subtracted as a set.
        for (int slot : GrindstoneMenuLayout.INPUT_SLOTS) {
            assertFalse(GrindstoneMenuLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " is a tray cell and must never be painted over");
        }
        for (int slot : List.of(GrindstoneMenuLayout.INFO_SLOT, GrindstoneMenuLayout.CONFIRM_SLOT,
                GrindstoneMenuLayout.BACK_SLOT, GrindstoneMenuLayout.CLOSE_SLOT)) {
            assertFalse(GrindstoneMenuLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " is a button and must not be painted over");
        }

        // AND IT COVERS EVERYTHING ELSE -- the half without which the assertions above are equally
        // satisfied by an EMPTY filler set and a screen that renders blank.
        assertEquals(GrindstoneMenuLayout.SIZE - 18, GrindstoneMenuLayout.FILLER_SLOTS.size(),
                "54 minus fourteen tray cells and four buttons is 36");
        // Mutation: drop removeAll(INPUT_SLOTS) -> the loop AND the cardinality redden.
    }

    @Test
    void everySlotIsEXACTLYOneOfTrayButtonOrFiller() {
        // The whole surface, not the cases that came to mind. A slot belonging to nothing renders
        // as air the player can put an item into and never get back; one belonging to two is a
        // paint order nobody decided.
        List<Integer> buttons = List.of(GrindstoneMenuLayout.INFO_SLOT,
                GrindstoneMenuLayout.CONFIRM_SLOT, GrindstoneMenuLayout.BACK_SLOT,
                GrindstoneMenuLayout.CLOSE_SLOT);
        List<Integer> orphans = new ArrayList<>();

        for (int slot = 0; slot < GrindstoneMenuLayout.SIZE; slot++) {
            int roles = 0;
            if (GrindstoneMenuLayout.INPUT_SLOTS.contains(slot)) roles++;
            if (GrindstoneMenuLayout.FILLER_SLOTS.contains(slot)) roles++;
            if (buttons.contains(slot)) roles++;
            if (roles != 1) orphans.add(slot);
        }

        assertEquals(List.of(), orphans, "every slot must have exactly one role");
        assertTrue(GrindstoneMenuLayout.SIZE > 0, "and the walk must not be empty");
        // Mutation: remove one slots.remove(..) from buildFiller -> that slot has two roles.
    }

    @Test
    void theTrayIsSORTEDWhenIterated_becauseShiftClickFillsItInIndexOrder() {
        // MenuRouting.firstEmptyInput iterates new TreeSet<>(menu.inputSlots()), so a shift-clicked
        // weapon lands in the first free cell IN INDEX ORDER. Set.copyOf leaves iteration order
        // unspecified, which is exactly why the router sorts rather than trusting the set -- this
        // row pins the CONTENT so the router's sort has the right fourteen to order.
        List<Integer> sorted = GrindstoneMenuLayout.INPUT_SLOTS.stream().sorted().toList();
        assertEquals(19, sorted.get(0), "the first free cell a shift-click finds is the top-left");
        assertEquals(34, sorted.get(sorted.size() - 1), "and the last is the bottom-right");
        assertEquals(EXPECTED_TRAY, sorted, "in between, the literals");
    }
}
