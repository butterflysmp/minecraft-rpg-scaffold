package io.github.butterflysmp.rpg.paper.weapon;

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
 * The attack_speed_boost_TEMP dev item, and reading a player's equipped attack-speed modifiers off it.
 *
 * A FIXTURE, exactly like {@code health_boost_TEMP} -- and it exists for the same reason. The
 * attack-speed stat bases at 1.0 and no content grants a bonus yet, so without a source the whole
 * feature is invisible at boot and provable only by unit test. This makes the stat something you can
 * hold, equip, and feel. It comes out when real content (an enchant, a passive, a build aspect)
 * grants attack speed. See NEXT.md, with the other _TEMP fixtures owing removal.
 *
 * The amount is the BONUS, not the multiplier: the stat resolves {@code 1.0 + Σ(modifiers)}, so
 * {@link #DEFAULT_BOOST} of 1.0 yields a resolved 2.0 -- twice the swing rate, an unmistakable
 * difference at boot rather than one you have to squint at.
 *
 * Keyed by EQUIPMENT SLOT, like the HP fixture, so whatever route the item leaves by -- swap, drop,
 * break, death, /clear -- it is simply absent from the desired map on the next scan and the
 * reconciler drops its source. No departure event to miss.
 */
public final class AttackSpeedModifierItems {

    private AttackSpeedModifierItems() {}

    /** +1.0 on a base of 1.0 -> a resolved 2.0: a 10-tick swing becomes 5. Visible, not subtle. */
    public static final double DEFAULT_BOOST = 1.0;

    /** Mint an attack_speed_boost_TEMP granting {@code amount} attack speed while held or worn. */
    public static ItemStack mint(Keys keys, double amount) {
        ItemStack item = new ItemStack(Material.CLOCK);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<yellow>Attack Speed <gray>(+" + amount + ") <dark_gray>[TEMP]")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(keys.attackSpeedBoost, PersistentDataType.DOUBLE, amount);
        });
        return item;
    }

    /**
     * The attack-speed modifiers the player's currently equipped items justify RIGHT NOW, keyed by
     * slot. This is the "desired" set the attack-speed reconciler converges to.
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

    /** The bonus this item grants if it is an attack_speed_boost_TEMP, else null. Untagged reject first. */
    private static Double boostAmount(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(keys.attackSpeedBoost, PersistentDataType.DOUBLE);
    }
}
