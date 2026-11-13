package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regrowth deadlines in memory, per player per slot. Kept across logout (nothing removes an entry
 * except its own expiry or a slot removal); lost on restart (it is a map). 1200 ticks is Ben's
 * "1 minute is fine for now".
 */
class RegrowthClockTest {

    private static final UUID BEN = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);

    private final AtomicLong now = new AtomicLong(100);
    private final RegrowthClock clock = new RegrowthClock(now::get);

    @Test
    void aBreakAtTick100RegrowsAtTick1300() {
        assertEquals(1300L, clock.start(BEN, "slot-a", 1200));
        now.set(1299);
        assertTrue(clock.isRegrowing(BEN, "slot-a"), "one tick before the deadline");
        now.set(1300);
        assertFalse(clock.isRegrowing(BEN, "slot-a"), "regrown AT the deadline");
        // Mutation: '<' -> '<=' in isRegrowing -> still regrowing at 1300 -> reddens.
    }

    @Test
    void playersAndSlotsAreIndependent() {
        clock.start(BEN, "slot-a", 1200);
        assertFalse(clock.isRegrowing(OTHER, "slot-a"), "another player's patch is not regrowing");
        assertFalse(clock.isRegrowing(BEN, "slot-b"), "another slot is not regrowing");
    }

    @Test
    void anExpiredEntryIsPrunedWhenAsked() {
        clock.start(BEN, "slot-a", 1200);
        assertEquals(1, clock.size());
        now.set(5000);
        assertFalse(clock.isRegrowing(BEN, "slot-a"));
        assertEquals(0, clock.size());
    }

    @Test
    void removingASlotClearsItForEveryPlayer() {
        clock.start(BEN, "slot-a", 1200);
        clock.start(OTHER, "slot-a", 1200);
        clock.start(BEN, "slot-b", 1200);
        clock.clearSlot("slot-a");
        assertFalse(clock.isRegrowing(BEN, "slot-a"));
        assertFalse(clock.isRegrowing(OTHER, "slot-a"));
        assertTrue(clock.isRegrowing(BEN, "slot-b"));
    }
}
