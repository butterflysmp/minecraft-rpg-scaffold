package io.github.butterflysmp.rpg.core.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Cooldowns keyed by (caster, abilityId). Takes a tick supplier rather than
 * calling Bukkit.getCurrentTick() so it can be tested with a fake clock.
 *
 * Thread-safe. Under Folia two players in different regions cast on different
 * threads at the same instant, so this is written to be entered concurrently.
 * The outer map is shared; each inner map belongs to one player and in practice
 * is only ever written by the thread owning that player's region.
 *
 * Bounded: clear(caster) drops the player's bucket outright. Call it when the
 * player leaves, or the map grows for the lifetime of the server.
 */
public final class CooldownTracker {
    private final LongSupplier currentTick;
    private final Map<UUID, Map<String, Long>> readyAt = new ConcurrentHashMap<>();

    public CooldownTracker(LongSupplier currentTick) {
        this.currentTick = currentTick;
    }

    private Long readyTickOf(UUID caster, String abilityId) {
        Map<String, Long> forCaster = readyAt.get(caster);
        return forCaster == null ? null : forCaster.get(abilityId);
    }

    public boolean isReady(UUID caster, String abilityId) {
        Long ready = readyTickOf(caster, abilityId);
        return ready == null || currentTick.getAsLong() >= ready;
    }

    public long ticksRemaining(UUID caster, String abilityId) {
        Long ready = readyTickOf(caster, abilityId);
        if (ready == null) return 0;
        return Math.max(0, ready - currentTick.getAsLong());
    }

    public void trigger(UUID caster, String abilityId, int cooldownTicks) {
        readyAt.computeIfAbsent(caster, id -> new ConcurrentHashMap<>())
                .put(abilityId, currentTick.getAsLong() + cooldownTicks);
    }

    /** Drop every cooldown for this player. O(1). Safe for an unknown player. */
    public void clear(UUID caster) {
        readyAt.remove(caster);
    }

    /** Number of players holding cooldown state. Bounds check for tests. */
    public int trackedPlayers() {
        return readyAt.size();
    }
}
