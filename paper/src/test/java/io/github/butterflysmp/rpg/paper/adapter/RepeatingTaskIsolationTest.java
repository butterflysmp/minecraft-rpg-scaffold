package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A LOOP BODY THAT THROWS STOPS THE LOOP CLEANLY. IT DOES NOT WEDGE IT.
 *
 * <h2>The defect, MEASURED before any of this was written</h2>
 *
 * <p>{@code RepeatingTask.step} called the body with no isolation. A probe on
 * {@link FakeTickTarget} with a throwing body read:
 *
 * <pre>
 * throw escaped advance()  YES
 * onStop ran               false
 * handle.isRunning()       TRUE
 * target.pending()         0
 * </pre>
 *
 * <p><b>The second and third lines are the damage, not the first.</b> The throw escaped
 * {@code step} before {@code arm()}, so the loop died -- and before {@code stop()}, so
 * {@code onStop} never ran. The handle therefore stayed {@code isRunning()}, its owner's map kept
 * the entry, and every caller that guards re-entry on exactly that refused to start a replacement.
 * <b>One throw and that player's loop was gone for the session, respawn included.</b>
 *
 * <h2>Why these rows and not one</h2>
 *
 * <p>The four properties fail independently and a single row asserting "it did not blow up" would
 * pass on a fix that swallowed the throw and kept ticking -- which is the shape that was explicitly
 * rejected. Each row below names the property and the mutation that reddens it.
 *
 * <p><b>The status rows are separate because the RELEASE is a separate rule.</b> Isolation stops the
 * loop; it does not by itself restore a speed modifier the loop had applied. That only happens
 * because {@code ImmobilizeStatus} and {@code SoakedStatus} now release in {@code onStop} as well as
 * at expiry, and nothing in {@code RepeatingTask} can enforce it.
 */
class RepeatingTaskIsolationTest {

    private static final RuntimeException BOOM = new IllegalStateException("boom");

    // ---- the isolation itself -----------------------------------------------------------------

    @Test
    void aThrowDoesNotEscapeIntoTheScheduler() {
        var target = new FakeTickTarget();
        RepeatingTask.start(target, 1, "probe", () -> { throw BOOM; }, () -> {});

        assertDoesNotThrow(() -> target.advance(1),
                "the body's throw reached the scheduler. On Paper that is an entity-task frame, and "
                        + "whatever it does with the throwable, the loop is already dead by then");
        // Mutation: delete the try/catch in step() -> the throw propagates -> reddens.
    }

    @Test
    void aThrownBodyStopsTheLoopAndRunsOnStop() {
        var target = new FakeTickTarget();
        boolean[] stopped = {false};
        TaskHandle handle = RepeatingTask.start(
                target, 1, "probe", () -> { throw BOOM; }, () -> stopped[0] = true);

        target.advance(1);

        assertTrue(stopped[0],
                "onStop did NOT run. This is the half that wedges: onStop is where a loop releases "
                        + "what it owns and where its owner drops the handle");
        assertFalse(handle.isRunning(),
                "the handle still reports running, so every re-entry guard in the project will "
                        + "REFUSE to start a replacement loop for this player");
        assertEquals(0, target.pending(),
                "a stopped loop must leave nothing scheduled");
        // Mutation: replace stop() with a bare `return` in the catch -> onStop never runs -> reddens.
    }

    @Test
    void theFailureIsReportedOnceWithTheLoopsNameAndTheOriginalCause() {
        var target = new FakeTickTarget();
        RepeatingTask.start(target, 1, "stats-bar", () -> { throw BOOM; }, () -> {});

        target.advance(50);   // long past the period: a loop that kept ticking would report again

        assertEquals(1, target.failures.size(),
                "exactly one report. ONCE is guaranteed by the STOP, not by a dedup set -- so more "
                        + "than one here means the loop survived its own failure and is re-arming");
        assertEquals("stats-bar", target.failures.get(0),
                "the report must name the LOOP, or a log line cannot say which of the nine died");
        assertSame(BOOM, target.failureCauses.get(0),
                "the ORIGINAL throwable must reach the reporter -- a wrapped or dropped cause loses "
                        + "the stack trace, which is the only thing that says where in the body it broke");
        // Mutation: report after stop(), or drop the cause -> reddens.
    }

    @Test
    void aReplacementLoopCanBeStartedAfterAFailure() {
        // THE ROW THE WHOLE FIX EXISTS FOR. The owner's guard is `if (existing != null &&
        // existing.isRunning()) return;` -- so "can it restart" IS "did isRunning() go false".
        var target = new FakeTickTarget();
        TaskHandle dead = RepeatingTask.start(target, 1, "probe", () -> { throw BOOM; }, () -> {});
        target.advance(1);

        assertFalse(dead.isRunning(), "precondition: the crashed loop reports not-running");

        int[] ticks = {0};
        TaskHandle replacement = RepeatingTask.start(
                target, 1, "probe", () -> { ticks[0]++; return true; }, () -> {});
        target.advance(3);

        assertEquals(3, ticks[0], "the replacement loop ticks normally");
        assertTrue(replacement.isRunning());
    }

