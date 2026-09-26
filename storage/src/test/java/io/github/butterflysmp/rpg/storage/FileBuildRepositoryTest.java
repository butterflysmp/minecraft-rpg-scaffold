package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBuildRepositoryTest {

    @TempDir
    Path dir;

    private static final Executor DIRECT = Runnable::run;

    private static final UUID PLAYER = UUID.fromString("1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f");

    private FileBuildRepository repo() {
        return new FileBuildRepository(dir, DIRECT);
    }

    private void write(String json) throws IOException {
        Files.writeString(dir.resolve(PLAYER + ".json"), json, StandardCharsets.UTF_8);
    }

    @Test
    void noFileMeansNoBuild() {
        assertEquals(Optional.empty(), repo().load(PLAYER).join());
    }

    /** Round trip, INCLUDING an empty slot: a null must come back as a null in the same position. */
    @Test
    void whatIsSavedIsWhatLoads_emptySlotsIncluded_andTheTempFileIsGone() {
        PlayerBuild saved = PlayerBuild.empty(PLAYER)
                .with(new CellLoadout("ranger", "fire", null, Arrays.asList(null, "solar_lance"), null, null))
                .with(new CellLoadout("mage", "fire", "ultimate_placeholder_mage",
                        List.of("ember_step", "solar_grenade"), null, null));
        repo().save(saved).join();

        PlayerBuild loaded = repo().load(PLAYER).join().orElseThrow();
        assertEquals(saved, loaded);
        CellLoadout ranger = loaded.loadout("ranger", "fire").orElseThrow();
        assertNull(ranger.ultimate());
        assertEquals(Arrays.asList(null, "solar_lance"), ranger.actives());
        assertTrue(Files.exists(dir.resolve(PLAYER + ".json")));
        assertFalse(Files.exists(dir.resolve(PLAYER + ".json.tmp")), "the temp file is moved, not left");
    }

    /** BS7's unit half: a structural fault is REFUSED (the future fails), never read as an empty build. */
    @Test
    void twoLoadoutsForOneCellRefuseTheWholeFile() throws IOException {
        write("""
                {"schemaVersion": 1, "playerId": "%s", "cells": [
                  {"classId": "ranger", "elementId": "fire"},
                  {"classId": "ranger", "elementId": "fire"}
                ]}""".formatted(PLAYER));
        CompletionException thrown = assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains("structurally invalid"), thrown.getCause().getMessage());
    }

    @Test
    void aWrongListLengthRefusesTheWholeFile() throws IOException {
        write("""
                {"schemaVersion": 1, "playerId": "%s", "cells": [
                  {"classId": "ranger", "elementId": "fire", "actives": ["recall"]}
                ]}""".formatted(PLAYER));
        assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
    }

    @Test
    void badJsonRefusesTheWholeFile() throws IOException {
        write("{ this is not json");
        CompletionException thrown = assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
        assertTrue(thrown.getCause().getMessage().contains("not valid JSON"), thrown.getCause().getMessage());
    }

    /** Absent lists in a hand-written file read as empty slots, not as a refusal. */
    @Test
    void absentListsReadAsEmptySlots() throws IOException {
        write("""
                {"schemaVersion": 1, "playerId": "%s", "cells": [
                  {"classId": "ranger", "elementId": "fire", "ultimate": "ultimate_placeholder_ranger"}
                ]}""".formatted(PLAYER));
        CellLoadout ranger = repo().load(PLAYER).join().orElseThrow().loadout("ranger", "fire").orElseThrow();
        assertEquals("ultimate_placeholder_ranger", ranger.ultimate());
        assertEquals(Arrays.asList(null, null), ranger.actives());
    }

    @Test
    void aNewerSchemaIsRefused() throws IOException {
        write("""
                {"schemaVersion": 99, "playerId": "%s", "cells": []}""".formatted(PLAYER));
        assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());
    }

    @Test
    void aFileWithNoStampIsStampedToOne() throws IOException {
        write("""
                {"playerId": "%s", "cells": []}""".formatted(PLAYER));
        assertEquals(1, repo().load(PLAYER).join().orElseThrow().schemaVersion());
    }
}
