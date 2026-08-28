package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sweep fraction: what a bystander takes when the primary is hit.
 *
 * Every expected value below was produced by EXECUTING the expression and pasting what it printed,
 * never by reasoning about the arithmetic -- the standing rule in DefenseTest and AttackChargeTest.
 * The exact-equality assertions are exact BECAUSE execution showed they are: 14.2 * 0.5 == 7.1 and
 * 8.0 * 0.5 == 4.0 both hold in binary floating point, and were checked before being written down.
 *
 * The load-bearing tests here are not the multiply -- it is one operator -- but the INHERITANCE
 * ones. Sweep's whole design claim is that a fraction of the primary's final figure carries the
 * enchant, the class bonus and the charge for free. Those tests state that claim in numbers taken
 * from the two places the primary's figure is actually built: EffectApplierTest's 8*1.15+5 = 14.2
 * ordering example, and AttackChargeTest's measured curve.
 *
 * Each test names the mutation it forces red.
 */
class SweepShareTest {

    private static final double EPS = 1e-9;

    // --- The fraction ---

    @Test
    void aSweptMobTakesTheDeclaredFractionOfWhatThePrimaryTook() {
        // Exact, not EPS: execution confirms 8.0 * 0.5 == 4.0 in binary floating point.
        assertEquals(4.0, SweepShare.of(8.0, 0.5));
        assertEquals(3.5, SweepShare.of(7.0, 0.5), EPS, "the emberblade's 7, swept");
        // Mutation: divide instead of multiply -> of(8, 0.5) = 16.0, a sweep that hits HARDER than
        // the swing that caused it -> reddens.
    }

    @Test
    void aZeroFractionLandsNothingAtAll() {
        assertEquals(0.0, SweepShare.of(8.0, SweepShare.NONE), EPS);
        assertEquals(0.0, SweepShare.of(1000.0, SweepShare.NONE), EPS);
        // Mutation: make NONE anything but 0.0, or add a floor the way AttackCharge.MIN_SCALE is a
        // floor -> a weapon that declares no sweep sweeps anyway -> reddens.
    }

    // --- What the fraction inherits, which is the whole design ---

    @Test
    void theEnchantAndClassBonusRideAlongBecauseTheyAreAlreadyInThePrimarysFigure() {
        // 14.2 is EffectApplier's own worked example, asserted in EffectApplierTest: an 8-damage
        // sword with Sharpness III (+15%) and +5 Melee gear deals 8*1.15 + 5 = 14.2. Sweep does not
        // reapply either modifier -- it takes half of the number they already produced.
        double primary = 8.0 * 1.15 + 5.0;
        assertEquals(14.2, primary, EPS, "the primary's figure, per EffectApplier's ordering");
        assertEquals(7.1, SweepShare.of(primary, 0.5));
        // Mutation: have the sweep rider recompute the chain from the weapon base instead of taking
        // the fraction -- 8*0.5 = 4.0, dropping the enchant and the bonus -> reddens.
    }

    @Test
    void theChargeRidesAlongTheSameWay() {
        // AttackChargeTest's curve: a half-charged 8-damage swing lands 3.2. Half of that is what
        // the bystander gets, so a poorly timed swing sweeps weakly and a wound-up one sweeps hard.
        double halfCharged = AttackCharge.apply(8.0, 0.5);
        assertEquals(3.2, halfCharged, EPS);
        assertEquals(1.6, SweepShare.of(halfCharged, 0.5), EPS);
        assertEquals(4.0, SweepShare.of(AttackCharge.apply(8.0, AttackCharge.FULL_CHARGE), 0.5), EPS,
                "and a full-charge swing sweeps for the full half");
        // Mutation: apply AttackCharge.scale a SECOND time inside the sweep path -> 0.64 rather than
        // 1.6, the charge counted twice -> reddens.
    }

    @Test
    void theBootMeasuredCurveSurvivesTheFraction() {
        // The 2026-08-28 Step 0 observation, swept: base 6.0 at charge 0.76 dealt 3.97248.
        assertEquals(1.98624, SweepShare.of(AttackCharge.apply(6.0, 0.76), 0.5), EPS);
        // Mutation: change the fraction's meaning from "of the primary" to "of the weapon base"
        // -> 3.0, which no longer tracks the swing that produced it -> reddens.
    }

    // --- The predicate, which two callers share ---

    @Test
    void onlyAPositiveFractionDeclaresASweep() {
        assertTrue(SweepShare.sweeps(0.5));
        assertTrue(SweepShare.sweeps(0.001), "any declared fraction sweeps, however small");
        assertFalse(SweepShare.sweeps(SweepShare.NONE), "absent means no sweep");
        assertFalse(SweepShare.sweeps(-1.0), "and a nonsense value is not a sweep either");
        // Mutation: >= instead of > -> an absent sweep reads as a declared one, so every weapon
        // sweeps for zero AND WeaponDefinition starts demanding a melee trigger from the bow
        // -> reddens.
    }

    // --- Shape ---

    @Test
    void theShareRisesWithBothTheHitAndTheFractionAndNeverExceedsTheHit() {
        for (double primary = 0.0; primary <= 40.0; primary += 2.5) {
            double previous = -1.0;
            for (double fraction = 0.0; fraction <= 1.0 + EPS; fraction += 0.05) {
                double share = SweepShare.of(primary, fraction);
                assertTrue(share >= previous - EPS,
                        "must rise with the fraction, at " + primary + "/" + fraction);
                assertTrue(share <= primary + EPS,
                        "a sweep never exceeds the hit it came from, at " + primary + "/" + fraction);
                previous = share;
            }
        }
        // Mutation: add a constant floor to the return -> share > primary at fraction 0 for a small
        // hit -> reddens on the upper bound, which fixed-point checks alone would not catch.
    }
}
