package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.Defense;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protection: the flat-points bonus on a piece's Defense.
 *
 * The headline is {@link #stackingAcrossAFullSetGetsNowhereNearImmunity} -- the one property that
 * makes per-piece stacking safe to ship at all. Bulwark needed a clamp because a block fraction can
 * reach 1.0; Defense cannot, because the curve is asymptotic, and this pins that rather than
 * asserting it in prose.
 *
 * Every number below was EXECUTED against the real classes and pasted, never derived.
 *
 * Each test names the mutation it forces red.
 */
class ProtectionTest {

    private static final double EPS = 1e-9;

    // --- The arithmetic ---------------------------------------------------------------------------

    @Test
    void aPieceCarriesItsOwnBonusOnTopOfItsMaterialPoints() {
        // A diamond chestplate is 8 points. Protection I/II/III take it to 11/14/17.
        assertEquals(11.0, Protection.effectiveDefense(8, 3), EPS);
        assertEquals(14.0, Protection.effectiveDefense(8, 6), EPS);
        assertEquals(17.0, Protection.effectiveDefense(8, 9), EPS);
        // Mutation: multiply instead of add -> 8 * 3 = 24 at level I -> reddens.
    }

    @Test
    void aPieceWithNoProtectionIsUntouched() {
        assertEquals(8.0, Protection.effectiveDefense(8, Protection.NONE), EPS);
        assertFalse(Protection.boosts(Protection.NONE), "zero declares no bonus");
        assertTrue(Protection.boosts(0.5), "anything above zero does");
        // Mutation: make boosts() use >= -> an unenchanted piece claims a bonus -> reddens.
    }

    @Test
    void aCosmeticPieceWorthNothingStillTakesTheBonus() {
        // A zero-defense piece is legal content (ArmorDefinition permits it), and Protection on one
        // must grant its points rather than multiplying nothing by something.
        assertEquals(9.0, Protection.effectiveDefense(0, 9), EPS);
        // Mutation: gate the addition on base > 0 -> reddens.
    }

    // --- The property that makes stacking safe ----------------------------------------------------

    @Test
    void stackingAcrossAFullSetGetsNowhereNearImmunity() {
        // THE HEADLINE. Four pieces, each summed by the reconciler, is the largest Defense this
        // slice can produce: full diamond (20) plus Protection III on every slot (36).
        double fullSet = Protection.effectiveDefense(3, 9)     // helmet  3 -> 12
                + Protection.effectiveDefense(8, 9)            // chest   8 -> 17
                + Protection.effectiveDefense(6, 9)            // legs    6 -> 15
                + Protection.effectiveDefense(3, 9);           // boots   3 -> 12
        assertEquals(56.0, fullSet, EPS, "20 material points plus 36 of Protection");

        // And what that is actually worth, executed through the real curve rather than reasoned:
        assertEquals(0.358974358974359, Defense.damageReduction(fullSet), EPS,
                "the maximum this slice can reach is about 35.9% -- roughly double bare diamond");
        assertEquals(0.16666666666666666, Defense.damageReduction(20), EPS, "bare diamond, for scale");
        assertTrue(Defense.damageReduction(fullSet) < 0.5,
                "a fully-enchanted set does not even halve a hit, which is why no clamp is needed");
        // Mutation: none available -- this asserts a property of Defense's curve, and it is here
        // rather than in DefenseTest because it is PROTECTION's justification for having no clamp.
    }

    @Test
    void evenAnAbsurdBonusCannotReachImmunityBecauseTheCurveIsAsymptotic() {
        // Bulwark needs Shield.clamp because block_dr 0.9 + 0.15 is 1.05, and a 15.0 hit then passes
        // 0.0. Protection needs no equivalent, and this is why: there is no finite points value that
        // turns a hit into nothing.
        for (double absurd : new double[] {100, 1_000, 10_000, 1e9}) {
            double reduction = Defense.damageReduction(Protection.effectiveDefense(20, absurd));
            assertTrue(reduction < 1.0, "still not immune at +" + absurd + ", got " + reduction);
            assertTrue(Defense.applyDefense(100, Protection.effectiveDefense(20, absurd)) > 0.0,
                    "a hit still lands at +" + absurd);
        }
        // Mutation: give Protection a clamp "for safety" -> harmless here, but it would cap the stat
        // where the curve already does the job; this test is what says the clamp is unnecessary.
    }
}
