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

    /** The tray, by hand: rows 2, 3 and 4 of the screen, columns 2 through 8. */
    private static final List<Integer> EXPECTED_TRAY = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34);

    /** The bar, by hand: the bottom row minus Back and Close. */
    private static final List<Integer> EXPECTED_BAR = List.of(45, 46, 47, 50, 51, 52, 53);

    @Test
    void theTrayIsTheTWENTYONECellsAndNothingElse() {
        assertEquals(21, GrindstoneMenuLayout.INPUT_SLOTS.size(),
                "twenty-one cells, Ben's ruling -- three rows of seven");
        assertEquals(EXPECTED_TRAY,
                GrindstoneMenuLayout.INPUT_SLOTS.stream().sorted().toList(),
                "the tray is rows 2, 3 and 4, columns 2-8");

        // THE TOP ROW DOES NOT REACH THE INFO SLOT. Row 1 holds the hint at slot 4; the tray starts
        // at row 2. A tray that grew upward by one more row would swallow it.
        assertFalse(GrindstoneMenuLayout.INPUT_SLOTS.contains(GrindstoneMenuLayout.INFO_SLOT),
                "the hint at slot 4 must never become a tray cell");
        // Mutation: widen the row loop to 0..3 -> row 1 joins the tray, INFO_SLOT included -> all
        // three redden.
    }

    @Test
    void theTrayKeepsAMarginAtBOTHEdges() {
        // Column 1 and column 9 stay filler on every tray row. Without this the tray runs into the
        // screen edge and the block stops reading as a block.
        for (int row = 1; row <= 3; row++) {
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
        assertEquals(GrindstoneMenuLayout.SIZE - 32, GrindstoneMenuLayout.FILLER_SLOTS.size(),
                "54 minus 21 tray cells, 7 bar cells and 4 chrome cells is 22");
        // Mutation: drop removeAll(INPUT_SLOTS) -> the loop AND the cardinality redden.
    }

    @Test
    void theBarIsTheBottomRowMinusBOTHChromeCells() {
        assertEquals(EXPECTED_BAR, GrindstoneMenuLayout.STATUS_SLOTS.stream().sorted().toList(),
                "45-47 and 50-53 -- the bottom row minus 48 and 49");
        assertEquals(7, GrindstoneMenuLayout.STATUS_SLOTS.size(), "SEVEN cells, both origins");

        // *** MUTS5-BACK IS IN SCOPE AGAIN, AND THIS IS WHAT MAKES 48 SAFE. ***
        // A bar across the bottom row beside Back and Close is the exact arrangement that produced
        // that defect: the bar painted over the button, and the only symptom was "Back doesn't work
        // sometimes". The subtraction is UNCONDITIONAL, so it holds on the block path too -- where
        // there is no Back button at all and the cell holds filler.
        assertFalse(GrindstoneMenuLayout.STATUS_SLOTS.contains(GrindstoneMenuLayout.BACK_SLOT),
                "the bar must NEVER paint over Back");
        assertFalse(GrindstoneMenuLayout.STATUS_SLOTS.contains(GrindstoneMenuLayout.CLOSE_SLOT),
                "nor over Close");
        // Mutation: drop slots.remove(BACK_SLOT) -> 8 cells, the literal list, and the Back
        // assertion all redden.
    }

    @Test
    void theBarNeverTouchesTheTRAY_theOTHERWriterTheTickNowHas() {
        // The countdown repaints CONFIRM_SLOT **and the seven bar cells** twice a second. The bar
        // is a SECOND writer on the tick, so it needs the same disjointness the button has -- and
        // the tray is now twenty-one cells of the player's gear rather than fourteen.
        for (int slot : GrindstoneMenuLayout.STATUS_SLOTS) {
            assertFalse(GrindstoneMenuLayout.INPUT_SLOTS.contains(slot),
                    "bar cell " + slot + " must never be a tray cell");
        }
        // Mutation: build the bar from row 4 instead of row 6 -> every assertion reddens.
    }

    @Test
    void theFOURSetsPARTITIONTheScreen_theInvariantThatShippedAHoleOnTHISScreenToo() {
        // *** THE FORM BEN ASKED FOR, AND IT CATCHES BOTH WAYS THIS SCREEN CAN GO WRONG. ***
        //
        // FILLER, the TRAY, the BAR and the CHROME are four disjoint sets whose union is the whole
        // screen. That single statement catches:
        //
        //   AN UNPAINTED CELL   a slot in none of them -- BACK_SLOT was exactly this from a world
        //                       block, subtracted from filler and painted only from the hub, so 48
        //                       was an invisible hole in the middle of the bar's row.
        //   A RENDER THAT REACHES THE TRAY   a slot in two of them -- which is the repaint that
        //                       destroys twenty-one items.
        //
        // Neither is visible to any assertion that checks one set at a time, and the first reached
        // a screenshot.
        for (int slot = 0; slot < GrindstoneMenuLayout.SIZE; slot++) {
            int roles = 0;
            if (GrindstoneMenuLayout.FILLER_SLOTS.contains(slot)) roles++;
            if (GrindstoneMenuLayout.INPUT_SLOTS.contains(slot)) roles++;
            if (GrindstoneMenuLayout.STATUS_SLOTS.contains(slot)) roles++;
            if (GrindstoneMenuLayout.CHROME_SLOTS.contains(slot)) roles++;
            assertEquals(1, roles, "slot " + slot + " must belong to EXACTLY ONE set, not " + roles);
        }
        assertEquals(GrindstoneMenuLayout.SIZE,
                GrindstoneMenuLayout.FILLER_SLOTS.size()
                        + GrindstoneMenuLayout.INPUT_SLOTS.size()
                        + GrindstoneMenuLayout.STATUS_SLOTS.size()
                        + GrindstoneMenuLayout.CHROME_SLOTS.size(),
                "22 + 21 + 7 + 4 = 54, exactly once each");
        // Mutation: drop removeAll(STATUS_SLOTS) from buildFiller -> seven slots have two roles.
    }

    @Test
    void theCHROMESetIsTheFourCellsRenderPaintsIndividually() {
        // SUPERSEDED IN PART: this row used to walk the whole surface over THREE sets and is now
        // the four-way partition below, which the status bar made necessary. What survives here is
        // the CHROME set's own contents -- the four cells render() paints one at a time, named as a
        // set so a fifth button cannot be added without appearing in it.
        assertEquals(List.of(4, 40, 48, 49),
                GrindstoneMenuLayout.CHROME_SLOTS.stream().sorted().toList(),
                "the hint, the button, Back and Close");
        assertEquals(4, GrindstoneMenuLayout.CHROME_SLOTS.size(), "four, and no more");
    }

    @Test
    void theTrayIsSORTEDWhenIterated_becauseShiftClickFillsItInIndexOrder() {
        // MenuRouting.firstEmptyInput iterates new TreeSet<>(menu.inputSlots()), so a shift-clicked
        // weapon lands in the first free cell IN INDEX ORDER. Set.copyOf leaves iteration order
        // unspecified, which is exactly why the router sorts rather than trusting the set -- this
        // row pins the CONTENT so the router's sort has the right fourteen to order.
        List<Integer> sorted = GrindstoneMenuLayout.INPUT_SLOTS.stream().sorted().toList();
        assertEquals(10, sorted.get(0), "the first free cell a shift-click finds is the top-left");
        assertEquals(34, sorted.get(sorted.size() - 1), "and the last is the bottom-right");
        assertEquals(EXPECTED_TRAY, sorted, "in between, the literals");
    }
}
