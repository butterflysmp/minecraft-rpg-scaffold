package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
     * The item a weapon is carried in. Its display name is coloured by rarity (an authored
     * colour in the name wins), and it carries weapon_id in its PDC -- which is the whole
     * of its identity. Everything else about the weapon lives in its content file.
     *
     * Phase 1 has no per-weapon material, so every weapon mints as a sword; that becomes a
     * weapon field when a non-melee weapon (the bow) needs a different item.
     */
    public static ItemStack mint(WeaponDefinition weapon, Keys keys) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize(weapon.displayName())
                    .colorIfAbsent(RarityColors.of(weapon.rarity()))
                    // Item names render italic by default; a weapon name should read plainly.
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, weapon.id());
        });
        return item;
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
