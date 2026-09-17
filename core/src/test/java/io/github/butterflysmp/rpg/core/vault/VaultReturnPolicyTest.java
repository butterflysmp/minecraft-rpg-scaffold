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
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(false, CELLS));
    }

    /** DEGRADED: everything comes back, because the items are in no file. */
    @Test
    void aDegradedVaultReturnsEveryInputCell() {
        assertEquals(CELLS, VaultReturnPolicy.returnedSlots(true, CELLS));
    }

    /**
     * The two answers DIFFER, asserted as one claim.
     *
     * <p>The two rows above are both satisfied by a policy that ignores {@code degraded} and returns
     * whatever its second argument is -- <b>pass {@code Set.of()} as the cells and they both pass on
     * a build with no conditional at all.</b> This row is what makes the boolean load-bearing.
     */
    @Test
    void theTwoAnswersAreNotTheSame() {
        Set<Integer> healthy = VaultReturnPolicy.returnedSlots(false, CELLS);
        Set<Integer> degraded = VaultReturnPolicy.returnedSlots(true, CELLS);

        assertTrue(healthy.isEmpty(), "healthy must be empty");
        assertEquals(3, degraded.size(), "degraded must be the whole page");
    }

    /**
     * An empty input is not a special case, in either state.
     *
     * <p>It is a real one: every cell of the page is opaque, or the page is genuinely empty. A
     * degraded empty page returns nothing because there is nothing to return -- which must not be
     * confused with the healthy answer, and is why the row above compares the two on a NON-empty
     * input.
     */
    @Test
    void anEmptyPageReturnsNothingWhicheverStateItIsIn() {
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(false, Set.of()));
        assertEquals(Set.of(), VaultReturnPolicy.returnedSlots(true, Set.of()));
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

        Set<Integer> returned = VaultReturnPolicy.returnedSlots(true, live);

        live.clear();
        assertEquals(3, returned.size(), "clearing the caller's set must not empty the answer");
        assertThrows(UnsupportedOperationException.class, () -> returned.remove(17),
                "and the answer itself cannot be edited by whoever receives it");
    }
}
