package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * P10(c) and the extra guard (the seat's ruling, 2026-11-24): a plugin chunk ticket is NOT counted (READ, one per chunk
 * and plugin), so a pad command and a run must never hold the pad's tickets at once. Both directions refused.
 */
class SelfTestOccupancyTest {

    @Test
    void aRunIsRefusedWhileThePadCommandIsInFlight() {
        var o = new SelfTestOccupancy();
        assertNull(o.refusePad(), "free: the pad command may start");
        o.padStarted();
        assertEquals(SelfTestOccupancy.PAD_BUSY, o.refuseRun());
        o.padEnded();
        assertNull(o.refuseRun(), "free again");
    }

    @Test
    void thePadCommandIsRefusedWhileARunIsActiveAndASecondPadCommandToo() {
        var o = new SelfTestOccupancy();
        o.runStarted();
        assertEquals(SelfTestOccupancy.RUN_BUSY, o.refusePad());
        assertEquals(SelfTestOccupancy.RUN_ALREADY, o.refuseRun(), "two runs cannot overlap");
        o.runEnded();
        o.padStarted();
        assertEquals(SelfTestOccupancy.PAD_BUSY, o.refusePad(), "nor two pad commands");
        o.padEnded();
        assertNull(o.refusePad());
        assertNull(o.refuseRun());
    }
}
