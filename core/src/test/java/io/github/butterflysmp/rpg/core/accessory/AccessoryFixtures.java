package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;

import java.util.List;
import java.util.Map;

/** Definitions the accessory tests share. Not a test class. */
final class AccessoryFixtures {

    private AccessoryFixtures() {}

    static AccessoryDefinition universal(String id, Map<AccessoryStat, Double> modifiers) {
        return new AccessoryDefinition(id, "Test " + id, Rarity.UNCOMMON, "echo_shard",
                AccessorySlotKind.UNIVERSAL, null, null, modifiers, List.of());
    }

    static AccessoryDefinition classItem(String id, AccessoryType type, Map<AccessoryStat, Double> modifiers) {
        return new AccessoryDefinition(id, "Test " + id, Rarity.RARE, "shulker_shell",
                AccessorySlotKind.CLASS, type.weaponClass(), type, modifiers, List.of());
    }

    static AccessoryDefinition wardCharm() {
        return universal("ward_charm", Map.of(AccessoryStat.DEFENSE, 3.0));
    }

    static AccessoryDefinition fletchersQuiver() {
        return classItem("fletchers_quiver", AccessoryType.QUIVER, Map.of(
                AccessoryStat.CLASS_DAMAGE, 3.0,
                AccessoryStat.CRIT_CHANCE, 0.05,
                AccessoryStat.HEALTH_REGEN, -0.04));
    }

    static AccessoryDefinition sagesScroll() {
        return classItem("sages_scroll", AccessoryType.SCROLL, Map.of(
                AccessoryStat.CLASS_DAMAGE, 3.0,
                AccessoryStat.MAX_MANA, 20.0,
                AccessoryStat.CRIT_CHANCE, -0.03));
    }

    static WeaponClass ranger() {
        return WeaponClass.RANGER;
    }
}
