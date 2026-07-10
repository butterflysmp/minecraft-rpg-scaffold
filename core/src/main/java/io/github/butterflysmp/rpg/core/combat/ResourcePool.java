package io.github.butterflysmp.rpg.core.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Ability resources -- energy, and whatever else content asks for -- keyed by
 * (owner, resourceId). Shaped like CooldownTracker on purpose: a tick supplier
 * rather than Bukkit, so it can be tested with a fake clock.
 *
 * Regeneration is LAZY. Nothing ticks; the current value is computed on read
 * from the amount last written and how many ticks have passed since. That means
 * no repeating task, no per-player scheduler entry, and no drift -- a pool that
 * nobody reads costs nothing.
 *
 * Thread-safe, for the same reason CooldownTracker is: under Folia two players
 * in different regions cast on different threads at the same instant. Consumption
 * is a single atomic compute, so two concurrent casts cannot both spend the last
 * 40 energy.
 *
 * Bounded: clear(owner) drops the owner's pools. Call it when a player leaves.
 */
public final class ResourcePool {

    /** Amount as of a tick. Everything between then and now is regeneration. */
    private record Entry(double amount, long asOfTick) {}

    private final LongSupplier currentTick;
    private final double max;
    private final double regenPerTick;
    private final Map<UUID, Map<String, Entry>> pools = new ConcurrentHashMap<>();

    public ResourcePool(LongSupplier currentTick, double max, double regenPerTick) {
        if (max <= 0) throw new IllegalArgumentException("max must be positive");
        if (regenPerTick < 0) throw new IllegalArgumentException("regenPerTick must not be negative");
        this.currentTick = currentTick;
        this.max = max;
        this.regenPerTick = regenPerTick;
    }

    public double max() {
        return max;
    }

    /** An owner nobody has charged anything to is full. */
    public double current(UUID owner, String resourceId) {
        Map<String, Entry> owned = pools.get(owner);
        Entry entry = owned == null ? null : owned.get(resourceId);
        return entry == null ? max : regenerated(entry);
    }

    private double regenerated(Entry entry) {
        long elapsed = Math.max(0, currentTick.getAsLong() - entry.asOfTick());
        return Math.min(max, entry.amount() + elapsed * regenPerTick);
    }

    /**
     * Spend {@code amount} if it is available. All-or-nothing: on failure not a
     * drop is taken, so a caller that reports "not enough energy" has not
     * quietly drained the player.
     *
     * @return true if the full amount was consumed
     */
    public boolean tryConsume(UUID owner, String resourceId, double amount) {
        if (amount <= 0) return true;            // a free ability always casts
        if (amount > max) return false;          // never satisfiable; do not wait forever

        Map<String, Entry> owned = pools.computeIfAbsent(owner, id -> new ConcurrentHashMap<>());

        // compute() applies the function atomically for this key, so the
        // read-modify-write below cannot interleave with another caster. A plain
        // get/put here lets 4-6 of 16 concurrent 40-energy casts through a pool
        // that fits 2 -- verified, not theoretical.
        boolean[] consumed = {false};
        owned.compute(resourceId, (id, entry) -> {
            double available = entry == null ? max : regenerated(entry);
            if (available < amount) {
                return entry; // untouched; null stays absent, which reads as full
            }
            consumed[0] = true;
            return new Entry(available - amount, currentTick.getAsLong());
        });
        return consumed[0];
    }

    /** Drop every pool for this owner. O(1). Safe for an unknown owner. */
    public void clear(UUID owner) {
        pools.remove(owner);
    }

    /** Number of owners holding resource state. Bounds check for tests. */
    public int trackedOwners() {
        return pools.size();
    }
}
