package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
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
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** PLAN-build-system.md section 3.1.2: the bundled pools load, and each refusal names its file. */
class PoolLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    /** Resolves an id against the BUNDLED abilities: every shipped ability's file is named for its id. */
    private static final Predicate<String> BUNDLED_ABILITY =
            id -> PoolLoaderTest.class.getResource("/content/abilities/" + id + ".yml") != null;

    /** Resolves an id against the BUNDLED fragments (slice 4): every shipped fragment's file is named for its id. */
    private static final Predicate<String> BUNDLED_FRAGMENT =
            id -> PoolLoaderTest.class.getResource("/content/fragments/" + id + ".yml") != null;

    /** An aspect id -> its target, read from the BUNDLED aspect file's own `target:` line (slice 5). */
    private static final java.util.function.Function<String, java.util.Optional<String>> BUNDLED_ASPECT_TARGET = id -> {
        try (var in = PoolLoaderTest.class.getResourceAsStream("/content/aspects/" + id + ".yml")) {
            if (in == null) return java.util.Optional.empty();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.startsWith("target: ")) return java.util.Optional.of(line.substring("target: ".length()).strip());
            }
            return java.util.Optional.empty();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    };

    /** A BEHAVIOUR fragment id -> its target, read from the bundled fragment file's own `target:` line (section 7.3). */
    private static final java.util.function.Function<String, java.util.Optional<String>> BUNDLED_FRAGMENT_TARGET = id -> {
        try (var in = PoolLoaderTest.class.getResourceAsStream("/content/fragments/" + id + ".yml")) {
            if (in == null) return java.util.Optional.empty();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.startsWith("target: ")) return java.util.Optional.of(line.substring("target: ".length()).strip());
            }
            return java.util.Optional.empty();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    };

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("PoolLoaderTest-" + System.nanoTime());
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

    private void copyBundled(String name) throws IOException {
        try (var in = getClass().getResourceAsStream("/content/builds/" + name)) {
            assertNotNull(in, "bundled " + name + " is missing from the classpath");
            Files.write(dir.resolve(name), in.readAllBytes());
        }
    }

    private PoolRegistry load(Predicate<String> abilityExists) {
        return new PoolLoader(log).loadAll(new File(dir.toString()), abilityExists, BUNDLED_FRAGMENT,
                BUNDLED_ASPECT_TARGET, BUNDLED_FRAGMENT_TARGET);
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    /** The predicate is only worth trusting if it can say NO: the control for every bundled row below. */
    @Test
    void theBundledAbilityPredicateIsNotBlind() {
        assertTrue(BUNDLED_ABILITY.test("solar_lance"));
        assertFalse(BUNDLED_ABILITY.test("no_such_ability"));
    }

    @Test
    void theBundledFireRangerPoolLoads() throws IOException {
        copyBundled("ranger.yml");
        copyBundled("ranger_fire.yml");

        PoolRegistry registry = load(BUNDLED_ABILITY);

        assertTrue(warnings.isEmpty(), warningText());
        PoolDefinition pool = registry.find("ranger", "fire").orElseThrow();
        assertEquals("recall", pool.defaultLoadout().idFor(LoadoutSlot.ACTIVE_1));
        assertEquals("solar_lance", pool.defaultLoadout().idFor(LoadoutSlot.ACTIVE_2));
        assertEquals("ultimate_placeholder_ranger", pool.defaultLoadout().idFor(LoadoutSlot.ULTIMATE));
        assertEquals(List.of("solar_lance", "recall"), pool.actives(), "the cell's own, then the class-wide recall");
        assertTrue(pool.fragments().contains("fragment_ember_cache"));
    }

    @Test
    void theBundledFireMagePoolLoads() throws IOException {
        copyBundled("mage_fire.yml");

        PoolRegistry registry = load(BUNDLED_ABILITY);

        assertTrue(warnings.isEmpty(), warningText());
        PoolDefinition pool = registry.find("mage", "fire").orElseThrow();
        assertEquals("ember_step", pool.defaultLoadout().idFor(LoadoutSlot.ACTIVE_1));
        assertEquals("solar_grenade", pool.defaultLoadout().idFor(LoadoutSlot.ACTIVE_2));
        assertEquals("ultimate_placeholder_mage", pool.defaultLoadout().idFor(LoadoutSlot.ULTIMATE));
    }

    /** The fragment predicate can say NO too -- the control for the fragment rows below. */
    @Test
    void theBundledFragmentPredicateIsNotBlind() {
        assertTrue(BUNDLED_FRAGMENT.test("fragment_vigor"));
        assertFalse(BUNDLED_FRAGMENT.test("no_such_fragment"));
    }

    /** Slice 4: each bundled pool offers at least four fragments, so all four slots can be filled. */
    @Test
    void eachBundledPoolOffersAtLeastFourFragments() throws IOException {
        copyBundled("ranger.yml");
        copyBundled("ranger_fire.yml");
        copyBundled("mage_fire.yml");
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertTrue(warnings.isEmpty(), warningText());
        for (PoolDefinition pool : registry.all()) {
            assertTrue(pool.fragments().size() >= 4, pool.cell() + " offers " + pool.fragments());
        }
    }

    @Test
    void aPoolNamingAnUnknownFragmentIsRefusedByName() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                ultimates: [ultimate_placeholder_ranger]
                actives: [recall, solar_lance]
                fragments: [fragment_vigor, fragment_nowhere]
                default:
                  ultimate: ultimate_placeholder_ranger
                  actives: [recall, solar_lance]
                """);

        PoolRegistry registry = load(BUNDLED_ABILITY);

        assertEquals(0, registry.size());
        assertTrue(warningText().contains("fragment_nowhere"), "the refusal names the missing id: " + warningText());
        assertFalse(warningText().contains("fragment_vigor,"), "and only the missing one: " + warningText());
    }

    /** The aspect lookup can say NO, and reads a real target -- the control for the aspect rows below. */
    @Test
    void theBundledAspectLookupIsNotBlind() {
        assertEquals(java.util.Optional.of("solar_lance"), BUNDLED_ASPECT_TARGET.apply("searing_lance"));
        assertEquals(java.util.Optional.empty(), BUNDLED_ASPECT_TARGET.apply("no_such_aspect"));
    }

    /** Slice 5: each bundled pool lists two aspects, each on one of its own abilities. */
    @Test
    void eachBundledPoolOffersTwoAspectsOnItsOwnAbilities() throws IOException {
        copyBundled("ranger.yml");
        copyBundled("ranger_fire.yml");
        copyBundled("mage_fire.yml");
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(List.of("searing_lance", "updraft"), registry.find("ranger", "fire").orElseThrow().aspects());
        assertEquals(List.of("cinder_wake", "lingering_sun"), registry.find("mage", "fire").orElseThrow().aspects());
    }

    /** An aspect whose TARGET the pool does not offer is refused: it could never be active there. */
    @Test
    void anAspectOnAnAbilityThePoolDoesNotOfferIsRefused() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                ultimates: [ultimate_placeholder_ranger]
                actives: [recall, solar_lance]
                aspects: [searing_lance, cinder_wake]
                default:
                  ultimate: ultimate_placeholder_ranger
                  actives: [recall, solar_lance]
                """);
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertEquals(0, registry.size());
        assertTrue(warningText().contains("cinder_wake") && warningText().contains("ember_step"),
                "names the aspect and its target: " + warningText());
        assertFalse(warningText().contains("searing_lance ("), "and not the legal one: " + warningText());
    }

    @Test
    void aPoolNamingAnUnknownAspectIsRefused() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                ultimates: [ultimate_placeholder_ranger]
                actives: [recall, solar_lance]
                aspects: [aspect_nowhere]
                default:
                  ultimate: ultimate_placeholder_ranger
                  actives: [recall, solar_lance]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("aspect_nowhere"), warningText());
    }

    /**
     * Ruling 7: FIRE only, because only fire has pools. Read from the SHIPPED directory, so a stray file is
     * noticed: two cells and one class file (section 7.1), which is not a cell.
     */
    @Test
    void exactlyTheTwoFirePoolsShip() throws IOException {
        File shipped = new File("src/main/resources/content/builds");
        String[] names = shipped.list((d, n) -> n.endsWith(".yml"));
        assertNotNull(names, "the shipped builds directory must be readable");
        java.util.Arrays.sort(names);
        assertEquals(List.of("mage_fire.yml", "ranger.yml", "ranger_fire.yml"), List.of(names));
        for (String name : names) copyBundled(name);
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(2, registry.size());
    }

    // ------------------------------------------------------------------ section 7.1: the class file

    /**
     * THE CLASS MERGE's row, on the SHIPPED files: without ranger.yml the Fire Ranger pool offers no recall, and
     * its default names it -- so the pool is refused, not quietly short an Active.
     */
    @Test
    void theShippedFireRangerPoolNeedsItsClassFile() throws IOException {
        copyBundled("ranger_fire.yml");
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("recall"), warningText());
    }

    /** Ruling 24: the Mage is offered no Ranger class-wide ability. */
    @Test
    void aClassFileReachesOnlyItsOwnClass() throws IOException {
        copyBundled("ranger.yml");
        copyBundled("ranger_fire.yml");
        copyBundled("mage_fire.yml");
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertTrue(registry.find("ranger", "fire").orElseThrow().actives().contains("recall"));
        assertFalse(registry.find("mage", "fire").orElseThrow().actives().contains("recall"));
    }

    /** One fact, one home: an id both class-wide and cell-listed is refused, naming it -- not de-duplicated. */
    @Test
    void anIdBothClassWideAndCellListedRefusesTheCell() throws IOException {
        copyBundled("ranger.yml");
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                ultimates: [ultimate_placeholder_ranger]
                actives: [recall, solar_lance]
                default:
                  ultimate: ultimate_placeholder_ranger
                  actives: [recall, solar_lance]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("ranger_fire.yml") && warningText().contains("class-wide"), warningText());
    }

    /** The discriminator's guard: a cell file that forgot `element:` is refused, never read as a class file. */
    @Test
    void aCellFileMissingItsElementIsRefusedNotReadAsAClassFile() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                actives: [recall]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("ranger_fire") && warningText().contains("element"), warningText());
    }

    @Test
    void aClassFileCarryingACellKeyIsRefusedByName() throws IOException {
        write("ranger.yml", """
                class: ranger
                actives: [recall]
                fragments: [fragment_vigor]
                """);
        load(BUNDLED_ABILITY);
        assertTrue(warningText().contains("ranger.yml") && warningText().contains("fragments"), warningText());
    }

    @Test
    void aClassFileNamingAnUnknownAbilityIsRefused() throws IOException {
        write("ranger.yml", "class: ranger\nactives: [recall_nowhere]\n");
        load(BUNDLED_ABILITY);
        assertTrue(warningText().contains("recall_nowhere"), warningText());
    }

    @Test
    void aClassFileWithNoCellIsNamed() throws IOException {
        write("ranger.yml", "class: ranger\nactives: [recall]\n");
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("no cell"), warningText());
    }

    // ------------------------------------------------------------------ section 7.3: behaviour fragments

    @Test
    void theBundledFragmentTargetLookupIsNotBlind() {
        assertEquals(java.util.Optional.of("recall"), BUNDLED_FRAGMENT_TARGET.apply("fragment_ember_cache"));
        assertEquals(java.util.Optional.empty(), BUNDLED_FRAGMENT_TARGET.apply("fragment_vigor"), "a stat fragment");
    }

    /** A behaviour fragment on an ability the pool does not offer is refused: it could never be active there. */
    @Test
    void aBehaviourFragmentOnAnAbilityThePoolDoesNotOfferIsRefused() throws IOException {
        write("mage_fire.yml", """
                class: mage
                element: fire
                ultimates: [ultimate_placeholder_mage]
                actives: [ember_step, solar_grenade]
                fragments: [fragment_vigor, fragment_ember_cache]
                default:
                  ultimate: ultimate_placeholder_mage
                  actives: [ember_step, solar_grenade]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("fragment_ember_cache") && warningText().contains("recall"), warningText());
        assertFalse(warningText().contains("fragment_vigor ("), "and not the stat fragment: " + warningText());
    }

    @Test
    void aPoolNamingAnUnknownAbilityIsRefusedByName() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                ultimates: [sunfall]
                actives: [recall, solar_lance]
                default:
                  ultimate: sunfall
                  actives: [recall, solar_lance]
                """);

        PoolRegistry registry = load(BUNDLED_ABILITY);

        assertEquals(0, registry.size());
        assertTrue(warningText().contains("ranger_fire.yml"), warningText());
        assertTrue(warningText().contains("sunfall"), "the refusal names the missing id: " + warningText());
    }

    @Test
    void aDefaultOutsideTheListsIsRefused() throws IOException {
        write("mage_fire.yml", """
                class: mage
                element: fire
                ultimates: [ultimate_placeholder_mage]
                actives: [ember_step, solar_grenade]
                default:
                  ultimate: ultimate_placeholder_mage
                  actives: [ember_step, solar_lance]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("solar_lance"), warningText());
    }

    @Test
    void anAbilityInBothListsIsRefused() throws IOException {
        write("mage_fire.yml", """
                class: mage
                element: fire
                ultimates: [solar_grenade]
                actives: [ember_step, solar_grenade]
                default:
                  ultimate: solar_grenade
                  actives: [ember_step, solar_grenade]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("both an ultimate and an active"), warningText());
    }

    @Test
    void aDefaultWithoutExactlyTwoActivesIsRefused() throws IOException {
        write("mage_fire.yml", """
                class: mage
                element: fire
                ultimates: [ultimate_placeholder_mage]
                actives: [ember_step, solar_grenade]
                default:
                  ultimate: ultimate_placeholder_mage
                  actives: [ember_step]
                """);
        assertEquals(0, load(BUNDLED_ABILITY).size());
        assertTrue(warningText().contains("exactly 2"), warningText());
    }

    /** Fail-soft: one bad file costs one cell, not the others. */
    @Test
    void aBadFileSkipsOnlyItself() throws IOException {
        copyBundled("ranger.yml");
        copyBundled("ranger_fire.yml");
        write("mage_fire.yml", "class: mage\nelement: fire\n");
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertEquals(1, registry.size());
        assertTrue(registry.find("ranger", "fire").isPresent());
    }
}
