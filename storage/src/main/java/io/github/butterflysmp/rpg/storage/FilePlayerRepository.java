package io.github.butterflysmp.rpg.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Milestone-1 storage. Deliberately dumb. Swap for Postgres at milestone 5
 * by implementing PlayerRepository -- nothing else in the codebase changes.
 *
 * The Executor is a constructor parameter on purpose. Blocking file I/O must not
 * run on ForkJoinPool.commonPool(), which is shared with the whole JVM and sized
 * for CPU-bound work. When this becomes Postgres, that parameter is where the
 * connection pool's threads get controlled.
 */
public final class FilePlayerRepository implements PlayerRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;
    private final Executor io;

    public FilePlayerRepository(Path root, Executor io) {
        this.root = root;
        this.io = Objects.requireNonNull(io, "io executor");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path fileFor(UUID id) { return root.resolve(id + ".json"); }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> load(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Path f = fileFor(playerId);
            if (!Files.exists(f)) return Optional.empty();
            try {
                String json = Files.readString(f, StandardCharsets.UTF_8);
                PlayerProfile raw = GSON.fromJson(json, PlayerProfile.class);
                // Bring an older on-disk shape up to date before anyone sees it.
                return Optional.ofNullable(raw).map(ProfileMigrations::migrate);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io);
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            Path f = fileFor(profile.playerId());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try {
                Files.writeString(tmp, GSON.toJson(profile), StandardCharsets.UTF_8);
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> tryAcquireLock(UUID playerId, String serverId) {
        return CompletableFuture.completedFuture(true); // single server, no contention
    }

    @Override
    public CompletableFuture<Void> releaseLock(UUID playerId, String serverId) {
        return CompletableFuture.completedFuture(null);
    }
}
