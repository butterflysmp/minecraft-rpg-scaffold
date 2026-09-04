package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import io.github.butterflysmp.rpg.core.recipe.RecipeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reverse index, and the collision policy it enforces.
 *
 * <p>This is the part of mint-on-craft that CAN be tested. Everything downstream of it -- the mint,
 * the durability gate, the Crafter guard -- needs a running server and is boot-gate-only, which is
 * exactly why the index was keyed on a String token and put in {@code core}: the policy that decides
 * whether a player receives a wrong item gets a real witness rather than a gate row.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftResultIndexTest {

    private static ShieldDefinition shield(String id, String craftResult) {
        return new ShieldDefinition(id, id, Rarity.COMMON, "shield", 0.35, List.of(),
                Optional.ofNullable(craftResult));
    }

    private static ArmorDefinition armor(String id, String material, String craftResult) {
        return new ArmorDefinition(id, id, Rarity.COMMON, material, ArmorSlot.CHEST, 6.0, List.of(),
                Optional.ofNullable(craftResult));
    }

    // ------------------------------------------------------------- the happy path

    @Test
    void aClaimedResultResolvesToItsDefinition() {
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "shield")), List.of(), problem -> fail("unexpected: " + problem));

        assertEquals("shield", index.forResult("shield").orElseThrow().id());
        assertEquals(1, index.size());
        assertEquals(1, index.claimed());
        assertEquals(0, index.contested());
        // Mutation: skip definitions whose claim is present -> reddens with an empty Optional.
    }

    @Test
    void gearThatClaimsNothingIsNotIndexed() {
        // The norm. Most gear never participates, and an index that swept everything in would mint
        // on crafts nobody opted into.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", null), armor("iron_chestplate", "iron_chestplate", null)),
                List.of(), problem -> fail("unexpected: " + problem));

        assertEquals(0, index.size());
        assertEquals(0, index.claimed());
        assertTrue(index.forResult("shield").isEmpty());
        // Mutation: fall back to material() when the claim is absent -> size becomes 2 -> reddens.
        // That mutation is the whole design error this key exists to avoid.
    }

    // ------------------------------------------------------------- the collision

    @Test
    void aContestedResultIsDROPPED_NotFirstWins() {
        // THE policy. Two definitions claiming one item means neither mints, so the player gets a
        // plain item they can craft again -- never a wrong one they own forever.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("first", "shield"), shield("second", "shield")), List.of(), problems::add);

        assertTrue(index.forResult("shield").isEmpty(),
                "a contested result must resolve to NOTHING, not to whichever was loaded first");
        assertEquals(0, index.size());
        assertEquals(2, index.claimed(), "both definitions still made a claim");
        assertEquals(1, index.contested());
        // Mutation: keep the first claimant instead of dropping -> the first assertion reddens.
        // That is the alphabetical-order-decides-the-economy bug.
    }

    @Test
    void aCollisionIsReportedOnceAndNamesEveryClaimant() {
        List<String> problems = new ArrayList<>();
        CraftResultIndex.build(
                List.of(shield("alpha", "shield"), shield("beta", "shield"), shield("gamma", "shield")),
                List.of(), problems::add);

        assertEquals(1, problems.size(), "one message per contested result, not one per claimant");
        String message = problems.get(0);
        assertTrue(message.contains("'alpha'"), message);
        assertTrue(message.contains("'beta'"), message);
        assertTrue(message.contains("'gamma'"), message);
        assertTrue(message.contains("shield"), message);
        // Mutation: never call onProblem -> reddens at 0 != 1. A silently dropped result is the
        // worst outcome available here: no mint, and no way to find out why.
    }

    @Test
    void anUncontestedResultBesideAContestedOneStillResolves() {
        // The collision must not poison the whole index. A content error in one file should cost
        // that one result, not every craft on the server.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("first", "shield"), shield("second", "shield"),
                        armor("iron_chestplate", "iron_chestplate", "iron_chestplate")),
                List.of(), problems::add);

        assertTrue(index.forResult("shield").isEmpty());
        assertEquals("iron_chestplate", index.forResult("iron_chestplate").orElseThrow().id());
        assertEquals(1, index.size());
        assertEquals(1, index.contested());
        // Mutation: abandon the whole index on the first collision -> the second assertion reddens.
    }

    // ---------------------------------------------------------- normalisation

    @Test
    void lookupAndBuildNormaliseTheSameWay() {
        // A lookup that normalised differently from the build would miss EVERY entry, and the only
        // symptom would be "crafting mints nothing" with nothing red and nothing logged.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "minecraft:SHIELD")), List.of(), problem -> fail("unexpected: " + problem));

        assertTrue(index.forResult("shield").isPresent(), "authored with a namespace, looked up without");
        assertTrue(index.forResult("SHIELD").isPresent(), "case must not matter");
        assertTrue(index.forResult("minecraft:shield").isPresent(), "namespace must not matter");
        // Mutation: drop the namespace strip in CraftResultToken.token -> all three redden.
        // Mutation: normalise on build but not on lookup -> the last two redden.
    }

    @Test
    void aClaimSpelledTwoWaysIsSTILLAContest() {
        // The subtle one. Without shared normalisation these two look like different results and
        // BOTH index -- so the same craft resolves to whichever the map happened to hold.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("first", "shield"), shield("second", "MINECRAFT:Shield")), List.of(), problems::add);

        assertEquals(1, index.contested(), "two spellings of one material are one contested result");
        assertTrue(index.forResult("shield").isEmpty());
        assertEquals(1, problems.size());
        // Mutation: normalise at lookup only, not at build -> contested becomes 0 and size 2.
    }

    // ----------------------------------------------------------- the zero guard

    @Test
    void anEmptyIndexIsAnAnswerTheCallerCanSee() {
        // ZERO IS A DEFECT, and the caller cannot warn about what it cannot observe. This pins that
        // size() reports it rather than the index pretending to be populated.
        CraftResultIndex index = CraftResultIndex.build(List.of(), List.of(), problem -> fail("unexpected"));

        assertEquals(0, index.size());
        assertEquals(0, index.claimed());
        assertEquals(0, index.contested());
        assertTrue(index.forResult("shield").isEmpty());
        // Mutation: return a hardcoded positive size -> reddens. The boot's zero-warning depends on
        // this number being honest.
    }

    @Test
    void aNullLookupIsNotACrash() {
        // Reachable: the lookup is fed a material name derived from a crafted item, on a path that
        // runs several times a second. Throwing here would break the preview for everyone rather
        // than declining one mint.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "shield")), List.of(), problem -> fail("unexpected"));

        assertTrue(index.forResult(null).isEmpty());
        // Mutation: drop the null guard -> NullPointerException -> reddens.
    }

    // ------------------------------------------------- the record's own refusal

    @Test
    void aBlankClaimIsRefusedByTheRecordRatherThanReadAsAbsent() {
        // A file that writes `craft_result:` with nothing after it stated an intention it did not
        // finish. Reading that as "does not participate" would leave the author hunting a mint that
        // was never going to happen.
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> shield("shield", "   "));
        assertTrue(blank.getMessage().contains("craft_result"), blank.getMessage());
        assertTrue(blank.getMessage().contains("shield"), blank.getMessage());
        // Mutation: treat blank as Optional.empty() -> reddens. The loader turns this throw into a
        // named, skipped file, which is how the author finds out.
    }

    // ------------------------------------------------- the RECIPE axis (slice 7)

    private static RecipeDefinition recipe(String id, String mints) {
        return new RecipeDefinition(id, List.of("A"), Map.of('A', "stick"), mints);
    }

    @Test
    void aRecipeClaimResolvesToItsGearDefinition() {
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("flint_staff", null)),
                List.of(recipe("flint_staff", "flint_staff")),
                problem -> fail("unexpected: " + problem));

        assertEquals("flint_staff", index.forRecipe("flint_staff").orElseThrow().id());
        assertEquals(1, index.recipesIndexed());
        assertEquals(0, index.recipesDropped());
        // Mutation: skip the recipe walk -> empty Optional -> reddens.
    }

    @Test
    void aRecipeClaimDoesNotPopulateTheMaterialMap() {
        // THE HIGHEST-VALUE TEST IN THE SLICE, and the recipe-axis restatement of
        // gearThatClaimsNothingIsNotIndexed's mutation note.
        //
        // The Flint Staff's material is `stick`. If a recipe claim ALSO wrote itself into the
        // result map -- keyed on the minted gear's material, which is the obvious "make the two
        // axes consistent" move -- then EVERY VANILLA STICK CRAFT IN THE WORLD would mint a Flint
        // Staff. That is precisely the design error the recipe key exists to avoid, and it is
        // invisible until a player crafts sticks.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("flint_staff", null)),
                List.of(recipe("flint_staff", "flint_staff")),
                problem -> fail("unexpected: " + problem));

        assertTrue(index.forResult("shield").isEmpty(), "the recipe axis must not claim a material");
        assertEquals(0, index.size(), "no RESULT is claimed here -- only a recipe");
        assertEquals(0, index.claimed());
        assertTrue(index.forRecipe("flint_staff").isPresent(), "and yet the recipe still resolves");
        // Mutation: back both lookups with one map, or index the recipe under its gear's
        // material -> reddens on the first two assertions.
    }

    @Test
    void twoRecipesMayMintTheSameGear() {
        // NOT a collision. Two recipes minting one weapon is TWO WAYS TO CRAFT IT, and this test is
        // what stops someone copying the result axis's drop-both policy across for symmetry.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("flint_staff", null)),
                List.of(recipe("flint_staff", "flint_staff"), recipe("flint_staff_alt", "flint_staff")),
                problem -> fail("unexpected: " + problem));

        assertEquals("flint_staff", index.forRecipe("flint_staff").orElseThrow().id());
        assertEquals("flint_staff", index.forRecipe("flint_staff_alt").orElseThrow().id());
        assertEquals(2, index.recipesIndexed());
        assertEquals(0, index.recipesDropped());
        // Mutation: apply the contested-result drop to the recipe axis -> both dropped -> reddens.
    }

    @Test
    void aRecipeWhoseMintsNamesNoGearIsDroppedAndReported() {
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", null)),
                List.of(recipe("ghost", "no_such_weapon")),
                problems::add);

        assertTrue(index.forRecipe("ghost").isEmpty());
        assertEquals(0, index.recipesIndexed());
        assertEquals(1, index.recipesDropped());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("no_such_weapon"), problems.get(0));
        // Mutation: index it against a null definition -> the registrar registers a recipe that
        // hands the player a plain item for their materials, forever, and nothing says why.
    }

    @Test
    void theTwoAxesAreIndependent() {
        // One definition reachable BOTH ways, and neither lookup standing in for the other. This is
        // the shape the precedence rule sits on top of: paper asks the recipe axis first.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "shield"), armor("helm", "iron_helmet", null)),
                List.of(recipe("helm_recipe", "helm")),
                problem -> fail("unexpected: " + problem));

        assertEquals("shield", index.forResult("shield").orElseThrow().id());
        assertTrue(index.forRecipe("shield").isEmpty(), "a craft_result claim is not a recipe claim");
        assertEquals("helm", index.forRecipe("helm_recipe").orElseThrow().id());
        assertTrue(index.forResult("iron_helmet").isEmpty(), "a recipe claim is not a result claim");
        // Mutation: let either map fall back to the other -> reddens on the two isEmpty assertions.
    }

    @Test
    void forRecipeOnNullIsEmpty() {
        // Reachable from three of the four call sites: a vanilla recipe, an unkeyed match and an
        // empty pin all arrive here as null. Throwing would break the preview for every player
        // rather than declining one mint.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "shield")), List.of(), problem -> fail("unexpected"));

        assertTrue(index.forRecipe(null).isEmpty());
        // Mutation: drop the null guard -> NullPointerException -> reddens.
    }

    @Test
    void theRecipeCountsAreSeparateFromTheResultCounts() {
        // The boot line prints both. One shared counter would let it read self-consistent and
        // wrong -- the exact failure the three-number positive control was written to prevent.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("alpha", "shield"), shield("beta", "shield"), armor("h", "x", null)),
                List.of(recipe("good", "h"), recipe("bad", "nothing")),
                problems::add);

        assertEquals(0, index.size(), "both shield claimants contested and dropped");
        assertEquals(2, index.claimed());
        assertEquals(1, index.contested());
        assertEquals(1, index.recipesIndexed());
        assertEquals(1, index.recipesDropped());
        assertEquals(2, problems.size(), "one for the contested result, one for the ghost recipe");
        // Mutation: fold recipesDropped into contested -> reddens on two numbers at once.
    }
}
