package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * The health_boost_TEMP dev item, and reading a player's equipped +max-HP modifiers off it.
 *
 * A fixture, same spirit as swing_TEMP / rooted_TEMP: it exists only to prove the equip/unequip
 * modifier lifecycle before real weapon stats exist, and comes out when they do. An item grants a
 * max-HP modifier WHILE equipped; the amount lives in the item's PDC so different copies can grant
 * different amounts.
 *
 * The modifier is keyed by EQUIPMENT SLOT, not by item: a slot holds one item, so "the +HP from the
 * main hand" is one stable source that add/replaces/removes cleanly as the slot's contents change.
 * This is what generalizes to real weapon stats -- a weapon in the main hand will contribute its
 * stats under the same slot source.
 */
public final class HealthModifierItems {

    private HealthModifierItems() {}

    /** The +max-HP a health_boost_TEMP grants by default -- 100 -> 400, 10 hearts -> 13. */
    public static final double DEFAULT_BOOST = 300.0;

    /** Mint a health_boost_TEMP granting {@code amount} max HP while held or worn. */
    public static ItemStack mint(Keys keys, double amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<red>Health Boost <gray>(+" + (int) amount + ") <dark_gray>[TEMP]")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(keys.healthBoost, PersistentDataType.DOUBLE, amount);
        });
        return item;
    }

    /**
     * The max-HP modifiers the player's currently equipped items justify RIGHT NOW, keyed by slot.
     * This is the "desired" set the reconciler converges to: whatever an item's departure route --
     * drop, swap, break, death, /clear -- the slot simply no longer yields an amount next scan, so
     * its source drops out. No departure event to miss.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Double amount = boostAmount(equipment.getItem(slot), keys);
            if (amount != null) desired.put(slot.name(), amount);
        }
        return desired;
    }

    /** The +HP this item grants if it is a health_boost_TEMP, else null. Fast untagged reject first. */
    private static Double boostAmount(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(keys.healthBoost, PersistentDataType.DOUBLE);
    }
}
