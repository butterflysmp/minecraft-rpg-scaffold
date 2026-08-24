package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.RefreshVerdict;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * The Lore Refresher: rebuild every custom weapon in a player's inventory from the content loaded
 * now, so an item minted before a content edit stops showing the old edit's display.
 *
 * Only the DISPLAY was ever stale. A weapon's behaviour has always been current -- swinging it
 * reads weapon_id off the item and looks up the CURRENT definition, so triggers and attack damage
 * follow a content edit with no item change at all. What mint() bakes does not: the name, the lore,
 * the material, the melee suppressor. So an item is weapon_id (persistent) plus a display derived
 * from content (a cache), and this rebuilds the cache. That is also the re-mintability invariant
 * the rarity/enchant work will lean on.
 *
 * Unconditional: every tagged item is re-minted on every pass, with no version stamp to skip
 * unchanged ones. Idempotent, and there is nothing yet to optimise -- 41 slots and a PDC read.
 *
 * The decision for each slot is NOT here: it is {@link RefreshVerdict#decide}, in core, where it can
 * be unit-tested. This class is the Bukkit half -- reading stacks, writing stacks -- and is
 * boot-witnessed for the same reason WeaponItems is: an ItemStack cannot be built without a running
 * server.
 */
public final class WeaponRefresher {

    private WeaponRefresher() {}

    /**
     * Refresh every custom weapon this player is carrying, and report how many were rebuilt.
     *
     * Scans {@code getContents()}, which for a PlayerInventory is all 41 slots in one array --
     * 0-35 storage and hotbar, 36-39 armour, 40 offhand. One loop, no slot-category enumeration,
     * and nothing carried in a hand or worn is missed. Weapons are single items, so replacing a
     * slot outright has no stack-count concern.
     *
     * The count is the caller's evidence that the scan actually did something. A scan that finds
     * nothing and a scan that silently failed look identical otherwise, which is the failure
     * CLAUDE.md's verification section is about -- so /rpg refresh reports this number rather than
     * saying "done".
     *
     * Must run on the thread that owns this player: it reads and writes their inventory. Join is
     * already on it; a command is not, and hops.
     */
    public static int refresh(Player player, WeaponRegistry weapons, AdapterContext adapters) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        int refreshed = 0;

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            String id = WeaponItems.weaponId(item, adapters.keys()).orElse(null);

            // Exhaustive over a sealed interface, with NO default arm: a fourth verdict becomes a
            // compile error here until it is handled, rather than being swallowed by a catch-all.
            switch (RefreshVerdict.decide(id, weapons)) {
                case RefreshVerdict.Untagged ignored -> { }   // not ours; leave it completely alone
                // Content was renamed or deleted under an item someone is holding. Leave the item
                // exactly as it is -- stripping it would cost a player a real weapon over a typo,
                // and it starts working again the moment the definition comes back. warnOnce, not
                // warning: this runs per player per login, and would otherwise repeat forever.
                case RefreshVerdict.Dangling dangling -> adapters.warnOnce(
                        "Refresh: item carries unknown weapon id '" + dangling.weaponId()
                                + "' -- left untouched; its content file is missing or renamed");
                case RefreshVerdict.Remint remint -> {
                    inventory.setItem(slot, WeaponItems.remint(item, remint.definition(), adapters));
                    refreshed++;
                }
            }
        }
        return refreshed;
    }
}
