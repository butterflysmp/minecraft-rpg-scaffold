package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.weapon.CollectPlan.Draw;
import io.github.butterflysmp.rpg.core.weapon.CollectPlan.Source;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The double-click's ordering rule and its stack arithmetic.
 *
 * <p>The gesture itself is performed by {@code MenuRouting} and is boot-gate-only, like everything
 * else on that surface. This is the half that does not have to be: which sources drain, in which
 * order, and how much comes off each. Extracted so the rule that protects a player's staged recipe
 * has a real witness rather than a gate row.
 *
 * <p>Each test names the mutation it forces red.
 */
class CollectPlanTest {

    private static Source inv(int slot, int amount) {
        return new Source(CollectPlan.TIER_INVENTORY, slot, amount);
    }

    private static Source grid(int slot, int amount) {
        return new Source(CollectPlan.TIER_MENU, slot, amount);
    }

    // ------------------------------------------------------------ the tier boundary

    @Test
    void theInventoryDrainsBeforeTheGridEvenWhenTheGridStacksAreSmaller() {
        // THE rule this class exists for. A staged recipe is made of PARTIAL stacks, so a
        // smallest-first order that ignored tiers would prefer the grid exactly when a recipe is
        // loaded -- the most vanilla ordering is the one that most reliably eats the layout.
        List<Draw> draws = CollectPlan.plan(
                List.of(grid(11, 6), inv(30, 64)), 1, 64);

        assertEquals(1, draws.size(), draws.toString());
        assertEquals(CollectPlan.TIER_INVENTORY, draws.get(0).source().tier(),
                "the inventory must drain first even though the grid stack is smaller");
        assertEquals(63, draws.get(0).amount());
        // Mutation: drop tier from DRAIN_ORDER -> the grid's 6 is taken first -> reddens.
    }

    @Test
    void theGridIsREACHEDWhenTheInventoryCannotFillTheCursor() {
        // The other half, and without it "inventory first" and "inventory only" are the same thing.
        // The natural implementation error here is a second pass that is never reached.
        List<Draw> draws = CollectPlan.plan(
                List.of(inv(30, 10), grid(11, 40)), 1, 64);

        assertEquals(2, draws.size(), draws.toString());
        assertEquals(CollectPlan.TIER_INVENTORY, draws.get(0).source().tier());
        assertEquals(10, draws.get(0).amount());
        assertEquals(CollectPlan.TIER_MENU, draws.get(1).source().tier());
        // 40, not 53: the cursor had room for 53 more after the inventory's 10, but the grid slot
        // only HOLDS 40, and a draw is capped by its source. Executed, not predicted -- the first
        // version of this assertion said 53 and the code was right.
        assertEquals(40, draws.get(1).amount(), "the grid supplies what it has");
        assertEquals(50, CollectPlan.total(draws));
        // Mutation: stop after the first tier -> reddens at 1 != 2. This is gate row S6's defect.
    }

    // --------------------------------------------------------- vanilla's consolidation

    @Test
    void withinATierTheSmallestStacksDrainFirst() {
        // The half of vanilla's behaviour that is KEPT: consolidate fragments, do not break up a
        // full stack while partials are lying around.
        List<Draw> draws = CollectPlan.plan(
                List.of(inv(30, 64), inv(31, 3), inv(32, 9)), 0, 64);

        assertEquals(3, draws.get(0).source().amount(), "smallest first");
        assertEquals(9, draws.get(1).source().amount());
        assertEquals(64, draws.get(2).source().amount());
        // Mutation: reverse the amount comparator -> takes the full stack first -> reddens.
    }

    @Test
    void tiesAreBrokenBySlotSoTwoRunsAgree() {
        // Determinism. GRID_SLOTS is a Set.copyOf, whose iteration order the JDK leaves
        // unspecified, so without this the plan would depend on hash order.
        List<Draw> draws = CollectPlan.plan(
                List.of(grid(31, 5), grid(11, 5), grid(21, 5)), 0, 64);

        assertEquals(11, draws.get(0).source().slot(), "left to right by slot");
        assertEquals(21, draws.get(1).source().slot());
        assertEquals(31, draws.get(2).source().slot());
        // Mutation: drop the slot tiebreak -> order becomes input order -> reddens.
    }

    // ------------------------------------------------------------- the stack cap

    @Test
    void theCursorNeverExceedsItsMaxStackSize() {
        // The 64-onto-40 shape, one tier up. Executed rather than reasoned about.
        List<Draw> draws = CollectPlan.plan(
                List.of(inv(30, 64), inv(31, 64)), 40, 64);

        assertEquals(24, CollectPlan.total(draws), "40 + 24 == 64, and not one more");
        assertEquals(1, draws.size(), "the second stack is never reached");
        // Mutation: ignore cursorAmount when computing room -> total becomes 64 -> reddens.
    }

    @Test
    void aFullCursorGathersNothing() {
        assertEquals(List.of(), CollectPlan.plan(List.of(inv(30, 64)), 64, 64));
        assertEquals(List.of(), CollectPlan.plan(List.of(inv(30, 64)), 99, 64));
        // Mutation: use `room < 0` instead of `<= 0` -> the exactly-full case draws -> reddens.
    }

    @Test
    void aDrawNeverExceedsWhatItsSourceHolds() {
        // The conservation property, stated directly: you cannot take five from a stack of three.
        List<Draw> draws = CollectPlan.plan(
                List.of(inv(30, 3), inv(31, 2)), 0, 64);

        for (Draw draw : draws) {
            assertTrue(draw.amount() <= draw.source().amount(),
                    "took " + draw.amount() + " from a stack of " + draw.source().amount());
        }
        assertEquals(5, CollectPlan.total(draws));
        // Mutation: take `room` rather than min(room, amount) -> reddens, and would duplicate items.
    }

    // ------------------------------------------------------------- the safe answers

    @Test
    void nonsenseInputsPlanNothingRatherThanThrowing() {
        // This runs inside a click handler. An empty plan is always safe -- performing nothing
        // loses nothing -- whereas throwing here would break the gesture for everyone.
        assertEquals(List.of(), CollectPlan.plan(null, 0, 64));
        assertEquals(List.of(), CollectPlan.plan(List.of(inv(30, 5)), 0, 0));
        assertEquals(List.of(), CollectPlan.plan(List.of(), 0, 64));
        // Mutation: drop the maxStackSize guard -> room is negative or the plan is empty for the
        // wrong reason -> reddens on the second row.
    }

    @Test
    void anEmptySourceIsSkippedNotDrawnFrom() {
        List<Draw> draws = CollectPlan.plan(
                List.of(inv(30, 0), inv(31, 4)), 0, 64);

        assertEquals(1, draws.size());
        assertEquals(4, draws.get(0).amount());
        // Mutation: drop the `amount <= 0` skip -> a zero-amount draw appears -> reddens.
    }
}
