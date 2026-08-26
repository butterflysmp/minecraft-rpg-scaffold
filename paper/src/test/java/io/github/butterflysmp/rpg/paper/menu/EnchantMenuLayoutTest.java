package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The enchant table's geometry, in the two-second loop.
 *
 * <p>The nine candidate slots are asserted as LITERALS -- 20, 29, 38 and so on -- and not by
 * re-deriving the formula the class uses. A test that recomputes the arithmetic it is checking
 * cannot fail: change the stride in both places and it stays green while every column moves. The
 * literals are the design decision; the formula is only how it is reached.
 *
 * <p>This is also the whole reason the layout is a separate class from the menu. An
 * {@code ItemStack} cannot be built without a running server, so if these numbers lived in
 * {@code EnchantMenu} the only way to check them would be to boot Paper and count squares by eye.
 */
class EnchantMenuLayoutTest {

    @Test
    void theThreeColumnsSitAtTheDesignedSlots() {
        // Columns 2, 4 and 6; rows 2, 3 and 4. Read down a column for one enchant slot's choices.
        assertEquals(20, EnchantMenuLayout.rawSlotFor(0, 0));
        assertEquals(29, EnchantMenuLayout.rawSlotFor(0, 1));
        assertEquals(38, EnchantMenuLayout.rawSlotFor(0, 2));

        assertEquals(22, EnchantMenuLayout.rawSlotFor(1, 0));
        assertEquals(31, EnchantMenuLayout.rawSlotFor(1, 1));
        assertEquals(40, EnchantMenuLayout.rawSlotFor(1, 2));

        assertEquals(24, EnchantMenuLayout.rawSlotFor(2, 0));
        assertEquals(33, EnchantMenuLayout.rawSlotFor(2, 1));
        assertEquals(42, EnchantMenuLayout.rawSlotFor(2, 2));
        // Mutation: COLUMN_STRIDE 2 -> 1 collapses the columns onto 2/3/4 -> reddens.
        // Mutation: FIRST_CANDIDATE_ROW 2 -> 1 lifts every cell a row -> reddens.
    }

    @Test
    void everyCellRoundTripsThroughItsRawSlot() {
        // cellAt is the exact inverse of rawSlotFor, which is what lets a click arrive as an index
        // and leave as a cell with no lookup table anywhere.
        for (int slot = 0; slot < EnchantMenuLayout.SLOTS; slot++) {
            for (int candidate = 0; candidate < EnchantMenuLayout.CANDIDATES; candidate++) {
                int raw = EnchantMenuLayout.rawSlotFor(slot, candidate);
                assertEquals(Optional.of(new EnchantMenuLayout.Cell(slot, candidate)),
                        EnchantMenuLayout.cellAt(raw),
                        "raw slot " + raw + " must address (" + slot + ", " + candidate + ")");
            }
        }
        // Mutation: change either direction alone -> reddens. Changing BOTH consistently is a
        // relayout, and theThreeColumnsSitAtTheDesignedSlots is what catches that.
    }

    @Test
    void chromeAndFillerAddressNoCell() {
        // A click on the input slot must never be read as a candidate: they are different actions
        // on different things, and confusing them would enchant on pickup.
        assertTrue(EnchantMenuLayout.cellAt(EnchantMenuLayout.CLOSE_SLOT).isEmpty());
        assertTrue(EnchantMenuLayout.cellAt(EnchantMenuLayout.INPUT_SLOT).isEmpty());
        assertTrue(EnchantMenuLayout.cellAt(EnchantMenuLayout.BOOKSHELF_SLOT).isEmpty());
        assertTrue(EnchantMenuLayout.cellAt(EnchantMenuLayout.INFO_SLOT).isEmpty());

        // The gaps BETWEEN the columns. 21 sits between slot 0 and slot 1 on a candidate row, so a
        // missing stride check would round it into a real cell and make filler clickable.
        assertTrue(EnchantMenuLayout.cellAt(21).isEmpty(), "the gap between columns is not a cell");
        assertTrue(EnchantMenuLayout.cellAt(23).isEmpty());
        // Mutation: drop the "offset % COLUMN_STRIDE != 0" arm -> 21 becomes a cell -> reddens.

        // The candidate rows do not run to the edges either.
        assertTrue(EnchantMenuLayout.cellAt(18).isEmpty(), "column 0 of a candidate row is filler");
        assertTrue(EnchantMenuLayout.cellAt(26).isEmpty(), "column 8 of a candidate row is filler");
        // Mutation: drop the "slot >= SLOTS" arm -> 26 becomes slot 3 -> reddens.
    }

