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
     *
     * <h2>*** REWRITTEN, NOT ADJUSTED, FOR THE 2026-09-18 RULING: PAGE 1 IS FREE ***</h2>
     *
     * <p>This row read {@code assertEquals(20, unlockLevel(0))}. <b>Page 1 now has no threshold at
     * all</b> and the 20 moved onto the hub shortcut. The six page thresholds did not move -- so
     * the honest edit is a rewritten claim about page 1 plus an untouched claim about pages 2-7,
     * rather than one number changed and the sentence left describing the old ladder.
     */
    @Test
    void pageOneIsFreeAndPagesTwoToSevenAreBensNumbers() {
        assertEquals(VaultPageGate.FREE, VaultPageGate.unlockLevel(0));
        assertTrue(VaultPageGate.isFree(0), "page 1 is free to everybody");

        assertEquals(25, VaultPageGate.unlockLevel(1));
        assertEquals(30, VaultPageGate.unlockLevel(2));
        assertEquals(35, VaultPageGate.unlockLevel(3));
        assertEquals(40, VaultPageGate.unlockLevel(4));
        assertEquals(45, VaultPageGate.unlockLevel(5));
        assertEquals(50, VaultPageGate.unlockLevel(6));
    }

    /**
     * *** PAGE 1 IS OPEN AT LEVEL 1, WHICH IS THE WHOLE OF THE NEW RULING. ***
     *
     * <p>Staged at level 1 rather than 19: 19 is one below the OLD threshold and a build that had
     * merely lowered the number would pass there. <b>1 is below anything anybody could have
     * written.</b>
     */
    @Test
    void pageOneIsOpenAtLevelONE() {
        assertTrue(VaultPageGate.unlocked(0, 1));
        assertEquals(1, VaultPageGate.unlockedPageCount(1), "exactly one page, and it is page 1");
    }

    /**
     * And page 1 is the ONLY free page.
     *
     * <p>Without this, the row above is equally consistent with a build that freed the whole vault
     * -- which would be a far larger ruling than the one that was made, and would read as working.
     */
    @Test
    void pageOneIsTheONLYFreePage() {
        assertTrue(VaultPageGate.isFree(0));
        for (int page = 1; page < VaultShape.PAGE_COUNT; page++) {
            assertFalse(VaultPageGate.isFree(page), "page " + (page + 1) + " must still be earned");
            assertFalse(VaultPageGate.unlocked(page, 1), "and must be shut at level 1");
        }
    }

    /**
     * *** 20 STILL HAS A JOB, AND IT IS THE HUB SHORTCUT. ***
     *
     * <p>All seven of Ben's numbers survived the ruling: 20 buys the convenience route, the other
     * six buy pages. A build that deleted the 20 along with page 1's threshold would open the
     * station cell at level 1 -- <b>silently, because a station with a threshold of zero renders as
     * unlocked and nothing throws.</b>
     */
    @Test
    void twentyMovedToTheHubShortcutRatherThanDisappearing() {
        assertEquals(20, VaultPageGate.HUB_SHORTCUT_LEVEL);
        org.junit.jupiter.api.Assertions.assertNotEquals(
                VaultPageGate.FREE, VaultPageGate.HUB_SHORTCUT_LEVEL,
                "the shortcut is NOT free, which is the difference the ruling created");
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
        // `>= FREE` RATHER THAN `> 0`, AND THE CHANGE IS THE RULING. This asserted a POSITIVE
        // threshold for every page, which page 1 no longer has. What the row is for is unchanged:
        // the array has an entry per page, so no page falls off the end of it.
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            assertTrue(VaultPageGate.unlockLevel(page) >= VaultPageGate.FREE,
                    "page " + page + " has no entry in the ladder");
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
        // RE-DERIVED FOR THE NEW LADDER (free / 25 / 30 / 35 / 40 / 45 / 50), not adjusted by one.
        // The old row read 0 at level 1 and 1 at level 20; the floor is now 1 everywhere, and the
        // first EARNED page arrives at 25.
        assertEquals(1, VaultPageGate.unlockedPageCount(1), "page 1 is free, so nobody has zero");
        assertEquals(1, VaultPageGate.unlockedPageCount(19));
        assertEquals(1, VaultPageGate.unlockedPageCount(20),
                "20 buys the HUB SHORTCUT, not a page -- the count must not move here");
        assertEquals(1, VaultPageGate.unlockedPageCount(24));
        assertEquals(2, VaultPageGate.unlockedPageCount(25), "the first EARNED page");
        assertEquals(3, VaultPageGate.unlockedPageCount(34));
        assertEquals(4, VaultPageGate.unlockedPageCount(35));
        assertEquals(VaultShape.PAGE_COUNT, VaultPageGate.unlockedPageCount(50));
        assertEquals(VaultShape.PAGE_COUNT, VaultPageGate.unlockedPageCount(99));
    }

    @Test
    void everyLevelHasSomethingUnlockedNow() {
        // *** THIS ROW WAS `nothingIsUnlockedBelowTwenty` AND ITS CLAIM IS NOW FALSE. ***
        //
        // It asserted that a sub-20 player had no vault at all, which was the old ruling's whole
        // shape. Renamed rather than edited in place, because the NAME carried the claim and a row
        // called "nothing is unlocked below twenty" asserting the opposite is worse than either.
        assertTrue(VaultPageGate.anyUnlocked(1), "page 1 is free, so there is no such player");
        assertTrue(VaultPageGate.anyUnlocked(19));
        assertTrue(VaultPageGate.anyUnlocked(20));
    }

    /**
     * *** A FREE PAGE HAS NO REFUSAL MESSAGE, AND ASKING FOR ONE THROWS. ***
     *
     * <p>The naive formatting is {@code "Vault page 1 unlocks at level 0"} -- grammatical, plausible,
     * and about nothing. <b>No shipped path reaches it</b>, because a free page is never locked and
     * the click never refuses, which is precisely why it needs a throw rather than a comment: an arm
     * nothing exercises is an arm nobody would notice was lying.
     */
    @Test
    void askingForAFreePagesRefusalThrowsRatherThanSayingLevelZero() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> VaultPageGate.refusal(0, 5));
        assertTrue(thrown.getMessage().contains("free"), thrown.getMessage());

        // AND THE CONTROL: an earned page still formats, so the throw is about freeness and not
        // about the method being broken.
        assertTrue(VaultPageGate.refusal(1, 5).contains("level 25"));
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
