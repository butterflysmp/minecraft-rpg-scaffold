package io.github.butterflysmp.rpg.storage;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A player's worn accessories, in their own file -- {@code accessories/<uuid>.json}.
 *
 * <h2>Why its own repository rather than a field on the profile</h2>
 *
 * {@code PLAN-accessories.md} §3.9. A profile field costs a schema bump and a 13th component through
 * every one of {@code PlayerProfile}'s {@code withX} rebuilds, and a bad accessory write would then
 * put the class, the XP and the Nexus slot at risk. Its own file limits the blast radius to the
 * accessories, and equipment sets later reuse the same store.
 *
 * <h2>What loads and what does not -- the vault's rules, stated for this file</h2>
 *
 * <ul>
 *   <li>An entry whose ITEM cannot be decoded still loads: it is text here, and only a reader
 *       decodes. It costs that one slot, and is written back unchanged.
 *   <li>A STRUCTURAL fault -- two entries for one slot, a slot outside {@code 0..3}, a blank item --
 *       refuses the whole file. The repository then reports it, and the service leaves the store
 *       unavailable for the session rather than presenting an empty one that would overwrite it.
 * </ul>
 *
 * <p>A record: the components are fixed at construction, and a change is a new record.
 */
public record PlayerAccessories(int schemaVersion, UUID playerId, List<AccessoryEntry> entries) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PlayerAccessories {
        Objects.requireNonNull(playerId, "playerId");
        List<AccessoryEntry> source = entries == null ? List.of() : entries;
        boolean[] seen = new boolean[AccessorySlots.COUNT];
        List<AccessoryEntry> sorted = new ArrayList<>();
        for (AccessoryEntry entry : source) {
            Objects.requireNonNull(entry, "accessory entry");
            if (seen[entry.slot()]) {
                throw new IllegalArgumentException("accessories for " + playerId + " have two entries"
                        + " for slot " + entry.slot() + "; refusing to load rather than silently"
                        + " discarding one");
            }
            seen[entry.slot()] = true;
            sorted.add(entry);
        }
        sorted.sort(Comparator.comparingInt(AccessoryEntry::slot));
        entries = List.copyOf(sorted);
    }

    public static PlayerAccessories empty(UUID playerId) {
        return new PlayerAccessories(CURRENT_SCHEMA_VERSION, playerId, List.of());
    }

    /** The stored item in {@code slot}, as text, if the slot is occupied. */
    public Optional<String> item(int slot) {
        AccessorySlots.requireSlot(slot);
        for (AccessoryEntry entry : entries) {
            if (entry.slot() == slot) return Optional.of(entry.item());
        }
        return Optional.empty();
    }

    /** A copy with {@code slot} set to {@code item}, or emptied when {@code item} is null. */
    public PlayerAccessories withSlot(int slot, String item) {
        AccessorySlots.requireSlot(slot);
        List<AccessoryEntry> next = new ArrayList<>();
        for (AccessoryEntry entry : entries) {
            if (entry.slot() != slot) next.add(entry);
        }
        if (item != null) next.add(new AccessoryEntry(slot, item));
        return new PlayerAccessories(schemaVersion, playerId, next);
    }

    public PlayerAccessories withSchemaVersion(int version) {
        return new PlayerAccessories(version, playerId, entries);
    }
}
