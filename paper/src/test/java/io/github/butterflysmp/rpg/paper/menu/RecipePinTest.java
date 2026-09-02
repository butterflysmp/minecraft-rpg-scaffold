package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The recipe pin: "you receive what you were shown".
 *
 * <p>The bulk loop re-matched the grid on every pass, so a grid loaded with planks and 50 iron
 * crafted its shields, ran out of planks, re-matched to the IRON NUGGET recipe and converted the
 * remaining ingots. The pin closes that by refusing to commit anything the preview did not show.
 *
 * <p>{@code CraftingMenu} cannot be constructed without a server, but these two are STATIC and touch
 * nothing but their arguments — so the decision has a real witness even though the menu around it
 * does not. {@code NamespacedKey.minecraft(...)} needs no plugin and no server.
 *
 * <p>Each test names the mutation it forces red.
 */
class RecipePinTest {

    /**
     * A keyed recipe. {@code Recipe} declares only {@code getResult()}, so this is the whole
     * interface — which is exactly why the production code must narrow through {@code Keyed}.
     */
    private record KeyedRecipe(NamespacedKey key) implements Recipe, Keyed {
        @Override public ItemStack getResult() { return null; }
        @Override public NamespacedKey getKey() { return key; }
    }

    /** A recipe with NO key — what {@code MerchantRecipe} is, and what the narrowing must survive. */
    private record UnkeyedRecipe() implements Recipe {
        @Override public ItemStack getResult() { return null; }
    }

    private static final NamespacedKey SHIELD = NamespacedKey.minecraft("shield");
    private static final NamespacedKey NUGGET = NamespacedKey.minecraft("iron_nugget");

    // ------------------------------------------------------------------ identity

    @Test
    void aKeyedRecipeYieldsItsKey() {
        assertEquals(Optional.of(SHIELD), CraftingMenu.identityOf(new KeyedRecipe(SHIELD)));
        // Mutation: return Optional.empty() always -> every craft refuses -> reddens.
    }

    @Test
    void anUnkeyedRecipeYieldsNothingRatherThanThrowing() {
        // Recipe is NOT Keyed on this API — verified by javap against the pinned jar — so a cast
        // would be a ClassCastException inside a click handler.
        assertEquals(Optional.empty(), CraftingMenu.identityOf(new UnkeyedRecipe()));
        assertEquals(Optional.empty(), CraftingMenu.identityOf(null));
        // Mutation: cast instead of instanceof -> throws -> reddens.
    }

    // --------------------------------------------------------------- the pin itself

    @Test
    void theSameRecipeMatchesItsPin() {
        assertTrue(CraftingMenu.matches(new KeyedRecipe(SHIELD), Optional.of(SHIELD)));
        // Two DIFFERENT NamespacedKey instances with the same value must match: the pin is captured
        // from one match and compared against a later one, so this relies on value equality.
        assertTrue(CraftingMenu.matches(
                new KeyedRecipe(NamespacedKey.minecraft("shield")), Optional.of(SHIELD)));
        // Mutation: compare with == instead of equals -> the second assertion reddens.
    }

    @Test
    void aDIFFERENTRecipeIsREFUSED() {
        // THE defect. The grid ran out of planks and now matches iron nuggets; the pin says shield.
        assertFalse(CraftingMenu.matches(new KeyedRecipe(NUGGET), Optional.of(SHIELD)));
        // Mutation: return true unconditionally -> reddens. That mutation converts a player's
        // remaining ingots into nuggets.
    }

    @Test
    void anUnkeyedMatchCanNeverSatisfyAPin() {
        // It cannot be pinned, so it cannot be confirmed as the same recipe. Bulk stops rather than
        // proceeding blind — the fail-safe direction.
        assertFalse(CraftingMenu.matches(new UnkeyedRecipe(), Optional.of(SHIELD)));
        // Mutation: treat an absent identity as matching -> reddens, and the loop would run on with
        // no idea what it is crafting.
    }

    @Test
    void anEmptyPinCommitsNOTHING() {
        // The preview showed nothing, so there is nothing the player was shown to receive. Includes
        // the null-match case: no recipe at all cannot satisfy an empty pin either.
        assertFalse(CraftingMenu.matches(new KeyedRecipe(SHIELD), Optional.empty()));
        assertFalse(CraftingMenu.matches(null, Optional.empty()));
        // Mutation: treat an empty pin as "anything goes" -> reddens, and a stale preview would
        // craft whatever the grid currently makes.
    }

    @Test
    void noRecipeMatchesNoPin() {
        assertFalse(CraftingMenu.matches(null, Optional.of(SHIELD)));
        // Mutation: drop the null handling in identityOf -> NullPointerException -> reddens.
    }
}
