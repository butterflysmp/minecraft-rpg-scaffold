package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Equipment screen's slots, pinned as LITERALS (the Nexus layout's lesson: a constant only ever
 * named symbolically has no guard), and the partition of the 54 into painted and filler.
 */
class EquipmentMenuLayoutTest {

    @Test
    void theSlotsArePinnedToTheirLiterals() {
        assertEquals(54, EquipmentMenuLayout.SIZE);
        assertEquals(48, EquipmentMenuLayout.BACK_SLOT, "the house back button");
        assertEquals(49, EquipmentMenuLayout.CLOSE_SLOT, "the house close button");
        assertEquals(2, EquipmentMenuLayout.ARMOR_LABEL_SLOT);
        assertEquals(1, EquipmentMenuLayout.ACCESSORY_LABEL_SLOT, "directly left of the Armour label (2)");
        assertEquals(11, EquipmentMenuLayout.HEAD_SLOT);
        assertEquals(20, EquipmentMenuLayout.CHEST_SLOT);
        assertEquals(29, EquipmentMenuLayout.LEGS_SLOT);
        assertEquals(38, EquipmentMenuLayout.FEET_SLOT);
        assertEquals(List.of(10, 19, 28, 37), EquipmentMenuLayout.ACCESSORY_SLOTS,
                "column 1, directly left of the armour column; index 0 (the class slot) first");
        for (int i = 0; i < 4; i++) {
            assertEquals(EquipmentMenuLayout.ACCESSORY_SLOTS.get(i) + 1,
                    List.of(EquipmentMenuLayout.HEAD_SLOT, EquipmentMenuLayout.CHEST_SLOT,
                            EquipmentMenuLayout.LEGS_SLOT, EquipmentMenuLayout.FEET_SLOT).get(i),
                    "accessory slot " + i + " sits immediately left of its armour row");
        }
    }

    @Test
    void everySlotIsExactlyOneOfPaintedOrFiller() {
        Set<Integer> union = new HashSet<>(EquipmentMenuLayout.PAINTED_SLOTS);
        union.addAll(EquipmentMenuLayout.FILLER_SLOTS);
        assertEquals(54, union.size(), "together they cover the whole screen");
        for (int slot : EquipmentMenuLayout.PAINTED_SLOTS) {
            assertFalse(EquipmentMenuLayout.FILLER_SLOTS.contains(slot), "slot " + slot + " is both");
        }
        assertEquals(12, EquipmentMenuLayout.PAINTED_SLOTS.size(),
                "4 armour + 4 accessory + 2 labels + back + close");
        assertEquals(42, EquipmentMenuLayout.FILLER_SLOTS.size());
    }

    /**
     * Columns 5-7 (0-indexed), rows 1-4 are RESERVED for Slice C (Equipment Sets). In this slice they
     * are plain filler: nothing is built there, and nothing painted may land there by accident.
     */
    @Test
    void theSliceCReservationIsPlainFiller() {
        int[] reserved = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
        for (int slot : reserved) {
            assertTrue(EquipmentMenuLayout.FILLER_SLOTS.contains(slot), "reserved slot " + slot + " is filler");
            assertFalse(EquipmentMenuLayout.PAINTED_SLOTS.contains(slot), "reserved slot " + slot + " is not painted");
            assertTrue(slot % 9 >= 5 && slot % 9 <= 7 && slot / 9 >= 1 && slot / 9 <= 4,
                    "the literal list is columns 5-7, rows 1-4: " + slot);
        }
        assertEquals(12, reserved.length, "three columns by four rows");
        // Mutation: paint anything into a reserved slot -> reddens.
    }

    @Test
    void theArmourMappingRoundTrips_andNothingElseIsArmour() {
        for (ArmorSlot armor : ArmorSlot.values()) {
            assertEquals(armor, EquipmentMenuLayout.armorSlotAt(EquipmentMenuLayout.slotOf(armor)).orElseThrow());
        }
        int armourSlots = 0;
        for (int slot = 0; slot < 54; slot++) {
            if (EquipmentMenuLayout.armorSlotAt(slot).isPresent()) armourSlots++;
        }
        assertEquals(4, armourSlots);
    }

    @Test
    void theAccessoryIndexMappingCoversExactlyTheFourAccessorySlots() {
        for (int i = 0; i < 4; i++) {
            assertEquals(i, EquipmentMenuLayout.accessoryIndexAt(EquipmentMenuLayout.ACCESSORY_SLOTS.get(i)).getAsInt());
        }
        int accessorySlots = 0;
        for (int slot = 0; slot < 54; slot++) {
            if (EquipmentMenuLayout.accessoryIndexAt(slot).isPresent()) accessorySlots++;
        }
        assertEquals(4, accessorySlots);
        assertTrue(EquipmentMenuLayout.accessoryIndexAt(EquipmentMenuLayout.HEAD_SLOT).isEmpty());
    }
}
