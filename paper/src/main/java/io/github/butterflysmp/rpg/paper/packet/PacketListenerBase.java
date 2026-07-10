package io.github.butterflysmp.rpg.paper.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.Player;

/**
 * ============================ READ THIS ============================
 * PacketEvents callbacks run on NETTY I/O THREADS, not the main server
 * thread and not a Folia region thread.
 *
 * Calling the Bukkit API from onPacketReceive / onPacketSend is a data
 * race. It will appear to work with one player on a test server and
 * corrupt state under real load. This is the single most common way to
 * destroy a packet-based plugin.
 *
 * Rules, no exceptions:
 *   - Inside a packet callback: read the packet, do pure computation,
 *     cancel it if you must. Touch NOTHING else.
 *   - To reach the game world, call bukkit(player, () -> ...).
 *   - Never hold a lock across the boundary.
 * ==================================================================
 */
public abstract class PacketListenerBase extends PacketListenerAbstract {

    private final Scheduler scheduler;

    protected PacketListenerBase(Scheduler scheduler, PacketListenerPriority priority) {
        super(priority);
        this.scheduler = scheduler;
    }

    /**
     * Hop from the Netty thread onto the thread that owns this player.
     * The ONLY sanctioned way to touch the Bukkit API from a packet callback.
     */
    protected final void bukkit(Player player, Runnable task) {
        if (player == null || !player.isOnline()) return;
        scheduler.onEntity(player, task);
    }
}
