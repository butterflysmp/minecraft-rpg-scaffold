package io.github.butterflysmp.rpg.paper.content;

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
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fight schema (PLAN-dragon-fight.md steps F4 to F7), and whether the shipped file satisfies it. The headline row
 * loads the REAL {@code content/fights/} and asserts a non-empty result: finding no fight is a defect, not a quiet
 * no-op. Every refusal row checks the result is empty AND the warning names the cause.
 */
class FightLoaderTest {

    @TempDir
    Path dir;

    private final List<String> lines = new ArrayList<>();
    private final Logger log = capturing();

    /** The mobs and sounds a server would know; the refusal rows use names outside these. */
    private static final Set<String> MOBS = Set.of("dark_mage", "knell");
    private static final Set<String> SOUNDS = Set.of("item.shield.block", "block.beacon.power_select");

    private Logger capturing() {
        Logger l = Logger.getAnonymousLogger();
        l.setUseParentHandlers(false);
        l.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { lines.add(r.getLevel() + " " + r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return l;
    }

    private FightLoader loader() {
        return new FightLoader(log, MOBS::contains, SOUNDS::contains);
    }

    private static final String GOOD = """
            settings:
              border_radius_blocks: 48.0
              mage_ring_radius_blocks: 12.0
              fast_close_blocks_per_second: 3.8
              slow_close_blocks_per_second: 0.6
              kill_expansion_blocks: 1.0
              crystal_count: 10
              crystal_interval_ticks: 80
              strike_interval_ticks: 40
              first_strike_damage: 20.0
            ring_look:
              particle: dust
              color: [170, 0, 255]
              dust_size: 1.6
              point_spacing_blocks: 1.0
              height_offsets_blocks: [0.2, 1.7, 3.2, 4.7]
              row_shift_blocks: 0.25
              turn_blocks_per_second: 1.0
              redraw_period_ticks: 4
            mage:
              mob: dark_mage
              refused_sound: item.shield.block
              refused_volume: 1.0
              refused_pitch: 0.8
            crystals:
              ring_radius_blocks: 40.0
              height_above_ground_blocks: 12.0
              lit_sound: block.beacon.power_select
              lit_volume: 1.0
              lit_pitch: 1.0
            fixture:
              participant_radius_blocks: 128.0
            """;

    private Map<String, FightContent> load(String yaml) throws IOException {
        Files.writeString(dir.resolve("test.yml"), yaml, StandardCharsets.UTF_8);
        return loader().loadAll(new File(dir.toString()));
    }

    @Test
    void theShippedDragonFileLoadsWithEveryNumberAndOneLoadedLine() {
        Map<String, FightContent> fights = loader().loadAll(new File("src/main/resources/content/fights"));
        assertEquals(1, fights.size(), "the shipped directory loads one fight: " + lines);
        FightContent d = fights.get("dragon");
        assertEquals(3.8, d.settings().fastCloseBlocksPerSecond(), 0.0, "Ben's");
        assertEquals(0.6, d.settings().slowCloseBlocksPerSecond(), 0.0, "Ben's");
        assertEquals(List.of(0.2, 1.7, 3.2, 4.7), d.look().heightOffsetsBlocks(), "Ben's four rows 1.5 apart");
        assertEquals(0.25, d.look().rowShiftBlocks(), 0.0);
        assertEquals(1.0, d.look().turnBlocksPerSecond(), 0.0);
        assertEquals("dark_mage", d.mageMob());
        assertEquals(new FightContent.Cue("item.shield.block", 1.0f, 0.8f), d.refusedCue());
        assertEquals(40.0, d.crystalRingRadiusBlocks(), 0.0);
        assertEquals(12.0, d.crystalHeightBlocks(), 0.0);
        assertEquals(new FightContent.Cue("block.beacon.power_select", 1.0f, 1.0f), d.litCue());
        assertEquals(List.of("INFO Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, "
                + "e_kill 1.00, 10 crystals every 80 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) "
                + "size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7], row shift 0.25, turn 1.00/s, every 4 ticks; "
                + "mage 'dark_mage' refused item.shield.block; crystals at 40.0, 12.0 up, lit block.beacon.power_select;"
                + " participants within 128.0"), lines);
    }

    @Test
    void theGoodFixtureLoads() throws IOException {
        assertEquals(1, load(GOOD).size(), "the fixture the refusal rows edit is itself good: " + lines);
    }

    private void refusedNaming(String yaml, String... needles) throws IOException {
        Map<String, FightContent> fights = load(yaml);
        assertTrue(fights.isEmpty(), "refused: " + lines);
        assertEquals(1, lines.size(), lines.toString());
        assertTrue(lines.get(0).startsWith("WARNING Refusing fight 'test' in test.yml: "), lines.get(0));
        for (String n : needles) assertTrue(lines.get(0).contains(n), "names '" + n + "': " + lines.get(0));
    }

    @Test
    void aMissingNumberRefusesTheFileByName() throws IOException {
        refusedNaming(GOOD.replace("  kill_expansion_blocks: 1.0\n", ""), "'kill_expansion_blocks:'", "missing");
    }

    @Test
    void aNonNumberRefusesTheFileByName() throws IOException {
        refusedNaming(GOOD.replace("crystal_count: 10", "crystal_count: ten"), "'crystal_count:'", "'ten'");
    }

    @Test
    void anUnsupportedParticleIsRefused() throws IOException {
        refusedNaming(GOOD.replace("particle: dust", "particle: flame"), "'flame' is not supported");
    }

    @Test
    void aBadColourIsRefused() throws IOException {
        refusedNaming(GOOD.replace("color: [170, 0, 255]", "color: [170, 0]"), "'color:'");
    }

    @Test
    void aCoreRangeRefusalNamesItsReason() throws IOException {
        refusedNaming(GOOD.replace("slow_close_blocks_per_second: 0.6", "slow_close_blocks_per_second: 6.0"),
                "must not exceed fastCloseBlocksPerSecond");
    }

    @Test
    void aMissingRowShiftIsRefused() throws IOException {
        refusedNaming(GOOD.replace("  row_shift_blocks: 0.25\n", ""), "'row_shift_blocks:'");
    }

    @Test
    void anUnknownMageMobIsRefused() throws IOException {
        refusedNaming(GOOD.replace("mob: dark_mage", "mob: nobody"), "'nobody' names no loaded mob file");
    }

    @Test
    void anUnknownSoundIsRefused() throws IOException {
        refusedNaming(GOOD.replace("lit_sound: block.beacon.power_select", "lit_sound: no.such.sound"),
                "'no.such.sound' is not a known sound");
    }
}
