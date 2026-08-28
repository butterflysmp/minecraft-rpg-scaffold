package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.combat.SweepShare;
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


    /**
     * attack_speed is optional in general -- a ranged or costed weapon has no melee cadence to
     * state, and 0 is the "not declared" reading the reconciler treats as absent.
     */
    @Test
    void attackSpeedDefaultsToZeroWhenOmittedOnAWeaponWithNoMeleeBasic() throws IOException {
        write("plainbow.yml", """
                id: plainbow
                element: kinetic
                class: ranger
                attack_damage: 6
                triggers:
                  right_click:
                    cooldown_ticks: 15
                    cast:
                      type: projectile
                    on_hit:
                      - type: weapon_damage
                        element: kinetic
                """);

        assertEquals(0, load().find("plainbow").orElseThrow().attackSpeed(), 1e-9,
                "no attack_speed field -> 0, and a ranged basic attack needs none");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * But a weapon whose basic hit VANILLA delivers must declare one, and the file is skipped and
     * named if it does not.
     *
     * This guard exists because the failure is otherwise SILENT and was measured: with no authored
     * speed nothing writes the wielder's attack-speed attribute, so it sits at the player base 4.0
     * -- a 5-tick charge period inside a 10-tick i-frame window, where every allowed swing is
     * already fully charged and AttackCharge is dead code. The weapon would load, mint, swing, and
     * deal damage; only the feel would be quietly wrong. A named skip is much the better failure.
     */
    @Test
    void aVanillaDrivenMeleeWeaponWithNoAttackSpeedIsSkippedRatherThanSilentlyWrong() throws IOException {
        write("aaa_speedless.yml", """
                id: speedless
                element: kinetic
                class: melee
                attack_damage: 8
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
        assertTrue(warningText().contains("aaa_speedless.yml"), warningText());
        assertTrue(warningText().contains("attack_speed"), warningText());
        // Mutation: drop the vanilla-driven-melee guard in WeaponDefinition -> the speedless weapon
        // loads and swings at the bare-fist 4.0 -> reddens.
    }

    /**
     * The same weapon WITHOUT a vanilla-driven melee trigger loads fine at speed 0 -- proving the
     * guard keys on the trigger shape and not merely on being class: melee. Same file, same class,
     * same attack_damage; only the payload differs (a literal, so an ability rather than a basic
     * attack), and that flips the verdict.
     */
    @Test
    void aMeleeClassWeaponWithNoBasicAttackNeedsNoSpeed() throws IOException {
        write("ritualblade.yml", """
                id: ritualblade
                element: kinetic
                class: melee
                attack_damage: 8
                triggers:
                  left_click:
                    cooldown_ticks: 20
                    cast:
                      type: melee
                    on_hit:
                      - type: damage
                        amount: 8
                        element: kinetic
                """);

        assertEquals(0, load().find("ritualblade").orElseThrow().attackSpeed(), 1e-9);
        assertTrue(warnings.isEmpty(), warningText());
        // Mutation: widen the guard to any class: melee weapon -> this is skipped -> reddens.
    }

    /** A negative attack_speed is a content bug, rejected like a negative attack_damage. */
    @Test
    void aNegativeAttackSpeedIsSkippedNotCrashed() throws IOException {
        write("aaa_backwards.yml", """
                id: backwards
                element: kinetic
                class: melee
                attack_damage: 8
                attack_speed: -1.6
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: weapon_damage
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        assertEquals(1, load().size(), "the valid weapon still loads");
        assertTrue(warningText().contains("aaa_backwards.yml"), warningText());
        assertTrue(warningText().contains("attack_speed"), warningText());
        // Mutation: drop the attackSpeed < 0 guard -> it loads, and the reconciler writes a negative
        // speed onto the player -> reddens.
    }

    /** The sweep fraction parses off the file, like the two stats beside it. */
    @Test
    void aDeclaredSweepIsReadFromTheFile() throws IOException {
        write("blade.yml", """
                id: blade
                element: kinetic
                class: melee
                attack_damage: 8
                attack_speed: 1.6
                sweep: 0.5
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: weapon_damage
                        element: kinetic
                """);

        assertEquals(0.5, load().find("blade").orElseThrow().sweep(), 1e-9);
        // Mutation: read the key under any other name -> the default 0 wins, the weapon silently
        // stops sweeping, and nothing anywhere reports it -> reddens.
    }

    /** Absent means no sweep -- the reason this field is not a migration for an operator's files. */
    @Test
    void sweepDefaultsToNoneWhenOmitted() throws IOException {
        write("ironblade.yml", VALID);

        assertEquals(SweepShare.NONE, load().find("ironblade").orElseThrow().sweep(),
                "a weapon file with no sweep key loads and simply does not sweep");
        assertTrue(warnings.isEmpty(), warningText());
        // Mutation: default the key to 0.5, or make it required -> an operator's already-edited
        // weapon file either sweeps unasked or is REJECTED on the next restart, which is exactly the
        // Stage 2 attack_speed migration this field was shaped to avoid -> reddens.
    }

    /** A negative sweep is a content bug, rejected and named like a negative attack_speed. */
    @Test
    void aNegativeSweepIsSkippedNotCrashed() throws IOException {
        write("aaa_inverted.yml", """
                id: inverted
                element: kinetic
                class: melee
                attack_damage: 8
                attack_speed: 1.6
                sweep: -0.5
                triggers:
                  left_click:
                    cast:
                      type: melee
                    on_hit:
                      - type: weapon_damage
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        assertEquals(1, load().size(), "the valid weapon still loads");
        assertTrue(warningText().contains("aaa_inverted.yml"), warningText());
        assertTrue(warningText().contains("sweep"), warningText());
        // Mutation: drop the sweep < 0 guard -> it loads and every swept mob is HEALED by the swing
        // -> reddens.
    }

    /**
     * A sweep on a weapon with no melee basic is named, not silently ignored -- and the rest of the
     * content still loads, which is the whole argument for a per-file skip over a boot failure.
     */
    @Test
    void aSweepOnAWeaponWithNoMeleeBasicIsSkippedAndNamed() throws IOException {
        write("aaa_sweepy_bow.yml", """
                id: sweepy_bow
                element: kinetic
                class: ranger
                material: bow
                sweep: 0.5
                triggers:
                  right_click:
                    cast:
                      type: projectile
                    on_hit:
                      - type: damage
                        amount: 6
                        element: kinetic
                """);
        write("ironblade.yml", VALID);

        assertEquals(1, load().size(), "the valid weapon still loads");
        assertTrue(warningText().contains("aaa_sweepy_bow.yml"), warningText());
        assertTrue(warningText().contains("sweep"), warningText());
        // Mutation: drop the cross-field guard -> the bow loads carrying a sweep that nothing will
        // ever read, and the author is never told it does nothing -> reddens.
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

        // The SWEEP fraction is authored on the shipped blade. Half, so a bystander takes half of
        // whatever the primary took -- the number the boot gate reads as "a real reward for a big
        // swing, not an instant clear".
        assertEquals(0.5, weapon.sweep(), 1e-9, "ironblade declares a sweep fraction");
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

        // The sweep fraction, authored on this blade too -- both shipped swords sweep, and the
        // emberblade's lower attack_damage means it sweeps for less, which is the trade it already
        // makes on the primary hit.
        assertEquals(0.5, weapon.sweep(), 1e-9, "emberblade declares a sweep fraction");
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

        // NO SWEEP, and this is how a non-blade has none: by omitting the key, not by appearing on
        // some hard-coded exclusion list. WeaponDefinition would in fact REFUSE to load this file if
        // it declared one, since a projectile trigger has no vanilla-driven melee swing to sweep from.
        assertEquals(SweepShare.NONE, weapon.sweep(), "the bow declares no sweep");

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
