package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.CraftOrder;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.SuggestionTier;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Every craftable recipe on the server, sorted, for the recipe browser to page through.
 *
 * <h2>WHY THIS IS NOT {@link RecipeProbe}</h2>
 *
 * {@code RecipeProbe.of} answers <i>"what can I make RIGHT NOW"</i> by walking the roster and
 * scoring it against one player's inventory, every time. This answers <i>"what recipes EXIST"</i>,
 * once, for everybody.
 *
 * <p><b>The browser needs both, and that is why both exist.</b> It filters this list down to what
 * the player can craft -- so the visible result resembles the probe's -- but the expensive,
 * player-independent half (roster membership, key, tier, body slot, ingredient shape) is computed
 * ONCE here rather than per open.
 *
 * <p><i>(An earlier version of this paragraph argued the opposite: that a browser must show what the
 * player CANNOT yet afford, because "the armor a player lacks materials for is precisely what they
 * opened the browser to find". That was correct for a browser meant to answer the Q16 squeeze. It is
 * not the brief -- see below -- and the sentence is replaced rather than left sitting here looking
 * live.)</i>
 *
 * <h2>THE CATALOGUE IS THE FULL ROSTER. THE BROWSER IS NOT.</h2>
 *
 * <b>The browser shows only what the player can craft RIGHT NOW</b>, and filters this list per
 * player each time it opens. That is a reversal of the premise this class was written under, taken
 * deliberately: the brief is <i>"an easy way to craft quickly"</i>, not a recipe encyclopedia, and
 * 1095 entries is clutter against that purpose.
 *
 * <p><b>The static catalogue survives the reversal unchanged, and is still the right structure.</b>
 * Roster membership, key, tier, body slot and ingredients do not depend on any player, so they are
 * computed once and shared; only the filter and the counts are per-player, and those are cheap. The
 * build-once decision, its lazy trigger and gate row Q24 are all untouched.
 *
 * <h2>WHAT THIS COSTS, SAID OUT LOUD</h2>
 *
 * <b>Armor the player cannot yet afford is now invisible EVERYWHERE.</b> The suggestion column is
 * three cells ranked by tier, so armor is squeezed out of it (gate row Q16); the browser now hides
 * what cannot be crafted. Between them, <b>no surface answers "what does a netherite helmet
 * need?"</b>
 *
 * <p>That is a consequence of the product decision, not a defect, and it is written here rather than
 * left to be discovered as a complaint. If it ever needs answering, the answer is a third surface --
 * a lookup — not a filter flag on this one.
 *
 * <h2>STATIC vs DYNAMIC, split by what actually changes</h2>
 *
 * The catalogue is STATIC -- roster membership, key, tier, body slot, ingredient shape. The COUNTS
 * and the craftable-now FILTER are dynamic: the browser recomputes both per open, and after every
 * craft. Mixing the two is what would force a full roster rebuild on every inventory change.
 *
 * <h2>BUILT ON FIRST OPEN, NOT AT BOOT</h2>
 *
 * <b>Our {@code onEnable} is not the end of plugin load.</b> Another plugin registering recipes in
 * its own {@code onEnable} may run after ours, and every one of those would be missed permanently,
 * with no symptom beyond "some recipes aren't in the browser" -- the worst kind of bug, because
 * nothing is broken enough to report.
 *
 * <p><b>Explicit invalidation was considered and REJECTED</b>, and the reason is worth keeping: an
 * event hook would cover {@code /reload} and NOT a mid-session datapack change, while being named as
 * though it covered both. That is gate rule 4's shape -- a check that looks like it discriminates
 * and does not -- moved into a cache. If invalidation is ever wanted it should be an ADMIN COMMAND
 * that says what it does, never a hook that looks automatic, and whoever writes it should first
 * verify against the pinned jar which events actually fire.
 *
 * <h2>WHAT MAKES THE CACHE SAFE IS THE CLICK PATH, NOT THE CACHE</h2>
 *
 * A click re-resolves the recipe by key against the LIVE roster ({@link #resolve}) and refuses
 * cleanly if it is gone. It never trusts the cached entry to still be craftable. A stale catalogue
 * therefore costs a player a polite refusal, never anything incorrect.
 *
 * <h2>Threading</h2>
 *
 * Main thread only. The build runs inside a player's click, so there is no cross-thread publication
 * to guard and no {@code volatile}: a second player's click is another main-thread task, strictly
 * after this one. If this ever moves off the main thread that stops being true, which is why it is
 * written down rather than left to be inferred from the absence of a keyword.
 */
public final class RecipeCatalogue {

    /**
     * One row in the browser.
     *
     * <p><b>Deliberately tiny, and deliberately NOT holding the {@code Recipe} or a display
     * {@code ItemStack}.</b> Minting a thousand icons at build time to show forty-five of them is
     * the obvious version and the wrong one; the icon is built per rendered page from
     * {@link #resolve}, which has the side benefit that what the player sees is always resolved from
     * the live roster rather than from whatever was true at first open.
     *
     * @param id        the recipe's key. The identity a click re-resolves through.
     * @param tier      what kind of thing it makes. The primary sort.
     * @param armorSlot which body slot, when it makes armor. Null otherwise -- see {@link CraftOrder}.
     */
    public record Entry(NamespacedKey id, SuggestionTier tier, ArmorSlot armorSlot)
            implements CraftOrder {

        /** {@link CraftOrder} orders by string key; the catalogue's identity is a namespaced one. */
        @Override
        public String key() {
            return id == null ? null : id.toString();
        }
    }

    /**
     * The catalogue's order, which is {@link CraftOrder#TIER_FIRST} and NOT a second copy of it.
     *
     * <p>It was a local {@code tier -> key} comparator until armor gained a body-slot order. Keeping
     * it local would have meant the catalogue sorted armor alphabetically while the browser sorted
     * it head-down, or -- worse and more likely -- both being edited to agree and drifting later.
     * <b>Armor is squeezed out of the three-cell column</b> (gate row Q16), so a disagreement
     * between two of the three orderings is invisible in play. See {@link CraftOrder}.
     *
     * <p>The alias is kept rather than inlined because {@code RecipeCatalogueOrderTest} names it,
     * and because "the catalogue has an order, and it is the shared one" is worth being able to
     * read at this end rather than only at the other.
     */
    static final Comparator<CraftOrder> ORDER = CraftOrder.TIER_FIRST;

    private final AdapterContext adapters;

    private List<Entry> entries;

    /**
     * How many entries have at least one ingredient this surface cannot ENUMERATE for display.
     *
     * <p>Not a craftability count. It feeds the honesty line in the
     * ingredient lore: <i>"these are the materials"</i> versus <i>"these are the materials I can
     * list"</i>.
     */
    private int partiallyListable;

    public RecipeCatalogue(AdapterContext adapters) {
        this.adapters = adapters;
    }

    /** The whole catalogue, sorted. Builds it on the first call. */
    public List<Entry> entries() {
        if (entries == null) build();
        return entries;
    }

    /** How many entries could not have their full ingredient list displayed. Zero is normal. */
    public int partiallyListable() {
        if (entries == null) build();
        return partiallyListable;
    }

    /**
     * The live recipe behind an entry, or null if it has left the roster since the catalogue was
     * built.
     *
     * <p><b>This is the whole reason a stale cache is survivable.</b> Every click goes through here
     * before anything is committed.
     */
    public Recipe resolve(Entry entry) {
        Recipe recipe = Bukkit.getRecipe(entry.id());
        return recipe != null && RecipeProbe.isCrafting(recipe) ? recipe : null;
    }

    /**
     * A recipe with no ingredients to enumerate -- {@code ComplexRecipe}, and nothing else.
     *
     * <p><b>These are now simply ABSENT from the browser, and that is honest under the current
     * contract.</b> They cannot be probed, so they can never be counted, so they never survive the
     * craftable-now filter. Nothing special happens to them; they fall out for the same reason a
     * recipe the player lacks materials for falls out.
     *
     * <p><b>There used to be a whole apparatus here -- an "inert" flag, a red pane, and lore reading
     * "Cannot be crafted here / use the crafting grid".</b> It existed because the browser claimed
     * to show EVERYTHING, and under that claim omitting a grid-craftable recipe would have been a
     * false absence: Q10's mistake in UI form. Under <i>"what you can craft here, right now"</i> the
     * absence is true, so the apparatus is gone and gate rows Q30/Q31 are struck as superseded.
     *
     * <p>Kept as a named method only because it explains the absence. <b>The exclusion axis still
     * has exactly ONE member</b>, and the correction that established that is worth not losing: an
     * earlier draft claimed an unprobeable {@code RecipeChoice} was a second member. It is not --
     * {@code satisfyingGroups} calls {@code choice.test(..)}, and every {@code RecipeChoice}
     * implementation answers {@code test} (it extends {@code Predicate<ItemStack>}, verified on the
     * pinned jar). A predicate-choice recipe is fully probeable, countable and craftable, and shows
     * up in this browser like anything else.
     */
    static boolean hasNoEnumerableIngredients(Recipe recipe) {
        List<?> ingredients = RecipeProbe.ingredientsOf(recipe);
        return ingredients == null || ingredients.isEmpty();
    }

    private void build() {

        List<Entry> built = new ArrayList<>();
        int unlistable = 0;
        int skipped = 0;

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (!RecipeProbe.isCrafting(recipe)) continue;

            NamespacedKey id = RecipeProbe.keyOf(recipe);
            if (id == null) {
                // Unkeyed and therefore unpinnable, so a click could never commit it safely.
                // Counted rather than dropped silently -- see the discovery guard below.
                skipped++;
                continue;
            }

            // ONE lookup, two projections. Tier and armor slot are both derived from the gear
            // definition this craft claims, so asking twice would be two chances to classify one
            // recipe two ways.
            GearDefinition claimed = RecipeProbe.claimedBy(recipe, adapters);
            built.add(new Entry(id, SuggestionTiers.of(claimed),
                    claimed instanceof ArmorDefinition armor ? armor.slot() : null));
            if (!IngredientLore.fullyListable(recipe)) unlistable++;
        }

        // TIER FIRST, THEN KEY. The tiebreak must be static and deterministic: without it entries
        // shuffle between rebuilds and a player's memory of where something sits is worthless.
        // CraftingMenuLayout.GRID_SLOTS' javadoc records this repo being bitten once already by an
        // iteration order the JDK does not define.
        built.sort(ORDER);

        this.entries = List.copyOf(built);
        this.partiallyListable = unlistable;

        // CLAUDE.md:104 -- a discovery that finds nothing is a DEFECT, not a quiet no-op. An empty
        // catalogue is indistinguishable from a working browser that happens to be blank, which is
        // exactly the shape that file records for getResource("content/").
        if (entries.isEmpty()) {
            adapters.log().severe("RECIPE CATALOGUE IS EMPTY. The browser walked the server's whole "
                    + "recipe roster and kept nothing. That is a defect, not an empty server: "
                    + "vanilla alone registers hundreds. Skipped " + skipped + " unkeyed.");
            return;
        }

        // THE INSTRUMENT WAS HERE, AND IT WAS DELETED IN THE COMMIT THAT RECORDED ITS NUMBER --
        // 2026-09-04, exactly as the rule required. The figures are in NEXT.md under
        // "Q24's MEASUREMENTS": 1095 entries, 7137/7994/11159us across three runs, 0 unkeyed
        // skipped, 0 not fully listable.
        //
        // WHAT THE NUMBER SAID, kept because it is the reason anyone would re-add a timer here:
        // the walk has NO early bail -- keeping everything is the point -- so at 7-11ms it is
        // 24-37x the suggestion probe's 298us, and 14-22% of a 50ms tick, paid once per server
        // lifetime on the MAIN THREAD inside a player's click. No perceptible stall, but with far
        // less headroom than the probe's 168x. If this walk ever gains work, that is the figure
        // that moves first, and it is worth re-measuring rather than assuming.
        //
        // Q2's craftable/probed/distinct-stack counts are a DIFFERENT measurement and are still
        // owed; this number must not be cited for them.
    }
}
