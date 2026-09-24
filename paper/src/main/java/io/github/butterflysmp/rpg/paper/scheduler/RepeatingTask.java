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
 *
 * <h2>*** AND A THIRD: A BODY THAT THROWS STOPS THE LOOP CLEANLY RATHER THAN WEDGING IT ***</h2>
 *
 * <p><b>Until 2026-09-24 this class called the body with no isolation at all, and the consequence
 * was not "the tick is lost" -- it was that the loop could never run again.</b> Measured on
 * {@code FakeTickTarget} before the fix, with a body that throws:
 *
 * <pre>
 * throw escaped advance()  YES
 * onStop ran               false      &lt;- the owner's map entry is never cleaned
 * handle.isRunning()       TRUE       &lt;- and so a re-entry guard REFUSES to restart it
 * target.pending()         0          &lt;- nothing is scheduled: the loop is dead
 * </pre>
 *
 * <p>The throw escaped {@link #step} before {@link #arm}, so the loop never re-armed -- <b>and
 * before {@link #stop}, so {@code onStop} never ran either.</b> That second half is the damaging
 * one: the handle stays {@code isRunning()}, its owner's map keeps the entry, and every caller
 * that guards re-entry on exactly that (<i>"a second start cannot stack a second bar on one
 * player"</i>) then refuses to start a replacement. <b>One throw, and that player's loop is gone
 * for the session -- respawn included.</b>
 *
 * <p><b>THE FIX IS DELIBERATELY NOT "KEEP TICKING".</b> The body is reported through
 * {@link RepeatingTaskTarget#loopFailed} and the loop then stops through its normal path, so:
 *
 * <ul>
 *   <li>the failure is <b>LOUD</b> -- it goes to a real log with the loop's name, the target's
 *       identity and the stack trace, rather than being swallowed;
 *   <li>it is <b>ONCE</b>, by construction rather than by a dedup set: the loop stops, so it
 *       cannot report twice;
 *   <li>{@code onStop} <b>RUNS</b>, so the owner's state is released and the handle is freed;
 *   <li>and {@code start()} <b>WORKS AGAIN</b>, because {@code isRunning()} is now false.
 * </ul>
 *
 * <p><b>Re-arming through the failure instead was rejected</b>: a body that throws once usually
 * throws every period, and a loop that survives its own failure turns one report into a log entry
 * per tick forever. Stopping makes the failure visible exactly once and leaves the system in a
 * state a later {@code start()} can repair.
 *
 * <p><b>{@code Error} IS NOT CAUGHT, AND THAT IS THE ONE CASE THAT STILL WEDGES.</b> Only
 * {@link RuntimeException} is routed to {@code loopFailed}; an {@code OutOfMemoryError} or a
 * {@code StackOverflowError} propagates exactly as before. Catching {@code Error} to tidy up a
 * loop would be swallowing the one class of failure nobody should keep running through, and a
 * {@code BooleanSupplier} cannot throw a checked exception, so {@code RuntimeException} is every
 * throwable a body can legitimately produce.
 *
 * <h2>THE NINE CALLERS, AND WHAT {@code onStop} NOW GUARANTEES FOR EACH</h2>
 *
 * <p><b>The damage was never uniform, and the walk is written out because "every loop had the bug"
 * is true and useless.</b> What a wedged loop COST depended entirely on what its owner did with the
 * handle, and only five of the nine were wedged at all.
 *
 * <pre>
 * WEDGED -- the owner guards re-entry on isRunning(), so a crashed loop BLOCKED its replacement
 *
 *   ImmobilizeStatus   releases the speed modifier (gated on isActive), drops the entry
 *                      BEFORE: a permanently-frozen mob, and isImmobilized() true forever
 *   SoakedStatus       releases the speed modifier (gated on isActive), drops the entry
 *                      BEFORE: a permanently-slowed mob, and isSoaked() true forever
 *   ScorchStatus       drops the entry -- it owns no attribute, so nothing to release
 *                      BEFORE: the victim could never be scorched again; trackedVictims() inflated
 *   StatsBarSystem     drops the handle from its per-player map
 *                      BEFORE: that player's action bar never returned, respawn included
 *   HealthRegenSystem  drops the handle from its per-player map
 *                      BEFORE: that player stopped regenerating for the session
 *
 * NOT WEDGED -- no isRunning() guard, so a crashed loop simply stopped. The gain here is the REPORT
 *
 *   PlayerHealthSystem    onStop is empty and correctly so: it stores no handle. A crash stopped
 *                         the equipment reconcile until the next join/respawn, silently.
 *   MobNameplateManager   same shape: no handle, no wedge, silent until the viewer rejoined.
 *   AnvilMenu             armingTask is overwritten on the next start, so no wedge.
 *   GrindstoneMenu        same.
 * </pre>
 *
 * <p><b>THE THREE STATUSES HAD A THIRD LEAK THAT IS EASY TO MISS, AND IT IS THE PREDICATE.</b>
 * {@code isImmobilized}, {@code isSoaked} and {@code isScorched} all read {@code task.isRunning()},
 * so a wedged loop did not merely fail to expire -- <b>it reported the status as STILL ACTIVE for
 * the rest of the session.</b> The Freeze listeners read {@code isImmobilized} as their "is this mob
 * frozen?" flag, so a crashed root left a mob that the game believed was frozen and that nothing
 * would ever unfreeze.
 *
 * <p><b>An empty {@code onStop} is not a defect and is not upgraded to one here.</b> Four of the
 * nine own nothing and store no handle; giving them a release would be inventing state to release.
 * They are listed so the next reader can see that the emptiness was checked rather than skipped.
 */
public final class RepeatingTask implements TaskHandle {

    private final RepeatingTaskTarget target;
    private final int periodTicks;
    private final String name;
    private final BooleanSupplier body;
    private final Runnable onStop;

    private boolean running = true;
    private boolean stopped = false; // guards onStop against a double fire (e.g. cancel after done)

    private RepeatingTask(RepeatingTaskTarget target, int periodTicks, String name,
                          BooleanSupplier body, Runnable onStop) {
        this.target = target;
        this.periodTicks = periodTicks;
        this.name = name;
        this.body = body;
        this.onStop = onStop;
    }

    /**
     * Begin ticking {@code body} every {@code periodTicks} on {@code target}'s thread. Runs
     * {@code onStop} exactly once when the task stops -- for ANY reason (body done, target
     * inactive, {@link #cancel()}, or <b>the body throwing</b>). The first tick is scheduled, not
     * run inline.
     *
     * <p><b>{@code onStop} MUST RELEASE ANYTHING THE LOOP OWNS, not merely forget it.</b> It is the
     * one path every ending shares, and since a throw now reaches it, a release written only into
     * the body's expiry arm is a release a crashed loop skips. See {@code ImmobilizeStatus}, whose
     * speed modifier is removed in BOTH places for exactly this reason.
     *
     * @param name what this loop is, for the log line a failure produces. A short kebab-case tag --
     *             {@code "stats-bar"}, {@code "immobilize"} -- not a sentence.
     */
    public static RepeatingTask start(RepeatingTaskTarget target, int periodTicks, String name,
                                      BooleanSupplier body, Runnable onStop) {
        RepeatingTask task = new RepeatingTask(target, periodTicks, name, body, onStop);
        task.arm();
        return task;
    }

    private void arm() {
        target.scheduleTick(periodTicks, this::step);
    }

    private void step() {
        if (!running) return;                          // cancelled between arming and firing
        if (!target.isActive()) { stop(); return; }    // removed/dead -- stop, touch nothing

        boolean again;
        try {
            again = body.getAsBoolean();
        } catch (RuntimeException failure) {
            // REPORT, THEN STOP THROUGH THE NORMAL PATH. The order matters: reporting first means
            // the log names the loop even if onStop itself goes wrong, and stopping through stop()
            // rather than by simply not re-arming is what runs onStop -- which releases whatever the
            // loop owned and frees the handle so start() can replace it.
            //
            // NOT RETHROWN. A rethrow would reach the platform's scheduler, which is where the old
            // behaviour ended up, and it buys nothing this line has not already done while costing
            // the caller's frame. Error is deliberately outside this catch; see the class javadoc.
            target.loopFailed(name, failure);
            stop();
            return;
        }

        if (!again) { stop(); return; }                // the body's own "done"
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
