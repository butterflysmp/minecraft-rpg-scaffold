package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code content/mob_elements.yml}: the vanilla entity type -> element table (PLAN-dragon-mobs.md, N2). A bad row is
 * REFUSED and NAMED, and the rest of the table loads, as {@code withering.yml}'s immune list does.
 */
class MobElementLoaderTest {

    @TempDir
    Path dir;

    private final List<LogRecord> warnings = new ArrayList<>();
    private Logger log;

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("MobElementLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private Map<String, String> load(String yaml) throws IOException {
        Path file = dir.resolve("mob_elements.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return new MobElementLoader(log, Set.of("void", "fire")::contains).load(file.toFile());
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void aGoodRowLoads() throws IOException {
        assertEquals(Map.of("enderman", "void"), load("enderman: void\n"));
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void anUnknownElementIsRefusedNamingTheRowAndTheRestLoad() throws IOException {
        Map<String, String> table = load("enderman: void\nblaze: fier\n");

        assertEquals(Map.of("enderman", "void"), table);
        assertTrue(warningText().contains("Refusing"), warningText());
        assertTrue(warningText().contains("blaze"), warningText());
        assertTrue(warningText().contains("'fier'"), warningText());
    }

    /** A namespaced or misspelled type would match no creature, so it is refused rather than kept as a dead row. */
    @Test
    void aTypeKeyThatNamesNoEntityIsRefused() throws IOException {
        Map<String, String> table = load("enderman: void\nendermen: void\n");

        assertEquals(Map.of("enderman", "void"), table);
        assertTrue(warningText().contains("endermen"), warningText());
    }

    @Test
    void aMissingFileIsAnEmptyTable() {
        assertEquals(Map.of(), new MobElementLoader(log, s -> true).load(new File(dir.toFile(), "absent.yml")));
    }

    /** The table we ship: exactly one row today, Ben's "normal Vanilla Enderman are also Void". */
    @Test
    void theBundledTableIsEndermanVoidAndNothingElse() throws IOException {
        Path file = dir.resolve("mob_elements.yml");
        try (var in = getClass().getResourceAsStream("/content/mob_elements.yml")) {
            assertNotNull(in, "the bundled table is missing from the classpath");
            Files.write(file, in.readAllBytes());
        }
        Map<String, String> table = new MobElementLoader(log, Set.of("void")::contains).load(file.toFile());
        assertEquals(Map.of("enderman", "void"), table);
        assertTrue(warnings.isEmpty(), warningText());
    }
}
