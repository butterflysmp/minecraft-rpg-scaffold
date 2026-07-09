package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.combat.CooldownTracker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CooldownTrackerTest {

    @Test
    void triggerThenNotReadyUntilTickElapses() {
        var tick = new java.util.concurrent.atomic.AtomicLong(0);
        var cooldowns = new CooldownTracker(tick::get);
        var player = UUID.randomUUID();

        assertTrue(cooldowns.isReady(player, "grenade"));
        cooldowns.trigger(player, "grenade", 200);

        assertFalse(cooldowns.isReady(player, "grenade"));
        assertEquals(200, cooldowns.ticksRemaining(player, "grenade"));

        tick.set(200);
        assertTrue(cooldowns.isReady(player, "grenade"));
        assertEquals(0, cooldowns.ticksRemaining(player, "grenade"));
    }

    /**
     * Under Folia two players in different regions cast at the same instant, on
     * different threads. With a plain HashMap that races: a resize can drop
     * entries outright, so a cooldown silently never fires.
     */
    @Test
    void concurrentTriggersFromManyThreadsDoNotLoseCooldowns() throws Exception {
        var cooldowns = new CooldownTracker(() -> 0L);
        int players = 64, abilitiesEach = 32;

        List<UUID> ids = java.util.stream.Stream.generate(UUID::randomUUID)
                .limit(players).toList();

        ExecutorService pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        try {
            for (UUID id : ids) {
                pool.submit(() -> {
                    start.await();
                    for (int a = 0; a < abilitiesEach; a++) {
                        cooldowns.trigger(id, "ability_" + a, 200);
                    }
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "workers did not finish");
        } finally {
            pool.shutdownNow();
        }

        // Every single (player, ability) pair must have survived the race.
        for (UUID id : ids) {
            for (int a = 0; a < abilitiesEach; a++) {
                assertFalse(cooldowns.isReady(id, "ability_" + a),
                        "lost cooldown for " + id + " ability_" + a);
            }
        }
    }

    @Test
    void clearRemovesOnlyThatPlayersCooldowns() {
        var cooldowns = new CooldownTracker(() -> 0L);
        var quitter = UUID.randomUUID();
        var stayer = UUID.randomUUID();

        cooldowns.trigger(quitter, "grenade", 200);
        cooldowns.trigger(stayer, "grenade", 200);

        cooldowns.clear(quitter);

        assertTrue(cooldowns.isReady(quitter, "grenade"), "quitter's entries must be gone");
        assertFalse(cooldowns.isReady(stayer, "grenade"), "stayer must be untouched");
    }

    /**
     * clear() must not merely reset the cooldown; it must drop the player's
     * bucket, or the map grows without bound for the lifetime of the server.
     */
    @Test
    void clearIsBoundedAndSafeForAnUnknownPlayer() {
        var cooldowns = new CooldownTracker(() -> 0L);
        assertEquals(0, cooldowns.trackedPlayers());

        var player = UUID.randomUUID();
        cooldowns.trigger(player, "grenade", 200);
        assertEquals(1, cooldowns.trackedPlayers());

        cooldowns.clear(player);
        assertEquals(0, cooldowns.trackedPlayers());

        assertDoesNotThrow(() -> cooldowns.clear(UUID.randomUUID()));
        assertEquals(0, cooldowns.trackedPlayers());
    }
}
