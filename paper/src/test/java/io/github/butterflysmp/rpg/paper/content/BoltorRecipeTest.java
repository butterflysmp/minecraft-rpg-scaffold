package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.recipe.RecipeDefinition;
import io.github.butterflysmp.rpg.core.recipe.RecipeRegistry;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE BOLTOR'S RECIPE (Ben, 2026-11-22: "The Boltor is Netherite tier. What is the crafting recipe for it from the old
 * repo?", and shown it, "keep the old recipe"). The old repo's registerBoltor (CreaperCrusher/Butterfly-SMP at cfde822,
 * crafting/RecipeRegistry.java, READ by the seat): "SNS", "GHG", " S "; S stick, N netherite ingot, G string, H tripwire
 * hook. Vanilla's crossbow is the same shape with an IRON ingot where this has netherite.
 *
 * <p><b>WHICH WEAPON A CRAFT MINTS (READ, RecipeProbe.claimedBy):</b> a matched recipe in OUR namespace is looked up by
 * its id first ({@code forRecipe}); only a recipe that is not ours (vanilla's {@code minecraft:crossbow}) falls through
 * to the result material ({@code forResult}, crossbow.yml's {@code craft_result: crossbow}). So the three grids are
 * three different keys, and this pins the index's answer for each, over the real content.
 */
class BoltorRecipeTest {

    private static Logger quiet() {
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        return log;
    }

    /** The SOURCE tree, never the classpath: a warm target/classes keeps a deleted file (ContentFreshnessTest). */
    private static File content(String name) {
        File f = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(f.exists(), "no content at " + f.getAbsolutePath());
        return f;
    }

    private static RecipeRegistry recipes() {
        return new RecipeLoader(quiet()).loadAll(content("recipes"));
    }

    private static CraftResultIndex index(List<String> problems) {
        Logger log = quiet();
        WeaponRegistry weapons = new WeaponLoader(log).loadAll(content("weapons"));
        List<GearDefinition> all = new ArrayList<>(weapons.all());
        all.addAll(new ShieldLoader(log).loadAll(content("shields")).all());
        all.addAll(new ArmorLoader(log).loadAll(content("armor")).all());
        all.addAll(new ToolLoader(log).loadAll(content("tools")).all());
        return CraftResultIndex.build(all, recipes().all(), problems::add);
    }

    /**
     * VANILLA'S CROSSBOW, READ from the pinned 26.1.2 server jar's {@code data/minecraft/recipe/crossbow.json}:
     * {@code "#&#", "~$~", " # "} with # stick, & iron_ingot, ~ string, $ tripwire_hook. A FIXTURE, copied, because the
     * jar is not on the test classpath; the gate's rows read it on the server.
     */
    private static final List<String> VANILLA_CROSSBOW_SHAPE = List.of("#&#", "~$~", " # ");
    private static final Map<Character, String> VANILLA_CROSSBOW_KEY =
            Map.of('#', "stick", '&', "iron_ingot", '~', "string", '$', "tripwire_hook");

    /** A shape and its key as the grid of material names it asks for, row by row; null for an empty cell. */
    private static List<List<String>> grid(List<String> shape, Map<Character, String> key) {
        List<List<String>> rows = new ArrayList<>();
        for (String row : shape) {
            List<String> cells = new ArrayList<>();
            for (char c : row.toCharArray()) cells.add(c == ' ' ? null : key.get(c));
            rows.add(cells);
        }
        return rows;
    }

    // ------------------------------------------------------------------ the recipe

    /** BEN: "keep the old recipe". The shape and the four ingredients exactly as the old repo's registerBoltor. */
    @Test
    void theOldRecipeLoadsAndMintsTheBoltor() {
        RecipeRegistry recipes = recipes();
        assertTrue(recipes.find("flint_staff").isPresent(), "CONTROL: the recipe walk sees flint_staff");
        RecipeDefinition r = recipes.find("boltor").orElseThrow(() -> new AssertionError("no boltor recipe"));
        assertEquals("boltor", r.mints());
        assertEquals(List.of("SNS", "GHG", " S "), r.shape());
        assertEquals(Map.of('S', "stick", 'N', "netherite_ingot", 'G', "string", 'H', "tripwire_hook"), r.ingredients());
        assertFalse(r.fitsInTwoByTwo(), "three by three: never the player's 2x2 grid");
    }

    // ------------------------------------------------------------------ the three grids

    /**
     * THE THREE GRIDS EACH MINT THEIR OWN WEAPON AND NEVER ANOTHER'S. The Breacher's and the Boltor's are ours, found by
     * recipe id; vanilla's crossbow is not ours, so it is found by its result material, which crossbow.yml claims.
     * Neither our recipe id is claimed through the material map, and nothing is contested.
     */
    @Test
    void eachGridMintsItsOwnWeapon() {
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = index(problems);
        assertEquals(Optional.of("boltor"), index.forRecipe("boltor").map(GearDefinition::id), "the Boltor's grid");
        assertEquals(Optional.of("breacher"), index.forRecipe("breacher").map(GearDefinition::id), "the Breacher's grid");
        assertEquals(Optional.of("crossbow"), index.forResult("crossbow").map(GearDefinition::id),
                "vanilla's crossbow grid falls through to the material, claimed by crossbow.yml");
        assertEquals(Optional.empty(), index.forRecipe("crossbow"),
                "no recipe of ours is named crossbow: vanilla's key never reaches the recipe map");
        assertEquals(0, index.contested(), "nothing contested: " + problems);
        assertEquals(0, index.recipesDropped(), "every recipe names a real weapon: " + problems);
    }

    /**
     * THE IRON VARIANT IS VANILLA'S CROSSBOW, AND THE BOLTOR'S GRID IS NOT. The Boltor's grid with netherite swapped for
     * iron is, cell for cell, vanilla's crossbow (so the gate's iron row mints the Crossbow, never a Boltor); unswapped
     * it differs in exactly the one cell. And no two of our recipes ask for the same grid.
     */
    @Test
    void theBoltorGridDiffersFromVanillasCrossbowInTheNetheriteCellOnly() {
        RecipeDefinition boltor = recipes().find("boltor").orElseThrow();
        List<List<String>> vanilla = grid(VANILLA_CROSSBOW_SHAPE, VANILLA_CROSSBOW_KEY);
        List<List<String>> ours = grid(boltor.shape(), boltor.ingredients());
        assertNotEquals(vanilla, ours, "the Boltor's grid must not BE vanilla's crossbow");

        Map<Character, String> ironed = new HashMap<>(boltor.ingredients());
        ironed.put('N', "iron_ingot");
        assertEquals(vanilla, grid(boltor.shape(), ironed), "with iron in place of netherite it is vanilla's crossbow");

        int differing = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (!java.util.Objects.equals(vanilla.get(row).get(col), ours.get(row).get(col))) differing++;
            }
        }
        assertEquals(1, differing, "exactly the netherite cell differs");

        List<List<List<String>>> seen = new ArrayList<>();
        for (RecipeDefinition r : recipes().all()) {
            List<List<String>> g = grid(r.shape(), r.ingredients());
            assertFalse(seen.contains(g), "two of our recipes ask for one grid: " + r.id());
            seen.add(g);
        }
    }

    // ------------------------------------------------------------------ the shipped counts

    /**
     * THE SHIPPED RECIPE COUNT, recounted: SEVEN files (breacher, boltor, crystal_shard_staff, cursed_emerald,
     * flint_staff, lapis_staff, living_lantern), all indexed, so the boot's Loaded line reads "7 recipes". The material
     * axis does not move: a recipe never populates it (CraftResultIndexTest.aRecipeClaimDoesNotPopulateTheMaterialMap),
     * so Mint-on-craft and Convert-on-arrival stay as BootCountLinesTest measures them. CONTROL: the files on disk.
     */
    @Test
    void sevenRecipesShipAndAllAreIndexed() {
        File[] files = content("recipes").listFiles((d, n) -> n.endsWith(".yml"));
        // 2026-11-25: 7 -> 8, the Bone Whistle's recipe (PR 5); recount: ls content/recipes/*.yml | wc -l = 8.
        assertEquals(8, files == null ? -1 : files.length, "CONTROL: the files on disk");
        assertEquals(8, recipes().size(), "every file loads");
        assertEquals(8, index(new ArrayList<>()).recipesIndexed(), "every recipe is indexed against a weapon");
    }
}
