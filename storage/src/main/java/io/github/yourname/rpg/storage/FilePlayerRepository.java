package io.github.yourname.rpg.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Milestone-1 storage. Deliberately dumb. Swap for Postgres at milestone 5
 * by implementing PlayerRepository -- nothing else in the codebase changes.
 */
public final class FilePlayerRepository implements PlayerRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;

    public FilePlayerRepository(Path root) {
        this.root = root;
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
                return Optional.ofNullable(GSON.fromJson(json, PlayerProfile.class));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
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
        });
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
