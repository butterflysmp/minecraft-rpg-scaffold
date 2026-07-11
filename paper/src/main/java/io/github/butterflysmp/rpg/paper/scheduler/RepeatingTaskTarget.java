package io.github.butterflysmp.rpg.paper.scheduler;

/**
 * The small seam a repeating task is written against, instead of an org.bukkit.Entity.
 *
 * This is deliberate. A repeating task's hardest property -- self-cancelling when the thing
 * it is attached to is removed, without throwing against a dead entity -- is the one that
 * hides until scale. Writing the task against this two-method abstraction (is it still here?
 * schedule my next tick on its thread) makes that cancellation logic unit-testable against a
 * fake, with no server. The Paper adapter ({@code EntityTaskTarget}) binds a real
 * LivingEntity to it; the fake in tests is a dozen lines.
 */
public interface RepeatingTaskTarget {

    /** False once the entity is removed or dead: the task must then stop and touch nothing. */
    boolean isActive();

    /** Defer {@code run} by {@code delayTicks} (>= 1), on this target's own thread. */
    void scheduleTick(int delayTicks, Runnable run);
}
