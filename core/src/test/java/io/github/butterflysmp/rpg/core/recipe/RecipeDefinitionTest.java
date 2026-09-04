package io.github.butterflysmp.rpg.core.recipe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shape rules, and the reason they are rules rather than hopes.
 *
 * <p><b>Every refusal here is a BOOT ABORT if it is removed.</b> {@code ShapedRecipe.shape},
 * {@code ShapedRecipe.setIngredient} and {@code NamespacedKey}'s constructor all throw
 * {@code IllegalArgumentException} from {@code Preconditions.checkArgument}; they do not report.
 * A definition that reaches the registrar unvalidated therefore takes the server down, instead of
 * costing one named, skipped file the way every other loader's bad input does. That is what these
 * tests are protecting -- not tidiness.
 *
 * <p>Each test names the mutation it forces red.
 */
class RecipeDefinitionTest {

    /** The Flint Staff's real shape: flint over two sticks. */
    private static RecipeDefinition flintStaff() {
        return new RecipeDefinition("flint_staff", List.of("F", "S", "S"),
                Map.of('F', "flint", 'S', "stick"), "flint_staff");
    }

    private static RecipeDefinition of(String id, List<String> shape,
                                       Map<Character, String> ingredients, String mints) {
        return new RecipeDefinition(id, shape, ingredients, mints);
    }

    // ------------------------------------------------------------- the happy path

    @Test
    void theFlintStaffShapeIsAccepted() {
        RecipeDefinition recipe = flintStaff();

        assertEquals("flint_staff", recipe.id());
        assertEquals("flint_staff", recipe.mints());
        assertEquals(List.of("F", "S", "S"), recipe.shape());
        assertEquals("flint", recipe.ingredients().get('F'));
        assertEquals("stick", recipe.ingredients().get('S'));
    }

    @Test
    void aSpaceInTheShapeIsAnEmptyCellAndNeedsNoIngredient() {
        // Mutation: require an ingredient for EVERY character rather than every non-space one.
        // That reddens here, and it makes every recipe smaller than a full 3x3 unauthorable.
        RecipeDefinition recipe = of("necklace", List.of(" I ", " L ", "   "),
                Map.of('I', "iron_ingot", 'L', "lapis_block"), "mana_necklace");

        assertEquals(2, recipe.ingredients().size());
    }

    // ------------------------------------------------------- the 2x2 inventory-grid boundary

    @Test
    void theFlintStaffDoesNotFitTheInventoryGrid() {
        // SAFETY BY SHAPE, NOT BY GUARD. The player's 2x2 grid is the one crafting surface that
        // neither mints nor is hijacked, so a recipe that fits there hands over a plain vanilla
        // item. The staff is three rows tall and cannot reach it.
        assertFalse(flintStaff().fitsInTwoByTwo());
    }

    @Test
    void aTwoByTwoShapeIsRecognised() {
        // Mutation: return a constant false. The boot warning then never fires, and the next person
        // to shorten a shape ships a recipe that silently gives out plain items in the inventory
        // grid -- which is the pre-existing `shears` behaviour, arrived at by accident.
        assertTrue(of("small", List.of("AA", "AA"), Map.of('A', "stick"), "x").fitsInTwoByTwo());
        assertTrue(of("one", List.of("A"), Map.of('A', "stick"), "x").fitsInTwoByTwo());
        assertTrue(of("row", List.of("AA"), Map.of('A', "stick"), "x").fitsInTwoByTwo());
    }

    @Test
    void aShapeTooWideOrTooTallDoesNotFitTheInventoryGrid() {
        // Both bounds, because a mutation dropping either one alone would still pass the other.
        assertFalse(of("tall", List.of("A", "A", "A"), Map.of('A', "stick"), "x").fitsInTwoByTwo());
        assertFalse(of("wide", List.of("AAA"), Map.of('A', "stick"), "x").fitsInTwoByTwo());
    }

    // ------------------------------------------------------------- the shape bounds

    @Test
    void aNonRectangularShapeIsRejected() {
        // Mutation: drop the width comparison. Bukkit's shape() then throws
        // "Crafting recipes must be rectangular" from inside onEnable, aborting the boot.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> of("ragged", List.of("AA", "B"), Map.of('A', "stick", 'B', "flint"), "x"));

        assertTrue(thrown.getMessage().contains("ragged"), thrown.getMessage());
    }

    @Test
    void moreThanThreeRowsIsRejected() {
        // Mutation: relax the row bound. Bukkit: "Crafting recipes should be 1, 2 or 3 rows".
        assertThrows(IllegalArgumentException.class,
                () -> of("tall", List.of("A", "A", "A", "A"), Map.of('A', "stick"), "x"));
    }

    @Test
    void aRowWiderThanThreeIsRejected() {
        // Mutation: relax the column bound. Same boot abort, from the other side of shape().
        assertThrows(IllegalArgumentException.class,
                () -> of("wide", List.of("AAAA"), Map.of('A', "stick"), "x"));
    }

