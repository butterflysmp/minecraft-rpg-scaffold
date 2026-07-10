package io.github.butterflysmp.rpg.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The seam that makes a server network possible later without a rewrite now.
 *
 * Async by contract, because the day this is Postgres you must not block a
 * region thread on a socket. Write the callers correctly from the start even
 * though the file implementation would let you get away with blocking.
 *
 * Implementations, in the order you will need them:
 *   1. FilePlayerRepository   <- today
 *   2. PostgresPlayerRepository + Redis cache  <- when you add server 2
 */
public interface PlayerRepository {

    CompletableFuture<Optional<PlayerProfile>> load(UUID playerId);

    CompletableFuture<Void> save(PlayerProfile profile);

    /**
     * Cross-server safety. Prevents the same profile being loaded on two
     * servers at once during a transfer. A no-op for the file impl; a real
     * lock once you have Redis. Design for it now, implement it later.
     */
    CompletableFuture<Boolean> tryAcquireLock(UUID playerId, String serverId);

    CompletableFuture<Void> releaseLock(UUID playerId, String serverId);
}
