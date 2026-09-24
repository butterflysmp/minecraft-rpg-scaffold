package io.github.butterflysmp.rpg.storage;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;

import java.util.Objects;

/**
 * One worn accessory: which slot it is in, and the item, as base64 of {@code serializeAsBytes()}.
 *
 * <p>The vault's {@link VaultEntry} shape, for the vault's reasons. The item is carried as TEXT and
 * decoded only by a reader, so an entry this build cannot decode is still carried byte for byte --
 * one unreadable accessory costs that slot, never the others. An empty slot has NO entry; a blank one
 * is refused.
 */
public record AccessoryEntry(int slot, String item) {

    public AccessoryEntry {
        AccessorySlots.requireSlot(slot);
        Objects.requireNonNull(item, "item");
        if (item.isBlank()) {
            throw new IllegalArgumentException("accessory entry at slot " + slot + " has a blank item;"
                    + " an empty slot is represented by having NO entry, never by a blank one");
        }
    }
}
