package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ingredient lore's rendering -- collapsing, ordering, and the honesty line.
 *
 * <p>Asking a {@code RecipeChoice} what it accepts needs a running server and is boot-gate-only.
 * Turning a list of labels into lore does not, and that is where the defects live, so it is
 * extracted. Same trade {@code MenuIcons} and {@code CollectPlan} make.
 *
 * <p>Each test names the mutation it forces red.
 */
class IngredientLoreTest {

    private static List<String> plain(List<Component> lore) {
        List<String> out = new ArrayList<>();
        for (Component line : lore) out.add(PlainTextComponentSerializer.plainText().serialize(line));
        return out;
    }

    private static IngredientLore.Slot listable(String label) {
        return new IngredientLore.Slot(label, true);
    }

    private static IngredientLore.Slot partial(String label) {
        return new IngredientLore.Slot(label, false);
    }

    @Test
    void identicalIngredientsCOLLAPSEWithACount() {
        // A shaped recipe with nine of one thing would otherwise be nine identical lines and push
        // the item's own lore off the bottom of the screen.
        List<String> lore = plain(IngredientLore.linesOf(List.of(
                listable("Oak Planks"), listable("Oak Planks"),
                listable("Oak Planks"), listable("Oak Planks"))));

        assertEquals(List.of("Needs:", "  4x Oak Planks"), lore);
        // Mutation: emit one line per slot instead of merging -> four "  Oak Planks" lines -> reddens.
    }

    @Test
    void aSingleIngredientHasNOCountPrefix() {
        // "1x Stick" reads like a bug. The prefix appears only when it says something.
        assertEquals(List.of("Needs:", "  Stick"),
                plain(IngredientLore.linesOf(List.of(listable("Stick")))));
        // Mutation: drop the `count > 1` guard -> "  1x Stick" -> reddens.
    }

    @Test
    void distinctIngredientsKEEPTheirRecipeOrder() {
        // For a shaped recipe this is top-left to bottom-right, which is how a player reads the
        // grid. A HashMap here would shuffle it between JVM runs.
        List<String> lore = plain(IngredientLore.linesOf(List.of(
                listable("Iron Ingot"), listable("Stick"), listable("Stick"), listable("Diamond"))));

        assertEquals(List.of("Needs:", "  Iron Ingot", "  2x Stick", "  Diamond"), lore);
        // Mutation: swap LinkedHashMap for HashMap -> the order becomes hash order -> reddens
        // (Iron Ingot / Stick / Diamond do not hash into insertion order).
    }

    @Test
    void anUnlistableIngredientADDSTheHonestyLine() {
        // THE line. A short list shown as though it were whole is a lie a player acts on.
        List<String> lore = plain(IngredientLore.linesOf(List.of(
                listable("Iron Ingot"), partial("something"))));

        assertEquals(4, lore.size());
        assertEquals("  (this recipe accepts more than can be listed)", lore.get(3));
        assertEquals("Needs:", lore.get(0));
        // Mutation: drop the honesty line -> size 3 -> reddens.
    }

    @Test
    void aFullyListableRecipeGetsNOHonestyLine() {
        // The other half, and the one that makes the test above discriminating: if the line were
        // always appended, the test above would pass just as green.
        List<String> lore = plain(IngredientLore.linesOf(List.of(
                listable("Iron Ingot"), listable("Stick"))));

        assertEquals(List.of("Needs:", "  Iron Ingot", "  Stick"), lore);
        for (String line : lore) {
            assertFalse(line.contains("more than can be listed"), "unexpected honesty line: " + line);
        }
        // Mutation: always append the honesty line -> reddens here, not above.
    }

    @Test
    void aLabelThatIsSometimesUnlistableIsTreatedAsUNLISTABLE() {
        // The merge is AND, not first-wins. If the same label arrives once listable and once not,
        // the honest answer is "not" -- and it must not depend on which came first.
        List<String> partialFirst = plain(IngredientLore.linesOf(List.of(
                partial("Planks"), listable("Planks"))));
        List<String> listableFirst = plain(IngredientLore.linesOf(List.of(
                listable("Planks"), partial("Planks"))));

        assertEquals(partialFirst, listableFirst, "the verdict must not depend on slot order");
        assertTrue(partialFirst.get(partialFirst.size() - 1).contains("more than can be listed"));
        // Mutation: `putIfAbsent` instead of `merge(.., a && b)` -> listableFirst loses the honesty
        // line while partialFirst keeps it -> the equality assertion reddens. This is the test that
        // catches an order-dependent truth claim, which is invisible in any single-order test.
    }

    @Test
    void noIngredientsMeansNoLinesAtAll() {
        // An inert entry wants to talk about the grid, not about materials, so the caller gets an
        // empty list and decides what to say instead. A bare "Needs:" heading with nothing under it
        // would be worse than silence.
        assertEquals(List.of(), IngredientLore.linesOf(List.of()));
        assertEquals(List.of(), IngredientLore.linesOf(null));
        // Mutation: emit the "Needs:" header before the empty check -> reddens.
    }

    @Test
    void joinNamesUpToThreeAlternativesThenCountsTheRest() {
        assertEquals("Oak Planks", IngredientLore.join(List.of("Oak Planks")));
        assertEquals("Oak Planks or Birch Planks",
                IngredientLore.join(List.of("Oak Planks", "Birch Planks")));
        assertEquals("A or B or C", IngredientLore.join(List.of("A", "B", "C")),
                "exactly three still lists them all");
        assertEquals("A or B or C or 1 more", IngredientLore.join(List.of("A", "B", "C", "D")),
                "the fourth tips it into a count");
        assertEquals("A or B or C or 3 more",
                IngredientLore.join(List.of("A", "B", "C", "D", "E", "F")));
        // Mutation: `subList(0, MAX)` -> `subList(0, MAX - 1)`, or `size() - MAX` -> `size()` ->
        // reddens on the "or N more" cases. The boundary at exactly three is the one that matters:
        // an off-by-one there prints "A or B or C or 0 more".
    }

    @Test
    void joinOfNothingIsSomethingRatherThanAnEmptyLabel() {
        // A blank label renders as a bullet with no text, which reads as a rendering bug rather
        // than as "we do not know".
        assertEquals("something", IngredientLore.join(List.of()));
        // Mutation: return "" -> reddens.
    }
}
