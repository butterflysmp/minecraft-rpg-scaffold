package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.paper.weapon.WeaponAttackItems;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** THE ACCESSORY SOURCE KEYS ARE DISJOINT FROM EVERY OTHER SCANNER'S. The row the key-collision
 * mutation must redden. ***
 *
 * <p>{@code Stat.putModifier} is put-or-REPLACE and a reconcile clears every source absent from its
 * map, so an accessory key equal to another scanner's key would silently replace that source (or be
 * replaced by it) once the maps are merged. This walks the REAL prefix constants and slot names --
 * not the comments, four of which were found describing the quiver-size keys wrongly.
 *
 * <p>Lives in {@code paper.health} because the prefix constants are package-private here.
 */
class AccessorySourceKeysTest {

    /** Every key or key prefix any other scanner in the reconcile loop emits. */
    private static List<String> otherScannersKeysAndPrefixes() {
        List<String> keys = new ArrayList<>(List.of(
                GrowthModifierItems.SOURCE_PREFIX,
                ManaBankModifierItems.SOURCE_PREFIX,
                ManaRegenModifierItems.SOURCE_PREFIX,
                HealthRegenModifierItems.SOURCE_PREFIX,
                QuiverSizeModifierItems.SOURCE_PREFIX,
                ExpandedQuiverModifierItems.SOURCE_PREFIX,
                ReloadTimeModifierItems.SOURCE_PREFIX,
                WeaponAttackItems.MAIN_HAND_SOURCE));
        // The bare-slot scanners (fixtures, attack speed, class damage, crit, defense) key by slot name.
        Arrays.stream(EquipmentSlot.values()).map(EquipmentSlot::name).forEach(keys::add);
        // DamageEnchantItems keys by enchant id -- every shipped enchant file.
        File[] enchants = new File("src/main/resources/content/enchants").listFiles((d, n) -> n.endsWith(".yml"));
        assertTrue(enchants != null && enchants.length > 0, "the enchant roster must be found, not assumed empty");
        for (File f : Objects.requireNonNull(enchants)) keys.add(f.getName().replace(".yml", ""));
        return keys;
    }

    @Test
    void everyAccessoryKeyIsDisjointFromEveryOtherScannersKeysAndPrefixes() {
        List<String> others = otherScannersKeysAndPrefixes();
        assertTrue(others.contains("CHEST") && others.contains("growth:") && others.contains("sharpness"),
                "control: the three kinds of key are all in the list, so an empty scan cannot pass");
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            String key = AccessoryContributions.sourceKey(slot);
            for (String other : others) {
                assertFalse(key.equals(other) || key.startsWith(other) || other.startsWith(key)
                                || other.startsWith(AccessoryContributions.SOURCE_PREFIX),
                        "accessory key '" + key + "' collides with '" + other + "'");
            }
        }
        // Mutation: set AccessoryContributions.SOURCE_PREFIX to "growth:" -> reddens.
    }

    /**
     * The fragment keys (the build system's slice 4) are disjoint from every other scanner's AND from the
     * accessories': the three maps merge into ONE desired map per stat, and a shared key would let one source
     * silently replace another.
     */
    @Test
    void everyFragmentKeyIsDisjointFromTheAccessoriesAndEveryOtherScanner() {
        List<String> others = new ArrayList<>(otherScannersKeysAndPrefixes());
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) others.add(AccessoryContributions.sourceKey(slot));
        assertTrue(others.contains("accessory:0") && others.contains("growth:"),
                "control: the accessory keys and a scanner prefix are in the list");
        for (int slot = 0; slot < io.github.butterflysmp.rpg.core.build.FragmentSlots.COUNT; slot++) {
            String key = io.github.butterflysmp.rpg.core.build.FragmentSlots.sourceKey(slot);
            for (String other : others) {
                assertFalse(key.equals(other) || key.startsWith(other) || other.startsWith(key)
                                || other.startsWith(io.github.butterflysmp.rpg.core.build.FragmentSlots.SOURCE_PREFIX),
                        "fragment key '" + key + "' collides with '" + other + "'");
            }
        }
    }

    @Test
    void noAccessoryKeyContainsQuiver_rulingA2() {
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            assertFalse(AccessoryContributions.sourceKey(slot).toLowerCase().contains("quiver"));
        }
        assertEquals("accessory:", AccessoryContributions.SOURCE_PREFIX);
    }
}
