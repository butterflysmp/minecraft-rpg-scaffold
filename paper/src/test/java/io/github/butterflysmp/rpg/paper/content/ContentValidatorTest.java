package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The walk is the interesting part, and it is pure map lookups, so no server.
 * The two Registry questions arrive as predicates for exactly that reason.
 */
class ContentValidatorTest {

    private static final Predicate<NamespacedKey> ALL_EXIST = key -> true;
    private static final Predicate<NamespacedKey> NONE_EXIST = key -> false;

    private static AbilityRegistry abilitiesWith(List<EffectSpec> onHit) {
        var registry = new AbilityRegistry();
        registry.register(new AbilityDefinition("solar_grenade", "Solar Grenade", "fire",
                "hunter", 200, new ResourceCost("mana", 40),
                new CastSpec.Projectile(1.2, 0.03, 100), onHit));
        return registry;
    }

    private static VisualRegistry visualsWith(String... ids) {
        var registry = new VisualRegistry();
        for (String id : ids) {
            registry.register(new VisualDefinition(id,
                    // speed stated rather than defaulted: the record deliberately has no
                    // convenience constructor, so no construction can be silent about its extra.
                    List.of(new VisualSpec.Particles(Particle.FLAME, 40, 0.6, 1.0))));
        }
        return registry;
    }

    private static StatusRegistry statusesWith(String... ids) {
        var registry = new StatusRegistry();
        for (String id : ids) {
            registry.register(new StatusDefinition.Fire(id));
        }
        return registry;
    }

    /** The seven real elements, so any test content wearing a valid element resolves. */
    private static final String[] REAL_ELEMENTS =
            {"fire", "water", "nature", "undead", "void", "wither", "kinetic"};

    private static ElementRegistry elementsWith(String... ids) {
        var registry = new ElementRegistry();
        for (String id : ids) {
            registry.register(new ElementDefinition(id, id));
        }
        return registry;
    }

    private static ContentValidator validator(VisualRegistry visuals, StatusRegistry statuses) {
        return new ContentValidator(visuals, statuses, elementsWith(REAL_ELEMENTS), ALL_EXIST, ALL_EXIST);
    }

    private static EffectSpec.Area areaContaining(EffectSpec.Targeted... effects) {
        return new EffectSpec.Area(4.0, 100, 20, List.of(effects));
    }

    @Test
    void resolvedReferencesProduceNoProblems() {
        var abilities = abilitiesWith(List.of(
                new EffectSpec.Visual("solar_detonation"),
                areaContaining(new EffectSpec.Status("scorch", 40, 0))));

        var problems = validator(visualsWith("solar_detonation"), statusesWith("scorch")).validate(abilities);

        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void danglingTopLevelVisualIdIsReported() {
        var abilities = abilitiesWith(List.of(new EffectSpec.Visual("nope")));

        var problems = validator(visualsWith("solar_detonation"), statusesWith("scorch")).validate(abilities);

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("solar_grenade"), problems.toString());
        assertTrue(problems.get(0).contains("nope"), problems.toString());
    }

    /**
     * The decisive case. solar_grenade's only status sits inside its area, so a
     * validator that walked only the top-level on_hit list would pass this while
     * checking nothing. Testing a bad top-level visual_id would not expose it.
     */
    @Test
    void danglingStatusIdNestedInsideAnAreaIsReported() {
        var abilities = abilitiesWith(List.of(
                new EffectSpec.Visual("solar_detonation"),          // resolves
                areaContaining(
                        new EffectSpec.Damage(2, "fire"),    // no reference
                        new EffectSpec.Status("nope", 40, 0))));    // dangles, one level down

        var problems = validator(visualsWith("solar_detonation"), statusesWith("scorch")).validate(abilities);

        assertEquals(1, problems.size(), "the nested status must be found: " + problems);
        assertTrue(problems.get(0).contains("solar_grenade"), problems.toString());
        assertTrue(problems.get(0).contains("nope"), problems.toString());
        assertTrue(problems.get(0).contains("status_id"), problems.toString());
    }

