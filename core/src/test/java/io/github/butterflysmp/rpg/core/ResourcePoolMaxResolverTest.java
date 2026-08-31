package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.MaxResolver;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PER-OWNER ceiling, and the max-change transition.
 *
 * <p>{@code ResourcePoolTest} is left byte-identical and stays the authority on everything the pool
 * did before Slice 2b -- that untouched pass is the faithfulness check on the resolver change, the
 * same one {@code EnchantCurve}'s lift used. This file covers only what is new.
 *
 * <p>The headline is {@link #raisingAnABSENTOwnersCeilingIsHeadroomOnceItIsPINNED}: absent means
 * full, so without the pin a player who has never cast reads the NEW ceiling the instant a Mana Bank
 * piece goes on -- free mana -- while a player who has cast once gets headroom. The same enchant,
 * two behaviours, decided by state nobody can see.
 *
 * Each test names the mutation it forces red.
 */
class ResourcePoolMaxResolverTest {

    private static final String MANA = "mana";
    private static final double EPS = 1e-9;

    private static final UUID PLAIN = UUID.randomUUID();
    private static final UUID ENCHANTED = UUID.randomUUID();

    /** 100 for everyone, 130 for one owner -- a Mana Bank III chestplate, in effect. */
    private static MaxResolver perOwner() {
        return (owner, resourceId) -> ENCHANTED.equals(owner) ? 130 : 100;
    }

    private static ResourcePool pool(AtomicLong tick, MaxResolver max) {
        return new ResourcePool(tick::get, max, 1.0);
    }

    // --- The ceiling is the OWNER'S -----------------------------------------------------------

    @Test
    void twoOwnersReadTheirOwnCeilingsRatherThanOneShared() {
        ResourcePool pool = pool(new AtomicLong(0), perOwner());
        assertEquals(100, pool.max(PLAIN, MANA), EPS);
        assertEquals(130, pool.max(ENCHANTED, MANA), EPS);
        // Absent means full -- at each owner's OWN ceiling, not at one number.
        assertEquals(100, pool.current(PLAIN, MANA), EPS);
        assertEquals(130, pool.current(ENCHANTED, MANA), EPS);
        // Mutation: ignore the owner in the resolver -> both read the same -> reddens.
    }

    @Test
    void theResolverIsAskedPerRESOURCEAndNotOnlyPerOwner() {
        // pools is keyed by (owner, resourceId) and the class promises "mana, and whatever else
        // content asks for". A resolver that ignored the id would have Mana Bank silently raise the
        // ceiling on every future resource at once -- and resourcesAreTrackedSeparatelyPerId would
        // stay green while being wrong, because it only ever exercises one ceiling.
        ResourcePool pool = pool(new AtomicLong(0),
                (owner, resourceId) -> MANA.equals(resourceId) ? 130 : 100);
        assertEquals(130, pool.max(ENCHANTED, MANA), EPS);
        assertEquals(100, pool.max(ENCHANTED, "grenade_charges"), EPS);
        // Mutation: drop the resourceId from the resolver signature -> the two agree -> reddens.
    }

    @Test
    void regenerationStopsAtTHISOwnersCeiling() {
        AtomicLong tick = new AtomicLong(0);
        ResourcePool pool = pool(tick, perOwner());
        pool.tryConsume(ENCHANTED, MANA, 100);          // 30 left of 130
        assertEquals(30, pool.current(ENCHANTED, MANA), EPS);

        tick.set(1_000_000);
        assertEquals(130, pool.current(ENCHANTED, MANA), EPS, "regen fills to 130, not to 100");
        // Mutation: have regenerated() clamp to a base constant -> 100 -> reddens.
    }

    @Test
    void aCostAboveTheBASEIsSatisfiableForAnOwnerWhoseCeilingIsHigher() {
        // THE tryConsume GUARD, and the only behaviour here that no read of current() would catch.
        // A 110-cost ability is unsatisfiable for a plain player and castable with Mana Bank on. If
        // the guard kept reading a base constant, the enchant would grant a ceiling but not the
        // castability -- and the refusal would report "needs 110, you have 130", which reads as a
        // contradiction on screen.
        ResourcePool pool = pool(new AtomicLong(0), perOwner());
        assertFalse(pool.tryConsume(PLAIN, MANA, 110), "110 is above a plain player's 100");
        assertTrue(pool.tryConsume(ENCHANTED, MANA, 110), "and inside an enchanted player's 130");
        assertEquals(20, pool.current(ENCHANTED, MANA), EPS);
        // Mutation: compare amount against a fixed base -> the second assertion reddens.
    }

    // --- THE HEADLINE: the max-change transition -----------------------------------------------

    @Test
    void raisingAnABSENTOwnersCeilingIsHeadroomOnceItIsPINNED() {
        // "Absent means full" is what makes this the awkward case. A player who has never cast has
        // no entry, so the moment their ceiling rises they read the NEW one -- 30 free mana. A
        // player who HAS cast keeps their stored amount and gets headroom. Pinning the pre-change
        // reading makes both behave the same way.
        AtomicLong tick = new AtomicLong(0);
        Map<UUID, Double> ceiling = new HashMap<>();
        ceiling.put(ENCHANTED, 100.0);
        ResourcePool pool = pool(tick, (owner, resourceId) -> ceiling.getOrDefault(owner, 100.0));

        assertEquals(100, pool.current(ENCHANTED, MANA), EPS, "absent, so full at the old ceiling");

        // What the reconcile does: read, raise, pin the reading it took.
        double before = pool.current(ENCHANTED, MANA);
        ceiling.put(ENCHANTED, 130.0);
        pool.setCurrent(ENCHANTED, MANA, before);

        assertEquals(130, pool.max(ENCHANTED, MANA), EPS, "the ceiling moved");
        assertEquals(100, pool.current(ENCHANTED, MANA), EPS,
                "and the amount did NOT -- headroom, not 30 free mana");
        // Mutation: skip the setCurrent pin -> current reads 130 -> reddens. THIS is the test that
        // separates "equipping is headroom" from "equipping is a free top-up".
    }

    @Test
    void loweringTheCeilingCLAMPSTheStoredAmountAtTheMomentOfWriting() {
        // The clamp would happen anyway on the next read, because regenerated() ends in
        // Math.min(ceiling, ...). It is stated HERE so a refactor of the regen path cannot drop the
        // unequip clamp with no test naming it.
        AtomicLong tick = new AtomicLong(0);
        Map<UUID, Double> ceiling = new HashMap<>();
        ceiling.put(ENCHANTED, 130.0);
        ResourcePool pool = pool(tick, (owner, resourceId) -> ceiling.getOrDefault(owner, 100.0));

        double before = pool.current(ENCHANTED, MANA);        // 130, absent means full
        assertEquals(130, before, EPS);

        ceiling.put(ENCHANTED, 100.0);                        // the piece comes off
        pool.setCurrent(ENCHANTED, MANA, before);

        assertEquals(100, pool.current(ENCHANTED, MANA), EPS, "clamped down to the new ceiling");
        // This test CANNOT see the clamp, which is worth knowing rather than assuming: deleting
        // setCurrent's Math.min leaves it green, because current() -> regenerated() ends in its own
        // min against the same ceiling. It pins the VALUE a player reads after unequipping, which is
        // the thing that matters on screen. What it does not pin is WHERE that value came from --
        // see setCurrentClampsAtTheMomentOfWRITINGAndNotMerelyAtTheNextREAD, which does.
    }

    @Test
    void loweringTheCeilingLeavesAnAlreadyLowerAmountALONE() {
        // The other branch of the same rule, and it is what stops the clamp being implemented as
        // "set current = max", which would turn taking a piece OFF into a refill.
        AtomicLong tick = new AtomicLong(0);
        Map<UUID, Double> ceiling = new HashMap<>();
        ceiling.put(ENCHANTED, 130.0);
        ResourcePool pool = pool(tick, (owner, resourceId) -> ceiling.getOrDefault(owner, 100.0));

        pool.tryConsume(ENCHANTED, MANA, 90);                 // 40 of 130
        double before = pool.current(ENCHANTED, MANA);
        assertEquals(40, before, EPS);

        ceiling.put(ENCHANTED, 100.0);
        pool.setCurrent(ENCHANTED, MANA, before);

        assertEquals(40, pool.current(ENCHANTED, MANA), EPS, "already below the new ceiling");
        // Mutation: have setCurrent write the ceiling rather than the amount -> 100, a free refill
        // for unequipping -> reddens.
    }

    @Test
    void setCurrentClampsAtTheMomentOfWRITINGAndNotMerelyAtTheNextREAD() {
        // THE test that makes the explicit clamp a decision rather than decoration, and it exists
        // because the one above it CANNOT fail. Measured, not assumed: with the Math.min deleted
        // from setCurrent, all ten other tests in this file stayed green.
        // loweringTheCeilingCLAMPS... reads through current(), current() calls
        // regenerated(entry, ceiling), and regenerated ends in its own Math.min -- so it reports
        // the clamped number whether or not the clamp was ever WRITTEN.
        //
        // The stored amount only becomes observable when the ceiling rises again with NO pin behind
        // it, because that is the one moment the regen path's min stops covering for it. The
        // production loop never produces that moment -- it pins on every real max change, and the
        // pin writes a value it just read back, which is already clamped. So this asserts
        // setCurrent's own contract in ISOLATION, which is precisely what the decision asked for:
        // the unequip clamp stated somewhere a refactor of regenerated cannot quietly take with it.
        AtomicLong tick = new AtomicLong(0);
        Map<UUID, Double> ceiling = new HashMap<>();
        ceiling.put(ENCHANTED, 100.0);
        ResourcePool pool = pool(tick, (owner, resourceId) -> ceiling.getOrDefault(owner, 100.0));

        pool.setCurrent(ENCHANTED, MANA, 130);                // above THIS owner's ceiling
        assertEquals(100, pool.current(ENCHANTED, MANA), EPS, "clamped, but the regen min agrees");

        ceiling.put(ENCHANTED, 130.0);                        // and now nothing is covering for it

        assertEquals(100, pool.current(ENCHANTED, MANA), EPS,
                "100 was STORED, so a later ceiling rise is headroom -- had the raw 130 been kept, "
                        + "it would reappear here in full");
        // Mutation: drop the Math.min from setCurrent -> the raw 130 is stored, invisible while the
        // ceiling is 100, and reappears the moment it rises -> reddens HERE and only here.
        // No tick passes in this test, so regeneration cannot be what produces the difference.
    }

    @Test
    void setCurrentRefusesAValueItCannotMeanRatherThanZeroingThePool() {
        ResourcePool pool = pool(new AtomicLong(0), perOwner());
        pool.tryConsume(ENCHANTED, MANA, 30);                 // 100 of 130
        pool.setCurrent(ENCHANTED, MANA, -5);
        pool.setCurrent(ENCHANTED, MANA, Double.NaN);
        assertEquals(100, pool.current(ENCHANTED, MANA), EPS, "neither write landed");
        // Mutation: drop the isFinite/negative guard -> NaN reaches the entry and every later read
        // of this pool is NaN -> reddens.
    }

    // --- Lifecycle ------------------------------------------------------------------------------

    @Test
    void clearIsStillAREFILLAndCannotStripTheOwnersCEILING() {
        // /rpg mana refill IS clear(owner), because absent-reads-as-full is the pool's own
        // definition of full. The ceiling lives behind the resolver rather than in a second map
        // this class owns -- which is exactly why a refill cannot take a player's Mana Bank with it.
        ResourcePool pool = pool(new AtomicLong(0), perOwner());
        pool.tryConsume(ENCHANTED, MANA, 120);
        assertEquals(10, pool.current(ENCHANTED, MANA), EPS);

        pool.clear(ENCHANTED);

        assertEquals(130, pool.current(ENCHANTED, MANA), EPS, "refilled to the BOOSTED ceiling");
        assertEquals(130, pool.max(ENCHANTED, MANA), EPS, "and the ceiling survived the clear");
        // Mutation: keep the maxima in a map inside the pool and drop it in clear() -> refill reads
        // 100 and the enchant is silently gone for the session -> reddens.
    }

    @Test
    void concurrentSpendsCannotOverdrawAPERPLAYERCeilingEither() throws Exception {
        // The sibling of ResourcePoolTest's concurrency test, at the boosted ceiling. It is here
        // because the resolver call MOVED: it is now taken once before compute() rather than inside
        // the mapping function. 130 fits exactly three 40s.
        ResourcePool pool = pool(new AtomicLong(0), perOwner());
        int threads = 16;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        try {
            for (int i = 0; i < threads; i++) {
                exec.submit(() -> {
                    start.await();
                    if (pool.tryConsume(ENCHANTED, MANA, 40)) succeeded.incrementAndGet();
                    return null;
                });
            }
            start.countDown();
            exec.shutdown();
            assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS), "workers finished");
        } finally {
            exec.shutdownNow();
        }
        assertEquals(3, succeeded.get(), "130 fits exactly three 40-mana casts");
        assertEquals(10, pool.current(ENCHANTED, MANA), EPS);
        // Mutation: resolve the ceiling INSIDE the compute lambda -> a map read under a bin lock on
        // the one path that must be atomic.
    }

    @Test
    void tryConsumeAsksTheResolverEXACTLYONCESoTheGuardAndTheSpendCannotDISAGREE() {
        // The plan asserted that resolving the ceiling inside compute() would redden the
        // concurrency test. Measured: it does not. Moving the resolve into the mapping function
        // left all 22 ResourcePool tests green, because a resolver over a plain map neither
        // deadlocks nor returns anything different. Recorded because a mutation row that never
        // reddens is a check that did not run.
        //
        // What IS observable is the arity, and that is the property actually worth holding.
        // tryConsume reads the ceiling twice in the mutated shape -- once for the
        // never-satisfiable guard, once for the absent-entry fallback -- and a resolver is a live
        // read of a player's stats, not a constant. Two reads straddling a gear change make
        // "the guard passed, then the spend refused" reachable, which reports to the player as
        // "needs 110, you have 130". One read cannot disagree with itself.
        //
        // It is also ConcurrentHashMap's own rule: a mapping function must not attempt to update
        // any mapping of the map it is computing on, and a resolver is arbitrary caller code.
        AtomicInteger asked = new AtomicInteger();
        ResourcePool pool = pool(new AtomicLong(0), (owner, resourceId) -> {
            asked.incrementAndGet();
            return 130;
        });

        assertTrue(pool.tryConsume(ENCHANTED, MANA, 40), "spent from an absent entry");
        assertEquals(1, asked.get(), "one resolve for the guard AND the fallback, not two");

        asked.set(0);
        assertTrue(pool.tryConsume(ENCHANTED, MANA, 40), "and again with an entry present");
        assertEquals(1, asked.get(), "still one -- regenerated() is handed the ceiling, not asked");
        // Mutation: resolve the ceiling inside the compute lambda -> 2 -> reddens.
    }
}
