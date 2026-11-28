package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M4 (the seat, 2026-11-28): "Scorch follows as before; the burn count changes with the shorter cast: re-execute it."
 * Through the REAL store, on the server's order as READ ({@link ScorchBurnsReadOrder}).
 *
 * <p><b>THE MODEL'S CONTROL IS THE LAST BOOT'S OWN READINGS</b> ({@code GATE-smolder.md}, READINGS, the boot of
 * {@code 01be9d3b}, pulse 2 one tick early): SM2's a3, hit at +0, +59, +120, +180, read 15 burns at +20 to +300; SM5's
 * d6, hit at +0 and +59, read EIGHT burns, +20 to +160 (the gate had predicted nine). A model that cannot reproduce both
 * is not the server's.
 */
class SmolderRetunedBurnsTest {

    private static List<Long> every20(long from, long to) {
        List<Long> out = new ArrayList<>();
        for (long t = from; t <= to; t += 20) out.add(t);
        return out;
    }

    @Test
    void theModelReproducesTheBootsTwoReadings() {
        var sm2 = ScorchBurnsReadOrder.burns(3000, 80, 0, 59, 120, 180);
        assertEquals(every20(20, 300), sm2.offsets(), "SM2 a3 as read: 15 burns, +20 to +300");
        var sm5 = ScorchBurnsReadOrder.burns(3000, 80, 0, 59);
        assertEquals(every20(20, 160), sm5.offsets(), "SM5 d6 as read: 8 burns, +20 to +160");
        assertEquals(3000 - 2 * 160 - 8 * 80, 3000 - 2 * 160 - sm5.total(), 1e-9, "SM5's end as read, 2040");
    }

    /**
     * SMOLDER RETUNED (M1, M4): one body met by all four pulses at one reach tick is hit at +0, +30, +60, +90 from its first
     * hit. EXECUTED: TEN burns of 80 (min(5% of 3000, the cap 80)), +20 to +200, 800 in all; the window restarted by the
     * last hit runs out 120 after it. Before Ben's change four hits 60 apart burned fifteen times (the control above). And
     * the shorter runs a cancel leaves: one hit 6 burns (480), two hits 7 (560), three hits 9 (720). On a shared tick (+60)
     * the burn runs before the refresh, so it counts against the old window, not the new.
     */
    @Test
    void fourHitsThirtyApartBurnTenTimesAndTheShorterRunsAsExecuted() {
        var four = ScorchBurnsReadOrder.burns(3000, 80, 0, 30, 60, 90);
        assertEquals(every20(20, 200), four.offsets());
        assertEquals(80.0, four.each(), 1e-9);
        assertEquals(800.0, four.total(), 1e-9);
        assertEquals(every20(20, 120), ScorchBurnsReadOrder.burns(3000, 80, 0).offsets());
        assertEquals(every20(20, 140), ScorchBurnsReadOrder.burns(3000, 80, 0, 30).offsets());
        assertEquals(every20(20, 180), ScorchBurnsReadOrder.burns(3000, 80, 0, 30, 60).offsets());
    }
}
