package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** PLAN-build-system.md section 7.4: a stream whose gaps never exceed 8 ticks is ONE hold. */
class InputHoldTest {

    private static InputHold feed(long... ticks) {
        InputHold hold = InputHold.NONE;
        for (long t : ticks) hold = hold.next(t);
        return hold;
    }

    @Test
    void theFirstInputStartsAHold() {
        assertEquals(100, feed(100).holdStartTick());
    }

    /** The measured held right-click: exactly 4 apart. It never leaves the hold it started. */
    @Test
    void aHeldRightClickIsOneHold() {
        assertEquals(100, feed(100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 148, 152).holdStartTick());
    }

    /** The measured held left click on a block: every tick. */
    @Test
    void aHeldBlockSwingIsOneHold() {
        long[] ticks = new long[60];
        for (int i = 0; i < ticks.length; i++) ticks[i] = 200 + i;
        assertEquals(200, feed(ticks).holdStartTick());
    }

    /** The edges of G = 8: a gap of exactly 8 continues, a gap of 9 starts a new hold. */
    @Test
    void aGapOfEightContinuesAndNineStartsANewHold() {
        assertEquals(100, feed(100, 108).holdStartTick());
        assertEquals(109, feed(100, 109).holdStartTick());
    }

    /**
     * THE HELD-INPUT GUARD's row, from the input side: every input extends the hold, INCLUDING the ones the
     * cast refused. A tracker fed only successful casts would see a 200-tick cooldown's refusals as silence and
     * read the hold as released.
     */
    @Test
    void theHoldIsCarriedByEveryInputNotOnlyTheOnesThatCast() {
        // Recall casts at 100; the held inputs after it are all refused (on cooldown), yet they are the hold.
        assertEquals(100, feed(100, 104, 108, 112, 116, 120, 124).holdStartTick());
    }

    @Test
    void theLastInputIsKept() {
        assertEquals(124, feed(100, 124).lastInputTick());
    }
}
