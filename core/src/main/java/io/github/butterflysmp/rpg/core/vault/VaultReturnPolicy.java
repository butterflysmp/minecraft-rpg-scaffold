package io.github.butterflysmp.rpg.core.vault;

import java.util.Set;

/**
 * What the vault hands back when its screen closes.
 *
 * <h2>*** ONE TERNARY, IN {@code core}, BECAUSE THE ALTERNATIVE CANNOT BE EXECUTED ***</h2>
 *
 * This is the {@code Menu.returnedSlots()} decision, and it is a decision the vault's whole safety
 * argument rests on:
 *
 * <pre>
 *   HEALTHY    return NOTHING      the items are on disk; that is what makes it storage
 *   DEGRADED   return EVERYTHING   a write failed, so the cells are the player's only copy
 * </pre>
 *
 * <p><b>It is extracted rather than inlined because the override is no longer a constant, and a
 * conditional safety hook has to be EXERCISED rather than read.</b> {@code Menu} cannot be
 * constructed without a running server -- {@code Bukkit.createInventory} -- so a test of
 * {@code NexusVaultMenu.returnedSlots} could only assert that the method exists, by scanning source.
 * That is the shape this project files under <i>a guard that cannot fire is indistinguishable from
 * one that protects you</i>.
 *
 * <p>Here both answers run in a unit test, which was Ben's condition on accepting the degraded
 * design: <i>"the override is no longer a constant and the 'opt-out is load-bearing' test must
 * exercise BOTH answers."</i> {@code GridClickIntent} was carved out of {@code MenuRouting} for the
 * identical reason.
 *
 * <h2>THE CURSOR IS NOT IN HERE, AND THAT IS NOT AN OMISSION</h2>
 *
 * {@code Menu.returnEverything} hands a cursor item back unconditionally, outside this policy. An
 * item on the cursor is in flight and is the player's whatever the menu believes about its own
 * cells, so it is not a decision to make per state.
 */
public final class VaultReturnPolicy {

    private VaultReturnPolicy() {}

    /**
     * The cells to hand back on close.
     *
     * @param degraded   has a write failed this session? Once true it stays true: the vault is
     *                   poisoned, so no later gesture could persist anything.
     * @param inputSlots the cells that hold the player's items -- the storage cells minus any
     *                   holding an entry this server could not decode, which never became items on
     *                   screen and must not be handed to anybody.
     * @return {@code inputSlots} while degraded, and an EMPTY set while healthy.
     */
    public static Set<Integer> returnedSlots(boolean degraded, Set<Integer> inputSlots) {
        // NOT `degraded ? inputSlots : Set.of()` with no copy: the caller's set is the menu's live
        // view of its own cells, and returnEverything iterates this while CLEARING those cells. An
        // aliased view that recomputed itself mid-iteration would be a ConcurrentModificationException
        // at best and a half-returned page at worst.
        return degraded ? Set.copyOf(inputSlots) : Set.of();
    }
}
