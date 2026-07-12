package io.github.butterflysmp.rpg.paper.adapter;

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
 */
public record AdapterContext(Scheduler scheduler, Keys keys,
                             VisualRegistry visuals, StatusRegistry statuses,
                             Logger log, Set<String> warned,
                             ImmobilizeStatus immobilize, SoakedStatus soaked,
                             ImmobilizeStatus freeze) {

    public AdapterContext(Scheduler scheduler, Keys keys, VisualRegistry visuals,
                          StatusRegistry statuses, Logger log) {
        this(scheduler, keys, visuals, statuses, log, ConcurrentHashMap.newKeySet(),
                new ImmobilizeStatus(), new SoakedStatus(), new ImmobilizeStatus());
    }

    /**
     * Report a content mistake once, however many times it is hit. Areas pulse from
     * region threads and hit every combatant in radius, so the set is concurrent.
     */
    public void warnOnce(String message) {
        if (warned.add(message)) log.warning(message);
    }
}
