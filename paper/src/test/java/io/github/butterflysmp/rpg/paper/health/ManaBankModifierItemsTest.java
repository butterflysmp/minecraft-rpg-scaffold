package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The part of the Mana Bank scan a unit test can reach: its source keys.
 *
 * <p>{@code desiredModifiers} needs a live {@code Player} and is boot-witnessed, like every scanner
 * in this package. The prefix is a plain constant, so it gets the assertion that can be made offline.
 *
 * <p><b>Unlike Growth's, this prefix guards nothing that exists yet</b>, and the test says so rather
 * than implying a live collision. Growth needed {@code "growth:"} because
 * {@code HealthModifierItems} already walked every slot on bare names and would have erased it.
 * Nothing else feeds the max-mana target today. The prefix is carried so that the FIRST scanner to
 * join this target cannot silently overwrite this one -- which is a cheap decision now and a
 * debugging session later.
 */
class ManaBankModifierItemsTest {

    @Test
    void everyManaBankSourceKeyIsDisjointFromABareSlotNameAndFromGrowths() {
        assertFalse(ManaBankModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix is what makes a future second scanner able to erase this one");

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String manaKey = ManaBankModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), manaKey,
                    "Mana Bank's key for " + slot + " must not be the bare slot name");
            // Growth feeds a DIFFERENT target, so this is not a live collision -- it is the property
            // that stays true if the two ever share one, which is the only reason to check it.
            assertNotEquals(GrowthModifierItems.SOURCE_PREFIX + slot.name(), manaKey,
                    "and must not collide with Growth's, in case the targets ever merge");
            assertTrue(manaKey.startsWith(ManaBankModifierItems.SOURCE_PREFIX));
        }
        // Mutation: set SOURCE_PREFIX to "" or to "growth:" -> reddens.
    }
}
