package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.build.Loadout;
import io.github.butterflysmp.rpg.core.build.LockedSlots;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.nexus.LockedItem;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

/**
 * What the Ability Stone needs that is fixed at boot: its key, the pools, and the ability registry for
 * display names. Carried on {@code AdapterContext} because the menus that toggle and move the stone
 * (Settings, the picker) already receive one, and threading a new argument through every
 * {@code NexusMenu} construction to reach them is the worse trade.
 *
 * <p>No player state lives here -- the profile is always passed in (CLAUDE.md invariant 3).
 */
public final class Stones {

    private final Keys keys;
    private final PoolRegistry pools;
    private final AbilityRegistry abilities;

    public Stones(Keys keys, PoolRegistry pools, AbilityRegistry abilities) {
        this.keys = keys;
        this.pools = pools;
        this.abilities = abilities;
    }

    public PoolRegistry pools() { return pools; }

    public AbilityRegistry abilities() { return abilities; }

    /**
     * The loadout this profile's cell casts. Slice 1: the pool's {@code default}, because nothing is
     * saved yet (slice 2 adds the saved one). Empty for a class or element of {@code none}, a null class
     * (PLAN-accessories 8.4: a null class survives load), or a cell with no pool.
     */
    public Optional<Loadout> loadoutOf(Optional<PlayerProfile> profile) {
        return profile.flatMap(p -> pools.find(p.archetypeId(), p.elementId()))
                .map(pool -> pool.defaultLoadout());
    }

    /** The stone as a locked item: hotbar only (ruling 2), default slot 7 (ruling 13). */
    public LockedItem lockedItem(Optional<PlayerProfile> profile) {
        return new LockedItem("the Ability Stone", item -> StoneItems.isStone(item, keys),
                () -> StoneItems.mint(keys, loadoutOf(profile), abilities),
                LockedSlots.DEFAULT_STONE_SLOT, LockedSlots.STONE_MAX_SLOT);
    }

    /**
     * Re-render the lore of every stone the player carries, for a loadout that changed under it (a class
     * or element change). Convergence keeps an existing stone rather than re-minting it, so without this
     * a stone would keep naming the old cell's abilities.
     */
    public void refreshLore(Player player, Optional<PlayerProfile> profile) {
        Optional<Loadout> loadout = loadoutOf(profile);
        PlayerInventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getSize(); index++) {
            ItemStack item = inventory.getItem(index);
            if (StoneItems.isStone(item, keys)) StoneItems.refreshLore(item, keys, loadout, abilities);
        }
    }
}
