package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four close paths, and which of them takes responsibility for the unpersisted cells.
 *
 * <h2>*** THE ROW THIS FILE EXISTS FOR IS {@code aDegradedOrdinaryCloseCLAIMS}. ***</h2>
 *
 * A reviewer walked the arbitration on the previous tip and read the degraded-Esc path as leaving
 * the flag unclaimed -- which would let a later failure callback drop cells the close had already
 * handed back. <b>It was claimed, in a statement after {@code returnEverything} whose correctness
 * rested on preceding the {@code closed} write the drop arm gates on.</b>
 *
 * <p>The behaviour was right and unexecutable, so a careful reading of it produced a confident,
 * detailed and wrong bug report. That is the cost of an invariant that lives in statement order:
 * <b>nobody can be shown to be wrong about it.</b> Here they can.
 */
class VaultCloseDisposalTest {

    /** A healthy close does nothing and must NOT claim -- the async arm is the only net left. */
    @Test
    void aHealthyCloseDoesNothingAndLeavesTheFlagFree() {
        assertEquals(VaultCloseDisposal.NOTHING, VaultCloseDisposal.of(false, false));
        assertEquals(VaultCloseDisposal.NOTHING, VaultCloseDisposal.of(false, true));
        assertFalse(VaultCloseDisposal.of(false, false).claimsTheCells(),
                "claiming here would silence the drop arm, which is the ONLY thing covering a"
                        + " failure that arrives after this close");
    }

    /**
     * *** A DEGRADED ORDINARY CLOSE HANDS BACK -- AND CLAIMS. ***
     *
     * <p>{@code returnEverything} disposes of the cells just as surely as a drop does. A close that
     * handed them to the player without claiming would leave a later failure callback free to win
     * the flag and put the same items on the floor: TWO COPIES.
     */
    @Test
    void aDegradedOrdinaryCloseHandsBackAndCLAIMS() {
        VaultCloseDisposal disposal = VaultCloseDisposal.of(true, false);

        assertEquals(VaultCloseDisposal.HAND_BACK, disposal);
        assertTrue(disposal.claimsTheCells(),
                "HAND_BACK disposes of the cells, so it must claim -- folding the disconnect test"
                        + " into the claim is the duplication bug this row exists for");
    }

    /** A degraded disconnect drops, and claims. */
    @Test
    void aDegradedDisconnectDropsAndClaims() {
        VaultCloseDisposal disposal = VaultCloseDisposal.of(true, true);

        assertEquals(VaultCloseDisposal.DROP, disposal);
        assertTrue(disposal.claimsTheCells());
    }

    /**
     * BOTH degraded arms claim, asserted as one statement.
     *
     * <p>The rows above are each satisfied by a build that claims for only its own arm. This is the
     * invariant in the form it is actually needed: <b>whoever disposes of the cells claims.</b>
     */
    @Test
    void everyDisposalThatTOUCHESTheCellsClaimsThem() {
        for (VaultCloseDisposal disposal : VaultCloseDisposal.values()) {
            boolean touches = disposal != VaultCloseDisposal.NOTHING;
            assertEquals(touches, disposal.claimsTheCells(),
                    disposal + " must claim exactly when it disposes of the cells");
        }
    }

    /**
     * The disconnect flag chooses the DESTINATION and never the claim.
     *
     * <p>Stated as its own row because that separation is the fix: the reviewer's prescription was
     * to claim on any degraded close and use DISCONNECT only to pick drop versus hand back.
     */
    @Test
    void theDisconnectFlagChangesWhereTheItemsGoAndNotWhetherTheyAreClaimed() {
        assertEquals(VaultCloseDisposal.of(true, false).claimsTheCells(),
                VaultCloseDisposal.of(true, true).claimsTheCells(),
                "both degraded closes claim; only the destination differs");
        org.junit.jupiter.api.Assertions.assertNotEquals(
                VaultCloseDisposal.of(true, false), VaultCloseDisposal.of(true, true),
                "and the destination really does differ, or this row is comparing one case to itself");
    }
}
