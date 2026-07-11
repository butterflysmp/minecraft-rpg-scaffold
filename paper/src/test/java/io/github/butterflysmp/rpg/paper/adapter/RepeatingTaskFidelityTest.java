package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 0: prove the instrument before trusting it. The lifecycle tests are only meaningful
 * if {@link FakeTickTarget} genuinely models a repeating task across ticks and a
 * cancellation, and if {@link RepeatingTask} genuinely re-arms and stops. A fake that ran
 * ticks inline and forgot them would show nothing "still scheduled", and the skip-cancel
 * mutations would redden nothing -- the whole suite green against a lie about time.
 */
class RepeatingTaskFidelityTest {

    @Test
    void aScheduledTickDefersInsteadOfRunningInline() {
        var target = new FakeTickTarget();
        int[] ran = {0};

        target.scheduleTick(1, () -> ran[0]++);

        target.advance(0);
        assertEquals(0, ran[0], "a tick scheduled for +1 must NOT run on the current frame");
        target.advance(1);
        assertEquals(1, ran[0], "it runs one tick later");
    }

    @Test
    void aRepeatingTaskFiresOncePerPeriodAndStaysScheduled() {
        var target = new FakeTickTarget();
        int[] ticks = {0};
        RepeatingTask.start(target, 1, () -> { ticks[0]++; return true; }, () -> {});

        assertEquals(1, target.pending(), "the first tick is armed, not run, by start()");
        assertEquals(0, ticks[0], "nothing has fired yet");

        target.advance(1);
        assertEquals(1, ticks[0], "one fire on the first tick");
        assertEquals(1, target.pending(), "and it re-armed itself for the next");

        target.advance(1);
        assertEquals(2, ticks[0], "one fire per tick across ticks");
        assertEquals(1, target.pending(), "still scheduled");
    }

    @Test
    void cancelStopsFutureFiresAndDrainsTheSchedule() {
        var target = new FakeTickTarget();
        int[] ticks = {0};
        boolean[] stopped = {false};
        TaskHandle handle = RepeatingTask.start(
                target, 1, () -> { ticks[0]++; return true; }, () -> stopped[0] = true);

        target.advance(1);
        assertEquals(1, ticks[0]);

        handle.cancel();
        assertTrue(stopped[0], "cancel runs onStop exactly once");
        assertFalse(handle.isRunning(), "and marks the task not-running");

        target.advance(5);
        assertEquals(1, ticks[0], "no fires after cancel");
        assertEquals(0, target.pending(), "the armed tick passed and was not re-armed: nothing left");
    }

    @Test
    void aBodyReturningFalseStopsTheTaskOnItsOwn() {
        var target = new FakeTickTarget();
        int[] ticks = {0};
        boolean[] stopped = {false};
        // Fire twice, then signal done.
        RepeatingTask.start(target, 1, () -> { ticks[0]++; return ticks[0] < 2; }, () -> stopped[0] = true);

        target.advance(10);
        assertEquals(2, ticks[0], "stops the tick it returns false");
        assertTrue(stopped[0], "onStop fired");
        assertEquals(0, target.pending(), "nothing left scheduled");
    }
}
