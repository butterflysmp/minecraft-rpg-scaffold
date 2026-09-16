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
 * <h2>THE NINE CHOOSERS ARE A WHOLE ROW, AND THE MAPPING IS THE POINT</h2>
 *
 * The screen is choosing a HOTBAR SLOT, and the hotbar is nine cells in a row. <b>So the choosers
 * are nine cells in a row, and chooser {@code n} is hotbar slot {@code n}</b> -- left to right,
 * same order, no translation for the player to perform. A grid, or a column, or nine buttons with
 * the number written on them would all work and all make the player read rather than point.
 *
 * <p>Row 3 ({@code 18..26}) rather than the row the hub's stats head is in, so the two screens do
 * not look like the same screen with different contents.
 *
 * <h2>CHROME IN THE BOTTOM ROW, per the hub's standing rule</h2>
 *
 * {@code NexusMenuLayout} states it: chrome in the bottom row, features in the body. Close keeps
 * its slot 49 so the button a player has learned does not move between our screens, and <b>Back
 * takes 48, immediately to its left -- the same cell {@code RecipeBrowserLayout.BACK_SLOT} already
 * uses.</b> Both back buttons in the plugin now agree; see {@link #BACK_SLOT}.
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
     * back button was already there</b>, so after this ruling BOTH back buttons in the plugin sit in
     * the same cell, and <b>the next screen that needs one has a single answer to copy rather than
     * two to choose between.</b>
     *
     * <p>Stated here because two literals that happen to match are indistinguishable from two that
     * agree on purpose, and the second is worth keeping. {@code SettingsMenuLayoutTest} asserts the
     * agreement rather than the number alone, so the two cannot drift apart silently --
     * {@code CraftingMenuLayout.CLOSE_SLOT} records the same relationship for Close, and records
     * that the enchant table deliberately did NOT join it.
     */
    static final int BACK_SLOT = 48;

    /**
     * Close. <b>49, the same slot the crafting menu and the hub use.</b> The one button that must
     * never move between screens; {@code CraftingMenuLayout.CLOSE_SLOT} carries the argument.
     */
    static final int CLOSE_SLOT = 49;

    /** The first chooser, and the start of the body row. Chooser {@code n} is {@code FIRST + n}. */
    static final int FIRST_SLOT_CHOOSER = 18;

    /** Nine, because the hotbar is nine. Derived from nothing -- it IS the hotbar's size. */
    static final int HOTBAR_SIZE = 9;

    /**
     * The nine chooser slots, in hotbar order.
     *
     * <p><b>A {@code List}, not a {@code Set}</b>, and for {@code CraftingMenuLayout
     * .SUGGESTION_SLOTS}' reason: the order IS the meaning here. Chooser index 3 must always render
     * in the cell that means hotbar slot 3, and a {@code Set.copyOf} discards iteration order --
     * that class's javadoc records a version of itself that promised an order it did not have.
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
     * Which hotbar slot this menu slot chooses, if it is a chooser at all.
     *
     * <p>{@code OptionalInt} rather than {@code -1}, matching
     * {@code CraftingMenuLayout.suggestionIndexOf} -- a click handler that forgets to test a
     * sentinel silently treats "not a chooser" as slot -1.
     */
    static OptionalInt chooserFor(int menuSlot) {
        int index = menuSlot - FIRST_SLOT_CHOOSER;
        return index >= 0 && index < HOTBAR_SIZE ? OptionalInt.of(index) : OptionalInt.empty();
    }

    private static List<Integer> buildChoosers() {
        List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < HOTBAR_SIZE; i++) slots.add(FIRST_SLOT_CHOOSER + i);
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
