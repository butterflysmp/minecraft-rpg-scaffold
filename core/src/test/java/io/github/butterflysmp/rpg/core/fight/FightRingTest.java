package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F1, THE RING (PLAN-dragon-fight.md section 2, the seat's mechanism ruling). */
class FightRingTest {

    private static final FightSettings S = FightTestSettings.placeholders();
    private static final double EPS = 1e-9;

    /** The arena centre C is at (0, 0); the Mage lands d blocks east, at (d, 0). */
    private static FastClosePath path(double d) {
        Landing landing = FastClosePath.plan(S, 0.0, 0.0, d, 0.0);
        return assertInstanceOf(Landing.Accepted.class, landing).path();
    }

    private static double[] point(Ring ring, double angleRadians) {
        return new double[] {
                ring.centreXBlocks() + ring.radiusBlocks() * Math.cos(angleRadians),
                ring.centreZBlocks() + ring.radiusBlocks() * Math.sin(angleRadians)};
    }

    /** The largest distance any point at a fixed angle moves between two rings, in blocks. */
    private static double fastestPointMoveBlocks(Ring a, Ring b) {
        double max = 0.0;
        for (int deg = 0; deg < 360; deg++) {
            double t = Math.toRadians(deg);
            double[] p = point(a, t);
            double[] q = point(b, t);
            max = Math.max(max, Math.hypot(q[0] - p[0], q[1] - p[1]));
        }
        return max;
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 18.0, 36.0})
    void noPointOfTheFastCloseOutrunsVFast(double d) {
        FastClosePath p = path(d);
        double capPerTick = S.fastCloseBlocksPerSecond() / FightSettings.TICKS_PER_SECOND;
        long end = (long) Math.ceil(p.durationTicks());
        double worst = 0.0;
        for (long e = 0; e < end; e++) {
            worst = Math.max(worst, fastestPointMoveBlocks(p.ringAfter(e), p.ringAfter(e + 1)));
        }
        assertTrue(worst <= capPerTick + EPS, "fastest point moved " + worst + " per tick, cap " + capPerTick);
        // POSITIVE CONTROL: the cap is reached, not merely respected. A path that crawled would pass the line above.
        assertEquals(capPerTick, worst, 1e-6, "the fastest point should travel AT the cap");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 18.0, 36.0})
    void noPointOfTheReturnOutrunsVFast(double d) {
        FastClosePath p = path(d);
        ReturnPath back = ReturnPath.fromSlowClose(p, 0L, S.mageRingRadiusBlocks());
        double capPerTick = S.fastCloseBlocksPerSecond() / FightSettings.TICKS_PER_SECOND;
        long home = back.homeTick();
        double worst = 0.0;
        for (long t = 0; t < home; t++) {
            worst = Math.max(worst, fastestPointMoveBlocks(back.ringAt(t), back.ringAt(t + 1)));
        }
        assertTrue(worst <= capPerTick + EPS, "fastest point moved " + worst + " per tick, cap " + capPerTick);
        assertEquals(capPerTick, worst, 1e-6, "the return's fastest point should travel AT the cap");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 18.0, 36.0})
    void eachLaterRingLiesInsideEveryEarlierOne(double d) {
        FastClosePath p = path(d);
        long end = (long) Math.ceil(p.durationTicks());
        List<Ring> rings = new ArrayList<>();
        for (long e = 0; e <= end; e += 4) rings.add(p.ringAfter(e));
        rings.add(p.ringAfter(end));
        for (int i = 0; i < rings.size(); i++) {
            for (int j = i + 1; j < rings.size(); j++) {
                Ring outer = rings.get(i);
                Ring inner = rings.get(j);
                double centres = Math.hypot(inner.centreXBlocks() - outer.centreXBlocks(),
                        inner.centreZBlocks() - outer.centreZBlocks());
                assertTrue(centres + inner.radiusBlocks() <= outer.radiusBlocks() + EPS,
                        "ring " + j + " pokes out of ring " + i + " at d=" + d);
            }
        }
    }

    @Test
    void aLandingAtExactly36IsAcceptedAnd36Point01IsRefused() {
        assertInstanceOf(Landing.Accepted.class, FastClosePath.plan(S, 0.0, 0.0, 36.0, 0.0));
        Landing far = FastClosePath.plan(S, 0.0, 0.0, 36.01, 0.0);
        Landing.TooFar refused = assertInstanceOf(Landing.TooFar.class, far);
        assertEquals(36.01, refused.distanceBlocks(), EPS);
        assertEquals(36.0, refused.maxDistanceBlocks(), EPS);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 18.0, 36.0})
    void theFastCloseLastsDPlus36OverVFastSeconds(double d) {
        FastClosePath p = path(d);
        double expectedTicks = (d + 36.0) / 5.0 * 20.0; // 144, 216, 288
        assertEquals(expectedTicks, p.durationTicks(), 1e-9);
        long whole = Math.round(expectedTicks);
        assertTrue(p.progressAfter(whole - 1) < 1.0, "still closing one tick before the end");
        assertEquals(1.0, p.progressAfter(whole), 0.0, "done on the exact tick");
        Ring last = p.ringAfter(whole);
        assertEquals(d, last.centreXBlocks(), EPS);
        assertEquals(12.0, last.radiusBlocks(), EPS);
        Ring first = p.ringAfter(0);
        assertEquals(0.0, first.centreXBlocks(), EPS);
        assertEquals(48.0, first.radiusBlocks(), EPS);
    }

    @Test
    void aKillAddsEKillAndNeverPastTwelve() {
        SlowClose slow = SlowClose.start(S, 0.0, 0.0, 0L);
        assertEquals(12.0, slow.afterKill(0L).radiusAt(0L), EPS, "a kill at 12 stays at 12");
        // 100 ticks = 5 s at 0.4 blocks/s = 2 blocks: 12 -> 10.
        assertEquals(10.0, slow.radiusAt(100L), EPS);
        SlowClose one = slow.afterKill(100L);
        assertEquals(11.0, one.radiusAt(100L), EPS);
        SlowClose two = one.afterKill(100L);
        assertEquals(12.0, two.radiusAt(100L), EPS);
        SlowClose three = two.afterKill(100L);
        assertEquals(12.0, three.radiusAt(100L), EPS, "capped at 12");
        assertEquals(11.6, three.radiusAt(120L), EPS, "and it keeps closing from there");
    }

    @Test
    void aClosedRingStaysClosed() {
        SlowClose slow = SlowClose.start(S, 5.0, 5.0, 0L);
        long closes = slow.closeTick();
        assertTrue(slow.radiusAt(closes - 1) > 0.0, "open the tick before");
        assertEquals(0.0, slow.radiusAt(closes), 0.0, "shut on the close tick");
        assertEquals(600L, closes, "12 blocks at 0.4 blocks/s is 30 s = 600 ticks");
        SlowClose afterKill = slow.afterKill(closes);
        assertSame(slow, afterKill, "a kill on the close tick changes nothing");
        assertSame(slow, slow.afterKill(closes + 500), "nor any later kill");
        assertEquals(0.0, afterKill.radiusAt(closes + 10_000), 0.0);
        assertFalse(slow.ringAt(closes).contains(5.0, 5.0), "no point is inside a closed ring, not even its centre");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 18.0, 36.0})
    void theReturnVisitsTheFastClosePositionsInReverse(double d) {
        FastClosePath p = path(d);
        long duration = Math.round(p.durationTicks());
        ReturnPath back = ReturnPath.fromSlowClose(p, 1_000L, S.mageRingRadiusBlocks());
        assertEquals(1_000L + duration, back.homeTick());
        for (long k = 0; k <= duration; k++) {
            Ring returning = back.ringAt(1_000L + k);
            Ring closing = p.ringAfter(duration - k);
            assertEquals(closing.centreXBlocks(), returning.centreXBlocks(), 1e-9, "k=" + k);
            assertEquals(closing.centreZBlocks(), returning.centreZBlocks(), 1e-9, "k=" + k);
            assertEquals(closing.radiusBlocks(), returning.radiusBlocks(), 1e-9, "k=" + k);
        }
    }

    @Test
    void theReturnsSlowPartVisitsTheSlowClosePositionsInReverse() {
        FastClosePath p = path(18.0);
        SlowClose slow = SlowClose.start(S, 18.0, 0.0, 0L);
        long died = 300L; // radius 12 - 0.4 x 15 = 6
        ReturnPath back = ReturnPath.fromSlowClose(p, died, slow.radiusAt(died));
        for (long k = 0; k <= died; k++) {
            assertEquals(slow.radiusAt(died - k), back.ringAt(died + k).radiusBlocks(), 1e-9, "k=" + k);
            assertEquals(18.0, back.ringAt(died + k).centreXBlocks(), 1e-9);
        }
        // Then the fast part, from (M, 12) back to (C, 48): 216 more ticks at d = 18.
        assertEquals(died + died + 216L, back.homeTick());
        Ring home = back.ringAt(back.homeTick());
        assertEquals(0.0, home.centreXBlocks(), 1e-9);
        assertEquals(48.0, home.radiusBlocks(), 1e-9);
    }

    @Test
    void insideIsInclusiveAtTheEdgeAndIgnoresHeight() {
        Ring ring = new Ring(3.0, -4.0, 12.0);
        assertTrue(ring.contains(15.0, -4.0), "exactly on the edge is inside");
        assertTrue(ring.contains(3.0, 8.0), "exactly on the edge, other axis");
        assertFalse(ring.contains(15.000001, -4.0), "a hair past the edge is outside");
        assertTrue(ring.contains(3.0, -4.0), "the centre is inside an open ring");
        // Height: contains takes no y at all, so a feet position anywhere above or below reads the same.
    }
}
