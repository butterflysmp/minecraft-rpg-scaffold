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

    /** Solar Flare (F8): shots land in ANY order; the third, whichever it is, ends the run; the remaining counts down. */
    @Test
    void landedCountsInAnyOrderAndTheLastEndsTheRun() {
        var runs = new PulseRuns();
        var run = runs.start(him, "SOLAR_FLARE", 3).run();
        runs.landed(run);
        runs.landed(run);
        assertTrue(runs.isCurrent(run));
        assertEquals(1, runs.of(him).map(r -> runs.end(him).orElseThrow().remaining()).orElseThrow());
        var again = runs.start(him, "SOLAR_FLARE", 3).run();
        for (int i = 0; i < 3; i++) runs.landed(again);
        assertEquals(0, runs.size(), "the third landing ends it");
        runs.landed(again);
        assertEquals(0, runs.size(), "a stale landing is ignored");
    }

    /** F13's two new shapes, in the server's yaw (east -90, yaw growing to his right) and pitch (positive down). */
    @Test
    void theShotAndBurstLines() {
        UUID caster = UUID.fromString("00000000-0000-0000-0000-00000000000c");
        double t = Math.tan(Math.toRadians(30));
        assertEquals("SOLAR_FLARE shot n=3 dir=-60.000 -0.000 tick=7 caster=" + caster,
                PulseLine.shot("SOLAR_FLARE", caster, 3, new Vec3(1, 0, t).normalize()).format("x", 7));
        assertEquals("SOLAR_FLARE shot n=1 dir=-120.000 -0.000 tick=7 caster=" + caster,
                PulseLine.shot("SOLAR_FLARE", caster, 1, new Vec3(1, 0, -t).normalize()).format("x", 7));
        assertEquals("SOLAR_FLARE shot n=2 dir=-90.000 45.000 tick=7 caster=" + caster,
                PulseLine.shot("SOLAR_FLARE", caster, 2, new Vec3(1, -1, 0)).format("x", 7), "pitch positive looking DOWN");
        assertEquals("SOLAR_FLARE burst n=2 cause=range origin=15.000 1.620 -3.500 tick=22 caster=" + caster,
                PulseLine.burst("SOLAR_FLARE", caster, 2, "range", new Vec3(15, 1.62, -3.5)).format("x", 22));
    }
}
