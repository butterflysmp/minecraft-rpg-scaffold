package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.core.weapon.QuiverMirror;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.weapon.QuiverItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * THE QUIVER IN THE NEXUS SLOT, DRAWN (2026-11-22; the decision is core's {@link QuiverMirror}). The write is thin on
 * purpose: find the Nexus item, ask the decision, replace the item only when what it shows must change.
 *
 * <p><b>THE WEAPON'S STAMPED COUNT IS THE TRUTH</b> (the seat's ruling 1): {@code QuiverItems.loadedIn} of the item in
 * his MAIN hand, present only on a minted quiver weapon. Nothing reads a quiver from the slot.
 *
 * <p><b>The star's state needs no profile read here</b>: when the star is off its item is removed
 * ({@code NexusSlots.removeStars}), so finding no Nexus item IS "off", and there is nothing to draw.
 *
 * <p><b>Re-drawn</b> after a shot and a reload's begin and end ({@code Quivers}), on a held-item change and at join and
 * respawn ({@code RpgListeners}), when the star is turned on ({@code SettingsMenu}), after the selftest's restore
 * ({@code SelfTestRun}), and on every stats-bar beat (every 10 ticks, {@code StatsBarSystem}) as the backstop. At quit
 * and at the plugin's disable it is put back to the STAR ({@link #restoreStar}), so a mirror is never saved: no saved
 * inventory can ever hold arrows where the star was.
 */
public final class NexusMirror {

    private NexusMirror() {}

    /** Re-draw from the item in his main hand. */
    public static void redraw(Player player, Keys keys) {
        redraw(player, keys, player.getInventory().getItemInMainHand());
    }

    /** Re-draw as if {@code held} were in his main hand (a held-item change, before the slot switches). */
    public static void redraw(Player player, Keys keys, ItemStack held) {
        PlayerInventory inventory = player.getInventory();
        int at = nexusSlot(inventory, keys);
        if (at < 0) return;   // the star is off (or not yet placed): nothing of ours to change
        QuiverMirror.Shown shown = QuiverMirror.decide(true, QuiverItems.loadedIn(held, keys));
        ItemStack current = inventory.getItem(at);
        if (shown instanceof QuiverMirror.Shown.Arrows arrows) {
            if (current.getType() == Material.ARROW && current.getAmount() == arrows.count()) return;
            inventory.setItem(at, NexusItems.mirror(keys, arrows.count()));
        } else {
            if (current.getType() == Material.NETHER_STAR) return;
            inventory.setItem(at, NexusItems.mint(keys));
        }
    }

    /** Put the star back wherever the mirror is: at quit and at disable, so no saved inventory holds the mirror. */
    public static void restoreStar(Player player, Keys keys) {
        PlayerInventory inventory = player.getInventory();
        int at = nexusSlot(inventory, keys);
        if (at < 0) return;
        if (inventory.getItem(at).getType() != Material.NETHER_STAR) inventory.setItem(at, NexusItems.mint(keys));
    }

    /** The slot holding the Nexus item (star or mirror), or -1. Found by its TAG, never by its material. */
    static int nexusSlot(PlayerInventory inventory, Keys keys) {
        for (int index = 0; index < inventory.getSize(); index++) {
            if (NexusItems.isNexus(inventory.getItem(index), keys)) return index;
        }
        return -1;
    }
}
