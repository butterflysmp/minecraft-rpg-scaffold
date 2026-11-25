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
 * <b>2026-11-25 (A2):</b> the check is now COVERAGE, decided in core ({@code ArmorCoverageTest}); this file also
 * pins the paper half with the server's armor STAGED. The account below is kept as written for slice 1, when the
 * check compared authored Defense with vanilla's.
 *
 * <p>The half of {@link ArmorConsistency} a unit test can reach: the ZERO case.
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
    void theZeroWarningSaysWhatAnEmptyRosterCosts() {
        // REWRITTEN 2026-11-25 (A1). This row used to pin that the warning named the TOOLTIPS, because
        // Defense was then sourced from vanilla and an empty roster cost nothing else. Under A1 an empty
        // roster costs the authored Defense itself: every worn piece falls back to vanilla's points (the
        // scan's CC'S CALL) and no set can exist. The warning must say THAT, or someone debugging a
        // full-diamond player reading 20 instead of 72 goes hunting in the stat path.
        Capture capture = new Capture();
        ArmorConsistency.check(new ArmorRegistry(), quietLogger(capture));
        String warning = capture.warnings().get(0);
        assertTrue(warning.contains("vanilla's armor points") && warning.contains("authored Defense"),
                "the warning must name the fallback, got: " + warning);
        // Mutation: reword the warning back to the tooltip story -> reddens.
    }

    @Test
    void aStagedServerArmorThatTheContentDoesNotCoverIsWarnedPerProblem() {
        // The live server's half needs an item registry; the decision with that half STAGED does not.
        ArmorRegistry content = new ArmorRegistry();
        content.register(new io.github.butterflysmp.rpg.core.weapon.ArmorDefinition("copper_helmet", "Copper Helmet",
                io.github.butterflysmp.rpg.core.weapon.Rarity.COMMON, "copper_helmet",
                io.github.butterflysmp.rpg.core.weapon.ArmorSlot.HEAD, 5, List.of()));
        java.util.Map<String, io.github.butterflysmp.rpg.core.weapon.ArmorSlot> server = new java.util.LinkedHashMap<>();
        server.put("copper_helmet", io.github.butterflysmp.rpg.core.weapon.ArmorSlot.HEAD);
        server.put("copper_boots", io.github.butterflysmp.rpg.core.weapon.ArmorSlot.FEET);
        server.put("turtle_helmet", io.github.butterflysmp.rpg.core.weapon.ArmorSlot.HEAD);

        Capture capture = new Capture();
        assertEquals(1, ArmorConsistency.check(content, server, quietLogger(capture)),
                "copper_boots uncovered; the turtle helmet excluded by name (C2)");
        assertTrue(capture.warnings().stream().anyMatch(m -> m.contains("copper_boots")), capture.warnings().toString());
        assertTrue(capture.warnings().stream().noneMatch(m -> m.contains("turtle_helmet")), capture.warnings().toString());
        // POSITIVE CONTROL: cover the boots and the same staging reports nothing.
        content.register(new io.github.butterflysmp.rpg.core.weapon.ArmorDefinition("copper_boots", "Copper Boots",
                io.github.butterflysmp.rpg.core.weapon.Rarity.COMMON, "copper_boots",
                io.github.butterflysmp.rpg.core.weapon.ArmorSlot.FEET, 4, List.of()));
        assertEquals(0, ArmorConsistency.check(content, server, quietLogger(new Capture())));
        // Mutation: empty EXCLUDED -> 2 problems, the turtle helmet named -> reddens.
    }
}
