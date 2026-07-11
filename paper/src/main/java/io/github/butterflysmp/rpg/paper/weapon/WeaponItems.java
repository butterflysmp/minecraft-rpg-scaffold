package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Minting a weapon item, and recognising one. An item is one of ours IFF it carries the
 * weapon_id PDC tag -- no tag, not a weapon, and the system leaves it untouched.
 *
 * This is the only place that reads or writes the weapon_id tag. The read is on the hot
 * path: the swing packet fires for every player on every click in 1b, so heldWeaponId's
 * FIRST act is the cheapest possible reject -- no meta means not ours -- before any
 * lookup or allocation.
 */
public final class WeaponItems {

    private WeaponItems() {}

    /**
     * The vanilla melee a held item deals is a SEPARATE damage path from a weapon's trigger.
     * A left-click swing (1b) would otherwise land both -- the iron sword's vanilla ~6 AND
     * the trigger's content damage -- double-hitting. We cancel the vanilla path so the
     * trigger's number stays authoritative.
     *
     * These two are the single source of truth for that decision, kept server-free so the
     * choice is unit-testable without constructing an ItemStack (which needs a running
     * server). mint() DERIVES the Bukkit Attribute from ATTACK_DAMAGE_ATTRIBUTE below, so
     * the constant the test asserts is the same one that reaches the item -- they cannot
     * drift. The load-bearing property: this touches ONLY vanilla attack damage; the
     * weapon's own damage flows through EffectSpec.Damage -> CombatantHandle.applyDamage,
     * which never goes through an attribute (see WeaponServiceTest, which damages with no
     * item at all).
     */
    public static final String ATTACK_DAMAGE_ATTRIBUTE = "attack_damage";

    /** A player's base attack_damage is 1.0, so -1.0 brings a held swing to a flat 0. */
    public static final double VANILLA_MELEE_SUPPRESSION = -1.0;

    /**
     * The item a weapon is carried in. Its display name is coloured by rarity (an authored
     * colour in the name wins), it carries weapon_id in its PDC -- the whole of its identity
     * -- and it carries the attack-damage suppressor above so the swing's vanilla melee is
     * zeroed. Everything else about the weapon lives in its content file.
     *
     * Phase 1 has no per-weapon material, so every weapon mints as a sword; that becomes a
     * weapon field when a non-melee weapon (the bow) needs a different item.
     */
    public static ItemStack mint(WeaponDefinition weapon, Keys keys) {
        ItemStack item = new ItemStack(materialOf(weapon.material()));
        Attribute attackDamage = Registry.ATTRIBUTE.getOrThrow(
                NamespacedKey.minecraft(ATTACK_DAMAGE_ATTRIBUTE));
        AttributeModifier suppressor = new AttributeModifier(
                keys.meleeSuppressor, VANILLA_MELEE_SUPPRESSION,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize(weapon.displayName())
                    .colorIfAbsent(RarityColors.of(weapon.rarity()))
                    // Item names render italic by default; a weapon name should read plainly.
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, weapon.id());
            // Setting an explicit attack_damage modifier suppresses the item's vanilla
            // default (+6 for iron), so the swing's melee is base 1.0 + (-1.0) = 0.
            meta.addAttributeModifier(attackDamage, suppressor);
        });
        return item;
    }

    /**
     * Resolve a weapon's material string ("bow", "iron_sword") to a Bukkit Material. An
     * unknown material falls back to a sword rather than crashing the give -- a wrong item
     * is visible in-game, where it can be fixed, and never blocks a boot.
     */
    private static Material materialOf(String material) {
        Material resolved = Material.matchMaterial(material);
        return resolved != null ? resolved : Material.IRON_SWORD;
    }

    /**
     * The weapon id of the player's main-hand item, if it is one of ours.
     *
     * First branch is the untagged fast-path reject: an empty hand, a dirt block, a
     * vanilla sword all lack item meta and return empty here having cost nothing. This is
     * the shape 1b's packet listener calls, so it must stay allocation-free on a miss.
     */
    public static Optional<String> heldWeaponId(Player player, Keys keys) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!held.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(held.getItemMeta().getPersistentDataContainer()
                .get(keys.weaponId, PersistentDataType.STRING));
    }
}
