package io.github.butterflysmp.rpg.paper.health;

import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading a player's DEFENSE off the vanilla armor they are wearing.
 *
 * Not a fixture, unlike {@code health_boost_TEMP} and its siblings: the source here is real, shipped
 * content -- a diamond chestplate -- so this feature is visible at boot on day one and needs no dev
 * item to demonstrate.
 *
 * <p>The armor values come from VANILLA ITSELF, via
 * {@link ItemType#getDefaultAttributeModifiers(EquipmentSlot)}, never from a hardcoded
 * {@code Material -> armor} table. A table would be the banned in-Java-content pattern, it would
 * silently omit every armor item added by a future Minecraft drop, and it would be a second place for
 * the truth about diamond to live. Vanilla's own numbers are already in the jar; this asks for them.
 *
 * <p>Keyed by EQUIPMENT SLOT, like {@link HealthModifierItems} and the two weapon scanners, so a
 * piece that leaves by any route -- swap, drop, break, death, {@code /clear} -- is simply absent from
 * the desired map on the next scan and the reconciler drops its source. No departure event to miss.
 *
 * <p>DEFAULT modifiers, not the item's effective ones, which is the right read for this pass and a
 * known seam for the next: custom gear that mints its own armor value onto an ItemStack would
 * override the default and go unseen here. That is the custom-gear crafting system, explicitly out of
 * scope; when it lands, this is the one method that has to learn about it.
 *
 * <p>No unit test, for the same reason its four siblings have none: every method needs a live Bukkit
 * {@code Player} and an {@code ItemType} registry, neither of which a unit test can construct. The
 * decisions worth testing were pushed into {@code core} -- the curve into
 * {@link io.github.butterflysmp.rpg.core.combat.Defense}, the diff into {@code ModifierReconciler} --
 * leaving only the Bukkit read here. The boot gate is what witnesses this file.
 */
public final class DefenseModifierItems {

    private DefenseModifierItems() {}

    /**
     * The four slots that can carry armor.
     *
     * Named explicitly rather than walking {@code EquipmentSlot.values()}, which also yields
     * {@code HAND}, {@code OFF_HAND}, {@code BODY} and {@code SADDLE}. A held helmet would contribute
     * nothing anyway -- its modifier is scoped to the HEAD slot group -- but naming the four states
     * the intent, and cannot start reading a horse's barding the next time the enum grows.
     */
    static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * The defense this player's worn armor grants right now, keyed by slot, in vanilla armor points.
     * This is the "desired" set {@code CombatantStats.reconcileDefenseModifiers} converges to, and its
     * summed value is the {@code nativeArmor} {@link ArmorBarOverride} must cancel.
     *
     * A slot holding nothing, or holding something with no armor value, contributes NO ENTRY rather
     * than a zero -- absent, not zeroed, so the reconciler's removal branch does the cleanup.
     */
    public static Map<String, Double> desiredModifiers(Player player) {
        Map<String, Double> worn = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return worn;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            double armor = armorOf(equipment.getItem(slot), slot);
            if (armor > 0) worn.put(slot.name(), armor);
        }
        return worn;
    }

    /** The summed value of a desired map -- the native armor total the bar override has to cancel. */
    public static double total(Map<String, Double> desired) {
        double sum = 0.0;
        for (double armor : desired.values()) sum += armor;
        return sum;
    }

    /**
     * The armor points this item inherently grants in {@code slot}, or 0 if it grants none.
     *
     * Sums ONLY {@link AttributeModifier.Operation#ADD_NUMBER} modifiers. Every vanilla armor
     * modifier is flat, but a scaling one summed as though it were flat would be silently wrong --
     * and "silently wrong" on this path means a bar that misreports how much damage you are turning
     * away, which is precisely the failure this pass exists to avoid.
     */
    private static double armorOf(ItemStack item, EquipmentSlot slot) {
        if (item == null || item.getType().isAir()) return 0.0;
        return vanillaArmorPoints(item.getType().asItemType(), slot);
    }

    /**
     * The armor points an ITEM TYPE inherently grants in {@code slot}, straight out of vanilla.
     *
     * Public because it is the number two subsystems have to agree on, and they are in different
     * packages: this scan feeds the Defense STAT, and {@code ArmorConsistency} checks it at boot
     * against the number a content file DISPLAYS. That check used to repeat this body verbatim so
     * the armor slice could leave this file byte-identical -- a deliberate, recorded, temporary
     * duplication, and the gear extraction is where it was always going to be paid off.
     *
     * One copy matters more here than in most places. If the two reads ever disagreed, the boot
     * check would be verifying content against a number the stat does not actually use, and it
     * would report a clean run while every armor tooltip lied.
     *
     * Sums ONLY {@link AttributeModifier.Operation#ADD_NUMBER} modifiers. Every vanilla armor
     * modifier is flat, but a scaling one summed as though it were flat would be silently wrong.
     */
    public static double vanillaArmorPoints(ItemType type, EquipmentSlot slot) {
        if (type == null) return 0.0;
        Multimap<Attribute, AttributeModifier> defaults = type.getDefaultAttributeModifiers(slot);
        double armor = 0.0;
        for (AttributeModifier modifier : defaults.get(Attribute.ARMOR)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                armor += modifier.getAmount();
            }
        }
        return armor;
    }
}
