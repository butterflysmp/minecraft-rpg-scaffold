package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9d c: DOWN, AND ALL DOWN. A death inside makes a player DOWN, not out. When no participant is alive inside and at
 * least one is down, the fight has FAILED, and everyone goes home {@code all_down_seconds} later.
 */
class DownRosterTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void theRuleIsNobodyAliveInsideAndAtLeastOneDown() {
        assertTrue(DownRoster.allDown(0, 1), "solo: his death is the fight's failure");
        assertTrue(DownRoster.allDown(0, 3));
        assertFalse(DownRoster.allDown(1, 2), "one alive inside: not failed");
        assertFalse(DownRoster.allDown(0, 0), "nobody down and nobody inside is 'nobody inside', not all down");
        assertFalse(DownRoster.allDown(2, 0));
    }

    @Test
    void aPlayerGoesDownOnceAndIsListedUntilRevivedOrOut() {
        DownRoster r = new DownRoster(100);
        assertTrue(r.down(A), "newly down");
        assertFalse(r.down(A), "already down: not twice");
        assertTrue(r.isDown(A));
        assertFalse(r.isDown(B));
        assertTrue(r.down(B));
        assertEquals(List.of(A, B), List.copyOf(r.downed()), "in the order they went down");
        assertEquals(2, r.downCount());
        assertTrue(r.revive(A), "a revive takes him off the list");
        assertFalse(r.revive(A), "not down any more");
        r.out(B);
        assertEquals(Set.of(), r.downed(), "out (quit or left) takes him off too");
    }

    /** Solo, two and three players: the fight fails on the tick the last alive goes down, and returns delay later. */
    @Test
    void allDownFailsOnceThenReturnsAfterTheDelay() {
        DownRoster r = new DownRoster(100);
        r.down(A);
        assertEquals(DownRoster.Check.NONE, r.check(10, 2), "two of three still alive");
        r.down(B);
        assertEquals(DownRoster.Check.NONE, r.check(11, 1), "one alive");
        r.down(C);
        assertEquals(DownRoster.Check.FAILED, r.check(12, 0), "the last goes down: FAILED, once");
        assertEquals(12L, r.failedAt());
        assertEquals(DownRoster.Check.WAITING, r.check(13, 0));
        assertEquals(DownRoster.Check.WAITING, r.check(111, 0), "one tick short of the delay");
        assertEquals(DownRoster.Check.RETURN_DUE, r.check(112, 0), "failedAt + 100");
        assertEquals(DownRoster.Check.RETURN_DUE, r.check(113, 0), "and stays due");
    }

    /** Once failed it stays failed: a down player quitting during the wait does not undo it. */
    @Test
    void aFailureIsStickyThroughTheWait() {
        DownRoster r = new DownRoster(40);
        r.down(A);
        assertEquals(DownRoster.Check.FAILED, r.check(5, 0));
        r.out(A);
        assertEquals(DownRoster.Check.WAITING, r.check(6, 0), "nobody down now, and it is still failed");
        assertEquals(DownRoster.Check.RETURN_DUE, r.check(45, 0));
    }

    @Test
    void aZeroDelayIsDueTheTickAfterTheFailure() {
        DownRoster r = new DownRoster(0);
        r.down(A);
        assertEquals(DownRoster.Check.FAILED, r.check(7, 0));
        assertEquals(DownRoster.Check.RETURN_DUE, r.check(8, 0));
    }

    /** F9e e: revives are counted per player per fight; a revive that finds him not down counts nothing. */
    @Test
    void revivesAreCountedPerPlayer() {
        DownRoster r = new DownRoster(100);
        assertEquals(0, r.revives(A));
        r.down(A);
        r.revive(A);
        r.down(A);
        r.revive(A);
        r.down(B);
        r.revive(B);
        assertFalse(r.revive(C), "never down");
        assertEquals(2, r.revives(A));
        assertEquals(1, r.revives(B));
        assertEquals(0, r.revives(C));
    }

    @Test
    void aNegativeDelayIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new DownRoster(-1));
    }
}
