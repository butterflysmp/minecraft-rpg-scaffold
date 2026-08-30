package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Thorns's arithmetic in isolation. What it does NOT cover is which value the rider feeds it -- that
 * is {@code ShieldExchangeTest}'s job, and the reason that class exists.
 *
 * <p>Every constant executed, never predicted.
 */
class ThornsTest {

    private static final double EPS = 1e-9;

    /** The shipped mob attack stat the boot gate reads against. */
    private static final double MOB = 15.0;

    /** The shipped curve: I, II, III. */
    private static final double I = 10, II = 20, III = 30;

    @Test
    void theShippedCurveOffTheShippedMobIsTheLadderTheBootGateReads() {
        assertEquals(1.5, Thorns.reflected(MOB, I), EPS);
        assertEquals(3.0, Thorns.reflected(MOB, II), EPS);
        assertEquals(4.5, Thorns.reflected(MOB, III), EPS);
    }

    @Test
    void theNumbersOnSCREENAreTheRoundedOnesAndAllThreeRungsSeparate() {
        // The popup is Math.round(amount) (DamageNumberText), so the gate reads WHOLE numbers. Pinned
        // here because a gate row quoting 1.5 could never be satisfied, and because this is what
        // separates the shipped reading from the rejected one in play.
        assertEquals(2, Math.round(Thorns.reflected(MOB, I)));
        assertEquals(3, Math.round(Thorns.reflected(MOB, II)));
        assertEquals(5, Math.round(Thorns.reflected(MOB, III)));

        // The REJECTED reading -- off the pass-through of the shipped 0.35 shield, which is 9.75 --
        // rounds to 1 / 2 / 3. EXECUTED after the base moved from 0.5; at the old base it was
        // 1 / 2 / 2, so the gate's discriminators changed with the shield and this is the current set.
        // All three rungs still separate on screen, and III (5 vs 3) is the clearest.
        double passThrough = MOB * 0.65;
        assertEquals(1, Math.round(Thorns.reflected(passThrough, I)));
        assertEquals(2, Math.round(Thorns.reflected(passThrough, II)));
        assertEquals(3, Math.round(Thorns.reflected(passThrough, III)));
    }

    @Test
    void aShieldWithoutThornsSendsBackExactlyNothing() {
        // The branch every blocked hit in the game takes today.
        assertEquals(0.0, Thorns.reflected(MOB, Thorns.NONE));
        assertFalse(Thorns.reflects(Thorns.reflected(MOB, Thorns.NONE)));
    }

    @Test
    void theReflectScalesWithTheBLOWRatherThanWithAnythingTheShieldDoes() {
        // The orthogonality, at the level of the formula: the only inputs are the blow and the
        // percent. There is no block fraction in this signature and there must never be one -- that
        // is what keeps Thorns and Bulwark independently tunable.
        for (double percent : new double[]{I, II, III}) {
            assertEquals(MOB * percent / 100.0, Thorns.reflected(MOB, percent), EPS);
        }
        // Twice the blow, twice back.
        assertEquals(2 * Thorns.reflected(MOB, III), Thorns.reflected(2 * MOB, III), EPS);
    }

    @Test
    void onlyAPositiveAmountIsWorthSendingBack() {
        assertFalse(Thorns.reflects(Thorns.NONE));
        assertFalse(Thorns.reflects(-0.0));
        assertTrue(Thorns.reflects(0.0001));
        assertTrue(Thorns.reflects(4.5));
    }

    @Test
    void aNEGATIVEBlowCannotHealTheAttacker() {
        // THE reason reflects() gates the OUTPUT rather than the percent, unlike Bulwark.boosts.
        // A percent can never be negative -- the content boundary refuses it -- but an attack STAT
        // can: Stat is base + sum(modifiers), so a debuffed attacker can carry a negative value.
        // negative x positive is negative, and a negative amount reaching applyDamage would HEAL the
        // mob that is attacking you.
        double reflected = Thorns.reflected(-20.0, III);
        assertTrue(reflected < 0, "the product really is negative: " + reflected);
        assertFalse(Thorns.reflects(reflected),
                "a negative reflect must never be dealt -- it would heal the attacker");

        // A percent-side gate would NOT have caught this: the percent is a perfectly good 30.
        assertTrue(III > 0, "the percent alone looks fine, which is why gating it is not enough");
    }

    @Test
    void aZeroBlowSendsNothingBackRatherThanZeroDamage() {
        // An untracked or freshly-seeded mob can have a 0 attack stat. Dealing a 0 would still paint
        // a "0" popup over it, which reads as a broken reflect rather than as no reflect.
        assertFalse(Thorns.reflects(Thorns.reflected(0.0, III)));
    }
}
