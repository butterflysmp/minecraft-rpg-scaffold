package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seven thresholds, and the two places an off-by-one would hide. */
class VaultPageGateTest {

    /**
     * Ben's numbers, pinned individually.
     *
     * <p><b>Written out rather than looped over an expected array</b>, which would be the same
     * literal twice and could not fail. Page 4's 35 is the row the brief dropped.
     */
    @Test
    void theSevenThresholdsAreBensNumbers() {
        assertEquals(20, VaultPageGate.unlockLevel(0));
        assertEquals(25, VaultPageGate.unlockLevel(1));
        assertEquals(30, VaultPageGate.unlockLevel(2));
        assertEquals(35, VaultPageGate.unlockLevel(3));
        assertEquals(40, VaultPageGate.unlockLevel(4));
        assertEquals(45, VaultPageGate.unlockLevel(5));
        assertEquals(50, VaultPageGate.unlockLevel(6));
    }

    /**
     * There is exactly one threshold per page.
     *
     * <p>The class asserts this at LOAD time and throws if it is wrong, so this row cannot observe
     * the failure -- by the time it runs, the class either loaded or no test in this file ran at
     * all. What it CAN check is the arithmetic the load-time assertion protects: the last page has a
     * threshold and one past it does not exist.
     */
    @Test
    void everyPageHasAThresholdAndNothingPastTheLastOne() {
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            assertTrue(VaultPageGate.unlockLevel(page) > 0, "page " + page + " has no threshold");
        }
        assertThrows(IllegalArgumentException.class,
                () -> VaultPageGate.unlockLevel(VaultShape.PAGE_COUNT));
        assertThrows(IllegalArgumentException.class, () -> VaultPageGate.unlockLevel(-1));
    }

    /**
     * Inclusive at the boundary, in both directions.
     *
     * <p>Staged at page 3 (threshold 35) rather than page 0: <b>page 0's threshold is 20 and the
     * page index is 0</b>, so a row there cannot tell a page index from a page number. 3 and 35
     * share no digits with each other or with the index.
     */
    @Test
    void theBoundaryIsInclusiveAndOneBelowIsLocked() {
        assertTrue(VaultPageGate.unlocked(3, 35));
        assertFalse(VaultPageGate.unlocked(3, 34));
        assertTrue(VaultPageGate.unlocked(3, 36));
    }

    /**
     * The count, at the three values where it can be wrong.
     *
     * <p>Level 34 is the interesting one: three pages, not four. A build that compared {@code >}
     * instead of {@code >=} would give 3 at level 35 and pass a row staged anywhere else.
     */
    @Test
    void unlockedPageCountWalksTheThresholds() {
        assertEquals(0, VaultPageGate.unlockedPageCount(1));
        assertEquals(0, VaultPageGate.unlockedPageCount(19));
        assertEquals(1, VaultPageGate.unlockedPageCount(20));
        assertEquals(3, VaultPageGate.unlockedPageCount(34));
        assertEquals(4, VaultPageGate.unlockedPageCount(35));
        assertEquals(VaultShape.PAGE_COUNT, VaultPageGate.unlockedPageCount(50));
        assertEquals(VaultShape.PAGE_COUNT, VaultPageGate.unlockedPageCount(99));
    }

    @Test
    void nothingIsUnlockedBelowTwenty() {
        assertFalse(VaultPageGate.anyUnlocked(19));
        assertTrue(VaultPageGate.anyUnlocked(20));
    }

    /**
     * The refusal names the PLAYER-FACING page number, the level it wants, and where they stand.
     *
     * <p><b>Staged so no two numbers in the row are equal</b>, which is what lets it detect a
     * transposition: page index 3 renders as page 4, the threshold is 35, the player is 34. A row
     * staged at page 0 level 20 could not tell {@code page + 1} from {@code page}.
     */
    @Test
    void theRefusalSaysBothFactsAndConvertsThePageNumber() {
        String refusal = VaultPageGate.refusal(3, 34);

        assertTrue(refusal.contains("page 4"), refusal);
        assertFalse(refusal.contains("page 3"), "the 0-based index must not reach the player: " + refusal);
        assertTrue(refusal.contains("level 35"), refusal);
        assertTrue(refusal.contains("You are level 34"), refusal);
    }
}
