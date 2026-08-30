package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The half of {@link ArmorConsistency} a unit test can reach: the ZERO case.
 *
 * <p>The mismatch check itself needs a live {@code ItemType} registry and therefore a running
 * server, so it is boot-witnessed -- the boot gate's row 1 is "zero mismatches reported". What CAN
 * be tested here is the branch that fires BEFORE any Bukkit call, and it is the branch most worth
 * pinning: a verifier handed nothing must say so.
 *
 * <p>That matters more than it looks. If {@code content/armor} ever loads empty, every other signal
 * still reads as healthy -- the Defense stat keeps working (it is sourced from vanilla, not from a
 * tag), no piece has a wrong tooltip because no piece exists, and a consistency check that silently
 * returned "0 mismatches" would be the strongest-looking evidence of all that nothing is wrong. The
 * failure mode this repo records twice, arriving through the one component whose whole job is to
 * notice.
 *
 * Each test names the mutation it forces red.
 */
class ArmorConsistencyTest {

    /** Captures what a Logger was actually told, so the assertion is on the message and not on a flag. */
    private static final class Capture extends Handler {
        final List<LogRecord> records = new ArrayList<>();
        @Override public void publish(LogRecord record) { records.add(record); }
        @Override public void flush() {}
        @Override public void close() {}

        List<String> warnings() {
            return records.stream()
                    .filter(r -> r.getLevel().intValue() >= Level.WARNING.intValue())
                    .map(LogRecord::getMessage)
                    .toList();
        }
    }

    private static Logger quietLogger(Capture capture) {
        Logger log = Logger.getLogger("ArmorConsistencyTest-" + System.nanoTime());
        log.setUseParentHandlers(false);   // do not spray the surefire output
        log.addHandler(capture);
        return log;
    }

    @Test
    void anEmptyRegistryIsWarnedAboutRatherThanReportedAsZeroMismatches() {
        Capture capture = new Capture();
        int mismatches = ArmorConsistency.check(new ArmorRegistry(), quietLogger(capture));

        assertEquals(0, mismatches, "nothing was checked, so nothing mismatched");
        assertFalse(capture.warnings().isEmpty(),
                "checking zero pieces must WARN -- a silent 0 is indistinguishable from a clean run");
        assertTrue(capture.warnings().stream().anyMatch(m -> m.contains("ZERO")),
                "the warning must say the count was zero, got: " + capture.warnings());
        // Mutation: return 0 early without logging when the registry is empty -> reddens. This is
        // the exact shape of the defect CLAUDE.md records twice: a discovery that finds nothing
        // reading as a discovery that passed.
    }

    @Test
    void theZeroWarningDoesNotClaimTheDefenseStatIsBroken() {
        // Precision in the message matters here, because the obvious reading is wrong. An empty
        // content/armor does NOT disable Defense: a plain vanilla chestplate still contributes its
        // full points, because DefenseModifierItems sources them from vanilla and never looks for
        // one of our tags. Someone debugging an empty roster must not go hunting in the stat path.
        Capture capture = new Capture();
        ArmorConsistency.check(new ArmorRegistry(), quietLogger(capture));
        String warning = capture.warnings().get(0);
        assertTrue(warning.contains("tooltip") || warning.contains("unverified"),
                "the warning must point at the tooltips it could not verify, got: " + warning);
        // Mutation: reword the warning to claim armor grants no defense -> reddens, and would send
        // the next reader into the wrong subsystem.
    }
}
