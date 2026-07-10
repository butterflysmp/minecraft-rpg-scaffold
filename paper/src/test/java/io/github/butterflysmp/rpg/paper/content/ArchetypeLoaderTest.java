package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.archetype.ArchetypeRegistry;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Same fail-soft contract as StatusLoaderTest: id is the filename, malformed skipped. */
class ArchetypeLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("ArchetypeLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    private ArchetypeRegistry load() {
        return new ArchetypeLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAnArchetypeAndTakesTheIdFromTheFilename() throws IOException {
        write("hunter.yml", """
                display_name: "<green>Hunter</green>"
                abilities:
                  - solar_grenade
                  - solar_lance
                """);

        ArchetypeRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        var hunter = registry.find("hunter").orElseThrow();
        assertEquals("hunter", hunter.id());
        assertEquals("<green>Hunter</green>", hunter.displayName());
        assertEquals(List.of("solar_grenade", "solar_lance"), hunter.abilityIds());
    }

    @Test
    void displayNameDefaultsToTheId() throws IOException {
        write("mage.yml", """
                abilities:
                  - arc_surge
                """);

        assertEquals("mage", load().find("mage").orElseThrow().displayName());
    }

    /** A class that grants nothing is a defect: skipped and named, not shipped empty. */
    @Test
    void archetypeWithEmptyAbilitiesIsSkippedNotCrashed() throws IOException {
        write("aaa_empty.yml", "display_name: Empty\nabilities: []\n");
        write("hunter.yml", "abilities:\n  - solar_grenade\n");

        ArchetypeRegistry registry = load();

        assertEquals(1, registry.size(), "the valid class must still load");
        assertTrue(warningText().contains("aaa_empty.yml"), warningText());
    }

    @Test
    void archetypeWithNoAbilitiesKeyIsSkippedNotCrashed() throws IOException {
        write("aaa_bare.yml", "display_name: Bare\n");
        write("hunter.yml", "abilities:\n  - solar_grenade\n");

        ArchetypeRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_bare.yml"), warningText());
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new ArchetypeLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The content we actually ship, parsed by the loader we actually run. */
    @Test
    void bundledHunterContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/archetypes/hunter.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("hunter.yml"), in.readAllBytes());
        }

        ArchetypeRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        assertEquals(List.of("arc_surge", "solar_grenade", "solar_lance", "void_slash"),
                registry.find("hunter").orElseThrow().abilityIds());
    }
}
