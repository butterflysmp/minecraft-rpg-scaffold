package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RUN PUTS HIS HEALTH BACK (2026-11-22, the seat's brief, PR 1). {@code GATE-knell-beam.md}'s setup read
 * {@code SELF customHP=160.000/350.000} before the level staging and {@code 100.000/100.000} after; the restore put his
 * XP back (max 350 again, a rise is headroom) and left him at 100. The snapshot now holds his health and writes it back
 * {@link SelfTestSnapshot#HEALTH_RESTORE_DELAY_TICKS} after the restore. The write itself needs a live server (the gate
 * reads its {@code HEALTH} line); the decision is pure and is pinned here.
 *
 * <p>Every fixture keeps saved, current and max distinct, so a transposition of any two cannot pass.
 */
class SelfTestHealthRestoreTest {

    /** The KB setup's own figures: saved 160, at 100 of a restored 350. Writes 160. Forces red: writing {@code max}. */
    @Test
    void itPutsBackWhatHeHadWhenTheMaxIsBack() {
        assertEquals(160.0, SelfTestSnapshot.healthToRestore(160.0, 100.0, 350.0));
    }

    /** The max not yet back (still 100): it writes at most the max, never above it. Forces red: dropping the min. */
    @Test
    void itNeverWritesAboveTheMaxItSees() {
        assertEquals(120.0, SelfTestSnapshot.healthToRestore(300.0, 90.0, 120.0));
    }

    /** Regeneration already put him above what he had: no write, so the restore never deals him damage. */
    @Test
    void itNeverLowersHim() {
        assertTrue(Double.isNaN(SelfTestSnapshot.healthToRestore(160.0, 170.0, 350.0)));
        assertTrue(Double.isNaN(SelfTestSnapshot.healthToRestore(160.0, 160.0, 350.0)), "equal: nothing to write");
    }

    /** Untracked at the start (NaN saved): nothing is written. */
    @Test
    void nothingSavedNothingWritten() {
        assertTrue(Double.isNaN(SelfTestSnapshot.healthToRestore(Double.NaN, 50.0, 350.0)));
    }

    /**
     * The delay outlasts ONE reconcile pass (5 ticks, {@code PlayerHealthSystem.RECONCILE_PERIOD_TICKS}), read from
     * the source so the two cannot drift apart. Forces red: a delay of 5 or less.
     */
    @Test
    void theDelayOutlastsAReconcilePass() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/health/PlayerHealthSystem.java"), StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("RECONCILE_PERIOD_TICKS = (\\d+);").matcher(src);
        assertTrue(m.find(), "the reconcile period is not found: the comparison is blind");
        int period = Integer.parseInt(m.group(1));
        assertEquals(5, period, "CONTROL: the period the delay was reasoned from");
        assertTrue(SelfTestSnapshot.HEALTH_RESTORE_DELAY_TICKS > period,
                "a write inside one pass can land before the max rises, and is clamped");
    }

    /** The run's end calls it, after the restore. Bounded at {@code end(}'s body. */
    @Test
    void theRunsEndSchedulesIt() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java"), StandardCharsets.UTF_8)
                .replace("\r", "");
        int start = src.indexOf("private void end(");
        int end = src.indexOf("\n    private ", start + 1);
        assertTrue(start > 0 && end > start, "end( is not found: the scan is blind");
        String body = src.substring(start, end);
        int restore = body.indexOf("snapshot.restore(");
        int health = body.indexOf("snapshot.restoreHealthLater(");
        assertTrue(restore > 0, "CONTROL: the bound holds the restore");
        assertTrue(health > restore, "the health write is scheduled after the XP restore");
    }
}
