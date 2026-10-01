package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Wither's ruled numbers (Ben, 2026-09-30, "8, yes everything else is fine") and the shared
 * {@link DotRates} rule both statuses now run.
 *
 * <p><b>The cap rows are staged where the cap BINDS</b>, as {@code ScorchTest} insists: on a 20-max
 * mob {@code min(1.0, cap)} is the percent arm for every shipped cap, so a deleted {@code min} and a
 * working one agree there. The figures are the seat's WS2b / WB4b mob-scaling arithmetic (a zombie's
 * max is its gear score): 100 under cap 2.0 and 200 under cap 8.0 bind; 20 and 100 do not.
 */
class WitherTest {

    /** Q-W1, Q-W2, Q-W3 as the shared store reads them. Mutation: any ruled number moved -> red. */
    @Test
    void theRuledNumbersReachTheSharedRecord() {
        assertEquals(40, Wither.RATES.periodTicks(), "Q-W1: a tick every 40 ticks");
        assertEquals(0.05, Wither.RATES.ratePerTick(), 0.0, "Q-W2: 5% of max per tick");
        assertEquals(0.5, Wither.RATES.capFraction(), 0.0, "Q-W2: the cap half the hit, as Scorch");
        assertEquals(200, Wither.RATES.defaultDurationTicks(), "Q-W3: 200 ticks");
        assertEquals(5, Wither.RATES.damageTicksFor(200), "Q-W3: ceil(200 / 40) = five ticks");
    }

    /**
     * BOTH ARMS, AT THE GATE ROWS' OWN MAXES. Mutation: drop the {@code min} in
     * {@code DotRates.damagePerTick} -> the two binding rows redden and the two percent rows do not.
     */
    @Test
    void theCapBindsOnTheBigZombiesAndThePercentArmOnTheSmallOnes() {
        assertEquals(2.0, Wither.RATES.damagePerTick(100, 2.0), 0.0, "WS2b: GS-100 zombie, cap binds");
        assertEquals(1.0, Wither.RATES.damagePerTick(20, 2.0), 0.0, "WS2b control: percent arm");
        assertEquals(8.0, Wither.RATES.damagePerTick(200, 8.0), 0.0, "WB4b: GS-200 zombie, cap binds");
        assertEquals(5.0, Wither.RATES.damagePerTick(100, 8.0), 0.0, "WB4b control: percent arm");
    }

    /**
     * SCORCH STILL READS ITS OWN NUMBERS THROUGH THE SHARED RECORD -- the delegation is a refactor and
     * must move nothing. Mutation: build {@code Scorch.RATES} from Wither's period -> the tick count
     * row reddens (120 / 40 = 3, not 6).
     */
    @Test
    void scorchDelegatesToItsOwnRatesAndNothingMoved() {
        assertEquals(6, Scorch.damageTicksFor(120), "Scorch's 120-tick window is still six ticks");
        assertEquals(20.0, Scorch.damagePerTick(5000, 20), 0.0, "and its cap still binds at 5000");
        assertEquals(5.0, Scorch.damagePerTick(100, 20), 0.0, "and its percent arm at 100");
        assertEquals(Scorch.PERIOD_TICKS, Scorch.RATES.periodTicks());
        assertEquals(Scorch.CAP_FRACTION, Scorch.RATES.capFraction(), 0.0);
    }

    /** The round-up rule, the shared half: 50 ticks of a 40-tick clock is two ticks, 0 is none. */
    @Test
    void aWindowRoundsUpToWholePeriods() {
        assertEquals(2, Wither.RATES.damageTicksFor(50));
        assertEquals(1, Wither.RATES.damageTicksFor(1));
        assertEquals(0, Wither.RATES.damageTicksFor(0));
    }

    @Test
    void aClockOfZeroIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new DotRates(0, 0.05, 0.5, 200));
    }
}
