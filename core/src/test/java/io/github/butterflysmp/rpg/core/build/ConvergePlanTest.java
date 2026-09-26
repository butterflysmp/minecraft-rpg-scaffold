package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvergePlanTest {

    private static final int STAR_DEFAULT = 8;
    private static final int STAR_MAX = 35;

    /**
     * THE ROW THAT KILLS MUTWELDDEFAULT. A chosen slot that is not the default must be the target; the
     * mutation that always returns the default left the whole suite green before this class existed.
     */
    @Test
    void theChosenSlotIsTheTargetNotTheDefault() {
        assertEquals(3, ConvergePlan.of(List.of(), 3, STAR_DEFAULT, STAR_MAX).target());
        assertEquals(27, ConvergePlan.of(List.of(), 27, STAR_DEFAULT, STAR_MAX).target());
    }

    @Test
    void anOutOfRangeOrUnknownSlotFallsBackToTheDefault() {
        assertEquals(STAR_DEFAULT, ConvergePlan.of(List.of(), -1, STAR_DEFAULT, STAR_MAX).target());
        assertEquals(STAR_DEFAULT, ConvergePlan.of(List.of(), 36, STAR_DEFAULT, STAR_MAX).target());
        // The stone's range is the hotbar: 9 is out of range for it.
        assertEquals(7, ConvergePlan.of(List.of(), 9, 7, 8).target());
    }

    @Test
    void noneFoundMints() {
        ConvergePlan plan = ConvergePlan.of(List.of(), 3, STAR_DEFAULT, STAR_MAX);
        assertEquals(ConvergePlan.MINT, plan.source());
        assertFalse(plan.noop());
        assertTrue(plan.clear().isEmpty());
    }

    @Test
    void oneAlreadyAtTheTargetIsANoop() {
        ConvergePlan plan = ConvergePlan.of(List.of(3), 3, STAR_DEFAULT, STAR_MAX);
        assertTrue(plan.noop());
        assertTrue(plan.clear().isEmpty());
    }

    @Test
    void oneElsewhereIsLiftedNotMinted() {
        ConvergePlan plan = ConvergePlan.of(List.of(20), 3, STAR_DEFAULT, STAR_MAX);
        assertEquals(20, plan.source());
        assertEquals(3, plan.target());
        assertFalse(plan.noop());
    }

    /** Surplus: keep the LOWEST index, delete the rest -- even when a higher one is already at the target. */
    @Test
    void surplusKeepsTheLowestAndClearsTheRest() {
        ConvergePlan plan = ConvergePlan.of(List.of(1, 3, 30), 3, STAR_DEFAULT, STAR_MAX);
        assertEquals(List.of(3, 30), plan.clear());
        assertEquals(1, plan.source());
        assertFalse(plan.noop(), "two copies, one at the target, is NOT already correct");
    }
}
