package io.github.butterflysmp.rpg.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * One JSON file per player under {@code accessories/}. {@link FileVaultRepository}'s shape exactly --
 * the same executor contract, the same temp-then-atomic-move write, the same refusal of a file that
 * exists and cannot be read -- because the reasons are the same and a second design would be a
 * second set of edge cases.
 *
 * <p><b>A refusal is not an empty result.</b> Loading a corrupt file as "no accessories" would let
 * the next save overwrite whatever it held. So a structural fault or bad JSON completes the future
 * EXCEPTIONALLY, and the caller leaves the store unavailable.
 */
public final class FileAccessoryRepository implements AccessoryRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;
    private final Executor io;

    public FileAccessoryRepository(Path root, Executor io) {
        this.root = Objects.requireNonNull(root, "root");
        this.io = Objects.requireNonNull(io, "io executor");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path fileFor(UUID id) { return root.resolve(id + ".json"); }

    @Override
    public CompletableFuture<Optional<PlayerAccessories>> load(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Path f = fileFor(playerId);
            if (!Files.exists(f)) return Optional.empty();
            String json;
            try {
                json = Files.readString(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            PlayerAccessories raw;
            try {
                raw = GSON.fromJson(json, PlayerAccessories.class);
            } catch (JsonParseException e) {
                throw refusal(playerId, "is not valid JSON", e);
            } catch (RuntimeException e) {
                // Gson wraps an exception from a record's canonical constructor; the structural
                // refusals (a duplicate slot, a slot out of range, a blank item) arrive this way.
                if (e.getCause() instanceof IllegalArgumentException structural) {
                    throw refusal(playerId, "is structurally invalid -- " + structural.getMessage(),
                            structural);
                }
                throw e;
            }
            return Optional.ofNullable(raw).map(AccessoryMigrations::migrate);
        }, io);
    }

    private static IllegalStateException refusal(UUID playerId, String problem, Throwable cause) {
        return new IllegalStateException(
                "Accessories file for " + playerId + " " + problem + ". Refusing to load it rather"
                        + " than presenting empty slots that would overwrite it.",
                cause);
    }

    @Override
    public CompletableFuture<Void> save(PlayerAccessories accessories) {
        return CompletableFuture.runAsync(() -> {
            Path f = fileFor(accessories.playerId());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try {
                Files.writeString(tmp, GSON.toJson(accessories), StandardCharsets.UTF_8);
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io);
    }
}
