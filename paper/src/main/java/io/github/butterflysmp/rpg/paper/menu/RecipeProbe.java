package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.CollectPlan;
import io.github.butterflysmp.rpg.core.weapon.CraftCount;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.SuggestionTier;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * What the player could craft from what they are carrying.
 *
 * <h2>This ENUMERATES recipes. It does NOT match them, and the difference is the whole licence</h2>
 *
 * {@code CraftingMenu}'s class javadoc records that the previous project hand-rolled shaped and
 * shapeless MATCHING by walking {@code recipeIterator()}, and that delegating to the server deleted
 * all of it. That judgement stands and is not being reversed here: every actual craft still goes
 * through {@code Bukkit.craftItemResult}, and the matrix this class assembles is handed to the
 * server to match exactly as the grid's is.
 *
 * <p>The two are different questions:
 *
 * <ul>
 *   <li><b>MATCHING</b> -- given a matrix, which recipe? The server answers, authoritatively.
 *   <li><b>ENUMERATION</b> -- given materials, which recipes are reachable? <b>Nothing answers
 *       this.</b> {@code getRecipesFor(ItemStack)} is keyed on the RESULT, not the ingredients --
 *       verified against the pinned jar. So the walk is unavoidable.
 * </ul>
 *
 * <h2>COMPLEX RECIPES ARE INVISIBLE HERE, PERMANENTLY, AND THAT IS NOT A GAP TO FILL</h2>
 *
 * {@code ComplexRecipe} is a bare marker interface -- verified against the pinned jar: it extends
 * {@code Recipe, Keyed} and declares nothing at all. It exposes no ingredients, so a recipe
 * registered that way cannot be counted by anybody, ever.
 *
 * <p><b>BE PRECISE ABOUT WHICH RECIPES THOSE ARE, because an earlier version of this paragraph was
 * not.</b> It claimed firework rockets, firework stars and dye tables are all absent. They are not:
 * the basic one-flight firework rocket is an ordinary shapeless recipe and enumerates perfectly
 * well. Only the CUSTOMIZABLE variants -- the multi-star rockets, and the dye/colouring recipes that
 * take an arbitrary number of inputs -- are complex. Gate row Q10 checks both halves for exactly
 * that reason.
 *
 * <p><b>Which vanilla recipes are registered as {@code ComplexRecipe} cannot be verified from the
 * API jar</b> -- it is server runtime data. So the rule stated here is the MECHANISM (no ingredients
 * exposed, therefore not countable) rather than a list of items, and the list belongs to the gate,
 * where it is observed.
 *
 * <p><b>Whatever falls on the complex side still crafts perfectly well IN THE GRID</b>, through the
 * server's matcher, and gate row S3 proves it. So the grid remains the complete surface and Quick
 * Craft is a convenience over the enumerable subset. <b>Do not "restore parity" by hand-implementing
 * them</b> -- that is precisely the mistake the class javadoc of {@code CraftingMenu} records the
 * previous project making.
 *
 * <h2>PROBE, do not enumerate the CHOICES</h2>
 *
 * The obvious design asks each ingredient slot "what materials satisfy you". That question cannot be
 * answered totally. Verified against the pinned jar:
 *
 * <pre>
 * RecipeChoice.getItemStack()          DEPRECATED
 * RecipeChoice.test(ItemStack)         on the interface, undeprecated, total
 *
 * MaterialChoice.getChoices()  -&gt; List&lt;Material&gt;
 * ExactChoice.getChoices()     -&gt; List&lt;ItemStack&gt;          (compares full meta)
 * ItemTypeChoice.itemTypes()   -&gt; RegistryKeySet&lt;ItemType&gt;
 * PredicateRecipeChoice        -&gt; an arbitrary lambda. NOT ENUMERABLE AT ALL.
 * </pre>
 *
 * Three shapes, three different APIs, and one that cannot be enumerated by anybody. So the question
 * is INVERTED: ask the slot "would THIS item satisfy you", which is the question the craft itself
 * will ask, is on the interface, and is total over every implementation including ones that do not
 * exist yet.
 *
 * <p>It works because the two sides have opposite cardinality: a recipe's accepted set is unbounded,
 * and a player's inventory is a few dozen distinct stacks. <b>Ask the small side.</b>
 *
 * <p><b>UNKNOWN MEANS UNSATISFIABLE.</b> A slot nothing in the inventory satisfies contributes an
 * empty list, and {@code CraftCount} drops the whole recipe. A future {@code RecipeChoice} kind we
 * cannot probe therefore costs a missing suggestion, never a button that fails partway through.
 *
 * <h2>Why the probe uses the player's REAL stacks and never a prototype</h2>
 *
 * {@code ExactChoice.test} compares full {@code ItemStack} meta. A freshly built
 * {@code new ItemStack(material)} can fail where the player's actual item passes, and vice versa.
 * So one representative is taken from each distinct group of the player's own items, and that exact
 * stack is what every choice is tested against.
 */
