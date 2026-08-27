package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
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
            class: melee
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
        assertEquals(WeaponClass.MELEE, weapon.weaponClass());

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
                class: mage
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
                class: melee
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
                class: melee
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

    /** class is REQUIRED: a weapon that omits it is skipped and named, not silently defaulted. */
    @Test
    void missingClassIsSkippedNotCrashed() throws IOException {
        write("aaa_classless.yml", """
                id: classless
                element: kinetic
                rarity: common
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

        assertEquals(1, registry.size(), "the valid weapon still loads");
        assertTrue(warningText().contains("aaa_classless.yml"), warningText());
        assertTrue(warningText().contains("class"), warningText());
        // Mutation: default the loader's missing class to MELEE -> the classless weapon loads -> reddens.
    }

    /** An unknown class value skips-and-names, exactly like a bad rarity (and like SUMMONER before it lands). */
    @Test
    void unknownClassIsSkippedNotCrashed() throws IOException {
        write("aaa_summon.yml", """
                id: summon
                element: kinetic
                class: summoner
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
        assertTrue(warningText().contains("aaa_summon.yml"), warningText());
        assertTrue(warningText().contains("summoner"), warningText());
    }

    /** A trigger's authored name and description carry into the ability for the tooltip. */
    @Test
    void triggerNameAndDescriptionLoad() throws IOException {
        write("named.yml", """
                id: named
                element: fire
                class: mage
                triggers:
                  right_click:
                    name: "Fireball"
                    description:
                      - "Hurl a bursting ember."
                      - "Scorches all it catches."
                    cast:
                      type: projectile
                    on_hit:
                      - type: damage
                        amount: 12
                        element: fire
                """);

        WeaponRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        var ability = registry.find("named").orElseThrow().trigger("right_click").orElseThrow().ability();
        assertEquals("Fireball", ability.displayName());
        assertEquals(List.of("Hurl a bursting ember.", "Scorches all it catches."), ability.description());
    }

    /** A scalar description: is a content mistake -- getStringList drops it, so the loader warns, named. */
    @Test
    void scalarDescriptionWarnsButStillLoads() throws IOException {
        write("scalar.yml", """
                id: scalar
                element: fire
                class: mage
                triggers:
                  right_click:
                    name: "Bolt"
                    description: "this should have been a list"
                    cast:
                      type: projectile
                    on_hit:
                      - type: damage
                        amount: 8
                        element: fire
                """);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size(), "a scalar description is cosmetic -- the weapon still loads");
        assertTrue(warningText().contains("scalar.yml") || warningText().contains("description"), warningText());
        assertTrue(registry.find("scalar").orElseThrow().trigger("right_click").orElseThrow()
                .ability().description().isEmpty(), "the dropped scalar leaves an empty description");
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
                class: melee
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

    /** attack_damage is optional: a ranged/costed weapon (bow, staff) has no melee and declares none. */
    @Test
    void attackDamageDefaultsToZeroWhenOmitted() throws IOException {
        write("plainsword.yml", """
                id: plainsword
                element: kinetic
                class: melee
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: damage
                        amount: 5
                        element: kinetic
                """);

        assertEquals(0, load().find("plainsword").orElseThrow().attackDamage(), 1e-9,
                "no attack_damage field -> 0 (no melee stat)");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** A negative attack_damage is a content bug: WeaponDefinition rejects it, so the file is skipped
     *  and named, exactly like any other malformed weapon -- not silently loaded as a heal-on-hit. */
    @Test
    void aNegativeAttackDamageIsSkippedNotCrashed() throws IOException {
        write("aaa_cursed.yml", """
                id: cursed
                element: kinetic
                class: melee
                attack_damage: -5
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: weapon_damage
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        WeaponRegistry registry = load();

        assertEquals(1, registry.size(), "the valid weapon still loads");
        assertTrue(warningText().contains("aaa_cursed.yml"), warningText());
        assertTrue(warningText().contains("attack_damage"), warningText());
        // Mutation: drop the attackDamage < 0 guard in WeaponDefinition -> the cursed weapon loads -> reddens.
    }

    @Test
    void aWeaponWithNoTriggersSectionIsSkippedNotCrashed() throws IOException {
        write("aaa_bare.yml", """
                id: bare
                element: kinetic
                rarity: common
                class: melee
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
                class: melee
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
        assertEquals(WeaponClass.MELEE, weapon.weaponClass(), "ironblade declares class melee");
        assertEquals("ironblade/left_click", weapon.trigger("left_click").orElseThrow().ability().id());

        // The melee damage is a STAT: a top-level attack_damage, and a weapon_damage on_hit that
        // reads it (no literal amount buried in the effect). This locks the SHAPE of that promotion
        // on the shipped file. The magnitude is balance and deliberately floats -- but > 0 is not
        // decorative: the WeaponDamage check below holds for attack_damage: 0 too, which would be a
        // sword that reads as a basic attack and deals nothing.
        assertTrue(weapon.attackDamage() > 0,
                "ironblade declares a melee damage stat for its weapon_damage swing to read");
        var swing = weapon.trigger("left_click").orElseThrow().ability().onHit().get(0);
        var weaponDamage = assertInstanceOf(EffectSpec.WeaponDamage.class, swing,
                "the swing deals weapon_damage (the caster's stat), not a literal Damage");
        assertEquals("kinetic", weaponDamage.element());
        // Mutation: revert the swing to `type: damage` / drop attack_damage -> reddens.
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
        assertTrue(embers.burst().radius() > 0, "the thrown ember bursts with a real radius");
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

        // Free left-click swing -- weapon_damage reading the weapon's attack_damage stat.
        var left = weapon.trigger("left_click").orElseThrow().ability();
        assertEquals(ResourceCost.FREE, left.cost(), "the left-click swing is free");
        assertInstanceOf(CastSpec.Melee.class, left.cast());
        assertTrue(weapon.attackDamage() > 0,
                "emberblade declares a melee damage stat for its weapon_damage swing to read");
        assertInstanceOf(EffectSpec.WeaponDamage.class, left.onHit().get(0),
                "the swing deals weapon_damage; the costed special below keeps a literal");

        // Costed right-click special -- the shared-mana proof, at the content level.
        var right = weapon.trigger("right_click").orElseThrow().ability();
        assertEquals("mana", right.cost().resourceId());
        // The amount is bounded, not pinned: 40 is balance. But it must be > 0, because
        // ResourceCost.FREE is new ResourceCost("none", 0) -- a plain record, not a sentinel -- so a
        // degenerate `mana: 0` is NOT equal to FREE and would slip past the resourceId check above.
        assertTrue(right.cost().amount() > 0, "the special is costed, not free");
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
        assertEquals(WeaponClass.RANGER, weapon.weaponClass(), "the bow is a ranger weapon");

        // The shot is on right_click (so the per-trigger cancellation suppresses the draw),
        // free (the Ranger economy), and a projectile (the ranged trigger).
        assertTrue(weapon.trigger("left_click").isEmpty(), "no left-click binding: left-click is free");
        var shot = weapon.trigger("right_click").orElseThrow().ability();
        assertEquals(ResourceCost.FREE, shot.cost(), "the shot is free -- the bow carries the damage");
        assertInstanceOf(CastSpec.Projectile.class, shot.cast());
        // The fire rate (cooldown_ticks) is deliberately NOT asserted: it is pure balance, and no
        // bound is defensible either, since ability_stone ships cooldown_ticks: 0. The three
        // properties this block is about -- on right_click, free, projectile -- are the three above.

        // The shot is a STAT-READING basic attack, not a literal: an attack_damage stat on the
        // weapon, and a weapon_damage payload that reads it back. This is what makes a "+N Ranged
        // Damage" modifier have something to grip, and what earns the shot attack-speed scaling.
        //
        // The magnitude floats, but > 0 is load-bearing rather than decorative: DamagePayload maps a
        // WeaponDamage effect to WEAPON_STAT regardless of the number (isBasicAttack passes 0.0
        // outright), so the two assertions below would BOTH still pass on attack_damage: 0 -- a bow
        // that renders as a basic attack, gets attack-speed scaled, and deals nothing.
        assertTrue(weapon.attackDamage() > 0, "hunters_bow declares a ranged damage stat to read");
        assertTrue(DamagePayload.isBasicAttack(shot.onHit()),
                "the bow's shot must be a basic attack, behind its leading visual");
        assertEquals(DamagePayload.DamageSource.WEAPON_STAT,
                DamagePayload.of(shot.onHit(), weapon.attackDamage()).orElseThrow().source(),
                "the amount comes from the stat, not a literal amount: in content");
        assertFalse(weapon.flavor().isEmpty(),
                "the trigger's old description now renders as weapon flavour");
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
        assertEquals(WeaponClass.MAGE, weapon.weaponClass(), "the staff is a mage weapon");

        // COSTED, unlike the bow's free shot -- the Mage spends mana to deal damage.
        var shot = weapon.trigger("right_click").orElseThrow().ability();
        assertEquals("mana", shot.cost().resourceId());
        assertTrue(shot.cost().amount() > 0, "the bolt is costed, not free");   // magnitude is balance
        assertInstanceOf(CastSpec.Projectile.class, shot.cast());
    }

    /**
     * WHICH right-click presses are basic attacks -- the fact RpgListeners.onRightClick keys on when
     * it decides whether a refused press earns chat feedback.
     *
     * The bow's shot is a basic attack, so a rejected shot is SILENT: it is bound to right_click
     * only so that binding suppresses the vanilla draw, and spamming fire is attacking, not casting.
     * The emberblade's Fireball and the staff's Bolt are costed abilities, so a rejected press still
     * says "On cooldown for Xs" -- a deliberate special deserves an answer.
     *
     * Pinned here because the listener itself is boot-witnessed. If someone later converts
     * ember_staff to a weapon_damage basic attack (the open Mage decision in NEXT.md), this reddens
     * and makes them notice they have also just silenced the staff's feedback.
     */
    @Test
    void onlyTheBowsRightClickIsABasicAttack() throws IOException {
        for (String id : List.of("hunters_bow", "emberblade", "ember_staff")) {
            try (var in = getClass().getResourceAsStream("/content/weapons/" + id + ".yml")) {
                assertNotNull(in, "bundled " + id + " is missing from the classpath");
                Files.write(dir.resolve(id + ".yml"), in.readAllBytes());
            }
        }

        WeaponRegistry registry = load();
        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(3, registry.size(), "all three weapons must load, or the rest of this proves nothing");

        assertTrue(isBasicAttackOnRightClick(registry, "hunters_bow"),
                "the bow's shot is a basic attack -- a refused shot must be silent");
        assertFalse(isBasicAttackOnRightClick(registry, "emberblade"),
                "the emberblade's Fireball is a costed ability -- it keeps its feedback");
        assertFalse(isBasicAttackOnRightClick(registry, "ember_staff"),
                "the staff's Bolt is a costed ability -- it keeps its feedback");
    }

    private static boolean isBasicAttackOnRightClick(WeaponRegistry registry, String weaponId) {
        return DamagePayload.isBasicAttack(registry.find(weaponId).orElseThrow()
                .trigger("right_click").orElseThrow().ability().onHit());
    }
}
