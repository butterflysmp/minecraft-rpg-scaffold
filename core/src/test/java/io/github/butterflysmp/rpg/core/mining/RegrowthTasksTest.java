package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's D1, 2026-11-13: at most ONE pending regrowth task per player and slot, and a task
 * that outlived its player's session (or its slot) does nothing when it runs. The token is what
 * makes a dropped or stale task harmless: only the current token may show the patch.
 */
class RegrowthTasksTest {

    private static final UUID BEN = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);

    private final RegrowthTasks tasks = new RegrowthTasks();

    @Test
    void theFirstClaimGetsATokenAndASecondClaimIsRefused() {
        OptionalLong first = tasks.claim(BEN, "slot-a");
        assertTrue(first.isPresent());
        assertTrue(tasks.claim(BEN, "slot-a").isEmpty(), "one pending task per player and slot");
        assertTrue(tasks.isCurrent(BEN, "slot-a", first.getAsLong()));
        assertEquals(1, tasks.size());
        // Mutation: claim always returns a fresh token (no dedupe) -> the second claim is present -> reddens.
    }

    @Test
    void playersAndSlotsClaimIndependently() {
        assertTrue(tasks.claim(BEN, "slot-a").isPresent());
        assertTrue(tasks.claim(OTHER, "slot-a").isPresent(), "another player");
        assertTrue(tasks.claim(BEN, "slot-b").isPresent(), "another slot");
        assertEquals(3, tasks.size());
    }

    @Test
    void releaseFreesTheMarkerOnlyForTheCurrentToken() {
        long token = tasks.claim(BEN, "slot-a").getAsLong();
        assertFalse(tasks.release(BEN, "slot-a", token + 1), "a stale token releases nothing");
        assertTrue(tasks.claim(BEN, "slot-a").isEmpty(), "still held");
        assertTrue(tasks.release(BEN, "slot-a", token));
        assertEquals(0, tasks.size());
        assertTrue(tasks.claim(BEN, "slot-a").isPresent(), "free again after the task showed the patch");
    }

    @Test
    void aQuitClearsThePlayersMarkersSoTheNextSessionSchedulesAgain() {
        long old = tasks.claim(BEN, "slot-a").getAsLong();
        tasks.claim(OTHER, "slot-a");
        tasks.clearPlayer(BEN);
        assertFalse(tasks.isCurrent(BEN, "slot-a", old), "the old session's task is now stale");
        OptionalLong fresh = tasks.claim(BEN, "slot-a");
        assertTrue(fresh.isPresent(), "the rejoined player gets a task");
        assertNotEquals(old, fresh.getAsLong(), "tokens are never reused, so the stale task stays stale");
        assertFalse(tasks.isCurrent(BEN, "slot-a", old));
        assertTrue(tasks.claim(OTHER, "slot-a").isEmpty(), "another player's marker is untouched");
    }

    @Test
    void aRemovedSlotClearsEveryPlayersMarkerThere() {
        long ben = tasks.claim(BEN, "slot-a").getAsLong();
        long other = tasks.claim(OTHER, "slot-a").getAsLong();
        tasks.claim(BEN, "slot-b");
        tasks.clearSlot("slot-a");
        assertFalse(tasks.isCurrent(BEN, "slot-a", ben));
        assertFalse(tasks.isCurrent(OTHER, "slot-a", other));
        assertEquals(1, tasks.size(), "slot-b's marker stays");
    }
}