public final class RecipeProbe {

    private RecipeProbe() {}

    /**
     * One group of the player's interchangeable stacks.
     *
     * @param id            the group's index, and the {@code CraftCount.Stock} id it becomes.
     * @param representative one of the player's ACTUAL stacks, for probing. Never a prototype.
     * @param total         how many the player holds across every stack in the group.
     * @param stacks        which inventory slots contribute, and how much each holds.
     *                      {@link CollectPlan.Source} rather than a new type: the collect gesture
     *                      already solved "plan against recorded slots, then apply the plan to those
     *                      slots", and the debit needs exactly that. Re-finding slots by similarity
     *                      afterwards is how "64 planks across three stacks" quietly goes wrong.
     */
    public record Group(int id, ItemStack representative, int total, List<CollectPlan.Source> stacks) {}

    /**
     * Everything one recompute produced.
     *
     * @param suggestions ranked, most-craftable first. Never contains a count of zero.
     * @param groups      indexed by {@link Group#id}, so a suggestion can be assembled and debited.
     * @param recipes     key to recipe, so a click can re-resolve what it pinned.
     */
    public record Result(List<CraftCount.Craftable> suggestions, List<Group> groups,
                         Map<String, Recipe> recipes) {

        public static Result empty() {
            return new Result(List.of(), List.of(), Map.of());
        }
    }

    /**
     * Group the player's carried items, walk the recipe roster, and rank what is reachable.
     *
     * <p><b>MINTED GEAR IS EXCLUDED BEFORE ANYTHING IS PROBED</b>, through the same
     * {@code CraftMatrixScreen.isGear} the grid screen and the Crafter guard use. This is that
     * chain's THIRD surface. Filtering here rather than later is what makes the exclusion total: a
     * tagged item never enters a group, so it can never be counted toward a suggestion and can never
     * be selected for the debit. There is no second place to remember.
     *
     * <p><b>Cost, and the mitigations, because this is the one hot path in the slice.</b> Naively
     * this is {@code recipes x slots x groups} calls to {@code test}, over a roster of more than a
     * thousand. Two things keep it cheap, and both matter:
     *
     * <ul>
     *   <li>the group list is built ONCE here, not per recipe;
     *   <li>{@code CraftCount} bails on the first unsatisfiable slot, and most of the roster dies on
     *       its first ingredient.
     * </ul>
     *
     * <p><b>Measure it rather than trusting this paragraph.</b> The caller times one recompute and
     * prints it at boot-gate time. If it is not comfortably sub-tick, the CADENCE is what changes --
     * not this algorithm.
     */
    public static Result of(PlayerInventory inventory, AdapterContext adapters) {
        List<Group> groups = groups(inventory, adapters.keys());
        if (groups.isEmpty()) return Result.empty();

        List<CraftCount.Stock> stock = stockOf(groups);

        List<CraftCount.Candidate> candidates = new ArrayList<>();
        Map<String, Recipe> recipes = new HashMap<>();

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();

            // Shaped and shapeless only. Furnace, smithing, stonecutting and campfire recipes are
            // not craftable on this surface at all; ComplexRecipe exposes no ingredients to probe.
            List<RecipeChoice> ingredients = ingredientsOf(recipe);
            if (ingredients == null || ingredients.isEmpty()) continue;

            // Unkeyed cannot be pinned, so it could never be committed -- the same rule the grid's
            // bulk loop applies. Skipped here rather than offered and refused on click.
            if (!(recipe instanceof Keyed keyed)) continue;

            List<List<Integer>> slots = new ArrayList<>(ingredients.size());
            for (RecipeChoice choice : ingredients) {
                slots.add(satisfyingGroups(choice, groups));
            }

            String key = keyed.getKey().toString();
            candidates.add(new CraftCount.Candidate(key, tierOf(recipe, adapters), slots));
            recipes.put(key, recipe);
        }

