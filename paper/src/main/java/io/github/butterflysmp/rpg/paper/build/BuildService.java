package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.storage.BuildRepository;
import io.github.butterflysmp.rpg.storage.CellLoadout;
import io.github.butterflysmp.rpg.storage.PlayerBuild;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Each online player's saved build, cached (PLAN-build-system.md section 2.1). {@code AccessoryService}'s
 * shape exactly, and for its reasons:
 * <ul>
 *   <li>loaded on join, asynchronously; a missing file is an EMPTY build;</li>
 *   <li>an unreadable file (bad JSON, two loadouts for one cell, a wrong list length) leaves the build
 *       UNUSABLE for the session, logged SEVERE -- and the stone then casts the pool default and says so
 *       on its lore. It is never read as empty, which would overwrite the file on the next save;</li>
 *   <li>every change is written through, the whole file at once;</li>
 *   <li>a failed write POISONS the cache for the session: later writes are refused and the file keeps its
 *       last good contents.</li>
 * </ul>
 *
 * <p>No Bukkit here: callbacks run on the storage I/O thread, and every caller hops back itself.
 */
public final class BuildService {

    private final BuildRepository repository;
    private final Logger log;
    private final Map<UUID, CompletableFuture<PlayerBuild>> loaded = new ConcurrentHashMap<>();

    public BuildService(BuildRepository repository, Logger log) {
        this.repository = repository;
        this.log = log;
    }

    /**
     * Start loading this player's build. The returned future completes (normally or not) when the load
     * settles, so a caller can re-render the stone's lore once the saved loadout is known.
     */
    public CompletableFuture<?> onJoin(UUID playerId) {
        CompletableFuture<PlayerBuild> loading = repository.load(playerId)
                .thenApply(found -> found.orElseGet(() -> PlayerBuild.empty(playerId)));
        loading.exceptionally(error -> {
            log.log(Level.SEVERE, "Failed to load the build for " + playerId + "; the Ability Stone will"
                    + " cast the pool default and the build will not be written this session", error);
            return null;
        });
        loaded.put(playerId, loading);
        return loading.handle((ignored, error) -> null);
    }

    public void onQuit(UUID playerId) {
        loaded.remove(playerId);
    }

    /** The build, if it has finished loading and did not fail. */
    public Optional<PlayerBuild> build(UUID playerId) {
        CompletableFuture<PlayerBuild> loading = loaded.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loading.getNow(null));
    }

    /** The build failed to load, or a write poisoned it: unusable for the rest of the session. */
    public boolean unusable(UUID playerId) {
        CompletableFuture<PlayerBuild> loading = loaded.get(playerId);
        return loading != null && loading.isDone() && loading.isCompletedExceptionally();
    }

    /**
     * Save one cell's loadout, write-through. Refused (false) while the build is loading or unusable.
     * {@code onSettled} runs on the I/O thread with whether the write reached disk.
     */
    public boolean save(UUID playerId, CellLoadout loadout, Consumer<Boolean> onSettled) {
        Optional<PlayerBuild> current = build(playerId);
        if (current.isEmpty()) return false;
        PlayerBuild updated = current.get().with(loadout);
        loaded.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).whenComplete((ignored, error) -> {
            if (error != null) {
                poison(playerId, error);
                log.log(Level.SEVERE, "Build write FAILED for " + playerId + " (" + loadout.classId() + "/"
                        + loadout.elementId() + "). The build is poisoned for the session; the file keeps its"
                        + " last good contents.", error);
            }
            onSettled.accept(error == null);
        });
        return true;
    }

    private void poison(UUID playerId, Throwable cause) {
        loaded.put(playerId, CompletableFuture.failedFuture(new IllegalStateException(
                "build write failed for " + playerId + "; poisoned for the session", cause)));
    }
}
