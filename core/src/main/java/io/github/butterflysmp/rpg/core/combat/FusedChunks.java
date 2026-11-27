package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * EVERY LIVE FUSED CHUNK (the seat's F2, 2026-11-26, the Blast Fungus), flying or at rest, ONE store per server (paper
 * holds it on {@code AdapterContext}; a test world holds its own), because a chunk outlives the cast that threw it by its
 * fuse. B6's guarantees, as {@link Orbs} gives them: the body is never saved, and paper removes a player's chunks on his
 * quit, death and world change, every chunk at server stop, his chunks at the selftest's clear_state and row-start CLEAN,
 * and sweeps a tagged body no live chunk owns ({@link #ownsMarker}).
 *
 * <p><b>EXACTLY ONCE.</b> A chunk goes off only by being {@link #take taken} out of this store: the flight takes it
 * before it detonates, and a removal takes it before it discards the body. Whichever takes it first is the only one that
 * acts; the other finds it gone and does nothing. So a chunk removed on a quit never goes off, and a chunk that has gone
 * off is never removed twice.
 *
 * <p><b>Threading:</b> a throw and each step run on the chunk's region, a removal on the player's thread or the server
 * thread; every method is {@code synchronized} and does a map operation.
 */
public final class FusedChunks {

    /**
     * One chunk. {@code id} is this store's own number (never reused); {@code markerId} its body, or null for a chunk
     * that renders none; {@code at} where the flight last put it; {@code resting} once it has met a block.
     */
    public record Chunk(long id, UUID owner, UUID markerId, Vec3 at, boolean resting) {}

    private final Map<Long, Chunk> live = new LinkedHashMap<>();
    private long nextId = 1;

    /** A new chunk of {@code owner}'s, flying from {@code at}; its id. */
    public synchronized long add(UUID owner, UUID markerId, Vec3 at) {
        long id = nextId++;
        live.put(id, new Chunk(id, owner, markerId, at, false));
        return id;
    }

    /** Whether chunk {@code id} is still live (not gone off, not removed). */
    public synchronized boolean live(long id) {
        return live.containsKey(id);
    }

    /** Record where the flight put chunk {@code id}; false (nothing recorded) when it is no longer live. */
    public synchronized boolean moved(long id, Vec3 at, boolean resting) {
        Chunk chunk = live.get(id);
        if (chunk == null) return false;
        live.put(id, new Chunk(id, chunk.owner(), chunk.markerId(), at, resting));
        return true;
    }

    /** Take chunk {@code id} out: present exactly once, to whichever caller takes it first. */
    public synchronized Optional<Chunk> take(long id) {
        return Optional.ofNullable(live.remove(id));
    }

    /** Take every chunk of {@code owner}'s out (his quit, death, world change; the selftest's clear). */
    public synchronized List<Chunk> takeAllOf(UUID owner) {
        List<Chunk> taken = new ArrayList<>();
        live.values().removeIf(c -> {
            if (!c.owner().equals(owner)) return false;
            taken.add(c);
            return true;
        });
        return taken;
    }

    /** Take every chunk out (server stop). */
    public synchronized List<Chunk> takeAll() {
        List<Chunk> all = new ArrayList<>(live.values());
        live.clear();
        return all;
    }

    /** Chunk {@code id}, if live. */
    public synchronized Optional<Chunk> of(long id) {
        return Optional.ofNullable(live.get(id));
    }

    /** {@code owner}'s live chunks, in the order they were thrown. */
    public synchronized List<Chunk> ofOwner(UUID owner) {
        return live.values().stream().filter(c -> c.owner().equals(owner)).toList();
    }

    /** Whether a live chunk's body is {@code markerId}: a tagged body none owns is an orphan, swept where it appears. */
    public synchronized boolean ownsMarker(UUID markerId) {
        for (Chunk chunk : live.values()) if (markerId.equals(chunk.markerId())) return true;
        return false;
    }

    public synchronized int size() {
        return live.size();
    }
}
