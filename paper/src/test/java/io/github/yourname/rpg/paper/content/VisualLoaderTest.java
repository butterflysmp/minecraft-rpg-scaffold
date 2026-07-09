package io.github.yourname.rpg.paper.content;

import org.bukkit.Particle;
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
 * Same fail-soft contract as AbilityLoaderTest: log, name the file, skip it, keep
 * loading. No server: Particle is a plain enum and NamespacedKey is a plain class,
 * so both resolve here. Sound and PotionEffectType would not, which is exactly why
 * this loader never touches them.
 */
class VisualLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("VisualLoaderTest-" + System.nanoTime());
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

    private VisualRegistry load() {
        return new VisualLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    private static final String VALID = """
            steps:
              - type: particle
                particle: FLAME
                count: 40
                spread: 0.6
              - type: sound
                key: entity.blaze.shoot
                volume: 1.0
                pitch: 1.0
            """;

    /** Steps are a sequence: a bang after a burst is not the same as before it. */
    @Test
    void loadsStepsInOrderAndTakesTheIdFromTheFilename() throws IOException {
        write("solar_detonation.yml", VALID);

        VisualRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());

        VisualDefinition def = registry.find("solar_detonation").orElseThrow();
        assertEquals(2, def.steps().size());

        var particles = assertInstanceOf(VisualSpec.Particles.class, def.steps().get(0));
        assertEquals(Particle.FLAME, particles.particle());
        assertEquals(40, particles.count());
        assertEquals(0.6, particles.spread(), 1e-9);

        var sound = assertInstanceOf(VisualSpec.Sound.class, def.steps().get(1));
        assertEquals("entity.blaze.shoot", sound.key());
        assertEquals("minecraft:entity.blaze.shoot", sound.namespacedKey().toString());
        assertEquals(1.0f, sound.volume());
    }

    @Test
    void countAndSpreadAreOptional() throws IOException {
        write("bare.yml", """
                steps:
                  - type: particle
                    particle: FLAME
                """);

        var particles = (VisualSpec.Particles) load().find("bare").orElseThrow().steps().get(0);
        assertEquals(10, particles.count());
        assertEquals(0.0, particles.spread(), 1e-9);
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void unknownParticleIsSkippedNotCrashed() throws IOException {
        write("aaa_typo.yml", """
                steps:
                  - type: particle
                    particle: NOT_A_PARTICLE
                """);
        write("solar_detonation.yml", VALID);

        VisualRegistry registry = load();

        assertEquals(1, registry.size(), "the valid visual must still load");
        assertTrue(warningText().contains("aaa_typo.yml"), warningText());
        assertTrue(warningText().contains("NOT_A_PARTICLE"), warningText());
    }

    /**
     * The case a name-only check misses. Particle.valueOf("DUST") succeeds; it is
     * spawnParticle that would throw, on the first cast, in front of a player.
     */
    @Test
    void particleNeedingADataObjectIsRejectedAtLoad() throws IOException {
        write("aaa_dust.yml", """
                steps:
                  - type: particle
                    particle: DUST
                """);
        write("solar_detonation.yml", VALID);

        VisualRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_dust.yml"), warningText());
        assertTrue(warningText().contains("requires a data object"), warningText());
        assertTrue(warningText().contains(Particle.DUST.getDataType().getSimpleName()), warningText());
    }

    /** A constant name, not a key. Would have been a silent no-sound at runtime. */
    @Test
    void invalidSoundKeyIsSkippedNotCrashed() throws IOException {
        write("aaa_shouty.yml", """
                steps:
                  - type: sound
                    key: ENTITY_BLAZE_SHOOT
                """);
        write("solar_detonation.yml", VALID);

        VisualRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_shouty.yml"), warningText());
        assertTrue(warningText().contains("Invalid sound key"), warningText());
    }

    @Test
    void missingStepsIsSkippedNotCrashed() throws IOException {
        write("aaa_empty.yml", "particle: FLAME\n");
        write("solar_detonation.yml", VALID);

        VisualRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_empty.yml"), warningText());
        assertTrue(warningText().contains("steps"), warningText());
    }

    @Test
    void unknownStepTypeIsSkippedNotCrashed() throws IOException {
        write("aaa_beam.yml", """
                steps:
                  - type: beam
                    to: somewhere
                """);
        write("solar_detonation.yml", VALID);

        VisualRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("beam"), warningText());
    }

    @Test
    void allFilesBrokenStillReturnsAnEmptyRegistry() throws IOException {
        write("a.yml", "steps: []\n");
        write("b.yml", "steps:\n  - type: particle\n    particle: NOPE\n");

        VisualRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(0, registry.size());
        assertEquals(3, warnings.size(), "two file warnings plus the summary");
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new VisualLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The content we actually ship, parsed by the loader we actually run. */
    @Test
    void bundledSolarDetonationContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/visuals/solar_detonation.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("solar_detonation.yml"), in.readAllBytes());
        }

        VisualRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());

        VisualDefinition def = registry.find("solar_detonation").orElseThrow();
        assertEquals(2, def.steps().size());
        assertInstanceOf(VisualSpec.Particles.class, def.steps().get(0));
        assertInstanceOf(VisualSpec.Sound.class, def.steps().get(1));
    }
}
