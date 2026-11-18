package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The implosion's inward spiral as pure geometry (the 2026-11-17 brief, Part D 5): drawn from the REACH to the ORIGIN
 * over its ticks, so "it must read at the stage's reach" is a property of this function and is tested here, not by eye.
 */
class SpiralTest {

    /** 10 ticks, 3 arms, 1.5 turns, 6 points per arm per tick. */
    private static final Spiral S = new Spiral(10, 3, 1.5, 6, 0.0);

    private static double radius(Vec3 p) {
        return Math.hypot(p.x(), p.z());
    }

    private static List<Vec3> all(Spiral s, double radius, int arm) {
        List<Vec3> out = new ArrayList<>();
        for (int t = 0; t < s.durationTicks(); t++) out.addAll(s.arm(radius, t, arm));
        return out;
    }

    @Test
    void itStartsAtTheReachAndEndsAtTheOrigin() {
        for (double reach : new double[] {2.0, 6.0}) {
            List<Vec3> arm0 = all(S, reach, 0);
            assertEquals(reach, radius(arm0.get(0)), 1e-12, "first point at the reach, " + reach);
            assertEquals(0.0, radius(arm0.get(arm0.size() - 1)), 1e-12, "last point at the origin");
        }
    }

    @Test
    void aStageOneSwirlIsTwoBlocksWideAndAStageFiveSwirlSix() {
        // The widest point of any tick is the radius it was handed: the swirl reads at the reach it is drawn at.
        double widest2 = 0;
        double widest6 = 0;
        for (int t = 0; t < 10; t++) {
            for (int a = 0; a < 3; a++) {
                for (Vec3 p : S.arm(2.0, t, a)) widest2 = Math.max(widest2, radius(p));
                for (Vec3 p : S.arm(6.0, t, a)) widest6 = Math.max(widest6, radius(p));
            }
        }
        assertEquals(2.0, widest2, 1e-12);
        assertEquals(6.0, widest6, 1e-12);
    }

    @Test
    void itMovesStrictlyInwardEveryPoint() {
        List<Vec3> arm = all(S, 6.0, 1);
        for (int i = 1; i < arm.size(); i++) {
            assertTrue(radius(arm.get(i)) < radius(arm.get(i - 1)), "inward at point " + i);
        }
        assertEquals(60, arm.size(), "10 ticks x 6 points");
    }

    @Test
    void eachTickDrawsItsOwnStretchOfTheCurveInOrder() {
        // Tick 0's points are all outside tick 1's: the swirl comes IN over the ticks.
        double innermost0 = S.arm(6.0, 0, 0).stream().mapToDouble(SpiralTest::radius).min().orElseThrow();
        double outermost1 = S.arm(6.0, 1, 0).stream().mapToDouble(SpiralTest::radius).max().orElseThrow();
        assertTrue(innermost0 > outermost1);
    }

    @Test
    void theArmsAreEvenlySpacedAndTheCurveTurnsTheAuthoredTurns() {
        // At the first point the arms sit at 0, 120 and 240 degrees; the angle swept by the end is 1.5 turns.
        for (int a = 0; a < 3; a++) {
            Vec3 first = S.arm(6.0, 0, a).get(0);
            assertEquals(Math.toRadians(120.0 * a), Math.atan2(first.z(), first.x()) < -1e-9
                    ? Math.atan2(first.z(), first.x()) + 2 * Math.PI : Math.atan2(first.z(), first.x()), 1e-9);
        }
        assertEquals(1.5 * 2 * Math.PI, S.angle(0, 1.0), 1e-12, "the angle at the end of the curve");
        assertEquals(0.0, S.angle(0, 0.0), 0.0);
        Spiral offset = new Spiral(10, 3, 1.5, 6, 60.0);
        assertEquals(Math.toRadians(60.0), offset.angle(0, 0.0), 1e-12, "a phase turns every arm by it");
    }

    @Test
    void theHeightIsNotTheSpiralsToDraw() {
        for (Vec3 p : all(S, 6.0, 2)) assertEquals(0.0, p.y(), 0.0, "flat: the caller lifts it");
    }

    @Test
    void aBadSpiralIsRefusedByName() {
        assertThrows(IllegalArgumentException.class, () -> new Spiral(1, 3, 1.5, 6, 0), "1 tick");
        assertThrows(IllegalArgumentException.class, () -> new Spiral(10, 0, 1.5, 6, 0), "no arms");
        assertThrows(IllegalArgumentException.class, () -> new Spiral(10, 3, -1, 6, 0), "negative turns");
        assertThrows(IllegalArgumentException.class, () -> new Spiral(10, 3, 1.5, 0, 0), "no points");
        assertThrows(IllegalArgumentException.class, () -> S.arm(6, 10, 0), "tick past the end");
        assertThrows(IllegalArgumentException.class, () -> S.arm(6, 0, 3), "an arm that does not exist");
    }
}
