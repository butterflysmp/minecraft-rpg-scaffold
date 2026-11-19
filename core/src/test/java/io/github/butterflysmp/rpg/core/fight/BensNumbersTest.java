package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN'S NUMBERS OF 2026-11-18 (PLAN-dragon-fight.md, eighth message): v_fast 3.8, v_slow 0.6. At 3.8 the fast close's
 * length (d + 36) / 3.8 seconds is not a whole number of ticks, so the slow close starts on the FIRST WHOLE TICK at
 * which the close's progress has reached 1 ({@link FastClosePath#durationWholeTicks}). These are the code's figures,
 * pinned so the gate can quote them: EXECUTED, never worked by hand.
 */
class BensNumbersTest {

    static FightSettings bens() {
        return new FightSettings(48.0, 12.0, 3.8, 0.6, 1.0, 10, 80L, 40L, 20.0);
    }

    private static FastClosePath path(double d) {
        return assertInstanceOf(Landing.Accepted.class, FastClosePath.plan(bens(), 0, 0, d, 0)).path();
    }

    @Test
    void theFastCloseAt38TakesTheseWholeTicks() {
        // (0 + 36) / 3.8 x 20 = 189.47; (18 + 36) = 284.21; (36 + 36) = 378.95.
        assertEquals(190L, path(0).durationWholeTicks());
        assertEquals(285L, path(18).durationWholeTicks());
        assertEquals(379L, path(36).durationWholeTicks());
        for (double d : new double[] {0, 18, 36}) {
            FastClosePath p = path(d);
            long whole = p.durationWholeTicks();
            assertTrue(p.progressAfter(whole - 1) < 1.0, "d=" + d + ": still closing the tick before");
            assertEquals(1.0, p.progressAfter(whole), 0.0, "d=" + d + ": done on the whole tick");
        }
    }

    @Test
    void theSlowCloseAt06ShutsAfter400Ticks() {
        // 12 / 0.6 = 20 s = 400 ticks.
        assertEquals(400L, SlowClose.start(bens(), 0, 0, 0L).closeTick());
    }

    @Test
    void theCycleWithBensNumbersStartsTheSlowCloseOnTick190AfterALandingAtC() {
        MageCycle cycle = new MageCycle(bens(), 0, 0);
        cycle.mageLanded(20L, 0, 0);
        assertEquals(MageCycle.Phase.FAST_CLOSE, cycle.tick(209L).phase());
        assertEquals(MageCycle.Phase.SLOW_CLOSE, cycle.tick(210L).phase());
        assertEquals(MageCycle.Phase.SLOW_CLOSE, cycle.tick(609L).phase());
        assertEquals(MageCycle.Phase.CLOSED, cycle.tick(610L).phase(), "210 + 400");
    }
}
