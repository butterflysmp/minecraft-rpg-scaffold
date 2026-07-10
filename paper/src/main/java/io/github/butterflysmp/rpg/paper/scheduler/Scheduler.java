package io.github.butterflysmp.rpg.paper.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * The ONLY way anything in this project schedules work.
 *
 * Why this exists:
 *  1. PacketEvents listeners run on Netty I/O threads. Anything touching the
 *     Bukkit API must be bounced back onto the owning thread. This is the bounce.
 *  2. Folia executes regions on separate threads. Code written against these
 *     methods migrates to Folia unchanged; code written against BukkitRunnable
 *     does not.
 *
 * DO NOT use BukkitRunnable, Bukkit.getScheduler().runTask, or plain
 * synchronized blocks anywhere in this codebase.
 */
public interface Scheduler {

    /** Run on the thread that owns this entity. Use for anything entity-scoped. */
    void onEntity(Entity entity, Runnable task);

    /** Run on the thread that owns this location's region. Use for block/world edits. */
    void onRegion(Location location, Runnable task);

    void onRegionLater(Location location, Runnable task, long delayTicks);

    /** Global state not tied to a region: scoreboards, plugin-wide counters. */
    void onGlobal(Runnable task);

    /** Off-thread work: database, HTTP. Never touch the Bukkit API from here. */
    void async(Runnable task);
}
