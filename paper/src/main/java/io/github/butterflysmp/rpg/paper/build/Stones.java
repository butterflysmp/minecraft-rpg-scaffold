package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;
import io.github.butterflysmp.rpg.core.build.LockedSlots;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.nexus.LockedItem;
import io.github.butterflysmp.rpg.storage.CellLoadout;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;
import java.util.UUID;

/**
 * What the Ability Stone needs that is fixed at boot: its key, the pools, the ability names, and the build
 * store. Carried on {@code AdapterContext} because the menus that toggle and move the stone (Settings, the
 * picker) already receive one, and threading a new argument through every {@code NexusMenu} construction to
 * reach them is the worse trade.
 *
 * <p>No player state lives here -- the profile is always passed in, and the build lives in
 * {@link BuildService} (CLAUDE.md invariant 3).
 */
public final class Stones {

    private final Keys keys;
    private final PoolRegistry pools;
    private final AbilityRegistry abilities;
    private final BuildService builds;

    public Stones(Keys keys, PoolRegistry pools, AbilityRegistry abilities, BuildService builds) {
        this.keys = keys;
        this.pools = pools;
        this.abilities = abilities;
        this.builds = builds;
    }

    public PoolRegistry pools() { return pools; }

    public AbilityRegistry abilities() { return abilities; }

    public BuildService builds() { return builds; }

    /**
     * What this player's cell has equipped: their SAVED loadout for the cell, or the pool's default when
     * they saved none (slice 2; {@code LoadoutResolution} holds the rules). Empty for a class or element of
     * {@code none}, a null class (PLAN-accessories 8.4: a null class survives load), or a cell with no pool.
     *
     * <p><b>An unusable or still-loading build reads as "nothing saved"</b>, so the stone casts the default
     * rather than nothing. A still-loading build is a join-time window measured in milliseconds; an
     * unusable one was logged SEVERE when it failed, and {@link #storeUnavailable} lets the lore say so.
     */
    public Optional<Equipped> equippedFor(UUID playerId, Optional<PlayerProfile> profile) {
        Optional<PoolDefinition> pool = profile.flatMap(p -> pools.find(p.archetypeId(), p.elementId()));
        if (pool.isEmpty()) return Optional.empty();
        PlayerProfile p = profile.get();
        Optional<LoadoutResolution.Saved> saved = builds.build(playerId)
                .flatMap(build -> build.loadout(p.archetypeId(), p.elementId()))
                .map(Stones::saved);
        return Optional.of(LoadoutResolution.resolve(pool.get(), saved));
    }

    /** The build store failed for this player this session: the stone is casting the pool default. */
    public boolean storeUnavailable(UUID playerId) {
        return builds.unusable(playerId);
    }

    private static LoadoutResolution.Saved saved(CellLoadout cell) {
        return new LoadoutResolution.Saved(cell.ultimate(), cell.actives().get(0), cell.actives().get(1));
    }

    /** The stone as a locked item: hotbar only (ruling 2), default slot 7 (ruling 13). */
    public LockedItem lockedItem(UUID playerId, Optional<PlayerProfile> profile) {
        return new LockedItem("the Ability Stone", item -> StoneItems.isStone(item, keys),
                () -> StoneItems.mint(keys, equippedFor(playerId, profile), storeUnavailable(playerId), abilities),
                LockedSlots.DEFAULT_STONE_SLOT, LockedSlots.STONE_MAX_SLOT);
    }

    /**
     * Re-render the lore of every stone the player carries, for a loadout that changed under it (a class
     * or element change, a saved slot, a build that finished loading). Convergence keeps an existing stone
     * rather than re-minting it, so without this a stone would keep naming abilities it no longer casts.
     */
    public void refreshLore(Player player, Optional<PlayerProfile> profile) {
        Optional<Equipped> equipped = equippedFor(player.getUniqueId(), profile);
        boolean unavailable = storeUnavailable(player.getUniqueId());
        PlayerInventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getSize(); index++) {
            ItemStack item = inventory.getItem(index);
            if (StoneItems.isStone(item, keys)) StoneItems.refreshLore(item, keys, equipped, unavailable, abilities);
        }
    }
}
