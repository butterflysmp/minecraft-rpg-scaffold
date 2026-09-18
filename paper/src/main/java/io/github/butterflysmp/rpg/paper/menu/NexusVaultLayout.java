package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.vault.VaultShape;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Where everything sits on the vault screen: a page selector on top, storage in the middle, chrome
 * at the bottom.
 *
 * <pre>
 *   row 1   indices  0-8    0 left arrow, 1-7 the seven PAGE BUTTONS, 8 right arrow
 *   row 2   indices  9-17   }
 *   row 3   indices 18-26   }  THE STORAGE. 36 cells, one vault page.
 *   row 4   indices 27-35   }
 *   row 5   indices 36-44   }
 *   row 6   indices 45-53   chrome. Back 48, Close 49.
 * </pre>
 *
 * <h2>*** SEVEN PAGES IN SEVEN BUTTON CELLS IS ASSERTED, NOT NOTICED ***</h2>
 *
 * {@code VaultShape}'s own javadoc fixes this row and names what goes wrong without the assertion:
 * raise {@code PAGE_COUNT} to eight and the eighth page <b>exists in storage, holds the player's
 * items, and has no way to be selected.</b> Not a compile error and not an exception -- an
 * unreachable page, which is indistinguishable from an empty one.
 *
 * <p>So {@link #PAGE_BUTTON_SLOTS} is <b>derived from {@code PAGE_COUNT}</b> rather than written
 * out, and {@link #assertFits} refuses at class load if the row cannot hold them.
 *
 * <h2>BACK 48 AND CLOSE 49, WHICH IS WHAT EVERY OTHER SCREEN DOES</h2>
 *
 * {@code CraftingMenuLayout}, {@code SettingsMenuLayout} and {@code RecipeBrowserLayout} all put
 * Back at 48 and Close at 49. A player has learned where those are; the vault does not get to
 * disagree.
 *
 * <p><b>Back is painted ONLY when there is somewhere to go back to</b> -- the hub. Opened from the
 * ender chest there is no hub in the story, so 48 is filler. That is
 * {@code CraftingMenuLayout.BACK_SLOT}'s own rule, arriving here.
 */
final class NexusVaultLayout {

    private NexusVaultLayout() {}

    /** Six rows, like every other screen in this plugin. */
    static final int SIZE = 54;

    /** The left arrow: one page down. Index 0, the far left of the selector row. */
    static final int PREV_SLOT = 0;

    /** The right arrow: one page up. Index 8, the far right of the selector row. */
    static final int NEXT_SLOT = 8;

    /** The first page button. The arrows bracket the run, so the buttons start at 1. */
    static final int FIRST_PAGE_SLOT = 1;

    /** Back to the hub. 48, as on every other screen that has one. */
    static final int BACK_SLOT = 48;

    /** Close. 49, as on every other screen. */
    static final int CLOSE_SLOT = 49;

    /** The first storage cell: the start of row 2. */
    static final int FIRST_STORAGE_SLOT = 9;

    /**
     * The page buttons, in page order. <b>Derived from {@link VaultShape#PAGE_COUNT}.</b>
     *
     * <p>Index into this by 0-based page; the <b>label</b> a player reads is {@code page + 1}, and
     * that conversion happens on the icon rather than here. Same edge-conversion the command does
     * in {@code RpgCommand}'s {@code vaultCell}.
     */
    static final int[] PAGE_BUTTON_SLOTS = buildPageButtons();

    /**
     * The 36 cells that hold the player's items, ascending.
     *
     * <p><b>Derived from {@link VaultShape#SLOTS_PER_PAGE}</b>, not written out, so the screen and
     * the file cannot disagree about how big a page is. A hand-written run of 36 literals is a
     * second source of truth for a number that already has one.
     */
    static final Set<Integer> STORAGE_SLOTS = buildStorage();

    /**
     * Every cell the screen paints individually -- the arrows, the page buttons and the two chrome
     * buttons. <b>Storage is NOT in here</b>: those cells hold the player's items, and painting
     * anything into them is how a vault eats a stack.
     *
     * <p>Used twice, and it is one list because {@code NexusMenuLayout} paid for two that
     * disagreed: slot 33 was subtracted from the filler and painted by nothing, leaving an
     * invisible clickable hole that reached a screenshot.
     */
    static final Set<Integer> PAINTED_SLOTS = buildPainted();

    /**
     * Filler: everything that is neither storage nor painted.
     *
     * <p><b>Set subtraction, for {@code NexusMenuLayout.FILLER_SLOTS}' reason</b> -- a filler pane
     * painted over a live button is invisible until someone clicks it, and painting over
     * {@link #CLOSE_SLOT} makes the screen unclosable except with Esc.
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    /**
     * *** THE ROW HOLDS THE BUTTONS, CHECKED AT CLASS LOAD. ***
     *
     * <p>Called from the static initialiser below rather than left to a test, for
     * {@code VaultPageGate}'s reason: a test can be deleted and a class-load check cannot. What it
     * refuses is the state in which a page exists and cannot be selected.
     */
    private static void assertFits() {
        int lastButton = FIRST_PAGE_SLOT + VaultShape.PAGE_COUNT - 1;
        if (lastButton >= NEXT_SLOT) {
            throw new IllegalStateException(
                    "the vault selector row holds " + (NEXT_SLOT - FIRST_PAGE_SLOT)
                            + " page buttons between the arrows, but VaultShape.PAGE_COUNT is "
                            + VaultShape.PAGE_COUNT + "; a page with no button is an unreachable"
                            + " page, which is indistinguishable from an empty one");
        }
        if (FIRST_STORAGE_SLOT + VaultShape.SLOTS_PER_PAGE > SIZE) {
            throw new IllegalStateException(
                    "a page of " + VaultShape.SLOTS_PER_PAGE + " cells starting at "
                            + FIRST_STORAGE_SLOT + " does not fit in a " + SIZE + "-slot screen");
        }
    }

    static {
        assertFits();
    }

    private static int[] buildPageButtons() {
        int[] slots = new int[VaultShape.PAGE_COUNT];
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            slots[page] = FIRST_PAGE_SLOT + page;
        }
        return slots;
    }

    private static Set<Integer> buildStorage() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int offset = 0; offset < VaultShape.SLOTS_PER_PAGE; offset++) {
            slots.add(FIRST_STORAGE_SLOT + offset);
        }
        return Set.copyOf(slots);
    }

    private static Set<Integer> buildPainted() {
        Set<Integer> slots = new LinkedHashSet<>();
        slots.add(PREV_SLOT);
        slots.add(NEXT_SLOT);
        for (int slot : PAGE_BUTTON_SLOTS) slots.add(slot);
        slots.add(BACK_SLOT);
        slots.add(CLOSE_SLOT);
        return Set.copyOf(slots);
    }

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        // THE SAME TWO SETS the screen paints and fills, subtracted rather than restated. A third
        // hand-maintained list is what NexusMenuLayout's hole at 33 was.
        slots.removeAll(PAINTED_SLOTS);
        slots.removeAll(STORAGE_SLOTS);
        return Set.copyOf(slots);
    }

    /** Which page a page button belongs to, or {@code -1} if this slot is not a page button. */
    static int pageAt(int slot) {
        for (int page = 0; page < PAGE_BUTTON_SLOTS.length; page++) {
            if (PAGE_BUTTON_SLOTS[page] == slot) return page;
        }
        return -1;
    }

    /**
     * Which vault slot a screen cell holds, or {@code -1} if this cell is not storage.
     *
     * <p><b>The one place the screen-index-to-vault-slot conversion happens.</b> Storage cell 9 is
     * vault slot 0. Doing this arithmetic at each call site is how a screen comes to write row 2
     * into row 3.
     */
    static int storageSlotAt(int slot) {
        if (!STORAGE_SLOTS.contains(slot)) return -1;
        return slot - FIRST_STORAGE_SLOT;
    }

    /** The screen cell that shows a given vault slot. The inverse of {@link #storageSlotAt}. */
    static int screenSlotFor(int vaultSlot) {
        return FIRST_STORAGE_SLOT + VaultShape.requireSlot(vaultSlot);
    }
}
