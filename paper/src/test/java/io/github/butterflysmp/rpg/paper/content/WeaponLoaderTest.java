package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.element.Element;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
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
 * Weapons load through the same fail-soft contract as abilities: a malformed file is
 * logged, named, and skipped, and every other weapon still loads. These tests pin that,
 * plus the two decisions of Phase 1: element defaults to kinetic, rarity to common, and
 * a trigger parses as an ability body through the shared AbilitySchema.
 */
class WeaponLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("WeaponLoaderTest-" + System.nanoTime());
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

    private WeaponRegistry load() {
        return new WeaponLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    private static final String VALID = """
            id: ironblade
            element: kinetic
            rarity: common
            triggers:
              left_click:
                cooldown_ticks: 10
                cast:
                  type: melee
                  reach: 3.5
                  arc_degrees: 120
                on_hit:
                  - type: damage
                    amount: 8
                    element: kinetic
            """;

    @Test
    void loadsAValidWeaponAndSynthesizesTheTriggerId() throws IOException {
        write("ironblade.yml", VALID);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size());
        WeaponDefinition weapon = registry.find("ironblade").orElseThrow();
        assertEquals(Element.KINETIC, weapon.element());
        assertEquals(Rarity.COMMON, weapon.rarity());

        TriggerBinding binding = weapon.trigger("left_click").orElseThrow();
        AbilityDefinition ability = binding.ability();
        assertEquals("ironblade/left_click", ability.id(), "the cooldown key must be (weapon, input)");
        assertInstanceOf(CastSpec.Melee.class, ability.cast());
        assertEquals(ResourceCost.FREE, ability.cost(), "no cost section means free");
        var damage = assertInstanceOf(EffectSpec.Damage.class, ability.onHit().get(0));
        assertEquals(8, damage.amount(), 1e-9);
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void elementDefaultsToKineticWhenOmitted() throws IOException {
        write("plainsword.yml", """
                id: plainsword
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: damage
                        amount: 5
                        element: kinetic
                """);

        WeaponRegistry registry = load();

        assertEquals(Element.KINETIC, registry.find("plainsword").orElseThrow().element());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void rarityDefaultsToCommonWhenOmitted() throws IOException {
        write("plainsword.yml", """
                id: plainsword
                element: kinetic
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: damage
                        amount: 5
                        element: kinetic
                """);

        assertEquals(Rarity.COMMON, load().find("plainsword").orElseThrow().rarity());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void unknownRarityIsSkippedNotCrashed() throws IOException {
        write("aaa_shiny.yml", """
                id: shiny
                element: kinetic
                rarity: mythic
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: damage
                        amount: 5
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size(), "the valid weapon must still load");
        assertTrue(warningText().contains("aaa_shiny.yml"), warningText());
        assertTrue(warningText().contains("mythic"), warningText());
    }

    @Test
    void unknownElementIsSkippedNotCrashed() throws IOException {
        write("aaa_typo.yml", """
                id: typo
                element: plasma
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: damage
                        amount: 5
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_typo.yml"), warningText());
        assertTrue(warningText().contains("plasma"), warningText());
    }

    @Test
    void aWeaponWithNoTriggersSectionIsSkippedNotCrashed() throws IOException {
        write("aaa_bare.yml", """
                id: bare
                element: kinetic
                rarity: common
                """);
        write("ironblade.yml", VALID);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_bare.yml"), warningText());
        assertTrue(warningText().contains("triggers"), warningText());
    }

    @Test
    void aTriggerWithAnUnknownCastTypeIsSkippedNotCrashed() throws IOException {
        write("aaa_warp.yml", """
                id: warp
                element: kinetic
                triggers:
                  left_click:
                    cast:
                      type: teleport
                    on_hit:
                      - type: damage
                        amount: 5
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_warp.yml"), warningText());
        assertTrue(warningText().contains("teleport"), warningText());
    }

    @Test
    void everyFileBrokenStillBootsWithZeroWeapons() throws IOException {
        write("a.yml", "id: a\nelement: kinetic\n");            // no triggers
        write("b.yml", "id: b\nelement: nonsense\ntriggers:\n  left_click:\n    cast:\n      type: melee\n");

        WeaponRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(0, registry.size());
        assertEquals(3, warnings.size(), "two file warnings plus the summary");
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new WeaponLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The content we actually ship, parsed by the loader we actually run. */
    @Test
    void bundledIronbladeContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/weapons/ironblade.yml")) {
            assertNotNull(in, "bundled ironblade is missing from the classpath");
            Files.write(dir.resolve("ironblade.yml"), in.readAllBytes());
        }

        WeaponRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        WeaponDefinition weapon = registry.find("ironblade").orElseThrow();
        assertEquals(Element.KINETIC, weapon.element());
        assertEquals(Rarity.COMMON, weapon.rarity());
        assertEquals("ironblade/left_click", weapon.trigger("left_click").orElseThrow().ability().id());
    }

    /** The shipped emberblade: a free left-click and a costed right-click on one weapon. */
    @Test
    void bundledEmberbladeContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/weapons/emberblade.yml")) {
            assertNotNull(in, "bundled emberblade is missing from the classpath");
            Files.write(dir.resolve("emberblade.yml"), in.readAllBytes());
        }

        WeaponRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        WeaponDefinition weapon = registry.find("emberblade").orElseThrow();
        assertEquals(Element.SOLAR, weapon.element());
        assertEquals(Rarity.RARE, weapon.rarity());

        // Free left-click swing.
        var left = weapon.trigger("left_click").orElseThrow().ability();
        assertEquals(ResourceCost.FREE, left.cost(), "the left-click swing is free");
        assertInstanceOf(CastSpec.Melee.class, left.cast());

        // Costed right-click special -- the shared-energy proof, at the content level.
        var right = weapon.trigger("right_click").orElseThrow().ability();
        assertEquals("energy", right.cost().resourceId());
        assertEquals(40, right.cost().amount(), 1e-9);
        assertInstanceOf(CastSpec.Projectile.class, right.cast());
    }
}
