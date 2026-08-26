package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The damage-enchant mechanism, which is the whole of Sharpness, Power and Attunement.
 *
 * Every case here is a guard whose absence is a real, shippable bug: a curve read off by one so
 * every level grants the wrong number, a level past an enchant's own max_level throwing out of a
 * reconcile tick, a class gate that lets Sharpness boost a bow, or a neutral value that turns an
 * unenchanted weapon's damage into zero.
 *
 * The headline is {@link #theClassGateKeepsSharpnessOffABow} for the rule and
 * {@link #aLevelPastThisEnchantsOwnCurveClampsRatherThanThrowing} for the guard nothing else covers.
 * Each test names the mutation it forces red.
 *
 * These numbers are the ones the boot gate reads: the shipped curve is [5, 10, 15], so III on the
 * ironblade's 8 is 9.2 and I on the ember_staff's 16 is 16.8. They are asserted here at FULL
 * PRECISION because the damage popup rounds to an integer, which cannot separate 8.4 from 8.
 */
class DamageEnchantsTest {

    private static final double EPS = 1e-9;

    /** The shipped curve, and the one every boot-gate number is derived from. */
    private static final List<Integer> SHIPPED = List.of(5, 10, 15);

    // --- The curve ---------------------------------------------------------------------------

    @Test
    void theCurveGrantsTheAuthoredPercentAtEachLevel() {
        // Off-by-one here is invisible in review and wrong in every game: level I would grant II's
        // percent and level III would run off the end of the list.
        assertEquals(5.0, DamageEnchants.percentAt(SHIPPED, 1), EPS);
        assertEquals(10.0, DamageEnchants.percentAt(SHIPPED, 2), EPS);
        assertEquals(15.0, DamageEnchants.percentAt(SHIPPED, 3), EPS);
        // Mutation: index `level` instead of `level - 1` -> I grants 10 -> reddens.
    }

    @Test
    void levelZeroAndBelowGrantNothingSoAnUnenchantedWeaponIsUntouched() {
        // By far the most common branch: every weapon in the game that carries no damage enchant
        // resolves through here on every reconcile tick. "Nearly nothing" would be a silent,
        // game-wide damage buff.
        assertEquals(0.0, DamageEnchants.percentAt(SHIPPED, 0), EPS);
        assertEquals(0.0, DamageEnchants.percentAt(SHIPPED, -1), EPS);
        assertEquals(0.0, DamageEnchants.percentAt(SHIPPED, Integer.MIN_VALUE), EPS);
        // Mutation: drop the `level <= 0` guard -> index -1 -> IndexOutOfBounds -> reddens.
    }

    @Test
    void aLevelPastThisEnchantsOwnCurveClampsRatherThanThrowing() {
        // THE guard nothing else in the tree provides. EnchantState.effective() clamps to the
        // MODEL's global MAX_LEVEL (3), not to this enchant's authored max_level -- so a two-entry
        // curve can legitimately be asked for level 3 by a hand-edited item, or by a blob a build
        // with different content wrote. Without the clamp that is an IndexOutOfBoundsException
        // thrown from inside a reconcile tick, on a path that must be total.
        List<Integer> twoLevels = List.of(4, 8);
        assertEquals(8.0, DamageEnchants.percentAt(twoLevels, 3), EPS, "clamps to its own top entry");
        assertEquals(8.0, DamageEnchants.percentAt(twoLevels, 99), EPS);
        assertEquals(8.0, DamageEnchants.percentAt(twoLevels, Integer.MAX_VALUE), EPS);
        assertEquals(15.0, DamageEnchants.percentAt(SHIPPED, 4), EPS, "and never past it");
        // Mutation: drop the Math.min(level, size) -> IndexOutOfBounds -> reddens.
    }

    @Test
    void anAbsentOrEmptyCurveGrantsNothingRatherThanThrowing() {
        // The loader refuses a damage enchant with no curve, so this is unreachable from content --
        // and it is pinned anyway, because percentAt runs inside a reconcile tick where an
        // exception is a stopped loop rather than a diagnosable error.
        assertEquals(0.0, DamageEnchants.percentAt(null, 3), EPS);
        assertEquals(0.0, DamageEnchants.percentAt(List.of(), 3), EPS);
        assertEquals(0.0, DamageEnchants.percentAt(Arrays.asList((Integer) null), 1), EPS);
    }

    // --- The multiplier ----------------------------------------------------------------------

    @Test
    void theMultiplierIsOnePlusThePercentOverAHundred() {
        // The one place the formula lives, so the two damage arms cannot disagree about it.
        assertEquals(1.05, DamageEnchants.multiplier(5.0), EPS);
        assertEquals(1.10, DamageEnchants.multiplier(10.0), EPS);
        assertEquals(1.15, DamageEnchants.multiplier(15.0), EPS);
        // Mutation: `percent / 100` -> `percent` -> 5 becomes x6.0 -> reddens.
    }

    @Test
    void aZeroPercentIsExactlyOneSoAnUnenchantedWeaponDealsWhatItAlwaysDid() {
        // The neutral value, and the reason the STAT carries a percent rather than a multiplier.
        // A multiplier-valued stat would have to base at 1.0, which Stat's summing turns into 2.0
        // with two sources -- and any 0.0 default on it silently zeroes all damage instead of
        // leaving it alone. This assertion is what makes 0.0 the safe default.
        assertEquals(1.0, DamageEnchants.multiplier(0.0), EPS);
        // Mutation: return `percent / 100` (drop the 1 +) -> an unenchanted weapon deals ZERO
        // damage -> reddens here and in EffectApplierTest.
    }

    @Test
    void theShippedCurveProducesTheNumbersTheBootGateReads() {
        // Cross-referenced by the boot gate, and asserted here at full precision because the damage
        // popup rounds: 8 * 1.05 renders as "8", identical to an unenchanted swing, so the gate
        // CANNOT witness Sharpness I on the ironblade. It witnesses the level reaching the curve on
        // the ember_staff (16.8 -> 17 vs 18.4 -> 18) and the arm applying it on the ironblade
        // (9.2 -> 9 vs a plain 8). If these numbers move, that gate is reading the wrong thing.
        assertEquals(8.4, 8 * DamageEnchants.multiplier(DamageEnchants.percentAt(SHIPPED, 1)), EPS);
        assertEquals(9.2, 8 * DamageEnchants.multiplier(DamageEnchants.percentAt(SHIPPED, 3)), EPS);
        assertEquals(16.8, 16 * DamageEnchants.multiplier(DamageEnchants.percentAt(SHIPPED, 1)), EPS);
        assertEquals(18.4, 16 * DamageEnchants.multiplier(DamageEnchants.percentAt(SHIPPED, 3)), EPS);
    }

    // --- The class gate ----------------------------------------------------------------------

    @Test
    void theClassGateKeepsSharpnessOffABow() {
        // The headline rule. BOTH grants are present and only one survives, so the test cannot pass
        // by returning everything or by returning nothing -- it pins the filter itself.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "sharpness", new DamageEnchants.Grant(WeaponClass.MELEE, SHIPPED, 3),
                "power", new DamageEnchants.Grant(WeaponClass.RANGER, SHIPPED, 3));

        Map<String, Double> onABow = DamageEnchants.matching(WeaponClass.RANGER, active);

        assertEquals(1, onABow.size(), "sharpness is inert on a bow");
        assertEquals(15.0, onABow.get("power"), EPS);
        assertNull(onABow.get("sharpness"), "a melee enchant must not reach a ranged weapon");
        // Mutation: drop the class equality check -> both land, size 2 -> reddens.
    }

    @Test
    void swappingTheHeldWeaponSwapsWhichEnchantIsActive() {
        // The mirror, so the gate is pinned in both directions rather than by one example that a
        // constant-returning implementation could satisfy.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "sharpness", new DamageEnchants.Grant(WeaponClass.MELEE, SHIPPED, 3),
                "attunement", new DamageEnchants.Grant(WeaponClass.MAGE, SHIPPED, 1));

        Map<String, Double> onASword = DamageEnchants.matching(WeaponClass.MELEE, active);
        Map<String, Double> onAStaff = DamageEnchants.matching(WeaponClass.MAGE, active);

        assertEquals(Map.of("sharpness", 15.0), onASword);
        assertEquals(Map.of("attunement", 5.0), onAStaff);
    }

    @Test
    void aUniversalEnchantMatchesWhateverItSitsOn() {
        // null class == universal. Unbreaking is universal, but it is a DURABILITY effect and never
        // reaches this map; this is the axis staying open for a universal DAMAGE enchant, and the
        // reason the gate tests `!= null && != heldClass` rather than plain `!=`.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "keen", new DamageEnchants.Grant(null, SHIPPED, 2));

        assertEquals(10.0, DamageEnchants.matching(WeaponClass.MELEE, active).get("keen"), EPS);
        assertEquals(10.0, DamageEnchants.matching(WeaponClass.RANGER, active).get("keen"), EPS);
        assertEquals(10.0, DamageEnchants.matching(WeaponClass.MAGE, active).get("keen"), EPS);
        // Mutation: treat a null class as "matches nothing" -> all three empty -> reddens.
    }

    @Test
    void twoDifferentEnchantsOnOneWeaponBothSurviveUnderTheirOwnKeys() {
        // Keyed by ENCHANT ID rather than one lumped "MAIN_HAND" source, which is what lets Stat do
        // the summing: two matching enchants compose to 25% without this method knowing they did.
        // A single lumped key would silently keep only the last one written.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "sharpness", new DamageEnchants.Grant(WeaponClass.MELEE, SHIPPED, 3),
                "keen", new DamageEnchants.Grant(null, SHIPPED, 2));

        Map<String, Double> desired = DamageEnchants.matching(WeaponClass.MELEE, active);

        assertEquals(2, desired.size(), "both are active and both keep their own source key");
        assertEquals(15.0, desired.get("sharpness"), EPS);
        assertEquals(10.0, desired.get("keen"), EPS);
    }

    @Test
    void aNonMatchingEnchantIsAbsentRatherThanPresentAtZero() {
        // Absent, not zeroed: the reconciler REMOVES a source that stops being desired, where a
        // 0.0-valued entry would leave a dead modifier on the stat forever.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "sharpness", new DamageEnchants.Grant(WeaponClass.MELEE, SHIPPED, 3));

        assertTrue(DamageEnchants.matching(WeaponClass.MAGE, active).isEmpty(),
                "not present at 0.0 -- absent, so the reconciler drops the source");
    }

    @Test
    void aLockedEnchantContributesNothing() {
        // Level 0 is LOCKED -- offered on the item, doing nothing. EnchantState.effective() already
        // filters these out, so this is the second line of defence, and it is here because
        // `matching` is total over whatever it is handed.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "sharpness", new DamageEnchants.Grant(WeaponClass.MELEE, SHIPPED, 0));

        assertTrue(DamageEnchants.matching(WeaponClass.MELEE, active).isEmpty());
    }

    @Test
    void anEmptyHandGrantsNothingEvenToAUniversalEnchant() {
        // A null held class means no weapon, or nothing of ours. These grants are read OFF the held
        // weapon so this is belt and braces -- written anyway so the function is total and cannot
        // be made to grant a universal enchant's percent to an empty hand.
        Map<String, DamageEnchants.Grant> active = Map.of(
                "keen", new DamageEnchants.Grant(null, SHIPPED, 3));

        assertTrue(DamageEnchants.matching(null, active).isEmpty());
        assertTrue(DamageEnchants.matching(WeaponClass.MELEE, null).isEmpty());
        // Mutation: drop the `heldClass == null` guard -> a universal enchant grants 15% unarmed.
    }
}
