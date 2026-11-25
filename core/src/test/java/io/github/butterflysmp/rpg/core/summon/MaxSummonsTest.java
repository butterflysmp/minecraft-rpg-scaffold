package io.github.butterflysmp.rpg.core.summon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** THE STAT max_summons (the seat's PR 1, 2026-11-24; Ben: "Default is 1 max summon ... Certain gear will increase this"). */
class MaxSummonsTest {

    @Test
    void anAbsentBaseIsBensDefaultOfOne() {
        assertEquals(1, MaxSummons.DEFAULT);
        assertEquals(1, MaxSummons.effective(null, List.of()), "a profile written before the stat reads 1");
    }

    @Test
    void theStoredBaseReplacesTheDefault() {
        assertEquals(0, MaxSummons.effective(0, List.of()), "the selftest's override of 0: the cap row");
        assertEquals(3, MaxSummons.effective(3, List.of()));
    }

    @Test
    void gearGrantsAreSummedOntoTheBase() {
        // No gear grants it in this slice; the sum is built so the first grant needs no new arithmetic.
        assertEquals(4, MaxSummons.effective(null, List.of(1, 2)), "1 + 1 + 2");
        assertEquals(5, MaxSummons.effective(2, List.of(3)), "2 + 3");
    }

    @Test
    void theSumNeverGoesBelowZero() {
        assertEquals(0, MaxSummons.effective(1, List.of(-3)), "a negative grant floors at 0, never a negative cap");
    }
}
