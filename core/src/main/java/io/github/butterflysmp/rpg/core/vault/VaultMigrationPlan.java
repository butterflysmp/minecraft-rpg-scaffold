package io.github.butterflysmp.rpg.core.vault;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where a player's vanilla ender chest lands on vault page 1, and what does not fit.
 *
 * <h2>*** SLOT-FOR-SLOT, AND OCCUPIED CELLS ARE SKIPPED RATHER THAN OVERWRITTEN ***</h2>
 *
 * The ender chest is 27 cells and a vault page is 36, so the chest lands in the top three rows and
 * the fourth row stays free. <b>Slot-for-slot rather than packed</b>, so the arrangement a player
 * built in their chest is the arrangement they find in the vault -- a packed migration would
 * silently rearrange everything they own.
 *
 * <p><b>An occupied page cell is SKIPPED.</b> It cannot happen on a first migration through the
 * screen, and it can happen today through {@code /rpg vault store}, which wrote to page 1 during
 * PR 1's gate rows. Overwriting would destroy the vault's own item; refusing the whole migration
 * would strand it forever. Skipping cannot lose anything, <b>because the ender chest is never
 * cleared</b> -- a skipped stack is still exactly where the player left it.
 *
 * <h2>THE CHEST IS NOT CLEARED, AND THAT IS A STANDING RULING RATHER THAN THIS CLASS'S CHOICE</h2>
 *
 * So migration is a COPY, and for a while the same items exist in two places. That is deliberate:
 * the vanilla chest is unreachable once the block is hijacked, so the copy is not a duplicate
 * anybody can spend -- and if the hijack is ever rolled back, the player's things are still in the
 * chest where they left them, which a clear-on-migrate would have made unrecoverable.
 *
 * <p>Pure: slot indices in, slot indices out. <b>No items, no {@code ItemStack}, no encoding.</b>
 * The screen places the real stacks using {@link Placement#moves()} and then writes the page through
 * the same path every gesture uses, so migration has no write path of its own to get wrong.
 */
public final class VaultMigrationPlan {

    private VaultMigrationPlan() {}

    /** How many cells a vanilla ender chest has. Vanilla's number, not ours. */
    public static final int ENDER_CHEST_SLOTS = 27;

    /**
     * What to move and what to leave.
     *
     * @param moves   chest slot to page slot, in ascending chest order. Slot-for-slot today, so
     *                every entry maps a key to itself -- <b>it is a map rather than a set because
     *                the caller must not have to know that</b>. A later ruling that packs the
     *                migration changes this class and nothing else.
     * @param skipped chest slots whose page cell was already occupied, ascending. <b>Not an
     *                error</b>: those stacks stay in the ender chest, which is never cleared.
     */
    public record Placement(Map<Integer, Integer> moves, List<Integer> skipped) {

        public Placement {
            moves = Map.copyOf(moves);
            skipped = List.copyOf(skipped);
        }

        /** Did this plan find anything to do? A migration that moves nothing still stamps. */
        public boolean isEmpty() {
            return moves.isEmpty();
        }
    }

    /**
     * Plan the copy.
     *
     * @param occupiedPageSlots which cells of vault page 1 already hold something. Includes cells
     *                          holding an <b>undecodable</b> entry, which is the case that matters:
     *                          this server cannot render it, so the screen must not place anything
     *                          on top of it.
     * @param filledChestSlots  which cells of the ender chest hold something.
     */
    public static Placement plan(Set<Integer> occupiedPageSlots, Set<Integer> filledChestSlots) {
        Map<Integer, Integer> moves = new LinkedHashMap<>();
        List<Integer> skipped = new ArrayList<>();

        // Ascending, so the result is deterministic and a test can read it. A HashSet's iteration
        // order is not the insertion order and not the natural order, and a migration whose output
        // depends on it would be a fixture nobody could stage twice.
        for (int chestSlot = 0; chestSlot < ENDER_CHEST_SLOTS; chestSlot++) {
            if (!filledChestSlots.contains(chestSlot)) continue;

            // The identity mapping is the whole placement rule today. Written as a variable rather
            // than inlined so the packed alternative is a one-line change here.
            int pageSlot = chestSlot;

            // Defensive, and it cannot fire while ENDER_CHEST_SLOTS <= SLOTS_PER_PAGE. Asserted
            // rather than assumed because the two constants come from different authorities --
            // vanilla's chest and our page -- and nothing makes them agree.
            if (!VaultShape.isSlot(pageSlot)) {
                throw new IllegalStateException(
                        "ender chest slot " + chestSlot + " maps to page slot " + pageSlot
                                + ", which is outside a vault page of " + VaultShape.SLOTS_PER_PAGE);
            }

            if (occupiedPageSlots.contains(pageSlot)) {
                skipped.add(chestSlot);
                continue;
            }
            moves.put(chestSlot, pageSlot);
        }

        return new Placement(moves, skipped);
    }
}
