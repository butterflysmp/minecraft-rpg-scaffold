package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** fromName mirrors Rarity: case-insensitive, null on a miss so the loader turns a bad value into a skip. */
class WeaponClassTest {

    @Test
    void fromNameIsCaseInsensitive() {
        assertEquals(WeaponClass.MELEE, WeaponClass.fromName("melee"));
        assertEquals(WeaponClass.RANGER, WeaponClass.fromName("Ranger"));
        assertEquals(WeaponClass.MAGE, WeaponClass.fromName("MAGE"));
    }

    @Test
    void fromNameNullOnUnknown() {
        assertNull(WeaponClass.fromName("bogus"));
        assertNull(WeaponClass.fromName(null));
        // SUMMONER is not a member yet -- a content file naming it is a skip, not a silent accept.
        assertNull(WeaponClass.fromName("summoner"));
    }
}
