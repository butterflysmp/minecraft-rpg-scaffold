package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantCodec;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The enchant table's geometry, in the two-second loop.
 *
 * <p>Every index is asserted as a LITERAL, not by re-deriving the formula the class uses: a test that recomputes the
 * arithmetic it checks cannot fail. The literals are the design decision; the formula is only how it is reached.
 *
 * <p><b>REWRITTEN 2026-11-23 for Ben's layout</b> (the seat's amendment): the info item at the top right (8), the
 * bookshelf left of it (7), the input where the info item was (4), the unique slot in column 2 and the normal slots in
 * columns 4, 6 and 8, four candidates each down rows 2-5 (the rows are the seat's placeholder). In 1-based prose,
 * {@code index = (row-1)*9 + (col-1)}. The Back and Close rows are kept as they were.
 */
class EnchantMenuLayoutTest {

    /** Every candidate cell, as the seat's amendment lists them. */
    @Test
    void theFourColumnsSitAtTheDesignedSlots() {
        int[][] expected = {
                {10, 19, 28, 37},   // slot 0, the UNIQUE slot: column 2
                {12, 21, 30, 39},   // slot 1: column 4
                {14, 23, 32, 41},   // slot 2: column 6
                {16, 25, 34, 43},   // slot 3: column 8
        };
        for (int slot = 0; slot < 4; slot++) {
            for (int c = 0; c < 4; c++) {
                assertEquals(expected[slot][c], EnchantMenuLayout.rawSlotFor(slot, c), "slot " + slot + " candidate " + c);
            }
        }
        // The lowest rank stays clear of the chrome row (45-53).
        assertTrue(EnchantMenuLayout.rawSlotFor(3, 3) < 45);
    }

    @Test
    void theFixtureSlotsAreWhereTheLayoutSaysTheyAre() {
        assertEquals(8, EnchantMenuLayout.INFO_SLOT, "Ben: the info item in the top right corner (row 1, column 9)");
        assertEquals(7, EnchantMenuLayout.BOOKSHELF_SLOT, "Ben: the bookshelf to the left of it (row 1, column 8)");
        assertEquals(4, EnchantMenuLayout.INPUT_SLOT, "Ben: the input where the info item was (row 1, column 5)");
        assertEquals(48, EnchantMenuLayout.BACK_SLOT, "Back does not move");
        assertEquals(49, EnchantMenuLayout.CLOSE_SLOT, "Close does not move");
        assertEquals(54, EnchantMenuLayout.SIZE);
        assertEquals(4, EnchantMenuLayout.SLOTS);
        assertEquals(4, EnchantMenuLayout.CANDIDATES);
    }

    /**
     * W3 (2026-11-23): the two columns that can be empty each have their marker, with its own words; a normal column has
     * none. The wordings are SEAT PLACEHOLDERS (the unique one is #249's, unchanged).
     */
    @Test
    void theWeaponAndUniqueColumnsHaveEmptyMarkersAndTheNormalOnesDoNot() {
        assertEquals(Optional.of(new EnchantMenuLayout.EmptyMarker("No weapon enchant", "Nothing is tied to this weapon yet.")),
                EnchantMenuLayout.emptyMarkerFor(0));
        assertEquals(Optional.of(new EnchantMenuLayout.EmptyMarker("No unique enchant",
                "Nothing unique is tied to this item's element yet.")), EnchantMenuLayout.emptyMarkerFor(1));
        assertEquals(Optional.empty(), EnchantMenuLayout.emptyMarkerFor(2));
        assertEquals(Optional.empty(), EnchantMenuLayout.emptyMarkerFor(3));
        // Each marker sits in its column's first cell, which is where the seat's W3 says the cells did not move.
        assertEquals(10, EnchantMenuLayout.rawSlotFor(0, 0), "column 2, the weapon slot's first cell");
        assertEquals(12, EnchantMenuLayout.rawSlotFor(1, 0), "column 4, the unique slot's first cell");
    }

    /** No two things share an index:sixteen cells and five fixtures, twenty-one distinct indices (the "Unique" label at 1 was removed, W3). */
    @Test
    void noTwoThingsShareAnIndex() {
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < EnchantMenuLayout.SLOTS; slot++) {
            for (int c = 0; c < EnchantMenuLayout.CANDIDATES; c++) {
                assertTrue(seen.add(EnchantMenuLayout.rawSlotFor(slot, c)), "cell (" + slot + "," + c + ") collides");
            }
        }
        for (int fixture : List.of(EnchantMenuLayout.INFO_SLOT, EnchantMenuLayout.BOOKSHELF_SLOT, EnchantMenuLayout.INPUT_SLOT,
                EnchantMenuLayout.BACK_SLOT, EnchantMenuLayout.CLOSE_SLOT)) {
            assertTrue(seen.add(fixture), "fixture " + fixture + " collides");
        }
        assertEquals(21, seen.size());
    }

    @Test
    void everyCellRoundTripsThroughItsRawSlot() {
        for (int slot = 0; slot < EnchantMenuLayout.SLOTS; slot++) {
            for (int c = 0; c < EnchantMenuLayout.CANDIDATES; c++) {
                assertEquals(Optional.of(new EnchantMenuLayout.Cell(slot, c)),
                        EnchantMenuLayout.cellAt(EnchantMenuLayout.rawSlotFor(slot, c)));
            }
        }
    }

    /** The other direction, over all 54 indices: exactly the sixteen cells are cells, and each maps back to itself. */
    @Test
    void everyRawSlotRoundTripsBackToItself_theOtherDirection() {
        int cells = 0;
        for (int raw = 0; raw < EnchantMenuLayout.SIZE; raw++) {
            Optional<EnchantMenuLayout.Cell> cell = EnchantMenuLayout.cellAt(raw);
            if (cell.isPresent()) {
                cells++;
                assertEquals(raw, EnchantMenuLayout.rawSlotFor(cell.get().slot(), cell.get().candidate()));
            }
        }
        assertEquals(16, cells, "exactly sixteen indices address a candidate cell");
    }

    @Test
    void chromeFillerAndGapsAddressNoCell() {
        for (int fixture : List.of(EnchantMenuLayout.INFO_SLOT, EnchantMenuLayout.BOOKSHELF_SLOT, EnchantMenuLayout.INPUT_SLOT,
                EnchantMenuLayout.BACK_SLOT, EnchantMenuLayout.CLOSE_SLOT)) {
            assertTrue(EnchantMenuLayout.cellAt(fixture).isEmpty(), "fixture " + fixture);
        }
        assertTrue(EnchantMenuLayout.cellAt(11).isEmpty(), "the gap between columns 2 and 4");
        assertTrue(EnchantMenuLayout.cellAt(9).isEmpty(), "column 1 of a candidate row");
        assertTrue(EnchantMenuLayout.cellAt(17).isEmpty(), "column 9 of a candidate row");
        assertTrue(EnchantMenuLayout.cellAt(46).isEmpty(), "row 6 is below the band");
        assertTrue(EnchantMenuLayout.cellAt(-1).isEmpty());
        assertTrue(EnchantMenuLayout.cellAt(EnchantMenuLayout.SIZE).isEmpty());
    }

    @Test
    void aCellOutsideTheBoundIsRefusedRatherThanWrappingOntoAnotherRow() {
        assertThrows(IllegalArgumentException.class, () -> EnchantMenuLayout.rawSlotFor(4, 0));
        assertThrows(IllegalArgumentException.class, () -> EnchantMenuLayout.rawSlotFor(0, 4));
        assertThrows(IllegalArgumentException.class, () -> EnchantMenuLayout.rawSlotFor(-1, 0));
    }

    @Test
    void backIsTheSAMESlotOnEVERYScreenThatHasOne() {
        assertEquals(SettingsMenuLayout.BACK_SLOT, EnchantMenuLayout.BACK_SLOT, "the enchant table agrees with settings");
        assertEquals(RecipeBrowserLayout.BACK_SLOT, EnchantMenuLayout.BACK_SLOT, "and with the recipe browser");
        assertEquals(CraftingMenuLayout.BACK_SLOT, EnchantMenuLayout.BACK_SLOT, "and with crafting");
        assertEquals(EnchantMenuLayout.CLOSE_SLOT - 1, EnchantMenuLayout.BACK_SLOT);
    }

    @Test
    void closeIsTheSAMESlotOnEVERYScreen_andThatIsTheWholeReasonItMoved() {
        assertEquals(CraftingMenuLayout.CLOSE_SLOT, EnchantMenuLayout.CLOSE_SLOT, "the enchant table agrees with crafting");
        assertEquals(NexusMenuLayout.CLOSE_SLOT, EnchantMenuLayout.CLOSE_SLOT, "and with the hub");
        assertEquals(SettingsMenuLayout.CLOSE_SLOT, EnchantMenuLayout.CLOSE_SLOT, "and with settings");
    }

    /** Every candidate of a 4 x 4 state has a cell, so it FITS. */
    @Test
    void aFourByFourStateFits() {
        EnchantState full = full();
        assertEquals(4, full.slots().size());
        assertTrue(EnchantMenuLayout.overflow(full).isEmpty(), "exactly 4x4 must FIT");
        for (int slot = 0; slot < 4; slot++) {
            for (int c = 0; c < full.slots().get(slot).candidates().size(); c++) {
                EnchantMenuLayout.rawSlotFor(slot, c);   // throws if any candidate has no cell
            }
        }
        // Mutation: ">" to ">=" in overflow -> a full, legal item is refused at the door -> reddens.
    }

    @Test
    void aFifthSlotIsReportedRatherThanTruncated() {
        Optional<String> problem = EnchantMenuLayout.overflow(full().addCandidate(4, "sharpness"));
        assertTrue(problem.isPresent(), "a 5th slot must be REFUSED, never silently hidden");
        assertTrue(problem.get().contains("5"), "the refusal must name the count: " + problem.get());
    }

    @Test
    void aFifthCandidateInOneSlotIsReportedAndNamesTheSlot() {
        EnchantState tooMany = EnchantState.empty()
                .addCandidate(0, "sharpness").addCandidate(0, "unbreaking")
                .addCandidate(0, "power").addCandidate(0, "attunement").addCandidate(0, "thorns");
        Optional<String> problem = EnchantMenuLayout.overflow(tooMany);
        assertTrue(problem.isPresent());
        assertTrue(problem.get().contains("1"), "name the slot at fault: " + problem.get());
        assertTrue(problem.get().contains("5"), "name the count: " + problem.get());
    }

    @Test
    void whatTheRollProducesIsWhatTheTableCanShow() {
        assertEquals(EnchantMenuLayout.SLOTS, EnchantRoll.SLOTS, "the roll makes more slots than the table shows");
        assertEquals(EnchantMenuLayout.CANDIDATES, EnchantRoll.MAX_CANDIDATES,
                "the roll can fill a slot fuller than the table shows");
    }

    /**
     * V4: AN ITEM ROLLED UNDER THE OLD THREE-SLOT SHAPE fits the new table: three slots of up to three candidates is
     * inside four of four, so the table accepts it rather than refusing it at the door, and every candidate it carries
     * has a cell (slot 0's NORMAL candidates show in the unique column; slot 3's column stays filler).
     */
    @Test
    void anOldThreeSlotItemFitsAndEveryCandidateHasACell() {
        EnchantState old = EnchantCodec.decode("v1;sharpness=2,unbreaking=0,power=0:0;unbreaking=1:-1;sharpness=0,unbreaking=3:1");
        assertEquals(3, old.slots().size());
        assertTrue(EnchantMenuLayout.overflow(old).isEmpty());
        assertEquals(10, EnchantMenuLayout.rawSlotFor(0, 0), "its slot 0 renders in the unique column");
        for (int slot = 0; slot < old.slots().size(); slot++) {
            for (int c = 0; c < old.slots().get(slot).candidates().size(); c++) EnchantMenuLayout.rawSlotFor(slot, c);
        }
    }

    /** Exactly what the table can show: four slots, four candidates each. */
    private static EnchantState full() {
        EnchantState s = EnchantState.empty();
        for (int slot = 0; slot < 4; slot++) {
            for (String id : List.of("sharpness", "unbreaking", "power", "attunement")) s = s.addCandidate(slot, id);
        }
        return s;
    }

    /**
     * THE MARKER BY THE GEAR'S KIND (the seat, 2026-11-25): an ARMOR piece's empty slot 0 is piece-specific (E1), so it
     * reads "No piece enchant" (SEAT PLACEHOLDER); every other kind keeps the weapon's words exactly, and the unique
     * column's words are the same for every kind.
     */
    @Test
    void armorsEmptySlotZeroReadsPieceAndEveryOtherKindKeepsTheWeaponsWords() {
        assertEquals(Optional.of(new EnchantMenuLayout.EmptyMarker("No piece enchant", "Nothing is tied to this armor slot yet.")),
                EnchantMenuLayout.emptyMarkerFor(0, io.github.butterflysmp.rpg.core.weapon.GearClass.ARMOR));
        for (io.github.butterflysmp.rpg.core.weapon.GearClass kind : io.github.butterflysmp.rpg.core.weapon.GearClass.values()) {
            if (kind == io.github.butterflysmp.rpg.core.weapon.GearClass.ARMOR) continue;
            assertEquals(EnchantMenuLayout.emptyMarkerFor(0), EnchantMenuLayout.emptyMarkerFor(0, kind), kind.name());
        }
        assertEquals(EnchantMenuLayout.emptyMarkerFor(1),
                EnchantMenuLayout.emptyMarkerFor(1, io.github.butterflysmp.rpg.core.weapon.GearClass.ARMOR), "unique: same words");
        // Mutation: drop the armor arm -> "No weapon enchant" on a helmet -> reddens.
    }
}
