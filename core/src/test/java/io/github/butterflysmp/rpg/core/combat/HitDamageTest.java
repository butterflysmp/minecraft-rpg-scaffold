package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The damage composition, now that it has one home instead of two copies.
 *
 * <p>{@code EffectApplierTest} is left BYTE-IDENTICAL and stays the authority on everything the two
 * arms did before this extraction -- that untouched pass is the faithfulness check, the same device
 * the {@code MaxResolver} and {@code RegenResolver} lifts used. Its four factor pins (85.8 for the
 * ordering, 92.70/85.80 for charge placement, 92 for the class addend, 8.0 for crit) are what prove
 * the arms still compose what they composed. This file covers the extracted function directly.
 *
 * <p><b>Every expected value was EXECUTED and pasted, never derived.</b>
 *
 * <p>Each test names the mutation it forces red.
 */
class HitDamageTest {

    private static final double EPS = 1e-9;

    // --- THE HEADLINE: the ordering, and the number that tells the designs apart ------------------

    @Test
    void thePercentScalesTheWEAPONAndTheFlatBonusIsAddedAFTERWARDS() {
        // The witness this codebase has carried in prose since the class-damage slice, now asserted
        // in the one place the formula lives. An 8-damage sword, Sharpness III (+15%), +5 Melee.
        assertEquals(14.2, HitDamage.hitBase(8, 15, 5), EPS,
                "8 * 1.15 + 5 = 14.2 -- the enchant scales what the WEAPON contributes");
        assertNotEquals(14.95, HitDamage.hitBase(8, 15, 5), EPS,
                "(8 + 5) * 1.15 = 14.95 is the OTHER design: the multiply moved outside the addition. "
                        + "Both are 'the enchant and the bonus both applied', so only the number "
                        + "distinguishes them.");
        // Mutation: (base + classBonus) * multiplier(pct) -> 14.95 -> reddens BOTH rows here and
        // EffectApplierTest's 85.8 pin.
    }

    @Test
    void eachSummandCanBeAbsentAndZeroIsTheNeutralForBoth() {
        assertEquals(8.0, HitDamage.hitBase(8, 0, 0), EPS,
                "no enchant, no gear: DamageEnchants.multiplier(0) is exactly 1.0, so the weapon "
                        + "deals precisely what it declares");
        assertEquals(9.2, HitDamage.hitBase(8, 15, 0), EPS, "enchant only");
        assertEquals(13.0, HitDamage.hitBase(8, 0, 5), EPS, "gear only");
        assertEquals(5.0, HitDamage.hitBase(0, 0, 5), EPS,
                "an unarmed caster is base 0, and gear alone cannot resurrect weapon-only melee "
                        + "because no held weapon means no matching grant -- but the arithmetic here "
                        + "is total either way");
        assertEquals(0.0, HitDamage.hitBase(0, 15, 0), EPS, "a percent of nothing is nothing");
        // Mutation: drop the + classBonus addend -> the gear-only rows redden, as does
        // EffectApplierTest's 92 pin.
    }

    // --- The tail, and the identity the stat sheet depends on -------------------------------------

    @Test
    void dealtAtFullChargeWithNoCritIsAnEXACTIdentitySoASheetCanShowTheHitBaseItself() {
        // THE property that lets /rpg stats display hitBase and honestly call it a swing. Not
        // "approximately equal" -- bit-identical, because both neutral factors are exactly 1.0.
        double hitBase = HitDamage.hitBase(8, 15, 5);
        assertEquals(hitBase, HitDamage.dealt(hitBase, AttackCharge.FULL_CHARGE, Crit.NO_CRIT), 0.0,
                "dealt(hitBase, 1.0, 1.0) == hitBase exactly, so the sheet is not showing a "
                        + "near-miss of a real hit");
        assertEquals(14.2, HitDamage.dealt(hitBase, AttackCharge.FULL_CHARGE, Crit.NO_CRIT), EPS);
        // Mutation: have dealt add or scale anything -> the exact-equality row reddens.
    }

    @Test
    void bothFactorsScaleTheWHOLEAmountIncludingTheFlatBonus() {
        double hitBase = HitDamage.hitBase(8, 15, 5);          // 14.2
        assertEquals(7.1, HitDamage.dealt(hitBase, 0.5, Crit.NO_CRIT), EPS, "half charge halves it");
        assertEquals(28.4, HitDamage.dealt(hitBase, AttackCharge.FULL_CHARGE, 2.0), EPS,
                "a base crit doubles it");
        assertEquals(2.84, HitDamage.dealt(hitBase, AttackCharge.MIN_SCALE, Crit.NO_CRIT), EPS,
                "and the floor of the charge curve is 0.2, never 0");

        // BOTH applied, proven by a pair whose product is 1: if either factor were dropped this
        // would read 7.1 or 28.4 rather than landing back on 14.2.
        assertEquals(14.2, HitDamage.dealt(hitBase, 0.5, 2.0), EPS,
                "half charge and a doubled crit cancel -- which only holds if BOTH were applied");

        // Scaling only the weapon base would leave the flat +5 as a spam-proof damage floor: at half
        // charge that would be 4.6 + 5 = 9.6 rather than 7.1, and enough +N gear would make more
        // swings beat timed swings. The model, inverted.
        assertNotEquals(9.6, HitDamage.dealt(hitBase, 0.5, Crit.NO_CRIT), EPS);
        // Mutation: drop chargeScale -> 14.2 where 7.1 is expected. Drop critMultiplier -> 14.2
        // where 28.4 is expected. Either reddens, and so does EffectApplierTest's 8.0 pin.
    }

    @Test
    void theTwoStepsComposeToWhatTheArmsUsedToWriteInline() {
        // The shape both EffectApplier arms now call, rebuilt here so the extraction's equivalence is
        // stated rather than inferred from the untouched suite. 100 - 14.2 = 85.8 is exactly
        // EffectApplierTest's ordering pin.
        assertEquals(85.8,
                100 - HitDamage.dealt(HitDamage.hitBase(8, 15, 5),
                        AttackCharge.FULL_CHARGE, Crit.NO_CRIT), EPS,
                "the health an 85.8 target is left at -- EffectApplierTest asserts the same number "
                        + "through the applier, so the two cannot drift apart silently");
        // Mutation: any change to either method that alters the composed value -> reddens here AND
        // in EffectApplierTest, which is the point of keeping both.
    }
}
