package io.github.butterflysmp.rpg.core.combat.stat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reconcile was chosen over event-driven add/remove precisely because it is leak-proof across EVERY
 * way an item can leave the hand -- drop, die, /clear, hotbar-swap, break, despawn -- not just a
 * clean unequip. At this level every one of those reduces to the same thing: the source is not in
 * the desired set this cycle. So these tests exercise that reduction, and the transition-fires-once
 * property (a steady state moves nothing). Each names the mutation it forces red.
 */
class ModifierReconcilerTest {

    private static final double EPS = 1e-9;
    private static final String SLOT = "mainhand";

    private static HealthState equipped() {
        var state = new HealthState(100, true);           // 100/100
        ModifierReconciler.reconcile(state, Map.of(SLOT, 300.0)); // 100/400
        return state;
    }

    @Test
    void equipAddsUnderTheSlotSourceWithHeadroom() {
        var state = new HealthState(100, true);
        boolean changed = ModifierReconciler.reconcile(state, Map.of(SLOT, 300.0));

        assertTrue(changed, "equipping a +HP item changed the max");
        assertEquals(400, state.max(), EPS, "max rose to 400");
        assertEquals(100, state.current(), EPS, "current unchanged -- headroom");
        // Mutation: reconcile heals current to the new max -> 400 -> reddens (equip = free heal).
    }

    @Test
    void everyRemovalPathReducesToAnAbsentSourceAndRemovesTheModifier() {
        // drop(Q), die, /clear, break, despawn: nothing equipped -> desired is empty.
        // hotbar-swap-away or swap to a non-tagged item: the tagged slot is simply not desired.
        // All five collapse to "SLOT is absent from desired" -- that is why none can be missed.
        List<Map.Entry<String, Map<String, Double>>> paths = List.of(
                Map.entry("drop / die / clear / break / despawn (nothing equipped)", Map.of()),
                Map.entry("swap to a non-tagged item (slot present but untracked)", Map.of()),
                Map.entry("moved to an unscanned slot (a different source only)", Map.of("cosmetic", 0.0)));

        for (var path : paths) {
            var state = equipped();                       // re-equip fresh each path: 100/400
            state.heal(1000);                             // 400/400, so the clamp has something to bite

            boolean changed = ModifierReconciler.reconcile(state, path.getValue());

            assertFalse(state.hasMaxModifier(SLOT), "the modifier is gone after: " + path.getKey());
            assertEquals(100, state.max(), EPS, "max back to base after: " + path.getKey());
            assertEquals(100, state.current(), EPS, "current clamped to new max after: " + path.getKey());
            assertTrue(changed, "the removal registered as a change: " + path.getKey());
        }
        // Mutation: only remove a source when desired maps it to 0 (a special "unequip" signal) ->
        // an item that simply vanished from desired leaks its modifier forever -> reddens on every path.
    }

    @Test
    void aSteadyStateMovesNothingSoTheTransitionFiresOnce() {
        var state = equipped();                           // 100/400 (headroom: current is 100, not 400)
        state.damage(50);                                 // 50/400 -- a wounded, still-equipped combatant

        boolean changed = ModifierReconciler.reconcile(state, Map.of(SLOT, 300.0));

        assertFalse(changed, "item still equipped, same amount -> nothing changed this cycle");
        assertEquals(50, state.current(), EPS, "current NOT re-healed to headroom nor re-clamped -- left at 50");
        assertEquals(400, state.max(), EPS, "max steady");
        // Mutation: reconcile re-applies (returns true / touches current) each tick -> current
        // snaps back toward 100 or the event fires every tick on a wounded standing player -> reddens.
    }

    @Test
    void raisingThenLoweringTheAmountAppliesHeadroomThenClamp() {
        var state = equipped();                           // 100/400
        state.heal(1000);                                 // 400/400

        assertTrue(ModifierReconciler.reconcile(state, Map.of(SLOT, 500.0)), "amount rose");
        assertEquals(600, state.max(), EPS, "max rose to 600");
        assertEquals(400, state.current(), EPS, "current unchanged on a rise -- headroom");

        assertTrue(ModifierReconciler.reconcile(state, Map.of(SLOT, 100.0)), "amount fell");
        assertEquals(200, state.max(), EPS, "max fell to 200");
        assertEquals(200, state.current(), EPS, "current clamped to the lower max");
        // Mutation: treat an amount change as no-op (only add/remove) -> max stays 400 -> reddens.
    }

    @Test
    void wouldRemoveNamesExactlyTheDroppedSources() {
        var state = equipped();
        assertEquals(java.util.Set.of(SLOT), ModifierReconciler.wouldRemove(state, Map.of()),
                "an empty desired drops the one applied source");
        assertEquals(java.util.Set.of(), ModifierReconciler.wouldRemove(state, Map.of(SLOT, 300.0)),
                "a desired that keeps the source drops nothing");
        // Mutation: wouldRemove ignores desired -> reports SLOT even when kept -> reddens.
    }
}
