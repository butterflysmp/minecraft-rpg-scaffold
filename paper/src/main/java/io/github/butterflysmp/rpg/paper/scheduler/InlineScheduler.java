package io.github.butterflysmp.rpg.paper.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

/**
 * THE SCHEDULER FOR A PLUGIN DISABLE, AND NOTHING ELSE. During {@code onDisable} the plugin already reads as disabled
 * ({@code JavaPlugin#setEnabled} clears {@code isEnabled} before it calls {@code onDisable}, read from the paper-api
 * jar), so {@link PaperScheduler}'s tasks are refused. The server is stopping on its main thread, and nothing else is
 * ticking: what must happen NOW runs now, here.
 *
 * <p><b>What was asked for LATER, or off-thread, cannot happen at all:</b> there is no later. Each such request is
 * handed to {@code refused} by name and its task never runs, so a restore that needed one says what it could not do
 * instead of losing it silently.
 *
 * <p>Never use this while the server is running: it ignores whose thread an entity or a region belongs to.
 */
public final class InlineScheduler implements Scheduler {

    private final Consumer<String> refused;

    public InlineScheduler(Consumer<String> refused) {
        this.refused = refused;
    }

    @Override public void onEntity(Entity entity, Runnable task) { task.run(); }
    @Override public void onRegion(Location location, Runnable task) { task.run(); }
    @Override public void onGlobal(Runnable task) { task.run(); }

    @Override public void onEntityLater(Entity entity, Runnable task, long delayTicks) { refused.accept("onEntityLater " + delayTicks); }
    @Override public void onRegionLater(Location location, Runnable task, long delayTicks) { refused.accept("onRegionLater " + delayTicks); }
    @Override public void async(Runnable task) { refused.accept("async"); }
}
