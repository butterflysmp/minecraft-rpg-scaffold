package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.build.FragmentContributions;
import io.github.butterflysmp.rpg.core.build.FragmentDefinition;
import io.github.butterflysmp.rpg.core.build.FragmentRegistry;
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

import java.util.List;
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
    private final FragmentRegistry fragments;
    private final io.github.butterflysmp.rpg.core.build.AspectRegistry aspects;

    /** ONE derive for the server: memoised on (base, active aspects), shared by the cast and the lore. */
    private final io.github.butterflysmp.rpg.core.build.AspectApplication application =
            new io.github.butterflysmp.rpg.core.build.AspectApplication();

    public Stones(Keys keys, PoolRegistry pools, AbilityRegistry abilities, BuildService builds,
                  FragmentRegistry fragments, io.github.butterflysmp.rpg.core.build.AspectRegistry aspects) {
        this.keys = keys;
        this.pools = pools;
        this.abilities = abilities;
        this.builds = builds;
        this.fragments = fragments;
        this.aspects = aspects;
    }

    public PoolRegistry pools() { return pools; }

    public AbilityRegistry abilities() { return abilities; }

    public BuildService builds() { return builds; }

    public FragmentRegistry fragments() { return fragments; }

    public io.github.butterflysmp.rpg.core.build.AspectRegistry aspects() { return aspects; }

    /**
     * The CURRENT cell's two aspect slots, as equipped (slice 5; {@code LoadoutResolution.aspects}): a saved id
     * the pool still offers, once each; nulls for empty. Two empties for no pool, and for a build still loading
     * or unusable. An equipped aspect may still be INACTIVE -- see {@link #deriveFor}.
     */
    public List<String> equippedAspects(UUID playerId, Optional<PlayerProfile> profile) {
        Optional<PoolDefinition> pool = profile.flatMap(p -> pools.find(p.archetypeId(), p.elementId()));
        if (pool.isEmpty()) {
            return java.util.Collections.unmodifiableList(
                    java.util.Arrays.asList(new String[io.github.butterflysmp.rpg.core.build.AspectSlots.COUNT]));
        }
        PlayerProfile p = profile.get();
        List<String> saved = builds.build(playerId)
                .flatMap(build -> build.loadout(p.archetypeId(), p.elementId()))
                .map(CellLoadout::aspects)
                .orElse(null);
        return io.github.butterflysmp.rpg.core.build.LoadoutResolution.aspects(pool.get(), saved);
    }

    /**
     * THIS PLAYER'S DERIVE (slice 5): an ability as their ACTIVE aspects change it. An aspect is active when its
     * target is one of the three abilities the cell has equipped (amendment 2); an inactive one contributes
     * nothing. The stone's cast, a non-op's /rpg cast and the Build screen's lore all call this, so the tooltip
     * and the cast cannot disagree. An operator's dev cast does NOT (ruling 10: the base ability). Never the
     * registry: another player casting the same ability gets the base.
     */
    public java.util.function.UnaryOperator<io.github.butterflysmp.rpg.core.ability.AbilityDefinition> deriveFor(
            UUID playerId, Optional<PlayerProfile> profile) {
        java.util.Set<String> equipped = equippedFor(playerId, profile)
                .map(e -> java.util.Set.copyOf(e.ids())).orElse(java.util.Set.of());
        List<io.github.butterflysmp.rpg.core.build.AspectDefinition> slotted = equippedAspects(playerId, profile).stream()
                .map(id -> id == null ? null : aspects.find(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (slotted.isEmpty()) return java.util.function.UnaryOperator.identity();
        return def -> application.derive(def,
                io.github.butterflysmp.rpg.core.build.AspectApplication.activeFor(def.id(), slotted, equipped));
    }

    /**
     * The CURRENT cell's four fragment slots, as equipped (slice 4; {@code LoadoutResolution.fragments}):
     * a saved id its pool still offers, once each; nulls for empty. Four empties for no pool, and for a
     * build still loading or unusable -- a fragment is a bonus, so the safe reading is "none".
     *
     * <p><b>THE CELL GATE (section 1.6):</b> only the current cell's fragments count. Another cell's
     * saved fragments stay in the build file and contribute nothing until the player switches back.
     */
    public List<String> equippedFragments(UUID playerId, Optional<PlayerProfile> profile) {
        Optional<PoolDefinition> pool = profile.flatMap(p -> pools.find(p.archetypeId(), p.elementId()));
        if (pool.isEmpty()) return java.util.Collections.unmodifiableList(java.util.Arrays.asList(new String[io.github.butterflysmp.rpg.core.build.FragmentSlots.COUNT]));
        PlayerProfile p = profile.get();
        List<String> saved = builds.build(playerId)
                .flatMap(build -> build.loadout(p.archetypeId(), p.elementId()))
                .map(CellLoadout::fragments)
                .orElse(null);
        return LoadoutResolution.fragments(pool.get(), saved);
    }

    /** What the current cell's fragments add to the stats, for the reconcile loop. {@code NONE} if none. */
    public FragmentContributions fragmentContributions(UUID playerId, Optional<PlayerProfile> profile) {
        List<String> ids = equippedFragments(playerId, profile);
        List<FragmentDefinition> slots = ids.stream()
                .map(id -> id == null ? null : fragments.find(id).orElse(null))
                .toList();
        return FragmentContributions.of(slots);
    }

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
