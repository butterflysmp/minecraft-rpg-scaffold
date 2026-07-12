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
 * Same fail-soft contract as AbilityLoaderTest. No server: the loader resolves no
 * PotionEffectType, which is the whole reason potion_type stays a NamespacedKey.
 */
class StatusLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("StatusLoaderTest-" + System.nanoTime());
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

    private StatusRegistry load() {
        return new StatusLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAFireStatusAndTakesTheIdFromTheFilename() throws IOException {
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        var fire = assertInstanceOf(StatusDefinition.Fire.class, registry.find("scorch").orElseThrow());
        assertEquals("scorch", fire.id());
    }

    @Test
    void loadsARootedStatusAsImmobilizeFromTheKind() throws IOException {
        write("rooted.yml", "kind: rooted\n");

        var immobilize = assertInstanceOf(StatusDefinition.Immobilize.class,
                load().find("rooted").orElseThrow());

        assertEquals("rooted", immobilize.id());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void loadsASoakedStatusFromTheKind() throws IOException {
        write("soaked.yml", "kind: soaked\n");

        var soaked = assertInstanceOf(StatusDefinition.Soaked.class,
                load().find("soaked").orElseThrow());

        assertEquals("soaked", soaked.id());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * The key is parsed, not resolved. Resolving it would need Registry.MOB_EFFECT,
     * which would need a server, which would make this test impossible.
     */
    @Test
    void loadsAPotionStatusWithoutResolvingTheEffectType() throws IOException {
        write("sluggish.yml", """
                kind: potion
                potion_type: slowness
                """);

        var potion = assertInstanceOf(StatusDefinition.Potion.class,
                load().find("sluggish").orElseThrow());

        assertEquals("minecraft:slowness", potion.potionType().toString());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * NamespacedKey.minecraft("Slowness") would throw, and it would throw later,
     * from inside a scheduler task. fromString returns null, so it lands here.
     */
    @Test
    void capitalisedPotionTypeIsSkippedNotThrown() throws IOException {
        write("aaa_shouty.yml", """
                kind: potion
                potion_type: Slowness
                """);
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(1, registry.size(), "the valid status must still load");
        assertTrue(warningText().contains("aaa_shouty.yml"), warningText());
        assertTrue(warningText().contains("Invalid potion_type"), warningText());
    }

    @Test
    void potionWithoutTypeIsSkippedNotCrashed() throws IOException {
        write("aaa_bare.yml", "kind: potion\n");
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_bare.yml"), warningText());
        assertTrue(warningText().contains("potion_type"), warningText());
    }

    @Test
    void unknownKindIsSkippedNotCrashed() throws IOException {
        write("aaa_bogus.yml", "kind: teleport\n");
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("teleport"), warningText());
    }

    @Test
    void missingKindIsSkippedNotCrashed() throws IOException {
        write("aaa_empty.yml", "duration_ticks: 40\n");
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("kind"), warningText());
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new StatusLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The content we actually ship, parsed by the loader we actually run. */
    @Test
    void bundledScorchContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/statuses/scorch.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("scorch.yml"), in.readAllBytes());
        }

        StatusRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        assertInstanceOf(StatusDefinition.Fire.class, registry.find("scorch").orElseThrow());
    }
}
