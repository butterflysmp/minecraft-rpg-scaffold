package io.github.butterflysmp.rpg.core.weapon;

/**
 * The four body slots armor is worn in.
 *
 * A closed, unordered enum, for the same reason {@link Rarity} is one and elements are not: this is
 * a fixed axis Minecraft decided, not authored content. Adding a fifth is a Minecraft event, not a
 * content edit.
 *
 * <p><b>THE CONSTANT NAMES ARE LOAD-BEARING AND MUST NOT BE PRETTIED.</b> They are the strings
 * {@code DefenseModifierItems.desiredModifiers} keys its map by -- it uses
 * {@code EquipmentSlot.name()}, and the reconciler matches sources by that string. So
 * {@code ArmorSlot.HEAD.name()} must equal {@code EquipmentSlot.HEAD.name()} exactly. Renaming this
 * constant to {@code HELMET} would compile everywhere, read better, and silently break nothing that
 * any test could see except the one written for it. That test exists; see {@code ArmorSlotTest}.
 *
 * <p>Deliberately NOT {@code GearClass}. That axis is enchant gating -- which enchants may sit on
 * which gear -- and it gains its armor constant when armor gets enchants. This axis is which limb
 * the item covers. They answer different questions and would fight if merged: gating may well end up
 * treating all armor as one class while this must always distinguish four.
 *
 * <p>{@code core} cannot import {@code org.bukkit.inventory.EquipmentSlot}, which is the whole
 * reason this enum exists rather than the Bukkit one being passed around.
 */
public enum ArmorSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET;

    /**
     * Case-insensitive lookup for the content loader. Returns null on a miss so the caller decides
     * what a bad name means -- the armor loader throws, turning a bad slot into a named, skipped
     * file, the same contract {@link Rarity#fromName} has.
     */
    public static ArmorSlot fromName(String name) {
        if (name == null) return null;
        for (ArmorSlot slot : values()) {
            if (slot.name().equalsIgnoreCase(name)) return slot;
        }
        return null;
    }
}
