package io.github.yourname.rpg.paper.content;

import io.github.yourname.rpg.core.ability.AbilityDefinition;
import io.github.yourname.rpg.core.ability.AbilityRegistry;
import io.github.yourname.rpg.core.ability.effect.EffectSpec;
import io.github.yourname.rpg.core.element.Element;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A typo in the 400th weapon must not take the server down. These tests pin the
 * fail-soft contract: log, name the file, skip it, keep loading.
 */
class AbilityLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("AbilityLoaderTest-" + System.nanoTime());
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

    private AbilityRegistry load() {
        return new AbilityLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    private static final String VALID = """
            id: solar_grenade
            element: solar
            cooldown_ticks: 200
            cast:
              type: projectile
            on_hit:
              - type: damage
                amount: 12
                element: solar
            """;

    @Test
    void loadsAValidAbility() throws IOException {
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        AbilityDefinition def = registry.find("solar_grenade").orElseThrow();
        assertEquals(Element.SOLAR, def.element());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** amplifier is optional -- most statuses have a single tier. */
    @Test
    void statusWithoutAmplifierDefaultsToZero() throws IOException {
        write("scorcher.yml", """
                id: scorcher
                element: solar
                on_hit:
                  - type: status
                    status_id: scorch
                    duration_ticks: 40
                """);

        AbilityRegistry registry = load();

        var status = (EffectSpec.Status) registry.find("scorcher").orElseThrow().onHit().get(0);
        assertEquals(0, status.amplifier());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void areaWithoutEffectsIsSkippedNotCrashed() throws IOException {
        write("aaa_broken.yml", """
                id: broken
                element: solar
                on_hit:
                  - type: area
                    radius: 4.0
                    duration_ticks: 100
                    tick_interval: 20
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size(), "the valid ability must still load");
        assertTrue(registry.find("solar_grenade").isPresent());
        assertTrue(warningText().contains("aaa_broken.yml"), warningText());
        assertTrue(warningText().contains("effects"), warningText());
    }

    /**
     * A tick_interval of 0 would make the area reschedule itself forever at zero delay.
     * EffectSpec.Area rejects it; the loader must report it like any content mistake.
     */
    @Test
    void areaWithZeroTickIntervalIsSkippedNotCrashed() throws IOException {
        write("aaa_storm.yml", """
                id: storm
                element: solar
                on_hit:
                  - type: area
                    radius: 4.0
                    duration_ticks: 100
                    tick_interval: 0
                    effects:
                      - type: damage
                        amount: 2
                        element: solar
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(1, registry.size(), "the valid ability must still load");
        assertTrue(warningText().contains("aaa_storm.yml"), warningText());
        assertTrue(warningText().contains("tick_interval"), warningText());
    }

    @Test
    void unknownElementIsSkippedNotCrashed() throws IOException {
        write("aaa_typo.yml", """
                id: typo
                element: sloar
                on_hit: []
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_typo.yml"), warningText());
        assertTrue(warningText().contains("sloar"), warningText());
    }

    @Test
    void missingRequiredFieldIsSkippedNotCrashed() throws IOException {
        write("aaa_noid.yml", "element: solar\n");
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_noid.yml"), warningText());
        assertTrue(warningText().contains("id"), warningText());
    }

    /** An area nesting an untargeted effect cannot be represented in core. */
    @Test
    void areaNestingAnUntargetedEffectIsSkippedNotCrashed() throws IOException {
        write("aaa_nested.yml", """
                id: nested
                element: solar
                on_hit:
                  - type: area
                    radius: 4.0
                    duration_ticks: 100
                    tick_interval: 20
                    effects:
                      - type: visual
                        visual_id: sparkles
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_nested.yml"), warningText());
        assertTrue(warningText().contains("cannot be nested"), warningText());
    }

    @Test
    void unknownEffectTypeIsSkippedNotCrashed() throws IOException {
        write("aaa_bogus.yml", """
                id: bogus
                element: solar
                on_hit:
                  - type: teleport
                    distance: 5
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("teleport"), warningText());
    }

    /** Every file broken: the server still boots, with zero abilities. */
    @Test
    void allFilesBrokenStillReturnsAnEmptyRegistry() throws IOException {
        write("a.yml", "element: solar\n");
        write("b.yml", "id: b\nelement: nonsense\n");

        AbilityRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(0, registry.size());
        assertEquals(3, warnings.size(), "two file warnings plus the summary");
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new AbilityLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /**
     * The content we actually ship, parsed by the loader we actually run. Its
     * area nests a status effect, which is exactly the shape that used to NPE
     * at runtime and the shape the Targeted-only nesting rule must still allow.
     */
    @Test
    void bundledSolarGrenadeContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/abilities/solar_grenade.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("solar_grenade.yml"), in.readAllBytes());
        }

        AbilityRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());

        AbilityDefinition def = registry.find("solar_grenade").orElseThrow();
        assertEquals(Element.SOLAR, def.element());
        assertEquals(200, def.cooldownTicks());
        assertEquals("energy", def.cost().resourceId());

        var area = def.onHit().stream()
                .filter(EffectSpec.Area.class::isInstance)
                .map(EffectSpec.Area.class::cast)
                .findFirst().orElseThrow();
        assertEquals(2, area.effects().size());
        assertInstanceOf(EffectSpec.Damage.class, area.effects().get(0));
        assertInstanceOf(EffectSpec.Status.class, area.effects().get(1));
    }
}
