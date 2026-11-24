package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a self-contained row stages for itself (the seat's rules 1 and 4, 2026-11-23): the probe's REACH, written in the
 * scenario, and the CLEARED INVENTORY, so no row asks anything of his.
 */
class SelfTestRowStagingTest {

    // --- rule 4: a probe's reach is the scenario's to state ---------------------------------------------------------

    @Test
    void aBareTagProbesSixteenAsEveryOlderRowDid() {
        assertEquals(new SelfTestStaging.ProbeArg("f", 16), SelfTestStaging.probeArg("f").orElseThrow());
    }

    @Test
    void aTagAndANumberProbeThatFar() {
        assertEquals(new SelfTestStaging.ProbeArg("f", 32), SelfTestStaging.probeArg("f 32").orElseThrow());
        assertEquals(new SelfTestStaging.ProbeArg("p", 64), SelfTestStaging.probeArg("p 64").orElseThrow());
    }

    @Test
    void aReachOutsideOneToSixtyFourOrNotANumberIsRefusedAtLoad() {
        for (String bad : List.of("", "f 0", "f 65", "f x", "f 16 2", "f -3")) {
            assertTrue(SelfTestStaging.probeArg(bad).isEmpty(), "'" + bad + "'");
            assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE, bad).isPresent(), "the loader refuses '" + bad + "'");
        }
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE, "f 32").isEmpty());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE, "f").isEmpty());
    }

    // --- rule 1: a gate asks nothing of his inventory ---------------------------------------------------------------

    @Test
    void clearingTakesEveryOccupiedStorageSlotButTheNexus() {
        boolean[] occupied = new boolean[36];
        boolean[] nexus = new boolean[36];
        occupied[0] = true;
        occupied[4] = true;
        occupied[8] = true;
        nexus[8] = true;
        occupied[20] = true;
        occupied[35] = true;
        assertArrayEquals(new int[] {0, 4, 20, 35}, SelfTestStaging.clearPlan(occupied, nexus));
    }

    @Test
    void anEmptyInventoryClearsNothing() {
        assertArrayEquals(new int[0], SelfTestStaging.clearPlan(new boolean[36], new boolean[36]));
    }

    @Test
    void theNewVerbsTakeNoArgumentAndThePreflightTakesItsBox() {
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.CLEAR_INVENTORY, "").isEmpty());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.CLEAR_INVENTORY, "all").isPresent());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.CLEAR_STATE, "").isEmpty());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.CLEAR_STATE, "x").isPresent());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PREFLIGHT, "0 -6 32 10").isEmpty());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PREFLIGHT, "0 -6 32").isPresent());
    }
}
