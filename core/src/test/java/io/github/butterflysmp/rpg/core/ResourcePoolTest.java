package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePoolTest {

    private static final String MANA = "mana";

    /** 100 mana, fully regenerated in 100 ticks. */
    private static ResourcePool pool(AtomicLong tick) {
        return new ResourcePool(tick::get, 100, 1.0);
    }

    @Test
    void anUnchargedOwnerStartsFull() {
        assertEquals(100, pool(new AtomicLong()).current(UUID.randomUUID(), MANA), 1e-9);
    }

    @Test
    void consumingSubtractsFromTheCurrentAmount() {
        var tick = new AtomicLong(0);
        var pool = pool(tick);
        var player = UUID.randomUUID();

        assertTrue(pool.tryConsume(player, MANA, 40));

        assertEquals(60, pool.current(player, MANA), 1e-9);
    }

    @Test
    void consumingMoreThanAvailableTakesNothing() {
        var tick = new AtomicLong(0);
        var pool = pool(tick);
        var player = UUID.randomUUID();
        pool.tryConsume(player, MANA, 80);

        assertFalse(pool.tryConsume(player, MANA, 40), "20 left, 40 requested");

        assertEquals(20, pool.current(player, MANA), 1e-9, "the failed spend must take nothing");
    }

    @Test
    void aCostLargerThanTheMaximumIsNeverSatisfiable() {
        var pool = pool(new AtomicLong());
        assertFalse(pool.tryConsume(UUID.randomUUID(), MANA, 101));
    }

    @Test
    void aFreeAbilityAlwaysCasts() {
        var pool = pool(new AtomicLong());
        var player = UUID.randomUUID();
        pool.tryConsume(player, MANA, 100);

        assertTrue(pool.tryConsume(player, MANA, 0), "zero cost with an empty pool");
    }

    @Test
    void manaRegeneratesLazilyAsTicksPass() {
        var tick = new AtomicLong(0);
        var pool = pool(tick);
        var player = UUID.randomUUID();
        pool.tryConsume(player, MANA, 100);
        assertEquals(0, pool.current(player, MANA), 1e-9);

        tick.set(30);
        assertEquals(30, pool.current(player, MANA), 1e-9);

        tick.set(75);
        assertEquals(75, pool.current(player, MANA), 1e-9);
    }

    @Test
    void regenerationStopsAtTheMaximum() {
        var tick = new AtomicLong(0);
        var pool = pool(tick);
        var player = UUID.randomUUID();
        pool.tryConsume(player, MANA, 50);

        tick.set(1_000_000);

        assertEquals(100, pool.current(player, MANA), 1e-9);
    }

    @Test
    void regeneratedManaCanBeSpent() {
        var tick = new AtomicLong(0);
        var pool = pool(tick);
        var player = UUID.randomUUID();
        pool.tryConsume(player, MANA, 100);

        assertFalse(pool.tryConsume(player, MANA, 40));
        tick.set(40);
        assertTrue(pool.tryConsume(player, MANA, 40));
        assertEquals(0, pool.current(player, MANA), 1e-9);
    }

    @Test
    void resourcesAreTrackedSeparatelyPerId() {
        var pool = pool(new AtomicLong());
        var player = UUID.randomUUID();

        pool.tryConsume(player, MANA, 100);

        assertEquals(0, pool.current(player, MANA), 1e-9);
        assertEquals(100, pool.current(player, "grenade_charges"), 1e-9);
    }

    @Test
    void poolsAreTrackedSeparatelyPerOwnerAndClearedIndividually() {
        var pool = pool(new AtomicLong());
        var quitter = UUID.randomUUID();
        var stayer = UUID.randomUUID();
        pool.tryConsume(quitter, MANA, 100);
        pool.tryConsume(stayer, MANA, 100);
        assertEquals(2, pool.trackedOwners());

        pool.clear(quitter);

        assertEquals(1, pool.trackedOwners());
        assertEquals(100, pool.current(quitter, MANA), 1e-9, "cleared owner reads as full");
        assertEquals(0, pool.current(stayer, MANA), 1e-9, "stayer untouched");
        assertDoesNotThrow(() -> pool.clear(UUID.randomUUID()));
    }

    /**
     * The pool has exactly 100. Sixteen threads each try to spend 40. Exactly two
     * may succeed -- a lost update would let a third through and hand a player a
     * free cast.
     */
    @Test
    void concurrentSpendsCannotOverdrawThePool() throws Exception {
        var pool = pool(new AtomicLong(0)); // clock frozen: no regen during the race
        var player = UUID.randomUUID();
        var succeeded = new AtomicInteger();

        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        try {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    start.await();
                    if (pool.tryConsume(player, MANA, 40)) succeeded.incrementAndGet();
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "workers did not finish");
        } finally {
            executor.shutdownNow();
        }

        assertEquals(2, succeeded.get(), "only two 40-mana casts fit in a 100 pool");
        assertEquals(20, pool.current(player, MANA), 1e-9);
    }
}
