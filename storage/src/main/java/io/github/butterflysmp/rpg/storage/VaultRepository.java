package io.github.butterflysmp.rpg.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The vault's seam, mirroring {@link PlayerRepository} so the day this is Postgres is one class.
 *
 * <p>Async by contract, for {@link PlayerRepository}'s reason and one more of its own: a vault write
 * is <b>larger than a profile write and happens far more often</b> -- once per item moved, rather
 * than once per level gained. Blocking a region thread on it would be felt.
 *
 * <p><b>Separate from {@code PlayerRepository} rather than a fifth method on it.</b> The two files
 * have different sizes, different write frequencies and different schema lines, and a single port
 * returning both would make every profile read pay for the vault.
 *
 * <p>No lock methods. {@code PlayerRepository} carries {@code tryAcquireLock} against the day a
 * player is loaded on two servers at once; when that day comes the lock is on the PLAYER, not on
 * each of their files, so it belongs on the port that already has it rather than duplicated here.
 */
public interface VaultRepository {

    /**
     * @return empty if the player has no vault file at all -- which means an EMPTY VAULT, not an
     *         error. Distinguishing the two is the caller's job and there is nothing to
     *         disambiguate: no vault and an empty vault are the same thing.
     */
    CompletableFuture<Optional<PlayerVault>> load(UUID playerId);

    CompletableFuture<Void> save(PlayerVault vault);
}
