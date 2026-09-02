package io.github.butterflysmp.rpg.core.weapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Which stacks a double-click gathers onto the cursor, in what order, and how much from each.
 *
 * <p><b>Pure, and in {@code core}, for the reason {@code GridClickIntent} is.</b> The gesture itself
 * has to be PERFORMED by the router -- see {@code MenuRouting} -- and nothing there can be built in
 * a unit test. What CAN be decided without a server is the part that is actually easy to get wrong:
 * which sources are eligible, which order they drain in, and how much comes off each before the
 * cursor is full. Splitting it this way keeps the ordering rule and the stack arithmetic OFF the
 * boot-gate-only list rather than adding three more entries to it.
 *
 * <h2>Two tiers, and the boundary is the one deliberate deviation from vanilla</h2>
 *
 * Vanilla's collect prefers the SMALLEST stacks wherever they are, which consolidates fragments
 * rather than breaking up full stacks. That behaviour is kept -- but only WITHIN a tier.
 *
 * <p>The player's own inventory drains first, and a crafting grid is only reached if the cursor is
 * still short. Smallest-first across both tiers at once would PREFER THE GRID exactly when a recipe
 * is loaded, because a staged recipe is made of partial stacks by definition -- six planks in a
 * slot, not sixty-four. The most faithful ordering is therefore the one that most reliably destroys
 * the player's layout, and this grid is not vanilla's transient one: players stage stacks here and
 * come back to them across many crafts.
 *
 * <p>Everything a player can actually perceive about the gesture -- that it consolidates fragments
 * rather than raiding full stacks -- is preserved. Only the tier boundary differs.
 */
public final class CollectPlan {

    private CollectPlan() {}

    /** The player's own inventory. Drains first. */
    public static final int TIER_INVENTORY = 0;

    /** A menu's own stacking slots. Reached only if the cursor is still short. */
    public static final int TIER_MENU = 1;

    /**
     * One stack the collect may draw from.
     *
     * @param tier   {@link #TIER_INVENTORY} or {@link #TIER_MENU}. Lower drains first.
     * @param slot   the caller's own identifier, opaque here. Orders ties, so a plan is stable.
     * @param amount how many are in it.
     */
    public record Source(int tier, int slot, int amount) {}

    /** Take {@code amount} from {@code source}. Never more than the source holds. */
    public record Draw(Source source, int amount) {}

    /**
     * Ties are broken by SLOT, so two runs against the same grid agree.
     *
     * <p>Not incidental: {@code MenuRouting.shiftMove} already sorts its targets "so a multi-input
     * menu fills left to right rather than in Set.of's unspecified order", and the same concern
     * applies here. Iteration order must not decide which of a player's slots gets drained --
     * especially since {@code CraftingMenuLayout.GRID_SLOTS} is a {@code Set.copyOf}, whose order
     * the JDK leaves unspecified.
     */
    private static final Comparator<Source> DRAIN_ORDER =
            Comparator.comparingInt(Source::tier)
                    .thenComparingInt(Source::amount)
                    .thenComparingInt(Source::slot);

    /**
     * Plan the gather.
     *
     * @param sources       every eligible stack. Order is irrelevant -- this sorts.
     * @param cursorAmount  what the cursor already holds.
     * @param maxStackSize  the cursor stack's own maximum, read from the ITEM rather than derived
     *                      from its Material, exactly as {@code MenuRouting.merge} does.
     * @return the draws to perform, in the order to perform them. Empty when the cursor is already
     *         full, when nothing is eligible, or when the inputs are nonsense -- an empty plan is
     *         always a safe answer, because performing nothing loses nothing.
     */
    public static List<Draw> plan(Collection<Source> sources, int cursorAmount, int maxStackSize) {
        List<Draw> draws = new ArrayList<>();
        if (sources == null || maxStackSize <= 0) return draws;

        int room = maxStackSize - Math.max(cursorAmount, 0);
        if (room <= 0) return draws;

        List<Source> ordered = new ArrayList<>(sources);
        ordered.sort(DRAIN_ORDER);

        for (Source source : ordered) {
            if (room <= 0) break;
            if (source.amount() <= 0) continue;

            int take = Math.min(room, source.amount());
            draws.add(new Draw(source, take));
            room -= take;
        }
        return draws;
    }

    /** How much a plan gathers in total. The caller's conservation check, and the tests'. */
    public static int total(Collection<Draw> draws) {
        int sum = 0;
        for (Draw draw : draws) sum += draw.amount();
        return sum;
    }
}
