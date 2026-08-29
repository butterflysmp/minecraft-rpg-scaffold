package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.Shield;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bulwark's composition rule, and above all the fact that it is ADDITIVE rather than one of the two
 * readings that look identical on the only shield we ship.
 *
 * <p>Every constant below was produced by RUNNING the expression against the real
 * {@code Shield.clamp}, never by hand. Three float claims made from reasoning in this project have
 * all been wrong.
 */
class BulwarkTest {

    private static final double EPS = 1e-9;

    /** The shipped roundshield. */
    private static final double ROUNDSHIELD = 0.5;

    /** The shipped curve, percent by level: I, II, III. */
    private static final double I = 5, II = 10, III = 15;

    @Test
    void theShippedCurveOnTheShippedShieldIsTheLadderTheBootGateReads() {
        // Exact, no epsilon: these three land on clean binary fractions and the gate quotes them.
        assertEquals(0.55, Bulwark.effectiveDr(ROUNDSHIELD, I));
        assertEquals(0.60, Bulwark.effectiveDr(ROUNDSHIELD, II));
        assertEquals(0.65, Bulwark.effectiveDr(ROUNDSHIELD, III));
    }

    @Test
    void anUnenchantedShieldIsUntouchedRatherThanRecomputed() {
        // The branch every shield in the game takes. NONE must be a true identity on the base DR,
        // or a plain block silently changes the day this class ships.
        assertEquals(ROUNDSHIELD, Bulwark.effectiveDr(ROUNDSHIELD, Bulwark.NONE));
        for (double dr = 0.0; dr <= 1.0; dr += 0.05) {
            assertEquals(Shield.clamp(dr), Bulwark.effectiveDr(dr, Bulwark.NONE), EPS,
                    "a zero bonus changed the block fraction at dr=" + dr);
        }
    }

    @Test
    void theCompositionIsADDITIVEAndNotEitherReadingThatMatchesItAtAHalf() {
        // WHAT 0.5 CAN AND CANNOT DO -- stated precisely, because the first draft of this comment
        // overclaimed and the mutation run caught it.
        //
        // At dr 0.5 the two REJECTED readings are bit-identical to EACH OTHER -- multiplicative
        // (dr * (1+p)) and diminishing (1-(1-dr)(1-p)) both give 0.525 / 0.55 / 0.575. Additive
        // gives 0.55 / 0.60 / 0.65 and is distinguishable from both, so the shipped-shield test
        // above DOES redden on either wrong implementation. Measured: both mutations failed it.
        //
        // What 0.5 cannot do is say WHICH rule a wrong implementation followed, and it pins the
        // composition at exactly one point rather than pinning the rule. That is what this test
        // adds. At dr 0.8 all three separate, and these are the executed values:
        //   additive 0.8500000000000001  0.9  0.9500000000000001
        //   multiply 0.8400000000000001  0.8800000000000001  0.9199999999999999
        //   cut-pass 0.81  0.8200000000000001  0.8300000000000001
        double base = 0.8;

        assertEquals(0.8500000000000001, Bulwark.effectiveDr(base, I), EPS);
        assertEquals(0.9,                Bulwark.effectiveDr(base, II), EPS);
        assertEquals(0.9500000000000001, Bulwark.effectiveDr(base, III), EPS);

        // And explicitly NOT the two rivals, so the intent survives a later "simplification".
        for (double p : new double[]{I, II, III}) {
            assertNotEquals(base * (1.0 + p / 100.0), Bulwark.effectiveDr(base, p), EPS,
                    "Bulwark became MULTIPLICATIVE at " + p + "% -- additive was the decision");
            assertNotEquals(1.0 - (1.0 - base) * (1.0 - p / 100.0), Bulwark.effectiveDr(base, p), EPS,
                    "Bulwark began cutting the PASS-THROUGH at " + p + "% -- additive was the decision");
        }
    }

