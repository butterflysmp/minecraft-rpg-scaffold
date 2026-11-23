package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seat's ruling H, 2026-11-22: AN ENCHANT ROLLS ONLY ON A WEAPON THAT HAS ITS QUANTITY, after the class filter. */
class EnchantRollQuantityTest {

    private static final EnchantRoll.Rollable DEFT =
            new EnchantRoll.Rollable("deft_hands", GearClass.RANGER, EnchantRoll.Requires.RELOAD);
    private static final EnchantRoll.Rollable QUIVER =
            new EnchantRoll.Rollable("expanded_quiver", GearClass.RANGER, EnchantRoll.Requires.QUIVER);
    private static final EnchantRoll.Rollable POWER = new EnchantRoll.Rollable("power", GearClass.RANGER);
    private static final EnchantRoll.Rollable SHARP = new EnchantRoll.Rollable("sharpness", GearClass.MELEE);
    private static final EnchantRoll.Rollable UNBREAKING = new EnchantRoll.Rollable("unbreaking", null);
    private static final List<EnchantRoll.Rollable> ROSTER = List.of(DEFT, QUIVER, POWER, SHARP, UNBREAKING);

    private static List<EnchantRoll.Rollable> pool(boolean reload, boolean quiver, GearClass c) {
        return EnchantRoll.poolFor(new EnchantRoll.Has(reload, quiver), c, ROSTER);
    }

    @Test
    void aRangerWeaponWithNeitherQuantityGetsNeitherEnchant() {
        assertEquals(List.of(POWER, UNBREAKING), pool(false, false, GearClass.RANGER), "the Crossbow");
    }

    @Test
    void eachQuantityAdmitsItsOwnEnchantOnly() {
        assertEquals(List.of(DEFT, POWER, UNBREAKING), pool(true, false, GearClass.RANGER));
        assertEquals(List.of(QUIVER, POWER, UNBREAKING), pool(false, true, GearClass.RANGER));
        assertEquals(List.of(DEFT, QUIVER, POWER, UNBREAKING), pool(true, true, GearClass.RANGER), "the Boltor");
    }

    /** The class filter is first: a quantity never admits an enchant of another class. */
    @Test
    void aQuantityNeverAdmitsAnotherClass() {
        assertEquals(List.of(SHARP, UNBREAKING), pool(true, true, GearClass.MELEE));
    }

    /** EVERY is the class filter alone, which is every pool as it was before ruling H. */
    @Test
    void everyIsTheClassFilterAlone() {
        assertEquals(List.of(DEFT, QUIVER, POWER, UNBREAKING),
                EnchantRoll.poolFor(EnchantRoll.Has.EVERY, GearClass.RANGER, ROSTER));
        assertTrue(EnchantRoll.Has.NOTHING.equals(new EnchantRoll.Has(false, false)));
    }

    /** The two-argument Rollable needs nothing, so every enchant built that way is unaffected. */
    @Test
    void aRollableWithoutARequirementNeedsNothing() {
        assertEquals(EnchantRoll.Requires.NONE, new EnchantRoll.Rollable("x", null).requires());
        assertEquals(EnchantRoll.Requires.NONE, new EnchantRoll.Rollable("x", null, null).requires());
    }
}
