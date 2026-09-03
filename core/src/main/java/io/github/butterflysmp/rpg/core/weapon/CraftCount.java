package io.github.butterflysmp.rpg.core.weapon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How many of each recipe a player can make from what they are carrying, ranked.
 *
 * <p><b>Pure, and in {@code core}, for the reason {@link CollectPlan} is.</b> Quick Craft's walk
 * over {@code recipeIterator()}, its {@code RecipeChoice.test} probing and its inventory debit all
 * need a running server and are boot-gate-only. What does NOT need one is the arithmetic: given a
 * multiset of materials and a set of recipes, how many of each is reachable. That is the single
 * largest piece of logic in the slice, and this keeps it OFF the boot-gate-only list rather than
 * adding the biggest entry yet to it.
 *
 * <h2>THE INVARIANT: THE COUNT MUST NEVER OVER-STATE</h2>
 *
 * A suggestion offering more than the player can make is a button that fails partway through --
 * ingredients spent, output short, and a player who cannot tell whether they were robbed. One
 * offering FEWER is a button that works.
 *
 * <p>So under-counting is the safe direction, and it is stated here as the property the tests assert
 * rather than left as an accident of the algorithm. {@link #rank} may return a count lower than the
 * true optimum. It may never return one higher.
 *
 * <h2>Where the under-count comes from, deliberately</h2>
 *
 * The walk is GREEDY with no backtracking: each ingredient slot takes the first group that has any
 * availability at all, and the assignment is never revisited. When a slot accepts alternatives, that
 * can pick a group another slot needed more.
 *
 * <p>Worked: a recipe with two slots, the first accepting oak OR birch planks, the second accepting
 * oak only. The player holds ONE oak and sixty-four birch. Slot one takes oak (first with
 * availability); slot two also needs oak; demand for oak is 2 against a stock of 1, so the count is
 * ZERO. The optimal assignment -- birch then oak -- makes one. The suggestion simply does not
 * appear, which is a missing button rather than a broken one.
 *
 * <p>Solving it properly is bipartite matching, and it is not worth it here: the failure mode of the
 * cheap version is invisible to a player, and the failure mode of a subtly wrong clever version is
 * a craft that takes materials and does not deliver.
 *
 * <h2>Identity is an int, not a material</h2>
 *
 * Nothing here knows what a "plank" is. The caller groups the player's stacks by whatever identity
 * it likes -- Quick Craft groups by (material + meta), because {@code RecipeChoice.ExactChoice}
 * compares full item meta -- and hands over opaque group ids. Same trade {@link CollectPlan} makes
 * with its {@code slot}: "the caller's own identifier, opaque here".
 */
public final class CraftCount {

    private CraftCount() {}

    /**
     * One group of interchangeable items the player holds.
     *
     * @param id     the caller's own group identifier, opaque here. Recipe slots refer to these.
     * @param amount how many the player has in total, summed across every stack in the group.
     */
    public record Stock(int id, int amount) {}

    /**
     * One candidate recipe, already probed.
     *
     * @param key   the caller's recipe identity, opaque here. Orders ties, so a ranking is stable.
     * @param tier  what KIND of thing this makes. <b>Supplied by the caller, never derived here</b>
     *              -- classifying a recipe needs {@code CraftResultIndex} and the sealed
     *              {@code GearDefinition} hierarchy, both of which are Bukkit-side. Same inversion
     *              the recipe probe uses: core is told, and sorts. Null is treated as
     *              {@link SuggestionTier#VANILLA}, so a caller that has not been taught about tiers
     *              still gets a sane ordering rather than an exception inside a click handler.
     * @param slots one entry per INGREDIENT slot the recipe requires; each entry lists the
     *              {@link Stock#id}s that satisfy that slot, in the caller's own deterministic
     *              order. An EMPTY list means "nothing the player holds satisfies this slot", which
     *              is how an unprobeable {@code RecipeChoice} arrives -- see {@link #countOf}.
     */
    public record Candidate(String key, SuggestionTier tier, List<List<Integer>> slots) {}

    /** A recipe the player can make, its display tier, and how many times. Never a count of zero. */
    public record Craftable(String key, SuggestionTier tier, int count) {}

    /**
     * TIER FIRST, then most-craftable, then key.
     *
     * <p>The tier leads because a suggestion that mints RPG gear is worth more than one that makes
     * sticks however many sticks are available -- a column sorted by count alone would bury a
     * craftable shield under sixty-four torches. {@link SuggestionTier}'s declaration order IS this
     * ordering.
     *
     * <p>The key tiebreak is not decoration: without it the order would depend on the iteration
     * order of {@code recipeIterator()}, which is the server's business and may differ between
     * boots. The same concern {@link CollectPlan}'s slot tiebreak addresses.
     */
    private static final Comparator<Craftable> RANKING =
            Comparator.comparingInt((Craftable c) -> c.tier().ordinal())
                    .thenComparing(Comparator.comparingInt(Craftable::count).reversed())
                    .thenComparing(Craftable::key);

    /**
     * Rank every candidate the player can actually make.
     *
     * @param candidates the probed recipes. Order is irrelevant -- this sorts.
     * @param stock      what the player holds, one entry per group. Duplicate ids are summed, so a
     *                   caller that emits one entry per stack rather than per group still gets a
     *                   correct total.
     * @return the craftable recipes, most first. Recipes with a count of zero are ABSENT rather than
     *         present with a zero -- a suggestion that cannot be made is not a suggestion. Empty is
     *         a perfectly ordinary answer and is also what nonsense inputs produce: this runs inside
     *         a click handler, where returning nothing loses nothing and throwing breaks the menu.
     */
    public static List<Craftable> rank(List<Candidate> candidates, List<Stock> stock) {
        List<Craftable> ranked = new ArrayList<>();
        if (candidates == null || stock == null) return ranked;

        Map<Integer, Integer> totals = totals(stock);
        if (totals.isEmpty()) return ranked;

        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.key() == null) continue;
            int count = countOf(candidate, totals);
            if (count > 0) ranked.add(new Craftable(candidate.key(), tierOf(candidate), count));
        }

        ranked.sort(RANKING);
        return ranked;
    }

    /**
     * How many times one recipe can be made.
     *
     * <p><b>Bails on the first unsatisfiable slot</b>, which is the performance mitigation as well as
     * the correctness one: most of a thousand-recipe roster dies on its first ingredient, so the
     * average cost is nothing like {@code recipes x slots x groups}.
     *
     * <p>A slot with no satisfying group returns zero. That is the UNKNOWN-MEANS-UNSATISFIABLE rule
     * arriving from the adapter: a {@code RecipeChoice} that cannot be probed contributes an empty
     * list, so the recipe silently does not appear rather than appearing and failing.
     */
    static int countOf(Candidate candidate, Map<Integer, Integer> totals) {
        List<Integer> chosen = assign(candidate, totals);
        if (chosen.isEmpty()) return 0;

        // How many of each group ONE craft consumes. Two slots choosing the same group demand two.
        Map<Integer, Integer> demand = new HashMap<>();
        for (Integer id : chosen) demand.merge(id, 1, Integer::sum);

        // The binding constraint: whichever demanded group runs out first.
        int count = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : demand.entrySet()) {
            int available = totals.getOrDefault(entry.getKey(), 0);
            count = Math.min(count, available / entry.getValue());
        }
        return count == Integer.MAX_VALUE ? 0 : count;
    }

    /**
     * Which group each ingredient slot draws from, in slot order.
     *
     * <p><b>PUBLIC because the caller that ASSEMBLES a crafting matrix must make exactly the same
     * choices this count was computed from.</b> If the assembly re-derived the greedy walk itself,
     * the two would be two implementations of one rule -- and the failure would be a suggestion
     * promising five crafts whose third one silently reaches for a group the count never allocated.
     * That is the "two callers agreeing today is not two callers sharing an input" defect this arc
     * has now met four times, so there is one walk and both read it.
     *
     * @return one group id per ingredient slot, or an EMPTY list when the recipe cannot be made at
     *         all. Empty is the same answer {@link #countOf} turns into a count of zero, so a caller
     *         cannot act on an assignment the count would have rejected.
     */
    public static List<Integer> assign(Candidate candidate, List<Stock> stock) {
        if (candidate == null || stock == null) return List.of();
        return assign(candidate, totals(stock));
    }

    private static List<Integer> assign(Candidate candidate, Map<Integer, Integer> totals) {
        List<List<Integer>> slots = candidate.slots();
        if (slots == null || slots.isEmpty()) return List.of();

        List<Integer> chosen = new ArrayList<>(slots.size());
        for (List<Integer> accepting : slots) {
            if (accepting == null || accepting.isEmpty()) return List.of();   // unsatisfiable: bail

            Integer pick = null;
            for (Integer id : accepting) {
                if (id != null && totals.getOrDefault(id, 0) > 0) {
                    pick = id;
                    break;   // FIRST with any availability. No backtracking -- see the class javadoc.
                }
            }
            if (pick == null) return List.of();   // every alternative is exhausted: bail

            chosen.add(pick);
        }
        return chosen;
    }

    /**
     * A candidate's tier, defaulting an absent one to {@link SuggestionTier#VANILLA}.
     *
     * <p>Null-tolerant on purpose. This runs inside a click handler on every recompute, and a
     * caller that has not been taught about tiers should get an unsorted-to-the-bottom suggestion
     * rather than a {@code NullPointerException} that breaks the whole menu. Defaulting DOWN is the
     * safe direction: an unclassified recipe sinks below everything classified rather than
     * displacing a minted weapon from the top of the column.
     */
    private static SuggestionTier tierOf(Candidate candidate) {
        return candidate.tier() == null ? SuggestionTier.VANILLA : candidate.tier();
    }

    /** Sum the stock by group id, ignoring non-positive amounts. */
    private static Map<Integer, Integer> totals(List<Stock> stock) {
        Map<Integer, Integer> totals = new HashMap<>();
        for (Stock entry : stock) {
            if (entry == null || entry.amount() <= 0) continue;
            totals.merge(entry.id(), entry.amount(), Integer::sum);
        }
        return totals;
    }
}
