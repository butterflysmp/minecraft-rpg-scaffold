package io.github.butterflysmp.rpg.core.mining;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Which (player, slot) pairs have a regrowth task pending, so there is at most ONE (the seat's D1,
 * 2026-11-13). The task itself lives on the player's entity scheduler in paper; this is only its
 * marker.
 *
 * <p>Each claim gets a fresh TOKEN, never reused. The task carries its token and acts only while it
 * is still the current one, so a task from an earlier session (the player quit, the slot was
 * removed) does nothing if it ever runs. The marker goes when the task shows the patch
 * ({@link #release}), on quit ({@link #clearPlayer}) and on slot removal ({@link #clearSlot}).
 * Not thread-safe: main thread only.
 */
public final class RegrowthTasks {

    private long nextToken = 1;
    private final Map<UUID, Map<String, Long>> pending = new HashMap<>();

    /** Records a pending task if there is none. @return its token, or empty if one is already pending */
    public OptionalLong claim(UUID player, String slotId) {
        Map<String, Long> mine = pending.computeIfAbsent(player, k -> new HashMap<>());
        if (mine.containsKey(slotId)) return OptionalLong.empty();
        long token = nextToken++;
        mine.put(slotId, token);
        return OptionalLong.of(token);
    }

    /** True while {@code token} is the pending task's token for this player and slot. */
    public boolean isCurrent(UUID player, String slotId, long token) {
        Map<String, Long> mine = pending.get(player);
        Long held = mine == null ? null : mine.get(slotId);
        return held != null && held == token;
    }

    /** Frees the marker if {@code token} is current. @return true if it was */
    public boolean release(UUID player, String slotId, long token) {
        if (!isCurrent(player, slotId, token)) return false;
        Map<String, Long> mine = pending.get(player);
        mine.remove(slotId);
        if (mine.isEmpty()) pending.remove(player);
        return true;
    }

    /** On quit: the player's tasks are gone with their session, so their markers go too. */
    public void clearPlayer(UUID player) {
        pending.remove(player);
    }

    /** On slot removal, for every player. */
    public void clearSlot(String slotId) {
        for (Iterator<Map<String, Long>> it = pending.values().iterator(); it.hasNext(); ) {
            Map<String, Long> mine = it.next();
            mine.remove(slotId);
            if (mine.isEmpty()) it.remove();
        }
    }

    /** How many markers are held. */
    public int size() {
        int n = 0;
        for (Map<String, Long> mine : pending.values()) n += mine.size();
        return n;
    }
}
