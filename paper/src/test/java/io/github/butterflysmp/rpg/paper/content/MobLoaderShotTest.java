package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobShot;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHALANX'S SHOT AS CONTENT (dragon-mobs step R7a): every number of the shot is read from the mob file, each key
 * required, each colour and key checked, and the bundled {@code phalanx.yml} carries Ben's 120 and 240 and the old
 * game's starting numbers.
 */
class MobLoaderShotTest {

    @TempDir Path dir;
    private Logger log;
    private final List<LogRecord> warnings = new ArrayList<>();

    private static final Set<String> SOUNDS = Set.of("entity.wither.ambient", "entity.shulker_bullet.hurt");
    private static final Set<String> MATERIALS = Set.of("bow", "amethyst_block");

    /** Every number differs from every other and from the shipped file's, so a transposition has nowhere to hide. */
    private static final String SHOT_OK = """
            shot:
              period_ticks: 77
              acquire_range: 21
              reacquire_range: 26
              speed: 0.35
              homing: 0.15
              life_ticks: 99
              max_distance: 19
              tolerance: 0.8
              damage: 13
              look:
                block: amethyst_block
                size: 0.45
                glow: [1, 2, 3]
                trail:
                  rgb: [4, 5, 6]
                  size: 1.25
                flash:
                  rgb: [7, 8, 9]
                  size: 1.75
                  every_ticks: 3
              sounds:
                fire:
                  - { key: entity.wither.ambient, volume: 0.25, pitch: 1.25 }
                destroyed:
                  - { key: entity.shulker_bullet.hurt, volume: 0.5, pitch: 0.75 }
            """;

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("MobLoaderShotTest-" + System.nanoTime());
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
    void everyNumberOfTheShotArrivesFromContent() throws IOException {
        write("p.yml", "base_entity: wither_skeleton\nmax_health: 240\nmain_hand: bow\n" + SHOT_OK);
        MobDefinition def = load().find("p").orElseThrow();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(new MobShot(77, 21, 26, 0.35, 0.15, 99, 19, 0.8, 13,
                new MobShot.ShotLook("amethyst_block", 0.45, 0x010203, 0x040506, 1.25, 0x070809, 1.75, 3),
                List.of(new BeamSound("entity.wither.ambient", 0.25, 1.25)),
                List.of(new BeamSound("entity.shulker_bullet.hurt", 0.5, 0.75))), def.shot());
        assertEquals("bow", def.mainHand());
    }

    @Test
    void aMobWithNoShotHasNoneAndNoMainHand() throws IOException {
        write("z.yml", "base_entity: zombie\nmax_health: 20\n");
        MobDefinition def = load().find("z").orElseThrow();
        assertEquals(null, def.shot());
        assertEquals(null, def.mainHand());
    }

    @Test
    void eachMissingKeyIsANamedSkippedFile() throws IOException {
        List<String> leaves = List.of("period_ticks", "acquire_range", "reacquire_range", "speed", "homing",
                "life_ticks", "max_distance", "tolerance", "damage");
        for (String leaf : leaves) {
            String yaml = SHOT_OK.replaceAll("(?m)^  " + leaf + ":.*\\n", "");
            assertTrue(!yaml.equals(SHOT_OK), "the fixture for " + leaf + " must actually remove it");
            write("no_" + leaf + ".yml", "base_entity: wither_skeleton\nmax_health: 240\n" + yaml);
        }
        assertEquals(0, load().size(), warningText());
        for (String leaf : leaves) {
            assertTrue(warningText().contains("shot." + leaf + " is required"), leaf + ": " + warningText());
        }
    }

    @Test
    void anUnknownBlockSoundOrItemOrABadColourIsRefusedByName() throws IOException {
        write("block.yml", "base_entity: wither_skeleton\nmax_health: 1\n" + SHOT_OK.replace("block: amethyst_block", "block: amethist_block"));
        write("sound.yml", "base_entity: wither_skeleton\nmax_health: 1\n" + SHOT_OK.replace("key: entity.wither.ambient", "key: entity.wither.ambiant"));
        write("item.yml", "base_entity: wither_skeleton\nmax_health: 1\nmain_hand: bowe\n");
        write("colour.yml", "base_entity: wither_skeleton\nmax_health: 1\n" + SHOT_OK.replace("glow: [1, 2, 3]", "glow: [1, 2, 300]"));
        assertEquals(0, load().size(), warningText());
        String w = warningText();
        assertTrue(w.contains("shot.look.block 'amethist_block' names no block"), w);
        assertTrue(w.contains("shot.sounds.fire key 'entity.wither.ambiant'"), w);
        assertTrue(w.contains("main_hand 'bowe' names no item"), w);
        assertTrue(w.contains("shot.look.glow [1, 2, 300] has a part outside 0..255"), w);
    }

    /**
     * THE PHALANX WE SHIP (step R7a): Ben's 120 and 240; the homing 0.0513 a tick, the seat's conversion of the old game's 10% per 2-tick step; the old game's 30, 35, 0.2, 35, 1.2 and 40, starting
     * numbers he left unchanged; the seat's chosen look; the bow.
     */
    @Test
    void theBundledPhalanxShootsBensNumbers() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/phalanx.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("phalanx.yml"), in.readAllBytes());
        }
        MobDefinition def = load().find("phalanx").orElseThrow();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(new MobShot(120, 30, 35, 0.2, 0.0513, 240, 35, 1.2, 40,
                new MobShot.ShotLook("amethyst_block", 0.4, 0xC88CFF, 0xC88CFF, 1.5, 0xFF69B4, 2.0, 4),
                List.of(new BeamSound("entity.wither.ambient", 0.5, 1.5)),
                List.of(new BeamSound("entity.shulker_bullet.hurt", 1.0, 1.0))), def.shot());
        assertEquals("bow", def.mainHand());
        assertEquals(240.0, def.maxHealth(), 0.0);
    }
}
