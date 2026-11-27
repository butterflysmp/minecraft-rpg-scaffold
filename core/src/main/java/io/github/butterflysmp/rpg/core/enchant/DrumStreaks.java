package io.github.butterflysmp.rpg.core.enchant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DRUMMING BEAT'S STREAKS, ONE PER PLAYER (the seat's M4). Held by the server's one {@code BehaviourHits}, beside the
 * shockwave streaks and the hit chains. {@link #forget} is every reset that is not a hit: a slot change, a swap, a drop,
 * an inventory click, his quit, death and world change, the selftest's clear_state and row-start CLEAN.
 */
public final class DrumStreaks {

    private final Map<UUID, DrumBeats.Streak> streaks = new HashMap<>();

    public synchronized DrumBeats.Streak of(UUID player) {
        return streaks.getOrDefault(player, DrumBeats.Streak.NONE);
    }

    /** Keep {@code streak} for {@code player}; {@code NONE} forgets him. */
    public synchronized void put(UUID player, DrumBeats.Streak streak) {
        if (streak == null || streak.n() == 0) streaks.remove(player);
        else streaks.put(player, streak);
    }

    public synchronized void forget(UUID player) {
        streaks.remove(player);
    }

    /** How many players hold a streak (the selftest's CLEAN reads it). */
    public synchronized int held() {
        return streaks.size();
    }
}
