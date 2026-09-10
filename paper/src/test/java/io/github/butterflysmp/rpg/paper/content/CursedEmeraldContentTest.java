package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.recipe.RecipeRegistry;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>THE CURSED EMERALD'S FIVE FILES ACTUALLY LOAD, AND IT SHIPS WITH NO BOOT WARNING.</b>
 *
 * <p>Modelled on {@link VolleyFixtureTest}, and it exists because that comparison was embarrassing:
 * <b>a never-craftable dev instrument had five permanent rows while a shipped, craftable weapon had
 * none.</b> {@code volley_stone} is reachable only through {@code /rpg give} behind a permission;
 * {@code cursed_emerald} is minted from a recipe players will use.
 *
 * <h2>WHY THE REST OF THE SUITE DOES NOT COVER THESE FILES</h2>
 *
 * {@code ContentValidatorTest} and the loader tests read bundled content through a
 * {@code copyBundled} helper that <b>names each file explicitly</b>. So a green run over the whole
 * content package proves nothing about a file it does not name -- and every loader here is
 * fail-soft, meaning a typo surfaces as a named, skipped file at boot and is silent to every other
 * test. <b>A check that did not run looks exactly like a check that passed.</b>
 *
 * <p>These rows were first written as a throwaway scratchpad program while authoring the content.
 * That closed the gap for one afternoon and then exited, which is not cover; they are permanent
 * here.
 *
 * <h2>WHAT IT CANNOT SEE</h2>
 *
 * Every visual question. Whether six bare damage numbers two ticks apart are legible, whether the
 * wind-up chime reads as a telegraph, whether a missed volley's bursts are visible at the render
 * cap -- none of that is reachable from here. That is {@code GATE-cursed-emerald.md}'s job, and a
 * green run of this file is not evidence for any of it.
 *
 * <p>Each row names the mutation it forces red.
 */
class CursedEmeraldContentTest {

