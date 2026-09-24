package io.github.butterflysmp.rpg.paper.scheduler;

/**
 * The small seam a repeating task is written against, instead of an org.bukkit.Entity.
 *
 * This is deliberate. A repeating task's hardest property -- self-cancelling when the thing
 * it is attached to is removed, without throwing against a dead entity -- is the one that
 * hides until scale. Writing the task against this abstraction (is it still here? schedule my
 * next tick on its thread; report a body that threw) makes that cancellation logic
 * unit-testable against a fake, with no server. The Paper adapter ({@code EntityTaskTarget})
 * binds a real LivingEntity to it; the fake in tests is a few dozen lines.
 */
public interface RepeatingTaskTarget {

    /** False once the entity is removed or dead: the task must then stop and touch nothing. */
    boolean isActive();

    /** Defer {@code run} by {@code delayTicks} (>= 1), on this target's own thread. */
    void scheduleTick(int delayTicks, Runnable run);

    /**
     * A loop body threw. Report it; {@link RepeatingTask} stops the loop immediately afterwards.
     *
     * <h2>WHY THE TARGET CARRIES THIS AND NOT {@code start()}</h2>
     *
     * <p><b>The target is the only thing in the seam that knows WHO the loop was for.</b> A report
     * naming the loop and not the player is a report you cannot act on when one player's bar dies
     * and forty others are fine. Putting it here keeps the identity and the reporting together, and
     * keeps {@link RepeatingTask} free of both Bukkit and a logger.
     *
     * <p><b>IT IS ABSTRACT RATHER THAN A {@code default} THAT SWALLOWS.</b> A default would let a new
     * implementor stay silent and still compile, which is the blind-fixture shape this project
     * records elsewhere -- and silence is the exact failure this whole method exists to end. There
     * are two implementors and a compile error is cheaper than a scanner.
     *
     * <p><b>"ONCE" IS GUARANTEED BY THE STOP, NOT BY A DEDUP SET.</b> {@code RepeatingTask} calls
     * this and then stops for good, so a given loop instance can reach it at most one time. An
     * implementation does NOT need {@code warnOnce} semantics and must not rely on them for
     * correctness -- a second loop for the same player is a second, genuinely new failure.
     *
     * @param loopName  what the loop is, for a human reading a log -- e.g. {@code "stats-bar"}
     * @param cause     what the body threw. {@code Error} is NOT routed here; see {@link RepeatingTask}
     */
    void loopFailed(String loopName, RuntimeException cause);
}
