package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.Durability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The durability arithmetic, which is the whole of the no-break promise.
 *
 * Every case here is a guard whose absence is a real, shippable bug: a weapon destroyed instead of
 * floored, a broken weapon that still swings, a negative damage value written onto a staff, or a
 * wear so large it wraps around into a free full repair. Redden by deleting either clamp, flipping
 * {@code isBroken}'s comparison, or dropping either {@code maxDurability <= 0} guard.
 *
 * The shipped maximums are the fixtures on purpose -- iron_sword 250 (ironblade, emberblade) and
 * bow 384 (hunters_bow) -- so these numbers are the ones the boot gate will read off a real bar.
 */
class DurabilityTest {

    /** iron_sword: ironblade and emberblade both mint on it. */
    private static final int IRON_SWORD = 250;
    /** bow: the hunters_bow's material, and the only other damageable one shipped. */
    private static final int BOW = 384;
    /** blaze_rod / amethyst_shard: ember_staff and ability_stone have NO vanilla durability. */
    private static final int NOT_DAMAGEABLE = 0;

    @Test
    void clampNeverReachesTheMaximumSoAWeaponCanNeverBeDestroyed() {
        // THE no-break promise. maxDurability itself is a destroyed item in vanilla's sense -- gone,
        // not inert -- so the design rests on this landing one short, however much is asked for.
        assertEquals(249, Durability.clamp(9999, IRON_SWORD));
        assertEquals(383, Durability.clamp(Integer.MAX_VALUE, BOW));
        // Mutation: drop the `- MIN_USES` -> 250 -> the next point destroys the weapon -> reddens.
    }

    @Test
    void clampLeavesAnOrdinaryDamageValueAlone() {
        // The identity case, and the one that runs constantly: every carryWear on an unchanged
        // material passes through here. If it is not an identity, a re-mint silently alters wear.
        assertEquals(0, Durability.clamp(0, IRON_SWORD));
        assertEquals(120, Durability.clamp(120, IRON_SWORD));
    }

    @Test
    void clampRefusesANegativeDamageValue() {
        // A negative damage is not "extra durability", it is a malformed item. Reachable through
        // carryWear if a stored value is ever wrong, and through the dev command's `set`.
        assertEquals(0, Durability.clamp(-5, IRON_SWORD));
        // Mutation: drop the `Math.max(..., 0)` -> -5 -> reddens.
    }

    @Test
    void clampIsTheReMintCaseThatWouldOtherwiseDestroyAWeapon() {
        // PR #12's step 9, as arithmetic. A weapon worn to 50/250 whose content material changes to
        // gold (32) would be copied to damage 50 on a 32-max item: damaged past its maximum, i.e. a
        // BROKEN item, produced by a DISPLAY refresh. This is the line WeaponItems.carryWear routes
        // through, and the reason it is a method rather than an inline min().
        assertEquals(31, Durability.clamp(50, 32));
    }

    @Test
    void aNonDamageableMaterialClampsToZeroRatherThanMinusOne() {
        // The guard in clamp, on its own. Without it the expression is min(max(x, 0), 0 - 1) == -1,
        // so every ember_staff and ability_stone would be written a NEGATIVE damage value. Asserted
        // directly rather than through isBroken, because this is a second, independent guard --
        // covering one does not cover the other.
        assertEquals(0, Durability.clamp(50, NOT_DAMAGEABLE));
        assertEquals(0, Durability.clamp(0, NOT_DAMAGEABLE));
        assertEquals(0, Durability.clamp(-5, NOT_DAMAGEABLE));
        // Mutation: delete clamp's `if (maxDurability <= 0)` -> -1 -> reddens.
    }

    @Test
    void isBrokenFiresAtTheFloorAndNotOnePointLate() {
        // >= not >. At 249/250 the last use is gone and the weapon must be inert; one point late and
        // a spent weapon still swings, which is the whole gate failing open.
        assertTrue(Durability.isBroken(249, IRON_SWORD), "at the floor, the weapon is spent");
        assertFalse(Durability.isBroken(248, IRON_SWORD), "one use left is not broken");
        assertTrue(Durability.isBroken(383, BOW));
        assertFalse(Durability.isBroken(382, BOW));
        // Mutation: `>=` -> `>` -> 249 reads usable -> reddens.
    }

    @Test
    void aFreshWeaponIsNotBroken() {
        // The state every minted weapon starts in. A gate that fired here would make every new
        // weapon inert on its first swing.
        assertFalse(Durability.isBroken(0, IRON_SWORD));
        assertFalse(Durability.isBroken(0, BOW));
    }

    @Test
    void aNonDamageableMaterialIsNeverBroken() {
        // The staff-and-stone exemption, structural rather than a convention each call site repeats.
        // Without the guard the threshold is 0 - 1 == -1, and EVERY damage value is >= -1 -- so the
        // ember_staff would read as permanently broken and could never be cast.
        assertFalse(Durability.isBroken(0, NOT_DAMAGEABLE));
        assertFalse(Durability.isBroken(999, NOT_DAMAGEABLE));
        // Mutation: delete isBroken's `if (maxDurability <= 0)` -> true -> reddens.
    }

    @Test
    void wearAccumulatesAndThenFloorsRatherThanDestroying() {
        assertEquals(10, Durability.wear(0, 10, IRON_SWORD));
        assertEquals(140, Durability.wear(100, 40, IRON_SWORD));
        assertEquals(249, Durability.wear(240, 100, IRON_SWORD),
                "past the floor, it stops at the floor");
        // Mutation: return currentDamage + amount unclamped -> 340 -> reddens.
    }

    @Test
    void wearInheritsClampsNonDamageableGuard() {
        // wear delegates its clamping to clamp, so the guard must carry through the delegation.
        // Asserted directly: this is the entry point the dev command and Pass 2's auto-wear call.
        assertEquals(0, Durability.wear(5, 10, NOT_DAMAGEABLE));
        // Mutation: delete clamp's `if (maxDurability <= 0)` -> -1 -> reddens here too.
    }

    @Test
    void aHugeWearAmountCannotOverflowIntoAFullRepair() {
        // currentDamage + amount in int wraps NEGATIVE for a large enough amount, and a negative
        // clamps to 0 -- so without the long widening, wearing a weapon hard enough would REPAIR it.
        // Same class of bug as a debuff looping round into the strongest buff in the game.
        assertEquals(249, Durability.wear(200, Integer.MAX_VALUE, IRON_SWORD));
        assertEquals(249, Durability.wear(Integer.MAX_VALUE, Integer.MAX_VALUE, IRON_SWORD));
        // Mutation: drop the (long) casts -> wraps to a negative -> clamps to 0 -> reddens.
    }

    @Test
    void repairMovesDamageDownAndFloorsAtFullyRepaired() {
        assertEquals(60, Durability.repair(100, 40));
        assertEquals(0, Durability.repair(10, 999), "over-repair is a full repair, not a negative");
        assertEquals(0, Durability.repair(0, 5));
        // Mutation: drop the `Math.max(..., 0)` -> -989 -> reddens.
    }

    @Test
    void aRepairedWeaponStopsBeingBroken() {
        // The round trip /rpg repair witnesses: worn to the floor, broken; repaired, usable again.
        int broken = Durability.wear(0, 9999, IRON_SWORD);
        assertTrue(Durability.isBroken(broken, IRON_SWORD));

        int repaired = Durability.repair(broken, broken);
        assertEquals(0, repaired);
        assertFalse(Durability.isBroken(repaired, IRON_SWORD), "a full repair must un-break it");
    }
}