    @Test
    void theBoostedFractionIsWhatTheHitIsActuallyScaledBy() {
        // The composition end to end, because effectiveDr is only meaningful through applyBlock.
        // 6.749999999999999 is EXECUTED and is NOT 6.75 -- hence the epsilon. The boot gate's
        // witness prints %.4f, so it reads 6.7500.
        assertEquals(6.749999999999999, Shield.applyBlock(15.0, Bulwark.effectiveDr(ROUNDSHIELD, I)), EPS);
        assertEquals(6.0,               Shield.applyBlock(15.0, Bulwark.effectiveDr(ROUNDSHIELD, II)), EPS);
        assertEquals(5.25,              Shield.applyBlock(15.0, Bulwark.effectiveDr(ROUNDSHIELD, III)), EPS);

        // Unenchanted, for the ladder's first rung -- the Slice 1 number, unchanged.
        assertEquals(7.5, Shield.applyBlock(15.0, Bulwark.effectiveDr(ROUNDSHIELD, Bulwark.NONE)), EPS);
    }

    @Test
    void moreBulwarkNeverLetsMoreThroughAndNeverLeavesTheHitItCameFrom() {
        // Monotonic and bounded, swept rather than sampled: the property a curve author relies on.
        for (double dr = 0.0; dr <= 1.0; dr += 0.1) {
            double previous = Double.MAX_VALUE;
            for (double p = 0.0; p <= 100.0; p += 5.0) {
                double passed = Shield.applyBlock(15.0, Bulwark.effectiveDr(dr, p));
                assertTrue(passed <= previous + EPS,
                        "more Bulwark let MORE through at dr=" + dr + " p=" + p);
                assertTrue(passed >= 0.0, "negative damage at dr=" + dr + " p=" + p);
                assertTrue(passed <= 15.0 + EPS, "Bulwark amplified the hit at dr=" + dr + " p=" + p);
                previous = passed;
            }
        }
    }

    @Test
    void aHighDrShieldReachesTotalImmunityAndTheClampIsWhatStopsItThere() {
        // NOT a defensive branch -- a real constraint on future content, recorded in NEXT.md.
        // Executed: 0.9 + 0.15 is 1.05, which clamps to exactly 1.0, and a 15.0 hit passes 0.0.
        assertEquals(1.05, 0.9 + 0.15, EPS, "the raw sum really does exceed 1.0");
        assertEquals(1.0, Bulwark.effectiveDr(0.9, III));
        assertEquals(0.0, Shield.applyBlock(15.0, Bulwark.effectiveDr(0.9, III)));

        // And never PAST 1.0, which would flip the hit into a heal-shaped negative.
        assertEquals(1.0, Bulwark.effectiveDr(0.99, 1000));
    }

    @Test
    void theGuardsAreInheritedFromShieldClampRatherThanRestated() {
        // Unreachable from content -- EnchantDefinition refuses a negative percent at the boundary
        // -- and asserted anyway, because this is the second consumer of a clamp Slice 1 shipped
        // with only one, and a clamp with one caller has never been composed with anything.
        assertEquals(0.0, Bulwark.effectiveDr(ROUNDSHIELD, -1000));
        assertEquals(0.0, Bulwark.effectiveDr(ROUNDSHIELD, Double.NaN));
        assertEquals(0.0, Bulwark.effectiveDr(Double.NaN, III));

        // A large negative zeroes the SHIELD, not merely the bonus (0.5 + -10.0 = -9.5 -> NONE).
        // Honest composition rather than a special case; pinned so it is a decision, not a surprise.
        assertEquals(0.0, Bulwark.effectiveDr(ROUNDSHIELD, -1000));
    }

    @Test
    void onlyAPositiveBonusDeclaresABoost() {
        // The rider's fast path: an unenchanted block must skip the composition entirely.
        assertFalse(Bulwark.boosts(Bulwark.NONE));
        assertFalse(Bulwark.boosts(-5));
        assertTrue(Bulwark.boosts(I));
        assertTrue(Bulwark.boosts(0.0001));
    }
}
