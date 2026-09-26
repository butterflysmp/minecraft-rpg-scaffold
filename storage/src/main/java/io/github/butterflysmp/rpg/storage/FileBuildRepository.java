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
 * {@code builds/<uuid>.json}, one file per player -- {@code FileAccessoryRepository}'s shape exactly: pretty
 * JSON, a temp file then an atomic move, and a structural fault refused rather than read as empty.
 *
 * <p><b>{@code serializeNulls} IS ON HERE, unlike the profile's Gson.</b> An empty slot is a {@code null},
 * and with this setting every empty slot is WRITTEN as {@code null} rather than left for the reader to
 * infer from absence. A file then shows every slot its loadout has, which is what a person hand-inspecting
 * one for BS7 needs. (Reading does not depend on it: {@link CellLoadout} normalises an absent list to
 * empty slots either way. {@code FileBuildRepositoryTest} round-trips an empty slot to pin that.)
 */
public final class FileBuildRepository implements BuildRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private final Path root;
    private final Executor io;

    public FileBuildRepository(Path root, Executor io) {
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
    public CompletableFuture<Optional<PlayerBuild>> load(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Path f = fileFor(playerId);
            if (!Files.exists(f)) return Optional.empty();
            String json;
            try {
                json = Files.readString(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            PlayerBuild raw;
            try {
                raw = GSON.fromJson(json, PlayerBuild.class);
            } catch (JsonParseException e) {
                throw refusal(playerId, "is not valid JSON", e);
            } catch (RuntimeException e) {
                // Gson wraps a record constructor's IllegalArgumentException; that is a STRUCTURAL fault
                // (two loadouts for one cell, a list of the wrong length), not a parse error.
                if (e.getCause() instanceof IllegalArgumentException structural) {
                    throw refusal(playerId, "is structurally invalid -- " + structural.getMessage(), structural);
                }
                throw e;
            }
            return Optional.ofNullable(raw).map(BuildMigrations::migrate);
        }, io);
    }

    private static IllegalStateException refusal(UUID playerId, String problem, Throwable cause) {
        return new IllegalStateException(
                "Build file for " + playerId + " " + problem + ". Refusing to load it rather than presenting"
                        + " an empty build that would overwrite it.",
                cause);
    }

    @Override
    public CompletableFuture<Void> save(PlayerBuild build) {
        return CompletableFuture.runAsync(() -> {
            Path f = fileFor(build.playerId());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try {
                Files.writeString(tmp, GSON.toJson(build), StandardCharsets.UTF_8);
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io);
    }
}
