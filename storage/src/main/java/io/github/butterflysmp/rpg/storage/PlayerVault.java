package io.github.butterflysmp.rpg.storage;

import io.github.butterflysmp.rpg.core.vault.VaultShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * One player's vault: seven pages of thirty-six, stored SPARSELY as occupied slots only.
 *
 * <p>Its own file beside the profile, not a field on {@link PlayerProfile}, and the reason is write
 * amplification rather than tidiness: the profile is loaded and saved WHOLESALE per player, so 252
 * item blobs living on it would rewrite every byte of a player's identity on each item they move.
 *
 * <h2>SPARSE, NOT A 252-CELL ARRAY</h2>
 *
 * An empty slot is represented by <b>having no entry</b>. Three things follow, and the third is the
 * one that matters:
 *
 * <ol>
 *   <li>A typical file is a few hundred bytes rather than a fixed quarter-megabyte.</li>
 *   <li>A hand-read file says what is in the vault, not where the holes are.</li>
 *   <li><b>One unreadable item costs one slot.</b> A whole-page blob would take all thirty-six down
 *       with it -- which is exactly why this does not use Paper's {@code serializeItemsAsBytes}.</li>
 * </ol>
 *
 * <h2>AN ABSENT FILE MEANS EMPTY, AND THERE IS NOTHING TO DISAMBIGUATE</h2>
 *
 * That is {@code lifetimeXp}'s case, not {@code nexusSlot}'s: empty is the correct reading of
 * absence, and no choice a player can make produces a different meaning for it. <b>So this record
 * carries no "has ever been opened" flag and needs no migration step to invent one.</b>
 *
 * <p><b>The MIGRATION stamp is a different question and does NOT live here.</b> "Has this player's
 * vanilla ender chest been copied in yet" genuinely has no room in this value space -- an empty page
 * 1 is indistinguishable from a migrated empty ender chest -- so that flag lives on the profile,
 * where a schema stamp can separate absence from choice.
 *
 * <h2>ENTRIES ARE SORTED, BECAUSE A DIFF IS A DEBUGGING INSTRUMENT</h2>
 *
 * Canonical order by page then slot, so two saves of the same contents are byte-identical and a
 * {@code git diff} of a test fixture shows what changed rather than what moved.
 */
public record PlayerVault(int schemaVersion, UUID playerId, List<VaultEntry> entries) {

    /**
     * Bump when the on-disk shape changes, and add a step to {@link VaultMigrations}.
     *
     * <p><b>This is a SECOND schema line, independent of {@link PlayerProfile#CURRENT_SCHEMA_VERSION}.</b>
     * The profile's newer-server refusal protects the file it is wired into and cannot see a sibling.
     * A vault written by a newer build and loaded by an older one would otherwise lose whatever
     * fields that build did not know about -- and then write the loss back. For a profile that costs
     * progress; here it costs items.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PlayerVault {
        Objects.requireNonNull(playerId, "playerId");

        // Gson leaves an absent array null. Empty is the right reading and the compact constructor
        // is the one place that can say so before anybody sees the record.
        List<VaultEntry> source = entries == null ? List.of() : entries;

        // Duplicate detection is on the PAIR, not on the slot alone -- slot 4 exists on all seven
        // pages and only (page, slot) identifies a cell. Keyed on a long so no string formatting
        // sits on the load path.
        Map<Long, VaultEntry> seen = new LinkedHashMap<>();
        for (VaultEntry entry : source) {
            Objects.requireNonNull(entry, "vault entry");
            long cell = (long) entry.page() * VaultShape.SLOTS_PER_PAGE + entry.slot();
            VaultEntry clash = seen.putIfAbsent(cell, entry);
            if (clash != null) {
                // REFUSED rather than last-write-wins. Two entries for one cell means two items
                // claiming one slot, and silently dropping either is the failure this whole slice
                // exists to prevent. Say which cell, so the file can be looked at.
                throw new IllegalArgumentException(
                        "vault for " + playerId + " has two entries for page " + entry.page()
                                + " slot " + entry.slot()
                                + "; refusing to load rather than silently discarding one");
            }
        }

        List<VaultEntry> sorted = new ArrayList<>(seen.values());
        sorted.sort(Comparator.comparingInt(VaultEntry::page).thenComparingInt(VaultEntry::slot));
        entries = List.copyOf(sorted);
    }

    /** A vault that has never been written. What an absent file deserialises to. */
    public static PlayerVault empty(UUID playerId) {
        return new PlayerVault(CURRENT_SCHEMA_VERSION, playerId, List.of());
    }

    /**
     * One page's occupied slots, as slot index to opaque item text.
     *
     * <p>Unmodifiable, and in slot order. Absent slots are absent from the map rather than mapped to
     * null -- a caller iterating this gets only real items, and a caller asking for a specific slot
     * gets {@code null} for empty, which is the same answer an inventory gives.
     */
    public Map<Integer, String> page(int page) {
        VaultShape.requirePage(page);
        Map<Integer, String> contents = new LinkedHashMap<>();
        for (VaultEntry entry : entries) {
            if (entry.page() == page) contents.put(entry.slot(), entry.item());
        }
        return Collections.unmodifiableMap(contents);
    }

    /**
     * This vault with ONE page replaced wholesale.
     *
     * <p><b>Wholesale is the correct granularity and a per-slot variant would be a trap.</b> The
     * screen writes a page back by encoding all thirty-six live cells at once; a slot-at-a-time API
     * would invite a caller to write the slot that changed and leave the thirty-five it did not look
     * at -- which is right until a gesture moves two cells, and a drag moves nine.
     *
     * <p>Entries whose item is null or blank are dropped rather than rejected: the caller is handing
     * over a snapshot of an inventory page, and empty cells are the normal case, not an error.
     */
    public PlayerVault withPage(int page, Map<Integer, String> contents) {
        VaultShape.requirePage(page);
        Objects.requireNonNull(contents, "contents");

        List<VaultEntry> next = new ArrayList<>();
        for (VaultEntry entry : entries) {
            if (entry.page() != page) next.add(entry);
        }
        for (Map.Entry<Integer, String> cell : contents.entrySet()) {
            String item = cell.getValue();
            if (item == null || item.isBlank()) continue;
            next.add(new VaultEntry(page, cell.getKey(), item));
        }
        return new PlayerVault(schemaVersion, playerId, next);
    }

    public PlayerVault withSchemaVersion(int version) {
        return new PlayerVault(version, playerId, entries);
    }

    /** How many slots hold something, across every page. For the dev read-back and for gate rows. */
    public int occupiedSlots() {
        return entries.size();
    }
}
