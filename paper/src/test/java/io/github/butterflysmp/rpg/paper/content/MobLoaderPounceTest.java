package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobPounce;
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
import java.util.OptionalDouble;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Skreel's {@code mount:} and {@code pounce:}, and the mount's {@code ridden_speed:} (dragon-mobs step R8): every
 * number from content, every bad key named, and the bundled pair as shipped.
 */
class MobLoaderPounceTest {

    @TempDir Path dir;
    private Logger log;
    private final List<LogRecord> warnings = new ArrayList<>();

    private static final Set<String> SOUNDS = Set.of("entity.spider.ambient", "entity.spider.hurt",
            "entity.player.attack.crit");
    private static final Set<String> MATERIALS = Set.of("iron_spear");

    /** Every number distinct from every other and from the shipped file's. */
    private static final String OK = """
            base_entity: wither_skeleton
            max_health: 110
            mount: m
            pounce:
              acquire_range: 31
              band:
                near: 7.5
                far: 11
              path_speed: 1.45
              windup_ticks: 18
              period_ticks: 130
              launch_vertical: 1.1
              timeout_ticks: 44
              reach: 2.75
              damage: 57
              knockback:
                horizontal: 0.65
                vertical: 0.35
              windup:
                particle: crit
                particle_count: 13
                every_ticks: 6
                sounds:
                  - { key: entity.spider.ambient, volume: 0.9, pitch: 1.1 }
              launch_sounds:
                - { key: entity.spider.hurt, volume: 0.8, pitch: 1.2 }
              land_sounds:
                - { key: entity.player.attack.crit, volume: 0.7, pitch: 1.3 }
            """;

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("MobLoaderPounceTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private MobRegistry load(Set<String> sounds) {
        return new MobLoader(log, Set.of("void")::contains, sounds::contains, MATERIALS::contains)
                .loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    @Test
    void everyNumberOfThePounceArrivesFromContent() throws IOException {
        write("s.yml", OK);
        MobDefinition def = load(SOUNDS).find("s").orElseThrow();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals("m", def.mount());
        assertEquals(new MobPounce(31, 7.5, 11, 1.45, 18, 130, 1.1, 44, 2.75, 57, 0.65, 0.35, "crit", 13, 6,
                List.of(new BeamSound("entity.spider.ambient", 0.9, 1.1)),
                List.of(new BeamSound("entity.spider.hurt", 0.8, 1.2)),
                List.of(new BeamSound("entity.player.attack.crit", 0.7, 1.3))), def.pounce());
        assertNull(def.riddenSpeed());
    }

    @Test
    void theRiddenSpeedArrivesAndAMobWithNoneOfTheThreeHasNone() throws IOException {
        write("m.yml", "base_entity: spider\nmax_health: 150\nscale: 1.4\nridden_speed: 1.25\n");
        write("z.yml", "base_entity: zombie\nmax_health: 20\n");
        MobRegistry r = load(SOUNDS);
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1.25, r.find("m").orElseThrow().riddenSpeed());
        MobDefinition z = r.find("z").orElseThrow();
        assertNull(z.mount());
        assertNull(z.pounce());
        assertNull(z.riddenSpeed());
    }

    @Test
    void eachMissingKeyIsANamedSkippedFile() throws IOException {
        write("a.yml", OK.replace("  reach: 2.75\n", ""));
        write("b.yml", OK.replace("    far: 11\n", ""));
        write("c.yml", OK.replace("    vertical: 0.35\n", ""));
        write("d.yml", OK.replace("    every_ticks: 6\n", ""));
        assertEquals(0, load(SOUNDS).size(), warningText());
        assertTrue(warningText().contains("pounce.reach is required"), warningText());
        assertTrue(warningText().contains("pounce.band.far is required"), warningText());
        assertTrue(warningText().contains("pounce.knockback.vertical is required"), warningText());
        assertTrue(warningText().contains("pounce.windup.every_ticks is required"), warningText());
    }

    @Test
    void aPounceWithNoMountAnUnknownSoundAndABadRiddenSpeedAreRefusedByName() throws IOException {
        write("a.yml", OK.replace("mount: m\n", ""));
        write("b.yml", OK.replace("key: entity.spider.hurt", "key: entity.spider.yelp"));
        write("c.yml", "base_entity: spider\nmax_health: 150\nridden_speed: 0\n");
        assertEquals(0, load(SOUNDS).size(), warningText());
        assertTrue(warningText().contains("authors a pounce but no mount"), warningText());
        assertTrue(warningText().contains("pounce.launch_sounds key 'entity.spider.yelp'"), warningText());
        assertTrue(warningText().contains("ridden_speed must be a multiplier above 0, was 0.0"), warningText());
    }

    private static void copyBundled(Path dir, String name) throws IOException {
        try (var in = MobLoaderPounceTest.class.getResourceAsStream("/content/mobs/" + name)) {
            assertNotNull(in, name);
            Files.write(dir.resolve(name), in.readAllBytes());
        }
    }

    /**
     * The shipped pair: the seat's numbers Ben accepted on 2026-11-17 (32, 8-12, 1 s, 6 s, 55, 3, "moderate" as 0.7/0.4),
     * cfde822's where they are silent (path 1.6, vertical 1.2, timeout 40), the mount's 160 and 1.3x only while ridden.
     */
    @Test
    void theBundledSkreelAndMountCarryTheAcceptedNumbers() throws IOException {
        copyBundled(dir, "skreel.yml");
        copyBundled(dir, "withered_mount.yml");
        MobRegistry r = load(SOUNDS);
        assertTrue(warnings.isEmpty(), warningText());
        MobDefinition s = r.find("skreel").orElseThrow();
        assertEquals("withered_mount", s.mount());
        assertEquals("iron_spear", s.mainHand());
        assertEquals("void", s.element());
        assertEquals(120.0, s.maxHealth());
        assertEquals(new MobPounce(32, 8, 12, 1.6, 20, 120, 1.2, 40, 3, 55, 0.7, 0.4, "crit", 15, 5,
                List.of(new BeamSound("entity.spider.ambient", 1.0, 1.0)),
                List.of(new BeamSound("entity.spider.hurt", 1.0, 1.0)),
                List.of(new BeamSound("entity.player.attack.crit", 1.0, 1.0))), s.pounce());
        MobDefinition m = r.find("withered_mount").orElseThrow();
        assertEquals(160.0, m.maxHealth());
        assertEquals(OptionalDouble.of(1.5), m.body().scale());
        assertEquals(OptionalDouble.empty(), m.body().speedMultiplier(), "its own speed when alone: no base multiplier");
        assertEquals(1.3, m.riddenSpeed());
        assertNull(m.element(), "Ben: \"No it's no element\"");
        assertNull(m.pounce());
    }
}
