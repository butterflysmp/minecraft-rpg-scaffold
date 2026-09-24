package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The accessories file: what it writes, what it refuses, and what an absent one means. Same harness
 * as {@link FileVaultRepositoryTest} -- an inline executor, so every future is already complete.
 */
class FileAccessoryRepositoryTest {

    @TempDir
    Path dir;

    private static final Executor DIRECT = Runnable::run;

    private static final UUID PLAYER = UUID.fromString("7e8f9a0b-1c2d-4e3f-8a4b-5c6d7e8f9a0b");

    private FileAccessoryRepository repo() {
        return new FileAccessoryRepository(dir, DIRECT);
    }

    @Test
    void noFileMeansNoAccessories() {
        assertEquals(Optional.empty(), repo().load(PLAYER).join());
    }

    @Test
    void whatIsSavedIsWhatLoads_andTheTempFileIsGone() {
        PlayerAccessories saved = PlayerAccessories.empty(PLAYER).withSlot(0, "class-blob").withSlot(3, "u-blob");
        repo().save(saved).join();

        assertEquals(Optional.of(saved), repo().load(PLAYER).join());
        assertTrue(Files.exists(dir.resolve(PLAYER + ".json")));
        assertFalse(Files.exists(dir.resolve(PLAYER + ".json.tmp")), "the temp file is moved, not left");
    }

    /**
     * An UNDECODABLE item is text to this layer: it loads and is carried unchanged. Only a reader
     * decodes, so one bad accessory costs one slot.
     */
    @Test
    void anItemThisBuildCannotDecodeStillLoads_asText() {
        PlayerAccessories saved = PlayerAccessories.empty(PLAYER).withSlot(1, "!!not-base64!!");
        repo().save(saved).join();
        assertEquals(Optional.of("!!not-base64!!"), repo().load(PLAYER).join().orElseThrow().item(1));
    }

    /** A STRUCTURAL fault refuses the whole file -- never an empty result that a save would overwrite. */
    @Test
    void aDuplicateSlotRefusesTheFile_ratherThanLoadingEmpty() throws IOException {
        write("""
                {"schemaVersion":1,"playerId":"%s","entries":[{"slot":2,"item":"a"},{"slot":2,"item":"b"}]}
                """.formatted(PLAYER));
        CompletionException thrown = assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains("structurally invalid"), thrown.getCause().getMessage());
    }

    @Test
    void aSlotOutOfRangeRefusesTheFile() throws IOException {
        write("""
                {"schemaVersion":1,"playerId":"%s","entries":[{"slot":4,"item":"a"}]}
                """.formatted(PLAYER));
        CompletionException thrown = assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
        assertTrue(thrown.getCause().getMessage().contains("structurally invalid"), thrown.getCause().getMessage());
    }

    @Test
    void badJsonRefusesTheFile() throws IOException {
        write("{ this is not json");
        CompletionException thrown = assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
        assertTrue(thrown.getCause().getMessage().contains("not valid JSON"), thrown.getCause().getMessage());
    }

    @Test
    void aVersionZeroFileIsStampedOnLoad() throws IOException {
        write("""
                {"playerId":"%s","entries":[{"slot":0,"item":"a"}]}
                """.formatted(PLAYER));
        assertEquals(1, repo().load(PLAYER).join().orElseThrow().schemaVersion());
    }

    private void write(String json) throws IOException {
        Files.writeString(dir.resolve(PLAYER + ".json"), json, StandardCharsets.UTF_8);
    }
}
