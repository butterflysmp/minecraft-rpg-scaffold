package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The server's one registry of pending pulses (Z5): one run per caster, ended with what was still to fire. */
class PulseRunsTest {

    private final UUID him = UUID.randomUUID();

    @Test
    void aRunEndedAfterTwoPulsesHandsBackTwoRemainingAndItsPendingHandle() {
        var runs = new PulseRuns();
        var run = runs.start(him, "SMOLDER", 4).run();
        runs.fired(run, 1);
        runs.fired(run, 2);
        runs.pending(run, 77);
        var ended = runs.end(him).orElseThrow();
        assertEquals(2, ended.remaining());
        assertEquals(77, ended.pendingHandle());
        assertFalse(runs.isCurrent(run));
        assertTrue(runs.end(him).isEmpty(), "ended once");
    }

    @Test
    void theLastPulseFinishesTheRunSoNothingIsLeftToEnd() {
        var runs = new PulseRuns();
        var run = runs.start(him, "SMOLDER", 4).run();
        for (int n = 1; n <= 4; n++) runs.fired(run, n);
        assertEquals(0, runs.size());
        assertTrue(runs.end(him).isEmpty());
    }

    @Test
    void aSecondStartReplacesTheFirstAndHandsItBack() {
        var runs = new PulseRuns();
        var first = runs.start(him, "SMOLDER", 4).run();
        runs.fired(first, 1);
        runs.pending(first, 5);
        var second = runs.start(him, "SMOLDER", 4);
        assertEquals(3, second.replaced().orElseThrow().remaining());
        assertEquals(5, second.replaced().orElseThrow().pendingHandle());
        assertFalse(runs.isCurrent(first));
        assertTrue(runs.isCurrent(second.run()));
        runs.fired(first, 2);   // a stale run's report is ignored
        assertEquals(4, runs.end(him).orElseThrow().remaining());
    }

    @Test
    void endAllEndsEveryCaster() {
        var runs = new PulseRuns();
        runs.start(him, "SMOLDER", 4);
        runs.start(UUID.randomUUID(), "SMOLDER", 4);
        assertEquals(2, runs.endAll().size());
        assertEquals(0, runs.size());
    }

    /** The four line shapes, pinned (Z10). */
    @Test
    void theFourLineShapes() {
        UUID target = new UUID(0, 9);
        UUID caster = new UUID(0, 1);
        assertEquals("SMOLDER cast caster=" + caster + " tick=100", PulseLine.cast("SMOLDER", caster).format("x", 100));
        assertEquals("SMOLDER pulse n=2 tick=160 origin=1.500 64.000 -2.250 caster=" + caster,
                PulseLine.pulse("SMOLDER", caster, 2, new Vec3(1.5, 64, -2.25)).format("x", 160));
        assertEquals("SMOLDER hit n=2 target=zombie " + target + " dist=3.000 tick=168 sent=160.000 caster=" + caster,
                PulseLine.hit("SMOLDER", caster, 2, target, 3.0, 160.0).format("zombie", 168));
        assertEquals("SMOLDER cancelled caster=" + caster + " remaining=2 cause=selftest tick=210",
                PulseLine.cancelled("SMOLDER", caster, 2, "selftest").format("x", 210));
    }
}
