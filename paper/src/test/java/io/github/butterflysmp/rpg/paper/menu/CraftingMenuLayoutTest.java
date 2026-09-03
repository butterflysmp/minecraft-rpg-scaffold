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
                "the column between the grid and the arrow is not a cell");
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

    // --- The status bar, and the close button inside it ---------------------------------------

    @Test
    void theStatusBarCanNEVERPaintOverTheCloseButton() {
        // THE guard, and the reason STATUS_SLOTS is set subtraction rather than a loop that skips.
        //
        // The bar spans the bottom row and CLOSE_SLOT is 49, inside it. Painting over the button
        // leaves the menu closable ONLY by Esc -- and Esc WORKS, so the symptom is "the X
        // disappeared", not anything that looks broken. Nothing else in the project would notice.
        assertFalse(CraftingMenuLayout.STATUS_SLOTS.contains(CraftingMenuLayout.CLOSE_SLOT),
                "the status bar must never paint over the close button");
        assertEquals(8, CraftingMenuLayout.STATUS_SLOTS.size(),
                "row 5 is nine slots; the close button is not one of them");
        // Mutation: drop the `slots.remove(CLOSE_SLOT)` -> both assertions redden.
    }

    @Test
    void theStatusBarIsEXACTLYTheBottomRowMinusTheButton() {
        // The literals, so a bar that drifted onto another row reddens here rather than being
        // discovered in game. 45..53 is row 5; 49 is the button.
        assertEquals(List.of(45, 46, 47, 48, 50, 51, 52, 53),
                CraftingMenuLayout.STATUS_SLOTS.stream().sorted().toList(),
                "the bar is row 5 minus slot 49");

        // And it must not overlap anything functional. The grid and the suggestions are three rows
        // up, but asserting it costs nothing and a future relayout is exactly when it stops holding.
        for (int slot : CraftingMenuLayout.STATUS_SLOTS) {
            assertFalse(CraftingMenuLayout.GRID_SLOTS.contains(slot), "slot " + slot + " is a grid cell");
            assertEquals(OptionalInt.empty(), CraftingMenuLayout.suggestionIndexOf(slot),
                    "slot " + slot + " is a suggestion cell");
            assertNotEquals(CraftingMenuLayout.RESULT_SLOT, slot);
            assertNotEquals(CraftingMenuLayout.BROWSER_SLOT, slot);
        }
        // Mutation: build the bar from row 4 -> the literal list reddens.
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
