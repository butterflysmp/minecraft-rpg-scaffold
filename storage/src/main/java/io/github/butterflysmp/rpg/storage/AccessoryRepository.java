package io.github.butterflysmp.rpg.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Where a player's worn accessories are kept. Async by contract, like every port in this module:
 * callers get a future and never block the thread that owns the player.
 *
 * <p>{@link #load} completes EMPTY for a player with no file -- no accessories and an empty set are
 * the same thing -- and EXCEPTIONALLY for a file that exists and cannot be read.
 */
public interface AccessoryRepository {

    CompletableFuture<Optional<PlayerAccessories>> load(UUID playerId);

    CompletableFuture<Void> save(PlayerAccessories accessories);
}
