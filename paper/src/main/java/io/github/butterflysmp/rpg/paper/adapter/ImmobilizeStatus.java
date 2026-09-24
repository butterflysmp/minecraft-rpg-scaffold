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
        //
        // *** onStop RELEASES THE MODIFIER AS WELL AS FORGETTING THE ENTRY, AND THAT IS THE WHOLE
        // POINT OF THE 2026-09-24 RULE. *** The expiry arm above removes it on the normal path; a
        // body that THROWS never reaches that arm, and before RepeatingTask gained isolation it
        // never reached onStop either -- so the 0-modifier stayed on the entity forever. This
        // class's own javadoc names that outcome: "a leaked 0-modifier is a permanently-frozen mob."
        //
        // Double removal is safe and is the seam's stated contract -- SpeedAttribute.removeSpeedModifier
        // is "Remove our keyed modifier IF PRESENT" -- so the normal path calling it twice is a no-op
        // rather than something needing a hasSpeedModifier() guard here.
        //
        // ON-THREAD BY CONSTRUCTION, not by luck: nothing calls cancel() on an immobilize task
        // (measured 2026-09-24 -- the only cancel() callers are ScorchStatus.forget, which owns no
        // attribute, and the display/menu loops), so stop() is reachable only from inside step(),
        // which runs on the entity's own thread.
        //
        // *** THAT PROPERTY IS UNGUARDED, AND SAYING SO IS THE POINT. *** Adding a cancel() for
        // immobilize -- a dispel, a /rpg clear -- would move this release onto the CANCELLER's
        // thread, which on Folia is a cross-region attribute write. Nothing in the suite would go
        // red. This comment is a measurement of today's call graph, not an enforced invariant, and
        // it is written as one rather than pointing at a signature test that does not exist.
        //
        // *** AND THE RELEASE IS GATED ON isActive(), WHICH IS NOT BELT-AND-BRACES. ***
        // onStop runs on EVERY ending, and one of them is RepeatingTask's removed/dead arm, whose
        // own comment is "stop, touch nothing". An ungated release there reaches for the attribute
        // of a mob that no longer exists -- this project's cardinal hazard, and
        // ImmobilizeStatusTest.whenTheMobDiesCleanupTouchesNothingAndLeavesNoState caught exactly
        // that when this release was first written without the gate.
        //
        // It is also the right SEMANTICS rather than merely the safe ones: a removed entity took its
        // attributes with it, so there is nothing left to restore and the leak this rule exists to
        // prevent cannot happen on that path.
        a.task = RepeatingTask.start(target, 1, "immobilize", tick, () -> {
            if (target.isActive()) speed.removeSpeedModifier();
            active.remove(id, a);
        });
        active.put(id, a);
    }

    /**
     * True while {@code id} has a live immobilize on this instance. Two instances use it:
     * {@code ctx.immobilize()} (Rooted) and {@code ctx.freeze()} (Freeze), so the Freeze
     * listeners read {@code ctx.freeze().isImmobilized(id)} as their "is this mob frozen?" flag.
     */
    public boolean isImmobilized(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning();
    }

    /** Ticks remaining on {@code id}'s immobilize, or 0 if not immobilized. For tests. */
    public int remainingTicks(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning() ? a.remaining : 0;
    }
}