    @Test
    void rowsOutsideTheCandidateBandAddressNoCell() {
        // Row 1 and row 5 are entirely filler. Without the row guard, row 5's column 2 (slot 47)
        // would read as candidate 3 of slot 0 -- a cell that renders nothing and clicks something.
        assertTrue(EnchantMenuLayout.cellAt(11).isEmpty(), "row 1 is above the candidate band");
        assertTrue(EnchantMenuLayout.cellAt(47).isEmpty(), "row 5 is below the candidate band");
        assertTrue(EnchantMenuLayout.cellAt(2).isEmpty(), "row 0 is chrome");
    }

    @Test
    void anIndexOutsideTheChestIsNotACell() {
        // The router hands cellAt a RAW slot, and a raw slot spans BOTH inventories -- 54 and up is
        // the player's own inventory. Reading one of those as a candidate would let a click in your
        // own hotbar enchant the weapon in the menu.
        assertTrue(EnchantMenuLayout.cellAt(EnchantMenuLayout.SIZE).isEmpty());
        assertTrue(EnchantMenuLayout.cellAt(70).isEmpty());
        assertTrue(EnchantMenuLayout.cellAt(-1).isEmpty());
        // Mutation: drop the bounds guard -> raw 74 reads as a cell or throws -> reddens.
    }

    @Test
    void theNineCellsAreDistinctAndNoneCollidesWithAFixture() {
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < EnchantMenuLayout.SLOTS; slot++) {
            for (int candidate = 0; candidate < EnchantMenuLayout.CANDIDATES; candidate++) {
                assertTrue(seen.add(EnchantMenuLayout.rawSlotFor(slot, candidate)),
                        "two cells share a chest slot");
            }
        }
        assertEquals(9, seen.size());
        // A candidate painted over the input slot would be painted over the player's WEAPON.
        assertFalse(seen.contains(EnchantMenuLayout.INPUT_SLOT));
        assertFalse(seen.contains(EnchantMenuLayout.CLOSE_SLOT));
        assertFalse(seen.contains(EnchantMenuLayout.BOOKSHELF_SLOT));
        assertFalse(seen.contains(EnchantMenuLayout.INFO_SLOT));
        // Mutation: move INPUT_SLOT to 20 -> reddens.
    }

    @Test
    void aCellOutsideTheBoundIsRefusedRatherThanWrappingOntoAnotherRow() {
        // Unchecked, rawSlotFor(3, 0) returns 26 -- a real, paintable chest slot in the filler at
        // the end of a candidate row. Returning a plausible wrong answer is worse than throwing.
        assertThrows(IllegalArgumentException.class, () -> EnchantMenuLayout.rawSlotFor(3, 0));
        assertThrows(IllegalArgumentException.class, () -> EnchantMenuLayout.rawSlotFor(0, 3));
        assertThrows(IllegalArgumentException.class, () -> EnchantMenuLayout.rawSlotFor(-1, 0));
    }

    @Test
    void aStateThatFitsReportsNoOverflow() {
        assertTrue(EnchantMenuLayout.overflow(EnchantState.empty()).isEmpty());
        assertTrue(EnchantMenuLayout.overflow(full()).isEmpty(), "exactly 3x3 must FIT");
        // Mutation: ">" to ">=" -> a full, legal weapon is refused at the door -> reddens.
    }

    @Test
    void aFourthSlotIsReportedRatherThanTruncated() {
        EnchantState tooManySlots = full().addCandidate(3, "sharpness");

        Optional<String> problem = EnchantMenuLayout.overflow(tooManySlots);

        assertTrue(problem.isPresent(), "a 4th slot must be REFUSED, never silently hidden");
        assertTrue(problem.get().contains("4"), "the refusal must name the count: " + problem.get());
        // Mutation: return Optional.empty() always -> the 4th slot renders nowhere, still works,
        // and the player cannot see or reach it -> reddens.
    }

    @Test
    void aFourthCandidateInOneSlotIsReportedAndNamesTheSlot() {
        // The half a slots().size() check alone would miss entirely.
        EnchantState tooManyCandidates = EnchantState.empty()
                .addCandidate(0, "sharpness").addCandidate(0, "unbreaking")
                .addCandidate(0, "power").addCandidate(0, "attunement");

        Optional<String> problem = EnchantMenuLayout.overflow(tooManyCandidates);

        assertTrue(problem.isPresent());
        assertTrue(problem.get().contains("1"), "name the slot at fault: " + problem.get());
        assertTrue(problem.get().contains("4"), "name the count: " + problem.get());
        // Mutation: check only state.slots().size() -> a 4-candidate slot sails through -> reddens.
    }

    /** Exactly what the table can show: three slots, three candidates each. */
    private static EnchantState full() {
        return EnchantState.empty()
                .addCandidate(0, "sharpness").addCandidate(0, "unbreaking").addCandidate(0, "power")
                .addCandidate(1, "sharpness").addCandidate(1, "unbreaking").addCandidate(1, "power")
                .addCandidate(2, "sharpness").addCandidate(2, "unbreaking").addCandidate(2, "power");
    }
}
