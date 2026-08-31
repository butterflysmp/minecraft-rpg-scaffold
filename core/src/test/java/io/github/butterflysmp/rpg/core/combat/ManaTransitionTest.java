package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two-reconcile-and-pin, against REAL {@link CombatantStats} and {@link ResourcePool} objects.
 *
 * <p>That is the whole reason this logic was extracted from {@code PlayerHealthSystem}'s loop: every
 * argument is a core type, so the behaviour that used to be reachable only by booting a server is a
 * unit test here. Two of these tests cover failures that are completely silent in production and that
 * no boot gate checking "does mana go up" would catch.
 *
 * <p>Each test names the mutation it forces red.
 */
class ManaTransitionTest {

    private static final String MANA = "mana";
    private static final double EPS = 1e-9;
    private static final double BASE_MAX = 100.0;
    private static final double BASE_RATE_PER_TICK = 1.0;

    /** A pool whose ceiling and rate both compose base + whatever the stat store currently holds. */
    private static ResourcePool poolOver(CombatantStats stats, AtomicLong tick) {
        return new ResourcePool(tick::get,
                (owner, resourceId) -> BASE_MAX + stats.maxManaBonusValue(owner),
                (owner, resourceId) -> BASE_RATE_PER_TICK + stats.manaRegenBonusValue(owner));
    }

    // --- THE HEADLINE: the trap that is invisible inline -----------------------------------------

    @Test
    void BOTHReconcilesRunEvenWhenTheFIRSTAlreadyReportedChanged() {
        // THE test this class was extracted to make possible. Written as
        //     if (reconcileMax(...) || reconcileManaRegen(...))
        // the || short-circuits, so on any tick where the CEILING changed the RATE reconcile never
        // runs. A regen piece equipped in the same tick as a Mana Bank piece would never register,
        // and one removed would never be dropped. It compiles, it looks right, and inside the paper
        // loop nothing can observe it.
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);
        ResourcePool pool = poolOver(stats, new AtomicLong(0));

        // Both change on the same tick -- max first, which is what arms the short circuit.
        ManaTransition.reconcile(stats, pool, id, MANA,
                Map.of("manabank:CHEST", 30.0), Map.of("manaregen:CHEST", 2.0));

        assertEquals(30.0, stats.maxManaBonusValue(id), EPS, "the ceiling stat converged");
        assertEquals(2.0, stats.manaRegenBonusValue(id), EPS,
                "AND THE RATE STAT DID TOO -- with `||` this reads 0.0, because the second "
                        + "reconcile never ran");
        // Mutation: replace the two locals with `if (reconcileMax(...) || reconcileRegen(...))`
        // -> the rate row reads 0.0 -> reddens.
    }

    // --- The freeze guard -------------------------------------------------------------------------

    @Test
    void aSteadyStatePinsNOTHINGOrManaStopsRegeneratingENTIRELY() {
        // The loop runs four times a second. A pin on every pass re-stamps the entry's asOfTick, so
        // elapsed never grows past the period and mana simply stops filling -- silently, with a stat
        // block that still reads correctly. NEXT.md records this for the ceiling; it is identical for
        // the rate.
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);
        AtomicLong tick = new AtomicLong(0);
        ResourcePool pool = poolOver(stats, tick);

        assertTrue(pool.tryConsume(id, MANA, 100), "spend to empty at tick 0");

        Map<String, Double> steadyMax = Map.of("manabank:CHEST", 30.0);
        Map<String, Double> steadyRegen = Map.of("manaregen:CHEST", 2.0);
        assertTrue(ManaTransition.reconcile(stats, pool, id, MANA, steadyMax, steadyRegen),
                "the first pass is a real transition and pins");

        // Now hold gear steady and run the loop repeatedly while time passes, exactly as the server
        // does at four passes a second.
        for (int t = 1; t <= 20; t++) {
            tick.set(t);
            assertFalse(ManaTransition.reconcile(stats, pool, id, MANA, steadyMax, steadyRegen),
                    "pass at tick " + t + " must not pin -- nothing moved");
        }

        assertEquals(60.0, pool.current(id, MANA), EPS,
                "20 ticks at the composed rate of 3.0 -- mana kept regenerating THROUGH the loop");
        // Mutation: pin unconditionally (drop the !maxChanged && !regenChanged early return) ->
        // every pass re-stamps asOfTick, elapsed is always 0, current stays 0.0 -> reddens.
    }

    // --- The rate transition ----------------------------------------------------------------------

    @Test
    void aRATEChangePinsSoTheNewSlopeAppliesFORWARDOnly() {
        // Without the pin, amount + elapsed * rate re-prices the past: 20 ticks accrued at 1.0 become
        // 20 ticks at 3.0 the instant the piece goes on. That is the free-on-equip defect the ceiling
        // already forbids.
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);
        AtomicLong tick = new AtomicLong(0);
        ResourcePool pool = poolOver(stats, tick);

        assertTrue(pool.tryConsume(id, MANA, 100), "empty at tick 0");
        tick.set(20);
        assertEquals(20.0, pool.current(id, MANA), EPS, "20 ticks at the base 1.0");

        assertTrue(ManaTransition.reconcile(stats, pool, id, MANA,
                Map.of(), Map.of("manaregen:CHEST", 2.0)), "the rate moved, so it pinned");

        assertEquals(20.0, pool.current(id, MANA), EPS,
                "STILL 20 -- equipping granted nothing. Unpinned this reads 60.");

        tick.set(30);
        assertEquals(50.0, pool.current(id, MANA), EPS,
                "and the new composed rate of 3.0 applies forward: 20 + 10 ticks x 3.0");
        // Mutation: pin only on maxChanged (ignore regenChanged) -> the middle row reads 60.0 ->
        // reddens. That is the whole slice in one assertion.
    }

    @Test
    void aCEILINGChangeStillPinsExactlyAsItDidBeforeThisSlice() {
        // The 2b regression guard. Raising the ceiling for an owner with no entry must be headroom,
        // not a free top-up.
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);
        ResourcePool pool = poolOver(stats, new AtomicLong(0));

        assertEquals(100.0, pool.current(id, MANA), EPS, "absent means full at the old ceiling");

        assertTrue(ManaTransition.reconcile(stats, pool, id, MANA,
                Map.of("manabank:CHEST", 30.0), Map.of()), "the ceiling moved, so it pinned");

        assertEquals(130.0, pool.max(id, MANA), EPS, "the ceiling rose");
        assertEquals(100.0, pool.current(id, MANA), EPS, "and the amount did NOT -- headroom");
        // Mutation: pin only on regenChanged (ignore maxChanged) -> this reads 130.0 -> reddens.
    }

    @Test
    void anUntrackedCombatantIsANoOpAndWritesNOTHINGToThePool() {
        // "A pool nobody reads costs nothing" has to survive this being called for every player every
        // tick, including one between bootstrap and register.
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        ResourcePool pool = poolOver(stats, new AtomicLong(0));

        assertFalse(ManaTransition.reconcile(stats, pool, id, MANA,
                Map.of("manabank:CHEST", 30.0), Map.of("manaregen:CHEST", 2.0)),
                "neither reconcile could move an untracked combatant, so nothing pinned");
        assertEquals(0, pool.trackedOwners(), "and no pool entry was created for them");
        // Mutation: pin unconditionally -> trackedOwners becomes 1 -> reddens.
    }
}
