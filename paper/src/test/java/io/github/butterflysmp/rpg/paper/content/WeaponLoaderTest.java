package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
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
        assertEquals("kinetic", weapon.element());
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

    /**
     * A trigger's cast is the shared AbilitySchema, so any cast type an ability supports works
     * in a weapon trigger unchanged -- including `dash`, which the Ability Stone leans on. This
     * pins that shared-grammar guarantee: if a weapon-specific cast path ever crept back in and
     * hardcoded a subset, `dash` in a trigger would break and this reddens.
     */
    @Test
    void loadsADashCastInATrigger() throws IOException {
        write("ability_stone.yml", """
                id: ability_stone
                element: kinetic
                triggers:
                  left_click:
                    cast:
                      type: dash
                      distance: 12
                      speed: 1.6
                      lift: 0.4
                    on_hit:
                      - type: damage
                        amount: 8
                        element: fire
                """);

        WeaponRegistry registry = load();

        TriggerBinding binding = registry.find("ability_stone").orElseThrow().trigger("left_click").orElseThrow();
        var dash = assertInstanceOf(CastSpec.Dash.class, binding.ability().cast());
        assertEquals(12, dash.distance(), 1e-9);
        assertEquals(0.4, dash.lift(), 1e-9);
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

        assertEquals("kinetic", registry.find("plainsword").orElseThrow().element());
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

    /**
     * A misspelled element no longer skips the weapon. Element is a content id now, carried
     * by the loader and validated by ContentValidator at boot -- a bad value warns, it does
     * not lose the weapon.
     */
    @Test
    void anUnknownElementValueStillLoads() throws IOException {
        write("plasmasword.yml", """
                id: plasmasword
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

        WeaponRegistry registry = load();

        assertEquals(1, registry.size());
        assertEquals("plasma", registry.find("plasmasword").orElseThrow().element(), "carried as-is");
        assertTrue(warnings.isEmpty(), warningText());
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
        write("a.yml", "id: a\nelement: kinetic\n");            // no triggers -> skipped
        write("b.yml", "id: b\nelement: fire\ntriggers:\n  left_click:\n    cast:\n      type: teleport\n"); // unknown cast -> skipped

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
        assertEquals("kinetic", weapon.element());
        assertEquals(Rarity.COMMON, weapon.rarity());
        assertEquals("ironblade/left_click", weapon.trigger("left_click").orElseThrow().ability().id());
    }

    /**
     * The shipped Ability Stone -- the dev instrument the boot test fires. Its left-click
     * mirrors Rekindle, so it carries the same throw_embers grammar; no test loaded it before,
     * so a mistyped key would have failed silently at boot on the very weapon used to test.
     * This pins the thrown-item shape on the real file.
     */
    @Test
    void bundledAbilityStoneContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/weapons/ability_stone.yml")) {
            assertNotNull(in, "bundled ability_stone is missing from the classpath");
            Files.write(dir.resolve("ability_stone.yml"), in.readAllBytes());
        }

        WeaponRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        var stone = registry.find("ability_stone").orElseThrow();

        var cast = stone.trigger("left_click").orElseThrow().ability();
        assertInstanceOf(CastSpec.Dash.class, cast.cast());
        var embers = cast.onHit().stream()
                .filter(EffectSpec.ThrowEmbers.class::isInstance)
                .map(EffectSpec.ThrowEmbers.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("no throw_embers on the boot weapon"));
        assertEquals("blaze_powder", embers.itemId());
        assertEquals(4.0, embers.burst().radius(), 1e-9);
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
        assertEquals("fire", weapon.element());
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

    /** The shipped bow: a non-sword material, and a free right-click projectile shot. */
    @Test
    void bundledHuntersBowContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/weapons/hunters_bow.yml")) {
            assertNotNull(in, "bundled hunters_bow is missing from the classpath");
            Files.write(dir.resolve("hunters_bow.yml"), in.readAllBytes());
        }

        WeaponRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        WeaponDefinition weapon = registry.find("hunters_bow").orElseThrow();
        assertEquals("bow", weapon.material(), "the bow is the first non-sword weapon");

        // The shot is on right_click (so the per-trigger cancellation suppresses the draw),
        // free (the Ranger economy), and a projectile (the ranged trigger).
        assertTrue(weapon.trigger("left_click").isEmpty(), "no left-click binding: left-click is free");
        var shot = weapon.trigger("right_click").orElseThrow().ability();
        assertEquals(ResourceCost.FREE, shot.cost(), "the shot is free -- the bow carries the damage");
        assertInstanceOf(CastSpec.Projectile.class, shot.cast());
        assertEquals(15, shot.cooldownTicks(), "cooldown is the fire rate");
    }

    /** The shipped staff: a costed right-click projectile -- the Mage's commit primary. */
    @Test
    void bundledEmberStaffContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/weapons/ember_staff.yml")) {
            assertNotNull(in, "bundled ember_staff is missing from the classpath");
            Files.write(dir.resolve("ember_staff.yml"), in.readAllBytes());
        }

        WeaponRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        WeaponDefinition weapon = registry.find("ember_staff").orElseThrow();
        assertEquals("blaze_rod", weapon.material(), "a staff, not a sword or a bow");

        // COSTED, unlike the bow's free shot -- the Mage spends energy to deal damage.
        var shot = weapon.trigger("right_click").orElseThrow().ability();
        assertEquals("energy", shot.cost().resourceId());
        assertEquals(30, shot.cost().amount(), 1e-9);
        assertInstanceOf(CastSpec.Projectile.class, shot.cast());
    }
}
