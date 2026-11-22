package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Wither;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A WEAKER BURN NEVER LOWERS A STRONGER ONE (the seat's ruling, 2026-11-22; the account is GATE-living-lantern.md, THE
 * BURN, READ). {@code DotStatus.apply}'s refresh arm keeps the LARGER of the standing cap and the new one; the credit
 * stays the newest applier's. Scorch and withering share the store, so the rule is one rule for both, on purpose.
 *
 * <p>The cause, as LL9 logged it on {@code cca65717}: a Living Lantern burn (14 x 0.5 = cap 7) on a 100-max zombie,
 * then an Ignite blast's 6 x 0.5 = cap 3 on the same body, and every burn after it read {@code cap=3.000 sent=3.000}.
 * Read here off the {@code DOTTICK} trace, the line that showed it.
 */
class BurnCapNeverLoweredTest {

    private static final UUID LANTERN = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID IGNITE = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private static List<DotTick> scorch(double firstCap, double secondCap) {
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        List<DotTick> ticks = new ArrayList<>();
        var scorch = new ScorchStatus("scorch", ticks::add);
        UUID id = UUID.randomUUID();
        scorch.apply(id, clock, sink, 1, firstCap, LANTERN, 120, "fire", 1);
        clock.advance(10);
        scorch.apply(id, clock, sink, 1, secondCap, IGNITE, 120, "fire", 2);
        clock.advance(40);   // to t=50: burns at 20 and 40
        return ticks;
    }

    /** THE ROW THE BURN READ WROTE: cap 7, then 3, still reads 7 (and burns the 5.0 percent arm, not 3.0). */
    @Test
    void aScorchAtCapSevenStaysAtSevenWhenACapThreeLandsOnIt() {
        List<DotTick> ticks = scorch(7.0, 3.0);
        assertEquals(2, ticks.size(), "two burns traced");
        for (DotTick t : ticks) {
            assertEquals(7.0, t.cap(), 1e-9, "the stronger cap stands: " + t);
            assertEquals(5.0, t.sent(), 1e-9, "min(5% of 100, 7): LL9 read 3.000 here before the ruling");
            assertEquals(IGNITE, t.applier(), "the credit is still the newest applier's");
        }
    }

    /** THE MIRROR, so the row is not passing because the field is never written: cap 3, then 7, reads 7. */
    @Test
    void aStrongerScorchStillRaisesTheCap() {
        for (DotTick t : scorch(3.0, 7.0)) {
            assertEquals(7.0, t.cap(), 1e-9, "a stronger burn raises it: " + t);
        }
    }

    /** WITHERING, THE SAME STORE AND THE SAME RULE: cap 4, then 1, still reads 4; and 1 then 4 reads 4. */
    @Test
    void witheringFollowsTheSameRuleBothWays() {
        for (double[] caps : new double[][] {{4.0, 1.0}, {1.0, 4.0}}) {
            var clock = new FakeTickTarget();
            var sink = new FakeScorchSink(clock, 100.0);
            List<DotTick> ticks = new ArrayList<>();
            var wither = new DotStatus(Wither.RATES, "wither", "withering", ticks::add);
            UUID id = UUID.randomUUID();
            wither.apply(id, clock, sink, 1, caps[0], LANTERN, 200, "wither");
            clock.advance(10);
            wither.apply(id, clock, sink, 1, caps[1], IGNITE, 200, "wither");
            clock.advance(80);   // burns at 40 and 80
            assertEquals(2, ticks.size(), "two withering burns traced");
            for (DotTick t : ticks) {
                assertEquals(4.0, t.cap(), 1e-9, "caps " + caps[0] + " then " + caps[1] + ": " + t);
                assertEquals(4.0, t.sent(), 1e-9, "min(5% of 100, 4): the cap binds");
                assertEquals(IGNITE, t.applier(), "the newest applier's credit");
            }
        }
    }
}
