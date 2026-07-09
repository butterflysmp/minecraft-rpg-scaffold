package io.github.yourname.rpg.paper.listener;

import io.github.yourname.rpg.core.combat.CooldownTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * The single Bukkit Listener. Registered once, in RpgPlugin.
 *
 * Resist adding a second one. Every listener that appears here should be a thin
 * adapter that hands the event to something in core; the game logic belongs there.
 */
public final class RpgListeners implements Listener {

    private final CooldownTracker cooldowns;

    public RpgListeners(CooldownTracker cooldowns) {
        this.cooldowns = cooldowns;
    }

    /**
     * Without this, every player who has ever cast anything keeps a cooldown
     * bucket until the server restarts. CooldownTracker is concurrent, so no
     * scheduler hop is needed to drop it.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.clear(event.getPlayer().getUniqueId());
    }
}
