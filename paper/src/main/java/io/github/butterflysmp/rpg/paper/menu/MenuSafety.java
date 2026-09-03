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

    /**
     * Would this whole stack fit in the player's inventory, without anything reaching the ground?
     *
     * <p><b>The look-before-you-leap half of {@link #give}, and it exists because a BULK loop must
     * not rely on the drop branch.</b> {@code give} is a last line of defence: it is correct for one
     * item the player already owned, where the ground is better than deletion. It is the wrong
     * answer sixty-four times in a row -- that is a pile of entities at the player's feet, which is
     * a lag vector as well as a surprise, and no message repeated sixty-four times helps.
     *
     * <p>So a bulk loop asks this BEFORE each pass and stops cleanly when the answer is no, having
     * crafted only what it could hand over. Nothing reaches the ground for any item, whatever its
     * stack size.
     *
     * <p><b>Deliberately not "is there an empty slot".</b> A partially-filled matching stack is real
     * room, and ignoring it would stop a stick craft with 63 sticks and eleven free slots. The room
     * is summed the way {@code addItem} actually fills: partial matching stacks first, then empties.
     *
     * <p>Storage contents only -- the 36 main slots, which is exactly what {@code addItem} uses.
     * Armor and the offhand are not somewhere a craft result may land.
     */
    public static boolean fits(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return true;

        int needed = item.getAmount();
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                needed -= item.getMaxStackSize();
            } else if (slot.isSimilar(item)) {
                needed -= Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            }
            if (needed <= 0) return true;
        }
        return needed <= 0;
    }
}
