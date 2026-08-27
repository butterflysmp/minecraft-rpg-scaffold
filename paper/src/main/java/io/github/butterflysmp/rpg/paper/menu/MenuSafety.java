package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Giving a player their own item back, without ever losing it.
 *
 * <p>Its own class rather than a private method because it is the last line of defence for every
 * menu that will ever hold something of a player's, and a menu that reimplements it will
 * reimplement it slightly worse.
 */
public final class MenuSafety {

    private MenuSafety() {}

    /**
     * Hand an item back: the inventory first, the ground at the player's feet when it is full.
     *
     * <p><b>Never silently discards, and never silently drops either.</b> A weapon appearing on the
     * floor with no explanation reads as a bug, and a player who does not notice loses it to
     * despawn -- so the drop branch says so. Discarding is not a branch at all: there is no path
     * through here that ends with the item gone.
     *
     * <p>{@code addItem} returns what would not fit rather than throwing, and returns an EMPTY map
     * on success -- so the leftover loop is the whole of the full-inventory case.
     */
    public static void give(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (leftover.isEmpty()) return;

        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
        player.sendMessage(Component.text("Your inventory was full -- dropped at your feet.",
                NamedTextColor.YELLOW));
    }
}
