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

    /** The Healing Tome as shipped (healing_tome.yml): no modifiers, one Radiant Pulse. */
    static AccessoryBehaviour.HealingPulse radiantPulse() {
        return new AccessoryBehaviour.HealingPulse("Radiant Pulse", 1.0, 80, 8.0, AccessoryBehaviour.Targets.PLAYERS,
                "happy_villager", 6, "heart", "block.amethyst_block.chime");
    }

    static AccessoryDefinition healingTome() {
        return new AccessoryDefinition("healing_tome", "Healing Tome", Rarity.RARE, "book", AccessorySlotKind.CLASS,
                WeaponClass.MAGE, AccessoryType.TOME, Map.of(), List.of(), radiantPulse());
    }

    /** The Blink Tome's Blink as shipped (blink_tome.yml). */
    static AccessoryBehaviour.Blink blink() {
        return new AccessoryBehaviour.Blink("Blink", 5.0, 16.0, 5.0, 160, 1200, "poof", 12,
                "entity.enderman.teleport");
    }

    static AccessoryDefinition blinkTome() {
        return new AccessoryDefinition("blink_tome", "Blink Tome", Rarity.RARE, "book", AccessorySlotKind.CLASS,
                WeaponClass.MAGE, AccessoryType.TOME, Map.of(), List.of(), blink());
    }

    static WeaponClass ranger() {
        return WeaponClass.RANGER;
    }
}
