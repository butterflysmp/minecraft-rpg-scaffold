package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * CONVERT ON ARRIVAL, THE MINT (the seat's ruling, 2026-11-20; core's {@code ArrivalClaims} decides WHETHER). A plain
 * vanilla sword reaching a player becomes the weapon that claims its material, with every acquisition's roll and stamp:
 * the same sequence as the crafting mint ({@code InventoryCraft.commitCraft}): mint, then the enchant roll, then the gear
 * score banded on the player's average.
 *
 * <ul>
 *   <li><b>Its WEAR is carried</b> (clamped to the material's maximum, which is the same material): a used sword stays
 *       used.</li>
 *   <li><b>Its VANILLA ENCHANTMENTS are NOT carried.</b> The minted sword is a fresh mint whose enchants are our roll; a
 *       vanilla Sharpness from a villager or a mob's gear is gone. Flagged for Ben.</li>
 * </ul>
 */
public final class ArrivalConverter {

    private ArrivalConverter() {}

    /** Mint {@code plain} as {@code weapon} for {@code player}. */
    public static ItemStack convert(ItemStack plain, WeaponDefinition weapon, Player player, AdapterContext adapters) {
        ItemStack minted = GearItems.mint(weapon, adapters);
        ItemMeta old = plain.getItemMeta();
        if (old != null) minted.editMeta(meta -> GearItems.carryWear(old, meta, minted.getType()));
        EnchantRollItems.rollOnAcquire(minted, weapon, adapters);
        GearScoreItems.stampOnAcquire(minted, weapon, player, adapters);
        return minted;
    }

    /** Whether the item already carries ANY gear id: a minted weapon, shield, armour piece, tool or accessory. */
    public static boolean tagged(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keys.weaponId) || pdc.has(keys.shieldId) || pdc.has(keys.armorId) || pdc.has(keys.toolId)
                || pdc.has(keys.accessoryId);
    }
}
