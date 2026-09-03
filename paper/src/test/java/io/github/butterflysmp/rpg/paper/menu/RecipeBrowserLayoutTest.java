package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.PageMath;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The browser's slot map. Each test names the mutation it forces red.
 *
 * <p>The load-bearing one is {@link #noSlotIsBothAnEntryAndAControl}: a control sharing a slot with
 * an entry is invisible in every screenshot and only shows up when a player clicks the last row and
 * crafts something they were trying to page past.
 */
class RecipeBrowserLayoutTest {

    @Test
    void theMenuIsSixRows() {
        assertEquals(54, RecipeBrowserLayout.SIZE);
        assertEquals(45, RecipeBrowserLayout.FOOTER_START, "five rows of entries, then the footer");
        assertEquals(0, RecipeBrowserLayout.SIZE % 9, "an inventory is always a whole number of rows");
    }

    @Test
    void entrySlotsAreRowsZeroToFourInReadingOrder() {
        List<Integer> slots = RecipeBrowserLayout.ENTRY_SLOTS;
        assertEquals(45, slots.size());
        assertEquals(0, slots.get(0), "the first entry is top-left");
        assertEquals(44, slots.get(44), "the last entry is the end of row 4");
        for (int i = 0; i < slots.size(); i++) {
            assertEquals(i, slots.get(i), "entry " + i + " must sit in slot " + i);
        }
        // Mutation: build the list backwards, or start at 9 -> reddens. Order is load-bearing: the
        // nth entry of a page is painted into the nth slot of this list.
    }

    @Test
    void entriesPerPageIsDERIVEDFromTheSlotListNotWrittenTwice() {
        assertEquals(RecipeBrowserLayout.ENTRY_SLOTS.size(), RecipeBrowserLayout.ENTRIES_PER_PAGE);
        assertEquals(45, RecipeBrowserLayout.ENTRIES_PER_PAGE);
        // Mutation: `static final int ENTRIES_PER_PAGE = 44;` -> reddens. This is the assertion that
        // stops PageMath and the renderer from ever disagreeing about how big a page is -- a
        // disagreement that shows up as entries silently missing from the end of every page.
    }

    @Test
    void noSlotIsBothAnEntryAndAControl() {
        // THE test. A control sharing a slot with an entry is invisible until a player clicks it.
        Set<Integer> controls = Set.of(
                RecipeBrowserLayout.PREV_SLOT, RecipeBrowserLayout.PAGE_SLOT,
                RecipeBrowserLayout.NEXT_SLOT, RecipeBrowserLayout.BACK_SLOT);
        for (int control : controls) {
            assertTrue(control >= RecipeBrowserLayout.FOOTER_START,
                    "control at slot " + control + " is inside the entry area");
            assertFalse(RecipeBrowserLayout.ENTRY_SLOTS.contains(control),
                    "slot " + control + " is both a control and an entry slot");
            assertTrue(RecipeBrowserLayout.entryIndexOf(control).isEmpty(),
                    "slot " + control + " must not resolve to an entry index");
        }
        // Mutation: move PREV_SLOT to 44 -> every assertion in this test reddens.
    }

    @Test
    void theFourControlsAreDISTINCT() {
        Set<Integer> controls = new HashSet<>(List.of(
                RecipeBrowserLayout.PREV_SLOT, RecipeBrowserLayout.PAGE_SLOT,
                RecipeBrowserLayout.NEXT_SLOT, RecipeBrowserLayout.BACK_SLOT));
        assertEquals(4, controls.size(), "two controls share a slot; one of them is unreachable");
        // Mutation: set BACK_SLOT = PAGE_SLOT -> reddens. Without this, one button silently paints
        // over another and the loser is simply never clickable.
    }

    @Test
    void footerFillerIsTheRestOfTheRowAndNothingElse() {
        Set<Integer> filler = RecipeBrowserLayout.FOOTER_FILLER;
        assertEquals(5, filler.size(), "nine footer cells minus four controls");

        for (int slot : filler) {
            assertTrue(slot >= 45 && slot < 54, "filler slot " + slot + " is outside the footer row");
        }
        assertFalse(filler.contains(RecipeBrowserLayout.PREV_SLOT));
        assertFalse(filler.contains(RecipeBrowserLayout.PAGE_SLOT));
        assertFalse(filler.contains(RecipeBrowserLayout.NEXT_SLOT));
        assertFalse(filler.contains(RecipeBrowserLayout.BACK_SLOT));
        assertEquals(Set.of(46, 47, 50, 51, 52), filler, "the whole row, minus the four controls");
        // Mutation: drop one `slots.remove(..)` -> the size and the corresponding assertFalse redden,
        // and in game a filler pane would paint over a live button.
    }

    @Test
    void everyFooterSlotIsEitherAControlOrFiller() {
        // The completeness half: set subtraction is only correct if the two halves cover the row.
        // Without this, a control could be removed from the filler set AND never painted, leaving a
        // permanently blank cell that looks like a rendering bug.
        for (int slot = RecipeBrowserLayout.FOOTER_START; slot < RecipeBrowserLayout.SIZE; slot++) {
            boolean control = slot == RecipeBrowserLayout.PREV_SLOT
                    || slot == RecipeBrowserLayout.PAGE_SLOT
                    || slot == RecipeBrowserLayout.NEXT_SLOT
                    || slot == RecipeBrowserLayout.BACK_SLOT;
            assertTrue(control ^ RecipeBrowserLayout.FOOTER_FILLER.contains(slot),
                    "footer slot " + slot + " is neither a control nor filler, or is both");
        }
    }

    @Test
    void entryIndexOfIsTheINVERSEOfTheSlotList() {
        for (int i = 0; i < RecipeBrowserLayout.ENTRIES_PER_PAGE; i++) {
            int slot = RecipeBrowserLayout.ENTRY_SLOTS.get(i);
            assertEquals(i, RecipeBrowserLayout.entryIndexOf(slot).orElseThrow(),
                    "slot " + slot + " must map back to entry " + i);
        }
        assertTrue(RecipeBrowserLayout.entryIndexOf(-1).isEmpty());
        assertTrue(RecipeBrowserLayout.entryIndexOf(54).isEmpty());
        assertTrue(RecipeBrowserLayout.entryIndexOf(RecipeBrowserLayout.FOOTER_START).isEmpty());
        // Mutation: `return OptionalInt.of(slot)` instead of the index -> reddens for every slot
        // whose index differs from its number, which today is none of them -- so the mutation that
        // ACTUALLY reddens this is reordering ENTRY_SLOTS. Said plainly because a test that cannot
        // fail is worth nothing however green, and identity mappings are where that hides.
    }

    @Test
    void theLayoutAndPageMathAgreeAboutWhatFitsOnAPage() {
        // The two classes that must not disagree, checked against each other rather than against a
        // number typed twice. 100 entries at 45 a page is 3 pages: 45, 45, 10.
        int size = RecipeBrowserLayout.ENTRIES_PER_PAGE;
        assertEquals(3, PageMath.pageCount(100, size));
        assertEquals(size, PageMath.sizeOfPage(0, size, 100));
        assertEquals(10, PageMath.sizeOfPage(2, size, 100));
        assertTrue(PageMath.sizeOfPage(0, size, 100) <= RecipeBrowserLayout.ENTRY_SLOTS.size(),
                "a page can never hold more entries than there are slots to paint them in");
    }
}
