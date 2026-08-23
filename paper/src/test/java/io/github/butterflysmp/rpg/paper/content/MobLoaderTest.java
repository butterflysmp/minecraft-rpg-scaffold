package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
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
 * The mob schema, and the fail-soft contract every loader shares: a malformed file is logged, NAMED,
 * and skipped, and the rest still load.
 */
class MobLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("MobLoaderTest-" + System.nanoTime());
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

    private MobRegistry load() {
        return new MobLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAMobWithItsIdFromTheFilename() throws IOException {
        write("knell.yml", """
                base_entity: wither_skeleton
                display_name: "Knell"
                max_health: 360
                """);

        MobRegistry mobs = load();

        MobDefinition knell = mobs.find("knell").orElseThrow();
        assertEquals("knell", knell.id(), "the id is the filename");
        assertEquals("wither_skeleton", knell.baseEntity());
        assertEquals("Knell", knell.displayName());
        assertEquals(360, knell.maxHealth(), 1e-9);
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void displayNameDefaultsToTheId() throws IOException {
        write("wraith.yml", "base_entity: zombie\nmax_health: 50\n");

        assertEquals("wraith", load().find("wraith").orElseThrow().displayName());
    }

    /** base_entity has NO default: a wrong guess at which creature to spawn is not a cosmetic miss. */
    @Test
    void aMobWithNoBaseEntityIsSkippedAndNamed() throws IOException {
        write("broken.yml", "display_name: \"Broken\"\nmax_health: 100\n");

        MobRegistry mobs = load();

        assertEquals(0, mobs.size(), "a mob with no base_entity must not load");
        assertTrue(warningText().contains("broken.yml"),
                "the warning must name the file at fault, got: " + warningText());
    }

    @Test
    void aMobWithNoMaxHealthIsSkippedAndNamed() throws IOException {
        // max_health defaults to 0.0, which MobDefinition rejects -- 0 HP is born dead, not a default.
        write("ghost.yml", "base_entity: zombie\n");

        assertEquals(0, load().size());
        assertTrue(warningText().contains("ghost.yml"), warningText());
    }

    @Test
    void oneMalformedFileDoesNotStopTheOthers() throws IOException {
        write("knell.yml", "base_entity: wither_skeleton\nmax_health: 360\n");
        write("broken.yml", "display_name: \"no base entity\"\n");

        MobRegistry mobs = load();

        assertEquals(1, mobs.size(), "the good mob still loads");
        assertTrue(mobs.find("knell").isPresent());
        assertEquals(2, warnings.size(), "one file warning plus the skipped-count summary");
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var mobs = new MobLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, mobs.size());
    }

    /** The content we actually ship, parsed by the loader we actually run. */
    @Test
    void theBundledKnellLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/knell.yml")) {
            assertNotNull(in, "bundled knell is missing from the classpath");
            Files.write(dir.resolve("knell.yml"), in.readAllBytes());
        }

        MobRegistry mobs = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, mobs.size());
        MobDefinition knell = mobs.find("knell").orElseThrow();
        assertEquals("wither_skeleton", knell.baseEntity(),
                "the Knell is a wither skeleton -- the point is that ordinary ones stay ordinary");
        assertEquals("Knell", knell.displayName());
        assertEquals(360, knell.maxHealth(), 1e-9);
    }
}