    @Test
    void aBodyThatThrowsOnItsSecondTickStillTicksItsFirst() {
        // The failure must not retroactively undo work the loop already did, and the loop must run
        // normally until the throw -- a fix that stopped pre-emptively would pass every row above.
        var target = new FakeTickTarget();
        int[] ticks = {0};
        RepeatingTask.start(target, 1, "probe", () -> {
            ticks[0]++;
            if (ticks[0] == 2) throw BOOM;
            return true;
        }, () -> {});

        target.advance(10);

        assertEquals(2, ticks[0], "one normal tick, then the throwing one, then nothing");
        assertEquals(1, target.failures.size());
    }

    // ---- the RELEASE rule, one row per status that owns something -----------------------------

    @Test
    void aCrashedImmobilizeReleasesItsSpeedModifier() {
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        var status = new ImmobilizeStatus();
        UUID id = UUID.randomUUID();

        // perTick is the body's one outward side effect -- the velocity-zero call, which touches a
        // live entity -- so throwing from it is both the realistic failure and the injectable one.
        status.apply(id, target, speed, 100, () -> { throw BOOM; });

        assertTrue(speed.hasSpeedModifier(), "precondition: applying rooted sets the 0-modifier");

        target.advance(1);

        assertFalse(speed.hasSpeedModifier(),
                "THE MOB IS PERMANENTLY FROZEN. ImmobilizeStatus' own javadoc names this outcome: "
                        + "'a leaked 0-modifier is a permanently-frozen mob'. The expiry arm removes "
                        + "it on the normal path, a throw never reaches that arm, so onStop has to");
        assertEquals(0, speed.modifierCount(), "and exactly zero of them, not a replaced one");
        assertFalse(status.isImmobilized(id), "the entry is gone too, so it can be re-applied");
        // Mutation: drop speed.removeSpeedModifier() from the onStop lambda -> reddens.
    }

    @Test
    void aCrashedSoakReleasesItsSpeedModifier() {
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        var status = new SoakedStatus();
        UUID id = UUID.randomUUID();

        // SoakedStatus' body only counts down and then calls removeSpeedModifier at expiry, so that
        // call IS the only injectable failure -- see FakeSpeedAttribute.failNextRemove. Duration 1:
        // tick 1 decrements to 0, tick 2 takes the expiry arm and throws out of it.
        status.apply(id, target, speed, 1);
        assertTrue(speed.hasSpeedModifier(), "precondition: a soak stack sets the slow modifier");

        speed.failNextRemove = true;
        target.advance(2);

        assertFalse(speed.hasSpeedModifier(),
                "the soak's expiry arm threw, so it never removed the modifier -- and without a "
                        + "release in onStop the entity stays slowed for good. Quieter than rooted's "
                        + "frozen mob, which is exactly why it needed writing down rather than noticing");
        assertFalse(status.isSoaked(id), "and the entry is gone");
        assertEquals(1, target.failures.size(), "the failure was reported once");
        // Mutation: drop speed.removeSpeedModifier() from the onStop lambda -> reddens.
    }

    @Test
    void aCrashedScorchFreesTheVictimToBeScorchedAgain() {
        var target = new FakeTickTarget();
        var sink = new FakeScorchSink(target, 100);
        var status = new ScorchStatus();
        UUID id = UUID.randomUUID();

        // ScorchStatus owns NO attribute, so its onStop is UNCHANGED by the release rule -- the map
        // entry is the only state and onStop already cleared it. What leaked was the entry surviving
        // a crash with isRunning() still TRUE: apply() takes its refresh branch on exactly that, so
        // the victim could never be scorched again and trackedVictims() counted them forever.
        // THIS ROW IS THEREFORE ABOUT THE ISOLATION ALONE, not about the release rule.
        sink.throwOnDeal = true;
        status.apply(id, target, sink, 1, 999, UUID.randomUUID(), 100, "fire", 0);
        assertEquals(1, status.trackedVictims(), "precondition: the victim is scorched");

        target.advance(Scorch.PERIOD_TICKS);

        assertEquals(0, status.trackedVictims(),
                "the crashed scorch still holds its victim: trackedVictims() stays inflated for the "
                        + "session and apply() refreshes a dead timer instead of starting a live one");

        sink.throwOnDeal = false;
        status.apply(id, target, sink, 1, 999, UUID.randomUUID(), 100, "fire", 0);
        assertEquals(1, status.trackedVictims(), "and a fresh scorch can be applied");
        assertTrue(status.isScorched(id));
    }
}
