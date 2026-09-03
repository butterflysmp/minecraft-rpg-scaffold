package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Where everything sits in the recipe browser: a 54-slot menu, five rows of entries and a footer.
 *
 * <h2>WHY ITS OWN MENU AND NOT A PANEL ON THE CRAFTING SCREEN</h2>
 *
 * The crafting screen has 30 free slots, which sounds like plenty. <b>Measured, they run
 * 4/5/3/2/1/1/1/3/10</b> -- the largest contiguous block is 10, and it straddles a row boundary, so
 * it reads as one row of nine plus a dangling cell. A browser needs a rectangle; there is no
 * rectangle there. So it gets its own inventory and the crafting menu navigates to it.
 *
 * <h2>THE PAGE SIZE IS A CONSEQUENCE, NOT A CHOICE</h2>
 *
 * {@link #ENTRIES_PER_PAGE} is rows 0-4 in full: 45. Deriving it from the slot set rather than
 * writing 45 down twice is what keeps {@code PageMath} and the renderer from ever disagreeing --
 * the class that computes which entries belong on a page and the class that paints them read the
 * same number from the same place.
 *
 * <p><b>"Page 1 is the gear page" is NOT an invariant of this layout.</b> That is arithmetic over
 * two numbers that can both move -- how many gear recipes exist, and how many fit on a page -- and
 * nothing would warn anyone when it stopped holding. The invariant is the SORT: all gear ahead of
 * all vanilla. See {@code SuggestionTier}.
 */
final class RecipeBrowserLayout {

    private RecipeBrowserLayout() {}

    /** Six rows, like the crafting menu. The last is chrome. */
    static final int SIZE = 54;

    /** The first slot of the footer row. */
    static final int FOOTER_START = 45;

    /** Previous page. Hidden -- not merely disabled -- on page 1; see {@code RecipeBrowserMenu}. */
    static final int PREV_SLOT = 45;

    /** The "Page N of M" readout. Never clickable. */
    static final int PAGE_SLOT = 49;

    /** Next page. Hidden on the last page. */
    static final int NEXT_SLOT = 53;

    /** Back to the crafting table. */
    static final int BACK_SLOT = 48;

    /**
     * Every slot an entry may occupy: rows 0-4, left to right, top to bottom.
     *
     * <p><b>Insertion-ordered and exposed as a List, not a Set.</b> The nth entry of a page goes in
     * the nth slot of this list, so the order is load-bearing rather than incidental --
     * {@code CraftingMenuLayout.GRID_SLOTS}' javadoc records this repo being bitten once by relying
     * on an iteration order the JDK does not define. A {@code Set} here would re-open exactly that.
     */
    static final List<Integer> ENTRY_SLOTS = buildEntrySlots();

    /** 45. Derived from {@link #ENTRY_SLOTS} so the two can never disagree. */
    static final int ENTRIES_PER_PAGE = ENTRY_SLOTS.size();

    /**
     * The footer cells that are pure filler -- everything on row 5 that is not a control.
     *
     * <p>Built by SET SUBTRACTION from the whole row rather than by a loop with {@code continue}
     * arms: adding a seventh control later means adding one line to the removal list, and a filler
     * pane painted over a live button is invisible until someone clicks it.
     */
    static final Set<Integer> FOOTER_FILLER = buildFooterFiller();

    /** Which entry index a slot holds, or empty if it is not an entry slot. */
    static OptionalInt entryIndexOf(int slot) {
        int index = ENTRY_SLOTS.indexOf(slot);
        return index < 0 ? OptionalInt.empty() : OptionalInt.of(index);
    }

    private static List<Integer> buildEntrySlots() {
        List<Integer> slots = new java.util.ArrayList<>(FOOTER_START);
        for (int slot = 0; slot < FOOTER_START; slot++) slots.add(slot);
        return List.copyOf(slots);
    }

    private static Set<Integer> buildFooterFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = FOOTER_START; slot < SIZE; slot++) slots.add(slot);
        slots.remove(PREV_SLOT);
        slots.remove(PAGE_SLOT);
        slots.remove(NEXT_SLOT);
        slots.remove(BACK_SLOT);
        return Set.copyOf(slots);
    }
}
