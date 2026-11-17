package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobBlink;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobShield;
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
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Phalanx's blink and shield blocks (dragon-mobs step R7b): every number from content, every bad key named. */
class MobLoaderBlinkTest {

    @TempDir Path dir;
    private Logger log;
    private final List<LogRecord> warnings = new ArrayList<>();

    private static final Set<String> SOUNDS = Set.of("entity.enderman.teleport", "item.shield.break");
    private static final Set<String> MATERIALS = Set.of("bow", "shield", "amethyst_block");

    /** Every number distinct from every other and from the shipped file's (5, 6, 16, 8, 3, 12, 1.0). */
    private static final String OK = """
            base_entity: wither_skeleton
            max_health: 240
            blink:
              trigger_range: 4.5
              near: 7
              far: 13
              attempts: 9
              level_tolerance: 2
              particle: squid_ink
              particle_count: 11
              sounds:
                - { key: entity.enderman.teleport, volume: 0.75, pitch: 1.25 }
            shield:
              item: shield
              particle: crit
              particle_count: 14
              sounds:
                - { key: item.shield.break, volume: 0.5, pitch: 0.8 }
            """;

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("MobLoaderBlinkTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private MobRegistry load() {
        return new MobLoader(log, Set.of("void")::contains, SOUNDS::contains, MATERIALS::contains)
                .loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    @Test
    void everyNumberOfTheBlinkAndTheShieldArrivesFromContent() throws IOException {
        write("p.yml", OK);
        MobDefinition def = load().find("p").orElseThrow();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(new MobBlink(4.5, 7, 13, 9, 2, List.of(new BeamSound("entity.enderman.teleport", 0.75, 1.25)),
                "squid_ink", 11), def.blink());
        assertEquals(new MobShield("shield", List.of(new BeamSound("item.shield.break", 0.5, 0.8)), "crit", 14),
                def.shield());
    }

    @Test
    void aMobWithNeitherBlockHasNeither() throws IOException {
        write("z.yml", "base_entity: zombie\nmax_health: 20\n");
        MobDefinition def = load().find("z").orElseThrow();
        assertNull(def.blink());
        assertNull(def.shield());
    }

    @Test
    void eachMissingKeyIsANamedSkippedFile() throws IOException {
        write("a.yml", OK.replace("  attempts: 9\n", ""));
        write("b.yml", OK.replace("  item: shield\n", ""));
        assertEquals(0, load().size(), warningText());
        assertTrue(warningText().contains("blink.attempts is required"), warningText());
        assertTrue(warningText().contains("shield.item is required"), warningText());
    }

    @Test
    void anUnknownItemSoundOrParticleOrOneThatNeedsDataIsRefusedByName() throws IOException {
        write("a.yml", OK.replace("item: shield", "item: buckler"));
        write("b.yml", OK.replace("key: item.shield.break", "key: item.shield.shatter"));
        write("c.yml", OK.replace("particle: squid_ink", "particle: inky"));
        write("d.yml", OK.replace("particle: crit", "particle: block"));
        assertEquals(0, load().size(), warningText());
        assertTrue(warningText().contains("shield.item 'buckler' names no item"), warningText());
        assertTrue(warningText().contains("shield.sounds key 'item.shield.shatter'"), warningText());
        assertTrue(warningText().contains("blink.particle 'inky' names no particle"), warningText());
        assertTrue(warningText().contains("shield.particle 'block' needs data"), warningText());
    }

    @Test
    void farUnderNearIsRefusedByName() throws IOException {
        write("a.yml", OK.replace("far: 13", "far: 5"));
        assertEquals(0, load().size(), warningText());
        assertTrue(warningText().contains("blink.far (5.0) must be at least blink.near (7.0)"), warningText());
    }

    /** The shipped Phalanx: cfde822's blink (X4-X6, Ben left unchanged), the Knell's tolerance, its shield. */
    @Test
    void theBundledPhalanxBlinksAndShieldsWithTheRecordsNumbers() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/phalanx.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("phalanx.yml"), in.readAllBytes());
        }
        MobLoader l = new MobLoader(log, Set.of("void")::contains,
                Set.of("entity.wither.ambient", "entity.shulker_bullet.hurt", "entity.enderman.teleport",
                        "item.shield.break")::contains, MATERIALS::contains);
        MobDefinition def = l.loadAll(new File(dir.toString())).find("phalanx").orElseThrow();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(new MobBlink(5, 6, 16, 8, 3, List.of(new BeamSound("entity.enderman.teleport", 1.0, 1.0)),
                "squid_ink", 12), def.blink());
        assertEquals(new MobShield("shield", List.of(new BeamSound("item.shield.break", 1.0, 1.0)), "crit", 16),
                def.shield());
    }
}
