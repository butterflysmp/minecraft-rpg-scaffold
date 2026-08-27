package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The defense curve is pure math, so it is pinned exactly here rather than boot-witnessed. The
 * headline is {@link #theTwoCurvesAreOneCurve} -- if mitigation and the bar ever disagree, the number
 * a player reads stops predicting the damage they take, and nothing on screen would say so.
 *
 * Every expected value below was produced by EXECUTING the expression against the real class and
 * pasting what it printed, never by reasoning about the arithmetic. That is not ceremony: the
 * identity in {@link #theTwoCurvesAreOneCurve} is NOT exact in binary floating point -- it differs by
 * about 3.6e-15 at defense 50 -- so a test written from the algebra would have asserted exact
 * equality and failed on a correct implementation.
 *
 * Each test names the mutation it forces red.
 */
class DefenseTest {

    private static final double EPS = 1e-9;

    // --- The curve ---------------------------------------------------------------------------

    @Test
    void oneHundredDefenceIsTheHalfLifeOnBothReadings() {
        // The one value a player is meant to be able to hold in their head. If it drifts, every
        // intuition built on it is wrong, and the drift is invisible without a number to check.
        assertEquals(0.5, Defense.damageReduction(100), EPS, "100 defense turns away exactly half");
        assertEquals(5.0, Defense.applyDefense(10, 100), EPS, "so a 10 hit lands as 5");
        // Mutation: change SCALE away from 100 -> the half-life moves off 100 -> reddens.
    }

    @Test
    void fullVanillaDiamondIsDeliberatelyLow() {
        // Diamond is STARTER gear here. A starter set that halved damage would leave real gear
        // nothing to grant. About one sixth is the decision; pinning it makes a later buff a visible
        // edit rather than a silent drift.
        assertEquals(0.16666666666666666, Defense.damageReduction(20), EPS,
                "full diamond (3+8+6+3 = 20 armor) turns away about one sixth, not half");
        assertEquals(83.33333333333333, Defense.applyDefense(100, 20), EPS,
                "so a 100 hit still lands for about 83 in a full diamond set");
        // Mutation: rescale so diamond reads about 50% -> reddens.
    }

    @Test
    void theDiamondSetClimbsPieceByPieceTheWayTheBootGateReads() {
        // The gate equips helmet -> chest -> legs -> boots and watches the number climb 3, 11, 17, 20.
        // Asserted here so the gate reads a curve someone already checked, rather than discovering it.
        assertEquals(0.02912621359223301, Defense.damageReduction(3), EPS, "helmet alone");
        assertEquals(0.0990990990990991, Defense.damageReduction(11), EPS, "plus chestplate");
        assertEquals(0.1452991452991453, Defense.damageReduction(17), EPS, "plus leggings");
        assertEquals(0.16666666666666666, Defense.damageReduction(20), EPS, "plus boots, the full set");
        // Mutation: any change to SCALE or the curve shape -> the whole ladder shifts -> reddens.
    }

    @Test
    void noDefenceReducesNothingAtAll() {
        // An untracked combatant resolves to 0 defense (the summand convention in CombatantStats), and
        // EVERY mob is untracked for defense. If 0 did not mean untouched, this pass would silently
        // nerf every hit in the game.
        assertEquals(10.0, Defense.applyDefense(10, 0), EPS, "no defense, no reduction");
        assertEquals(0.0, Defense.damageReduction(0), EPS, "and nothing to draw on the bar");
        // Mutation: see the negative-defense test -- the same guard is the only thing covering it.
    }

    @Test
    void negativeDefenceIsGuardedRatherThanAmplifyingTheHit() {
        // A future armor-shred debuff could push defense below zero. Without the guard the divisor
        // drops under 1 and the "reduction" becomes a damage BUFF -- a debuff that loops around into
        // the strongest buff in the game. At exactly -100 it divides by zero.
        assertEquals(10.0, Defense.applyDefense(10, -50), EPS,
                "negative defense leaves the hit alone; it must never multiply it up");
        assertEquals(10.0, Defense.applyDefense(10, -100), EPS,
                "-100 would divide by zero without the guard");
        assertEquals(0.0, Defense.damageReduction(-50), EPS, "and reads as no reduction, never negative");
        // Mutation: drop the defense<=0 guard -> -50 gives 10*100/50 = 20, DOUBLE damage -> reddens.
    }

    @Test
    void reductionRisesWithDefenceAndApproachesButNeverReachesTotalImmunity() {
        // Asymptotic, not capped. A cap would make "immune" reachable, and an immune player is a
        // stuck fight. Monotonic, so more armor is never worse than less.
        double previous = -1.0;
        for (double defense : new double[] {0, 1, 20, 100, 300, 900, 10_000, 1e12}) {
            double reduction = Defense.damageReduction(defense);
            assertTrue(reduction > previous, "reduction must rise with defense, at " + defense);
            assertTrue(reduction < 1.0, "and never reach total immunity, at " + defense);
            assertTrue(Defense.applyDefense(100, defense) > 0.0,
                    "so a hit always lands for something, at " + defense);
            previous = reduction;
        }
        // Mutation: clamp reduction at 1.0, or subtract flat -> immunity becomes reachable and a hit
        // lands for 0 (or heals) -> reddens.
    }

    @Test
    void theTwoCurvesAreOneCurve() {
        // THE headline. Mitigation uses the surviving fraction; the bar draws the removed fraction.
        // If they drift apart the bar becomes a lie about the damage taken -- and a lie no in-game
        // check would catch, because both halves would still look individually sensible.
        //
        // Asserted with EPS, NOT exact equality: the two expressions are algebraically identical but
        // not bit-identical in binary. Executed and observed, they differ by about 3.6e-15 at
        // defense 50. A test demanding == here would fail on correct code.
        for (double defense : new double[] {0, 1, 3, 11, 17, 20, 50, 100, 300, 900, 1e6}) {
            assertEquals(37.5 * (1 - Defense.damageReduction(defense)),
                    Defense.applyDefense(37.5, defense), EPS,
                    "the bar's fraction and the damage's fraction must agree, at defense " + defense);
        }
        // Mutation: change SCALE in ONE of the two methods -> they part company -> reddens.
    }

    // --- The armor bar -----------------------------------------------------------------------

    @Test
    void armorBarPointsSpreadTheReductionAcrossAFullBar() {
        assertEquals(0.0, Defense.armorBarPoints(0), EPS, "no defense draws an empty bar");
        assertEquals(3.333333333333333, Defense.armorBarPoints(20), EPS,
                "full diamond fills about 3.33 of 20 points -- about 1.7 of the 10 icons, one sixth");
        assertEquals(10.0, Defense.armorBarPoints(100), EPS, "the half-life fills exactly half the bar");
        // Mutation: draw the bar from raw armor instead of DR -> full diamond fills 20/20 -> reddens.
    }

    @Test
    void theBarModifierCancelsTheNativeArmorSumExactly() {
        // The whole override: native + modifier must land on armorBarPoints, or the bar reads
        // material plus a fudge instead of damage reduction.
        assertEquals(-16.666666666666668, Defense.barModifier(20, 20), EPS,
                "full diamond natively sums 20 points and wants 3.33, so the modifier is about -16.67");
        for (double defense : new double[] {0, 3, 11, 17, 20}) {
            assertEquals(Defense.armorBarPoints(defense), defense + Defense.barModifier(defense, defense),
                    EPS, "native + modifier must equal the DR bar, at defense " + defense);
        }
        assertEquals(0.0, Defense.barModifier(0, 0), EPS,
                "bare, the modifier is a no-op -- nothing to cancel and nothing to draw");
        // Mutation: return armorBarPoints without subtracting native -> the bar reads DR ON TOP of
        // material, so full diamond overfills -> reddens.
    }

    @Test
    void theBarNeverApproachesTheVanillaThirtyPointClamp() {
        // armorBarPoints is bounded by [0,20) because damageReduction is bounded by [0,1). If it could
        // exceed 30 the attribute would clamp and the bar would silently stop tracking DR at the top.
        for (double defense : new double[] {0, 20, 100, 10_000, 1e12}) {
            double points = Defense.armorBarPoints(defense);
            assertTrue(points >= 0.0 && points < Defense.FULL_ARMOR_BAR_POINTS,
                    "bar points must stay inside [0,20), at defense " + defense + " got " + points);
        }
        // Mutation: scale by more than FULL_ARMOR_BAR_POINTS -> a huge defense passes 30 and the
        // attribute clamps -> reddens.
    }
}
