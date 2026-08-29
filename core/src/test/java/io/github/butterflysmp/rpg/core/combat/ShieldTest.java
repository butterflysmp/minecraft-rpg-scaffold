package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The block fraction: what gets through a raised shield.
 *
 * The headline is {@link #blockAndArmorBothApplyRatherThanOneShadowingTheOther} -- block and
 * armor are two mitigations on one hit, and the whole claim of this slice is that they COMPOSE rather than one
 * shadowing the other. If they ever stopped composing, a shielded player in armor would quietly take
 * one mitigation's worth and nothing on screen would say which.
 *
 * The two clamp tests are close behind, and they guard opposite catastrophes rather than one bound
 * twice: a negative fraction DOUBLES the hit, and a fraction above one makes the damage NEGATIVE,
 * which reaches {@code CombatantStats.damage} and heals the victim. Both are reachable from a
 * hand-edited content file, which is why the clamp is in the arithmetic and not only in the loader.
 *
 * Every expected value below was produced by EXECUTING the expression against the real class and
 * pasting what it printed, never by reasoning about the arithmetic -- the standing rule in
 * DefenseTest and AttackChargeTest. That rule earned its keep here twice. The composition test was
 * drafted asserting the two orderings are IDENTICAL, on the reasoning that multiplication commutes;
 * executed over 22400 combinations they differ in 4780 of them, by up to 2.842170943040401e-14
 * (worst case damage 123.5, block 0.3, defense 1) -- because swapping those steps is reassociation,
 * not commutation. And applyBlock(8, 0.3) == 5.6 exactly, which looks like a rounding accident and
 * is not: it was checked before being relied on.
 *
 * Each test names the mutation it forces red.
 */
class ShieldTest {

    private static final double EPS = 1e-9;

    // --- The fraction ------------------------------------------------------------------------

    @Test
    void aBlockedHitLandsTheDeclaredFractionAndNothingMore() {
        // The common shield's 0.5: half stopped, half through. This is the one number a player is
        // meant to be able to hold, and the whole of the shipped shield's mechanical identity.
        // Exact, not EPS: execution confirms 8.0 * 0.5 == 4.0 in binary floating point.
        assertEquals(4.0, Shield.applyBlock(8.0, 0.5));
        assertEquals(3.5, Shield.applyBlock(7.0, 0.5), EPS, "an odd hit halves cleanly too");
        assertEquals(500.0, Shield.applyBlock(1000.0, 0.5), EPS, "and the fraction does not tire");
        // Mutation: divide instead of multiply -> applyBlock(8, 0.5) = 16.0, a shield that DOUBLES
        // the hit it stopped -> reddens.
    }

    @Test
    void theFractionStoppedAndTheFractionPassedAreOneNumberReadTwoWays() {
        // The content file authors the fraction STOPPED; the arithmetic needs the fraction PASSED.
        // If these two drift, the tooltip stops predicting the damage -- the same failure
        // DefenseTest guards with theTwoCurvesAreOneCurve, and invisible for the same reason: both
        // halves would still look individually sensible.
        for (double blockDr : new double[] {0.0, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0}) {
            assertEquals(37.5 * Shield.passThrough(blockDr), Shield.applyBlock(37.5, blockDr), EPS,
                    "the passed fraction and the applied damage must agree, at " + blockDr);
        }
        assertEquals(0.75, Shield.passThrough(0.25), EPS,
                "a quarter stopped is three quarters through");
        // Mutation: change the 1 - x flip in ONE of the two methods -> they part company -> reddens.
    }

    @Test
    void aShieldThatDeclaresNoBlockDoesNothingRatherThanSomethingSubtle() {
        // A shield authored with block_dr absent or 0 must leave the hit EXACTLY alone. Anything
        // else and "no block" becomes a small silent block nobody wrote down.
        assertEquals(8.0, Shield.applyBlock(8.0, Shield.NONE), EPS);
        assertEquals(1.0, Shield.passThrough(Shield.NONE), EPS, "everything gets through");
        assertEquals(0.0, Shield.applyBlock(0.0, 0.5), EPS, "and a zero hit stays zero when blocked");
        // Mutation: make NONE anything but 0.0, or add a floor to passThrough the way
        // AttackCharge.MIN_SCALE is a floor -> a shield that declares no block blocks anyway
        // -> reddens.
    }

    @Test
    void aTotalBlockStopsEverythingWithoutGoingPastZero() {
        // 1.0 is the upper clamp, and it must land on exactly nothing rather than overshooting into
        // a heal. No shipped shield declares it -- this pins what the bound MEANS.
        assertEquals(0.0, Shield.applyBlock(8.0, Shield.FULL), EPS);
        assertEquals(0.0, Shield.passThrough(Shield.FULL), EPS);
        // Mutation: clamp the upper bound anywhere above 1.0 -> passThrough goes negative and the
        // hit heals -> reddens.
    }

    // --- The clamp, which fails in BOTH directions ---------------------------------------------

    @Test
    void aNegativeBlockDrIsGuardedRatherThanAmplifyingTheHit() {
        // block_dr: -1 in a hand-edited content file. Unclamped this is 8 * (1 - (-1)) = 16: a
        // shield that DOUBLES the hit it was raised to stop. The loader also rejects the range, and
        // that is not this guard's job -- this one stands between an ALREADY-MINTED item and the
        // arithmetic, where no loader runs.
        assertEquals(8.0, Shield.applyBlock(8.0, -1.0), EPS,
                "a negative fraction leaves the hit alone; it must never multiply it up");
        assertEquals(1.0, Shield.passThrough(-1.0), EPS, "and reads as nothing blocked, never more");
        assertEquals(8.0, Shield.applyBlock(8.0, -1000.0), EPS, "however far below zero it goes");
        // Mutation: drop the lower clamp in Shield.clamp -> applyBlock(8, -1) = 16.0, DOUBLE damage
        // from a shield -> reddens.
    }

    @Test
    void aBlockDrAboveOneIsClampedRatherThanHealingTheVictim() {
        // The worse direction. block_dr: 2 unclamped is 8 * (1 - 2) = -8, and negative damage
        // reaches CombatantStats.damage and HEALS -- a shield that makes you stronger the more you
        // are hit. It looks like a feature until someone stands in a swarm and never dies.
        assertEquals(0.0, Shield.applyBlock(8.0, 2.0), EPS,
                "an over-full fraction stops everything; it must never return a negative");
        assertEquals(0.0, Shield.applyBlock(8.0, 1000.0), EPS, "however far above one it goes");
        assertEquals(0.0, Shield.passThrough(2.0), EPS, "and nothing gets through, never a negative");
        // Mutation: drop the Math.min in Shield.clamp -> applyBlock(8, 2) = -8.0, a hit that heals
        // -> reddens.
    }

    @Test
    void aNaNBlockDrIsRejectedRatherThanPoisoningTheDamage() {
        // NaN is the third bad input and it does not behave like the other two: it fails EVERY
        // comparison, so a Math.max/Math.min pair written the natural way round propagates it. A
        // NaN hit does not reduce a health bar -- it makes it a value no comparison is true about,
        // so the player is neither alive nor dead in any branch that checks.
        assertEquals(8.0, Shield.applyBlock(8.0, Double.NaN), EPS, "NaN blocks nothing at all");
        assertEquals(0.0, Shield.clamp(Double.NaN), EPS, "and clamps to NONE, not to itself");
        // Mutation: write the guard as Math.max(NONE, blockDr) -> Math.max propagates NaN, so
        // applyBlock returns NaN and the health bar takes a value it can never compare -> reddens.
    }

    // --- The predicate -------------------------------------------------------------------------

    @Test
    void onlyAPositiveFractionDeclaresABlock() {
        assertTrue(Shield.blocks(0.5));
        assertTrue(Shield.blocks(0.001), "any declared fraction blocks, however small");
        assertFalse(Shield.blocks(Shield.NONE), "absent means no block");
        assertFalse(Shield.blocks(-1.0), "and a nonsense value is not a block either");
        assertFalse(Shield.blocks(Double.NaN), "nor is NaN");
        // Mutation: >= instead of > -> an absent block reads as a declared one, so every shield
        // "blocks" for zero and the rider's witness fires on every hit in the game -> reddens.
    }

    // --- Shape -------------------------------------------------------------------------------

    @Test
    void theReducedHitFallsWithTheFractionAndNeverLeavesTheHitItCameFrom() {
        // Bounded and monotonic. The upper bound is what stops a block amplifying; the lower bound
        // is what stops it healing. Fixed-point checks at 0 and 1 alone would catch neither in the
        // middle of the range.
        for (double damage : new double[] {0.0, 0.5, 8.0, 37.5, 100.0, 1e6}) {
            double previous = Double.MAX_VALUE;
            for (double blockDr = 0.0; blockDr <= 1.0 + EPS; blockDr += 0.05) {
                double reduced = Shield.applyBlock(damage, blockDr);
                assertTrue(reduced <= previous + EPS,
                        "must fall as the block rises, at " + damage + "/" + blockDr);
                assertTrue(reduced >= 0.0,
                        "a block never heals, at " + damage + "/" + blockDr + " got " + reduced);
                assertTrue(reduced <= damage + EPS,
                        "a block never amplifies, at " + damage + "/" + blockDr + " got " + reduced);
                previous = reduced;
            }
        }
        // Mutation: add a constant to the return -> reduced > damage at blockDr 0 for a small hit
        // -> reddens on the upper bound, which the fixed-point tests above would not catch.
    }

    // --- Composition with armor, which is the whole point ---------------------------------------

    @Test
    void blockAndArmorBothApplyRatherThanOneShadowingTheOther() {
        // THE headline, and the ladder the boot gate reads off rather than discovers. An 8-damage
        // mob hit against a player in full vanilla diamond (20 armor, DefenseTest's own number)
        // holding the common shield:
        //
        //   unblocked, unarmored  8.0
        //   armored only          6.666666666666667
        //   blocked only          4.0
        //   both                  3.3333333333333335
        //
        // Both must be strictly less than either alone. If block ever shadowed armor -- or armor
        // silently ate the block -- the "both" figure would land on one of the middle two, and
        // nothing in game would say which mitigation had gone missing.
        double raw = 8.0;
        double armoredOnly = Defense.applyDefense(raw, 20);
        double blockedOnly = Shield.applyBlock(raw, 0.5);
        double both = Defense.applyDefense(Shield.applyBlock(raw, 0.5), 20);

        assertEquals(6.666666666666667, armoredOnly, EPS, "full diamond alone");
        assertEquals(4.0, blockedOnly, EPS, "the common shield alone");
        assertEquals(3.3333333333333335, both, EPS, "block then armor, both applied");

        assertTrue(both < blockedOnly - EPS, "both must beat the shield alone");
        assertTrue(both < armoredOnly - EPS, "and must beat the armor alone");
        // Mutation: drop either factor from the composition -- return applyDefense(raw, 20) or
        // applyBlock(raw, 0.5) -> `both` lands on 6.67 or 4.0 and one of the two strict
        // comparisons -> reddens.
        //
        // NOT a mutation this test catches, and it was RUN to find that out: swapping the two
        // steps here to applyBlock(applyDefense(raw, 20), 0.5) leaves all 11 tests GREEN. At these
        // three values the orderings are bit-identical, and everywhere else they differ by at most
        // 2.8e-14 -- below any epsilon worth asserting. THE ORDER IS NOT OBSERVABLE IN THE
        // ARITHMETIC and no test here can guard it. It is fixed by the PIPELINE instead: the block
        // is applied in the rider and defense a thread hop later inside CombatantStats.damage,
        // with no call site at which they could be swapped. Do not rename this test back to
        // claiming an order it does not check.
    }

    @Test
    void theCompositionIsAssertedWithAnEpsilonBecauseSwappingTheStepsIsNotBitExact() {
        // Recorded because the draft of the test above asserted the two orderings are IDENTICAL, on
        // the reasoning that multiplication commutes. It does -- but swapping these two steps is
        // REASSOCIATION, not commutation: (d*0.5)*100/120 against (d*100/120)*0.5. Executed over
        // 22400 combinations of damage, fraction and defense the two differ in 4780, by at most
        // 2.842170943040401e-14. The worst case is pinned here so the claim stays a measurement.
        double blockFirst = Defense.applyDefense(Shield.applyBlock(123.5, 0.3), 1);
        double armorFirst = Shield.applyBlock(Defense.applyDefense(123.5, 1), 0.3);
        assertEquals(85.59405940594057, blockFirst, EPS, "block then armor, the shipped order");
        assertEquals(85.5940594059406, armorFirst, EPS, "armor then block, the road not taken");
        assertTrue(blockFirst != armorFirst,
                "the two orderings are NOT bit-identical -- if this ever passes as equal, the "
                        + "epsilon in the composition test above has stopped being necessary and "
                        + "somebody should find out why");
        assertEquals(0.0, Math.abs(blockFirst - armorFirst), 1e-13,
                "but they agree far below anything a player could see");
        // Mutation: rewrite the headline test to assert blockFirst == armorFirst exactly -> that
        // test reddens on correct code, which is the failure this one exists to have already found.
    }
}