    @Test
    void anEmptyShapeIsRejected() {
        // Mutation: allow an absent shape through. A recipe with no rows registers as nothing.
        assertThrows(IllegalArgumentException.class,
                () -> of("empty", List.of(), Map.of(), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> of("empty", null, Map.of(), "x"));
    }

    @Test
    void aShapeOfNothingButSpacesIsRejected() {
        // Mutation: drop the anyCell check. A recipe of blanks would match an EMPTY GRID, which
        // is a recipe that fires whenever a player opens a crafting table with nothing in it.
        assertThrows(IllegalArgumentException.class,
                () -> of("blank", List.of("   ", "   "), Map.of(), "x"));
    }

    // ------------------------------------------------------------- shape vs ingredients

    @Test
    void everyNonSpaceShapeCharacterNeedsAnIngredient() {
        // Mutation: allow the missing mapping. shape() seeds its map with a NULL for 'B' and
        // setIngredient never replaces it -- the recipe registers half-formed and matches nothing,
        // with nothing anywhere saying why.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> of("gap", List.of("AB"), Map.of('A', "stick"), "x"));

        assertTrue(thrown.getMessage().contains("'B'"), thrown.getMessage());
    }

    @Test
    void anIngredientKeyedOnACharacterAbsentFromTheShapeIsRejected() {
        // Mutation: pass it through. Bukkit's setIngredient throws
        // "Symbol does not appear in the shape" -- boot abort.
        assertThrows(IllegalArgumentException.class,
                () -> of("stray", List.of("A"), Map.of('A', "stick", 'Z', "flint"), "x"));
    }

    @Test
    void anIngredientKeyedOnASpaceIsRejected() {
        // Mutation: allow it. Bukkit: "Space in recipe shape must represent no ingredient".
        assertThrows(IllegalArgumentException.class,
                () -> of("spaced", List.of("A "), Map.of('A', "stick", ' ', "flint"), "x"));
    }

    @Test
    void aBlankIngredientMaterialIsRejected() {
        // Mutation: treat blank as absent. Material.matchMaterial("") is null, so the recipe is
        // silently dropped at registration and the author hunts a recipe that never existed.
        assertThrows(IllegalArgumentException.class,
                () -> of("blankmat", List.of("A"), Map.of('A', "  "), "x"));
    }

    // ------------------------------------------------------------- the id, which becomes a key

    @Test
    void anIdThatIsNotALegalKeyValueIsRejected() {
        // Mutation: drop the pattern check. new NamespacedKey(plugin, "flint staff") lower-cases
        // and then THROWS on the space -- inside onEnable, taking the whole server down over one
        // badly named file.
        assertThrows(IllegalArgumentException.class,
                () -> of("flint staff", List.of("A"), Map.of('A', "stick"), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> of("FlintStaff", List.of("A"), Map.of('A', "stick"), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> of("", List.of("A"), Map.of('A', "stick"), "x"));
    }

    @Test
    void theLegalKeyCharactersAreAccepted() {
        // The other side of the same rule: NamespacedKey allows dots, underscores, hyphens and
        // forward slashes, so the pattern must not be narrower than Bukkit's.
        assertDoesNotThrow(() -> of("a.b_c-d/e0", List.of("A"), Map.of('A', "stick"), "x"));
    }

    // ------------------------------------------------------------- mints

    @Test
    void blankMintsIsRejected() {
        // Mutation: read blank as absent. The recipe registers, hands the player a plain vanilla
        // item forever, and reports nothing -- the silent failure CraftResultToken's blank-throw
        // exists to prevent, on the axis where the field is the entire point.
        assertThrows(IllegalArgumentException.class,
                () -> of("nomint", List.of("A"), Map.of('A', "stick"), "  "));
        assertThrows(IllegalArgumentException.class,
                () -> of("nomint", List.of("A"), Map.of('A', "stick"), null));
    }

    // ------------------------------------------------------------- normalisation and copying

    @Test
    void ingredientTokensAreNormalisedTheSameWayCraftResultTokensAre() {
        // Asserts the STORED token, not a lookup. Material.matchMaterial copes with all three
        // spellings, so a test that went through it could not fail -- and a test that cannot fail
        // is worth nothing however green.
        RecipeDefinition recipe = of("norm", List.of("AB"),
                Map.of('A', "MINECRAFT:Flint", 'B', "  Stick  "), "x");

        assertEquals("flint", recipe.ingredients().get('A'));
        assertEquals("stick", recipe.ingredients().get('B'));
    }

    @Test
    void theShapeIsDefensivelyCopiedFromTheCaller() {
        // Mutation: `shape = shape` instead of List.copyOf. The loader builds this from a mutable
        // YAML structure, so a shared reference is a definition that can change after validation --
        // which is validation that proves nothing.
        //
        // THE INGREDIENT MAP IS DELIBERATELY NOT ASSERTED HERE. It cannot be aliased to the
        // caller's map whatever the constructor does, because normalisation rebuilds it into a
        // fresh LinkedHashMap first. An assertion on it would pass with Map.copyOf REMOVED --
        // verified by mutation, 2026-09-03 -- and a test that cannot fail is worth nothing however
        // green. What Map.copyOf actually buys on that field is immutability, which is the next
        // test.
        List<String> shape = new ArrayList<>(List.of("A"));

        RecipeDefinition recipe = of("copy", shape, Map.of('A', "stick"), "x");
        shape.add("BB");

        assertEquals(List.of("A"), recipe.shape());
    }

    @Test
    void theStoredCollectionsAreUnmodifiable() {
        // Mutation: hand back `normalised` (a LinkedHashMap) rather than Map.copyOf of it, or the
        // caller's list rather than List.copyOf. Either reddens here. This is the assertion that
        // witnesses Map.copyOf at all -- see the note above for why the aliasing one does not.
        RecipeDefinition recipe = flintStaff();

        assertThrows(UnsupportedOperationException.class, () -> recipe.ingredients().put('Z', "x"));
        assertThrows(UnsupportedOperationException.class, () -> recipe.shape().add("ZZ"));
    }
}