    /** Quiet: these loaders are fail-soft and log warnings, and the rows below assert on state. */
    private static Logger quiet() {
        Logger log = Logger.getLogger("CursedEmeraldContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.setLevel(Level.OFF);
        return log;
    }

    private static Path contentRoot() {
        try {
            var url = CursedEmeraldContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static File dir(String name) {
        return new File(contentRoot().resolve(name).toString());
    }

    private static WeaponRegistry shippedWeapons() {
        return new WeaponLoader(quiet()).loadAll(dir("weapons"));
    }

    private static VisualRegistry shippedVisuals() {
        return new VisualLoader(quiet()).loadAll(dir("visuals"));
    }

    private static ElementRegistry shippedElements() {
        return new ElementLoader(quiet()).loadAll(dir("elements"));
    }

    private static StatusRegistry shippedStatuses() {
        return new StatusLoader(quiet()).loadAll(dir("statuses"));
    }

    private static RecipeRegistry shippedRecipes() {
        return new RecipeLoader(quiet()).loadAll(dir("recipes"));
    }

    /**
     * The weapon, behind a POSITIVE CONTROL that comes first.
     *
     * <p>Everything below is a lookup in a registry built by walking a directory. A walk that found
     * nothing returns an EMPTY registry, and "cursed_emerald is absent" would then be reported
     * identically whether the file is broken or the walk is blind. The control asserts a
     * known-present weapon that has nothing to do with this slice.
     */
    private static WeaponDefinition cursedEmerald() {
        var weapons = shippedWeapons();
        assertTrue(weapons.find("lapis_staff").isPresent(),
                "CONTROL FAILED: the weapon walk cannot even see lapis_staff, so it is blind -- and "
                        + "a blind walk reports a missing emerald exactly like a broken one");
        return weapons.find("cursed_emerald").orElseThrow(
                () -> new AssertionError("cursed_emerald.yml did not load. The loaders are fail-soft, "
                        + "so a typo in it is a named, skipped file at boot and silent everywhere "
                        + "else -- this row is the only thing that can see it"));
    }

    private static TriggerBinding trigger(WeaponDefinition weapon) {
        return weapon.trigger("right_click").orElseThrow(
                () -> new AssertionError("cursed_emerald has no 'right_click' trigger"));
    }

    @Test
    void allFiveFilesLoadThroughTheRealLoaders() {
        var emerald = cursedEmerald();
        assertEquals("emerald", emerald.material(),
                "the MATERIAL is the lever for indestructibility -- see the file");
        assertEquals("kinetic", emerald.element(),
                "considered and chosen over `nature`; kinetic declares no applies_status, which is "
                        + "what keeps the damage numbers bare and gate row CE2 readable");
        assertEquals(0.0, emerald.attackDamage(), 1e-9,
                "AUTHORED 0, not omitted -- WeaponLoader defaults to 0.0, so absence and intent "
                        + "resolve alike and the decision would be invisible");

        var visuals = shippedVisuals();
        assertTrue(visuals.find("emerald_windup").isPresent(), "emerald_windup.yml did not load");
        assertTrue(visuals.find("emerald_beam").isPresent(), "emerald_beam.yml did not load");
        assertTrue(visuals.find("emerald_impact").isPresent(), "emerald_impact.yml did not load");

        var recipe = shippedRecipes().find("cursed_emerald").orElseThrow(
                () -> new AssertionError("recipes/cursed_emerald.yml did not load"));
        assertEquals("cursed_emerald", recipe.mints(),
                "the claim points recipe -> weapon, never the reverse");
        // Mutation: rename any of the five files -> the matching lookup empties and this reddens.
    }

    @Test
    void everyVISUALTheWeaponNamesActuallyExists() {
        var emerald = cursedEmerald();
        var visuals = shippedVisuals();
        var ability = trigger(emerald).ability();

        assertEquals(List.of(new EffectSpec.Visual("emerald_windup")), ability.onCast(),
                "the telegraph is the whole reason a one-second commitment is fair, and on_cast is "
                        + "VISUALS ONLY -- AbilitySchema throws by name for anything else there");
        assertTrue(ability.onHit().contains(new EffectSpec.Visual("emerald_impact")),
                "on_hit fires PER SHOT under the wrapper, so this is six impacts per cast");

        var ray = assertInstanceOf(CastSpec.Ray.class,
                ((CastSpec.Volley) ability.cast()).of());
        assertEquals("emerald_beam", ray.beam());

        for (String id : List.of("emerald_windup", "emerald_beam", "emerald_impact")) {
            assertTrue(visuals.find(id).isPresent(), id + " is named by the weapon but did not load");
        }
        // ContentValidator names a dangling beam at boot, but an UNREFERENCED visual is SILENT: the
        // validator walks references outward and has no orphan check. All three of these ARE
        // referenced -- windup by on_cast, beam by the ray, impact by on_hit -- so a dangling one
        // would warn. They are asserted anyway, exactly as the volley fixture's two are, because
        // "would warn" is a claim about a channel nobody is required to read.
    }

    @Test
    void theRecipeMintsTheWeaponAndCannotFitATwoByTwo() {
        var recipes = shippedRecipes();
        assertTrue(recipes.find("lapis_staff").isPresent(),
                "CONTROL FAILED: the recipe walk cannot see lapis_staff, so it is blind");

        var recipe = recipes.find("cursed_emerald").orElseThrow(
                () -> new AssertionError("recipes/cursed_emerald.yml did not load"));

        assertTrue(shippedWeapons().find(recipe.mints()).isPresent(),
                "a recipe whose `mints` cannot be resolved is WARNED ABOUT AND THEN DROPPED by "
                        + "RecipeRegistrar -- it would not register, and the weapon would be "
                        + "uncraftable with only a boot warning to say so");

        assertEquals(3, recipe.shape().size(), "three rows");
        assertFalse(recipe.fitsInTwoByTwo(),
                "SAFE BY SHAPE, NOT BY GUARD. A recipe that fits a 2x2 can be crafted in the "
                        + "player's own inventory grid, which mints NOTHING and hands over a plain "
                        + "vanilla emerald. Three rows cannot fit. Shortening this shape is the "
                        + "edit that would open the hole, and this is what fires when someone does");
        // Mutation: drop a row from the shape -> fitsInTwoByTwo goes true and this reddens.
    }

    /**
     * <b>THE RANGE RULING IS GUARDED, NOT JUST THE VALUE.</b>
     *
     * <p>32 is not a taste call: {@code GATE-volley.md} V3 measured that particles stop rendering at
     * 32 blocks on the CLIENT, so a longer ray would hit a target its beam never reached. The ruling
     * is <i>visible reach equals real reach</i>.
     *
     * <p><b>A bare {@code assertEquals(32, range)} would be satisfied by a coincidence.</b> Someone
     * retuning the weapon to 32 for an unrelated reason, or restoring the plan's original 64 and
     * then tuning back down, would leave this green with the reasoning gone. So the row also
     * requires the FILE to still carry its why -- the number and the argument travel together or the
     * next reader inherits a magic constant.
     */
    @Test
    void theRayRangeIs32AndTheFileSaysWHY() throws IOException {
        var volley = assertInstanceOf(CastSpec.Volley.class, trigger(cursedEmerald()).ability().cast());
        assertEquals(20, volley.windupTicks());
        assertEquals(6, volley.shots());
        assertEquals(2, volley.intervalTicks());

        var ray = assertInstanceOf(CastSpec.Ray.class, volley.of());
        assertEquals(32.0, ray.range(), 1e-9,
                "RULED 32, not the plan's 64. If this is being changed, GATE-cursed-emerald.md's "
                        + "CE4 and CE8 stagings move with it -- both are distance-dependent");

        String source = Files.readString(
                contentRoot().resolve("weapons").resolve("cursed_emerald.yml"), StandardCharsets.UTF_8);
        assertTrue(source.contains("32 BLOCKS"),
                "the file must still record WHAT WAS MEASURED. A number without its reason is a "
                        + "magic constant the next reader will 'improve'");
        assertTrue(source.contains("VISIBLE REACH EQUALS REAL REACH"),
                "and the RULING the number implements, which is the part that survives a retune");
        // Mutation: delete the range comment block from the yml -> this reddens while the value
        // still reads 32, which is the whole point of asserting on the source text as well.
    }

    /**
     * <b>THE MIRROR OF RAKE'S EXPECTED WARNING.</b>
     *
     * <p>{@code VolleyFixtureTest} pins that {@code volley_stone} produces EXACTLY ONE boot warning,
     * because its Rake trigger authors {@code cooldown_ticks: 0} deliberately and that warning is
     * gate row V5's only witness. This row is the opposite claim about a SHIPPED weapon:
     * <b>content players can craft must produce NO warning at all.</b> A warning nobody is meant to
     * act on is how a console stops being read.
     *
     * <p>It is also the row that fires the day someone drops {@code cooldown_ticks} below the derived
     * floor -- the likeliest future edit, since 30 looks redundant next to a mechanism that already
     * enforces it.
     */
    @Test
    void theWeaponProducesNOBootWarning() {
        var problems = new ContentValidator(shippedVisuals(), shippedStatuses(), shippedElements(),
                key -> true, key -> true)
                .validateWeapons(List.of(cursedEmerald()));

        assertTrue(problems.isEmpty(),
                "a SHIPPED weapon must boot clean. Authoring cooldown_ticks below the derived floor "
                        + "(windup 20 + (shots 6 - 1) x interval 2 = 30) emits a Content: warning by "
                        + "design: " + problems);

        var ability = trigger(cursedEmerald()).ability();
        int floor = CastSpec.minimumCooldownTicks(ability.cast());
        assertEquals(30, floor, "the derived floor, from the three numbers the cast owns");
        assertEquals(30, ability.cooldownTicks(),
                "AUTHORED EQUAL TO THE FLOOR, and redundant as a guard on purpose -- "
                        + "max(authored, derived) enforces it either way. It is authored so the "
                        + "TOOLTIP renders 'Cooldown: 1.5s' (it renders the AUTHORED value, so 0 "
                        + "would print no cooldown line on a weapon guarded for 30 ticks), and so "
                        + "this file boots without a permanent expected warning");

        // THE REGISTRIES ARE THE SHIPPED ONES, NOT EMPTY STUBS, for the reason VolleyFixtureTest
        // records: an empty ElementRegistry MANUFACTURES warnings about kinetic being undefined --
        // true of the stub and false of the server -- and a fixture that manufactures problems
        // cannot assert an exact problem count.
        //
        // Mutation: author cooldown_ticks 29 -> one warning appears and this reddens. Author 0 (the
        // plan's original recommendation) -> same.
    }
}
