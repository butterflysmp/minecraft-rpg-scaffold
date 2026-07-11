package io.github.butterflysmp.rpg.paper.scheduler;

import java.util.function.BooleanSupplier;

/**
 * A repeating task with a clean lifecycle, written against {@link RepeatingTaskTarget}
 * rather than a Bukkit Entity so its cancellation logic is testable without a server.
 *
 * The loop re-arms itself through the target's own {@link RepeatingTaskTarget#scheduleTick}
 * every period, exactly as an entity scheduler would -- so a fake target drives it
 * identically to Paper. The two cancel decisions live HERE, in production the fake
 * exercises, not in the fake and not in the Paper binding:
 *
 *  - the target reports inactive (removed/dead) -> stop, and do NOT run the body, so nothing
 *    touches a dead entity;
 *  - the body returns false (its own "done", e.g. a duration ran out) -> stop.
 *
 * Deleting either guard leaves the loop re-arming forever; the lifecycle tests catch it
 * because the fake still shows a task scheduled.
 */
public final class RepeatingTask implements TaskHandle {

    private final RepeatingTaskTarget target;
    private final int periodTicks;
    private final BooleanSupplier body;
    private final Runnable onStop;

    private boolean running = true;
    private boolean stopped = false; // guards onStop against a double fire (e.g. cancel after done)

    private RepeatingTask(RepeatingTaskTarget target, int periodTicks,
                          BooleanSupplier body, Runnable onStop) {
        this.target = target;
        this.periodTicks = periodTicks;
        this.body = body;
        this.onStop = onStop;
    }

    /**
     * Begin ticking {@code body} every {@code periodTicks} on {@code target}'s thread. Runs
     * {@code onStop} exactly once when the task stops -- for ANY reason (body done, target
     * inactive, or {@link #cancel()}). The first tick is scheduled, not run inline.
     */
    public static RepeatingTask start(RepeatingTaskTarget target, int periodTicks,
                                      BooleanSupplier body, Runnable onStop) {
        RepeatingTask task = new RepeatingTask(target, periodTicks, body, onStop);
        task.arm();
        return task;
    }

    private void arm() {
        target.scheduleTick(periodTicks, this::step);
    }

    private void step() {
        if (!running) return;                          // cancelled between arming and firing
        if (!target.isActive()) { stop(); return; }    // removed/dead -- stop, touch nothing
        if (!body.getAsBoolean()) { stop(); return; }  // the body's own "done"
        arm();                                         // re-arm for the next period
    }

    private void stop() {
        if (stopped) return;
        stopped = true;
        running = false;
        onStop.run();
    }

    @Override public void cancel() { stop(); }

    @Override public boolean isRunning() { return running; }
}
