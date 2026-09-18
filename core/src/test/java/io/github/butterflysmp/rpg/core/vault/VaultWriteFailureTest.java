package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The failure report's CONTENT, because the failure POLICY rests on it.
 *
 * <h2>*** THIS FILE IS NOT LOG-FORMATTING PEDANTRY, AND IT IS THE ONE ROW THAT SAYS SO ***</h2>
 *
 * The vault accepts a duplication arm over a destruction arm, and the entire argument for that
 * choice is that a duplicate is <b>reversible by an operator</b>. Reversible requires knowing WHO,
 * WHICH PAGE, WHICH SLOT and WHAT -- so a report missing any of those does not merely read poorly,
 * it <b>retroactively invalidates the decision that was taken on its strength.</b>
 *
 * <p>Ben's condition when he accepted the residual, 2026-09-17: <i>"assert the content of that line
 * in a test -- it is load-bearing, not logging."</i>
 */
class VaultWriteFailureTest {

    private static final UUID PLAYER = UUID.fromString("c41a9b03-5e7d-4268-91bf-3a0d6c85e29b");

    /**
     * Staged so that <b>no two quantities the row reads are equal</b>: storage page 2, player-facing
     * page 3, slots 17 and 31, two items with different names.
     *
     * <p>A report staged at page 0 slot 0 could not detect the page conversion, a swap of page for
     * slot, or a build that printed the index where the player-facing number belongs.
     */
    private static Map<Integer, VaultCell> twoItems() {
        Map<Integer, VaultCell> cells = new LinkedHashMap<>();
        // Deliberately NOT in slot order, so the sort is doing something.
        cells.put(31, new VaultCell("cccc", "OAK_LOG x64"));
        cells.put(17, new VaultCell("aaaa", "DIAMOND_SWORD x1 (Boltor)"));
        return cells;
    }

    @Test
    void theReportNamesThePlayerThePageAndEveryItem() {
        String report = VaultWriteFailure.report(PLAYER, 2, twoItems());

        assertTrue(report.contains(PLAYER.toString()), report);
        assertTrue(report.contains("storage page 2"), report);
        assertTrue(report.contains("page 3 as the player sees it"), report);
        assertTrue(report.contains("slot 17: DIAMOND_SWORD x1 (Boltor)"), report);
        assertTrue(report.contains("slot 31: OAK_LOG x64"), report);
    }

    /**
     * The three facts that make it actionable rather than a notification.
     *
     * <p>Asserted as SEPARATE claims because they are three different decisions -- the write did not
     * land, nothing further will be written, and the same item may now exist twice -- and a report
     * can carry any two of them while being useless.
     */
    @Test
    void theReportSaysNotOnDiskPoisonedAndPossiblyDuplicated() {
        String report = VaultWriteFailure.report(PLAYER, 2, twoItems());

        assertTrue(report.contains("did NOT reach disk"), report);
        assertTrue(report.contains("POISONED"), report);
        assertTrue(report.contains("shutdown flush skips it"), report);
        assertTrue(report.contains("reverse by hand"), report);
    }

    /**
     * Slots ascend, whatever order the caller handed them over in.
     *
     * <p>Two reports of one incident must read the same way; the caller's map is an inventory
     * snapshot whose iteration order is not a promise. Same reason {@code PlayerVault} sorts.
     */
    @Test
    void slotsAreListedInAscendingOrder() {
        String report = VaultWriteFailure.report(PLAYER, 2, twoItems());

        assertTrue(report.indexOf("slot 17") < report.indexOf("slot 31"), report);
    }

    /**
     * An empty cell is not listed. There is nothing to reverse about a cell holding nothing.
     *
     * <p>The control is the OTHER slot in the same report: a build that dropped the emptiness check
     * would list slot 5 as {@code "UNDECODABLE -- kept verbatim"}, which is a real message for a
     * real case and would read as correct.
     */
    @Test
    void emptyCellsAreSkipped() {
        Map<Integer, VaultCell> cells = new LinkedHashMap<>();
        cells.put(5, VaultCell.empty());
        cells.put(17, new VaultCell("aaaa", "DIAMOND_SWORD x1 (Boltor)"));

        String report = VaultWriteFailure.report(PLAYER, 2, cells);

        assertTrue(report.contains("slot 17"), report);
        assertFalse(report.contains("slot 5"), report);
    }

    /**
     * *** A FAILED WRITE OF AN EMPTY PAGE SAYS WHAT IS WRONG, RATHER THAN LISTING NOTHING. ***
     *
     * <p>This is the take-out-the-last-item case, and it is the most likely incident to be dismissed:
     * there is no cell to name, so a naive report ends on a colon with nothing under it and reads
     * like a spurious warning. What is wrong is what the page NO LONGER contains -- the file still
     * holds the item the player is walking away with.
     */
    @Test
    void aClearingWriteThatFailsSaysTheFileStillHoldsTheOldContents() {
        String report = VaultWriteFailure.report(PLAYER, 2, Map.of());

        assertTrue(report.contains("being CLEARED"), report);
        assertTrue(report.contains("still holds"), report);
        assertFalse(report.contains("slot "), "nothing to list, so no slot line: " + report);
    }

    /** A page outside the vault is a programming error, not a message to format. */
    @Test
    void anImpossiblePageIsRefusedRatherThanReported() {
        assertThrows(IllegalArgumentException.class,
                () -> VaultWriteFailure.report(PLAYER, VaultShape.PAGE_COUNT, Map.of()));
    }
}
