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
     * [ 11 12 13 ]
     * [ 20 21 22 ]
     * [ 29 30 31 ]
     * </pre>
     */
    private static final int[] EXPECTED = {11, 12, 13, 20, 21, 22, 29, 30, 31};

    @Test
    void theGridSitsWhereTheLayoutSaysItDoes() {
        for (int index = 0; index < EXPECTED.length; index++) {
            assertEquals(EXPECTED[index], CraftingMenuLayout.rawSlotForMatrix(index),
                    "matrix index " + index);
        }
        // Mutation: FIRST_GRID_COLUMN 2 -> 1 -> every row reddens naming the index.
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
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(14),
                "the column between the grid and the result is not a cell");
        assertEquals(OptionalInt.empty(), CraftingMenuLayout.matrixIndexOf(10),
                "the column left of the grid is not a cell");
        // Mutation: drop the column bound in matrixIndexOf -> slots 10 and 14 redden.
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
        assertEquals(List.of(11, 12, 13, 20, 21, 22, 29, 30, 31), resolving,
                "exactly these raw slots are grid cells");
        // Mutation: widen the row bound to `row <= GRID` -> slots 38,39,40 join the list -> reddens.
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

    /** Row 4, left to right. Written out for the reason EXPECTED is: a test that recomputes cannot fail. */
    private static final List<Integer> EXPECTED_SUGGESTIONS =
            List.of(36, 37, 38, 39, 40, 41, 42, 43, 44);

    @Test
    void theSuggestionColumnSitsInROW4() {
        // PINNED AS LITERALS, because this is a SAFETY decision and not a look. A suggestion spends
        // materials on a single click with no confirmation; row 5 sits directly above the player's
        // own inventory, one row of travel from an ordinary misclick coming up off the hotbar.
        // Moving the column down must be a deliberate edit to this list, not a quiet constant change.
        assertEquals(EXPECTED_SUGGESTIONS, CraftingMenuLayout.SUGGESTION_SLOTS);
        for (int index = 0; index < EXPECTED_SUGGESTIONS.size(); index++) {
            assertEquals(EXPECTED_SUGGESTIONS.get(index),
                    CraftingMenuLayout.rawSlotForSuggestion(index), "suggestion " + index);
        }
        // Mutation: SUGGESTION_ROW 4 -> 5 -> every row reddens naming the index.
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
        assertEquals(9, CraftingMenuLayout.SUGGESTION_SLOTS.size(), "the walk must not be empty or short");
        // Mutation: move the column onto row 3 -> the grid overlap assertions redden.
    }

    @Test
    void theBrowserButtonIsTheONLYFunctionalCellBelowTheSuggestions() {
        // Row 5 is a deliberate buffer between a materials-spending button and the player's own
        // inventory. Exactly one cell in it does anything.
        assertEquals(49, CraftingMenuLayout.BROWSER_SLOT);
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
