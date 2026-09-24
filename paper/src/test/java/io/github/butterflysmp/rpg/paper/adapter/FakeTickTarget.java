package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The test double the whole lifecycle suite stands on: a {@link RepeatingTaskTarget} that
 * is a CLOCK, not a run-it-inline stub.
 *
 * A scheduled tick lands {@code delayTicks} in the FUTURE and fires only when the clock is
 * advanced past it; a task that re-arms itself fires once per period across ticks, exactly
 * like Paper's entity scheduler. {@link #pending()} counts ticks the clock still holds, so a
 * test can see whether a repeating task is still scheduled or has stopped. Its own fidelity
 * -- that it defers instead of running inline, and that pending() tracks tasks across ticks
 * and cancellation -- is proved in RepeatingTaskFidelityTest before any lifecycle test trusts
 * it. This is the FakeWorld-discarded-delayTicks lesson: a fake that lies about time makes
 * every green test meaningless.
 */
final class FakeTickTarget implements RepeatingTaskTarget {

    /** Flip to false to simulate the entity being removed/dead. */
    boolean active = true;

    private record Scheduled(long dueTick, long seq, Runnable run) {}

    private final PriorityQueue<Scheduled> queue = new PriorityQueue<>(
            Comparator.comparingLong(Scheduled::dueTick).thenComparingLong(Scheduled::seq));

    private long now = 0L;
    private long seq = 0L;

    /**
     * Every loop failure this target was told about, newest last.
     *
     * <p><b>RECORDED RATHER THAN PRINTED, because the reporting is the thing under test.</b> A fake
     * that swallowed {@code loopFailed} would make "the loop reported before it stopped" untestable,
     * and a fake that printed it would make a passing suite noisy enough that nobody reads it.
     */
    final List<String> failures = new ArrayList<>();

    /** The causes, kept beside {@link #failures} so a row can assert the ORIGINAL throwable survives. */
    final List<RuntimeException> failureCauses = new ArrayList<>();

    @Override public boolean isActive() { return active; }

    @Override public void loopFailed(String loopName, RuntimeException cause) {
        failures.add(loopName);
        failureCauses.add(cause);
    }

    @Override public void scheduleTick(int delayTicks, Runnable run) {
        if (delayTicks < 1) {
            throw new IllegalArgumentException(
                    "scheduleTick requires delayTicks >= 1, got " + delayTicks);
        }
        queue.add(new Scheduled(now + delayTicks, seq++, run));
    }

    /** Run the clock forward, firing every tick that comes due, in due order. */
    void advance(int ticks) {
        long target = now + ticks;
        while (!queue.isEmpty() && queue.peek().dueTick() <= target) {
            Scheduled next = queue.poll();
            now = next.dueTick(); // a re-armed tick lands at now+period, not now
            next.run().run();
        }
        now = target;
    }

    /** How many ticks the clock still holds -- i.e. whether a repeating task is still scheduled. */
    int pending() { return queue.size(); }

    long now() { return now; }
}
