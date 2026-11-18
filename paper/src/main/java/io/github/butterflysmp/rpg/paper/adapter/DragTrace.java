package io.github.butterflysmp.rpg.paper.adapter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * THE DRAG'S PER-TICK INSTRUMENT, ITS SWITCH AND ITS ONE COUNTER (feat/drag-additive, 2026-11-18; the seat's Part D).
 * While {@link #on()}, {@link ImplosionDrags} logs one {@code DRAGTICK} line per player victim per drag tick. It is
 * OFF at startup, turned ON only by the selftest's {@code drag_tick: on} step, and turned OFF again by the run's
 * restore ({@code SelfTestSnapshot}).
 *
 * <p><b>THE MOVES COUNTER</b> is the one thing a packet thread touches: {@code MoveCountListener} calls
 * {@link #moved} for every position-bearing move packet a player sends ({@code ServerboundMovePlayerPacket}'s
 * {@code Pos} and {@code PosRot}), on the Netty thread, and does nothing else (CLAUDE.md THREADING). The count is
 * cumulative; each drag remembers what it last read, so "moves since the last tick" is a difference and two drags on
 * one player do not steal each other's counts. Pure Java: no Bukkit here, so the packet thread can call it.
 */
public final class DragTrace {

    private volatile boolean on;
    private final Map<UUID, AtomicLong> moves = new ConcurrentHashMap<>();

    public boolean on() {
        return on;
    }

    public void set(boolean on) {
        this.on = on;
    }

    /** NETTY THREAD: one position-bearing move packet from {@code player}. Touches nothing else. */
    public void moved(UUID player) {
        if (player != null) moves.computeIfAbsent(player, k -> new AtomicLong()).incrementAndGet();
    }

    /** Every position-bearing move packet {@code player} has sent since the counter first saw him. */
    public long moves(UUID player) {
        AtomicLong n = moves.get(player);
        return n == null ? 0 : n.get();
    }

    /** NETTY THREAD: he disconnected; his count goes, so the map holds only the connected. */
    public void forget(UUID player) {
        if (player != null) moves.remove(player);
    }
}
