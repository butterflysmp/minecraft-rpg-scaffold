package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one thing about the Growth scan a unit test can reach: that its source keys cannot collide
 * with {@link HealthModifierItems}'.
 *
 * <p>{@code desiredModifiers} itself needs a live {@code Player} and is boot-witnessed, like every
 * scanner in this package. But the PREFIX is a plain constant, and it is the whole reason the two
 * scanners can share one reconcile map -- so it gets the one assertion that can be made offline.
 *
 * <p>{@code GrowthTest} in core pins the CONSEQUENCE (two sources on one key silently replace, two
 * namespaced sources sum). This pins the cause: that the prefix is actually there and actually
 * disjoint.
 */
class GrowthModifierItemsTest {

    @Test
    void everyGrowthSourceKeyIsDisjointFromABareSlotName() {
        // HealthModifierItems walks ALL EquipmentSlot values and keys by slot.name(). If Growth used
        // the same shape, a health_boost_TEMP and a Growth piece in one slot would collide on one
        // key and Stat.putModifier -- which is put-or-REPLACE -- would keep only one of them.
        assertFalse(GrowthModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix is exactly the collision this constant exists to prevent");

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String growthKey = GrowthModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), growthKey,
                    "Growth's key for " + slot + " must not be the bare slot name");
            assertTrue(growthKey.startsWith(GrowthModifierItems.SOURCE_PREFIX),
                    "and must carry the prefix that makes it disjoint");
        }
        // Mutation: set SOURCE_PREFIX to "" -> reddens here, and in the live scan a fixture item and
        // a Growth piece in the same slot would silently erase one another.
    }
}
