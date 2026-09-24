package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Minting and reading accessory items -- the fifth gear kind's {@code ShieldItems}.
 *
 * <h2>The item carries its ID and nothing else</h2>
 *
 * An accessory's stats come from its DEFINITION, looked up by id, and are read only by the accessory
 * scanner from the player's accessory store. No stat key is written here. The all-slot scanners
 * ({@code HealthModifierItems}, {@code CritModifierItems}, the regen scans...) read their keys off
 * ANY equipped stack, main hand included, so a stat key on this item would grant the stat while the
 * accessory was merely held. {@code AccessoryItemsWritesOnlyItsIdTest} asserts that this file writes
 * exactly one PDC key.
 *
 * <h2>What the mint does not do</h2>
 *
 * No attribute modifiers (the materials carry none), no durability (none of them has any), no enchant
 * container and no gear score (ruling A4). {@code setMaxStackSize(1)} per the standing decision: every
 * minted item is a single item, and the four accessory materials stack to 64 on their own.
 */
public final class AccessoryItems {

    private AccessoryItems() {}

    public static ItemStack mint(AccessoryDefinition accessory, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack item = new ItemStack(materialOf(accessory));
        item.editMeta(meta -> {
            meta.displayName(WeaponItems.displayName(accessory.displayName(), accessory.rarity()));
            meta.getPersistentDataContainer().set(keys.accessoryId, PersistentDataType.STRING, accessory.id());
            meta.setMaxStackSize(1);   // an accessory is always a single item
            applyLore(meta, accessory);
        });
        return item;
    }

    /**
     * Re-mint against the current definition. Only the id tag is carried: an accessory has no wear,
     * no enchants and no score, so {@code GearItems.carryInstanceData}'s other four carries have
     * nothing to move -- and calling it anyway would invite a future carry (a score, say) onto a kind
     * ruled out of it.
     */
    public static ItemStack remint(ItemStack old, AccessoryDefinition current, AdapterContext adapters) {
        ItemStack fresh = mint(current, adapters);
        ItemMeta oldMeta = old.getItemMeta();
        if (oldMeta == null) return fresh;
        fresh.editMeta(meta -> GearItems.carryTag(oldMeta, meta, adapters.keys().accessoryId));
        return fresh;
    }

    public static void refreshLore(ItemMeta meta, AccessoryDefinition accessory, AdapterContext adapters) {
        applyLore(meta, accessory);
    }

    private static void applyLore(ItemMeta meta, AccessoryDefinition accessory) {
        meta.lore(AccessoryLore.build(accessory));
    }

    /**
     * The definition's material. It is one of {@code AccessoryDefinition.MATERIALS}, checked at load,
     * so a miss here means the server's material registry disagrees with that allowlist -- and the
     * fallback is the universal accessory's inert material, never something wearable.
     */
    private static Material materialOf(AccessoryDefinition accessory) {
        Material resolved = Material.matchMaterial(accessory.material());
        return resolved != null ? resolved : Material.ECHO_SHARD;
    }

    /** The accessory id on any item, if it carries one. */
    public static Optional<String> accessoryId(ItemStack item, Keys keys) {
        return GearItems.idOf(item, keys.accessoryId);
    }

    /** True when {@code item} is one of our accessories. */
    public static boolean isAccessory(ItemStack item, Keys keys) {
        return accessoryId(item, keys).isPresent();
    }
}
