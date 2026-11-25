package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * "A MOB THAT DAMAGED HIM" (D4; the seat's 4b, 2026-11-24), fed from the ONE place every damage to a player passes: our
 * store's health changes, with the dealer the store was given. A mob's melee (its id), a mob's projectile (READ:
 * {@code RpgListeners.attributableId} names the CAUSING entity, the shooter) and a mob's ability (its id) all arrive here.
 * Only a non-player dealer damaging a player, for more than nothing, counts. The guard asks {@link #damagedAt} and compares
 * it with its window; old entries are kept only for {@link #KEEP_TICKS}.
 */
public final class SummonFeeds implements HealthListener {

    /** How long an entry is kept at all: well beyond any damaged window (CC'S CALL; the window is the YAML's). */
    static final long KEEP_TICKS = 1200;

    private final LongSupplier now;
    private final Map<UUID, Map<UUID, Long>> byPlayer = new ConcurrentHashMap<>();

    public SummonFeeds(LongSupplier now) {
        this.now = now;
    }

    @Override
    public void onChange(HealthChange change) {
        if (change.kind() != HealthChange.Kind.DAMAGE || !change.targetIsPlayer() || change.dealerIsPlayer()) return;
        if (!(change.amount() > 0) || change.dealer() == null || change.dealer().equals(change.target())) return;
        long tick = now.getAsLong();
        Map<UUID, Long> his = byPlayer.computeIfAbsent(change.target(), k -> new ConcurrentHashMap<>());
        his.values().removeIf(at -> tick - at > KEEP_TICKS);
        his.put(change.dealer(), tick);
    }

    /** The tick this mob last damaged him, if it has within {@link #KEEP_TICKS}. */
    public OptionalLong damagedAt(UUID player, UUID mob) {
        Map<UUID, Long> his = byPlayer.get(player);
        Long at = his == null ? null : his.get(mob);
        return at == null ? OptionalLong.empty() : OptionalLong.of(at);
    }

    /** His quit: nothing of his is kept. */
    public void forget(UUID player) {
        byPlayer.remove(player);
    }
}
