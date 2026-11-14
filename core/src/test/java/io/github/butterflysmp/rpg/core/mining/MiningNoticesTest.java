package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The DENIED notice's text and how long it owns the action bar. NAME THE QUANTITY: the hold is in
 * SERVER TICKS, an integer. Ben (2026-11-14): "Is it possible to have the pickaxe message over ride the
 * action bar for 1.5s" -- 1.5 s at 20 TPS is 30 ticks, and under lag it is longer in real time.
 */
class MiningNoticesTest {

    @Test
    void theDeniedHoldIsBensOnePointFiveSecondsInTicks() {
        assertEquals(30, MiningNotices.DENIED_HOLD_TICKS, "Ben: 1.5 s, at 20 ticks a second");
    }

    @Test
    void theDeniedTextIsUnchangedFromSliceOne() {
        // Slice 1's gate (M-DENY) predicted this exact text; S1-c only changes how long it shows.
        assertEquals("You need a pickaxe to mine this.", MiningNotices.DENIED_TEXT);
    }
}
