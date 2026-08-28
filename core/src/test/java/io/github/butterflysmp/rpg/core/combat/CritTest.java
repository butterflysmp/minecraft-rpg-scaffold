package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The crit roll and multiplier, on exact boundary values rather than on a random source.
 *
 * Every expected value below was produced by EXECUTING the expression and pasting what it printed,
 * never by reasoning about the arithmetic -- the standing rule in DefenseTest and AttackChargeTest.
 * The exact-equality assertions are exact BECAUSE execution showed they are: 8 * 2.0 == 16.0 and
 * 14.2 * 2 == 28.4 both hold in binary floating point, and were checked before being written down.
 *
 * The boundary pair (0.1499 crits at 15%, 0.15 does not) is the whole point of taking the roll as a
 * parameter: with ThreadLocalRandom inside, neither row could be written at all, and the < vs <=
 * question -- immeasurable in play -- would be settled by nothing.
 *
 * Each test names the mutation it forces red.
 */
class CritTest {

    private static final double EPS = 1e-9;

    // --- The boundary, which is why the roll is a parameter ---

    @Test
    void theRollIsBelowTheChanceToCritAndTheChanceItselfIsNotBelowItself() {
        assertTrue(Crit.crits(0.15, 0.1499), "just under 15% crits");
        assertFalse(Crit.crits(0.15, 0.15), "exactly 15% does NOT -- the roll is half-open [0,1)");
        assertTrue(Crit.crits(0.15, 0.0), "the bottom of the range always crits");
        assertFalse(Crit.crits(0.15, 0.9999), "the top never does");
        // Mutation: < -> <= -> crits(0.15, 0.15) becomes true, the critting set gains one point and
        // the observed rate stops being exactly the stat -> reddens on the second assertion.
    }

    @Test
    void theMultiplierIsOneAddTheBonusOnACritAndExactlyOneOtherwise() {
        // Exact, not EPS: execution confirms these are exact in binary floating point. The non-crit
        // 1.0 matters most -- it is applied to EVERY hit, so an approximate identity would drift the
        // whole game's damage on the ~85% of swings that do not crit.
        assertEquals(2.0, Crit.multiplier(0.15, Crit.BASE_DAMAGE, 0.1499));
        assertEquals(Crit.NO_CRIT, Crit.multiplier(0.15, Crit.BASE_DAMAGE, 0.15));
        assertEquals(1.0, Crit.NO_CRIT, "a non-crit must be an exact identity, not merely close");
        // Mutation: return critDamage rather than 1 + critDamage -> a base crit deals 1.0x, i.e.
        // critting stops doing anything at all while still reporting itself -> reddens.
    }

    @Test
    void aBaseCritIsExactlyDoubleAndTheBonusIsWhatGearRaises() {
        assertEquals(2.0, Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.0), EPS,
                "base 0.15 / +1.0 -> 2.0x");
        assertEquals(2.5, Crit.multiplier(Crit.BASE_CHANCE, 1.5, 0.0), EPS,
                "a +0.5 crit-damage boost -> 2.5x, stacking additively like every other stat");
        assertEquals(16.0, 8.0 * Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.0),
                "the ironblade's 8, critting");
        // Mutation: make BASE_DAMAGE a multiplier (2.0) rather than a bonus -> a base crit becomes
        // 3.0x and every gear boost is off by one whole hit -> reddens.
    }

    // --- The clamp, at both ends ---

    @Test
    void aChanceAtOrAboveOneAlwaysCritsRatherThanOverflowingIntoASecondTier() {
        assertTrue(Crit.crits(1.0, 0.999999), "100% crits on the highest roll the source can give");
        assertTrue(Crit.crits(2.0, 0.999999), "and 200% is capped to that, not to something stronger");
        assertEquals(1.0, Crit.chance(2.0), EPS);
        assertEquals(1.0, Crit.chance(1.0), EPS);
        // Mutation: drop the Math.min -> chance(2.0) is 2.0. Still always crits, so `crits` alone
        // cannot see it -- which is why chance() is asserted directly here -> reddens on chance(2.0).
    }

    @Test
    void aNegativeChanceNeverCritsOnPurposeRatherThanByAccident() {
        assertFalse(Crit.crits(-1.0, 0.0), "the lowest possible roll still does not crit");
        assertEquals(0.0, Crit.chance(-1.0), EPS);
        assertEquals(Crit.NO_CRIT, Crit.multiplier(-1.0, 5.0, 0.0), EPS,
                "and a large crit damage cannot be reached through a negative chance");
        // Mutation: drop the Math.max -> chance(-1.0) is -1.0. `crits` still returns false, so the
        // behaviour is unchanged and only the direct chance() assertion catches it -> reddens.
    }

    /** Zero chance is the mob case, and it must hold for EVERY roll, not just a sampled one. */
    @Test
    void aZeroChanceNeverCritsForAnyRollWhichIsWhatMakesAMobsHitsNeverCrit() {
        for (double roll = 0.0; roll < 1.0; roll += 0.01) {
            assertFalse(Crit.crits(0.0, roll), "a 0% dealer must never crit, at roll " + roll);
            assertEquals(Crit.NO_CRIT, Crit.multiplier(0.0, Crit.BASE_DAMAGE, roll), EPS);
        }
        // Mutation: <= instead of < -> a roll of exactly 0.0 crits at 0% chance, so a mob crits on
        // one draw in 2^53 -- unreachable in play, caught here -> reddens on the first iteration.
    }

    // --- Shape ---

    @Test
    void aHigherChanceCritsOnStrictlyMoreRollsAndNeverOnFewer() {
        for (double roll = 0.0; roll < 1.0; roll += 0.05) {
            boolean atBase = Crit.crits(0.15, roll);
            boolean boosted = Crit.crits(0.45, roll);
            assertTrue(!atBase || boosted,
                    "a crit-chance boost must never REMOVE a crit, at roll " + roll);
        }
        assertTrue(Crit.crits(0.45, 0.30), "and it adds rolls the base chance refused");
        assertFalse(Crit.crits(0.15, 0.30));
        // Mutation: invert the comparison to roll > chance -> the boost inverts, a crit-chance item
        // makes you crit LESS, and the boot's "gear raises the rate" row silently reverses -> reddens.
    }
}
