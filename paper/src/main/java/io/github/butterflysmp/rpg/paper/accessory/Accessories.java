package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.AccessoryRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.vault.VaultCodec;
import io.github.butterflysmp.rpg.paper.weapon.AccessoryItems;
import io.github.butterflysmp.rpg.storage.PlayerAccessories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The accessory kind's runtime: its registry, its store, and the one read everything else makes --
 * "which definitions is this player wearing?".
 *
 * <p>Carried on {@code AdapterContext}, which every screen and command already holds, rather than
 * threaded through the Nexus hub's constructor and the ten call sites that rebuild it.
 *
 * <h2>Decoded once per stored record, not once per reconcile tick</h2>
 *
 * The store holds each item as base64 text. The reconcile loop asks four times a second, and
 * decoding four ItemStacks per player per pass would put the NBT reader on the hottest loop in the
 * plugin. So the decoded definitions are memoised against the stored record's IDENTITY: a write
 * replaces the record, the next read sees a different object and decodes again, and every other read
 * is a map lookup.
 *
 * <p>A slot whose item cannot be decoded, or whose id no longer has a content file, reads as
 * {@code null} -- it contributes nothing, and the stored text is left exactly as it was.
 */
public final class Accessories {

    private final AccessoryRegistry registry;
    private final AccessoryService service;
    private final Keys keys;

    private record Decoded(PlayerAccessories from, List<AccessoryDefinition> worn) {}

    private final Map<UUID, Decoded> decoded = new ConcurrentHashMap<>();

    public Accessories(AccessoryRegistry registry, AccessoryService service, Keys keys) {
        this.registry = registry;
        this.service = service;
        this.keys = keys;
    }

    public AccessoryRegistry registry() { return registry; }

    public AccessoryService service() { return service; }

    /**
     * The four slots' definitions, index = slot; {@code null} for an empty or unreadable slot. Empty
     * Optional when the store is not loaded or unusable -- different from four nulls, and the stats
     * sheet says which.
     */
    public Optional<List<AccessoryDefinition>> worn(UUID playerId) {
        Optional<PlayerAccessories> stored = service.accessories(playerId);
        if (stored.isEmpty()) {
            decoded.remove(playerId);
            return Optional.empty();
        }
        Decoded memo = decoded.get(playerId);
        if (memo == null || memo.from() != stored.get()) {
            memo = new Decoded(stored.get(), decode(stored.get()));
            decoded.put(playerId, memo);
        }
        return Optional.of(memo.worn());
    }

    /** What the player's accessories contribute, for a profile class of {@code profileClass}. */
    public AccessoryContributions contributions(UUID playerId, String profileClass) {
        return worn(playerId)
                .map(slots -> AccessoryContributions.of(slots, profileClass))
                .orElse(AccessoryContributions.NONE);
    }

    /** Drop the memo for a leaving player. */
    public void forget(UUID playerId) {
        decoded.remove(playerId);
    }

    private List<AccessoryDefinition> decode(PlayerAccessories stored) {
        List<AccessoryDefinition> slots = new ArrayList<>(Collections.nCopies(AccessorySlots.COUNT, null));
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            final int index = slot;
            stored.item(slot)
                    .flatMap(VaultCodec::decode)
                    .flatMap(item -> AccessoryItems.accessoryId(item, keys))
                    .flatMap(registry::find)
                    .ifPresent(def -> slots.set(index, def));
        }
        return Collections.unmodifiableList(slots);
    }
}
