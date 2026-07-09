package io.github.yourname.rpg.core.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Cooldowns keyed by (caster, abilityId). Takes a tick supplier rather than
 * calling Bukkit.getCurrentTick() so it can be tested with a fake clock.
 */
public final class CooldownTracker {
    private final LongSupplier currentTick;
    private final Map<String, Long> readyAt = new HashMap<>();

    public CooldownTracker(LongSupplier currentTick) {
        this.currentTick = currentTick;
    }

    private static String key(UUID caster, String abilityId) {
        return caster + ":" + abilityId;
    }

    public boolean isReady(UUID caster, String abilityId) {
        Long ready = readyAt.get(key(caster, abilityId));
        return ready == null || currentTick.getAsLong() >= ready;
    }

    public long ticksRemaining(UUID caster, String abilityId) {
        Long ready = readyAt.get(key(caster, abilityId));
        if (ready == null) return 0;
        return Math.max(0, ready - currentTick.getAsLong());
    }

    public void trigger(UUID caster, String abilityId, int cooldownTicks) {
        readyAt.put(key(caster, abilityId), currentTick.getAsLong() + cooldownTicks);
    }

    public void clear(UUID caster) {
        readyAt.keySet().removeIf(k -> k.startsWith(caster + ":"));
    }
}
