package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Immobilize (Rooted): stop a mob dead for a duration, then cleanly restore it.
 *
 * TWO mechanisms, because they cover each other's gaps: a MOVEMENT_SPEED modifier of factor 0
 * kills the mob's self-propelled AI drive at the source (velocity-zero alone only cancels the
 * AI's output each tick, imperfectly, leaving a ~1% creep); the per-tick {@code perTick}
 * (velocity-zero) kills IMPARTED movement a speed attribute doesn't touch -- knockback, jumps.
 * Together the mob is actually immobilized. It still turns and can melee in range -- movement
 * only, not the whole AI (that separates Rooted from Freeze).
 *
 * Shared, one instance -- like {@link AdapterContext}'s warn-once set -- because re-rooting a
 * mob must find its existing task and refresh it, not stack a second one. Keyed by entity
 * UUID; the map is concurrent because different entities apply from different region threads,
 * but a single entity's entry is only ever read/written on that entity's own thread.
 *
 * The lifecycle -- register + set speed 0 on apply, remove the modifier on expiry (base speed
 * restored EXACTLY -- a leaked 0-modifier is a permanently-frozen mob), never touch a removed
 * entity, refresh-not-stack -- is unit-tested against fakes; the same modifier-cleanup shape
 * Soaked hardened. Only the Bukkit binding (velocity-zero, the real attribute) is boot-witnessed.
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
     * {@code speed} carries the MOVEMENT_SPEED modifier (set to 0 on start, removed on expiry);
     * {@code perTick} is the per-frame velocity-zero (a counter in tests). Both run on the
     * target's own thread and never fire against a removed target.
     */
    public void apply(UUID id, RepeatingTaskTarget target, SpeedAttribute speed,
                      int durationTicks, Runnable perTick) {
        Active existing = active.get(id);
        if (existing != null && existing.task.isRunning()) {
            existing.remaining = Math.max(existing.remaining, durationTicks); // refresh; modifier already 0
            return;
        }
        Active a = new Active(durationTicks);
        speed.addSpeedModifier(0.0);             // MOVEMENT_SPEED x0: kill the self-propelled AI drive
        BooleanSupplier tick = () -> {
            if (a.remaining <= 0) {
                speed.removeSpeedModifier();     // expiry (alive): base speed restored exactly
                return false;                    // stop; onStop cleans the map entry
            }
            perTick.run();                       // velocity-zero: kill imparted movement (knockback, jumps)
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