    /**
     * The same trap as the nested-area case, one effect type over. When scorch moved into
     * a burst, a validator that only knew about Area would have stopped checking it --
     * silently, and while still passing every other test.
     */
    @Test
    void danglingStatusIdNestedInsideABurstIsReported() {
        var abilities = abilitiesWith(List.of(
                new EffectSpec.Visual("solar_detonation"),                 // resolves
                new EffectSpec.Burst(4.0, List.of(
                        new EffectSpec.Damage(6, "fire"),           // no reference
                        new EffectSpec.Status("nope", 40, 0)))));          // dangles

        var problems = validator(visualsWith("solar_detonation"), statusesWith("scorch")).validate(abilities);

        assertEquals(1, problems.size(), "the status nested in the burst must be found: " + problems);
        assertTrue(problems.get(0).contains("solar_grenade"), problems.toString());
        assertTrue(problems.get(0).contains("nope"), problems.toString());
        assertTrue(problems.get(0).contains("status_id"), problems.toString());
    }

    @Test
    void effectsWithoutReferencesAreIgnored() {
        var abilities = abilitiesWith(List.of(
                new EffectSpec.Damage(12, "fire"),
                new EffectSpec.Heal(5),
                new EffectSpec.Knockback(1.5)));

        assertTrue(validator(visualsWith(), statusesWith()).validate(abilities).isEmpty());
    }

    @Test
    void everyDanglingReferenceIsReportedNotJustTheFirst() {
        var abilities = abilitiesWith(List.of(
                new EffectSpec.Visual("no_visual"),
                areaContaining(new EffectSpec.Status("no_status", 40, 0))));

        var problems = validator(visualsWith(), statusesWith()).validate(abilities);

        assertEquals(2, problems.size(), problems.toString());
    }

