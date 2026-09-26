package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAN-build-system.md section 7.4: from tick 10 to tick 50 after Recall, once, and never from Recall's hold. */
class RecastRuleTest {

    private static final long RECALL = 1000;
    private static final int FROM = 10;
    private static final int WINDOW = 50;

    /** A clean press: its hold started at the press itself, after Recall. */
    private static boolean press(long tick) {
        return RecastRule.accepts(RECALL, tick, FROM, WINDOW, false, tick);
    }

    /** THE WINDOW'S EDGES, both inclusive, and the ticks either side of each. */
    @Test
    void theWindowIsTenToFiftyInclusive() {
        assertFalse(press(RECALL + 9), "tick 9 is before the floor");
        assertTrue(press(RECALL + 10), "tick 10 is the floor, inclusive");
        assertTrue(press(RECALL + 30));
        assertTrue(press(RECALL + 50), "tick 50 is ruling 29's 2.5 s, inclusive");
        assertFalse(press(RECALL + 51), "tick 51 is after the window");
    }

    @Test
    void theRecallInputItselfIsNotARecast() {
        assertFalse(press(RECALL));
    }

    /** ONE RECAST PER CAST. */
    @Test
    void aUsedWindowAcceptsNothing() {
        assertFalse(RecastRule.accepts(RECALL, RECALL + 20, FROM, WINDOW, true, RECALL + 20));
    }

    /**
     * THE HELD-INPUT GUARD: an input inside the window whose hold began at (or before) Recall's own input is
     * the click that cast Recall, still held. Held right-click lands at RECALL+12, inside the window.
     */
    @Test
    void anInputFromTheHoldThatCastRecallIsNot() {
        assertFalse(RecastRule.accepts(RECALL, RECALL + 12, FROM, WINDOW, false, RECALL));
        assertFalse(RecastRule.accepts(RECALL, RECALL + 48, FROM, WINDOW, false, RECALL - 40),
                "a hold begun before Recall, still going");
    }

    /** Released and pressed again: a new hold, started after Recall, inside the window. */
    @Test
    void aNewHoldInsideTheWindowIs() {
        assertTrue(RecastRule.accepts(RECALL, RECALL + 25, FROM, WINDOW, false, RECALL + 21));
    }
}
