package io.github.butterflysmp.rpg.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The port for a player's saved build (PLAN-build-system.md section 2.1). Async by contract, like every
 * store here. {@code load} completes EMPTY for a player with no file, and EXCEPTIONALLY for a file that
 * cannot be trusted -- never with an empty build standing in for an unreadable one, which would overwrite
 * it on the next save.
 */
public interface BuildRepository {

    CompletableFuture<Optional<PlayerBuild>> load(UUID playerId);

    CompletableFuture<Void> save(PlayerBuild build);
}