    /**
     * A DANGLING on_cast visual_id is reported, and the report says on_cast.
     *
     * It needs its own walk because on_cast is a separate list from on_hit -- a validator that
     * only walked on_hit would pass a weapon whose cast sound names a file that does not exist,
     * and the symptom in game is SILENCE, which is indistinguishable from a weapon that was never
     * given a cast sound at all. Exactly the invisibility that makes a dangling visual_id a named
     * warning rather than a quiet no-op everywhere else in this class.
     *
     * Mutation: delete the onCast loop in ContentValidator.validate -> 0 problems, reddens.
     */
    @Test
    void aDanglingOnCastVisualIsReportedAndNamedAsOnCast() {
        var registry = new AbilityRegistry();
        registry.register(new AbilityDefinition("flint_bolt", "Fire Bolt", "fire",
                "none", 24, new ResourceCost("mana", 5),
                new CastSpec.Projectile(1.4, 0.05, 40, "flint_trail"),
                List.of(new EffectSpec.Visual("flint_impact")), List.of(),
                List.of(new EffectSpec.Visual("no_such_cast_visual"))));

        var problems = validator(visualsWith("flint_impact"), statusesWith()).validate(registry);

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("on_cast"),
                "the problem must say which list it came from: " + problems.get(0));
        assertTrue(problems.get(0).contains("no_such_cast_visual"), problems.get(0));
    }

    /** A well-formed key that names no effect. Only a live Registry knows; hence the seam. */
    @Test
    void potionTypeThatNamesNoEffectIsReported() {
        var statuses = new StatusRegistry();
        statuses.register(new StatusDefinition.Potion("sluggish", NamespacedKey.minecraft("slowness")));

        var problems = new ContentValidator(visualsWith(), statuses, elementsWith(REAL_ELEMENTS), NONE_EXIST, ALL_EXIST)
                .validate(new AbilityRegistry());

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("sluggish"), problems.toString());
        assertTrue(problems.get(0).contains("slowness"), problems.toString());
    }

    @Test
    void soundKeyThatNamesNoSoundEventIsReported() {
        var visuals = new VisualRegistry();
        visuals.register(new VisualDefinition("boom", List.of(
                new VisualSpec.Sound("entity.blaze.shoot",
                        NamespacedKey.minecraft("entity.blaze.shoot"), 1.0f, 1.0f))));

        var problems = new ContentValidator(visuals, statusesWith(), elementsWith(REAL_ELEMENTS), ALL_EXIST, NONE_EXIST)
                .validate(new AbilityRegistry());

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("boom"), problems.toString());
        assertTrue(problems.get(0).contains("entity.blaze.shoot"), problems.toString());
    }

    /**
     * The content we actually ship, parsed by the loaders we actually run. A replica
     * built by hand here would keep passing after someone renamed scorch.yml.
     *
     * Only the ability -> visual/status references are checked: the Registry lookups
     * need a server, and they are exactly what the predicate seam exists to defer.
     */
    @Test
    void bundledContentHasNoDanglingReferences(@TempDir Path dir) throws IOException {
        var log = Logger.getLogger("ContentValidatorTest-" + System.nanoTime());
        var abilities = new AbilityLoader(log).loadAll(copyBundled(dir, "abilities", "solar_grenade.yml"));
        var visuals = new VisualLoader(log).loadAll(copyBundled(dir, "visuals", "solar_detonation.yml"));
        // rooted.yml is staged alongside scorch because the grenade now references rooted
        // (the rooted_TEMP burst fixture). Back to scorch.yml only / size 1 when it is removed.
        var statuses = new StatusLoader(log).loadAll(copyBundled(dir, "statuses", "scorch.yml", "rooted.yml"));

        assertEquals(1, abilities.size(), "bundled ability failed to load");
        assertEquals(1, visuals.size(), "bundled visual failed to load");
        assertEquals(2, statuses.size(), "bundled statuses failed to load");

        var problems = validator(visuals, statuses).validate(abilities);

        assertTrue(problems.isEmpty(), problems.toString());
    }

    /**
     * THE SHIPPED FLINT STAFF, parsed by the loaders that actually run, asserted against the
     * numbers cfde822 authored -- not a hand-built replica, which would keep passing after
     * someone edited the yml.
     *
     * <p>The `speed` assertions are the load-bearing ones and they are why this test exists at
     * all. `speed` is Bukkit's `extra`, and the schema default is 1.0 -- because the 6-arg
     * spawnParticle this adapter used to call passed 1.0, so every older visual was authored by
     * eye against it. cfde822's staff used 0.0 everywhere EXCEPT the impact flame's 0.05. An
     * omitted `speed` in either new file would therefore be a fast outward spray where the old
     * repo had none, and nothing else in the build would notice: the file parses, the visual
     * plays, and only a boot would show it -- against a bolt nobody has seen before, which is
     * exactly the reading this slice must not spend twice.
     *
     * <p>It also proves the wiring end to end: trail -> the cast, on_cast -> the trigger, and
     * every visual_id resolving to a file that exists.
     */
    @Test
    void theShippedFlintStaffCarriesCfde822sNumbers(@TempDir Path dir) throws IOException {
        var log = Logger.getLogger("ContentValidatorTest-" + System.nanoTime());
        var visuals = new VisualLoader(log).loadAll(
                copyBundled(dir, "visuals", "flint_cast.yml", "flint_trail.yml", "flint_impact.yml"));
        var weapons = new WeaponLoader(log).loadAll(copyBundled(dir, "weapons", "flint_staff.yml"));

        assertEquals(3, visuals.size(), "a bundled flint visual failed to parse");
        assertEquals(1, weapons.size(), "flint_staff.yml failed to parse");

        // --- the trail: FLAME x2 spread 0.05 speed 0.0, SMOKE x1 spread 0.03 speed 0.0 ---
        var trail = visuals.find("flint_trail").orElseThrow().steps();
        var trailFlame = assertInstanceOf(VisualSpec.Particles.class, trail.get(0));
        assertEquals(Particle.FLAME, trailFlame.particle());
        assertEquals(2, trailFlame.count());
        assertEquals(0.05, trailFlame.spread(), 1e-9);
        assertEquals(0.0, trailFlame.speed(), 1e-9, "cfde822's trail flame does not drift");
        var trailSmoke = assertInstanceOf(VisualSpec.Particles.class, trail.get(1));
        assertEquals(Particle.SMOKE, trailSmoke.particle());
        assertEquals(1, trailSmoke.count());
        assertEquals(0.03, trailSmoke.spread(), 1e-9);
        assertEquals(0.0, trailSmoke.speed(), 1e-9, "nor its smoke");

        // --- the impact: FLAME x14 spread 0.2 SPEED 0.05 (the one non-zero), SMOKE x4 speed 0 ---
        var impact = visuals.find("flint_impact").orElseThrow().steps();
        var impactFlame = assertInstanceOf(VisualSpec.Particles.class, impact.get(0));
        assertEquals(14, impactFlame.count());
        assertEquals(0.2, impactFlame.spread(), 1e-9);
        assertEquals(0.05, impactFlame.speed(), 1e-9,
                "the ONE non-zero extra in the whole staff -- what puffs the burst outward");
        assertEquals(0.0, assertInstanceOf(VisualSpec.Particles.class, impact.get(1)).speed(), 1e-9);

        // --- the wiring: the trigger names the trail, and announces itself on cast ---
        var trigger = weapons.find("flint_staff").orElseThrow().triggers().get(0);
        var cast = assertInstanceOf(CastSpec.Projectile.class, trigger.ability().cast());
        assertEquals("flint_trail", cast.trail(), "the bolt must leave something behind it");
        assertEquals(List.of(new EffectSpec.Visual("flint_cast")), trigger.ability().onCast(),
                "and must be audible on the frame the trigger is pressed");

        // --- and nothing dangles ---
        assertTrue(validator(visuals, statusesWith("scorch")).validateWeapons(weapons.all()).isEmpty(),
                "every visual_id the staff names must resolve");
    }

    // --- kit -> ability/weapon/element cross-reference, behind the Predicate<String> seams ---

    private static ContentValidator bareValidator() {
        return new ContentValidator(visualsWith(), statusesWith(), elementsWith(REAL_ELEMENTS), ALL_EXIST, ALL_EXIST);
    }

    private static final Predicate<String> ANY_ID = id -> true;

    @Test
    void kitWhoseGrantsAllExistProducesNoProblems() {
        var kit = new KitDefinition("ranger", "fire", "Fire Ranger",
                List.of(new WeaponGrant("hunters_bow", true)), List.of("arc_surge"));

        var problems = bareValidator().validateKits(List.of(kit), ANY_ID, ANY_ID);

        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void kitNamingAnUnknownAbilityOrWeaponIsReported() {
        var kit = new KitDefinition("ranger", "fire", "Fire Ranger",
                List.of(new WeaponGrant("real_bow", true), new WeaponGrant("typo_bow", false)),
                List.of("arc_surge", "typo_surge"));

        var problems = bareValidator().validateKits(List.of(kit),
                id -> id.equals("arc_surge"), id -> id.equals("real_bow"));

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("typo_surge") && p.contains("ability")), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("typo_bow") && p.contains("weapon")), problems.toString());
    }

    /** An unknown element on a kit warns, the same checkElement seam as a damage effect. */
    @Test
    void kitNamingAnUnknownElementIsReported() {
        var kit = new KitDefinition("ranger", "plasma", "Plasma Ranger",
                List.of(), List.of("arc_surge"));

        var problems = bareValidator().validateKits(List.of(kit), ANY_ID, ANY_ID);

        assertTrue(problems.stream().anyMatch(p -> p.contains("plasma") && p.contains("element")), problems.toString());
    }

    /**
     * The case a per-id check passes and still gets wrong: every grant dangles, so there is
     * no "remaining id" to complain about -- but the cell is unplayable. Report the empty
     * resolved set on top of the per-id problems.
     */
    @Test
    void kitWithNoExistingGrantIsReportedAsUnplayable() {
        var ghost = new KitDefinition("ghost", "fire", "Ghost",
                List.of(new WeaponGrant("gone_bow", true)), List.of("gone_a"));

        var problems = bareValidator().validateKits(List.of(ghost), id -> false, id -> false);

        // one dangling ability + one dangling weapon + one "nobody can play this cell"
        assertEquals(3, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("nobody can play")), problems.toString());
    }

    // --- element -> definition validation (element is inert identity; a bad one warns) ---

    @Test
    void danglingElementOnAnAbilityIsReported() {
        var abilities = new AbilityRegistry();
        abilities.register(new AbilityDefinition("x", "X", "plasma", "none",
                0, ResourceCost.FREE, new CastSpec.Self(), List.of()));

        var problems = validator(visualsWith(), statusesWith()).validate(abilities);

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("plasma"), problems.toString());
        assertTrue(problems.get(0).contains("element"), problems.toString());
    }

    /** The Damage arm of checkEffect, reached by the Area recursion -- nested elements too. */
    @Test
    void danglingElementOnANestedDamageEffectIsReported() {
        var abilities = abilitiesWith(List.of(areaContaining(new EffectSpec.Damage(2, "plasma"))));

        var problems = validator(visualsWith(), statusesWith()).validate(abilities);

        assertTrue(problems.stream().anyMatch(p -> p.contains("plasma") && p.contains("element")),
                problems.toString());
    }

    @Test
    void aValidElementProducesNoProblem() {
        var abilities = abilitiesWith(List.of(new EffectSpec.Damage(2, "void")));

        assertTrue(validator(visualsWith(), statusesWith()).validate(abilities).isEmpty());
    }

    // --- mobs: base_entity must name a real LIVING entity -------------------------------------------

    private static final java.util.function.Predicate<String> LIVING = name -> name.equals("wither_skeleton");
    private static final java.util.function.Predicate<String> EXISTS =
            name -> name.equals("wither_skeleton") || name.equals("arrow");

    private static java.util.List<io.github.butterflysmp.rpg.core.mob.MobDefinition> mob(String baseEntity) {
        return java.util.List.of(
                new io.github.butterflysmp.rpg.core.mob.MobDefinition("knell", baseEntity, "Knell", 360));
    }

    @Test
    void aMobNamingARealLivingEntityIsClean() {
        assertTrue(validator(visualsWith(), statusesWith())
                .validateMobs(mob("wither_skeleton"), EXISTS, LIVING).isEmpty());
    }

    @Test
    void aMobNamingNoEntityAtAllIsReported() {
        var problems = validator(visualsWith(), statusesWith())
                .validateMobs(mob("wither_skeletn"), EXISTS, LIVING);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("knell"), problems.toString());
        assertTrue(problems.get(0).contains("wither_skeletn"), problems.toString());
    }

    /**
     * The check that earns its keep. "arrow" is a perfectly real EntityType, so an existence-only
     * check passes it -- and then /rpg spawn ClassCastExceptions on a non-LivingEntity, weeks later,
     * for whoever tries it. Redden by dropping the isAlive half and this is the test that fails.
     */
    @Test
    void aMobNamingARealButNonLivingEntityIsReported() {
        var problems = validator(visualsWith(), statusesWith())
                .validateMobs(mob("arrow"), EXISTS, LIVING);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("not a living entity"), problems.toString());
    }

    /** Copies the named shipped resources into their own directory and returns that directory. */
    private static File copyBundled(Path root, String kind, String... files) throws IOException {
        Path dir = Files.createDirectories(root.resolve(kind));
        for (String file : files) {
            try (var in = ContentValidatorTest.class.getResourceAsStream("/content/" + kind + "/" + file)) {
                assertNotNull(in, "bundled content is missing from the classpath: " + kind + "/" + file);
                Files.write(dir.resolve(file), in.readAllBytes());
            }
        }
        return dir.toFile();
    }
}
