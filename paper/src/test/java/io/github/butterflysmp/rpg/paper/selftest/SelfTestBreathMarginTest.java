package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EB4R3's MARGIN (the seat's queue, item 4, 2026-11-19). EB4R2 stood him on the breath for 55 ticks. The cloud applies
 * at most once every 20 ticks (READ at F9b, its reapplication delay), so its applications in a stay starting at s are at
 * s, s+20, s+40, s+60: three in 55 ticks, and a FOURTH (120 before armour, over 100) only 5 ticks past the end. EB4R3's
 * stay is short enough that a fourth needs at least 10 ticks more than the row gives. Read from the shipped scenario
 * file, so the row cannot drift from the arithmetic.
 */
class SelfTestBreathMarginTest {

    /** The cloud's reapplication delay, ticks (READ at F9b: `interval=20` on every to-breath line). */
    private static final int REAPPLY_TICKS = 20;
    /** The arithmetic's limit: three breaths, 90, are under 100; a fourth is not. */
    private static final int MOST_BREATHS = 3;
    private static final int SPARE_TICKS = 10;

    @Test
    void eb4R3sStayLeavesAFourthBreathAtLeastTenTicksAway() {
        List<Scenario> end = ScenarioLoader.load(path -> SelfTestBreathMarginTest.class.getClassLoader()
                .getResourceAsStream(path)).gates().get("fight-end");
        Scenario s = end.stream().filter(x -> x.row().equals("EB4R3")).findFirst()
                .orElseThrow(() -> new AssertionError("no EB4R3 in fight-end"));
        int stay = -1;
        List<Scenario.Step> steps = s.steps();
        for (int i = 0; i < steps.size() - 1; i++) {
            if (steps.get(i).verb() == Scenario.Verb.CMD && steps.get(i).arg().trim().equals("rpg fight dragon to-breath")) {
                Scenario.Step next = steps.get(i + 1);
                assertEquals(Scenario.Verb.WAIT, next.verb(), "the stay is the wait right after to-breath");
                stay = Integer.parseInt(next.arg().trim());
                assertTrue(steps.get(i + 2).arg().trim().startsWith("tp @s "), "and then he is moved off the cloud");
            }
        }
        assertTrue(stay > 0, "found the stay");
        int fourth = MOST_BREATHS * REAPPLY_TICKS; // the earliest a fourth application can come, from the stay's start
        assertTrue(fourth - stay >= SPARE_TICKS, "a fourth breath is " + (fourth - stay) + " ticks past the stay of " + stay);
    }
}
