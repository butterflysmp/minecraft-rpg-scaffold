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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An element is the thinnest content type: an id (the filename) and a display name. These
 * pin that, plus the fail-soft contract shared by every loader, plus that the seven shipped
 * elements load.
 */
class ElementLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("ElementLoaderTest-" + System.nanoTime());
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

    private ElementRegistry load() {
        return new ElementLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAnElementWithItsIdAndDisplayName() throws IOException {
        write("fire.yml", "display_name: \"<red>Fire</red>\"\n");

        ElementRegistry registry = load();

        assertEquals(1, registry.size());
        ElementDefinition fire = registry.find("fire").orElseThrow();
        assertEquals("fire", fire.id(), "the id is the filename");
        assertEquals("<red>Fire</red>", fire.displayName());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void displayNameDefaultsToTheId() throws IOException {
        write("kinetic.yml", "");

        assertEquals("kinetic", load().find("kinetic").orElseThrow().displayName());
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new ElementLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The seven shipped elements, parsed by the loader we actually run. */
    @Test
    void theSevenBundledElementsLoad() throws IOException {
        String[] ids = {"fire", "water", "nature", "undead", "void", "wither", "kinetic"};
        for (String id : ids) {
            try (var in = getClass().getResourceAsStream("/content/elements/" + id + ".yml")) {
                assertNotNull(in, "bundled element is missing from the classpath: " + id);
                Files.write(dir.resolve(id + ".yml"), in.readAllBytes());
            }
        }

        ElementRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(7, registry.size());
        for (String id : ids) {
            assertTrue(registry.find(id).isPresent(), id + " must load");
        }
    }
}
