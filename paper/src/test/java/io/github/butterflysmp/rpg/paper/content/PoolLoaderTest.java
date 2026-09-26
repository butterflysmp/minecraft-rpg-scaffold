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
        return new PoolLoader(log).loadAll(new File(dir.toString()), abilityExists, BUNDLED_FRAGMENT);
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
        copyBundled("ranger_fire.yml");

        PoolRegistry registry = load(BUNDLED_ABILITY);

        assertTrue(warnings.isEmpty(), warningText());
        PoolDefinition pool = registry.find("ranger", "fire").orElseThrow();
        assertEquals("rekindle", pool.defaultLoadout().idFor(LoadoutSlot.ACTIVE_1));
        assertEquals("solar_lance", pool.defaultLoadout().idFor(LoadoutSlot.ACTIVE_2));
        assertEquals("ultimate_placeholder_ranger", pool.defaultLoadout().idFor(LoadoutSlot.ULTIMATE));
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
                actives: [rekindle, solar_lance]
                fragments: [fragment_vigor, fragment_nowhere]
                default:
                  ultimate: ultimate_placeholder_ranger
                  actives: [rekindle, solar_lance]
                """);

        PoolRegistry registry = load(BUNDLED_ABILITY);

        assertEquals(0, registry.size());
        assertTrue(warningText().contains("fragment_nowhere"), "the refusal names the missing id: " + warningText());
        assertFalse(warningText().contains("fragment_vigor,"), "and only the missing one: " + warningText());
    }

    /** Ruling 7: FIRE only, because only fire has pools. A count, so a stray third file is noticed. */
    @Test
    void exactlyTheTwoFirePoolsShip() throws IOException {
        copyBundled("ranger_fire.yml");
        copyBundled("mage_fire.yml");
        assertEquals(2, load(BUNDLED_ABILITY).size());
    }

    @Test
    void aPoolNamingAnUnknownAbilityIsRefusedByName() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                ultimates: [sunfall]
                actives: [rekindle, solar_lance]
                default:
                  ultimate: sunfall
                  actives: [rekindle, solar_lance]
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
        copyBundled("ranger_fire.yml");
        write("mage_fire.yml", "class: mage\nelement: fire\n");
        PoolRegistry registry = load(BUNDLED_ABILITY);
        assertEquals(1, registry.size());
        assertTrue(registry.find("ranger", "fire").isPresent());
    }
}
