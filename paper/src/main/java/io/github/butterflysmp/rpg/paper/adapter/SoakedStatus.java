package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Soaked: a stacking, multiplicative movement-slow that cleans up exactly.
 *
 * Reuses the per-tick primitive ({@link RepeatingTask}) for the duration countdown -- Soaked
 * is the primitive's second configuration after Rooted. What's new is removable Bukkit state:
 * a movement-speed modifier that must be set on a stack change, held while alive, and
 * **fully removed at expiry** so base speed is exactly restored. A leaked modifier is a mob
 * permanently slow after Soaked ended; that -- not "it slows" -- is the acceptance.
 *
 * The one modifier is set only on a stack change (via {@link #setSlow}, which replaces), and
 * NEVER in the per-tick body (countdown only). So there is always exactly one keyed modifier,
 * not N. Expiry removes it while the mob is alive. On death the primitive stops before the
 * body runs, so the modifier is never touched on a removed entity -- it dies with the entity,
 * while onStop still clears our per-target state. Shared one instance (like
 * {@link ImmobilizeStatus}); the map is concurrent because different entities apply from
 * different region threads, but a single entity's entry is only touched on its own thread.
 */
public final class SoakedStatus {

    /** Tuning. Named, not magic: lift to soaked.yml when the curve is tuned in play. */
    private static final double DECAY_PER_STACK = 0.9;
    private static final double SPEED_FLOOR = 0.6;

    private static double factor(int stacks) {
        return Math.max(SPEED_FLOOR, Math.pow(DECAY_PER_STACK, stacks));
    }

    /** A live soak: its handle, its stack count, and the ticks it has left (owner-thread only). */
    private static final class Active {
        RepeatingTask task;
        int stacks;
        int remaining;
        Active(int stacks, int remaining) { this.stacks = stacks; this.remaining = remaining; }
    }

    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    /**
     * Apply a soak stack to {@code id}, or add a stack + refresh the whole timer if already
     * soaked (all-at-once decay). {@code speed} is the movement-speed seam (real attribute in
     * production, a fake that reads back in tests).
     */
    public void apply(UUID id, RepeatingTaskTarget target, SpeedAttribute speed, int durationTicks) {
        Active a = active.get(id);
        if (a != null && a.task.isRunning()) {
            a.stacks++;
            a.remaining = durationTicks;       // refresh the whole timer, not a per-stack clock
            setSlow(speed, a.stacks);
            return;
        }
        Active na = new Active(1, durationTicks);
        setSlow(speed, na.stacks);             // set on stack change, NOT every tick
        BooleanSupplier tick = () -> {
            if (na.remaining <= 0) {
                speed.removeSoakModifier();    // expiry, on the entity's own thread: base restored
                return false;
            }
            na.remaining--;
            return true;                       // countdown only -- the one modifier persists between ticks
        };
        na.task = RepeatingTask.start(target, 1, tick, () -> active.remove(id, na));
        active.put(id, na);
    }

    /** Replace the one keyed modifier so a stack change never leaves two -- the guard bug 4 tests. */
    private static void setSlow(SpeedAttribute speed, int stacks) {
        if (speed.hasSoakModifier()) speed.removeSoakModifier();
        speed.addSoakModifier(factor(stacks));
    }

    /** True while {@code id} has a live soak. For tests and future multi-status composition. */
    public boolean isSoaked(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning();
    }

    /** Current stack count on {@code id}, or 0 if not soaked. For tests. */
    public int stacks(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning() ? a.stacks : 0;
    }
}
