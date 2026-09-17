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

    // THE BAR HAS NO SHARED FIXTURE, DELIBERATELY. It used to be one seven-cell list, and a single
    // constant cannot describe a set that forks by origin without implying the fork is not there.
    // Both literals are written out at their assertion instead.

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
    void theBarFORKSByOrigin_sevenFromTheHubAndEIGHTFromABlock() {
        // *** THE GEOMETRY FORKS, AND THIS FILE USED TO ASSERT THAT IT MUST NOT. ***
        //
        // It read "SEVEN cells, both origins", on the argument that a readout whose geometry
        // depends on how you got there is not a readout. Ben agreed to that BEFORE seeing it and
        // ruled the other way after: from a block there is no Back button, so 48 was a black filler
        // pane sitting in the middle of a row of colour-changing ones.
        assertEquals(List.of(45, 46, 47, 50, 51, 52, 53),
                GrindstoneMenuLayout.statusSlots(true).stream().sorted().toList(),
                "from the HUB: the bottom row minus Back and Close");
        assertEquals(List.of(45, 46, 47, 48, 50, 51, 52, 53),
                GrindstoneMenuLayout.statusSlots(false).stream().sorted().toList(),
                "from a BLOCK: 48 joins the bar, because no Back button is drawn on it");

        assertEquals(7, GrindstoneMenuLayout.statusSlots(true).size(), "seven from the hub");
        assertEquals(8, GrindstoneMenuLayout.statusSlots(false).size(), "EIGHT from a block");

        // *** MUTS5-BACK IS STILL OUT OF SCOPE, AND THE FORK IS WHY RATHER THAN IN SPITE OF IT. ***
        // That defect was a bar painting over a button that was THERE. The bar takes 48 only on the
        // path where no button is drawn, so the two are mutually exclusive BY CONSTRUCTION.
        assertFalse(GrindstoneMenuLayout.statusSlots(true).contains(GrindstoneMenuLayout.BACK_SLOT),
                "where Back IS drawn, the bar must never paint over it");
        assertFalse(GrindstoneMenuLayout.statusSlots(true).contains(GrindstoneMenuLayout.CLOSE_SLOT),
                "nor over Close");
        assertFalse(GrindstoneMenuLayout.statusSlots(false).contains(GrindstoneMenuLayout.CLOSE_SLOT),
                "and Close is never a bar cell on either path");
        // Mutation: drop the `if (fromHub)` so Back always leaves the bar -> the block literal and
        // the EIGHT reddens.
    }

    @Test
    void theBarNeverTouchesTheTRAY_onEITHEROrigin() {
        // The countdown repaints CONFIRM_SLOT **and the bar** twice a second. The bar is a SECOND
        // writer on the tick, so it needs the same disjointness the button has -- and it is now
        // eight cells on one path, so BOTH are checked rather than the one that used to exist.
        for (boolean fromHub : new boolean[] {true, false}) {
            for (int slot : GrindstoneMenuLayout.statusSlots(fromHub)) {
                assertFalse(GrindstoneMenuLayout.INPUT_SLOTS.contains(slot),
                        "bar cell " + slot + " (fromHub=" + fromHub + ") must never be a tray cell");
            }
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
        // AND IT IS CHECKED ON BOTH ORIGINS NOW, because the bar and the chrome fork. 48 moves
        // between them and must be in EXACTLY ONE on each path -- a slot that fell out of both on
        // the block path is precisely the hole that shipped.
        for (boolean fromHub : new boolean[] {true, false}) {
            for (int slot = 0; slot < GrindstoneMenuLayout.SIZE; slot++) {
                int roles = 0;
                if (GrindstoneMenuLayout.FILLER_SLOTS.contains(slot)) roles++;
                if (GrindstoneMenuLayout.INPUT_SLOTS.contains(slot)) roles++;
                if (GrindstoneMenuLayout.statusSlots(fromHub).contains(slot)) roles++;
                if (GrindstoneMenuLayout.chromeSlots(fromHub).contains(slot)) roles++;
                assertEquals(1, roles, "slot " + slot + " (fromHub=" + fromHub
                        + ") must belong to EXACTLY ONE set, not " + roles);
            }
            assertEquals(GrindstoneMenuLayout.SIZE,
                    GrindstoneMenuLayout.FILLER_SLOTS.size()
                            + GrindstoneMenuLayout.INPUT_SLOTS.size()
                            + GrindstoneMenuLayout.statusSlots(fromHub).size()
                            + GrindstoneMenuLayout.chromeSlots(fromHub).size(),
                    "the four sets cover the screen exactly once, fromHub=" + fromHub);
        }

        // THE FILLER DOES NOT FORK, AND THAT IS ARITHMETIC RATHER THAN A DECISION: the whole bottom
        // row is bar-or-chrome on both paths, nine cells either way.
        assertEquals(22, GrindstoneMenuLayout.FILLER_SLOTS.size(),
                "54 - 21 tray - 9 bottom row - info - confirm = 22, on both origins");
        // Mutation: subtract the individual buttons instead of the ROW in buildFiller -> 48 lands
        // in the filler AND the block bar -> two roles -> reddens on the fromHub=false pass.
    }

    @Test
    void theCHROMESetForksWithTheBar_andTheTwoAreCOMPLEMENTSInTheBottomRow() {
        assertEquals(List.of(4, 40, 48, 49),
                GrindstoneMenuLayout.chromeSlots(true).stream().sorted().toList(),
                "from the HUB: the hint, the button, Back and Close");
        assertEquals(List.of(4, 40, 49),
                GrindstoneMenuLayout.chromeSlots(false).stream().sorted().toList(),
                "from a BLOCK: no Back, so 48 is not chrome -- it is a bar cell");

        // THE COMPLEMENT PROPERTY, which is what makes the filler origin-independent: bar plus the
        // chrome cells IN THE BOTTOM ROW is the whole bottom row, nine cells, on both paths.
        for (boolean fromHub : new boolean[] {true, false}) {
            long chromeInBottomRow = GrindstoneMenuLayout.chromeSlots(fromHub).stream()
                    .filter(slot -> slot >= 45).count();
            assertEquals(9, GrindstoneMenuLayout.statusSlots(fromHub).size() + chromeInBottomRow,
                    "bar + bottom-row chrome = the whole row, fromHub=" + fromHub);
        }
        // Mutation: drop CLOSE_SLOT from chromeSlots -> the complement row reddens on both passes.
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
