package io.github.butterflysmp.rpg.paper.menu;

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
 * {@code RecipeProbe} answers <i>"what can I make RIGHT NOW"</i>: it drops anything the player
 * cannot currently satisfy. That is exactly right for the suggestion column and exactly wrong for a
 * browser -- the armor a player lacks materials for is precisely what they opened the browser to
 * find, and under the probe it is as absent on page 4 as it is in cell 2.
 *
 * <p>So this is a SECOND walk of a different shape: the full roster, unfiltered by inventory.
 *
 * <h2>STATIC vs DYNAMIC, split by what actually changes</h2>
 *
 * The catalogue is STATIC -- roster membership, key, tier, whether it is inert. The COUNTS are
 * dynamic and are scored per rendered page, never stored here. Mixing the two is what would force a
 * rebuild on every inventory change.
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
     * @param id    the recipe's key. The identity a click re-resolves through.
     * @param tier  what kind of thing it makes. The primary sort.
     * @param inert this menu can NEVER craft it -- see {@link #isInert}. It is still shown.
     */
    public record Entry(NamespacedKey id, SuggestionTier tier, boolean inert) {}

    /**
     * TIER FIRST, THEN KEY -- the browser's whole ordering, extracted so it has a unit test.
     *
     * <p>Building the catalogue needs a running server. Deciding what order it comes out in does
     * not, and that is where the defects are, so the comparator is a named constant rather than a
     * lambda buried in {@link #build}. Same trade {@code CollectPlan} and {@code MenuIcons} make.
     *
     * <p><b>The tiebreak must be STATIC and DETERMINISTIC.</b> Without it entries shuffle between
     * rebuilds and a player's memory of where something sits is worthless.
     * {@code CraftingMenuLayout.GRID_SLOTS}' javadoc records this repo being bitten once already by
     * an iteration order the JDK does not define. The recipe key is the obvious choice: it is
     * unique, stable across restarts, and already the identity a click re-resolves through.
     *
     * <p><b>The invariant is "all gear sorts ahead of all vanilla". It is NOT "page 1 is the gear
     * page"</b> -- that is arithmetic over two numbers that can both move, and nothing would warn
     * anyone when it stopped holding.
     */
    static final Comparator<Entry> ORDER =
            Comparator.comparingInt((Entry entry) -> entry.tier().ordinal())
                    .thenComparing(entry -> entry.id().toString());

    private final AdapterContext adapters;

    private List<Entry> entries;

    /**
     * How many entries have at least one ingredient this surface cannot ENUMERATE for display.
     *
     * <p>Not a craftability count -- see {@link #isInert}. It feeds the honesty line in the
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
     * Can this menu never craft it, whatever the player is carrying?
     *
     * <p><b>The exclusion axis has exactly ONE member: a recipe that exposes no ingredients.</b>
     * {@code ComplexRecipe} -- multi-star fireworks, dye recipes, book cloning -- declares none at
     * all, so there is nothing to assemble a matrix out of and no amount of material helps.
     *
     * <p><b>An earlier draft had a second member and it was FALSE.</b> The premise was that an
     * unprobeable {@code RecipeChoice} gets dropped by {@code satisfyingGroups}. It does not:
     * that method calls {@code choice.test(..)}, and every {@code RecipeChoice} implementation
     * answers {@code test} -- it extends {@code Predicate<ItemStack>}, verified on the pinned jar.
     * The only empty path is a null choice, which {@code ingredientsOf} already filters. A
     * predicate-choice recipe is fully probeable, countable and CRAFTABLE here.
     *
     * <p>Inert entries are still LISTED, and that is the decision worth defending. Excluding them
     * would tell the player a recipe does not exist when it does and they can make it -- in the
     * vanilla grid, through the server's own matcher. Routing them into the ordinary
     * missing-materials refusal would be worse still: that refusal is temporary and actionable,
     * this one is permanent, and a player would gather materials, return, and meet the identical
     * message for ever.
     */
    public static boolean isInert(Recipe recipe) {
        List<?> ingredients = RecipeProbe.ingredientsOf(recipe);
        return ingredients == null || ingredients.isEmpty();
    }

    private void build() {
        long startedAt = System.nanoTime();

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

            built.add(new Entry(id, RecipeProbe.tierOf(recipe, adapters), isInert(recipe)));
            if (!IngredientLore.fullyListable(recipe)) unlistable++;
        }

        // TIER FIRST, THEN KEY. The tiebreak must be static and deterministic: without it entries
        // shuffle between rebuilds and a player's memory of where something sits is worthless.
        // CraftingMenuLayout.GRID_SLOTS' javadoc records this repo being bitten once already by an
        // iteration order the JDK does not define.
        built.sort(ORDER);

        this.entries = List.copyOf(built);
        this.partiallyListable = unlistable;

        long micros = (System.nanoTime() - startedAt) / 1_000;

        // CLAUDE.md:104 -- a discovery that finds nothing is a DEFECT, not a quiet no-op. An empty
        // catalogue is indistinguishable from a working browser that happens to be blank, which is
        // exactly the shape that file records for getResource("content/").
        if (entries.isEmpty()) {
            adapters.log().severe("RECIPE CATALOGUE IS EMPTY. The browser walked the server's whole "
                    + "recipe roster and kept nothing. That is a defect, not an empty server: "
                    + "vanilla alone registers hundreds. Skipped " + skipped + " unkeyed.");
            return;
        }

        // THE INSTRUMENT. The catalogue walk has NO early bail -- keeping everything is the point --
        // so it is a bigger job than the 298us the suggestion probe was measured at, and under lazy
        // build it runs on the MAIN THREAD inside a player's click rather than at boot where nobody
        // is waiting. Q2's number must not be cited for it.
        //
        // This log line is TEMPORARY: it exists to put a real number in the gate row, and is deleted
        // in the commit that records that number. See PLAN-1b-swing-listener.md:134, and slice 5's
        // lesson that a passing row is exactly when "remove before merge" gets skipped.
        adapters.log().info("Recipe catalogue built: " + entries.size() + " entries in "
                + micros + "us (" + skipped + " unkeyed skipped, "
                + partiallyListable + " not fully listable)");
    }
}
