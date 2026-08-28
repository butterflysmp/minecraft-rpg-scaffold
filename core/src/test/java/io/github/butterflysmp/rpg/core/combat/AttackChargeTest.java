package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The charge curve, which is the whole of a swing's timing reward.
 *
 * Every expected value below was produced by EXECUTING the expression and pasting what it printed,
 * never by reasoning about the arithmetic -- the standing rule in DefenseTest, and the reason
 * scale(0.1) is asserted against an EPS rather than against a literal 0.208 (it is actually
 * 0.20800000000000002). The two exact-equality assertions are exact BECAUSE execution showed they
 * are, not because the algebra says they ought to be.
 *
 * The curve is also anchored to a real observation, not just to itself: the 2026-08-28 Step 0 boot
 * logged a vanilla iron sword (base 6.0) dealing a raw 3.9725 at a read charge of 0.76. If this
 * class ever stops agreeing with theBootObservedThisExactCurve below, our melee has diverged from
 * the vanilla feel it is imitating.
 *
 * Each test names the mutation it forces red.
 */
class AttackChargeTest {

    private static final double EPS = 1e-9;

    // --- The curve ---

    @Test
    void anUnchargedSwingLandsTheFloorRatherThanNothing() {
        assertEquals(0.2, AttackCharge.scale(0.0), EPS, "an instant re-click is weak, not free");
        assertEquals(1.6, AttackCharge.apply(8, 0.0), EPS, "an 8-damage sword still lands 1.6");
        // Mutation: drop MIN_SCALE -> 0.0 and apply(8,0) -> 0.0, which reads as a broken weapon
        // rather than a punished swing, and makes spam deal literally nothing -> reddens.
    }

    @Test
    void aFullyChargedSwingIsAnExactIdentity() {
        // Exact, not EPS: verified by execution that scale(1.0) == 1.0 and 8 * 1.0 == 8.0 in binary
        // floating point. This matters because EVERY ability, projectile and non-melee caller passes
        // FULL_CHARGE -- if the identity were merely approximate, the whole game's damage would
        // drift by a rounding error the moment the charge factor was threaded through.
        assertEquals(1.0, AttackCharge.scale(AttackCharge.FULL_CHARGE));
        assertEquals(8.0, AttackCharge.apply(8, AttackCharge.FULL_CHARGE));
        // Mutation: make MIN_SCALE + CHARGED_SHARE != 1.0 (e.g. 0.2 + 0.75) -> reddens.
    }

    @Test
    void theCurveIsQuadraticSoTheLastOfTheWindUpIsWorthWaitingFor() {
        // Half charge is worth 0.4, NOT the 0.6 a linear ramp would give. This single number is the
        // difference between timing mattering and timing being cosmetic.
        assertEquals(0.4, AttackCharge.scale(0.5), EPS);
        assertEquals(0.25, AttackCharge.scale(0.25), EPS);
        assertEquals(0.65, AttackCharge.scale(0.75), EPS);
        // Mutation: replace clamped*clamped with clamped -> scale(0.5) becomes 0.6 -> reddens.
    }

    @Test
    void theBootObservedThisExactCurve() {
        // Step 0, 2026-08-28: vanilla iron sword, attack damage 6.0, PRE cooldown=0.7600,
        // RIDER rawDamage=3.9725 (the server prints 4dp; the exact value is 3.97248).
        assertEquals(3.97248, AttackCharge.apply(6.0, 0.76), EPS,
                "our curve must reproduce the vanilla damage the boot actually measured");
        assertEquals(6.0, AttackCharge.apply(6.0, 1.0), EPS,
                "and the full-charge swing the same boot measured");
        // Mutation: change MIN_SCALE or CHARGED_SHARE at all -> reddens. This is the test that ties
        // the constants to an observation instead of to each other.
    }

    // --- The clamp, which guards an open input ---

    @Test
    void aChargeAboveFullIsClampedRatherThanAmplifyingTheHit() {
        // The charge crosses a thread hop and a per-player stash before it gets here. A stale or
        // corrupted read must never multiply damage UP: unclamped, 2.0 would scale by 3.4.
        assertEquals(1.0, AttackCharge.scale(2.0), EPS);
        assertEquals(1.0, AttackCharge.scale(1000.0), EPS);
        assertEquals(8.0, AttackCharge.apply(8, 2.0), EPS, "never more than the earned full hit");
        // Mutation: drop the Math.min -> scale(2.0) = 3.4, apply(8,2) = 27.2 -> reddens.
    }

    @Test
    void aNegativeChargeIsClampedRatherThanSquaredBackIntoAFullSwing() {
        // The sign trap: the curve squares its input, so WITHOUT the lower clamp a -1.0 reads as
        // 0.2 + 1.0*0.8 = 1.0 -- a full-power swing from the weakest possible input. The upper
        // clamp alone does not catch this, which is why both halves are asserted separately.
        assertEquals(0.2, AttackCharge.scale(-1.0), EPS);
        assertEquals(0.2, AttackCharge.scale(-0.5), EPS);
        // Mutation: drop the Math.max -> scale(-1.0) = 1.0, the strongest swing in the game from a
        // garbage value -> reddens.
    }

    // --- Shape ---

    @Test
    void theCurveRisesWithChargeAndStaysInsideItsTwoBounds() {
        double previous = -1.0;
        for (double charge = 0.0; charge <= 1.0 + EPS; charge += 0.05) {
            double scale = AttackCharge.scale(charge);
            assertTrue(scale > previous, "must rise with charge, at " + charge);
            assertTrue(scale >= AttackCharge.MIN_SCALE, "never below the floor, at " + charge);
            assertTrue(scale <= 1.0, "never above a full hit, at " + charge);
            assertTrue(AttackCharge.apply(8, charge) > 0.0, "a hit always lands, at " + charge);
            previous = scale;
        }
        // Mutation: negate CHARGED_SHARE -> the curve falls as the swing winds up -> reddens on the
        // rising assertion, which a fixed-point check at 0 and 1 alone would not catch.
    }
}
