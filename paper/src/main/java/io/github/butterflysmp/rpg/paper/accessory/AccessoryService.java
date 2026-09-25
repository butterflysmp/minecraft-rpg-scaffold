package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.storage.AccessoryRepository;
import io.github.butterflysmp.rpg.storage.PlayerAccessories;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns each online player's worn accessories: loaded on join, written through on every change,
 * dropped on quit. {@code VaultService}'s shape, for {@code VaultService}'s reasons -- and one
 * difference, stated below, in the order a write and its item movement happen.
 *
 * <h2>Unavailable is not empty</h2>
 *
 * A file that exists and cannot be read completes the load EXCEPTIONALLY and stays that way: the
 * accessories contribute nothing this session and every write is refused, so the file keeps its
 * last good contents. Presenting empty slots instead would let the next equip overwrite them.
 *
 * <h2>A failed write POISONS the store for the session</h2>
 *
 * The cache is updated before the write reaches disk, so a failed write leaves the cache AHEAD of
 * the file. Accepting a later write would republish that unpersisted state. So a failure replaces
 * the cache with a failed future: nothing further is written, the file keeps its last good
 * contents, and a SEVERE line names the player.
 *
 * <h2>The difference from the vault: the item moves only after the write settles</h2>
 *
 * {@link #write}'s callback runs when the write has reached disk or failed, and the Equipment screen
 * moves the item only then -- an unequip hands the item back only on SUCCESS, and an equip gives it
 * back only on FAILURE. So the item is never both in the file and in the inventory. The residuals --
 * a write that reached disk but reported failure, and a success whose hand-back never runs because
 * the player disconnected first -- are the vault's accepted DECISION 2 family.
 *
 * <p>Callbacks run on the storage I/O thread. Anything that touches Bukkit must hop first; the
 * Equipment screen does.
 */
public final class AccessoryService {

    private final AccessoryRepository repository;
    private final Logger log;
    private final Map<UUID, CompletableFuture<PlayerAccessories>> loaded = new ConcurrentHashMap<>();

    public AccessoryService(AccessoryRepository repository, Logger log) {
        this.repository = repository;
        this.log = log;
    }

    /** Start loading a joining player's accessories. A missing file means none. */
    public void onJoin(UUID playerId) {
        CompletableFuture<PlayerAccessories> loading = repository.load(playerId)
                .thenApply(found -> found.orElseGet(() -> PlayerAccessories.empty(playerId)));
        loading.exceptionally(error -> {
            log.log(Level.SEVERE, "Failed to load accessories for " + playerId
                    + "; they will contribute nothing and will not be written this session", error);
            return null;
        });
        loaded.put(playerId, loading);
    }

    /**
     * Forget a leaving player. Nothing is saved here: every change was written through when it was
     * made, so there is nothing pending to flush.
     */
    public void onQuit(UUID playerId) {
        loaded.remove(playerId);
    }

    /** The player's accessories, if loaded and readable. Empty while loading, and when unavailable. */
    public Optional<PlayerAccessories> accessories(UUID playerId) {
        CompletableFuture<PlayerAccessories> loading = loaded.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loading.getNow(null));
    }

    /** True when the load failed or a write poisoned the store -- NOT merely still loading. */
    public boolean unusable(UUID playerId) {
        CompletableFuture<PlayerAccessories> loading = loaded.get(playerId);
        return loading != null && loading.isDone() && loading.isCompletedExceptionally();
    }

    /**
     * Set {@code slot} to {@code item} (base64 text), or empty it when {@code item} is null, and
     * write the whole record through.
     *
     * @param onSettled called once the write has reached disk ({@code true}) or failed
     *                  ({@code false}), on the storage I/O thread
     * @return false when the store is not loaded or unusable -- nothing was changed or written
     */
    public boolean write(UUID playerId, int slot, String item, Consumer<Boolean> onSettled) {
        AccessorySlots.requireSlot(slot);
        Optional<PlayerAccessories> current = accessories(playerId);
        if (current.isEmpty()) return false;

        PlayerAccessories updated = current.get().withSlot(slot, item);
        loaded.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).whenComplete((ignored, error) -> {
            if (error != null) {
                poison(playerId, error);
                log.log(Level.SEVERE, "Accessories write FAILED for " + playerId + " (slot " + slot
                        + "). The store is poisoned for the session; the file keeps its last good"
                        + " contents.", error);
            }
            onSettled.accept(error == null);
        });
        return true;
    }

    private void poison(UUID playerId, Throwable cause) {
        loaded.put(playerId, CompletableFuture.failedFuture(new IllegalStateException(
                "accessories write failed for " + playerId + "; poisoned for the session", cause)));
    }
}
