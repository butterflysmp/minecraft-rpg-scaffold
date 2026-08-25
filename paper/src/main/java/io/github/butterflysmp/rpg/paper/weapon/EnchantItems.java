package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantCodec;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reading and writing an item's enchant state. The Bukkit half only.
 *
 * Same split as {@link WeaponItems} and {@code ClassDamageModifierItems}: every DECISION about
 * enchant state lives in {@code core/enchant} where a unit test can reach it, and this holds
 * nothing but the PDC I/O, which needs a running server and so is boot-witnessed instead.
 *
 * <p>Nothing here interprets the blob beyond handing it to {@link EnchantCodec}, and the codec is
 * total -- so a malformed or future-grammar string produces an empty state rather than an
 * exception. That matters most at {@link #activeLevel}, which runs on the swing path.
 */
public final class EnchantItems {

    private EnchantItems() {}

    /**
     * The enchant state on an item's meta, or an empty one.
     *
     * Never null and never throws, for any item, including one whose blob was written by a build
     * that does not exist yet.
     */
    public static EnchantState read(ItemMeta meta, Keys keys) {
        if (meta == null) return EnchantState.empty();
        return EnchantCodec.decode(meta.getPersistentDataContainer()
                .get(keys.enchantData, PersistentDataType.STRING));
    }

    /**
     * The same read, widened to a stack.
     *
     * The null guard is the addition, for the reason {@code WeaponItems.weaponId} gives: an
     * inventory is mostly empty slots, and {@code getContents()} is full of nulls.
     */
    public static EnchantState read(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return EnchantState.empty();
        return read(item.getItemMeta(), keys);
    }

    /**
     * Write the state, and mark the item as rolled.
     *
     * The rolled flag is set even when the state is EMPTY, which is the point of it being a
     * separate key: "this item's slots have been decided, and they came to nothing" has to be
     * distinguishable from "this item has never been through the process". Pass 1 only ever writes
     * it; the roster pass is what reads it, to refuse a second roll.
     */
    public static void write(ItemMeta meta, EnchantState state, Keys keys) {
        meta.getPersistentDataContainer()
                .set(keys.enchantData, PersistentDataType.STRING, EnchantCodec.encode(state));
        meta.getPersistentDataContainer().set(keys.enchantRolled, PersistentDataType.BYTE, (byte) 1);
    }

    /** Remove BOTH keys, returning the item to never-enchanted rather than rolled-empty. */
    public static void clear(ItemMeta meta, Keys keys) {
        meta.getPersistentDataContainer().remove(keys.enchantData);
        meta.getPersistentDataContainer().remove(keys.enchantRolled);
    }

    /** Has this item been through a roll, whatever the roll produced? Reserved; nothing reads it yet. */
    public static boolean isRolled(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte flag = item.getItemMeta().getPersistentDataContainer()
                .get(keys.enchantRolled, PersistentDataType.BYTE);
        return flag != null && flag == 1;
    }

    /**
     * The level {@code enchantId} is ACTIVE at on this item, or 0. THE SEAM'S READ.
     *
     * <p>Runs on every basic attack that reaches {@code WeaponDurability.applyWearOnUse}, so the
     * first act is the cheapest possible reject: an item with no meta, or with no enchant blob at
     * all, returns 0 having decoded nothing and allocated nothing. That is the overwhelmingly
     * common case -- every unenchanted weapon, every swing -- and it is the same fast-reject shape
     * {@code WeaponItems.weaponId} uses for the same reason.
     *
     * <p>Deliberately does NOT consult the enchant registry. {@code Unbreaking.consumes} clamps the
     * level itself, so behaviour needs no lookup, and skipping it keeps the hot path to one PDC
     * read plus a short parse. It also means an enchant whose content file was deleted keeps
     * WORKING rather than silently switching off, which is why the tooltip renders an unknown id
     * instead of hiding it: the two agree.
     */
    public static int activeLevel(ItemStack item, Keys keys, String enchantId) {
        if (item == null || !item.hasItemMeta()) return 0;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(keys.enchantData, PersistentDataType.STRING);
        if (raw == null) return 0;
        return EnchantCodec.decode(raw).activeLevel(enchantId);
    }
}
