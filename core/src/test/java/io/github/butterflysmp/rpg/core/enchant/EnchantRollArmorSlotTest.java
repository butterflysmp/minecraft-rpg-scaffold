package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Has;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E1 (the seat, 2026-11-25): slot 0 is PIECE-SPECIFIC BY ARMOR SLOT. An {@code armor_slot: head} enchant is offered in
 * slot 0 of a helmet and of no chestplate, and every helmet shares the one pool (Ben: "Helmet boots chestplate and
 * leggings all different"). No such enchant ships in this slice; these rows author one.
 */
class EnchantRollArmorSlotTest {

    private static final Rollable HEAD_ONLY = Rollable.forWeapon("visor", GearClass.ARMOR, EnchantRoll.Requires.NONE,
            EnchantRoll.armorSlotPool(ArmorSlot.HEAD));
    private static final Rollable NORMAL = new Rollable("protection", GearClass.ARMOR);

    @Test
    void theFourSlotsHaveFourDistinctPoolsDisjointFromEveryWeaponId() {
        Set<String> keys = new HashSet<>();
        for (ArmorSlot s : ArmorSlot.values()) keys.add(EnchantRoll.armorSlotPool(s));
        assertEquals(4, keys.size(), "helmet, chestplate, leggings and boots all different");
        assertTrue(keys.stream().allMatch(k -> k.contains(":")), "a weapon id is a file name and carries no colon");
        assertEquals("armor_slot:head", EnchantRoll.armorSlotPool(ArmorSlot.HEAD));
        assertThrows(IllegalArgumentException.class, () -> EnchantRoll.armorSlotPool(null));
    }

    @Test
    void aHeadEnchantRollsInSlotZeroOfEveryHelmetAndNoChestplate() {
        List<Rollable> roster = List.of(HEAD_ONLY, NORMAL);
        assertEquals(List.of(HEAD_ONLY), EnchantRoll.weaponPoolFor(EnchantRoll.armorSlotPool(ArmorSlot.HEAD),
                Has.NOTHING, GearClass.ARMOR, roster), "every helmet rolls by the head key");
        assertEquals(List.of(), EnchantRoll.weaponPoolFor(EnchantRoll.armorSlotPool(ArmorSlot.CHEST),
                Has.NOTHING, GearClass.ARMOR, roster), "no chestplate");
        assertEquals(List.of(), EnchantRoll.weaponPoolFor("iron_helmet", Has.NOTHING, GearClass.ARMOR, roster),
                "never by the piece's own id: the pool is the slot's");
        // And never in a normal slot: it is WEAPON kind.
        assertEquals(List.of(NORMAL), EnchantRoll.normalPoolFor(Has.NOTHING, GearClass.ARMOR, roster));
        // Mutation: make armorSlotPool return the same key for every slot -> the chestplate row reddens.
    }

    @Test
    void aRolledHelmetCarriesItInSlotZero() {
        EnchantState rolled = EnchantRoll.roll(EnchantRoll.armorSlotPool(ArmorSlot.HEAD), java.util.Optional.empty(),
                Has.NOTHING, GearClass.ARMOR, List.of(HEAD_ONLY, NORMAL), CandidateCascade.ONE, () -> 0.0);
        assertEquals("visor", rolled.slots().get(0).candidates().get(0).enchantId());
        assertTrue(rolled.slots().get(1).candidates().isEmpty(), "slot 1, element-specific: armor has none");
    }
}
