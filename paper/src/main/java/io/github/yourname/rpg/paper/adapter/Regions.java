package io.github.yourname.rpg.paper.adapter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

/**
 * The one place that asks Paper whether this thread may touch that entity.
 *
 * Commit A of NEXT.md existed because three javadocs asserted threading properties that
 * turned out to be false. A comment is not enforcement. Every synchronous read of an
 * entity's state goes through here first, so a violation is a stack trace naming the line
 * rather than a race nobody can reproduce.
 *
 * What this buys, precisely: on Paper every region scheduler runs on the main thread, so
 * isOwnedByCurrentRegion is always true and this check is free -- and silent. It cannot
 * catch a snapshot captured on the wrong side of a same-thread hop. What it catches, and
 * the only thing that actually corrupts state, is a cross-region entity read on Folia.
 */
final class Regions {

    private Regions() {}

    static void requireOwned(Entity entity) {
        if (!Bukkit.isOwnedByCurrentRegion(entity)) {
            throw new IllegalStateException("Read of entity " + entity.getUniqueId()
                    + " from a thread that does not own its region. A snapshot must be taken"
                    + " on the thread that owns the entity.");
        }
    }
}
