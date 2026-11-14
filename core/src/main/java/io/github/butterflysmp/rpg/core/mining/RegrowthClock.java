package io.github.butterflysmp.rpg.core.mining;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * When each player's broken patch at each slot regrows, in server ticks. The seat's ruling,
 * 2026-11-12: <b>in memory, keyed by player UUID, KEPT when the player logs out, LOST on
 * restart.</b> Nothing here removes an entry on logout. An entry goes when its deadline passes
 * (pruned when asked) or when its slot is removed.
 *
 * <p>NAME THE QUANTITY: {@code regrowth_ticks} counts SERVER ticks, so under 20 TPS a "minute" is
 * longer in real time. Ben: "1 minute is fine for now" = 1200 ticks. Not thread-safe: main thread
 * only.
 */
public final class RegrowthClock {

    private final LongSupplier now;
    private final Map<UUID, Map<String, Long>> deadlines = new HashMap<>();

    /** @param now the current server tick; the paper side passes {@code Bukkit::getCurrentTick} */
    public RegrowthClock(LongSupplier now) {
        this.now = now;
    }

    /** Starts regrowth for that player's patch at that slot. @return the deadline tick */
    public long start(UUID player, String slotId, int regrowthTicks) {
        long deadline = now.getAsLong() + regrowthTicks;
        deadlines.computeIfAbsent(player, k -> new HashMap<>()).put(slotId, deadline);
        return deadline;
    }

    /** True until the deadline tick; false from the deadline on. Prunes an expired entry. */
    public boolean isRegrowing(UUID player, String slotId) {
        Map<String, Long> slots = deadlines.get(player);
        if (slots == null) return false;
        Long deadline = slots.get(slotId);
        if (deadline == null) return false;
        if (now.getAsLong() < deadline) return true;
        slots.remove(slotId);
        if (slots.isEmpty()) deadlines.remove(player);
        return false;
    }

    /** The deadline, if that player's patch at that slot is regrowing now. */
    public java.util.OptionalLong deadline(UUID player, String slotId) {
        if (!isRegrowing(player, slotId)) return java.util.OptionalLong.empty();
        return java.util.OptionalLong.of(deadlines.get(player).get(slotId));
    }

    /** Ends one player's regrowth at one slot (S2: {@code /rpg mining roll} forces a fresh patch). */
    public void clear(UUID player, String slotId) {
        Map<String, Long> slots = deadlines.get(player);
        if (slots == null) return;
        slots.remove(slotId);
        if (slots.isEmpty()) deadlines.remove(player);
    }

    /** Ends every regrowth of one player (the dev command {@code /rpg mining regrow}). */
    public void clearPlayer(UUID player) {
        deadlines.remove(player);
    }

    /** Clears a removed slot for every player. */
    public void clearSlot(String slotId) {
        for (Iterator<Map<String, Long>> it = deadlines.values().iterator(); it.hasNext(); ) {
            Map<String, Long> slots = it.next();
            slots.remove(slotId);
            if (slots.isEmpty()) it.remove();
        }
    }

    /** How many (player, slot) deadlines are held. */
    public int size() {
        int n = 0;
        for (Map<String, Long> slots : deadlines.values()) n += slots.size();
        return n;
    }
}
