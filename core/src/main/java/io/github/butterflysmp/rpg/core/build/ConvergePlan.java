package io.github.butterflysmp.rpg.core.build;

import java.util.List;

/**
 * What a locked item's convergence must WRITE, decided without an inventory: the pure half of
 * {@code NexusSlots.converge}, which only executes this.
 *
 * <p><b>THE SPLIT WAS A NAMED DEBT, AND THIS SLICE WAS ITS TRIGGER.</b> {@code NexusSlots.converge}'s
 * javadoc deferred it (2026-09-16) "to the next slice that opens this method for any other reason".
 * The Ability Stone opens it -- convergence now runs for two items -- so the split is taken. Its payoff
 * is named there: {@code MUTWELDDEFAULT} (ignore the chosen slot, always use the default) left the whole
 * suite green, and {@code ConvergePlanTest} is what kills it now.
 *
 * <p>The rules, unchanged from the star's:
 * <ul>
 *   <li>an out-of-range chosen slot -- including "not known" (negative) -- falls back to the default;</li>
 *   <li>every copy but the LOWEST-index one is deleted (surplus locked items are ours, not the
 *       player's, so deleting them is not destroying property);</li>
 *   <li>one copy already at the target is a no-op;</li>
 *   <li>otherwise the survivor is lifted into the target, or a fresh one is minted if there is none;</li>
 *   <li>whatever sat in the target is displaced -- the executor hands it back, never deletes it.</li>
 * </ul>
 *
 * @param clear   indices to empty (the surplus copies)
 * @param source  the index to lift the surviving copy from, or {@link #MINT} to mint a fresh one
 * @param target  where it must end up
 * @param noop    nothing to do beyond {@code clear} (which is then empty)
 */
public record ConvergePlan(List<Integer> clear, int source, int target, boolean noop) {

    /** "No copy exists: mint one." */
    public static final int MINT = -1;

    public ConvergePlan {
        clear = List.copyOf(clear);
    }

    /**
     * @param found       every index currently holding the item, ascending
     * @param chosenSlot  the player's chosen slot, or any negative for "not known"
     * @param defaultSlot the fallback
     * @param maxSlot     the highest index the item may occupy
     */
    public static ConvergePlan of(List<Integer> found, int chosenSlot, int defaultSlot, int maxSlot) {
        // READ ONCE: the target is decided here and never re-derived, so the no-op test and the write
        // cannot compare against two different slots.
        int target = chosenSlot >= 0 && chosenSlot <= maxSlot ? chosenSlot : defaultSlot;
        List<Integer> surplus = found.size() > 1 ? found.subList(1, found.size()) : List.of();
        int held = found.isEmpty() ? MINT : found.get(0);
        boolean noop = held == target && found.size() == 1;
        return new ConvergePlan(surplus, held, target, noop);
    }
}
