package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * The equipment facts the Equipment screen's armour rules need, read off a LIVE item -- the only
 * place in the plugin that reads the equippable component.
 *
 * <p>Every read here is the pinned API as quoted in the Slice B Phase 1 message (paper-api
 * 26.1.2.build.74-stable sources, implementation read from the 26.1.2 server jar), never an assumption:
 *
 * <ul>
 *   <li>{@code ItemStack.getData(DataComponentTypes.EQUIPPABLE)} reads the item's FULL component
 *       map -- {@code CraftItemStack.getData} reads {@code handle.getComponents()}, a patch over the
 *       prototype -- so a plain vanilla helmet answers with its default component.
 *   <li>{@code Equippable.allowedEntities()} is null when every entity may equip it.
 * </ul>
 *
 * <p>Pure helpers over one ItemStack; they touch no world state and are safe on the thread that owns
 * the player.
 */
final class EquipmentReads {

    private EquipmentReads() {}

    private static final TypedKey<EntityType> PLAYER =
            TypedKey.create(RegistryKey.ENTITY_TYPE, EntityType.PLAYER.key());

    /**
     * The armour slot this item's equippable component names, or empty when it has none or names a
     * non-armour slot (a hand, the body, a saddle). Vanilla's rule: no component means main hand only.
     */
    static Optional<ArmorSlot> armorSlotOf(ItemStack item) {
        // EXPERIMENTAL API (F4): getData and Equippable are @ApiStatus.Experimental in the pinned
        // paper-api. Re-check this call on every Paper bump.
        Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return Optional.empty();
        return switch (equippable.slot()) {
            case HEAD -> Optional.of(ArmorSlot.HEAD);
            case CHEST -> Optional.of(ArmorSlot.CHEST);
            case LEGS -> Optional.of(ArmorSlot.LEGS);
            case FEET -> Optional.of(ArmorSlot.FEET);
            case HAND, OFF_HAND, BODY, SADDLE -> Optional.empty();
        };
    }

    /** Do the item's allowed entities admit a player? True when the component sets none. */
    static boolean allowedForPlayer(ItemStack item) {
        // EXPERIMENTAL API (F4): see armorSlotOf.
        Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return true;
        RegistryKeySet<EntityType> allowed = equippable.allowedEntities();
        return allowed == null || allowed.contains(PLAYER);
    }

    /**
     * Does the item carry Curse of Binding? Vanilla keys its rule on the enchantment EFFECT
     * {@code prevent_armor_change}; in 26.1.2's data only {@code binding_curse} carries it, so the two
     * agree. A datapack adding the effect to another enchantment would diverge -- recorded in
     * {@code ArmorPlacement} and in {@code GATE-accessories-b.md}'s divergence section.
     */
    static boolean bound(ItemStack item) {
        return item.containsEnchantment(Enchantment.BINDING_CURSE);
    }
}