        return new Result(CraftCount.rank(candidates, stock), groups, recipes);
    }

    /**
     * Which groups satisfy this ingredient slot.
     *
     * <p>The whole inversion, in four lines. Every {@code RecipeChoice} implementation answers
     * {@code test}; none of them is required to answer anything else.
     */
    private static List<Integer> satisfyingGroups(RecipeChoice choice, List<Group> groups) {
        List<Integer> satisfying = new ArrayList<>();
        if (choice == null) return satisfying;   // unknown means unsatisfiable
        for (Group group : groups) {
            if (choice.test(group.representative())) satisfying.add(group.id());
        }
        return satisfying;
    }

    /**
     * A recipe's ingredient slots, or null for a kind this surface cannot count.
     *
     * <p>Uses {@code getChoiceMap}/{@code getChoiceList} and NOT {@code getIngredientMap}/
     * {@code getIngredientList} -- verified against the pinned jar, the latter pair is DEPRECATED
     * and flattens a choice to one representative stack, which would silently narrow every recipe
     * that accepts alternatives to whichever material Bukkit happened to list first.
     *
     * <h2>MADE PUBLIC IN SLICE 6, AND THE EXPOSURE IS ADDITIVE</h2>
     *
     * {@link RecipeCatalogue} and {@link IngredientLore} read this to decide whether a recipe is
     * inert and to render its materials. <b>Nothing about {@link #of} changed</b>: the suggestion
     * column still consumes exactly what it consumed before, from the same walk, filtered the same
     * way.
     *
     * <p>This is stated here AND at the call sites because two consumers sharing one walk is exactly
     * how a change to one would slip into the other -- and because gate row Q10(b) asserts a
     * multi-star firework never reaches the COLUMN. If exposing complex recipes had widened
     * {@code Result.suggestions}, Q10 would have been broken by a change that reads like a
     * visibility edit.
     */
    public static List<RecipeChoice> ingredientsOf(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            List<RecipeChoice> ingredients = new ArrayList<>();
            for (String row : shaped.getShape()) {
                for (char cell : row.toCharArray()) {
                    RecipeChoice choice = shaped.getChoiceMap().get(cell);
                    // A blank in the shape is a genuinely empty cell, not an unprobeable slot.
                    if (choice != null) ingredients.add(choice);
                }
            }
            return ingredients;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return shapeless.getChoiceList();
        }
        // Includes ComplexRecipe, which declares no ingredients at all, and every non-crafting
        // recipe kind the iterator yields.
        return null;
    }

    /**
     * Probe ONE recipe against a fresh set of groups.
     *
     * <p><b>THE BULK LOOP CALLS THIS, NOT {@link #of}.</b> A shift-click that crafts sixty-four
     * times must not walk the thousand-recipe roster sixty-four times -- that is the bulk trap, and
     * the correct version and the expensive version read almost identically. What each pass
     * genuinely needs is CURRENT AVAILABILITY, which is 36 inventory slots and one recipe: trivial.
     * The roster walk happens ONCE, after the loop.
     *
     * @return the candidate, or null for a recipe this surface cannot count.
     */
    public static CraftCount.Candidate probeOne(Recipe recipe, List<Group> groups,
                                                SuggestionTier tier) {
        List<RecipeChoice> ingredients = ingredientsOf(recipe);
        if (ingredients == null || ingredients.isEmpty()) return null;
        if (!(recipe instanceof Keyed keyed)) return null;

        List<List<Integer>> slots = new ArrayList<>(ingredients.size());
        for (RecipeChoice choice : ingredients) slots.add(satisfyingGroups(choice, groups));
        return new CraftCount.Candidate(keyed.getKey().toString(), tier, slots);
    }

    /**
     * Which tier a recipe's OUTPUT sorts in.
     *
     * <p>Asks the same {@code claimFor} question the commit does -- does content claim this crafted
     * material -- and hands the answer to {@link SuggestionTiers}. Sharing the lookup is what stops
     * a suggestion sorting as a weapon and then minting nothing, or the reverse.
     *
     * <p>The durability gate is here for the same belt-and-braces reason {@code CraftingMenu.claimFor}
     * carries it: boot already refuses a {@code craft_result} on a material with no durability, so
     * nothing in the index can fail it, but the index is reachable from a caller that has not been
     * through that validation.
     */
    public static SuggestionTier tierOf(Recipe recipe, AdapterContext adapters) {
        ItemStack result = recipe == null ? null : recipe.getResult();
        if (result == null || result.getType().isAir()) return SuggestionTier.VANILLA;
        if (WeaponDurability.maxOf(result).isEmpty()) return SuggestionTier.VANILLA;

        GearDefinition claimed = adapters.craftResults()
                .forResult(result.getType().getKey().getKey()).orElse(null);
        return SuggestionTiers.of(claimed);
    }

    /**
     * The player's carried items, grouped by {@code isSimilar} and with their slots recorded.
     *
     * <p>{@code isSimilar} is the grouping key rather than {@code Material}, because it is the same
     * comparison {@code ExactChoice.test} makes: two stacks that differ only in meta are different
     * ingredients as far as a recipe is concerned, and merging them would let a named or enchanted
     * item stand in for a plain one.
     *
     * <p>Storage contents only -- the 36 main slots. Armor being worn and the offhand are not
     * crafting materials, and consuming what someone is wearing would be a surprise no button
     * should be able to deliver.
     *
     * <p><b>CHEAP: 36 slots, NO RECIPE WALK.</b> That is the load-bearing half of the sentence and
     * it is why the bulk loop may call this once per pass -- see {@link #probeOne} and the bulk
     * trap. The cost note was carried by this method's original one-line javadoc and was dropped
     * when the fuller rationale replaced it in slice 6; it is restored here because "what does this
     * cost" is what stops the next reader calling it in a loop, and the rationale above does not
     * answer that.
     */
    public static List<Group> groupsOf(PlayerInventory inventory, Keys keys) {
        return groups(inventory, keys);
    }

    /** The stock list a group set presents to {@code CraftCount}. */
    public static List<CraftCount.Stock> stockOf(List<Group> groups) {
        List<CraftCount.Stock> stock = new ArrayList<>();
        for (Group group : groups) stock.add(new CraftCount.Stock(group.id(), group.total()));
        return stock;
    }

    private static List<Group> groups(PlayerInventory inventory, Keys keys) {
        List<Group> groups = new ArrayList<>();
        List<List<CollectPlan.Source>> stacks = new ArrayList<>();
        List<ItemStack> representatives = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();

        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;

            // THE INVARIANT. A minted item is never a material -- not counted, and therefore never
            // assembled into a matrix and never debited. Third surface for this chain.
            if (CraftMatrixScreen.isGear(item, keys)) continue;

            int found = -1;
            for (int index = 0; index < representatives.size(); index++) {
                if (representatives.get(index).isSimilar(item)) {
                    found = index;
                    break;
                }
            }
            if (found < 0) {
                representatives.add(item.clone());
                totals.add(0);
                stacks.add(new ArrayList<>());
                found = representatives.size() - 1;
            }
            totals.set(found, totals.get(found) + item.getAmount());
            stacks.get(found).add(
                    new CollectPlan.Source(CollectPlan.TIER_INVENTORY, slot, item.getAmount()));
        }

        for (int index = 0; index < representatives.size(); index++) {
            groups.add(new Group(index, representatives.get(index), totals.get(index),
                    List.copyOf(stacks.get(index))));
        }
        return groups;
    }

    /**
     * A crafting matrix built from the player's own items, and the exact slots it came out of.
     *
     * @param matrix nine cells, row-major, as {@code Server.getCraftingRecipe} documents.
     * @param draws  what to remove from the inventory, per slot. <b>This is the debit</b>, and it
     *               names SLOTS rather than materials for the reason {@link Group#stacks} does.
     */
    public record Assembly(ItemStack[] matrix, List<CollectPlan.Source> draws) {}

    /**
     * Lay a recipe out into a 3x3 matrix using the groups the count already allocated.
     *
     * <p><b>The assignment comes from {@code CraftCount.assign} and is NOT re-derived here.</b> If
     * this method ran its own greedy walk, the count and the assembly would be two implementations
     * of one rule, and a suggestion could promise five crafts whose third reached for a group the
     * count never allocated. One walk, both read it.
     *
     * <p><b>The assignment is in the same order {@link #ingredientsOf} produced</b> -- shape order
     * with blanks skipped, for a shaped recipe -- so this walks the shape again in that identical
     * order and consumes the assignment as it goes. That coupling is why both live in this file.
     *
     * <p>Shaped recipes are placed at the TOP-LEFT of the 3x3. Vanilla's matcher handles a recipe
     * smaller than the grid wherever it sits, and top-left is the placement with no arithmetic to
     * get wrong.
     *
     * @return the assembly, or null when the recipe cannot be laid out -- an unusable assignment, a
     *         shape that does not fit, or a group that has run dry. Null is the safe answer: the
     *         caller refuses the craft and nothing has been taken.
     */
    public static Assembly assemble(Recipe recipe, List<Group> groups, List<Integer> assignment) {
        List<RecipeChoice> ingredients = ingredientsOf(recipe);
        if (ingredients == null || assignment == null || assignment.size() != ingredients.size()) {
            return null;
        }

        // How much of each group is still unspent as this assembly proceeds, per SLOT. Copied so a
        // failed assembly leaves the real inventory untouched.
        Map<Integer, List<int[]>> remaining = new HashMap<>();
        for (Group group : groups) {
            List<int[]> stacks = new ArrayList<>();
            for (CollectPlan.Source source : group.stacks()) {
                stacks.add(new int[]{source.slot(), source.amount()});
            }
            remaining.put(group.id(), stacks);
        }

        ItemStack[] matrix = new ItemStack[9];
        Map<Integer, Integer> taken = new HashMap<>();   // inventory slot -> how many

        int cursor = 0;
        if (recipe instanceof ShapedRecipe shaped) {
            String[] shape = shaped.getShape();
            for (int row = 0; row < shape.length && row < 3; row++) {
                String line = shape[row];
                for (int column = 0; column < line.length() && column < 3; column++) {
                    if (shaped.getChoiceMap().get(line.charAt(column)) == null) continue;
                    ItemStack one = drawOne(assignment.get(cursor++), groups, remaining, taken);
                    if (one == null) return null;
                    matrix[row * 3 + column] = one;
                }
            }
        } else if (recipe instanceof ShapelessRecipe) {
            if (ingredients.size() > 9) return null;
            for (int index = 0; index < ingredients.size(); index++) {
                ItemStack one = drawOne(assignment.get(cursor++), groups, remaining, taken);
                if (one == null) return null;
                matrix[index] = one;
            }
        } else {
            return null;
        }

        List<CollectPlan.Source> draws = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : taken.entrySet()) {
            draws.add(new CollectPlan.Source(CollectPlan.TIER_INVENTORY, entry.getKey(), entry.getValue()));
        }
        return new Assembly(matrix, draws);
    }

    /**
     * Take exactly one item from a group, recording which inventory slot it came out of.
     *
     * <p>Drains a group's stacks in the order {@link Group#stacks} recorded them, which is inventory
     * slot order. Deterministic, so two assemblies of the same recipe against the same inventory
     * debit the same slots.
     */
    private static ItemStack drawOne(int groupId, List<Group> groups,
                                     Map<Integer, List<int[]>> remaining,
                                     Map<Integer, Integer> taken) {
        List<int[]> stacks = remaining.get(groupId);
        if (stacks == null) return null;

        for (int[] stack : stacks) {
            if (stack[1] <= 0) continue;
            stack[1]--;
            taken.merge(stack[0], 1, Integer::sum);

            for (Group group : groups) {
                if (group.id() != groupId) continue;
                ItemStack one = group.representative().clone();
                one.setAmount(1);
                return one;
            }
            return null;
        }
        return null;   // the group ran dry mid-assembly
    }

    /** Re-resolve a pinned suggestion. Empty when the recipe has gone away since the recompute. */
    public static NamespacedKey keyOf(Recipe recipe) {
        return recipe instanceof Keyed keyed ? keyed.getKey() : null;
    }

    /** Is this a recipe a crafting grid can actually produce? */
    public static boolean isCrafting(Recipe recipe) {
        return recipe instanceof CraftingRecipe;
    }
}
