package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.archetype.Archetype;
import io.github.butterflysmp.rpg.core.element.Element;
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
        registry.register(new AbilityDefinition("solar_grenade", "Solar Grenade", Element.SOLAR,
                "hunter", 200, new ResourceCost("energy", 40),
                new CastSpec.Projectile(1.2, 0.03, 100), onHit));
        return registry;
    }

    private static VisualRegistry visualsWith(String... ids) {
        var registry = new VisualRegistry();
        for (String id : ids) {
            registry.register(new VisualDefinition(id,
                    List.of(new VisualSpec.Particles(Particle.FLAME, 40, 0.6))));
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

    private static ContentValidator validator(VisualRegistry visuals, StatusRegistry statuses) {
        return new ContentValidator(visuals, statuses, ALL_EXIST, ALL_EXIST);
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
                        new EffectSpec.Damage(2, Element.SOLAR),    // no reference
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
                        new EffectSpec.Damage(6, Element.SOLAR),           // no reference
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
                new EffectSpec.Damage(12, Element.SOLAR),
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

    /** A well-formed key that names no effect. Only a live Registry knows; hence the seam. */
    @Test
    void potionTypeThatNamesNoEffectIsReported() {
        var statuses = new StatusRegistry();
        statuses.register(new StatusDefinition.Potion("sluggish", NamespacedKey.minecraft("slowness")));

        var problems = new ContentValidator(visualsWith(), statuses, NONE_EXIST, ALL_EXIST)
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

        var problems = new ContentValidator(visuals, statusesWith(), ALL_EXIST, NONE_EXIST)
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
        var statuses = new StatusLoader(log).loadAll(copyBundled(dir, "statuses", "scorch.yml"));

        assertEquals(1, abilities.size(), "bundled ability failed to load");
        assertEquals(1, visuals.size(), "bundled visual failed to load");
        assertEquals(1, statuses.size(), "bundled status failed to load");

        var problems = validator(visuals, statuses).validate(abilities);

        assertTrue(problems.isEmpty(), problems.toString());
    }

    // --- archetype -> ability cross-reference, behind the Predicate<String> seam ---

    private static ContentValidator bareValidator() {
        return new ContentValidator(visualsWith(), statusesWith(), ALL_EXIST, ALL_EXIST);
    }

    @Test
    void archetypeWhoseAbilitiesAllExistProducesNoProblems() {
        var hunter = new Archetype("hunter", "Hunter", List.of("solar_grenade", "solar_lance"));

        var problems = bareValidator().validateArchetypes(List.of(hunter), id -> true);

        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void archetypeNamingAnUnknownAbilityIsReported() {
        var hunter = new Archetype("hunter", "Hunter", List.of("solar_grenade", "typo_lance"));
        // Everything resolves except the typo.
        Predicate<String> exists = id -> id.equals("solar_grenade");

        var problems = bareValidator().validateArchetypes(List.of(hunter), exists);

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("hunter"), problems.toString());
        assertTrue(problems.get(0).contains("typo_lance"), problems.toString());
    }

    /**
     * The case a per-id check passes and still gets wrong: every id dangles, so there
     * is no "remaining id" to complain about -- but the class is unplayable. This must
     * report the empty resolved set on top of the per-id problems.
     */
    @Test
    void archetypeWithNoExistingAbilityIsReportedAsUnplayable() {
        var ghost = new Archetype("ghost", "Ghost", List.of("gone_a", "gone_b"));

        var problems = bareValidator().validateArchetypes(List.of(ghost), id -> false);

        // two dangling ids + one "nobody can play this class"
        assertEquals(3, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("nobody can play")), problems.toString());
    }

    /** Copies one shipped resource into its own directory and returns that directory. */
    private static File copyBundled(Path root, String kind, String file) throws IOException {
        Path dir = Files.createDirectories(root.resolve(kind));
        try (var in = ContentValidatorTest.class.getResourceAsStream("/content/" + kind + "/" + file)) {
            assertNotNull(in, "bundled content is missing from the classpath: " + kind + "/" + file);
            Files.write(dir.resolve(file), in.readAllBytes());
        }
        return dir.toFile();
    }
}
