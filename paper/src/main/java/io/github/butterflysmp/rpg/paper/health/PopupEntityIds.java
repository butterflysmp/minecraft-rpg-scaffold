package io.github.butterflysmp.rpg.paper.health;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allocates fake entity ids for client-side damage-number displays, counting strictly DOWN from
 * {@link Integer#MAX_VALUE}. The server assigns real entity ids counting UP from 0, so this range can't
 * collide with a real entity within a server's uptime (ids reset on restart). One id per number, never
 * reused. Thread-safe (an {@code AtomicInteger}); the popup fires on the target's owning thread.
 */
final class PopupEntityIds {

    private final AtomicInteger next;

    PopupEntityIds() {
        this(Integer.MAX_VALUE);
    }

    /** Seam for tests: start the down-counter at a known value. */
    PopupEntityIds(int start) {
        this.next = new AtomicInteger(start);
    }

    int next() {
        return next.getAndDecrement();
    }
}
