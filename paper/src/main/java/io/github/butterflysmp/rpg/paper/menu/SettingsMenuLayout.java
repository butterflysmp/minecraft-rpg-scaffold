package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Where everything sits on the Nexus settings screen.
 *
 * <p>Six rows, like every other screen in this plugin. One row of nine slot choosers in the body,
 * and chrome in the bottom row.
 *
 * <h2>THE PICKER IS THE INVENTORY, MIRRORED -- AND THE MAPPING IS STILL THE POINT</h2>
 *
 * <b>The screen is choosing an INVENTORY SLOT, so the picker is the inventory.</b> Thirty-six
 * choosers in rows 2-5, laid out exactly as the player's own inventory is:
 *
 * <pre>
 *   menu 9..35   ->  inventory 9..35    IDENTITY -- 27 of 36 cells map to themselves
 *   menu 36..44  ->  inventory 0..8     the HOTBAR, on the bottom row where it belongs
 * </pre>
 *
 * <b>Twenty-seven of the thirty-six need no translation at all</b>, and the nine that do are the
 * hotbar, which every player already reads as the bottom row. Pointing, not arithmetic.
 *
 * <h2>THE ARGUMENT THIS REPLACED, BECAUSE IT WAS SOUND AND ITS SUBJECT MOVED</h2>
 *
 * This javadoc read: <i>"THE NINE CHOOSERS ARE A WHOLE ROW, AND THE MAPPING IS THE POINT -- the
 * screen is choosing a HOTBAR SLOT, and the hotbar is nine cells in a row."</i>
 *
 * <p><b>The shape was justified BY the hotbar's shape</b>, which is why it could not survive the
 * star being allowed anywhere: nine-in-a-row is the right picture of a hotbar and the wrong picture
 * of an inventory. <b>The principle is untouched and is what produced the new shape too</b> -- the
 * picker looks like the thing it is picking from.
 *
 * <p>Rows 2-5 ({@code 9..44}) rather than the row the hub's stats head is in, so the two screens do
 * not look like the same screen with different contents.
 *
 * <h2>CHROME IN THE BOTTOM ROW, per the hub's standing rule</h2>
 *
 * {@code NexusMenuLayout} states it: chrome in the bottom row, features in the body. Close keeps
 * its slot 49 so the button a player has learned does not move between our screens, and <b>Back
 * takes 48, immediately to its left -- the same cell {@code RecipeBrowserLayout.BACK_SLOT} already
 * uses.</b> Every back button in the plugin agrees; see {@link #BACK_SLOT}.
 */
final class SettingsMenuLayout {

    private SettingsMenuLayout() {}

    /** Six rows, like the hub and the crafting menu. */
    static final int SIZE = 54;

    /** The first slot of the bottom row. */
    static final int BOTTOM_ROW_START = 45;

    /**
     * Back to the Nexus. <b>48, immediately left of Close.</b>
     *
     * <h2>THIS IS CONVERGENCE, NOT COINCIDENCE -- {@code RecipeBrowserLayout.BACK_SLOT} IS ALSO 48</h2>
     *
     * It was 45 -- the first slot of the chrome row -- until Ben ruled 48. <b>The recipe browser's
     * back button was already there</b>, so after this ruling BOTH back buttons in the plugin sat in
     * the same cell, and <b>the next screen that needs one has a single answer to copy rather than
     * two to choose between.</b>
     *
     * <p><b>TWO BECAME FOUR, AND THE PREDICTION IS WHY THIS PARAGRAPH EXISTS.</b> "The next screen
     * that needs one has a single answer to copy" was written when there were two; the next two
     * screens copied it. {@code CraftingMenuLayout.BACK_SLOT} left column 8 for this cell, and
     * {@code EnchantMenuLayout.BACK_SLOT} arrived here on a screen that had no back button at all.
     * <b>48/49 is now the rule for every screen, with no exception to name.</b>
     *
     * <p>Stated here because two literals that happen to match are indistinguishable from two that
     * agree on purpose, and the second is worth keeping. {@code EnchantMenuLayoutTest} asserts the
     * agreement across all four rather than the numbers alone, so they cannot drift apart silently.
     *
     * <p><b>A stale cross-reference was removed here.</b> This paragraph used to end by saying
     * {@code CraftingMenuLayout.CLOSE_SLOT} "records that the enchant table deliberately did NOT
     * join it" for Close. That stopped being true when the enchant screen moved Close to 49; all
     * four screens have agreed on Close since, and {@code EnchantMenuLayoutTest} pins it.
     */
    static final int BACK_SLOT = 48;

    /**
     * Close. <b>49, the same slot the crafting menu and the hub use.</b> The one button that must
     * never move between screens; {@code CraftingMenuLayout.CLOSE_SLOT} carries the argument.
     */
    static final int CLOSE_SLOT = 49;

    /** The first chooser: menu slot 9, row 2 column 1 -- the top-left of the mirrored inventory. */
    static final int FIRST_SLOT_CHOOSER = 9;

    /** Thirty-six: the main inventory, hotbar and storage. No armour, no offhand. */
    static final int MAIN_INVENTORY_SIZE = 36;

    /** Nine, because the hotbar is nine -- now only the size of the bottom ROW of the picker. */
    static final int HOTBAR_SIZE = 9;

    /**
     * The thirty-six chooser slots, in menu order.
     *
     * <p><b>A {@code List}, not a {@code Set}</b>, and for {@code CraftingMenuLayout
     * .SUGGESTION_SLOTS}' reason: the order IS the meaning here, and a {@code Set.copyOf} discards
     * iteration order -- that class's javadoc records a version of itself that promised an order it
     * did not have.
     *
     * <p><b>The list index is NOT the inventory slot</b>, and that is the one thing about this
     * layout that has to be read rather than assumed: the list runs in MENU order, and
     * {@link #chooserFor} is what converts. See its mapping table.
     */
    static final List<Integer> SLOT_CHOOSERS = buildChoosers();

    /**
     * Every slot that is plain filler -- everything except the choosers and the two buttons.
     *
     * <p>Built by SET SUBTRACTION, the construction {@code NexusMenuLayout.FILLER_SLOTS} and
     * {@code RecipeBrowserLayout.FOOTER_FILLER} both use: <b>a filler pane painted over a live
     * button is invisible until someone clicks it.</b> Adding a control later is one more removal
     * line, not a remembered skip condition.
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    /**
     * Which INVENTORY slot this menu slot chooses, if it is a chooser at all.
     *
     * <h2>THE MIRROR, AND IT IS THE ONLY ARITHMETIC ON THIS SCREEN</h2>
     *
     * <pre>
     *   menu  9..35   ->  inventory  9..35     IDENTITY
     *   menu 36..44   ->  inventory  0..8      the hotbar, on the bottom row
     * </pre>
     *
     * <b>The identity half is not a coincidence and it is not free either</b> -- it is what you get
     * when a 54-slot chest's rows 2-5 are laid over a player inventory whose storage starts at 9.
     * The predecessor project reached the same mapping, and it was re-derived here rather than
     * copied, because a mapping that is <i>almost</i> the identity is the kind that gets a
     * plausible off-by-nine and passes every test staged in the identity half.
     *
     * <p>{@code OptionalInt} rather than {@code -1}, matching
     * {@code CraftingMenuLayout.suggestionIndexOf} -- a click handler that forgets to test a
     * sentinel silently treats "not a chooser" as slot -1.
     */
    static OptionalInt chooserFor(int menuSlot) {
        if (menuSlot < FIRST_SLOT_CHOOSER || menuSlot >= FIRST_SLOT_CHOOSER + MAIN_INVENTORY_SIZE) {
            return OptionalInt.empty();
        }
        int storageCells = MAIN_INVENTORY_SIZE - HOTBAR_SIZE;          // 27
        int lastStorageMenuSlot = FIRST_SLOT_CHOOSER + storageCells;   // 36, the first hotbar cell
        return menuSlot < lastStorageMenuSlot
                ? OptionalInt.of(menuSlot)                             // 9..35 -> 9..35
                : OptionalInt.of(menuSlot - lastStorageMenuSlot);      // 36..44 -> 0..8
    }

    private static List<Integer> buildChoosers() {
        List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) slots.add(FIRST_SLOT_CHOOSER + i);
        return List.copyOf(slots);
    }

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(SLOT_CHOOSERS);
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }
}
