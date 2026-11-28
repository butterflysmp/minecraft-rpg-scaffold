package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.Color;
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
import java.util.Arrays;
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

    /**
     * AN ABSENT `speed` IS 1.0, NOT 0.0, AND THIS TEST IS THE ONLY THING HOLDING THAT.
     *
     * `speed` is Bukkit's `extra`. Before the field existed, PaperCombatWorld.present called the
     * 6-argument spawnParticle, whose default chain in the pinned Paper API ends at `dconst_1` --
     * so every visual in content/ was authored, by eye, against extra = 1.0, without anyone
     * choosing it. 0.0 is the reflexive default for a new numeric field and it would silently
     * restyle ember_burst, ember_trail, solar_detonation, solar_lance, arc_surge (since deleted, ruling
     * 22) and void_slash
     * at once, with no test failing and nothing in a diff to point at.
     *
     * Mutation: change the 1.0 in VisualLoader to 0.0 -> this reddens. Nothing else does.
     */
    @Test
    void anAbsentSpeedIsOnePreservingWhatEveryOlderVisualWasAuthoredAgainst() throws IOException {
        write("bare.yml", """
                steps:
                  - type: particle
                    particle: FLAME
                """);

        var particles = (VisualSpec.Particles) load().find("bare").orElseThrow().steps().get(0);
        assertEquals(1.0, particles.speed(), 1e-9,
                "absent speed must stay the extra=1.0 the 6-arg spawnParticle always passed");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** And a file that states one gets it -- flint_impact's 0.05 flame drift. */
    @Test
    void speedIsParsedWhenAuthored() throws IOException {
        write("drift.yml", """
                steps:
                  - type: particle
                    particle: FLAME
                    count: 14
                    spread: 0.2
                    speed: 0.05
                  - type: particle
                    particle: SMOKE
                    count: 4
                    spread: 0.1
                    speed: 0.0
                """);

        var steps = load().find("drift").orElseThrow().steps();
        assertEquals(0.05, ((VisualSpec.Particles) steps.get(0)).speed(), 1e-9);
        assertEquals(0.0, ((VisualSpec.Particles) steps.get(1)).speed(), 1e-9,
                "an explicit 0.0 must survive, not fall back to the 1.0 default");
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
     * EVERY DATA-TAKING PARTICLE EXCEPT DUST IS STILL REJECTED AT LOAD, BY NAME.
     *
     * <p><b>THIS TEST REPLACES ONE THAT ASSERTED DUST ITSELF WAS REJECTED, AND THE SWAP IS
     * DELIBERATE RATHER THAN INCIDENTAL.</b> {@code particleNeedingADataObjectIsRejectedAtLoad}
     * used DUST as its specimen, so opening the schema to DUST would have deleted that guard's
     * only witness while leaving the rule it guarded -- "a particle whose data we cannot supply
     * fails at load, not at the first cast in front of a player" -- fully in force and completely
     * unwatched. That is NEXT.md's <i>A TUNING CHANGE CAN DELETE A RULE'S ONLY WITNESS WITHOUT
     * TOUCHING THE RULE</i>, so the coverage is handed over here explicitly rather than quietly
     * lost.
     *
     * <p><b>IT ENUMERATES THE AXIS RATHER THAN THE CASES WE HAPPEN TO KNOW.</b> A hardcoded list
     * of BLOCK/ITEM/VIBRATION would go stale the next time Paper adds a data-taking particle, and
     * nothing would say so -- the same defect EnchantLoaderTest had one directory over, where a
     * hardcoded roster meant bulwark.yml was never once loaded. So it walks Particle.values(),
     * takes everything whose data type is neither Void nor DustOptions, and <b>fails loudly if
     * that set is empty</b>: discovering nothing here would make every assertion below vacuous.
     *
     * <p>Mutation: widen the loader's check to {@code p.getDataType() != Void.class} -- admit any
     * data object rather than DustOptions specifically -> every member of the set loads and this
     * reddens naming the first one.
     */
    @Test
    void everyDataTakingParticleExceptDustIsStillRejectedByName() throws IOException {
        List<Particle> rejected = Arrays.stream(Particle.values())
                .filter(p -> p.getDataType() != Void.class)
                .filter(p -> p.getDataType() != Particle.DustOptions.class)
                // TRAIL'S DATA BECAME EXPRESSIBLE 2026-11-28 (M3): handed over to
                // trailArrivesWithItsColourAndItsServerSetLifetimeAndTargetsItsOwnPoint and trailsRefusalsAreNamedSkippedFiles,
                // the same explicit swap DUST's was. This guard SAW it: it went red the moment the gate opened.
                .filter(p -> p.getDataType() != Particle.Trail.class)
                .toList();

        assertFalse(rejected.isEmpty(), "discovered NO data-taking particles besides DUST -- a scan "
                + "that finds nothing is a defect, not an empty axis, and it would make every "
                + "assertion below pass vacuously");

        for (Particle p : rejected) {
            warnings.clear();
            write("aaa_probe.yml", "steps:\n  - type: particle\n    particle: " + p.name() + "\n");
            write("solar_detonation.yml", VALID);

            VisualRegistry registry = load();

            assertEquals(1, registry.size(), p + " must not load; the valid visual still must");
            assertTrue(warningText().contains("aaa_probe.yml"), p + ": " + warningText());
            assertTrue(warningText().contains("requires a data object"), p + ": " + warningText());
            assertTrue(warningText().contains(p.getDataType().getSimpleName()),
                    p + " must be named with the type it wanted: " + warningText());
        }
    }

    /**
     * AND DUST NOW LOADS, WITH THE COLOUR AND SIZE THE FILE ASKED FOR.
     *
     * <p>The other half of the swap above. Asserts the RESOLVED DustOptions rather than merely
     * that the file parsed, because "it loaded" would pass on options built from the wrong
     * channels -- and a wrong colour is precisely the thing no unit test can otherwise see.
     *
     * <p>Mutation: revert the DustOptions arm in VisualLoader.particle() -> the file is skipped
     * and orElseThrow reddens. Swap two channels in dust() -> the colour assertion reddens with
     * the transposed value.
     */
    @Test
    void dustIsAcceptedNowThatTheSchemaCanSupplyItsData() throws IOException {
        write("beam.yml", """
                steps:
                  - type: particle
                    particle: DUST
                    color: [40, 90, 240]
                    size: 1.2
                """);

        var particles = (VisualSpec.Particles) load().find("beam").orElseThrow().steps().get(0);

        assertNotNull(particles.dust(), "DUST must arrive carrying its data object");
        assertEquals(Color.fromRGB(40, 90, 240), particles.dust().getColor(), "the authored blue");
        assertEquals(1.2f, particles.dust().getSize(), 1e-6, "and the authored size");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * TRAIL (M3, 2026-11-28): a particle whose lifetime the SERVER sets arrives carrying its colour and its
     * {@code duration_ticks}, no dust; and the data handed to {@code spawnParticle} for a point is a {@code
     * Particle.Trail} TARGETING THAT SAME POINT, so the particle stays where it is drawn.
     *
     * <p>Mutations: the Trail arm out of the data-type gate -> the file is skipped and orElseThrow reddens; the target
     * taken from anywhere but the drawn point -> the target assertion reddens.
     */
    @Test
    void trailArrivesWithItsColourAndItsServerSetLifetimeAndTargetsItsOwnPoint() throws IOException {
        write("spiral.yml", """
                steps:
                  - type: particle
                    particle: TRAIL
                    count: 1
                    spread: 0.0
                    speed: 0.0
                    color: [255, 120, 0]
                    duration_ticks: 8
                """);
        var particles = (VisualSpec.Particles) load().find("spiral").orElseThrow().steps().get(0);
        assertEquals(org.bukkit.Particle.TRAIL, particles.particle());
        assertEquals(null, particles.dust());
        assertEquals(Color.fromRGB(255, 120, 0), particles.trail().color());
        assertEquals(8, particles.trail().durationTicks());
        var at = new org.bukkit.Location(null, 3.5, 64.1, -2.25);
        var data = (org.bukkit.Particle.Trail) particles.dataAt(at);
        assertEquals(at, data.getTarget());
        assertEquals(8, data.getDuration());
        assertEquals(Color.fromRGB(255, 120, 0), data.getColor());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * TRAIL's refusals, each a named, skipped file (M3): no {@code duration_ticks}; a {@code size} (TRAIL has none);
     * a duration of 0; {@code duration_ticks} on FLAME, whose lifetime the server cannot set; and TRAIL in a
     * {@code spiral} step, which supplies no trail data. The control file beside each loads.
     */
    @Test
    void trailsRefusalsAreNamedSkippedFiles() throws IOException {
        String[][] cases = {
                {"aaa_t1.yml", "steps:\n  - type: particle\n    particle: TRAIL\n    color: [255, 120, 0]\n",
                        "requires 'duration_ticks'"},
                {"aaa_t2.yml", "steps:\n  - type: particle\n    particle: TRAIL\n    color: [255, 120, 0]\n"
                        + "    duration_ticks: 8\n    size: 1.0\n", "sets 'size'"},
                {"aaa_t3.yml", "steps:\n  - type: particle\n    particle: TRAIL\n    color: [255, 120, 0]\n"
                        + "    duration_ticks: 0\n", "must be 1 or more"},
                {"aaa_t4.yml", "steps:\n  - type: particle\n    particle: FLAME\n    duration_ticks: 8\n",
                        "whose lifetime the server cannot set"},
                {"aaa_t5.yml", "steps:\n  - type: spiral\n    particle: TRAIL\n    color: [255, 120, 0]\n"
                        + "    duration_ticks: 8\n    arms: 3\n    turns: 1\n    points_per_tick: 4\n", "supplies no trail data"},
        };
        for (String[] c : cases) {
            warnings.clear();
            for (var f : java.nio.file.Files.list(dir).toList()) java.nio.file.Files.delete(f);
            write(c[0], c[1]);
            write("solar_detonation.yml", VALID);
            assertEquals(1, load().size(), c[0]);
            assertTrue(warningText().contains(c[0]) && warningText().contains(c[2]), c[0] + ": " + warningText());
        }
    }

    /**
     * A DUST STEP WITHOUT A COLOUR IS A NAMED, SKIPPED FILE -- not a black beam, and not a
     * NullPointerException at the first cast in front of a player.
     */
    @Test
    void dustWithoutAColourIsRejected() throws IOException {
        write("aaa_bare_dust.yml", "steps:\n  - type: particle\n    particle: DUST\n");
        write("solar_detonation.yml", VALID);

        assertEquals(1, load().size());
        assertTrue(warningText().contains("aaa_bare_dust.yml"), warningText());
        assertTrue(warningText().contains("requires a 'color'"), warningText());
    }

    /**
     * THE INVERSE GUARD, AND IT IS THE HALF THAT IS EASY TO FORGET.
     *
     * <p>Authoring {@code color:} on FLAME cannot work -- FLAME takes no data object. Silently
     * ignoring it would leave the author with a field they set, a file that loaded, and no way at
     * all to discover the colour never applied. A field the code quietly drops is
     * indistinguishable from a field that works.
     *
     * <p>Mutation: return null early for a non-DUST particle without checking whether colour was
     * authored -> this reddens, and a coloured FLAME becomes a silent no-op.
     */
    @Test
    void aColourAuthoredOnAParticleThatTakesNoDataIsRejected() throws IOException {
        write("aaa_coloured_flame.yml", """
                steps:
                  - type: particle
                    particle: FLAME
                    color: [40, 90, 240]
                """);
        write("solar_detonation.yml", VALID);

        assertEquals(1, load().size());
        assertTrue(warningText().contains("aaa_coloured_flame.yml"), warningText());
        assertTrue(warningText().contains("takes no data object"), warningText());
    }

    /**
     * AN ABSENT samples_per_block IS 4.0, NOT 0.0 -- the third instance of "absent is not zero" on
     * this schema, after speed and the projectile body's upward pop.
     *
     * <p>0.0 is the reflexive default for a new numeric field, and here it would mean <b>a beam
     * that draws nothing at all</b>: BeamSamples.along returns an empty list, presentAlong loops
     * zero times, and nothing errors anywhere. A silent no-op, which is the defect shape this repo
     * has now recorded three times.
     *
     * <p>Mutation: change the default in VisualLoader to 0.0 -> this reddens. Nothing else does.
     */
    @Test
    void anAbsentSamplesPerBlockIsFourNotZero() throws IOException {
        write("bare.yml", "steps:\n  - type: particle\n    particle: FLAME\n");
        var particles = (VisualSpec.Particles) load().find("bare").orElseThrow().steps().get(0);
        assertEquals(4.0, particles.samplesPerBlock(), 1e-9,
                "absent density must be 4 per block, never 0 -- 0 draws no beam, silently");
    }

    /** And a file that states one gets it -- lapis_beam authors its 4 explicitly anyway. */
    @Test
    void samplesPerBlockIsParsedWhenAuthored() throws IOException {
        write("dense.yml", """
                steps:
                  - type: particle
                    particle: FLAME
                    samples_per_block: 12
                """);
        var particles = (VisualSpec.Particles) load().find("dense").orElseThrow().steps().get(0);
        assertEquals(12.0, particles.samplesPerBlock(), 1e-9);
    }

    /**
     * THE SHIPPED lapis_beam.yml CARRIES THE PORTED NUMBERS, loaded through the REAL loader.
     *
     * <p>The fixture tests above prove the schema CAN express a colour; this proves the file on
     * disk actually asks for the right one. Nothing else checks the shipped beam -- its colour,
     * its size and its density are invisible to every other test in the suite, and to every test
     * that could ever exist, since none of the three has an observable effect off a client.
     *
     * <p>Mutation: edit any of the numbers in the shipped yml -> this reddens naming it.
     */
    @Test
    void theShippedLapisBeamCarriesThePortedNumbers() {
        File shipped = new File("src/main/resources/content/visuals");
        assertTrue(shipped.isDirectory(), "expected shipped visuals at " + shipped.getAbsolutePath());

        var beam = new VisualLoader(log).loadAll(shipped).find("lapis_beam").orElseThrow(
                () -> new AssertionError("lapis_beam.yml did not load -- see the loader warning"));

        var dust = (VisualSpec.Particles) beam.steps().get(0);
        assertEquals(Particle.DUST, dust.particle());
        assertEquals(Color.fromRGB(40, 90, 240), dust.dust().getColor(), "the ported lapis blue");
        assertEquals(1.2f, dust.dust().getSize(), 1e-6, "the ported size");
        assertEquals(4.0, dust.samplesPerBlock(), 1e-9, "the ported max(2, distance * 4) density");
        assertEquals(1, dust.count(), "one dust per sample point, as the old repo spawned");
        assertEquals(0.0, dust.speed(), 1e-9, "inert for DUST, but authored rather than omitted");
    }

    /**
     * THE BLAZE KING'S STAFF'S TWO VISUALS LOAD FROM THE SHIPPED TREE WITH cfde822's NUMBERS (LEGACY-B,
     * Q-B6: Ben, "use the old sounds and effects").
     *
     * <p>The shipped-tree load above asserts {@code lapis_beam} only, and the loader SKIPS a bad file
     * with a warning rather than failing, so without this row a typo in either new file (a particle
     * name, a sound key) would ship green and show up as a boot warning and a silent staff. Every
     * number is cfde822 {@code StaffListener}'s, read for {@code PLAN-legacy-b.md} section 8.
     *
     * <p>Mutation: edit any number in either shipped yml -> this reddens naming it.
     */
    @Test
    void theShippedBlazeFireballVisualsCarryCfde822sNumbers() {
        VisualRegistry shipped = new VisualLoader(log).loadAll(new File("src/main/resources/content/visuals"));

        var cast = shipped.find("blaze_fireball_cast").orElseThrow(
                () -> new AssertionError("blaze_fireball_cast.yml did not load: " + warningText()));
        assertEquals(1, cast.steps().size(), "the cast is one sound");
        var shoot = (VisualSpec.Sound) cast.steps().get(0);
        assertEquals("entity.blaze.shoot", shoot.key());
        assertEquals(1.0f, shoot.volume(), 0f);
        assertEquals(1.2f, shoot.pitch(), 0f);

        var impact = shipped.find("blaze_fireball_impact").orElseThrow(
                () -> new AssertionError("blaze_fireball_impact.yml did not load: " + warningText()));
        assertEquals(3, impact.steps().size(), "flame, lava, crack");
        var flame = (VisualSpec.Particles) impact.steps().get(0);
        assertEquals(Particle.FLAME, flame.particle());
        assertEquals(18, flame.count());
        assertEquals(0.25, flame.spread(), 0.0);
        assertEquals(0.06, flame.speed(), 0.0);
        var lava = (VisualSpec.Particles) impact.steps().get(1);
        assertEquals(Particle.LAVA, lava.particle());
        assertEquals(5, lava.count());
        assertEquals(0.1, lava.spread(), 0.0);
        assertEquals(0.0, lava.speed(), 0.0, "authored, not the schema's 1.0 default");
        var crack = (VisualSpec.Sound) impact.steps().get(2);
        assertEquals("item.firecharge.use", crack.key());
        assertEquals(1.0f, crack.volume(), 0f);
        assertEquals(1.3f, crack.pitch(), 0f);
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

    /** Ruling 22: a stale arc_surge.yml in an old data folder is SKIPPED and named once, never deleted. */
    @Test
    void aRetiredVisualFileIsSkippedAndNamedOnce() throws IOException {
        write("arc_surge.yml", "steps:\n  - type: sound\n    key: block.beacon.activate\n    volume: 0.7\n    pitch: 1.4\n");
        write("clean.yml", "steps:\n  - type: sound\n    key: block.beacon.activate\n    volume: 0.7\n    pitch: 1.4\n");
        VisualRegistry registry = load();
        assertTrue(registry.find("arc_surge").isEmpty(), "a retired visual must not load from a stale copy");
        assertTrue(registry.find("clean").isPresent(), "the control file still loads: " + warningText());
        assertEquals(1, warnings.size(), "ONE warning: " + warningText());
        assertTrue(warningText().contains("arc_surge.yml"), warningText());
        assertFalse(warningText().contains("clean.yml"), warningText());
    }

    // ---- the void implosion's spiral (PLAN-dragon-mobs.md, the 2026-11-17 brief, Part D 5) ----

    /** Every authored field arrives: the geometry, the DUST colour and size, the default radius and the height. */
    @Test
    void aSpiralStepArrivesWithItsGeometryColourRadiusAndHeight() throws IOException {
        write("swirl.yml", """
                steps:
                  - type: spiral
                    particle: DUST
                    color: [75, 0, 110]
                    size: 1.4
                    duration_ticks: 12
                    arms: 4
                    turns: 1.25
                    points_per_tick: 5
                    phase_degrees: 45
                    radius: 2.5
                    height: 0.75
                """);

        var spiral = (VisualSpec.Spiral) load().find("swirl").orElseThrow().steps().get(0);

        assertEquals(new io.github.butterflysmp.rpg.core.shape.Spiral(12, 4, 1.25, 5, 45.0), spiral.shape());
        assertEquals(Color.fromRGB(75, 0, 110), spiral.dust().getColor());
        assertEquals(1.4f, spiral.dust().getSize(), 1e-6);
        assertEquals(2.5, spiral.radius(), 0.0, "the radius used when the caller names none");
        assertEquals(0.75, spiral.height(), 0.0);
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * THE SWIRL LASTS THE DRAG'S SECOND (Ben's 1, 2026-11-18: "the swirl lasting the same second"): both of the shipped
     * void_implosion's spirals are drawn over exactly the ticks the shipped Phage drags for. Two files, one number:
     * reading phage.yml's own duration_ticks here makes a change to either alone red.
     */
    @Test
    void theShippedSwirlLastsAsLongAsTheShippedPhageDrags() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/visuals/void_implosion.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("void_implosion.yml"), in.readAllBytes());
        }
        int phageDrag;
        try (var in = getClass().getResourceAsStream("/content/mobs/phage.yml")) {
            assertNotNull(in);
            var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            phageDrag = yaml.getInt("blast.duration_ticks");
        }
        assertEquals(20, phageDrag, "the Phage drags for 20 ticks");
        var spirals = load().find("void_implosion").orElseThrow().steps().stream()
                .filter(s -> s instanceof VisualSpec.Spiral).map(s -> (VisualSpec.Spiral) s).toList();
        assertEquals(2, spirals.size(), "black and dark purple");
        for (VisualSpec.Spiral s : spirals) assertEquals(phageDrag, s.shape().durationTicks());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** The optional fields default: no phase, a radius of 1 block, a height of 0.5. */
    @Test
    void aSpiralsOptionalFieldsDefault() throws IOException {
        write("swirl.yml", """
                steps:
                  - type: spiral
                    particle: SMOKE
                    duration_ticks: 10
                    arms: 3
                    turns: 1.5
                    points_per_tick: 6
                """);

        var spiral = (VisualSpec.Spiral) load().find("swirl").orElseThrow().steps().get(0);

        assertEquals(0.0, spiral.shape().phaseDegrees(), 0.0);
        assertEquals(1.0, spiral.radius(), 0.0);
        assertEquals(0.5, spiral.height(), 0.0);
        assertNull(spiral.dust(), "a particle without data carries none");
    }

    /** A spiral missing its geometry, or with a geometry core refuses, is a named, skipped file. */
    @Test
    void aMalformedSpiralIsRefusedByName() throws IOException {
        write("noarms.yml", """
                steps:
                  - type: spiral
                    particle: SMOKE
                    duration_ticks: 10
                    turns: 1.5
                    points_per_tick: 6
                """);
        write("onetick.yml", """
                steps:
                  - type: spiral
                    particle: SMOKE
                    duration_ticks: 1
                    arms: 3
                    turns: 1.5
                    points_per_tick: 6
                """);

        VisualRegistry visuals = load();

        assertTrue(visuals.find("noarms").isEmpty());
        assertTrue(visuals.find("onetick").isEmpty());
        assertTrue(warningText().contains("noarms.yml"), warningText());
        assertTrue(warningText().contains("onetick.yml"), warningText());
    }

    /**
     * THE BUNDLED void_implosion: black and dark purple DUST, inward over 20 ticks since 2026-11-18 (was 10; CHOSEN,
     * listed for Ben), and a deep sound. The 20 is the Phage's drag ({@link #theShippedSwirlLastsAsLongAsTheShippedPhageDrags}).
     */
    @Test
    void theBundledVoidImplosionIsABlackAndADarkPurpleSpiralAndADeepSound() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/visuals/void_implosion.yml")) {
            assertNotNull(in, "void_implosion.yml must ship");
            java.nio.file.Files.write(dir.resolve("void_implosion.yml"), in.readAllBytes());
        }
        var steps = load().find("void_implosion").orElseThrow().steps();
        var black = (VisualSpec.Spiral) steps.get(0);
        var purple = (VisualSpec.Spiral) steps.get(1);
        assertEquals(Color.fromRGB(16, 16, 16), black.dust().getColor());
        assertEquals(Color.fromRGB(88, 24, 140), purple.dust().getColor());
        assertEquals(20, black.shape().durationTicks());
        assertEquals(20, purple.shape().durationTicks());
        assertEquals(60.0, purple.shape().phaseDegrees(), 0.0, "purple's arms sit between black's");
        assertTrue(steps.stream().anyMatch(s -> s instanceof VisualSpec.Sound), "a deep sound");
        assertTrue(warnings.isEmpty(), warningText());
    }
}
