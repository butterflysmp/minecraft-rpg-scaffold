package io.github.butterflysmp.rpg.paper.adapter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EVERY TASK CORE SCHEDULES AT A POINT, SO IT CAN BE ENDED THERE (dragon-mobs step R7b, the seat's C2).
 * {@code PaperCombatWorld.schedule} is the one place core's point work enters the scheduler: an area field's pulses
 * ({@code EffectApplier}), a projectile's flight ({@code ProjectileFlight}), a ray's steps and an ember's tracking
 * ({@code CastExecutor}, {@code EffectApplier}), an Ignite fuse. Each link of such a chain is registered here with its
 * world and point; it runs only if it is still here when its delay ends ({@link #take}). {@link #cancelNear} ends every
 * link inside a sphere, and a chain whose pending link is gone never schedules the next. Entity-bound work
 * ({@code scheduleOn}) dies with its entity and is not here.
 *
 * <p>Thread-safe: links are added on region threads and taken on them, and the selftest's clean cancels from the
 * player's thread. On Paper all of those are the main thread.
 */
public final class PendingWork {

    private record Pending(UUID world, double x, double y, double z) {}

    private final Map<Long, Pending> live = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    /** Register one link scheduled at this point; its id is what {@link #take} asks with. */
    public long add(UUID world, double x, double y, double z) {
        long id = seq.incrementAndGet();
        live.put(id, new Pending(world, x, y, z));
        return id;
    }

    /** The link's delay ended: true (and forgotten) when it may run; false when it was cancelled. */
    public boolean take(long id) {
        return live.remove(id) != null;
    }

    /** End every pending link in {@code world} within {@code radius} BLOCKS of the point, inclusive. Returns how many. */
    /**
     * P2 (the seat, 2026-11-27): end every chain whose point is inside a box, {minX, minY, minZ, maxX, maxY, maxZ}, the max
     * exclusive, in that world. The selftest's row-start clean uses the pad's region, so no chain outside it is touched.
     */
    public int cancelIn(UUID world, double[] b) {
        int n = 0;
        for (var e : live.entrySet()) {
            Pending p = e.getValue();
            if (!p.world().equals(world)) continue;
            boolean in = p.x() >= b[0] && p.x() < b[3] && p.y() >= b[1] && p.y() < b[4] && p.z() >= b[2] && p.z() < b[5];
            if (in && live.remove(e.getKey(), p)) n++;
        }
        return n;
    }

    public int cancelNear(UUID world, double x, double y, double z, double radius) {
        int n = 0;
        double r2 = radius * radius;
        for (var e : live.entrySet()) {
            Pending p = e.getValue();
            if (!p.world().equals(world)) continue;
            double dx = p.x() - x, dy = p.y() - y, dz = p.z() - z;
            if (dx * dx + dy * dy + dz * dz <= r2 && live.remove(e.getKey(), p)) n++;
        }
        return n;
    }

    /** How many links are pending, anywhere. */
    public int size() {
        return live.size();
    }
}
