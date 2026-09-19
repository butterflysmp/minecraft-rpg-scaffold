package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The write-time pair: what counts as empty, and why the description cannot be null. */
class VaultCellTest {

    /**
     * Blank counts as empty, matching {@code PlayerVault.withPage}'s own rule.
     *
     * <p><b>The two must agree or a cell is reported in a failure log and then dropped by the
     * record</b> -- an operator chasing an item that was never going to be written. Blank is the
     * case that diverges: null is obviously empty to anybody, and {@code "   "} is obviously empty
     * to neither.
     */
    @Test
    void nullAndBlankAreBothEmpty() {
        assertTrue(new VaultCell(null, "anything").isEmpty());
        assertTrue(new VaultCell("", "anything").isEmpty());
        assertTrue(new VaultCell("   ", "anything").isEmpty());
        assertFalse(new VaultCell("aaaa", "anything").isEmpty());
    }

    /**
     * A null description is refused at construction.
     *
     * <p>Not defaulted, because a cell with no description is a cell the failure log cannot report,
     * and the caller is the only one who knows what to call it. {@link VaultCell#UNDESCRIBED} is the
     * answer for "genuinely nothing to say", and it is a visible string rather than a silent
     * {@code "null"} in a log line an operator is meant to act on.
     */
    @Test
    void aNullDescriptionIsRefusedRatherThanDefaulted() {
        assertThrows(NullPointerException.class, () -> new VaultCell("aaaa", null));
    }

    @Test
    void anEmptyCellStillCarriesADescription() {
        assertTrue(VaultCell.empty().isEmpty());
        assertEquals(VaultCell.UNDESCRIBED, VaultCell.empty().description());
    }
}
