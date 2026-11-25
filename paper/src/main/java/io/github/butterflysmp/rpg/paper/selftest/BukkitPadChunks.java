package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * The Bukkit half of {@link SelfTestPadTickets.Chunks} (P10): the plugin's chunk tickets on one world. The plugin handle
 * is {@code RpgPlugin}'s, passed in through {@code SelfTestService}. READ by javap on the pinned 26.1.2 server jar:
 * {@code World#addPluginChunkTicket} requires an enabled plugin and, when it adds the ticket, calls {@code getChunkAt}
 * (a load when the chunk is not loaded: hence {@link SelfTestPadTickets} only takes on a chunk {@link #isLoaded} just
 * reported loaded); {@code removePluginChunkTicket} returns false, and does nothing, when no ticket is held;
 * {@code getChunkAtAsync}'s future completes on the server's main thread.
 */
final class BukkitPadChunks implements SelfTestPadTickets.Chunks {

    private final World world;
    private final Plugin plugin;

    BukkitPadChunks(World world, Plugin plugin) {
        this.world = world;
        this.plugin = plugin;
    }

    @Override public boolean isLoaded(int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override public boolean take(int x, int z) {
        return world.addPluginChunkTicket(x, z, plugin);
    }

    @Override public boolean release(int x, int z) {
        return world.removePluginChunkTicket(x, z, plugin);
    }

    @Override public void request(int x, int z, Runnable loaded) {
        // A failed load runs the callback too: the chunk is then reported unloaded, and SelfTestPadTickets requests it
        // again or fails, by its own count.
        world.getChunkAtAsync(x, z, true).whenComplete((chunk, err) -> loaded.run());
    }
}
