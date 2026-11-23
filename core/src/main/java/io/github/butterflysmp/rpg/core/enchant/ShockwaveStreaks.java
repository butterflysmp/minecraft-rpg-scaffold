package io.github.butterflysmp.rpg.core.enchant;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * THE SHOCKWAVE'S DECISION, PURE (the seat's B3, 2026-11-23): who has hit what, when, and whether THIS hit fires a
 * shockwave. Nothing here touches a world; {@code BehaviourHits} executes what this decides.
 *
 * <p><b>The rule, as the seat read it to Ben (not objected to):</b> per player and target, keep the times of his hits.
 * When a hit makes it {@code hits} or more on that target within the last {@code windowTicks}, AND his shockwave cooldown
 * ({@code cooldownTicks}, per PLAYER, not per target) is clear, the hit fires one and the cooldown starts. The count is
 * NOT cleared: the next hit fires again if the window and the cooldown allow. Four pellets in one tick fire exactly one
 * (the third fires, the fourth meets the cooldown).
 *
 * <p><b>"Within the last {@code windowTicks}" is read as {@code now - t < windowTicks}</b>: a hit 59 ticks before still
 * counts, one 60 ticks before does not (SEAT PLACEHOLDER 2026-11-23 for the boundary tick; Ben's "within 3 seconds").
 * The cooldown is clear when {@code now - lastFired >= cooldownTicks}.
 *
 * <p><b>Forgetting:</b> a target's record goes when it dies ({@link #forgetTarget}), when its newest hit is a window old
 * (pruned on every hit), and a player's whole record on his quit ({@link #forgetPlayer}). State is keyed by the ENCHANT's
 * id as well, so two shockwave enchants would keep separate counts and separate cooldowns.
 *
 * <p><b>Threading:</b> hits land on whichever thread owns the struck mob (a Folia region thread later), so every method is
 * {@code synchronized}; the work inside is a few map operations.
 */
public final class ShockwaveStreaks {

    private record Key(UUID player, String enchantId) {}

    private static final class Streak {
        long lastFired = Long.MIN_VALUE / 2;
        final Map<UUID, Deque<Long>> hitsByTarget = new HashMap<>();
    }

    private final Map<Key, Streak> streaks = new HashMap<>();

    /**
     * Record one HIT by {@code player} on {@code target} at tick {@code now}, under {@code enchantId}'s shockwave, and say
     * whether it fires one.
     */
    public synchronized boolean hit(UUID player, String enchantId, UUID target, long now,
                                    EnchantBehaviour.Shockwave shockwave) {
        Streak streak = streaks.computeIfAbsent(new Key(player, enchantId), k -> new Streak());
        prune(streak, now, shockwave.windowTicks());
        Deque<Long> times = streak.hitsByTarget.computeIfAbsent(target, t -> new ArrayDeque<>());
        times.addLast(now);
        boolean enough = times.size() >= shockwave.hits();
        boolean cooled = now - streak.lastFired >= shockwave.cooldownTicks();
        if (enough && cooled) {
            streak.lastFired = now;
            return true;
        }
        return false;
    }

    /** Drop every hit older than the window, and every target left with none. */
    private static void prune(Streak streak, long now, int windowTicks) {
        Iterator<Map.Entry<UUID, Deque<Long>>> targets = streak.hitsByTarget.entrySet().iterator();
        while (targets.hasNext()) {
            Deque<Long> times = targets.next().getValue();
            while (!times.isEmpty() && now - times.peekFirst() >= windowTicks) times.removeFirst();
            if (times.isEmpty()) targets.remove();
        }
    }

    /** The target died: forget every player's hits on it. */
    public synchronized void forgetTarget(UUID target) {
        for (Streak streak : streaks.values()) streak.hitsByTarget.remove(target);
    }

    /** The player quit: forget everything he had. */
    public synchronized void forgetPlayer(UUID player) {
        streaks.keySet().removeIf(key -> key.player().equals(player));
    }

    /** How many hits {@code player} holds on {@code target} under {@code enchantId} (a test and dev read). */
    public synchronized int held(UUID player, String enchantId, UUID target) {
        Streak streak = streaks.get(new Key(player, enchantId));
        if (streak == null) return 0;
        Deque<Long> times = streak.hitsByTarget.get(target);
        return times == null ? 0 : times.size();
    }

    /** How many targets {@code player} holds any hit on (a test read: the idle prune and the forgets). */
    public synchronized int targets(UUID player, String enchantId) {
        Streak streak = streaks.get(new Key(player, enchantId));
        return streak == null ? 0 : streak.hitsByTarget.size();
    }
}
