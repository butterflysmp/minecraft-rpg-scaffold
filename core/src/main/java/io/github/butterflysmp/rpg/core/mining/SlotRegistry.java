package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The placed slots, IN MEMORY (the seat's ruling 4, 2026-11-13: no dev file; lost on restart, and
 * the gate re-places its slot each boot).
 *
 * <p>It is NOT the area model: it knows nothing of regions, of a no-break rule, or of which areas
 * get patches. It answers two questions: which slot, if any, covers this block (for the
 * BlockDamageEvent handler), and which slots touch this chunk (for the chunk-load render).
 * Not thread-safe: main thread only.
 */
public final class SlotRegistry {

    private final Map<String, Slot> byId = new LinkedHashMap<>();
    private final Map<String, Slot> byBlock = new HashMap<>();
    private final Map<String, Set<Slot>> byChunk = new HashMap<>();

    private static String blockKey(String world, BlockPos b) {
        return world + "|" + b.x() + "|" + b.y() + "|" + b.z();
    }

    private static String chunkKey(String world, int cx, int cz) {
        return world + "|" + cx + "|" + cz;
    }

    /**
     * Places a slot.
     *
     * @throws IllegalArgumentException if any footprint block is already under another slot; the
     *         message names the first shared block
     */
    public void place(Slot slot) {
        if (byId.containsKey(slot.id())) {
            throw new IllegalArgumentException("a slot already exists at " + slot.id());
        }
        for (BlockPos b : slot.frame().footprint()) {
            Slot other = byBlock.get(blockKey(slot.world(), b));
            if (other != null) {
                throw new IllegalArgumentException("block " + b.x() + "," + b.y() + "," + b.z()
                        + " is already under slot " + other.id());
            }
        }
        byId.put(slot.id(), slot);
        for (BlockPos b : slot.frame().footprint()) {
            byBlock.put(blockKey(slot.world(), b), slot);
            // >> 4 is a floor division by 16, so x = -1 is chunk -1, not chunk 0.
            byChunk.computeIfAbsent(chunkKey(slot.world(), b.x() >> 4, b.z() >> 4), k -> new LinkedHashSet<>()).add(slot);
        }
    }

    /** Removes a slot and frees its blocks. */
    public Optional<Slot> remove(String id) {
        Slot slot = byId.remove(id);
        if (slot == null) return Optional.empty();
        for (BlockPos b : slot.frame().footprint()) {
            byBlock.remove(blockKey(slot.world(), b));
            String ck = chunkKey(slot.world(), b.x() >> 4, b.z() >> 4);
            Set<Slot> inChunk = byChunk.get(ck);
            if (inChunk != null) {
                inChunk.remove(slot);
                if (inChunk.isEmpty()) byChunk.remove(ck);
            }
        }
        return Optional.of(slot);
    }

    /** The slot whose footprint covers this block, if any. */
    public Optional<Slot> at(String world, BlockPos block) {
        return Optional.ofNullable(byBlock.get(blockKey(world, block)));
    }

    /** Every slot with at least one footprint block in this chunk, in placement order. */
    public List<Slot> inChunk(String world, int cx, int cz) {
        Set<Slot> slots = byChunk.get(chunkKey(world, cx, cz));
        return slots == null ? List.of() : new ArrayList<>(slots);
    }

    public Optional<Slot> find(String id) { return Optional.ofNullable(byId.get(id)); }

    public Collection<Slot> all() { return Collections.unmodifiableCollection(byId.values()); }
}
