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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final Set<String> MOBS = Set.of("dark_mage", "knell", "ender_dragon");
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
              alternate_turns: true
              redraw_period_ticks: 4
            mage:
              mob: dark_mage
              refused_sound: item.shield.block
              refused_volume: 1.0
              refused_pitch: 0.8
              landing_lightning: true
            crystals:
              ring_radius_blocks: 40.0
              height_above_ground_blocks: 12.0
              lit_sound: block.beacon.power_select
              lit_volume: 1.0
              lit_pitch: 1.0
            arena:
              centre_x: 0
              centre_z: 0
              floor_y: 64
              disc_radius_blocks: 56
              disc_thickness_blocks: 3
              pillar_count: 10
              pillar_ring_radius_blocks: 40.0
              pillar_width_blocks: 3
              pillar_top_above_floor_blocks: 12
              time_of_day_ticks: 18000
              entry_offset_blocks: [0.0, 0.0]
              environment: end
              all_down_seconds: 7
              source: generated
            fixture:
              participant_radius_blocks: 128.0
            dragon:
              mob: ender_dragon
              mage_thresholds: [0.80, 0.60, 0.40, 0.20]
              win_return_ticks: 200
              boss_bar_color: purple
              bite: 81.0
              wing: 41.0
              breath_per_second: 31.0
              contact_hit_interval_ticks: 21
            revive:
              revive_radius_blocks: 1.6
              revive_seconds: 4
              revive_health_fraction: 0.6
              label: "test {player} {seconds}"
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
        assertEquals(List.of(0.2, 1.7, 3.2, 4.7, 6.2, 7.7), d.look().heightOffsetsBlocks(),
                "six rows 1.5 apart (Ben: \"yes, maybe another row or 2\")");
        assertEquals(0.25, d.look().rowShiftBlocks(), 0.0);
        assertEquals(1.0, d.look().turnBlocksPerSecond(), 0.0);
        assertTrue(d.look().alternateTurns(), "Ben: \"maybe alternate twists on every other row\"");
        assertEquals(240, d.settings().crystalIntervalTicks(), "Ben: \"no make it 12 seconds each crystal\"");
        assertEquals(10, d.settings().crystalCount());
        assertEquals(new io.github.butterflysmp.rpg.core.fight.ArenaShape(0, 0, 64, 56, 3, 10, 40.0, 3, 12),
                d.arena().shape(), "the brief's generated stand-in arena (SEAT PLACEHOLDERS)");
        assertEquals(18000L, d.arena().timeOfDayTicks(), "midnight (SEAT PLACEHOLDER: the sky)");
        assertEquals(0.0, d.arena().entryDxBlocks(), 0.0);
        assertEquals(0.0, d.arena().entryDzBlocks(), 0.0);
        assertEquals("dark_mage", d.mageMob());
        assertEquals(new FightContent.Cue("item.shield.block", 1.0f, 0.8f), d.refusedCue());
        assertEquals(40.0, d.crystalRingRadiusBlocks(), 0.0);
        assertEquals(12.0, d.crystalHeightBlocks(), 0.0);
        assertEquals(new FightContent.Cue("block.beacon.power_select", 1.0f, 1.0f), d.litCue());
        assertEquals(List.of("INFO Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, "
                + "e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) "
                + "size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s "
                + "alternating, every 4 ticks; "
                + "mage 'dark_mage' refused item.shield.block, landing lightning on; crystals at 40.0, 12.0 up, lit block.beacon.power_select;"
                + " participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to"
                + " +12, time 18000, entry (0.0, 0.0), environment end, all down 5 s; dragon 'ender_dragon' Mages below"
                + " [0.8, 0.6, 0.4, 0.2], win"
                + " return 200 ticks, bar purple, bite 80.0, wing 40.0, breath 30.0/s, one contact hit per 20 ticks;"
                + " revive within 1.50 blocks for 3 s at 0.50 of max, no limit,"
                + " label '{player} is down: sneak on them for {seconds} seconds to revive'"), lines);
        assertEquals(new FightContent.Dragon("ender_dragon", List.of(0.80, 0.60, 0.40, 0.20), 200, "purple",
                new io.github.butterflysmp.rpg.core.fight.DragonAttacks(80.0, 40.0, 30.0, 20)),
                d.dragon(), "Ben: \"let's do 80 60 40 and 20 percent\"; the win's 200 and purple are SEAT PLACEHOLDERS;"
                        + " bite 80, wing 40, breath 30 a second: \"Yes those numbers are fine to start with\"");
        assertTrue(d.mageLandingLightning(), "Ben: \"Add a lightning strike to the mage landing\"");
        assertEquals(FightContent.Environment.END, d.arena().environment(), "Ben: \"yes let's try it\" (F9c)");
    }

    @Test
    void aMissingDragonSectionIsRefusedByName() throws IOException {
        refusedNaming(GOOD.substring(0, GOOD.indexOf("dragon:\n")), "no 'dragon:' section");
    }

    @Test
    void anUnknownDragonMobIsRefused() throws IOException {
        refusedNaming(GOOD.replace("mob: ender_dragon", "mob: wyvern"), "dragon.mob 'wyvern' names no loaded mob file");
    }

    @Test
    void thresholdsThatRiseAreRefusedWithTheQueuesOwnReason() throws IOException {
        refusedNaming(GOOD.replace("[0.80, 0.60, 0.40, 0.20]", "[0.60, 0.80]"), "dragon.mage_thresholds",
                "must fall strictly");
    }

    @Test
    void aThresholdOfOneIsRefused() throws IOException {
        refusedNaming(GOOD.replace("[0.80, 0.60, 0.40, 0.20]", "[1.0, 0.5]"), "dragon.mage_thresholds",
                "strictly between 0 and 1");
    }

    @Test
    void aNegativeWinDelayIsRefused() throws IOException {
        refusedNaming(GOOD.replace("win_return_ticks: 200", "win_return_ticks: -1"), "win_return_ticks must be 0 or more");
    }

    @Test
    void anUnknownBossBarColourIsRefused() throws IOException {
        refusedNaming(GOOD.replace("boss_bar_color: purple", "boss_bar_color: mauve"), "boss_bar_color", "'mauve'");
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
    void aMissingArenaNumberIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("  disc_radius_blocks: 56\n", ""), "'disc_radius_blocks:'", "missing");
    }

    /** Crystal spot n stands on pillar n (the brief): a count that disagrees has a crystal with nowhere to stand. */
    @Test
    void aCrystalCountThatIsNotThePillarCountIsRefused() throws IOException {
        refusedNaming(GOOD.replace("pillar_count: 10", "pillar_count: 8"), "crystal_count 10", "pillar_count 8");
    }

    @Test
    void anArenaWhosePillarsHangOffTheDiscIsRefusedWithTheShapesReason() throws IOException {
        refusedNaming(GOOD.replace("disc_radius_blocks: 56", "disc_radius_blocks: 40"), "hangs off a disc");
    }

    @Test
    void aMissingAlternateTurnsIsRefused() throws IOException {
        refusedNaming(GOOD.replace("  alternate_turns: true\n", ""), "'alternate_turns:'", "missing");
    }

    @Test
    void aNonBooleanAlternateTurnsIsRefused() throws IOException {
        refusedNaming(GOOD.replace("alternate_turns: true", "alternate_turns: sometimes"), "'alternate_turns:'",
                "'sometimes'");
    }

    /** A round trip needs BOTH answers: a true-only row passes on a loader that hardcoded true. */
    @Test
    void alternateTurnsArrivesAsWrittenEitherWay() throws IOException {
        assertTrue(load(GOOD).get("test").look().alternateTurns());
        lines.clear();
        assertFalse(load(GOOD.replace("alternate_turns: true", "alternate_turns: false")).get("test").look()
                .alternateTurns(), lines.toString());
    }

    /** F9b, section 4: each attack number arrives as written, each distinct, so a swap between two cannot pass. */
    @Test
    void theDragonsAttackNumbersArriveAsWritten() throws IOException {
        var a = load(GOOD).get("test").dragon().attacks();
        assertEquals(81.0, a.bite(), 0.0);
        assertEquals(41.0, a.wing(), 0.0);
        assertEquals(31.0, a.breathPerSecond(), 0.0);
        assertEquals(21, a.contactHitIntervalTicks(), "F9b fix A: the window, as written");
    }

    @Test
    void aMissingOrZeroContactWindowIsRefused() throws IOException {
        refusedNaming(GOOD.replace("  contact_hit_interval_ticks: 21\n", ""), "'contact_hit_interval_ticks:'");
        lines.clear();
        refusedNaming(GOOD.replace("contact_hit_interval_ticks: 21", "contact_hit_interval_ticks: 0"),
                "contact_hit_interval_ticks must be at least 1");
    }

    @Test
    void aMissingAttackNumberIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("  wing: 41.0\n", ""), "'wing:'");
    }

    @Test
    void aNegativeAttackNumberIsRefusedWithCoresReason() throws IOException {
        refusedNaming(GOOD.replace("bite: 81.0", "bite: -1.0"), "dragon attacks", "bite", "0 or more");
    }

    /** F9c, section 3 a: the fight world's environment arrives as written, both ways (a round trip). */
    @Test
    void theEnvironmentArrivesAsWrittenEitherWay() throws IOException {
        assertEquals(FightContent.Environment.END, load(GOOD).get("test").arena().environment());
        lines.clear();
        assertEquals(FightContent.Environment.NORMAL, load(GOOD.replace("environment: end", "environment: normal"))
                .get("test").arena().environment(), lines.toString());
    }

    /** Item 2 (C3): the arena's source arrives as written either way, and a template's file name with it. */
    @Test
    void theArenaSourceArrivesAsWrittenEitherWay() throws IOException {
        FightContent.Arena g = load(GOOD).get("test").arena();
        assertEquals(FightContent.Source.GENERATED, g.source(), lines.toString());
        assertEquals(null, g.templateFile());
        lines.clear();
        FightContent.Arena t = load(GOOD.replace("  source: generated\n", "  source: template\n  template_file: ring_one\n"))
                .get("test").arena();
        assertEquals(FightContent.Source.TEMPLATE, t.source(), lines.toString());
        assertEquals("ring_one", t.templateFile());
        assertTrue(lines.stream().anyMatch(l -> l.contains("environment end, all down 7 s, template 'ring_one'; dragon")),
                "a template is named in the loaded line: " + lines);
    }

    @Test
    void aBadArenaSourceIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("source: generated", "source: vanilla"), "arena.source", "'vanilla'");
        lines.clear();
        refusedNaming(GOOD.replace("  source: generated\n", ""), "arena.source", "missing");
        lines.clear();
        refusedNaming(GOOD.replace("source: generated", "source: template"), "template_file", "missing");
        lines.clear();
        refusedNaming(GOOD.replace("  source: generated\n", "  source: template\n  template_file: ../evil\n"),
                "template_file", "'../evil'");
    }

    /** F9e: the revive's numbers arrive as written (each differs from the shipped one), and no limit is the default. */
    @Test
    void theReviveArrivesAsWrittenAndUnlimitedByDefault() throws IOException {
        FightContent.Revive r = load(GOOD).get("test").revive();
        assertEquals(1.6, r.radiusBlocks(), 1e-9, lines.toString());
        assertEquals(4, r.seconds());
        assertEquals(0.6, r.healthFraction(), 1e-9);
        assertEquals(java.util.OptionalInt.empty(), r.maxRevives(), "absent is unlimited");
        assertEquals("test {player} {seconds}", r.label());
        lines.clear();
        assertEquals(java.util.OptionalInt.of(2), load(GOOD.replace("  revive_seconds: 4\n",
                "  revive_seconds: 4\n  max_revives: 2\n")).get("test").revive().maxRevives(), lines.toString());
    }

    @Test
    void aBadReviveValueIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("revive_seconds: 4", "revive_seconds: 0"), "revive_seconds", "1 or more");
        lines.clear();
        refusedNaming(GOOD.replace("revive_health_fraction: 0.6", "revive_health_fraction: 1.5"),
                "revive_health_fraction", "at most 1");
        lines.clear();
        refusedNaming(GOOD.replace("  revive_seconds: 4\n", "  revive_seconds: 4\n  max_revives: -1\n"),
                "max_revives", "0 or more");
        lines.clear();
        refusedNaming(GOOD.replace("  label: \"test {player} {seconds}\"\n", ""), "label", "missing");
    }

    /** F9d c: how long ALL DOWN waits before everyone goes home arrives as written (7 here, 5 shipped). */
    @Test
    void theAllDownWaitArrivesAsWritten() throws IOException {
        assertEquals(7, load(GOOD).get("test").arena().allDownSeconds(), lines.toString());
        lines.clear();
        assertEquals(0, load(GOOD.replace("all_down_seconds: 7", "all_down_seconds: 0")).get("test").arena()
                .allDownSeconds(), "0 is allowed: home on the next tick");
    }

    @Test
    void aMissingOrNegativeAllDownWaitIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("  all_down_seconds: 7\n", ""), "all_down_seconds", "missing");
        lines.clear();
        refusedNaming(GOOD.replace("all_down_seconds: 7", "all_down_seconds: -1"), "all_down_seconds", "0 or more");
    }

    @Test
    void anUnknownOrMissingEnvironmentIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("environment: end", "environment: nether"), "arena.environment", "'nether'");
        lines.clear();
        refusedNaming(GOOD.replace("  environment: end\n", ""), "arena.environment", "missing");
    }

    /** F9b, section 6: the landing's lightning is a content switch, and both answers arrive (a round trip). */
    @Test
    void theLandingLightningSwitchArrivesAsWrittenEitherWay() throws IOException {
        assertTrue(load(GOOD).get("test").mageLandingLightning());
        lines.clear();
        assertFalse(load(GOOD.replace("landing_lightning: true", "landing_lightning: false")).get("test")
                .mageLandingLightning(), lines.toString());
    }

    @Test
    void aMissingLandingLightningSwitchIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("  landing_lightning: true\n", ""), "'landing_lightning:'", "missing");
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
