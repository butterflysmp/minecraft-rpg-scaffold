package io.github.butterflysmp.rpg.paper.health;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allocates fake entity ids for client-side damage-number displays, counting strictly DOWN from
 * {@link Integer#MAX_VALUE}. The server assigns real entity ids counting UP from 0, so this range can't
 * collide with a real entity within a server's uptime (ids reset on restart). One id per number, never
 * reused. Thread-safe (an {@code AtomicInteger}); the popup fires on the target's owning thread.
 *
 * <p><b>ONE COUNTER FOR EVERY PACKET-ONLY ENTITY</b> (PLAN-mining.md F-M7, the seat's ruling 8,
 * 2026-11-13). RpgPlugin builds a single instance and hands it to the damage popups AND to mining's
 * displays, so the two cannot collide. This is the smallest move that does it: the class stays where
 * it is, under its old name, with its package-private test seam unchanged, and only becomes public.
 */
public final class PopupEntityIds {

    private final AtomicInteger next;

    public PopupEntityIds() {
        this(Integer.MAX_VALUE);
    }

    /** Seam for tests: start the down-counter at a known value. */
    PopupEntityIds(int start) {
        this.next = new AtomicInteger(start);
    }

    public int next() {
        return next.getAndDecrement();
    }
}
