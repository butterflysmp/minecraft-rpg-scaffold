package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vault's dimensions, pinned to LITERALS.
 *
 * <p>Every figure here is written out rather than read back through the class under test. Asserting
 * {@code PAGE_COUNT == VaultShape.PAGE_COUNT} is a tautology that survives any mutation; the point of
 * this file is that a change to either factor has to be made HERE as well, deliberately, by someone
 * who has read why.
 *
 * <p><b>{@code TOTAL_SLOTS} is the interesting one.</b> It is derived in production and literal here,
 * so it fails if EITHER factor moves -- which is the one assertion the two separate pins cannot make
 * between them.
 */
class VaultShapeTest {

    @Test
    void theShapeIsSevenPagesOfThirtySix() {
        assertEquals(7, VaultShape.PAGE_COUNT, "seven pages -- the seven unlock levels depend on it");
        assertEquals(36, VaultShape.SLOTS_PER_PAGE, "four rows of nine");
    }

    @Test
    void totalSlotsIsDerivedAndCatchesEitherFactorMoving() {
        assertEquals(252, VaultShape.TOTAL_SLOTS,
                "252 is the headline figure for this feature; it is derived in production so that it"
                        + " cannot drift, and literal here so that a drift in either factor reddens");
    }

    @Test
    void pageIndicesAreZeroBasedAndBounded() {
        assertFalse(VaultShape.isPage(-1), "negative is not a page");
        assertTrue(VaultShape.isPage(0), "the first page is zero-based");
        assertTrue(VaultShape.isPage(6), "the last page is PAGE_COUNT - 1");
        assertFalse(VaultShape.isPage(7), "PAGE_COUNT itself is one past the end");
    }

    @Test
    void slotIndicesAreZeroBasedAndBounded() {
        assertFalse(VaultShape.isSlot(-1), "negative is not a slot");
        assertTrue(VaultShape.isSlot(0), "the first slot is zero-based");
        assertTrue(VaultShape.isSlot(35), "the last slot is SLOTS_PER_PAGE - 1");
        assertFalse(VaultShape.isSlot(36), "SLOTS_PER_PAGE itself is one past the end");
    }

    /**
     * The staged values are 9 and 40 -- neither is a page count, a slot count, a boundary, or equal
     * to the other. A refusal staged on 7 would pass whether the check read {@code PAGE_COUNT} or
     * {@code SLOTS_PER_PAGE} wrongly; a refusal staged on 36 would pass against either bound.
     */
    @Test
    void requirePageNamesTheValueAndTheRange() {
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> VaultShape.requirePage(9));
        assertTrue(thrown.getMessage().contains("9"), "the message must name the offending value");
        assertTrue(thrown.getMessage().contains("0..6"), "and the legal range, not just a complaint");
    }

    @Test
    void requireSlotNamesTheValueAndTheRange() {
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> VaultShape.requireSlot(40));
        assertTrue(thrown.getMessage().contains("40"), "the message must name the offending value");
        assertTrue(thrown.getMessage().contains("0..35"), "and the legal range, not just a complaint");
    }

    /**
     * Both requires RETURN their argument so they can wrap an expression inline. A guard that
     * validated and returned void would read identically at the call site and silently drop the
     * value.
     */
    @Test
    void theRequiresReturnWhatTheyWereGiven() {
        assertEquals(5, VaultShape.requirePage(5));
        assertEquals(31, VaultShape.requireSlot(31));
    }
}
