package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;
import io.github.butterflysmp.rpg.paper.content.VisualRegistry;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Everything the Bukkit adapters need in order to do their job, built once in
 * onEnable and shared by every adapter instance.
 *
 * Shared is the operative word. PaperCombatWorld.combatantsNear builds a fresh
 * BukkitCombatant per entity per call, and RpgCommand builds a fresh
 * PaperCombatWorld per cast. A warn-once set living on either would be reborn
 * empty on every area pulse, which turns warn-once into warn-always -- the exact
 * log spam it exists to prevent. It has to live here.
 *
 * The content registries ride along for the same reason they always have: the things
 * that need them are reached through {@code adapters}, not through the plugin. {@code elements}
 * joined visuals/statuses so {@link io.github.butterflysmp.rpg.paper.weapon.WeaponItems#mint}
 * can colour a weapon's element line from that element's own content, rather than threading
 * an ElementRegistry through five RpgCommand signatures to reach two mint call sites.
 *
 * <p>{@code enchants} rides along for exactly the same reason: EnchantLore needs an enchant's
 * display name and max_level to render "Unbreaking III", and the only things that render lore are
 * WeaponItems.mint and remint, both of which already take an AdapterContext.
 *
 * <p>{@code craftResults} is the third instance of that argument. The only thing that asks "should
 * this crafted item become one of ours" is {@code CraftingMenu}, which is constructed with nothing
 * but a player and this context -- so threading the three gear registries through the hijack table
 * to reach one lookup would be exactly the five-signature detour {@code elements} was admitted to
 * avoid. The index is built once at boot and is immutable afterwards, like every other registry here.
 */
public record AdapterContext(Scheduler scheduler, Keys keys,
                             VisualRegistry visuals, StatusRegistry statuses,
                             ElementRegistry elements, EnchantRegistry enchants,
                             Logger log, Set<String> warned,
                             ImmobilizeStatus immobilize, SoakedStatus soaked,
                             ImmobilizeStatus freeze, CombatantStats stats, double anchorDrift,
                             CraftResultIndex craftResults) {

    public AdapterContext(Scheduler scheduler, Keys keys, VisualRegistry visuals,
                          StatusRegistry statuses, ElementRegistry elements,
                          EnchantRegistry enchants, Logger log,
                          CombatantStats stats, double anchorDrift,
                          CraftResultIndex craftResults) {
        this(scheduler, keys, visuals, statuses, elements, enchants, log, ConcurrentHashMap.newKeySet(),
                new ImmobilizeStatus(), new SoakedStatus(), new ImmobilizeStatus(), stats, anchorDrift,
                craftResults);
    }

    /**
     * Report a content mistake once, however many times it is hit. Areas pulse from
     * region threads and hit every combatant in radius, so the set is concurrent.
     */
    public void warnOnce(String message) {
        if (warned.add(message)) log.warning(message);
    }
}
