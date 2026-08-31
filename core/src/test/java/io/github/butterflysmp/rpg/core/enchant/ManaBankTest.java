package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mana Bank: the bonus, and the whole path from a worn piece to a ceiling.
 *
 * <p>The headline is {@link #fourPiecesComposeThroughTheStatAndTheResolverIntoOneCeiling} -- the
 * end-to-end that no other test covers, because this enchant is the first whose value crosses TWO
 * stores. Growth's bonus lands on the same object that holds max health; this one lands on
 * {@code HealthState} and is read by {@code ResourcePool} through a resolver, so "the stat is right"
 * and "the pool agrees" are two different claims.
 *
 * <p>{@code ResourcePoolMaxResolverTest} owns the pool's own behaviour. This owns the composition.
 *
 * Each test names the mutation it forces red.
 */
class ManaBankTest {

    private static final double EPS = 1e-9;
    private static final String MANA = "mana";
    private static final double BASE = 100.0;

    // --- The arithmetic ---------------------------------------------------------------------------

    @Test
    void aPieceContributesItsBonusAndNothingElse() {
        assertEquals(10.0, ManaBank.contribution(10), EPS);
        assertEquals(30.0, ManaBank.contribution(30), EPS);
        assertFalse(ManaBank.boosts(ManaBank.NONE), "zero declares no bonus");
        assertTrue(ManaBank.boosts(0.5), "anything above zero does");
        // Mutation: make boosts() use >= -> the scan writes a zero-valued source instead of leaving
        // it absent, and the reconciler stops dropping it -> reddens.
    }

    // --- THE HEADLINE: stat -> resolver -> pool ---------------------------------------------------

    @Test
    void fourPiecesComposeThroughTheStatAndTheResolverIntoOneCeiling() {
        UUID player = UUID.randomUUID();
        CombatantStats stats = new CombatantStats();
        stats.register(player, CombatantStats.DEFAULT_PLAYER_BASE, true);

        // What the reconcile loop converges to: one namespaced source per worn piece.
        stats.reconcileMaxManaModifiers(player, Map.of(
                "manabank:HEAD", ManaBank.contribution(30),
                "manabank:CHEST", ManaBank.contribution(30),
                "manabank:LEGS", ManaBank.contribution(30),
                "manabank:FEET", ManaBank.contribution(30)));

        assertEquals(120.0, stats.maxManaBonusValue(player), EPS,
                "the stat holds only the gear-contributed part");

        // And what the pool makes of it, through the resolver paper wires.
        ResourcePool pool = new ResourcePool(new AtomicLong(0)::get,
                (owner, resourceId) -> MANA.equals(resourceId)
                        ? BASE + stats.maxManaBonusValue(owner)
                        : BASE,
                1.0);
        assertEquals(220.0, pool.max(player, MANA), EPS, "base 100 plus a full Mana Bank III set");
        assertEquals(220.0, pool.current(player, MANA), EPS, "and absent still means full");
        // Mutation: have the stat hold the TOTAL rather than the bonus -> the resolver double-counts
        // the base and the ceiling reads 320 -> reddens.
    }

    @Test
    void anUntrackedCombatantResolvesToTheBASERatherThanThrowing() {
        // The reason maxManaBonusValue mirrors defenseValue (0.0 for untracked) rather than max()
        // (which THROWS). This is read from inside tryConsume, on whatever thread is casting, for
        // any owner -- including a mob firing a costed trigger before it is ever registered. A throw
        // there would come out of a cast.
        CombatantStats stats = new CombatantStats();
        UUID neverRegistered = UUID.randomUUID();
        assertEquals(0.0, stats.maxManaBonusValue(neverRegistered), EPS);

        ResourcePool pool = new ResourcePool(new AtomicLong(0)::get,
                (owner, resourceId) -> BASE + stats.maxManaBonusValue(owner), 1.0);
        assertEquals(BASE, pool.max(neverRegistered, MANA), EPS);
        assertTrue(pool.tryConsume(neverRegistered, MANA, 40), "and a mob can still spend");
        // Mutation: implement maxManaBonusValue with require(id) like max() -> this throws, and so
        // does every mob cast -> reddens.
    }

    @Test
    void twoPiecesInOneSlotWouldREPLACESoTheKeysArePerSlot() {
        // Stat.putModifier is put-or-REPLACE. Four pieces sum only because each writes its own key.
        // Collapsing them onto one source would silently keep whichever the scan wrote last.
        UUID player = UUID.randomUUID();
        CombatantStats stats = new CombatantStats();
        stats.register(player, CombatantStats.DEFAULT_PLAYER_BASE, true);

        stats.reconcileMaxManaModifiers(player, Map.of("manabank:HEAD", 30.0, "manabank:CHEST", 30.0));
        assertEquals(60.0, stats.maxManaBonusValue(player), EPS, "distinct keys sum");

        stats.reconcileMaxManaModifiers(player, Map.of("manabank", 30.0));
        assertEquals(30.0, stats.maxManaBonusValue(player), EPS,
                "one shared key holds one value, whatever the player is wearing");
        // Mutation: key the scan by a constant instead of by slot -> a full set grants 30 -> reddens.
    }

    @Test
    void removingEveryPieceLeavesTheStatAtZeroAndTheCeilingAtBase() {
        // Leak-proofness, at the mana target. Absence is the removal signal, as everywhere else.
        UUID player = UUID.randomUUID();
        CombatantStats stats = new CombatantStats();
        stats.register(player, CombatantStats.DEFAULT_PLAYER_BASE, true);

        stats.reconcileMaxManaModifiers(player, Map.of("manabank:CHEST", 30.0));
        assertEquals(30.0, stats.maxManaBonusValue(player), EPS);

        stats.reconcileMaxManaModifiers(player, Map.of());
        assertEquals(0.0, stats.maxManaBonusValue(player), EPS, "the piece came off by any route");
        // Mutation: skip the removal branch -> the bonus survives the piece -> reddens.
    }

    @Test
    void theReconcileReportsWhetherItActuallyMovedSoTheCallerCanPin() {
        // Unlike its void siblings, this reconciler returns a boolean -- the caller needs it to know
        // whether to pin the pool's pre-change reading. A steady state must report false, or the pin
        // would run four times a second and freeze regeneration.
        UUID player = UUID.randomUUID();
        CombatantStats stats = new CombatantStats();
        stats.register(player, CombatantStats.DEFAULT_PLAYER_BASE, true);

        assertTrue(stats.reconcileMaxManaModifiers(player, Map.of("manabank:CHEST", 30.0)),
                "a new source is a transition");
        assertFalse(stats.reconcileMaxManaModifiers(player, Map.of("manabank:CHEST", 30.0)),
                "the same source at the same amount is not");
        assertTrue(stats.reconcileMaxManaModifiers(player, Map.of()), "and removal is");
        // Mutation: return true unconditionally -> the pin runs every tick, pinning current to
        // itself forever, and mana stops regenerating -> reddens.
    }
}
