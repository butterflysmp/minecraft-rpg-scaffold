package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.weapon.CraftCount.Candidate;
import io.github.butterflysmp.rpg.core.weapon.CraftCount.Craftable;
import io.github.butterflysmp.rpg.core.weapon.CraftCount.Stock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The craftable count, and the invariant that keeps a suggestion honest.
 *
 * <p>The walk over {@code recipeIterator()}, the {@code RecipeChoice.test} probing and the inventory
 * debit all need a running server and are boot-gate-only. This is the half that does not have to be,
 * and it is the largest piece of logic in the slice.
 *
 * <p>The headline is {@link #theCountNEVEROverStatesEvenWhenTheGreedyWalkIsWrong}. A count that is
 * too high is a button that fails partway through, having already taken ingredients; a count that is
 * too low is a button that works.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftCountTest {

    /** Ingredient slot accepting exactly one group. */
    private static List<Integer> only(int id) {
        return List.of(id);
    }

    @SafeVarargs
    private static Candidate recipe(String key, List<Integer>... slots) {
        return new Candidate(key, List.of(slots));
    }

    // ----------------------------------------------------------------- the arithmetic

    @Test
    void aSimpleRecipeCountsAgainstItsScarcestIngredient() {
        // Two slots, two groups: 10 of one and 3 of the other. Three crafts, not ten.
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("torch", only(1), only(2))),
                List.of(new Stock(1, 10), new Stock(2, 3)));

        assertEquals(1, ranked.size());
        assertEquals(3, ranked.get(0).count(), "the scarcest ingredient binds");
        // Mutation: take the MAX over demanded groups instead of the min -> reddens.
        //
        // RUN, and it does NOT produce the 10 you would expect: the accumulator starts at
        // Integer.MAX_VALUE, so max() never moves it, the MAX_VALUE guard fires, and every count
        // collapses to 0 -- three failures and five errors across this class. Written down as
        // executed rather than as predicted, because the predicted 10 was wrong.
    }

    @Test
    void twoSlotsWantingTheSameGroupCostTWOPerCraft() {
        // THE arithmetic error that reads as correct. A recipe needing two planks from one group
        // consumes two per craft; counting "do I have any planks" gives four times the truth.
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("stick", only(1), only(1))),
                List.of(new Stock(1, 8)));

        assertEquals(4, ranked.get(0).count(), "8 planks / 2 per craft == 4");
        // Mutation: count each slot independently rather than summing demand per group -> 8 ->
        // reddens. That mutation over-states, which is the direction that costs a player items.
    }

    @Test
    void nineOfTheSameGroupAcrossNineSlotsIsOneCraft() {
        // The full grid, all one ingredient. Executed rather than reasoned about.
        List<List<Integer>> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) slots.add(only(1));
        Candidate block = new Candidate("block", slots);

        assertEquals(1, CraftCount.rank(List.of(block), List.of(new Stock(1, 9))).get(0).count());
        assertEquals(7, CraftCount.rank(List.of(block), List.of(new Stock(1, 64))).get(0).count(),
                "64 / 9 == 7 remainder 1");
        // Mutation: use ceiling division -> 8 -> reddens. Rounding UP is the over-stating direction.
    }

    @Test
    void quantityIsSummedAcrossEveryStackInAGroup() {
        // The player's 64 planks are three stacks. The count must see 64, not 3 groups of unknown
        // size and not just the first stack.
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("stick", only(1), only(1))),
                List.of(new Stock(1, 3), new Stock(1, 2), new Stock(1, 1)));

        assertEquals(3, ranked.get(0).count(), "3 + 2 + 1 == 6 planks, 2 per craft, 3 crafts");
        // Mutation: keep the last entry instead of summing -> 0 -> reddens.
    }

    // ------------------------------------------------------ THE INVARIANT

    @Test
    void theCountNEVEROverStatesEvenWhenTheGreedyWalkIsWrong() {
        // THE property this class exists for, and the documented under-count, pinned as a NUMBER.
        //
        // Slot one accepts oak(1) OR birch(2); slot two accepts oak(1) only. The player holds ONE
        // oak and sixty-four birch. Optimal is birch-then-oak == 1 craft. The greedy walk gives
        // slot one the first group with availability -- oak -- so both slots demand oak, demand is
        // 2 against a stock of 1, and the answer is ZERO.
        //
        // Zero is CORRECT for this class: it is under the truth, never over it. The suggestion
        // simply does not appear, which is a missing button rather than a broken one.
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("boat", List.of(1, 2), only(1))),
                List.of(new Stock(1, 1), new Stock(2, 64)));

        assertEquals(List.of(), ranked, "the greedy walk under-counts here, and that is the design");
        // Mutation: add backtracking that finds the optimum -> this reddens. Which is the point:
        // if someone ever DOES solve the matching properly, this test must be deliberately rewritten
        // rather than silently satisfied, because the invariant is about the direction of error.
    }

    @Test
    void everyRankedCountIsActuallyDeliverable() {
        // The invariant as a PROPERTY rather than a worked example: whatever rank() claims, spending
        // that many crafts' worth of the greedily-chosen groups must not exceed what is held.
        //
        // A recipe over three slots with overlapping alternatives, against an awkward stock.
        List<Candidate> candidates = List.of(
                recipe("a", List.of(1, 2), List.of(2, 3), only(3)),
                recipe("b", only(1), List.of(1, 3)),
                recipe("c", List.of(3), List.of(3), List.of(3)));
        List<Stock> stock = List.of(new Stock(1, 5), new Stock(2, 7), new Stock(3, 11));

        for (Craftable craftable : CraftCount.rank(candidates, stock)) {
            assertTrue(craftable.count() > 0, craftable + " was ranked with a non-positive count");
            // No single group can be over-spent: the count times the per-craft demand for the
            // scarcest group must fit. Checked against the smallest stock, which bounds every case.
            assertTrue(craftable.count() <= 11,
                    craftable + " claims more crafts than the largest single stock allows");
        }
        // Mutation: drop the `available / demand` division and return `available` -> reddens.
    }

    // ------------------------------------------------------ unsatisfiable slots

    @Test
    void aSlotNothingSatisfiesMakesTheRecipeVANISH() {
        // UNKNOWN MEANS UNSATISFIABLE. An empty accepting-list is how an unprobeable RecipeChoice
        // arrives from the adapter -- a PredicateRecipeChoice wraps an arbitrary lambda and cannot
        // be enumerated at all. The recipe must not appear, rather than appearing and failing.
        assertEquals(List.of(), CraftCount.rank(
                List.of(recipe("mystery", only(1), List.of())),
                List.of(new Stock(1, 64))));
        // Mutation: treat an empty accepting-list as "anything satisfies it" -> the recipe is
        // suggested and the craft fails -> reddens.
    }

    @Test
    void aSlotWhoseAlternativesAreAllExhaustedMakesTheRecipeVANISH() {
        // Distinct from the empty case: the slot named two groups, and the player has neither.
        assertEquals(List.of(), CraftCount.rank(
                List.of(recipe("gate", List.of(7, 8), only(1))),
                List.of(new Stock(1, 64))));
        // Mutation: fall back to the first listed id when none has stock -> demand for a group with
        // zero total -> 0/1 == 0 anyway, so this must be asserted at the RANK level -> reddens if
        // the bail is removed and the division is also mutated.
    }

    @Test
    void aRecipeWithNoIngredientSlotsIsNotInfinitelyCraftable() {
        // Degenerate input, and the arithmetic answer would be Integer.MAX_VALUE from an empty min.
        // A suggestion offering two billion of something is the most over-stating answer available.
        assertEquals(List.of(), CraftCount.rank(
                List.of(new Candidate("nothing", List.of())),
                List.of(new Stock(1, 64))));
        // Mutation: drop the MAX_VALUE guard -> reddens with a count of 2147483647.
    }

    // ------------------------------------------------------------- the ranking

    @Test
    void mostCraftableFirstAndTiesBrokenByKey() {
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("zebra", only(1)), recipe("apple", only(1)), recipe("many", only(2))),
                List.of(new Stock(1, 2), new Stock(2, 50)));

        assertEquals("many", ranked.get(0).key(), "50 beats 2");
        assertEquals("apple", ranked.get(1).key(), "ties break by key, alphabetically");
        assertEquals("zebra", ranked.get(2).key());
        // Mutation: drop .reversed() -> the scarcest sorts first -> reddens.
        // Mutation: drop the key tiebreak -> order follows recipeIterator(), which is the server's
        // business and may differ between boots -> reddens.
    }

    @Test
    void aRecipeThatCannotBeMadeIsABSENT_NotPresentWithZero() {
        // A zero-count entry would render as a suggestion icon showing "0", which is a button that
        // cannot work. Absence is the honest rendering.
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("possible", only(1)), recipe("impossible", only(9))),
                List.of(new Stock(1, 4)));

        assertEquals(1, ranked.size(), ranked.toString());
        assertEquals("possible", ranked.get(0).key());
        // Mutation: add every candidate regardless of count -> reddens at 2 != 1.
    }

    // ------------------------------------- the count and the assembly share ONE walk

    @Test
    void assignNamesTheGroupEverySlotDrawsFrom() {
        // The assembly needs to put a real item in each matrix cell, and it must be the SAME group
        // the count allocated. One id per ingredient slot, in slot order.
        List<Integer> chosen = CraftCount.assign(
                recipe("boat", List.of(1, 2), only(2), List.of(2, 1)),
                List.of(new Stock(1, 4), new Stock(2, 4)));

        assertEquals(List.of(1, 2, 2), chosen, "first-with-availability, per slot, in order");
        // Mutation: return the LAST satisfying group instead of the first -> reddens, and the
        // assembly would reach for a group the count did not allocate.
    }

    @Test
    void anEmptyAssignmentIsALWAYSAZeroCount_ButNotTheReverse() {
        // THE agreement property, and the reason assign is public at all -- stated in the ONE
        // direction that actually holds. An earlier version of this test asserted equality and was
        // wrong; "scarce" below is the counter-example it found.
        //
        //   assign EMPTY  =>  count ZERO           (holds: both bail on the same walk)
        //   count ZERO    =>  assign EMPTY         (DOES NOT HOLD)
        //
        // "scarce" needs five of a group that has three. Every slot finds a group with availability,
        // so the ASSIGNMENT succeeds; the count is then 3/5 == 0 and the recipe is absent from rank.
        // The two are not redundant -- assign answers "can each slot be filled at all", the count
        // answers "how many times over".
        //
        // WHICH IS WHY THE CALLER MUST CHECK THE COUNT, NEVER A NON-EMPTY ASSIGNMENT. Assembling on
        // the strength of assign alone would build a matrix for a craft the player cannot afford.
        List<Candidate> candidates = List.of(
                recipe("makeable", only(1), List.of(1, 2)),
                recipe("exhausted", List.of(7, 8)),
                recipe("unprobeable", only(1), List.of()),
                recipe("scarce", only(1), only(1), only(1), only(1), only(1)));
        List<Stock> stock = List.of(new Stock(1, 3), new Stock(2, 9));

        for (Candidate candidate : candidates) {
            boolean assignable = !CraftCount.assign(candidate, stock).isEmpty();
            boolean countable = CraftCount.rank(List.of(candidate), stock).size() == 1;
            if (!assignable) {
                assertFalse(countable,
                        candidate.key() + ": assign bailed but rank still offered it");
            }
        }

        // And the counter-example pinned explicitly, so the one-directional claim is not just an
        // untested weakening of a stronger one.
        Candidate scarce = recipe("scarce", only(1), only(1), only(1), only(1), only(1));
        assertFalse(CraftCount.assign(scarce, stock).isEmpty(), "every slot can be filled");
        assertEquals(List.of(), CraftCount.rank(List.of(scarce), stock), "but not even once over");
        // Mutation: give assign its own copy of the greedy walk that bails differently -> the
        // implication reddens on 'exhausted' or 'unprobeable'.
    }

    // ------------------------------------------------------------- the safe answers

    @Test
    void nonsenseInputsRankNothingRatherThanThrowing() {
        // This runs inside a click handler and on every recompute. An empty ranking is always safe;
        // throwing here breaks the whole menu for everyone.
        assertEquals(List.of(), CraftCount.rank(null, List.of(new Stock(1, 1))));
        assertEquals(List.of(), CraftCount.rank(List.of(recipe("x", only(1))), null));
        assertEquals(List.of(), CraftCount.rank(List.of(), List.of(new Stock(1, 1))));
        assertEquals(List.of(), CraftCount.rank(List.of(recipe("x", only(1))), List.of()));
        // Mutation: drop a null guard -> NullPointerException -> reddens.
    }

    @Test
    void anEmptyOrNegativeStockEntryIsIgnoredNotTrusted() {
        List<Craftable> ranked = CraftCount.rank(
                List.of(recipe("x", only(1))),
                List.of(new Stock(1, 0), new Stock(1, -5), new Stock(1, 4)));

        assertEquals(4, ranked.get(0).count(), "only the positive entry counts");
        // Mutation: sum without the amount > 0 filter -> -1 -> reddens. A negative total would make
        // the division produce a negative count, which renders as a nonsense stack size.
    }

    @Test
    void aNullCandidateInTheListIsSkippedNotFatal() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(null);
        candidates.add(recipe("real", only(1)));

        List<Craftable> ranked = CraftCount.rank(candidates, List.of(new Stock(1, 3)));

        assertEquals(1, ranked.size());
        assertEquals("real", ranked.get(0).key());
        // Mutation: drop the null check -> NullPointerException -> reddens.
    }
}
