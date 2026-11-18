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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fight schema (PLAN-dragon-fight.md step F4), and whether the shipped file satisfies it. The headline row loads the
 * REAL {@code content/fights/} and asserts a non-empty result: finding no fight is a defect, not a quiet no-op. Every
 * refusal row checks the result is empty AND the warning names the cause.
 */
class FightLoaderTest {

    @TempDir
    Path dir;

    private final List<String> lines = new ArrayList<>();
    private final Logger log = capturing();

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

    private static final String GOOD = """
            settings:
              border_radius_blocks: 48.0
              mage_ring_radius_blocks: 12.0
              fast_close_blocks_per_second: 5.0
              slow_close_blocks_per_second: 0.4
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
              height_offsets_blocks: [0.2, 1.2, 2.2]
              redraw_period_ticks: 4
            fixture:
              participant_radius_blocks: 128.0
            """;

    private Map<String, FightContent> load(String yaml) throws IOException {
        Files.writeString(dir.resolve("test.yml"), yaml, StandardCharsets.UTF_8);
        return new FightLoader(log).loadAll(new File(dir.toString()));
    }

    @Test
    void theShippedDragonFileLoadsWithEveryNumberAndOneLoadedLine() {
        Map<String, FightContent> fights = new FightLoader(log).loadAll(new File("src/main/resources/content/fights"));
        assertEquals(1, fights.size(), "the shipped directory loads one fight: " + lines);
        FightContent d = fights.get("dragon");
        assertEquals(48.0, d.settings().borderRadiusBlocks(), 0.0);
        assertEquals(12.0, d.settings().mageRingRadiusBlocks(), 0.0);
        assertEquals(5.0, d.settings().fastCloseBlocksPerSecond(), 0.0);
        assertEquals(0.4, d.settings().slowCloseBlocksPerSecond(), 0.0);
        assertEquals(1.0, d.settings().killExpansionBlocks(), 0.0);
        assertEquals(10, d.settings().crystalCount());
        assertEquals(80L, d.settings().crystalIntervalTicks());
        assertEquals(40L, d.settings().strikeIntervalTicks());
        assertEquals(20.0, d.settings().firstStrikeDamage(), 0.0);
        assertEquals(List.of(170, 0, 255), List.of(d.look().red(), d.look().green(), d.look().blue()));
        assertEquals(1.6, d.look().dustSize(), 0.0);
        assertEquals(List.of(0.2, 1.2, 2.2), d.look().heightOffsetsBlocks());
        assertEquals(4, d.look().redrawPeriodTicks());
        assertEquals(128.0, d.participantRadiusBlocks(), 0.0);
        assertEquals(List.of("INFO Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 5.00, v_slow 0.40, "
                + "e_kill 1.00, 10 crystals every 80 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) "
                + "size 1.60 every 1.00 blocks at [0.2, 1.2, 2.2] every 4 ticks; participants within 128.0"), lines);
    }

    @Test
    void aMissingNumberRefusesTheFileByName() throws IOException {
        Map<String, FightContent> fights = load(GOOD.replace("  kill_expansion_blocks: 1.0\n", ""));
        assertTrue(fights.isEmpty());
        assertEquals(1, lines.size(), lines.toString());
        assertTrue(lines.get(0).startsWith("WARNING Refusing fight 'test' in test.yml: "), lines.get(0));
        assertTrue(lines.get(0).contains("'kill_expansion_blocks:'") && lines.get(0).contains("missing"), lines.get(0));
    }

    @Test
    void aNonNumberRefusesTheFileByName() throws IOException {
        Map<String, FightContent> fights = load(GOOD.replace("crystal_count: 10", "crystal_count: ten"));
        assertTrue(fights.isEmpty());
        assertTrue(lines.get(0).contains("'crystal_count:'") && lines.get(0).contains("'ten'"), lines.get(0));
    }

    @Test
    void anUnsupportedParticleIsRefused() throws IOException {
        Map<String, FightContent> fights = load(GOOD.replace("particle: dust", "particle: flame"));
        assertTrue(fights.isEmpty());
        assertTrue(lines.get(0).contains("'flame' is not supported"), lines.get(0));
    }

    @Test
    void aBadColourIsRefused() throws IOException {
        Map<String, FightContent> fights = load(GOOD.replace("color: [170, 0, 255]", "color: [170, 0]"));
        assertTrue(fights.isEmpty());
        assertTrue(lines.get(0).contains("'color:'"), lines.get(0));
    }

    @Test
    void aCoreRangeRefusalNamesItsReason() throws IOException {
        // v_slow above v_fast: the return's slow part would outrun the cap (FightSettings).
        Map<String, FightContent> fights = load(GOOD.replace("slow_close_blocks_per_second: 0.4",
                "slow_close_blocks_per_second: 6.0"));
        assertTrue(fights.isEmpty());
        assertTrue(lines.get(0).contains("must not exceed fastCloseBlocksPerSecond"), lines.get(0));
    }

    @Test
    void theGoodFixtureLoads() throws IOException {
        Map<String, FightContent> fights = load(GOOD);
        assertEquals(1, fights.size(), "the fixture the refusal rows edit is itself good: " + lines);
    }
}
