package io.github.butterflysmp.rpg.core.mining;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The miss sound's cooldown (Mining S2, the seat's ruling 9, on Ben's "Miss sound should have a
 * slight cooldown, let's say .4 seconds"): per player, a miss sound plays only if at least
 * {@link MiningNotices#MISS_SOUND_COOLDOWN_TICKS} server ticks have passed since the last miss sound
 * that actually PLAYED. A silent miss does not restart the count.
 *
 * <p>The outcome is still a MISS either way; this decides only the sound. In memory, main thread
 * only (the BlockDamageEvent handler).
 */
public final class MissSoundCooldown {

    private final Map<UUID, Long> lastPlayed = new HashMap<>();

    /** @return true if the miss sound should play now, in which case this records {@code now} */
    public boolean tryPlay(UUID player, long now) {
        Long last = lastPlayed.get(player);
        if (last != null && now - last < MiningNotices.MISS_SOUND_COOLDOWN_TICKS) return false;
        lastPlayed.put(player, now);
        return true;
    }

    /** Forgets a player (on quit). */
    public void forget(UUID player) {
        lastPlayed.remove(player);
    }
}
