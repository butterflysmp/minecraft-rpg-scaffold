package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One Knell's beam clock (PLAN-dragon-mobs.md step R5). The waits come from an INJECTED draw, so every row fixes them.
 * The fixture's wait bounds (37 and 61) and telegraph (7, flashes at 0 and 5) are deliberately not the shipped 100,
 * 200, 12, 0, 9, so no two quantities a row reads collide; the shipped numbers are pinned in paper.
 */
class BeamCycleTest {

    private static final MobBeam BEAM = new MobBeam(37, 61, 7, List.of(0, 5), 40.0, 0.6, 1.0, 35.0);

    /** A draw that returns queued waits and records the bounds it was asked for. */
    private static final class Queued implements BeamCycle.Draw {
        final Deque<Integer> waits = new ArrayDeque<>();
        final List<String> asked = new ArrayList<>();

        Queued(Integer... w) {
            waits.addAll(List.of(w));
        }

        @Override public int ticks(int min, int max) {
            asked.add(min + ".." + max);
            return waits.removeFirst();
        }
    }

    /** Tick {@code n} times; return the 1-based tick numbers on which {@code event} came back. */
    private static List<Integer> ticksOf(BeamCycle c, int n, BeamCycle.Event event, Runnable onAttempt) {
        List<Integer> at = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            BeamCycle.Event e = c.tick();
            if (e == BeamCycle.Event.ATTEMPT) onAttempt.run();
            if (e == event) at.add(i);
        }
        return at;
    }

    @Test
    void theFirstWaitIsDrawnFromTheBeamsBoundsAtTheStart() {
        Queued draw = new Queued(40);
        BeamCycle c = new BeamCycle(BEAM, draw);
        assertEquals(List.of("37..61"), draw.asked, "the bounds are content's, both passed");
        assertEquals(40, c.waitLeft());
    }

    @Test
    void theAttemptComesExactlyTheDrawnNumberOfTicksAfterTheStart() {
        Queued draw = new Queued(40, 50);
        BeamCycle c = new BeamCycle(BEAM, draw);
        List<Integer> attempts = ticksOf(c, 40, BeamCycle.Event.ATTEMPT, c::noTarget);
        assertEquals(List.of(40), attempts);
    }

    @Test
    void anAttemptWithNoTargetWaitsAFreshDraw() {
        Queued draw = new Queued(40, 50, 37);
        BeamCycle c = new BeamCycle(BEAM, draw);
        List<Integer> attempts = ticksOf(c, 90, BeamCycle.Event.ATTEMPT, c::noTarget);
        assertEquals(List.of(40, 90), attempts, "40, then 50 more");
        assertEquals(List.of("37..61", "37..61", "37..61"), draw.asked, "the third drawn at the second attempt");
    }

    @Test
    void theTelegraphRootsThenFiresExactlyItsLengthAfterTheLock() {
        Queued draw = new Queued(40, 45);
        BeamCycle c = new BeamCycle(BEAM, draw);
        List<BeamCycle.Event> events = new ArrayList<>();
        for (int i = 1; i <= 40 + 7; i++) {
            BeamCycle.Event e = c.tick();
            if (e == BeamCycle.Event.ATTEMPT) {
                c.begin();
                assertTrue(c.inTelegraph());
                assertEquals(0, c.elapsed(), "the lock is the telegraph's tick 0");
            }
            if (i > 40) events.add(e);
        }
        // Ticks 41..46 are telegraph ticks 1..6; tick 47 is telegraph tick 7 = the telegraph's length: the fire.
        assertEquals(List.of(BeamCycle.Event.ROOT, BeamCycle.Event.ROOT, BeamCycle.Event.ROOT, BeamCycle.Event.ROOT,
                BeamCycle.Event.ROOT, BeamCycle.Event.ROOT, BeamCycle.Event.FIRE), events);
        assertFalse(c.inTelegraph());
        assertEquals(45, c.waitLeft(), "a fresh wait is drawn AT the fire");
        assertEquals(List.of("37..61", "37..61"), draw.asked);
    }

    @Test
    void afterAFireTheNextAttemptIsTheFreshWaitLater() {
        Queued draw = new Queued(40, 45);
        BeamCycle c = new BeamCycle(BEAM, draw);
        int fireTick = -1;
        List<Integer> attempts = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            BeamCycle.Event e = c.tick();
            if (e == BeamCycle.Event.ATTEMPT) {
                attempts.add(i);
                if (attempts.size() == 1) c.begin(); else { draw.waits.add(50); c.noTarget(); }
            }
            if (e == BeamCycle.Event.FIRE) fireTick = i;
        }
        assertEquals(47, fireTick);
        assertEquals(List.of(40, 47 + 45), attempts);
    }

    @Test
    void theWarningFlashesOnTheAuthoredTelegraphTicksOnly() {
        BeamCycle c = new BeamCycle(BEAM, new Queued(37, 50));
        for (int i = 1; i < 37; i++) c.tick();
        assertEquals(BeamCycle.Event.ATTEMPT, c.tick());
        c.begin();
        List<Integer> flashed = new ArrayList<>();
        if (c.flashing()) flashed.add(c.elapsed());
        while (c.inTelegraph()) {
            BeamCycle.Event e = c.tick();
            if (e == BeamCycle.Event.ROOT && c.flashing()) flashed.add(c.elapsed());
        }
        assertEquals(List.of(0, 5), flashed);
    }

    @Test
    void anUnresolvedAttemptIsAskedAgainNextTickRatherThanLost() {
        BeamCycle c = new BeamCycle(BEAM, new Queued(37, 50));
        for (int i = 1; i < 37; i++) c.tick();
        assertEquals(BeamCycle.Event.ATTEMPT, c.tick());
        assertEquals(BeamCycle.Event.ATTEMPT, c.tick(), "neither begin nor noTarget was called");
    }

    @Test
    void aDrawOutsideTheBoundsIsRefusedLoudly() {
        assertThrows(IllegalStateException.class, () -> new BeamCycle(BEAM, (min, max) -> 36));
        assertThrows(IllegalStateException.class, () -> new BeamCycle(BEAM, (min, max) -> 62));
    }

    @Test
    void beginOutsideAnAttemptIsRefused() {
        BeamCycle c = new BeamCycle(BEAM, new Queued(40));
        assertThrows(IllegalStateException.class, c::begin);
    }

    @Test
    void aForceMakesTheAttemptDueNowAndNeverInterruptsATelegraph() {
        BeamCycle c = new BeamCycle(BEAM, new Queued(60, 50));
        assertEquals(BeamCycle.Event.WAIT, c.tick());
        assertTrue(c.forceDue(), "waiting: the force takes");
        c.begin();   // the instrument's attempt, run in the same tick as the force
        assertTrue(c.inTelegraph());
        assertFalse(c.forceDue(), "a telegraph runs on");
        assertTrue(c.inTelegraph());
        assertEquals(0, c.elapsed());
    }

    @Test
    void theProductionDrawIsUniformOverBothEndsIncluded() {
        BeamCycle.Draw d = BeamCycle.uniform(new Random(20261117L));
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int i = 0; i < 20000; i++) {
            int t = d.ticks(100, 200);
            lo = Math.min(lo, t);
            hi = Math.max(hi, t);
        }
        assertEquals(100, lo, "the minimum is reachable");
        assertEquals(200, hi, "the maximum is reachable: inclusive");
    }

    @Test
    void theProductionDrawOfAFixedWaitIsThatWait() {
        assertEquals(150, BeamCycle.uniform(new Random(1)).ticks(150, 150));
    }
}
