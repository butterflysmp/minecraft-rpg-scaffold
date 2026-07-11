package io.github.butterflysmp.rpg.paper.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Backed by Paper's region/entity schedulers, which Paper provides precisely
 * so plugins can be Folia-ready. When you eventually run Folia, this class
 * should require zero changes.
 */
public final class PaperScheduler implements Scheduler {
    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) { this.plugin = plugin; }

    @Override
    public void onEntity(Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, t -> task.run(), null);
    }

    @Override
    public void onEntityLater(Entity entity, Runnable task, long delayTicks) {
        // The entity scheduler rejects a delay of 0; clamp to 1 tick, as onRegionLater does.
        // The `retired` runnable is null: if the entity is gone before the delay, do nothing.
        entity.getScheduler().runDelayed(plugin, t -> task.run(), null, Math.max(1L, delayTicks));
    }

    @Override
    public void onRegion(Location location, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void onRegionLater(Location location, Runnable task, long delayTicks) {
        // Region scheduler rejects a delay of 0; clamp to 1 tick.
        long delay = Math.max(1L, delayTicks);
        Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> task.run(), delay);
    }

    @Override
    public void onGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void async(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }
}
