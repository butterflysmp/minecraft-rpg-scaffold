package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The grid's geometry, pinned as LITERALS.
 *
 * <p>Following {@code EnchantMenuLayoutTest}'s reasoning exactly: a test that recomputes the
 * arithmetic it is checking cannot fail. The nine slot numbers are written out, so a relayout that
 * changes both directions consistently -- which every round-trip assertion would sail through --
 * still reddens here.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftingMenuLayoutTest {

    /**
     * The nine cells, by hand, in the server's matrix order.
     *
     * <pre>
     * [ 10 11 12 ]
     * [ 19 20 21 ]
     * [ 28 29 30 ]
     * </pre>
     */
    private static final int[] EXPECTED = {10, 11, 12, 19, 20, 21, 28, 29, 30};

    @Test
    void theGridSitsWhereTheLayoutSaysItDoes() {
        for (int index = 0; index < EXPECTED.length; index++) {
            assertEquals(EXPECTED[index], CraftingMenuLayout.rawSlotForMatrix(index),
                    "matrix index " + index);
        }
        // Mutation: FIRST_GRID_COLUMN 1 -> 2 -> every row reddens naming the index.
    }

    @Test
    void everyCellRoundTripsThroughItsRawSlot() {
        for (int index = 0; index < CraftingMenuLayout.MATRIX_LENGTH; index++) {
            int raw = CraftingMenuLayout.rawSlotForMatrix(index);
            assertEquals(OptionalInt.of(index), CraftingMenuLayout.matrixIndexOf(raw),
                    "raw slot " + raw + " should map back to matrix index " + index);
        }
        // Mutation: transpose matrixIndexOf to `column * GRID + row` -> reddens on index 1.
        // That transpose is the one that silently mirrors every SHAPED recipe.
    }

    @Test
    void theMatrixOrderIsROWMajorBecauseTheServersIs() {
        // Server.getCraftingRecipe documents [0 1 2 / 3 4 5 / 6 7 8]. Index 1 must be the cell to
        // the RIGHT of index 0, not the one below it. A transposed layout matches every shapeless
        // recipe correctly and every shaped one wrongly, which reads as "some recipes are broken".
        assertEquals(CraftingMenuLayout.rawSlotForMatrix(0) + 1,
                CraftingMenuLayout.rawSlotForMatrix(1),
                "matrix index 1 sits immediately right of index 0");
        assertEquals(CraftingMenuLayout.rawSlotForMatrix(0) + 9,
                CraftingMenuLayout.rawSlotForMatrix(3),
                "matrix index 3 sits one row below index 0");
        // Mutation: swap row/column in rawSlotForMatrix -> both reddens.
    }

    @Test
    void chromeAndFillerAddressNoCell() {
        // The slots that must never resolve to a grid cell, named individually.
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(CraftingMenuLayout.CLOSE_SLOT));
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(CraftingMenuLayout.RESULT_SLOT),
                "the RESULT slot is not a grid cell -- if it were, the matrix would include its own output");
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(13),
                "column 4 -- between the grid and the result -- is not a cell");
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(9),
                "the column left of the grid is not a cell");
        // Slot 22 sits DIRECTLY RIGHT of grid cell 21, and this is what pins the grid to three
        // columns rather than letting it quietly extend into column 4.
        //
        // A LITERAL, not a constant, because the constant is gone: 22 used to be ARROW_SLOT and is
        // now ordinary filler. The assertion is not about the arrow and never was -- deleting it
        // with the decoration would have removed the only guard on the grid's right-hand bound.
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(22),
                "slot 22 is immediately right of the grid and must never be an ingredient cell");
        // Mutation: drop the column bound in matrixIndexOf -> slots 9 and 13 redden.
        // NOTE these moved with the grid in slice 5: the old literals were 10 and 14, and slot 10
        // is now a grid CELL. A relayout that left them alone would have asserted the opposite of
        // the truth and passed for the wrong reason.
    }

    @Test
    void everySlotInTheWholeInventoryIsEitherACellOrNot() {
        // The whole surface, not the cases that came to mind: exactly nine raw slots may resolve,
        // and they are exactly the nine written above. A widened bound that let a tenth slot
        // resolve would be a filler pane the matrix silently reads as an ingredient.
        List<Integer> resolving = new ArrayList<>();
        for (int raw = 0; raw < CraftingMenuLayout.SIZE; raw++) {
            if (CraftingMenuLayout.matrixIndexOf(raw).isPresent()) resolving.add(raw);
        }
        assertEquals(List.of(10, 11, 12, 19, 20, 21, 28, 29, 30), resolving,
                "exactly these raw slots are grid cells");
        // Mutation: widen the row bound to `row <= GRID` -> slots 37,38,39 join the list -> reddens.
    }

    @Test
    void aRawSlotInThePlayersOwnInventoryIsNeverACell() {
        // Raw slots past SIZE are the player's own inventory. The router already refuses them, but
        // this is what keeps a hand-built call from indexing the matrix with one.
        for (int raw = CraftingMenuLayout.SIZE; raw < CraftingMenuLayout.SIZE + 36; raw++) {
            assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(raw),
                    "raw slot " + raw + " is in the player's inventory");
        }
        // Mutation: drop the `rawSlot >= SIZE` guard -> reddens on 54.
    }

    @Test
    void theGridSlotSetIsTheNineCellsAndNothingElse() {
        assertEquals(CraftingMenuLayout.MATRIX_LENGTH, CraftingMenuLayout.GRID_SLOTS.size(),
                "nine cells, no duplicates");
        for (int expected : EXPECTED) {
            assertTrue(CraftingMenuLayout.GRID_SLOTS.contains(expected),
                    "GRID_SLOTS is missing cell " + expected);
        }
        assertFalse(CraftingMenuLayout.GRID_SLOTS.contains(CraftingMenuLayout.RESULT_SLOT),
                "the result slot must never be an input slot -- returnEverything would hand out the preview");
        // Mutation: add RESULT_SLOT to GRID_SLOTS -> the last assertion reddens. That is the
        // duplication path, asserted rather than argued.
    }

    // --- The Quick Craft column -------------------------------------------------------------

    /** Column 7, rows 1-3, top to bottom. Written out for the reason EXPECTED is. */
    private static final List<Integer> EXPECTED_SUGGESTIONS =
            List.of(16, 25, 34);

    @Test
    void theSuggestionColumnSitsBesideTheGridInCOLUMN7() {
        // PINNED AS LITERALS, because this is a SAFETY decision and not a look. A suggestion spends
        // materials on a single click with no confirmation, so it must stay AWAY from the player's
        // own inventory boundary -- the edge they cross most often coming up off the hotbar.
        //
        // It was row 4 and is now column 7 rows 1-3, which is FURTHER from that boundary, beside
        // the grid at eye level. The concern that put it low is better served by putting it high;
        // it was satisfied by the move, not abandoned. Moving it DOWN must be a deliberate edit to
        // this list, not a quiet constant change.
        assertEquals(EXPECTED_SUGGESTIONS, CraftingMenuLayout.SUGGESTION_SLOTS);
        for (int index = 0; index < EXPECTED_SUGGESTIONS.size(); index++) {
            assertEquals(EXPECTED_SUGGESTIONS.get(index),
                    CraftingMenuLayout.rawSlotForSuggestion(index), "suggestion " + index);
        }
        // Mutation: SUGGESTION_COLUMN 7 -> 8 -> every row reddens naming the index.
    }

    @Test
    void theSuggestionOrderIsSTABLE_BecauseIndexNMustAlwaysRenderInCellN() {
        // GRID_SLOTS is a Set.copyOf whose iteration order the JDK leaves undefined; this is a List
        // for exactly that reason. A ranking whose cells shuffled between recomputes would be
        // unclickable -- the player aims at the third icon and the fourth one crafts.
        for (int index = 0; index < CraftingMenuLayout.SUGGESTIONS; index++) {
            int raw = CraftingMenuLayout.rawSlotForSuggestion(index);
            assertEquals(OptionalInt.of(index), CraftingMenuLayout.suggestionIndexOf(raw),
                    "raw slot " + raw + " should map back to suggestion " + index);
            assertEquals(raw, CraftingMenuLayout.SUGGESTION_SLOTS.get(index),
                    "the list and the function must agree at " + index);
        }
        // Mutation: build SUGGESTION_SLOTS through a Set -> the list assertion reddens.
    }

    @Test
    void theSuggestionsNeverOverlapTheGridTheResultOrTheBrowser() {
        // The whole surface again. A suggestion cell that was also a grid cell would be read into
        // the crafting matrix as an ingredient; one that was the browser button would craft when the
        // player meant to navigate.
        for (int raw : CraftingMenuLayout.SUGGESTION_SLOTS) {
            assertFalse(CraftingMenuLayout.GRID_SLOTS.contains(raw), "slot " + raw + " is a grid cell");
            assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(raw),
                    "slot " + raw + " resolves to a matrix index");
            assertNotEquals(CraftingMenuLayout.RESULT_SLOT, raw);
            assertNotEquals(CraftingMenuLayout.CLOSE_SLOT, raw);
            assertNotEquals(CraftingMenuLayout.BROWSER_SLOT, raw);
        }
        assertEquals(3, CraftingMenuLayout.SUGGESTION_SLOTS.size(), "the walk must not be empty or short");
        // Mutation: move the column onto row 3 -> the grid overlap assertions redden.
    }

    @Test
    void theBrowserButtonIsTheONLYFunctionalCellBelowTheSuggestions() {
        // Row 5 is a deliberate buffer between a materials-spending button and the player's own
        // inventory. Exactly one cell in it does anything.
        assertEquals(26, CraftingMenuLayout.BROWSER_SLOT);
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.suggestionIndexOf(
                CraftingMenuLayout.BROWSER_SLOT), "the browser button must not craft");
        assertEquals(OptionalInt.empty(),
                CraftingMenuLayout.matrixIndexOf(CraftingMenuLayout.BROWSER_SLOT));
        // Mutation: put the browser button in row 4 -> it lands inside SUGGESTION_SLOTS ->
        // suggestionIndexOf resolves -> reddens.
    }

    @Test
    void backIsCHROMEBesideClose_andIsPinnedToItsLITERAL() {
        // THE CONSTANT IS PINNED BECAUSE A MUTATION ONCE FOUND NOTHING. MUTS5-BACK moved BACK_SLOT
        // from 17 to 48 and the ENTIRE SUITE STAYED GREEN. That mutation is now THE SHIPPED VALUE,
        // and what made it dangerous then has been removed: 48 was a live STATUS_SLOT the bar would
        // have painted straight over, and the bar has since given the cell up permanently.
        assertEquals(48, CraftingMenuLayout.BACK_SLOT, "Back is the bottom row, beside Close");

        // 48/49, LIKE EVERY OTHER SCREEN. Settings and the recipe browser were already this pair and
        // their javadoc calls it convergence rather than coincidence; this screen makes it four.
        assertEquals(CraftingMenuLayout.CLOSE_SLOT - 1, CraftingMenuLayout.BACK_SLOT,
                "Back sits immediately left of Close");
        assertEquals(CraftingMenuLayout.BACK_SLOT / 9, CraftingMenuLayout.CLOSE_SLOT / 9,
                "and in the SAME ROW -- consecutive indices can straddle a row boundary");

        // The recipe book keeps its column on its own merits. It is no longer half of a
        // "navigation column" rule -- that rule had two instances, Back was one of them, and it
        // was deleted rather than renumbered when Back left.
        assertEquals(8, CraftingMenuLayout.BROWSER_SLOT % 9,
                "the recipe book is the rightmost column, at the foot of the suggestion column");

        // *** THIS ASSERTION HAS NOW CHANGED MEANING TWICE, AND ONLY ONCE DID ITS TEXT MOVE. ***
        // At BACK_SLOT 17 it was VACUOUS -- 17 is not in the bottom row, so nothing was guarded.
        // At 48 with an unconditional subtraction it was THE GUARANTEE. It is now scoped to the
        // origin where Back is actually DRAWN, which is the only path on which it can be violated:
        // from a table there is no button at 48 for the bar to cover.
        assertFalse(CraftingMenuLayout.statusSlots(true).contains(CraftingMenuLayout.BACK_SLOT),
                "where Back IS drawn, the bar must NEVER paint over it -- the symptom of losing "
                        + "this is the quiet one, 'Back doesn't work sometimes'");

        assertEquals(OptionalInt.empty(),
                CraftingMenuLayout.matrixIndexOf(CraftingMenuLayout.BACK_SLOT),
                "Back must not craft");
        assertEquals(OptionalInt.empty(),
                CraftingMenuLayout.suggestionIndexOf(CraftingMenuLayout.BACK_SLOT),
                "nor be a suggestion cell");
        assertNotEquals(CraftingMenuLayout.RESULT_SLOT, CraftingMenuLayout.BACK_SLOT);
        assertNotEquals(CraftingMenuLayout.INDICATOR_SLOT, CraftingMenuLayout.BACK_SLOT);
        // Mutations applied and measured; kill sets RECORDED in the PR body.
    }

    @Test
    void everySlotInTheWholeInventoryIsEitherASuggestionOrNot() {
        // Exactly nine raw slots may resolve as suggestions, and they are exactly the nine above.
        // A widened bound would make a filler pane craft when clicked.
        List<Integer> resolving = new ArrayList<>();
        for (int raw = 0; raw < CraftingMenuLayout.SIZE; raw++) {
            if (CraftingMenuLayout.suggestionIndexOf(raw).isPresent()) resolving.add(raw);
        }
        assertEquals(EXPECTED_SUGGESTIONS, resolving, "exactly these raw slots are suggestion cells");

        for (int raw = CraftingMenuLayout.SIZE; raw < CraftingMenuLayout.SIZE + 36; raw++) {
            assertEquals(OptionalInt.empty(), CraftingMenuLayout.suggestionIndexOf(raw),
                    "raw slot " + raw + " is in the player's own inventory");
        }
        // Mutation: drop the `row != SUGGESTION_ROW` check -> every slot resolves -> reddens.
        // Mutation: drop the `rawSlot >= SIZE` guard -> reddens on 54.
    }

    // --- The status bar, and the two chrome cells inside it ------------------------------------

    @Test
    void theStatusBarCanNEVERPaintOverEitherChromeButton() {
        // THE guard, and the reason STATUS_SLOTS is set subtraction rather than a loop that skips.
        //
        // The bar spans the bottom row and BOTH buttons are inside it. Painting over Close leaves
        // the menu closable ONLY by Esc -- and Esc WORKS, so the symptom is "the X disappeared",
        // not anything that looks broken. Painting over Back is quieter still: "Back doesn't work
        // sometimes". Nothing else in the project would notice either.
        // CLOSE leaves the bar on BOTH paths -- it is drawn on both.
        assertFalse(CraftingMenuLayout.statusSlots(true).contains(CraftingMenuLayout.CLOSE_SLOT),
                "the status bar must never paint over the close button, from the Nexus");
        assertFalse(CraftingMenuLayout.statusSlots(false).contains(CraftingMenuLayout.CLOSE_SLOT),
                "nor from a table");

        // BACK leaves it only where Back is drawn, which is the fork Ben ruled after seeing the
        // screen. The old assertion here was "SEVEN, BOTH ORIGINS", on the argument that a readout
        // whose width depends on how you got there is not a readout -- overruled by looking, and
        // the second stated reason about this cell to die that way.
        assertFalse(CraftingMenuLayout.statusSlots(true).contains(CraftingMenuLayout.BACK_SLOT),
                "from the Nexus, where the arrow IS drawn, the bar must not cover it");
        assertTrue(CraftingMenuLayout.statusSlots(false).contains(CraftingMenuLayout.BACK_SLOT),
                "from a table, where nothing is drawn there, 48 IS a bar cell");

        assertEquals(7, CraftingMenuLayout.statusSlots(true).size(), "seven from the Nexus");
        assertEquals(8, CraftingMenuLayout.statusSlots(false).size(), "EIGHT from a table");
        // Mutations applied and measured; kill sets RECORDED in the PR body.
    }

    @Test
    void theStatusBarIsEXACTLYTheBottomRowMinusWhicheverChromeIsDrawn() {
        // The literals, so a bar that drifted onto another row reddens here rather than being
        // discovered in game. 45..53 is row 5; 48 is Back and 49 is Close.
        assertEquals(List.of(45, 46, 47, 50, 51, 52, 53),
                CraftingMenuLayout.statusSlots(true).stream().sorted().toList(),
                "from the Nexus: row 5 minus 48 and 49");
        assertEquals(List.of(45, 46, 47, 48, 50, 51, 52, 53),
                CraftingMenuLayout.statusSlots(false).stream().sorted().toList(),
                "from a table: row 5 minus 49 only");

        // And it must not overlap anything functional, on EITHER path. The grid and the suggestions
        // are three rows up, but asserting it costs nothing and a future relayout is exactly when
        // it stops holding.
        for (boolean fromNexus : new boolean[] {true, false}) {
            for (int slot : CraftingMenuLayout.statusSlots(fromNexus)) {
                assertFalse(CraftingMenuLayout.GRID_SLOTS.contains(slot),
                        "slot " + slot + " is a grid cell");
                assertEquals(OptionalInt.empty(), CraftingMenuLayout.suggestionIndexOf(slot),
                        "slot " + slot + " is a suggestion cell");
                assertNotEquals(CraftingMenuLayout.RESULT_SLOT, slot);
                assertNotEquals(CraftingMenuLayout.BROWSER_SLOT, slot);
            }
        }
        // Mutation: build the bar from row 4 -> both literal lists redden.
    }

    @Test
    void anOutOfRangeSuggestionIndexIsRefusedLoudly() {
        assertThrows(IllegalArgumentException.class,
                () -> CraftingMenuLayout.rawSlotForSuggestion(-1));
        assertThrows(IllegalArgumentException.class,
                () -> CraftingMenuLayout.rawSlotForSuggestion(CraftingMenuLayout.SUGGESTIONS));
        // Mutation: drop the bounds check -> silently returns a slot in the chrome or the grid.
    }

    @Test
    void anOutOfRangeMatrixIndexIsRefusedLoudly() {
        assertThrows(IllegalArgumentException.class, () -> CraftingMenuLayout.rawSlotForMatrix(-1));
        assertThrows(IllegalArgumentException.class,
                () -> CraftingMenuLayout.rawSlotForMatrix(CraftingMenuLayout.MATRIX_LENGTH));
        // Mutation: drop the bounds check -> silently returns a slot in the chrome -> reddens.
    }
}
