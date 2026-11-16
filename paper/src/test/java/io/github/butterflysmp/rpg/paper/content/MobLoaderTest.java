package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobBody;
import io.github.butterflysmp.rpg.core.mob.MobSplit;
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
        return new MobLoader(log, ELEMENTS::contains).loadAll(new File(dir.toString()));
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
        var mobs = new MobLoader(log, ELEMENTS::contains).loadAll(new File(dir.toFile(), "does_not_exist"));
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
