package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "What does this take?", as lore lines, and an honest admission when it cannot say.
 *
 * <h2>WHAT THE PINNED JAR ACTUALLY OFFERS -- verified with javap, not remembered</h2>
 *
 * <ul>
 *   <li>{@code RecipeChoice} extends {@code Predicate<ItemStack>}, so {@code test} is total over
 *       every implementation. That is what {@code RecipeProbe} inverts against, and it is why an
 *       unlistable ingredient is still perfectly craftable.
 *   <li><b>{@code RecipeChoice.getItemStack()} is DEPRECATED -- since 1.13.1, confirmed with
 *       {@code javap -v} on the pinned jar, which reports {@code Deprecated: true}.</b> It is the
 *       obvious way to get one representative item for a choice that cannot be enumerated, and this
 *       class deliberately does NOT call it. It is deprecated for the same reason
 *       {@code getIngredientMap} is: it flattens a choice to a single stack, which is exactly the
 *       narrowing {@code RecipeProbe}'s javadoc warns about. Reaching for it here would put a
 *       deprecated flattening call into the one place whose entire job is to be honest about
 *       flattening. An unlistable ingredient is described as <i>"something"</i> and the honesty
 *       line carries the rest.
 *   <li>{@code MaterialChoice.getChoices()} returns {@code List<Material>}, and
 *       {@code ItemTypeRecipeChoiceImpl EXTENDS MaterialChoice}, so one {@code instanceof} covers
 *       both the material and the item-type forms.
 *   <li>{@code ExactChoice.getChoices()} returns {@code List<ItemStack>}.
 *   <li>{@code PredicateRecipeChoice} is <b>package-private</b> in {@code io.papermc.paper.potion}
 *       -- it cannot be named from here at all, only fallen through to. It exposes
 *       {@code itemStackPredicate()} and nothing enumerable.
 * </ul>
 *
 * <h2>THE LIMITATION IS ENUMERATION, NOT CRAFTABILITY</h2>
 *
 * A choice that cannot list its alternatives is still fully testable, so the recipe is fully
 * probeable, countable and craftable. What is lost is only the ability to write
 * <i>"oak, birch, spruce or jungle planks"</i> instead of <i>"planks (and others)"</i>.
 *
 * <p>Which is why the honesty line matters: a short list shown as though it were whole is a lie a
 * player acts on. <i>"These are the materials"</i> versus <i>"these are the materials I can
 * list."</i>
 */
final class IngredientLore {

    /** How many alternatives to name before giving up and saying "or others". */
    private static final int MAX_ALTERNATIVES = 3;

    private IngredientLore() {}

    /**
     * Can every ingredient of this recipe be fully enumerated for display?
     *
     * <p>False does NOT mean the recipe is unusable -- see the class javadoc. It means the lore must
     * admit the list is partial. Used by {@link RecipeCatalogue} for its boot count, and by
     * {@link #of} for the line it appends.
     */
    static boolean fullyListable(Recipe recipe) {
        List<RecipeChoice> ingredients = RecipeProbe.ingredientsOf(recipe);
        if (ingredients == null) return true;    // no ingredients at all is not a PARTIAL list
        for (RecipeChoice choice : ingredients) {
            if (choice != null && !enumerable(choice)) return false;
        }
        return true;
    }

    /**
     * The ingredient lines for a recipe, as chrome to sit above the item's own lore.
     *
     * <p>Identical ingredients are COLLAPSED WITH A COUNT -- "4x Oak Planks", not four lines saying
     * "Oak Planks". A shaped recipe with nine of one thing would otherwise fill the tooltip and push
     * everything else off the bottom of the screen.
     *
     * <p>Returns an empty list for a recipe with no ingredients to show; the caller decides what to
     * say instead, because an inert entry wants to talk about the grid rather than about materials.
     */
    static List<Component> of(Recipe recipe) {
        List<RecipeChoice> ingredients = RecipeProbe.ingredientsOf(recipe);
        if (ingredients == null) return List.of();

        List<Slot> slots = new ArrayList<>(ingredients.size());
        for (RecipeChoice choice : ingredients) {
            if (choice == null) continue;        // a blank cell in a shaped recipe
            slots.add(new Slot(describe(choice), enumerable(choice)));
        }
        return linesOf(slots);
    }

