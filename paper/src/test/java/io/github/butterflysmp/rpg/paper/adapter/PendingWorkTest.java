package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WORK SCHEDULED AT A POINT CAN BE ENDED THERE (dragon-mobs step R7b, the seat's C2): an ability's area field, a
 * projectile's flight, an Ignite fuse, each a task that reschedules itself at a point. The boot of 3dc0e184 read a
 * grenade's field from one row destroying the next row's shot, because nothing could end it.
 */
class PendingWorkTest {

    private static final UUID W = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    void aTaskRunsOnceWhenNotCancelled() {
        PendingWork w = new PendingWork();
        long id = w.add(W, 0, 64, 0);
        assertTrue(w.take(id));
        assertFalse(w.take(id), "taken once");
    }

    @Test
    void cancellingNearAPointEndsTheTasksInsideTheRadiusAndNoOthers() {
        PendingWork w = new PendingWork();
        long inside = w.add(W, 10, 64, 0);
        long edge = w.add(W, 0, 64, 16);
        long outside = w.add(W, 0, 64, 16.01);
        long otherWorld = w.add(OTHER, 0, 64, 0);
        assertEquals(2, w.cancelNear(W, 0, 64, 0, 16));
        assertFalse(w.take(inside), "cancelled: it never runs");
        assertFalse(w.take(edge), "the radius is inclusive");
        assertTrue(w.take(outside));
        assertTrue(w.take(otherWorld), "another world is never touched");
    }

    @Test
    void aChainStopsBecauseItsNextLinkIsAddedAfterAndItsCurrentLinkIsGone() {
        PendingWork w = new PendingWork();
        long link = w.add(W, 0, 64, 0);
        assertEquals(1, w.cancelNear(W, 0, 64, 0, 5));
        assertFalse(w.take(link), "the pending link is the one cancelled; it never runs, so it never schedules the next");
        assertEquals(0, w.size());
    }
}
