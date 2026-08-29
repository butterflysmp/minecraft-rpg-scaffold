package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.Shield;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bulwark's composition rule, and above all the fact that it is ADDITIVE rather than either of the
 * two readings it is easily confused with. Those two coincide at dr 0.5 -- the base this shield
 * used to carry -- and separate at the 0.35 it carries now, so both bases are asserted.
 *
 *
 * <p>Every constant below was produced by RUNNING the expression against the real
 * {@code Shield.clamp}, never by hand. Three float claims made from reasoning in this project have
 * all been wrong.
 */
class BulwarkTest {

    private static final double EPS = 1e-9;

    /** The shipped shield. */
    private static final double SHIELD = 0.35;

    /**
     * The base the shipped shield used to carry, kept as a named VALUE rather than as "the shield".
     * It is where the two rejected readings coincide, which is a real property of 0.5 and the reason
     * this test still asserts there -- see theCompositionIsADDITIVE... below.
     */
    private static final double LEGACY_HALF = 0.5;

    /** The shipped curve, percent by level: I, II, III. */
    private static final double I = 5, II = 10, III = 15;

    @Test
    void theShippedCurveOnTheShippedShieldIsTheLadderTheBootGateReads() {
        // EXECUTED, and NOT the clean 0.40 / 0.45 they look like: 0.35 + 0.05 in binary floating
        // point is 0.39999999999999997. The tooltip's one-decimal rounding is what renders these as
        // "40%" / "45%" / "50%". Do not "tidy" these constants -- they are what the code returns.
        assertEquals(0.39999999999999997, Bulwark.effectiveDr(SHIELD, I));
        assertEquals(0.44999999999999996, Bulwark.effectiveDr(SHIELD, II));
        assertEquals(0.5,                 Bulwark.effectiveDr(SHIELD, III));
    }

    @Test
    void anUnenchantedShieldIsUntouchedRatherThanRecomputed() {
        // The branch every shield in the game takes. NONE must be a true identity on the base DR,
        // or a plain block silently changes the day this class ships.
        assertEquals(SHIELD, Bulwark.effectiveDr(SHIELD, Bulwark.NONE));
        for (double dr = 0.0; dr <= 1.0; dr += 0.05) {
            assertEquals(Shield.clamp(dr), Bulwark.effectiveDr(dr, Bulwark.NONE), EPS,
                    "a zero bonus changed the block fraction at dr=" + dr);
        }
    }

    @Test
    void theCompositionIsADDITIVEAndNotEitherReadingThatMatchesItAtAHalf() {
        // THE SHIPPED BASE MOVED FROM 0.5 TO 0.35, AND THAT CHANGED WHAT THIS TEST IS FOR.
        //
        // At 0.5 the two REJECTED readings were bit-identical to each other -- multiplicative
        // (dr * (1+p)) and diminishing (1-(1-dr)(1-p)) both gave 0.525 / 0.55 / 0.575 -- so a
        // shipped-shield assertion could tell "wrong" from "right" but never which wrong.
        //
        // At 0.35 they separate. Executed: additive 0.39999999999999997, multiplicative 0.3675,
        // diminishing 0.38250000000000006 -- all three distinct at every level. So the shipped
        // shield now discriminates the rule by itself, which the old one could not.
        //
        // Both bases are still asserted, for different jobs. 0.5 is kept as LEGACY_HALF because the
        // coincidence there is a real property worth guarding -- it is the case where a test could
        // most easily look green while being blind -- and 0.8 keeps a third, unrelated base so the
        // rule is pinned rather than two points. These are the executed values at 0.8:
        //   additive 0.8500000000000001  0.9  0.9500000000000001
        //   multiply 0.8400000000000001  0.8800000000000001  0.9199999999999999
        //   cut-pass 0.81  0.8200000000000001  0.8300000000000001
        double base = 0.8;

        assertEquals(0.8500000000000001, Bulwark.effectiveDr(base, I), EPS);
        assertEquals(0.9,                Bulwark.effectiveDr(base, II), EPS);
        assertEquals(0.9500000000000001, Bulwark.effectiveDr(base, III), EPS);

        // And at the SHIPPED base, which now separates them too -- the property 0.5 lacked.
        for (double p : new double[]{I, II, III}) {
            double additive = Bulwark.effectiveDr(SHIELD, p);
            double multiplicative = Shield.clamp(SHIELD * (1.0 + p / 100.0));
            double diminishing = Shield.clamp(1.0 - (1.0 - SHIELD) * (1.0 - p / 100.0));
            assertNotEquals(multiplicative, additive, EPS, "shipped base stopped separating at " + p);
            assertNotEquals(diminishing, additive, EPS, "shipped base stopped separating at " + p);
            assertNotEquals(multiplicative, diminishing, EPS,
                    "the two REJECTED readings coincide at the shipped base -- they did at 0.5, and "
                            + "the whole point of 0.35 is that they no longer do");
        }

        // At LEGACY_HALF they DO coincide, which is the property that base is kept for.
        for (double p : new double[]{I, II, III}) {
            assertEquals(Shield.clamp(LEGACY_HALF * (1.0 + p / 100.0)),
                    Shield.clamp(1.0 - (1.0 - LEGACY_HALF) * (1.0 - p / 100.0)), EPS,
                    "at 0.5 the two rejected readings are supposed to be indistinguishable");
        }

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
        // 9.000000000000002 is EXECUTED and is NOT 9.0 -- hence the epsilon. The boot gate's
        // witness prints %.4f, so it reads 9.0000.
        assertEquals(9.000000000000002, Shield.applyBlock(15.0, Bulwark.effectiveDr(SHIELD, I)), EPS);
        assertEquals(8.25,              Shield.applyBlock(15.0, Bulwark.effectiveDr(SHIELD, II)), EPS);
        assertEquals(7.5,               Shield.applyBlock(15.0, Bulwark.effectiveDr(SHIELD, III)), EPS);

        // Unenchanted, the ladder's first rung: a 15.0 hit passes 9.75 through a 0.35 shield.
        assertEquals(9.75, Shield.applyBlock(15.0, Bulwark.effectiveDr(SHIELD, Bulwark.NONE)), EPS);
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
        assertEquals(0.0, Bulwark.effectiveDr(SHIELD, -1000));
        assertEquals(0.0, Bulwark.effectiveDr(SHIELD, Double.NaN));
        assertEquals(0.0, Bulwark.effectiveDr(Double.NaN, III));

        // A large negative zeroes the SHIELD, not merely the bonus (0.35 + -10.0 = -9.65 -> NONE).
        // Honest composition rather than a special case; pinned so it is a decision, not a surprise.
        assertEquals(0.0, Bulwark.effectiveDr(SHIELD, -1000));
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
