package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.paper.menu.CraftMatrixScreen.MatrixVerdict;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The gear-tag screen's WALK -- the half of it that can be wrong in an interesting way.
 *
 * <p>Reading a PDC needs a live {@code ItemStack} and a live {@code Keys}, so the tag read itself is
 * boot-witnessed (gate rows 7 and 8). What is testable, and what actually carries the safety
 * property, is whether the walk looks at every slot. A screen that checked only the first cell would
 * pass every hand-run trial where the tester put the weapon in slot 0, and would silently eat the
 * item for every player who did not.
 *
 * <p>{@code null} stands in for an ItemStack throughout: the predicate is injected, so no real item
 * is ever constructed and nothing here touches a server.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftMatrixScreenTest {

    /** A matrix of nine empty cells. Nulls are what a real crafting matrix is mostly made of. */
    private static ItemStack[] emptyMatrix() {
        return new ItemStack[CraftingMenuLayout.MATRIX_LENGTH];
    }

    /** Tags exactly the cell at {@code taggedIndex}, by identity of the array position. */
    private static Predicate<ItemStack> tagAt(ItemStack[] matrix, int taggedIndex) {
        return item -> item != null && item == matrix[taggedIndex];
    }

    @Test
    void anUntaggedMatrixIsEligibleForTheServersMatcher() {
        assertEquals(MatrixVerdict.VANILLA_ELIGIBLE,
                CraftMatrixScreen.verdict(emptyMatrix(), item -> false));
        // Mutation: return CONTAINS_GEAR unconditionally -> nothing would ever craft -> reddens.
    }

    @Test
    void gearInANYSlotHidesTheWholeMatrixFromTheServer() {
        // THE test. Every one of the nine positions, not the first and the last: this is the
        // property that a "check slot 0" bug passes in every casual trial and fails for real
        // players.
        for (int tagged = 0; tagged < CraftingMenuLayout.MATRIX_LENGTH; tagged++) {
            ItemStack[] matrix = emptyMatrix();
            // A distinct non-null sentinel in the tagged cell, so identity picks it out.
            ItemStack[] sentinels = new ItemStack[CraftingMenuLayout.MATRIX_LENGTH];
            assertNull(matrix[tagged], "fixture should start empty");

            final int index = tagged;
            Predicate<ItemStack> isGear = item -> item == sentinels[index];
            matrix[tagged] = sentinels[index];

            assertEquals(MatrixVerdict.CONTAINS_GEAR,
                    CraftMatrixScreen.verdict(matrix, isGear),
                    "gear in matrix slot " + tagged + " must hide the matrix");
        }
        // Mutation: `for (int i = 0; i < 1; i++)` -> only slot 0 checked -> reddens on slot 1.
        // Mutation: return VANILLA_ELIGIBLE always -> reddens on slot 0.
    }

    @Test
    void everySlotIsActuallyOfferedToThePredicate() {
        // Sharper than the above: the walk must OFFER all nine, not merely happen to find one. A
        // short-circuit that stopped early would still pass the previous test for slot 0.
        ItemStack[] matrix = emptyMatrix();
        List<Integer> offered = new ArrayList<>();

        CraftMatrixScreen.verdict(matrix, item -> {
            offered.add(offered.size());
            return false;
        });

        assertEquals(CraftingMenuLayout.MATRIX_LENGTH, offered.size(),
                "the screen must look at every cell before declaring a matrix clean");
        // Mutation: skip the last cell -> reddens with 8 != 9.
    }

    @Test
    void theWalkStopsAtTheFirstTaggedItem() {
        // The other direction: once the answer is known it must not keep asking. Not a performance
        // point -- it means the verdict cannot depend on how many tagged items there are.
        ItemStack[] matrix = emptyMatrix();
        int[] asked = {0};

        MatrixVerdict verdict = CraftMatrixScreen.verdict(matrix, item -> {
            asked[0]++;
            return true;
        });

        assertEquals(MatrixVerdict.CONTAINS_GEAR, verdict);
        assertEquals(1, asked[0], "the first tagged cell settles it");
        // Mutation: replace the early return with a flag set in a full loop -> reddens at 9 != 1.
    }

    @Test
    void aNullMatrixIsNotACrash() {
        // Reachable: a Crafter's container can hand back an empty or absent array, and the guard
        // runs at LOWEST on every craft on the server. Throwing here would break vanilla crafting
        // for everyone rather than refusing one craft.
        assertEquals(MatrixVerdict.VANILLA_ELIGIBLE,
                CraftMatrixScreen.verdict((ItemStack[]) null, item -> true));
        // Mutation: drop the null guard -> NullPointerException -> reddens.
    }

    @Test
    void aMatrixOfNullsNeverConsultsTheTagReadWithAnythingReal() {
        // The screen is called against arbitrary slots, and GearItems.idOf is documented as
        // null-guarded for exactly that reason. This pins that the walk passes nulls through rather
        // than filtering them out -- if it filtered, a future predicate that treated null as
        // meaningful would silently never see them.
        ItemStack[] matrix = emptyMatrix();
        int[] nulls = {0};

        CraftMatrixScreen.verdict(matrix, item -> {
            if (item == null) nulls[0]++;
            return false;
        });

        assertEquals(CraftingMenuLayout.MATRIX_LENGTH, nulls[0],
                "every empty cell is still offered to the tag read");
        // Mutation: `if (item == null) continue;` -> reddens at 0 != 9.
    }
}
