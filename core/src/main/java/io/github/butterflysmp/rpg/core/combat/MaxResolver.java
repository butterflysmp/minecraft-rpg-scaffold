package io.github.butterflysmp.rpg.core.combat;

import java.util.UUID;

/**
 * How full one owner's pool of one resource can get.
 *
 * <p>{@link ResourcePool} took a single {@code double} until Armor Slice 2b, which was correct for
 * exactly as long as nothing could raise a player's maximum. Mana Bank is that thing, so the ceiling
 * became a question with an owner in it.
 *
 * <h2>A function rather than a map inside the pool, and that is the whole design</h2>
 *
 * The per-player value lives on {@code HealthState} as a reconciled {@code Stat}, beside max health,
 * converged by the same leak-proof diff every other gear stat uses. Handing the pool a resolver
 * rather than a second map it owns keeps that in one place:
 *
 * <ul>
 *   <li>{@code ResourcePool.clear(owner)} still means "refill", because there is no second map for
 *       it to drop. That matters: {@code /rpg mana refill} IS {@code clear}, so a pool that owned
 *       the maxima would have had refill silently strip a player's Mana Bank.
 *   <li>{@code core} gains no dependency on the stat store. The lambda is built in {@code paper},
 *       where both halves are already in scope.
 *   <li>The pool stays thread-safe by having no new mutable state at all.
 * </ul>
 *
 * <h2>It must be TOTAL</h2>
 *
 * Called from {@code tryConsume} on whichever thread is casting, for any owner — including a mob
 * firing a costed trigger, and a player between bootstrap and register. {@code CombatantStats.max}
 * THROWS for an untracked id while the pool has no untracked concept at all, so an implementation
 * that forwards to the stat store must check {@code tracks} first and fall back to the base. A
 * resolver that throws would throw from inside a cast.
 *
 * <p>It is also asked per RESOURCE, not only per owner. {@code ResourcePool} is keyed by
 * {@code (owner, resourceId)} and promises "mana, and whatever else content asks for", so an
 * implementation that ignored the id would raise the ceiling on every future resource at once.
 */
@FunctionalInterface
public interface MaxResolver {

    /**
     * The ceiling for {@code owner}'s {@code resourceId} right now. Never throws; returns the base
     * for an owner or a resource it does not know about.
     */
    double maxFor(UUID owner, String resourceId);

    /** The pre-2b behaviour: one ceiling for everyone. Used by tests and as a base fallback. */
    static MaxResolver fixed(double max) {
        return (owner, resourceId) -> max;
    }
}
