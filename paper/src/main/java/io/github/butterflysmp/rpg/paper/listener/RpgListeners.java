package io.github.butterflysmp.rpg.paper.listener;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * The single Bukkit Listener. Registered once, in RpgPlugin.
 *
 * Resist adding a second one. Every handler here should be a thin adapter that
 * hands the event to something that does the actual work; the logic belongs there.
 */
public final class RpgListeners implements Listener {

    private final CooldownTracker cooldowns;
    private final ResourcePool resources;
    private final ProfileService profiles;

    public RpgListeners(CooldownTracker cooldowns, ResourcePool resources, ProfileService profiles) {
        this.cooldowns = cooldowns;
        this.resources = resources;
        this.profiles = profiles;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
    }

    /**
     * Without these clears, every player who has ever cast anything keeps a
     * cooldown and resource bucket until the server restarts. Both structures
     * are concurrent, so no scheduler hop is needed to drop them.
     *
     * Dropping the energy pool is also correct game behaviour, not just hygiene:
     * an absent pool reads as full, so a returning player starts charged.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.clear(playerId);
        resources.clear(playerId);
        profiles.onQuit(playerId);
    }
}
