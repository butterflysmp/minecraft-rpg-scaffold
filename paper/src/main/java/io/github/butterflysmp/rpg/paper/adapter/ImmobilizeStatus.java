package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Immobilize (Rooted): zero a mob's velocity every tick for a duration, then cleanly stop.
 *
 * Shared, one instance -- like {@link AdapterContext}'s warn-once set -- because re-rooting a
 * mob must find its existing task and refresh it, not stack a second one. Keyed by entity
 * UUID; the map is concurrent because different entities apply from different region threads,
 * but a single entity's entry is only ever read/written on that entity's own thread.
 *
 * The lifecycle -- registered on apply, self-cancel on expiry, self-cancel when the target
 * reports removed, refresh-not-stack -- is unit-tested against a fake target. The velocity
 * zeroing (the {@code perTick} action) is the only Bukkit-touching part and is boot-witnessed.
 */
public final class ImmobilizeStatus {

    /** A live root: its handle plus the ticks it has left (touched only on the owner's thread). */
    private static final class Active {
        RepeatingTask task;
        int remaining;
        Active(int remaining) { this.remaining = remaining; }
    }

    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    /**
     * Start rooting {@code id}, or refresh an existing root instead of stacking a second task.
     * {@code perTick} is the per-frame effect (velocity-zero in production, a counter in
     * tests); it runs on the target's own thread and never fires against a removed target.
     */
    public void apply(UUID id, RepeatingTaskTarget target, int durationTicks, Runnable perTick) {
        Active existing = active.get(id);
        if (existing != null && existing.task.isRunning()) {
            existing.remaining = Math.max(existing.remaining, durationTicks); // refresh, do not stack
            return;
        }
        Active a = new Active(durationTicks);
        BooleanSupplier tick = () -> {
            if (a.remaining <= 0) return false;  // expired -> stop; onStop cleans the map entry
            perTick.run();
            a.remaining--;
            return true;
        };
        // start() only schedules the first tick, so the task never fires before we store it.
        a.task = RepeatingTask.start(target, 1, tick, () -> active.remove(id, a));
        active.put(id, a);
    }

    /** True while {@code id} has a live root. For tests and future multi-status composition. */
    public boolean isRooted(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning();
    }

    /** Ticks remaining on {@code id}'s root, or 0 if it is not rooted. For tests. */
    public int remainingTicks(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning() ? a.remaining : 0;
    }
}
