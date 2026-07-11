package io.github.butterflysmp.rpg.paper.scheduler;

/**
 * A handle to a running repeating task. The one cancellation surface the one-shot
 * scheduler methods never needed -- a repeating task has to be stoppable.
 */
public interface TaskHandle {

    /** Stop the task. Idempotent: safe to call after it has already stopped. */
    void cancel();

    /** True until the task stops -- whether by its body signalling done, its target going
        away, or {@link #cancel()}. Used to tell a live root from a dead one before refreshing. */
    boolean isRunning();
}
