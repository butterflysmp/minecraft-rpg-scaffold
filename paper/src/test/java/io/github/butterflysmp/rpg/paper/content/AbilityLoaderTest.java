package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
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
            element: fire
            cooldown_ticks: 200
            cast:
              type: projectile
            on_hit:
              - type: damage
                amount: 12
                element: fire
            """;

    @Test
    void loadsAValidAbility() throws IOException {
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        AbilityDefinition def = registry.find("solar_grenade").orElseThrow();
        assertEquals("fire", def.element());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** The dash cast: a new shape, parsed like every other, carrying its four-effect payload. */
    @Test
    void loadsADashCastWithItsPayload() throws IOException {
        write("ember_step.yml", """
                id: ember_step
                element: fire
                cooldown_ticks: 160
                cast:
                  type: dash
                  distance: 12
                  speed: 1.6
                  lift: 0.4
                on_hit:
                  - type: damage
                    amount: 8
                    element: fire
                  - type: knockback
                    strength: 1.0
                  - type: status
                    status_id: scorch
                    duration_ticks: 60
                  - type: visual
                    visual_id: solar_detonation
                """);

        AbilityDefinition def = load().find("ember_step").orElseThrow();

        var dash = assertInstanceOf(CastSpec.Dash.class, def.cast());
        assertEquals(12, dash.distance(), 1e-9);
        assertEquals(1.6, dash.speed(), 1e-9);
        assertEquals(0.4, dash.lift(), 1e-9);
        assertEquals(4, def.onHit().size(), "damage + knockback + status + visual");
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

    /**
     * A misspelled element no longer skips the file. Element is a content id now, validated
     * by ContentValidator at boot -- not by the loader, which carries whatever string it is
     * given. A bad value warns later; it does not lose the ability.
     */
    @Test
    void anUnknownElementValueStillLoads() throws IOException {
        write("typo.yml", """
                id: typo
                element: fyre
                on_hit: []
                """);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertEquals("fyre", registry.find("typo").orElseThrow().element(), "carried as-is, not resolved");
        assertTrue(warnings.isEmpty(), warningText());
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

    @Test
    void loadsABurst() throws IOException {
        write("blast.yml", """
                id: blast
                element: solar
                on_hit:
                  - type: burst
                    radius: 4.0
                    effects:
                      - type: damage
                        amount: 6
                        element: solar
                      - type: status
                        status_id: scorch
                        duration_ticks: 40
                """);

        var burst = (EffectSpec.Burst) load().find("blast").orElseThrow().onHit().get(0);

        assertEquals(4.0, burst.radius(), 1e-9);
        assertEquals(2, burst.effects().size());
        assertInstanceOf(EffectSpec.Damage.class, burst.effects().get(0));
        assertInstanceOf(EffectSpec.Status.class, burst.effects().get(1));
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** A burst nesting an untargeted effect cannot be represented in core, same as an area. */
    @Test
    void burstNestingAnUntargetedEffectIsSkippedNotCrashed() throws IOException {
        write("aaa_nested_burst.yml", """
                id: nested
                element: solar
                on_hit:
                  - type: burst
                    radius: 4.0
                    effects:
                      - type: visual
                        visual_id: sparkles
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_nested_burst.yml"), warningText());
        assertTrue(warningText().contains("cannot be nested"), warningText());
        assertTrue(warningText().contains("burst"), warningText());
    }

    @Test
    void burstWithZeroRadiusIsSkippedNotCrashed() throws IOException {
        write("aaa_flat.yml", """
                id: flat
                element: solar
                on_hit:
                  - type: burst
                    radius: 0
                    effects:
                      - type: damage
                        amount: 6
                        element: solar
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_flat.yml"), warningText());
        assertTrue(warningText().contains("radius"), warningText());
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
        write("a.yml", "element: fire\n");                              // no id -> skipped
        write("b.yml", "id: b\nelement: fire\non_hit:\n  - type: teleport\n"); // unknown effect -> skipped

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
     * The content we actually ship, parsed by the loader we actually run. Both its burst
     * and its area nest a status effect, which is the shape that used to NPE at runtime
     * and the shape the Targeted-only nesting rule must still allow.
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
        assertEquals("fire", def.element());
        assertEquals(200, def.cooldownTicks());
        assertEquals("energy", def.cost().resourceId());

        // The blast: splash damage plus the ignition, on the detonation frame.
        var burst = def.onHit().stream()
                .filter(EffectSpec.Burst.class::isInstance)
                .map(EffectSpec.Burst.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("no burst: mobs would ignite late"));
        // 3 effects: splash damage, the scorch ignition, and rooted_TEMP -- a boot-test
        // fixture on the grenade burst. When rooted_TEMP is removed in the status content
        // pass, this count returns to 2 and the get(2) assertion below is deleted.
        assertEquals(3, burst.effects().size());
        assertInstanceOf(EffectSpec.Damage.class, burst.effects().get(0));
        var ignition = assertInstanceOf(EffectSpec.Status.class, burst.effects().get(1));
        assertEquals("scorch", ignition.statusId());
        var rootedTemp = assertInstanceOf(EffectSpec.Status.class, burst.effects().get(2));
        assertEquals("rooted", rootedTemp.statusId()); // rooted_TEMP: remove with the fixture

        // The field: a damage pulse, and a scorch that refreshes the burn.
        var area = def.onHit().stream()
                .filter(EffectSpec.Area.class::isInstance)
                .map(EffectSpec.Area.class::cast)
                .findFirst().orElseThrow();
        assertEquals(2, area.effects().size());
        assertInstanceOf(EffectSpec.Damage.class, area.effects().get(0));
        assertInstanceOf(EffectSpec.Status.class, area.effects().get(1));
    }
}
