package io.github.butterflysmp.rpg.core.enchant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * WILDFIRE'S CHAIN, PURE (the seat's brief of 2026-11-24, PR E), beside {@link ShockwaveStreaks}: who has a chain on what,
 * and what THIS hit's factor is. Nothing here touches a world; {@code BehaviourHits.chainFactor} asks it.
 *
 * <p><b>The rule (Ben: "Same Target, 3s rolling just like Kinetic Tremors, no", then "B"):</b> per player, enchant and
 * TARGET, a chain of hits. A hit EXTENDS the chain when it lands within {@code windowTicks} of the PREVIOUS hit on that
 * target ({@code now - last < windowTicks}: 59 extends, exactly 60 starts again, SEAT PLACEHOLDER 2026-11-24 for the
 * boundary, OPEN FOR BEN); otherwise the chain starts again AT THIS HIT. The n-th hit's factor is
 * {@code 1 + min(cap, step x (n - 1))}, so the first hit has no bonus. Nothing else resets it (Ben: "no").
 *
 * <p><b>Forgetting</b>, as the shockwave's streaks are forgotten: a target's chains on its death ({@link #forgetTarget}),
 * a player's on his quit ({@link #forgetPlayer}), and a chain whose last hit is a window old on that player's next hit.
 *
 * <p><b>Threading:</b> a hit lands on the thread that owns the struck mob, so every method is {@code synchronized}.
 */
public final class HitChains {

    private record Key(UUID player, String enchantId, UUID target) {}

    private static final class Chain {
        long last;
        int count;
        int windowTicks;
    }

    private final Map<Key, Chain> chains = new HashMap<>();

    /**
     * Record one HIT by {@code player} on {@code target} at tick {@code now} under {@code enchantId}'s chain at
     * {@code level}, and answer its factor.
     */
    public synchronized double hit(UUID player, String enchantId, UUID target, long now,
                                   EnchantBehaviour.HitChain chain, int level) {
        // THE WINDOW IS DECIDED ONCE, BY THE PRUNE: a chain whose last hit is a window old is gone before this hit is
        // counted, so a surviving chain is by construction one this hit extends, and a fresh one starts at 1. (It was
        // also tested here until MUTCHAINWIN stayed green: two sites holding one boundary mask each other's mutation.)
        prune(player, now);
        Chain c = chains.computeIfAbsent(new Key(player, enchantId, target), k -> new Chain());
        c.count = c.count + 1;
        c.last = now;
        c.windowTicks = chain.windowTicks();
        return chain.factorFor(level, c.count);
    }

    /** Drop every chain of {@code player} whose last hit is a window old. */
    private void prune(UUID player, long now) {
        Iterator<Map.Entry<Key, Chain>> it = chains.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key, Chain> e = it.next();
            if (e.getKey().player().equals(player) && now - e.getValue().last >= e.getValue().windowTicks) it.remove();
        }
    }

    /** The target died: forget every chain on it. */
    public synchronized void forgetTarget(UUID target) {
        chains.keySet().removeIf(k -> k.target().equals(target));
    }

    /** The player quit: forget every chain of his. */
    public synchronized void forgetPlayer(UUID player) {
        chains.keySet().removeIf(k -> k.player().equals(player));
    }

    /** How many chains {@code player} holds (a test and dev read: the forgets and the idle prune). */
    public synchronized int held(UUID player) {
        return (int) chains.keySet().stream().filter(k -> k.player().equals(player)).count();
    }
}
