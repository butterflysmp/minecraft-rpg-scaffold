package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.github.butterflysmp.rpg.core.accessory.AccessorySlots.Verdict.CLASS_SLOT_LOCKED;
import static io.github.butterflysmp.rpg.core.accessory.AccessorySlots.Verdict.OK;
import static io.github.butterflysmp.rpg.core.accessory.AccessorySlots.Verdict.WRONG_CLASS;
import static io.github.butterflysmp.rpg.core.accessory.AccessorySlots.Verdict.WRONG_SLOT_KIND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The class gate (ruling A1) and the equip decision. The gate is the load-bearing guard of the
 * class slot, so its rows cover every profile value that can reach it: a matching class, another
 * class, "none", null, and a wrong-case token.
 */
class AccessorySlotsTest {

    private final AccessoryDefinition quiver = AccessoryFixtures.fletchersQuiver();
    private final AccessoryDefinition ward = AccessoryFixtures.wardCharm();

    @Test
    void theShapeIsOneClassSlotAndThreeUniversal() {
        assertEquals(4, AccessorySlots.COUNT);
        assertEquals(AccessorySlotKind.CLASS, AccessorySlots.kindOf(0));
        for (int slot = 1; slot < 4; slot++) {
            assertEquals(AccessorySlotKind.UNIVERSAL, AccessorySlots.kindOf(slot));
        }
        assertThrows(IllegalArgumentException.class, () -> AccessorySlots.requireSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> AccessorySlots.requireSlot(4));
    }

    /**
     * *** THE CLASS GATE. The row the class-gate mutation must redden. ***
     *
     * <p>A class accessory contributes ONLY for a profile whose class is exactly its token.
     */
    @Test
    void aClassAccessoryContributesOnlyForItsOwnClass_exactly() {
        assertTrue(AccessorySlots.contributes(quiver, "ranger"));
        for (String other : Arrays.asList("mage", "melee", "none", null, "Ranger", "RANGER", " ranger", "")) {
            assertFalse(AccessorySlots.contributes(quiver, other),
                    "a ranger quiver must be inert for profile class '" + other + "'");
        }
        // Mutation: make contributes() return true for CLASS items -> reddens.
    }

    @Test
    void aUniversalAccessoryContributesForEveryone() {
        for (String profile : Arrays.asList("ranger", "mage", "none", null)) {
            assertTrue(AccessorySlots.contributes(ward, profile), String.valueOf(profile));
        }
    }

    @Test
    void equipping_theWholeGrid() {
        // The class slot, locked for no class.
        assertEquals(CLASS_SLOT_LOCKED, AccessorySlots.canEquip(0, quiver, "none"));
        assertEquals(CLASS_SLOT_LOCKED, AccessorySlots.canEquip(0, quiver, null));
        assertEquals(CLASS_SLOT_LOCKED, AccessorySlots.canEquip(0, ward, "none"),
                "locked means locked: not even a (wrong) universal item gets a different answer");
        // The class slot, with a class.
        assertEquals(OK, AccessorySlots.canEquip(0, quiver, "ranger"));
        assertEquals(WRONG_CLASS, AccessorySlots.canEquip(0, quiver, "mage"));
        assertEquals(WRONG_CLASS, AccessorySlots.canEquip(0, quiver, "Ranger"), "case-sensitive, as the Build screen's class token is");
        assertEquals(WRONG_SLOT_KIND, AccessorySlots.canEquip(0, ward, "ranger"));
        // The universal slots.
        for (int slot = 1; slot < 4; slot++) {
            assertEquals(OK, AccessorySlots.canEquip(slot, ward, "none"));
            assertEquals(OK, AccessorySlots.canEquip(slot, ward, null));
            assertEquals(WRONG_SLOT_KIND, AccessorySlots.canEquip(slot, quiver, "ranger"));
        }
    }

    @Test
    void theClassTokenIsThePoolSpelling() {
        assertEquals(List.of("melee", "ranger", "mage"), Arrays.stream(
                io.github.butterflysmp.rpg.core.weapon.WeaponClass.values())
                .map(AccessorySlots::classToken).toList());
        assertEquals("none", AccessorySlots.NO_CLASS);
    }
}
