package io.github.yourname.rpg.paper.listener;

import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.paper.profile.ProfileService;
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
    private final ProfileService profiles;

    public RpgListeners(CooldownTracker cooldowns, ProfileService profiles) {
        this.cooldowns = cooldowns;
        this.profiles = profiles;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
    }

    /**
     * Without the cooldown clear, every player who has ever cast anything keeps
     * a bucket until the server restarts. CooldownTracker is concurrent, so no
     * scheduler hop is needed to drop it.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.clear(playerId);
        profiles.onQuit(playerId);
    }
}