    /**
     * One ingredient slot, reduced to the two things the rendering cares about.
     *
     * @param label    what to call it, already flattened to text
     * @param listable whether {@code label} names everything the slot accepts
     */
    record Slot(String label, boolean listable) {}

    /**
     * The rendering, with every Bukkit type already resolved away -- so it has a real unit test.
     *
     * <p>Same trade {@code CollectPlan}, {@code GridClickIntent} and {@code MenuIcons} make. What
     * needs a server is asking a {@code RecipeChoice} what it accepts; what does NOT is deciding how
     * a list of labels becomes lore, and that is where the collapsing, the ordering and the honesty
     * line live -- all of them easy to get wrong and invisible until someone opens the menu.
     */
    static List<Component> linesOf(List<Slot> slots) {
        if (slots == null || slots.isEmpty()) return List.of();

        // LinkedHashMap: insertion order is the recipe's own order, which for a shaped recipe reads
        // top-left to bottom-right. A HashMap here would shuffle the list between JVM runs, which is
        // the same undefined-iteration-order trap CraftingMenuLayout.GRID_SLOTS records.
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Boolean> listable = new LinkedHashMap<>();

        for (Slot slot : slots) {
            if (slot == null) continue;
            counts.merge(slot.label(), 1, Integer::sum);
            // AND, not putIfAbsent: if the SAME label arrives once listable and once not, the honest
            // answer is "not". putIfAbsent would let whichever came first decide, which is an
            // order-dependent truth claim -- the exact thing the LinkedHashMap above is here to stop.
            listable.merge(slot.label(), slot.listable(), (a, b) -> a && b);
        }
        if (counts.isEmpty()) return List.of();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Needs:", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        boolean anyPartial = false;
        for (Map.Entry<String, Integer> each : counts.entrySet()) {
            int count = each.getValue();
            String text = (count > 1 ? count + "x " : "") + each.getKey();
            lines.add(Component.text("  " + text, NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (!listable.getOrDefault(each.getKey(), true)) anyPartial = true;
        }

        if (anyPartial) {
            // THE HONESTY LINE. Without it a partial list reads as a whole one, and a player stands
            // in front of a chest wondering why the thing they were told to bring is not accepted.
            lines.add(Component.text("  (this recipe accepts more than can be listed)",
                    NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true));
        }
        return lines;
    }

    /**
     * Can this choice list everything it accepts?
     *
     * <p>{@code MaterialChoice} covers {@code ItemTypeChoice} too, because the implementation
     * extends it -- confirmed on the pinned jar. Everything else falls through, which today means
     * the package-private predicate choice and anything a future Paper adds.
     */
    private static boolean enumerable(RecipeChoice choice) {
        return choice instanceof RecipeChoice.MaterialChoice
                || choice instanceof RecipeChoice.ExactChoice;
    }

    /** A short human label for one ingredient slot. */
    private static String describe(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice materials) {
            return join(materials.getChoices().stream().map(IngredientLore::pretty).toList());
        }
        if (choice instanceof RecipeChoice.ExactChoice exact) {
            return join(exact.getChoices().stream()
                    .map(item -> pretty(item.getType())).distinct().toList());
        }
        // The general fallback: a choice this surface cannot enumerate. Today that is the
        // package-private predicate choice, which cannot even be named from here.
        //
        // choice.getItemStack() would name one representative and is NOT used -- it is deprecated
        // since 1.13.1 (javap -v on the pinned jar: "Deprecated: true"), precisely because it
        // flattens a choice to one stack. See the class javadoc. "something" plus the honesty line
        // says less and claims nothing false; a flattened representative says more and quietly
        // implies it is the only thing accepted.
        return "something";
    }

    static String join(List<String> names) {
        if (names.isEmpty()) return "something";
        if (names.size() <= MAX_ALTERNATIVES) return String.join(" or ", names);
        return String.join(" or ", names.subList(0, MAX_ALTERNATIVES))
                + " or " + (names.size() - MAX_ALTERNATIVES) + " more";
    }

    /** OAK_PLANKS -> "Oak Planks". */
    private static String pretty(Material material) {
        StringBuilder out = new StringBuilder(material.name().length());
        for (String word : material.name().split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return out.toString();
    }
}
