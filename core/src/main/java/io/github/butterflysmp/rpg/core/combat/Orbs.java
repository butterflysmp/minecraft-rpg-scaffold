package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * EVERY STANDING ORB, ONE PER PLAYER (Lapis Crystal's D2, 2026-11-23). ONE instance per server (paper holds it on
 * {@code AdapterContext}; a test world holds its own), because an orb outlives the cast that threw it by 300 ticks.
 * Pure state: nothing here touches a world. {@code CastExecutor.throwOrb} places and expires orbs; paper removes them on a
 * quit, a death, a world change and server stop, and sweeps an orphan body ({@link #ownsMarker}).
 *
 * <p><b>The generation</b> makes a stale task harmless: an arrival or expiry scheduled for an orb that a newer throw (or a
 * removal) has replaced finds a different generation and does nothing.
 *
 * <p><b>Threading:</b> a throw runs on its caster's thread, an expiry on the orb's region, a removal on the player's;
 * every method is {@code synchronized} and does a map operation.
 */
public final class Orbs {

    /**
     * One orb. {@code restPoint} is where it comes to rest; {@code arrivedTick} is -1 while it flies. {@code weaponId},
     * {@code enchantId} and {@code level} name the GRANT that threw it (null and 0 for an orb no grant threw), so D3 can
     * tell his own staff's beam and price the crystal's shots.
     */
    public record Orb(UUID owner, UUID markerId, long generation, Vec3 restPoint, double radius, int lifeTicks,
                      long arrivedTick, String weaponId, String enchantId, int level, Shot shot) {
        public boolean resting() {
            return arrivedTick >= 0;
        }

        Orb arrived(long tick) {
            return new Orb(owner, markerId, generation, restPoint, radius, lifeTicks, tick, weaponId, enchantId, level, shot);
        }
    }

    /**
     * D3: what the orb does when his own beam meets it (the orb cast's {@code shot:}), in this package's own terms
     * ({@code core.combat} imports nothing from {@code core.ability}). Null: it does nothing.
     */
    public record Shot(double radius, List<Double> percentByLevel, String element, String visual) {}

    private final Map<UUID, Orb> byOwner = new HashMap<>();
    private long nextGeneration = 1;

    /** A new orb for {@code owner}, flying; returns the one it REPLACES, if any (one per player). */
    public synchronized PlaceResult place(UUID owner, UUID markerId, Vec3 restPoint, double radius, int lifeTicks,
                                          String weaponId, String enchantId, int level, Shot shot) {
        Orb placed = new Orb(owner, markerId, nextGeneration++, restPoint, radius, lifeTicks, -1, weaponId, enchantId, level,
                shot);
        Orb previous = byOwner.put(owner, placed);
        return new PlaceResult(placed, Optional.ofNullable(previous));
    }

    public record PlaceResult(Orb placed, Optional<Orb> replaced) {}

    /** The orb of {@code generation} arrived at {@code tick}; false if it was replaced or removed meanwhile. */
    public synchronized Optional<Orb> arrive(UUID owner, long generation, long tick) {
        Orb orb = byOwner.get(owner);
        if (orb == null || orb.generation() != generation) return Optional.empty();
        Orb arrived = orb.arrived(tick);
        byOwner.put(owner, arrived);
        return Optional.of(arrived);
    }

    /** Remove {@code owner}'s orb only if it is still {@code generation} (an expiry). */
    public synchronized Optional<Orb> removeIf(UUID owner, long generation) {
        Orb orb = byOwner.get(owner);
        if (orb == null || orb.generation() != generation) return Optional.empty();
        byOwner.remove(owner);
        return Optional.of(orb);
    }

    /** Remove {@code owner}'s orb, whatever it is (his quit, death, world change). */
    public synchronized Optional<Orb> remove(UUID owner) {
        return Optional.ofNullable(byOwner.remove(owner));
    }

    /** Every orb, removed (server stop). */
    public synchronized List<Orb> removeAll() {
        List<Orb> all = new ArrayList<>(byOwner.values());
        byOwner.clear();
        return all;
    }

    /** {@code owner}'s orb, flying or resting. */
    public synchronized Optional<Orb> of(UUID owner) {
        return Optional.ofNullable(byOwner.get(owner));
    }

    /** Whether a live orb's body is {@code markerId}: a body none owns is an orphan, swept where it appears. */
    public synchronized boolean ownsMarker(UUID markerId) {
        for (Orb orb : byOwner.values()) if (orb.markerId().equals(markerId)) return true;
        return false;
    }

    public synchronized int size() {
        return byOwner.size();
    }
}
