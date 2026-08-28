package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code crit_chance_boost_TEMP} and {@code crit_damage_boost_TEMP} dev items.
 *
 * FIXTURES, exactly like {@code attack_speed_boost_TEMP} and {@code health_boost_TEMP}, and for the
 * same reason: crit chance bases at 0.15 and crit damage at 1.0, and no content grants either yet, so
 * without a source "gear can modify it" is provable only by unit test. These make both halves
 * something you can hold and feel. They come out when real content grants crit. See NEXT.md, with the
 * other _TEMP fixtures owing removal.
 *
 * <p>TWO stats in one class rather than two classes, because they are the same fixture twice over --
 * same PDC shape, same slot scan, same reconcile discipline, differing only in which key they carry
 * and which stat they converge. Splitting them would duplicate every line below to vary one field.
 *
 * <p>Both amounts are BONUSES, not resolved values: crit chance resolves {@code 0.15 + Σ(modifiers)}
 * and crit damage {@code 1.0 + Σ(modifiers)}. So {@link #DEFAULT_CHANCE_BOOST} of 0.35 yields a
 * resolved 0.5 -- one swing in two, unmistakable over a short boot rather than a rate you would have
 * to count a hundred swings to believe -- and {@link #DEFAULT_DAMAGE_BOOST} of 1.0 yields a resolved
 * 2.0, i.e. a 3.0x crit against the base 2.0x.
 *
 * <p>Keyed by EQUIPMENT SLOT, like every other fixture, so whatever route an item leaves by -- swap,
 * drop, break, death, {@code /clear} -- it is simply absent from the next scan and the reconciler
 * drops its source. No departure event to miss.
 */
public final class CritModifierItems {

    private CritModifierItems() {}

    /** +0.35 on a base of 0.15 -> a resolved 0.5: crits half the time. Countable in a short boot. */
    public static final double DEFAULT_CHANCE_BOOST = 0.35;

    /** +1.0 on a base of 1.0 -> a resolved 2.0 bonus, i.e. a 3.0x crit rather than 2.0x. */
    public static final double DEFAULT_DAMAGE_BOOST = 1.0;

    /** Mint a crit_chance_boost_TEMP granting {@code amount} crit chance while held or worn. */
    public static ItemStack mintChance(Keys keys, double amount) {
        return mint(keys.critChanceBoost, amount, Material.GOLDEN_SWORD,
                "<gold>Crit Chance <gray>(+" + amount + ")");
    }

    /** Mint a crit_damage_boost_TEMP granting {@code amount} crit BONUS while held or worn. */
    public static ItemStack mintDamage(Keys keys, double amount) {
        return mint(keys.critDamageBoost, amount, Material.GOLDEN_AXE,
                "<gold>Crit Damage <gray>(+" + amount + ")");
    }

    /**
     * The crit-CHANCE modifiers the player's equipped items justify right now, keyed by slot. The
     * "desired" set {@code CombatantStats.reconcileCritChanceModifiers} converges to.
     */
    public static Map<String, Double> desiredChanceModifiers(Player player, Keys keys) {
        return desired(player, keys.critChanceBoost);
    }

    /** The crit-DAMAGE modifiers, same shape. See above. */
    public static Map<String, Double> desiredDamageModifiers(Player player, Keys keys) {
        return desired(player, keys.critDamageBoost);
    }

    private static ItemStack mint(NamespacedKey key, double amount, Material material, String name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize(name + " <dark_gray>[TEMP]")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, amount);
        });
        return item;
    }

    private static Map<String, Double> desired(Player player, NamespacedKey key) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Double amount = boostAmount(equipment.getItem(slot), key);
            if (amount != null) desired.put(slot.name(), amount);
        }
        return desired;
    }

    /** The bonus this item grants under {@code key}, else null. Untagged rejects first. */
    private static Double boostAmount(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
    }
}
