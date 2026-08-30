package io.github.butterflysmp.rpg.paper.health;

import com.google.common.collect.Multimap;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.Protection;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantValues;
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
 * Reading a player's DEFENSE off the armor they are wearing, and -- separately -- the NATIVE ARMOR
 * that armor puts on the vanilla attribute.
 *
 * <h2>Two numbers, not one, and that separation is the whole point of this class</h2>
 *
 * They were one map until Armor Slice 2a. That was sound only while a piece's Defense equalled the
 * armor its material natively grants, which was true for exactly as long as nothing could add
 * Defense to a piece. The moment one can, the two diverge:
 *
 * <ul>
 *   <li><b>{@link Worn#defense()}</b> is what the STAT converges to -- what each slot contributes to
 *       mitigation. A Protection III chestplate contributes 8 + 9.
 *   <li><b>{@link Worn#nativeArmor()}</b> is what the vanilla {@code armor} attribute actually holds,
 *       which {@link ArmorBarOverride} has to cancel before refilling the bar from damage reduction.
 *       That same chestplate contributes 8, because an enchant of ours puts nothing on the attribute.
 * </ul>
 *
 * Feeding the first where the second belongs makes {@code Defense.barModifier} over-subtract by
 * exactly the enchant's contribution, and the attribute lands negative -- which Minecraft clamps to
 * zero. <b>The bar then reads EMPTY on the most-armored player in the game</b>, while the stat, the
 * mitigation and the tooltip all stay correct. Nothing throws and no test can see it; the boot gate's
 * armor-bar row is what witnesses it.
 *
 * <h2>ONE WALK, returned together</h2>
 *
 * {@link #scan} returns both from a single pass over the four slots rather than offering two methods.
 * Two methods would be two walks, and a player who swapped a piece between them would get a stat and
 * a bar computed from different equipment. Returning a record makes that desync unrepresentable.
 *
 * <p>It is also why the defense map keeps ONE ENTRY PER SLOT whose value is that piece's total,
 * rather than separate entries for the material and the enchant. {@code Stat.putModifier} is
 * put-or-REPLACE, so two sources keyed {@code "CHEST"} would silently mean the second wins; and
 * {@code ModifierReconciler.reconcile} removes every applied source absent from the map it is given,
 * so reconciling twice -- once per source -- would have the two calls annihilate each other. One
 * entry meaning "what this slot contributes to Defense" avoids both without inventing a compound key.
 *
 * <p>The armor values come from VANILLA ITSELF, via
 * {@link ItemType#getDefaultAttributeModifiers(EquipmentSlot)}, never from a hardcoded
 * {@code Material -> armor} table. A table would be the banned in-Java-content pattern, it would
 * silently omit every armor item added by a future Minecraft drop, and it would be a second place for
 * the truth about diamond to live.
 *
 * <p>Keyed by EQUIPMENT SLOT, like {@link HealthModifierItems} and the weapon scanners, so a piece
 * that leaves by any route -- swap, drop, break, death, {@code /clear} -- is simply absent from the
 * map on the next scan and the reconciler drops its source. No departure event to miss.
 *
 * <p>No unit test, for the same reason its siblings have none: every method needs a live Bukkit
 * {@code Player} and an {@code ItemType} registry, neither of which a unit test can construct. The
 * decisions worth testing were pushed into {@code core} -- the curve into
 * {@link io.github.butterflysmp.rpg.core.combat.Defense}, the diff into {@code ModifierReconciler}.
 */
public final class DefenseModifierItems {

    private DefenseModifierItems() {}

    /**
     * What a player's worn armor is contributing right now, read once.
     *
     * @param defense     slot -> the Defense that slot contributes, in armor points. The reconciler's
     *                    desired set. A slot contributing nothing has NO ENTRY rather than a zero --
     *                    absent, not zeroed, so the removal branch does the cleanup.
     * @param nativeArmor the sum the vanilla {@code armor} attribute actually holds, which is what
     *                    {@link ArmorBarOverride} must cancel. <b>Never pass {@code defense}'s sum
     *                    here.</b>
     */
    public record Worn(Map<String, Double> defense, double nativeArmor) {}

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
     * Read both numbers off the player's worn armor in one pass.
     *
     * <p>They DIVERGE as of Armor Slice 2a: Protection adds to the first and not the second, because
     * an enchant of ours writes nothing onto the vanilla armor attribute. A Protection III diamond
     * chestplate contributes 17 to the stat and 8 to the native sum.
     */
    public static Worn scan(Player player, Keys keys, EnchantRegistry enchants) {
        Map<String, Double> defense = new HashMap<>();
        double nativeArmor = 0.0;

        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return new Worn(defense, nativeArmor);

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = equipment.getItem(slot);

            // The material's own points. This -- and ONLY this -- is what the vanilla attribute
            // holds, so it is what the bar has to cancel.
            double vanilla = armorOf(piece, slot);
            nativeArmor += vanilla;

            // Plus whatever this piece's own Protection grants. ONE decode per slot, the hoist
            // ShieldBlock.resolve already models, and the reason the enchant registry is a
            // parameter rather than something read per-enchant.
            double bonus = EnchantValues.totalFor(
                    EnchantItems.read(piece, keys), enchants, EnchantEffect.DEFENSE);

            double contributed = Protection.effectiveDefense(vanilla, bonus);
            if (contributed > 0) defense.put(slot.name(), contributed);
        }
        return new Worn(defense, nativeArmor);
    }

    /** The armor points this item inherently grants in {@code slot}, or 0 if it grants none. */
    private static double armorOf(ItemStack item, EquipmentSlot slot) {
        if (item == null || item.getType().isAir()) return 0.0;
        return vanillaArmorPoints(item.getType().asItemType(), slot);
    }

    /**
     * The armor points an ITEM TYPE inherently grants in {@code slot}, straight out of vanilla.
     *
     * Public because it is the number two subsystems have to agree on, and they are in different
     * packages: this scan feeds the Defense STAT and the armor bar, and {@code ArmorConsistency}
     * checks it at boot against the number a content file DISPLAYS.
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
