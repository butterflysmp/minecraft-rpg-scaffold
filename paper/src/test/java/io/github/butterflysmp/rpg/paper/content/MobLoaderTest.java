package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobBody;
import io.github.butterflysmp.rpg.core.mob.MobSplit;
import io.github.butterflysmp.rpg.core.mob.MobGrowth;
import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.StagedDistance;
import io.github.butterflysmp.rpg.core.mob.StageBody;
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

    /** The element ids the loader may accept; production passes the loaded ElementRegistry's. */
    private static final java.util.Set<String> ELEMENTS = java.util.Set.of("void", "fire", "kinetic");

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
        return new MobLoader(log, ELEMENTS::contains, SOUNDS::contains).loadAll(new File(dir.toString()));
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
        var mobs = new MobLoader(log, ELEMENTS::contains, SOUNDS::contains).loadAll(new File(dir.toFile(), "does_not_exist"));
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
        // Bounded, not pinned: 360 is balance, and a boss HP retune must not fail the build. What
        // matters is that the Knell declares CUSTOM health at all -- that is what makes it one of
        // ours rather than an ordinary wither skeleton.
        //
        // Measured: unlike the attack_damage and cost bounds in WeaponLoaderTest, this one is NOT
        // the primary guard. MobLoader already rejects max_health <= 0 ("max_health must be > 0"),
        // so setting it to 0 is caught by warnings.isEmpty() above before reaching here. Kept as a
        // deliberate restatement of the invariant at the point it is read, not as load-bearing cover.
        assertTrue(knell.maxHealth() > 0, "the Knell declares custom health");
    }

    // ---- dragon-mobs step R1: body fields, element, stages (PLAN-dragon-mobs.md) ----

    /** A file with no body fields loads exactly as before: a vanilla body, no element, no stages. */
    @Test
    void aFileWithNoBodyFieldsLoadsAsBefore() throws IOException {
        write("wraith.yml", "base_entity: zombie\nmax_health: 50\n");

        MobDefinition wraith = load().find("wraith").orElseThrow();

        assertEquals(MobBody.VANILLA, wraith.body());
        assertNull(wraith.element());
        assertNull(wraith.stages());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** Both directions of the round trip: an authored value ARRIVES, and an absent one stays absent. */
    @Test
    void scaleSpeedAndElementArrive() throws IOException {
        write("knell.yml", """
                base_entity: wither_skeleton
                max_health: 360
                scale: 1.6
                speed: 0.6
                element: void
                """);
        write("phalanx.yml", "base_entity: wither_skeleton\nmax_health: 240\nspeed: 0.65\n");

        MobRegistry mobs = load();

        MobDefinition knell = mobs.find("knell").orElseThrow();
        assertEquals(OptionalDouble.of(1.6), knell.body().scale());
        assertEquals(OptionalDouble.of(0.6), knell.body().speedMultiplier());
        assertEquals("void", knell.element());
        MobDefinition phalanx = mobs.find("phalanx").orElseThrow();
        assertEquals(OptionalDouble.empty(), phalanx.body().scale(), "no scale authored, none arrives");
        assertEquals(OptionalDouble.of(0.65), phalanx.body().speedMultiplier());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** An element that names nothing is REFUSED at load, the file and the id both named, and the rest load. */
    @Test
    void anUnknownElementIsRefusedNamingTheFileAndTheId() throws IOException {
        write("knell.yml", "base_entity: wither_skeleton\nmax_health: 360\nelement: viod\n");
        write("wraith.yml", "base_entity: zombie\nmax_health: 50\n");

        MobRegistry mobs = load();

        assertTrue(mobs.find("knell").isEmpty(), "a mob naming an unknown element does not load");
        assertTrue(mobs.find("wraith").isPresent(), "the others still load");
        assertTrue(warningText().contains("knell.yml"), warningText());
        assertTrue(warningText().contains("'viod'"), warningText());
    }

    @Test
    void stagesLoadWithTheFirstStagesHealthAsTheMax() throws IOException {
        write("phage.yml", """
                base_entity: creeper
                element: void
                stages:
                  count: 5
                  first: { height: 1.0, speed: 1.5, max_health: 80 }
                  last:  { height: 3.0, speed: 0.5, max_health: 160 }
                """);

        MobDefinition phage = load().find("phage").orElseThrow();

        assertEquals(5, phage.stages().count());
        assertEquals(new StageBody(1.0, 1.5, 80), phage.stages().first());
        assertEquals(new StageBody(3.0, 0.5, 160), phage.stages().last());
        assertEquals(80, phage.maxHealth(), 0.0, "a staged mob's max_health is its stage-1 health");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** A staged file must not also author the fields its stages own: two sources for one number is refused. */
    @Test
    void aStagedFileThatAlsoAuthorsTopLevelBodyFieldsIsRefused() throws IOException {
        write("phage.yml", """
                base_entity: creeper
                max_health: 80
                stages:
                  count: 5
                  first: { height: 1.0, speed: 1.5, max_health: 80 }
                  last:  { height: 3.0, speed: 0.5, max_health: 160 }
                """);
        write("phage2.yml", """
                base_entity: creeper
                scale: 2.0
                stages:
                  count: 5
                  first: { height: 1.0, speed: 1.5, max_health: 80 }
                  last:  { height: 3.0, speed: 0.5, max_health: 160 }
                """);

        MobRegistry mobs = load();

        assertEquals(0, mobs.size());
        assertTrue(warningText().contains("phage.yml"), warningText());
        assertTrue(warningText().contains("phage2.yml"), warningText());
    }

    // ---- dragon-mobs step R2: the split ----

    private static final String STAGES = """
            stages:
              count: 5
              first: { height: 1.0, speed: 1.5, max_health: 80 }
              last:  { height: 3.0, speed: 0.5, max_health: 160 }
            """;

    /** The split's three numbers ARRIVE from content, and a file without the block has none. */
    @Test
    void theSplitBlockArrives() throws IOException {
        write("phage.yml", "base_entity: creeper\n" + STAGES
                + "split:\n  at: last\n  count: 3\n  child_stage: 2\n  spread: 0.75\n");
        write("plain.yml", "base_entity: creeper\n" + STAGES);

        MobRegistry mobs = load();

        assertEquals(new MobSplit(3, 2, 0.75), mobs.find("phage").orElseThrow().split());
        assertNull(mobs.find("plain").orElseThrow().split(), "no split block, no split");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** A split on an unstaged mob, an `at` other than last, and a missing key are each a named, skipped file. */
    @Test
    void aMalformedSplitIsRefused() throws IOException {
        write("nostages.yml", "base_entity: creeper\nmax_health: 80\n"
                + "split:\n  at: last\n  count: 2\n  child_stage: 1\n  spread: 0.5\n");
        write("first.yml", "base_entity: creeper\n" + STAGES
                + "split:\n  at: first\n  count: 2\n  child_stage: 1\n  spread: 0.5\n");
        write("nospread.yml", "base_entity: creeper\n" + STAGES + "split:\n  at: last\n  count: 2\n  child_stage: 1\n");

        MobRegistry mobs = load();

        assertEquals(0, mobs.size());
        for (String f : List.of("nostages.yml", "first.yml", "nospread.yml")) {
            assertTrue(warningText().contains(f), f + ": " + warningText());
        }
    }

    /** The Phage we ship splits into 2 at stage 1, half a block either side. */
    @Test
    void theBundledPhageSplitsIntoTwoStageOnes() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/phage.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("phage.yml"), in.readAllBytes());
        }
        assertEquals(new MobSplit(2, 1, 0.5), load().find("phage").orElseThrow().split());
        assertTrue(warnings.isEmpty(), warningText());
    }

    // ---- the plan's step R4: growth ----

    /** The growth block's three values ARRIVE from content, and a file without the block has none. */
    @Test
    void theGrowthBlockArrives() throws IOException {
        write("phage.yml", "base_entity: creeper\n" + STAGES
                + "growth:\n  element: fire\n  radius: 9.5\n  stages_per_death: 2\n");
        write("plain.yml", "base_entity: creeper\n" + STAGES);

        MobRegistry mobs = load();

        assertEquals(new MobGrowth("fire", 9.5, 2), mobs.find("phage").orElseThrow().growth());
        assertNull(mobs.find("plain").orElseThrow().growth());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** An unknown growth element, growth on an unstaged mob, and a missing key are each a named, skipped file. */
    @Test
    void aMalformedGrowthIsRefused() throws IOException {
        write("viod.yml", "base_entity: creeper\n" + STAGES
                + "growth:\n  element: viod\n  radius: 6\n  stages_per_death: 1\n");
        write("unstaged.yml", "base_entity: creeper\nmax_health: 80\n"
                + "growth:\n  element: void\n  radius: 6\n  stages_per_death: 1\n");
        write("noradius.yml", "base_entity: creeper\n" + STAGES + "growth:\n  element: void\n  stages_per_death: 1\n");

        MobRegistry mobs = load();

        assertEquals(0, mobs.size());
        for (String f : List.of("viod.yml", "unstaged.yml", "noradius.yml")) {
            assertTrue(warningText().contains(f), f + ": " + warningText());
        }
        assertTrue(warningText().contains("'viod'"), warningText());
    }

    /** The Phage we ship grows on void deaths within 6 blocks, one stage each. */
    @Test
    void theBundledPhageGrowsOnVoidDeathsWithinSixBlocks() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/phage.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("phage.yml"), in.readAllBytes());
        }
        assertEquals(new MobGrowth("void", 6.0, 1), load().find("phage").orElseThrow().growth());
        assertTrue(warnings.isEmpty(), warningText());
    }

    // ---- dragon-mobs step R3: the blast ----

    /** The fuse's distances by stage (the void implosion's Part C): a number is the last stage's, scaled by height. */
    private static MobBlast blastOf(double reach, double trigger, double cancel, int fuse, double first, double last,
                                    double pull, double lift, String visual) {
        return new MobBlast(StagedDistance.byHeight(reach), StagedDistance.byHeight(trigger),
                StagedDistance.byHeight(cancel), fuse, first, last, pull, lift, visual);
    }

    /**
     * The blast's fields ARRIVE from content (the pull, the lift and the visual since the void implosion; the trigger
     * and the cancel, and every distance by stage, since its Part C), and a file without the block has none.
     */
    @Test
    void theBlastBlockArrives() throws IOException {
        write("phage.yml", "base_entity: creeper\n" + STAGES
                + "blast:\n  reach: 4.5\n  trigger: 2.5\n  cancel: 5.5\n  damage: { first: 11, last: 77 }\n"
                + "  fuse_ticks: 44\n  pull: 0.75\n  lift: 0.5\n  visual: void_implosion\n");
        write("plain.yml", "base_entity: creeper\n" + STAGES);

        MobRegistry mobs = load();

        assertEquals(blastOf(4.5, 2.5, 5.5, 44, 11, 77, 0.75, 0.5, "void_implosion"),
                mobs.find("phage").orElseThrow().blast());
        assertNull(mobs.find("plain").orElseThrow().blast(), "no blast block, no blast");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * THE CONTENT-ONLY WAY TO TUNE THE SMALL ONE APART: a distance authored as {@code { first, last }} arrives as a
     * linear staged distance, a number as the last stage's scaled by height. Both forms in one file.
     */
    @Test
    void aDistanceAuthoredAsFirstAndLastArrivesLinear() throws IOException {
        write("phage.yml", "base_entity: creeper\n" + STAGES
                + "blast:\n  reach: { first: 1.5, last: 6 }\n  trigger: 3\n  cancel: { first: 3, last: 7 }\n"
                + "  damage: { first: 60, last: 150 }\n  fuse_ticks: 30\n  pull: 1.25\n  lift: 1\n  visual: v\n");

        MobBlast b = load().find("phage").orElseThrow().blast();

        assertEquals(new StagedDistance(1.5, 6.0), b.reach());
        assertEquals(StagedDistance.byHeight(3), b.trigger());
        assertEquals(new StagedDistance(3.0, 7.0), b.cancel());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * A blast on an unstaged mob; a missing key (reach, trigger, cancel, damage, pull, lift, visual); a zero fuse; a
     * negative pull; a cancel not beyond its trigger; a {@code { first }} with no {@code last}; and a STALE
     * {@code knockback:} key (the push the pull replaced, as a data folder copied before 2026-11-17 still has it) are
     * each a named, skipped file. Every file but the one under test carries every other key, so each is refused for its
     * own.
     */
    @Test
    void aMalformedBlastIsRefused() throws IOException {
        String head = "base_entity: creeper\n" + STAGES + "blast:\n";
        String reach = "  reach: 3\n";
        String fuse = "  trigger: 1.5\n  cancel: 3.5\n";
        String damage = "  damage: { first: 20, last: 50 }\n  fuse_ticks: 30\n";
        String rest = "  pull: 1\n  lift: 1\n  visual: v\n";
        write("unstaged.yml", "base_entity: creeper\nmax_health: 80\nblast:\n" + reach + fuse + damage + rest);
        write("nolast.yml", head + reach + fuse + "  damage: { first: 20 }\n  fuse_ticks: 30\n" + rest);
        write("noreach.yml", head + fuse + damage + rest);
        write("notrigger.yml", head + reach + "  cancel: 3.5\n" + damage + rest);
        write("nocancel.yml", head + reach + "  trigger: 1.5\n" + damage + rest);
        write("nofuse.yml", head + reach + fuse + "  damage: { first: 20, last: 50 }\n  fuse_ticks: 0\n" + rest);
        write("nopull.yml", head + reach + fuse + damage + "  lift: 1\n  visual: v\n");
        write("negpull.yml", head + reach + fuse + damage + "  pull: -1\n  lift: 1\n  visual: v\n");
        write("nolift.yml", head + reach + fuse + damage + "  pull: 1\n  visual: v\n");
        write("novisual.yml", head + reach + fuse + damage + "  pull: 1\n  lift: 1\n");
        write("inverted.yml", head + reach + "  trigger: 3.5\n  cancel: 1.5\n" + damage + rest);
        write("halfstaged.yml", head + "  reach: { first: 1 }\n" + fuse + damage + rest);
        write("staleknock.yml", head + reach + fuse + damage + "  knockback: 1\n" + rest);

        MobRegistry mobs = load();

        assertEquals(0, mobs.size());
        for (String f : List.of("unstaged.yml", "nolast.yml", "noreach.yml", "notrigger.yml", "nocancel.yml",
                "nofuse.yml", "nopull.yml", "negpull.yml", "nolift.yml", "novisual.yml", "inverted.yml",
                "halfstaged.yml", "staleknock.yml")) {
            assertTrue(warningText().contains(f), f + ": " + warningText());
        }
    }

    /**
     * THE PHAGE WE SHIP: vanilla's creeper numbers for its last stage (reach 6, trigger 3, cancel 7, fuse 30), each
     * earlier stage's distances by its height (Ben: "The Large phage being the current distances and scaling everything
     * down to the small phage"; the seat's ruling 1 on #204); the 60 and 150; a PULL at 1.25 x the push's horizontal
     * with the push's own LIFT (the seat's ruling 2); drawn as void_implosion.
     */
    @Test
    void theBundledPhageScalesItsDistancesByHeightAndPullsWithThePushsLift() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/phage.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("phage.yml"), in.readAllBytes());
        }
        assertEquals(blastOf(6, 3, 7, 30, 60, 150, 1.25, 1.0, "void_implosion"),
                load().find("phage").orElseThrow().blast());
        assertTrue(warnings.isEmpty(), warningText());
    }

    // ---- dragon-mobs step R5: the Knell's beam (and its sounds, the follow-up of 2026-11-17) ----

    /** The sound keys this test's loader treats as real; production asks the server's sound registry. */
    private static final java.util.Set<String> SOUNDS =
            java.util.Set.of("block.bell.use", "block.bell.resonate", "block.amethyst_block.chime", "entity.cat.ambient",
                    "entity.wither.ambient", "entity.shulker_bullet.hurt",
                    // step R7b: the Phalanx's blink and shield
                    "entity.enderman.teleport", "item.shield.break",
                    // step R8: the Skreel's pounce
                    "entity.spider.ambient", "entity.spider.hurt", "entity.player.attack.crit");

    private static final String SOUNDS_OK = "  sounds:\n"
            + "    telegraph:\n      - { key: entity.cat.ambient, volume: 0.25, pitch: 1.75 }\n"
            + "    fire:\n      - { key: block.bell.resonate, volume: 1.5, pitch: 0.5 }\n"
            + "      - { key: block.amethyst_block.chime, volume: 0.75, pitch: 1.25 }\n";

    private static final String BEAM_OK = "beam:\n  cycle_ticks: { min: 33, max: 77 }\n  telegraph_ticks: 9\n"
            + "  flash_at: [0, 5]\n  range: 25.5\n  aim_height: 0.4\n  tolerance: 0.75\n  damage: 21\n" + SOUNDS_OK;

    private static final List<io.github.butterflysmp.rpg.core.mob.BeamSound> TELEGRAPH_OK =
            List.of(new io.github.butterflysmp.rpg.core.mob.BeamSound("entity.cat.ambient", 0.25, 1.75));
    private static final List<io.github.butterflysmp.rpg.core.mob.BeamSound> FIRE_OK = List.of(
            new io.github.butterflysmp.rpg.core.mob.BeamSound("block.bell.resonate", 1.5, 0.5),
            new io.github.butterflysmp.rpg.core.mob.BeamSound("block.amethyst_block.chime", 0.75, 1.25));

    /**
     * Every one of the beam's numbers ARRIVES from content, each a value no other key in the block shares (so a
     * transposition of two keys in the loader cannot pass), the sounds in their two lists in order, and a file without
     * the block has none.
     */
    @Test
    void theBeamBlockArrives() throws IOException {
        write("knell.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK);
        write("plain.yml", "base_entity: zombie\nmax_health: 20\n");
        MobRegistry mobs = load();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(new io.github.butterflysmp.rpg.core.mob.MobBeam(33, 77, 9, List.of(0, 5), 25.5, 0.4, 0.75, 21.0,
                TELEGRAPH_OK, FIRE_OK), mobs.find("knell").orElseThrow().beam());
        assertNull(mobs.find("plain").orElseThrow().beam(), "no beam block, no beam");
    }

    /** A staged mob may author a beam too: the beam does not depend on stages. */
    @Test
    void aStagedMobMayCarryABeam() throws IOException {
        write("staged.yml", "base_entity: creeper\n" + STAGES + BEAM_OK);
        MobRegistry mobs = load();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(21.0, mobs.find("staged").orElseThrow().beam().damage(), 0.0);
    }

    /** Each missing key, and a flash outside the telegraph, is a named, skipped file. */
    @Test
    void aMalformedBeamIsRefused() throws IOException {
        for (String key : List.of("cycle_ticks.min", "cycle_ticks.max", "telegraph_ticks", "flash_at", "range",
                "aim_height", "tolerance", "damage")) {
            String leaf = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;
            String yaml = key.equals("cycle_ticks.min") ? BEAM_OK.replace("{ min: 33, max: 77 }", "{ max: 77 }")
                    : key.equals("cycle_ticks.max") ? BEAM_OK.replace("{ min: 33, max: 77 }", "{ min: 33 }")
                    : BEAM_OK.replaceAll("(?m)^  " + leaf + ":.*\\n", "");
            assertTrue(!yaml.equals(BEAM_OK), "the fixture for " + key + " must actually remove it");
            write("no_" + leaf + ".yml", "base_entity: wither_skeleton\nmax_health: 360\n" + yaml);
        }
        write("lateflash.yml", "base_entity: wither_skeleton\nmax_health: 360\n"
                + BEAM_OK.replace("[0, 5]", "[0, 9]"));
        MobRegistry mobs = load();
        assertEquals(0, mobs.size(), warningText());
        for (String f : List.of("no_min.yml", "no_max.yml", "no_telegraph_ticks.yml", "no_flash_at.yml",
                "no_range.yml", "no_aim_height.yml", "no_tolerance.yml", "no_damage.yml", "lateflash.yml")) {
            assertTrue(warningText().contains(f), f + ": " + warningText());
        }
        assertTrue(warningText().contains("beam.flash_at 9"), warningText());
    }

    /**
     * THE SOUNDS ARE CONTENT, AND EACH IS CHECKED: a key the server does not know is a named, skipped file (the key
     * named); so is a missing sounds block, a missing list, and a sound with no key, volume or pitch.
     */
    @Test
    void aMalformedSoundIsRefusedByName() throws IOException {
        String cat = "{ key: entity.cat.ambient, volume: 0.25, pitch: 1.75 }";
        String fireList = "    fire:\n      - { key: block.bell.resonate, volume: 1.5, pitch: 0.5 }\n"
                + "      - { key: block.amethyst_block.chime, volume: 0.75, pitch: 1.25 }\n";
        write("typo.yml", "base_entity: wither_skeleton\nmax_health: 360\n"
                + BEAM_OK.replace("block.bell.resonate", "block.bell.resonnate"));
        write("nosounds.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK.replace(SOUNDS_OK, ""));
        write("nofire.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK.replace(fireList, ""));
        write("nokey.yml", "base_entity: wither_skeleton\nmax_health: 360\n"
                + BEAM_OK.replace(cat, "{ volume: 0.25, pitch: 1.75 }"));
        write("novolume.yml", "base_entity: wither_skeleton\nmax_health: 360\n"
                + BEAM_OK.replace(cat, "{ key: entity.cat.ambient, pitch: 1.75 }"));
        write("nopitch.yml", "base_entity: wither_skeleton\nmax_health: 360\n"
                + BEAM_OK.replace(cat, "{ key: entity.cat.ambient, volume: 0.25 }"));
        for (String f : List.of("nosounds", "nofire", "nokey", "novolume", "nopitch")) {
            assertTrue(Files.readString(dir.resolve(f + ".yml")).length()
                    < ("base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK).length(), f + " removed something");
        }
        MobRegistry mobs = load();
        assertEquals(0, mobs.size(), warningText());
        for (String f : List.of("typo.yml", "nosounds.yml", "nofire.yml", "nokey.yml", "novolume.yml", "nopitch.yml")) {
            assertTrue(warningText().contains(f), f + ": " + warningText());
        }
        assertTrue(warningText().contains("block.bell.resonnate"), "the unknown key is named: " + warningText());
    }

    /**
     * THE KNELL WE SHIP (KNELL 5, 2026-11-17): 65 damage; a 24-tick telegraph flashing at tick 0 only (Ben, "remove it"), the shot in
     * the middle of the 48 ticks from the lock to the move (Ben's "I want the shot to be in the middle"); and two bells:
     * at the telegraph the bell back at pitch 1.0 (he answered "no" to 0.8) then the resonance; at the fire the bell at
     * pitch 1.5 (confirmed by play), then the chime. The cycle, range, aim and tolerance are unchanged.
     */
    @Test
    void theBundledKnellBeamsInTheMiddle() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/knell.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("knell.yml"), in.readAllBytes());
        }
        assertEquals(new io.github.butterflysmp.rpg.core.mob.MobBeam(100, 200, 24, List.of(0), 40.0, 0.6, 1.0, 65.0,
                List.of(new io.github.butterflysmp.rpg.core.mob.BeamSound("block.bell.use", 1.0, 1.0),
                        new io.github.butterflysmp.rpg.core.mob.BeamSound("block.bell.resonate", 1.4, 1.0)),
                List.of(new io.github.butterflysmp.rpg.core.mob.BeamSound("block.bell.use", 1.0, 1.5),
                        new io.github.butterflysmp.rpg.core.mob.BeamSound("block.amethyst_block.chime", 1.0, 1.0))),
                load().find("knell").orElseThrow().beam());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * THE TELEPORT'S 24 AGAINST THE CYCLE'S 100 (KNELL 5, the shot in the middle): the shipped
     * delay leaves 76 ticks before the earliest next attempt, so a move never falls inside a telegraph; the load-time
     * check refuses a delay that is not shorter.
     */
    @Test
    void theBundledKnellsDelayIsSeventySixTicksShorterThanItsCycle() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/knell.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("knell.yml"), in.readAllBytes());
        }
        var knell = load().find("knell").orElseThrow();
        assertEquals(76, knell.beam().cycleMinTicks() - knell.teleport().delayTicks());
    }

    // ---- dragon-mobs step R6: the teleport after the beam lands ----

    private static final String TELEPORT_OK = "teleport:\n  delay_ticks: 17\n  arc_degrees: { min: 25, max: 65 }\n"
            + "  spots_per_side: 4\n  level_tolerance: 2\n  require_sight: false\n";

    /** Every number ARRIVES, each a value no other key shares, and a file without the block has none. */
    @Test
    void theTeleportBlockArrives() throws IOException {
        write("knell.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK + TELEPORT_OK);
        write("beamonly.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK);
        MobRegistry mobs = load();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(new io.github.butterflysmp.rpg.core.mob.MobTeleport(17, 25, 65, 4, 2, false),
                mobs.find("knell").orElseThrow().teleport());
        assertNull(mobs.find("beamonly").orElseThrow().teleport());
    }

    /**
     * Each missing key is a named, skipped file; so is a teleport with no beam; and a delay not shorter than the
     * beam's cycle minimum (33 in BEAM_OK) is refused with the file and both numbers named.
     */
    @Test
    void aMalformedTeleportIsRefused() throws IOException {
        for (String key : List.of("delay_ticks", "spots_per_side", "level_tolerance", "require_sight")) {
            String yaml = TELEPORT_OK.replaceAll("(?m)^  " + key + ":.*\\n", "");
            assertTrue(!yaml.equals(TELEPORT_OK), key);
            write("no_" + key + ".yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK + yaml);
        }
        write("no_min.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK
                + TELEPORT_OK.replace("{ min: 25, max: 65 }", "{ max: 65 }"));
        write("no_max.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK
                + TELEPORT_OK.replace("{ min: 25, max: 65 }", "{ min: 25 }"));
        write("nobeam.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + TELEPORT_OK);
        write("slowmove.yml", "base_entity: wither_skeleton\nmax_health: 360\n" + BEAM_OK
                + TELEPORT_OK.replace("delay_ticks: 17", "delay_ticks: 33"));
        MobRegistry mobs = load();
        assertEquals(0, mobs.size(), warningText());
        for (String f : List.of("no_delay_ticks.yml", "no_spots_per_side.yml", "no_level_tolerance.yml",
                "no_require_sight.yml", "no_min.yml", "no_max.yml", "nobeam.yml", "slowmove.yml")) {
            assertTrue(warningText().contains(f), f + ": " + warningText());
        }
        assertTrue(warningText().contains("teleport.delay_ticks (33) must be shorter than beam.cycle_ticks.min (33)"),
                warningText());
    }

    /**
     * THE KNELL WE SHIP: 12 ticks after the hit (Ben's "the same time between the warning shot and the actual beam",
     * B), 30 to 70 degrees (Ben's, A), and the seat's provisional 5 a side (T-a), 3 blocks (T-b) and sight (T-c).
     */
    @Test
    void theBundledKnellTeleportsAsRuledAndProposed() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/mobs/knell.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("knell.yml"), in.readAllBytes());
        }
        assertEquals(new io.github.butterflysmp.rpg.core.mob.MobTeleport(24, 30, 70, 5, 3, true),
                load().find("knell").orElseThrow().teleport());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** Every bundled mob file loads with no warning, through the loader production runs. */
    @Test
    void everyBundledMobLoads() throws IOException {
        for (String name : List.of("knell", "phage", "phalanx", "skreel", "withered_mount")) {
            try (var in = getClass().getResourceAsStream("/content/mobs/" + name + ".yml")) {
                assertNotNull(in, name + " is missing from the classpath");
                Files.write(dir.resolve(name + ".yml"), in.readAllBytes());
            }
        }

        MobRegistry mobs = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(5, mobs.size());
        assertEquals(OptionalDouble.of(1.6), mobs.find("knell").orElseThrow().body().scale());
        assertEquals("void", mobs.find("phage").orElseThrow().element());
        assertNull(mobs.find("withered_mount").orElseThrow().element(), "the mount has no element (Ben)");
    }
}
