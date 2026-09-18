package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** THE OPT-OUT IS LOAD-BEARING, AND BOTH ANSWERS ARE EXERCISED HERE. ***
 *
 * <h2>WHY THIS FILE EXISTS AT ALL</h2>
 *
 * The vault is the only menu in the plugin that does not hand its contents back on close, and that
 * opt-out is what makes it storage rather than a workbench. <b>It is also one boolean away from
 * being a shredder</b>: if a write fails and the screen still returns nothing, the items are in no
 * file and are handed to nobody.
 *
 * <p>Ben's condition when he accepted that design, 2026-09-17: <i>"the override is no longer a
 * constant and the 'opt-out is load-bearing' test must exercise BOTH answers."</i> A test that only
 * ever saw the empty answer could not tell a working degrade path from one that never fires.
 *
 * <h2>*** AND THE DEGRADED ANSWER WAS "EVERYTHING" UNTIL IT WAS READ AS A DUPLICATOR ***</h2>
 *
 * Returning every cell hands the player a page the FILE STILL HOLDS -- poisoning leaves disk at its
 * last good copy. The rows below now pin the DIFFERENCE, and the overlap case is the one that was
 * missing entirely.
 */
class VaultReturnPolicyTest {

    /**
     * Three cells, and <b>not cells 0-2</b>: a set staged on the low indices cannot distinguish a
     * policy that returns its input from one that returns a fresh range.
     */
    private static final Set<Integer> CELLS = Set.of(17, 24, 31);

    /** HEALTHY: nothing comes back, because the items are on disk. */
    @Test
    void aHealthyVaultReturnsNothing() {
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(false, CELLS, Set.of()));
    }

    /**
     * HEALTHY beats everything else, including a disk view that disagrees.
     *
     * <p>Staged with an {@code alreadyOnDisk} that is deliberately WRONG -- empty, while the cells
     * are in fact persisted. A build that computed the difference first and checked {@code degraded}
     * second would hand a healthy vault's entire page back on every close.
     */
    @Test
    void aHealthyVaultReturnsNothingEvenWhenNothingLooksPersisted() {
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(false, CELLS, Set.of()));
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(false, CELLS, CELLS));
    }

    /** DEGRADED with nothing on disk: everything comes back, because nothing else has it. */
    @Test
    void aDegradedVaultWithNothingPersistedReturnsEveryCell() {
        assertEquals(CELLS, VaultReturnPolicy.returnedSlots(true, CELLS, Set.of()));
    }

    /**
     * *** THE OVERLAP CASE: ONLY THE CELLS THE FILE DOES NOT HAVE. ***
     *
     * <p>Cell 24 is on disk; 17 and 31 are not. A build that returned the whole page would hand back
     * a cell the vault still holds -- **the player has two, and the count of duplicated stacks is the
     * size of the page.**
     */
    @Test
    void aDegradedVaultReturnsOnlyWhatIsNotAlreadyOnDisk() {
        assertEquals(Set.of(17, 31), VaultReturnPolicy.returnedSlots(true, CELLS, Set.of(24)));
    }

    /**
     * Fully persisted: nothing comes back, even degraded.
     *
     * <p>This is the common shape of a failed write that changed ONE cell -- the other thirty-five
     * are exactly as the file has them, and handing them over would duplicate every one.
     */
    @Test
    void aDegradedVaultWhoseCellsAreAllPersistedReturnsNothing() {
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(true, CELLS, CELLS));
    }

    /**
     * A disk view naming cells that are not inputs changes nothing.
     *
     * <p>It happens: a cell holding an entry this server cannot decode is on disk and is NOT an
     * input slot. Subtracting it must not remove anything else, and must not throw.
     */
    @Test
    void persistedCellsOutsideTheInputSetAreIgnored() {
        assertEquals(CELLS, VaultReturnPolicy.returnedSlots(true, CELLS, Set.of(4, 9, 44)));
    }

    /**
     * The two answers DIFFER, asserted as one claim.
     *
     * <p>Without it, the rows above are satisfied by a policy that ignores {@code degraded} and
     * returns whatever the subtraction produces. <b>This row is what makes the boolean
     * load-bearing.</b>
     */
    @Test
    void theTwoAnswersAreNotTheSame() {
        Set<Integer> healthy = VaultReturnPolicy.returnedSlots(false, CELLS, Set.of());
        Set<Integer> degraded = VaultReturnPolicy.returnedSlots(true, CELLS, Set.of());

        assertTrue(healthy.isEmpty(), "healthy must be empty");
        assertEquals(3, degraded.size(), "degraded with nothing persisted must be the whole page");
    }

    /**
     * An empty input is not a special case, in either state.
     *
     * <p>It is a real one: every cell of the page is opaque, or the page is genuinely empty.
     */
    @Test
    void anEmptyPageReturnsNothingWhicheverStateItIsIn() {
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(false, Set.of(), Set.of()));
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(true, Set.of(), Set.of()));
    }

    /**
     * *** THE RESULT IS A COPY, AND IT IS NOT AN ALIAS OF THE MENU'S LIVE VIEW. ***
     *
     * <p>{@code returnEverything} iterates the returned set while CLEARING the cells it names. If
     * this handed back the caller's own mutable set, a screen that recomputed that set from its
     * cells would be mutating the collection being iterated -- and the visible symptom is not a
     * crash, it is a page that came back half-returned.
     */
    @Test
    void theReturnedSetIsACopyAndIsImmutable() {
        Set<Integer> live = new LinkedHashSet<>(Set.of(17, 24, 31));

        Set<Integer> returned = VaultReturnPolicy.returnedSlots(true, live, Set.of());

        live.clear();
        assertEquals(3, returned.size(), "clearing the caller's set must not empty the answer");
        assertThrows(UnsupportedOperationException.class, () -> returned.remove(17),
                "and the answer itself cannot be edited by whoever receives it");
    }
}
