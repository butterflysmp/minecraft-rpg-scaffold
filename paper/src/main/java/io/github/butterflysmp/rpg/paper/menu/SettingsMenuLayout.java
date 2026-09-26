package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Where everything sits on the Nexus settings screen: <b>a row of BUTTONS.</b>
 *
 * <p>Six rows, like every other screen here. A short band of settings in the body, chrome in the
 * bottom row, filler everywhere else.
 *
 * <pre>
 *   22   SLOT      opens the Nexus slot picker as its own screen
 *   24   TOGGLE    the Nexus star on or off
 *   48   Back to the Nexus
 *   49   Close
 * </pre>
 *
 * <h2>*** THIS FILE'S ORGANISING ARGUMENT HAS NOW BEEN REWRITTEN THREE TIMES, AND THIS IS THE
 * REWRITE THAT CHANGED WHAT THE SCREEN IS ***</h2>
 *
 * The first version said <i>"THE NINE CHOOSERS ARE A WHOLE ROW, AND THE MAPPING IS THE POINT --
 * the screen is choosing a HOTBAR SLOT, and the hotbar is nine cells in a row."</i> The second
 * replaced that with the same argument over thirty-six cells, when the star was allowed anywhere:
 * <i>"the picker looks like the thing it is picking from."</i>
 *
 * <p><b>Both were arguments about a PICKER, and this screen is no longer one.</b> The thirty-six
 * choosers, the mirror, and {@code slotName} all moved to {@link NexusSlotPickerLayout}; what is
 * left is a menu of settings, of which the slot is one.
 *
 * <p><b>The principle that produced both earlier shapes is intact and is what produced this one</b>
 * -- a screen looks like the thing it is for. A screen of settings looks like a list of settings.
 * <b>Leaving the choosers here would have stacked a third layer of argument in a file that had
 * stopped being about them</b>, which is why Ben ruled the picker into its own class rather than a
 * second section of this one.
 *
 * <h2>TWO BUTTONS, SPACED, RATHER THAN ADJACENT</h2>
 *
 * 22 and 24 leave a gap at 23. <b>They are not a band</b> in {@code NexusMenuLayout}'s sense -- the
 * hub's three stations are a contiguous run because a player reads them as one group of the same
 * kind. These two are different kinds of thing: one opens a screen, one flips a switch. The gap
 * says so without a label.
 *
 * <h2>CHROME IN THE BOTTOM ROW, per the hub's standing rule</h2>
 *
 * Close keeps 49 and Back takes 48 -- <b>the same pair as every other screen, INCLUDING the picker
 * this one now opens.</b> The predecessor put its picker's back at 49, which is our Close, so the
 * one button a player has learned closed the screen on one of two pages they reach in sequence.
 *
 * <p><b>The LABELS differ because the destinations do</b>: <i>"Back to the Nexus"</i> here,
 * <i>"Back to Settings"</i> on the picker. Same slot, same shape, different word.
 */
final class SettingsMenuLayout {

    private SettingsMenuLayout() {}

    /** Six rows, like the hub and the crafting menu. */
    static final int SIZE = 54;

    /** The first slot of the bottom row. */
    static final int BOTTOM_ROW_START = 45;

    /**
     * Back to the Nexus. <b>48, immediately left of Close</b> -- the plugin's one answer for a back
     * button, and unchanged by this slice.
     */
    static final int BACK_SLOT = 48;

    /** Close. <b>49</b>, the one button that must never move between our screens. */
    static final int CLOSE_SLOT = 49;

    /** Open the slot picker. Row 3, column 5 -- left of centre. */
    static final int SLOT_SETTING_SLOT = 22;

    /** Switch the Nexus star on or off. Row 3, column 7 -- right of centre, with 23 left clear. */
    static final int TOGGLE_SETTING_SLOT = 24;

    /**
     * Open the Ability Stone's slot picker (hotbar only, ruling 2). Row 4, DIRECTLY BELOW the star's
     * picker, so the two items' settings read as two rows of one table (PLAN-build-system.md 2.5).
     */
    static final int STONE_SLOT_SETTING_SLOT = 31;

    /** Switch the Ability Stone on or off. Row 4, directly below the star's toggle. */
    static final int STONE_TOGGLE_SETTING_SLOT = 33;

    /**
     * Every settings button, so the filler and the click handler read one list.
     *
     * <p><b>ONE LIST USED TWICE rather than two lists checked against each other</b> -- the
     * {@code PAINTED_SLOTS} lesson. The hub shipped an invisible clickable hole at slot 33 because
     * a cell was subtracted from the filler and painted by nothing; a single list makes that
     * unrepresentable rather than detectable.
     */
    static final Set<Integer> SETTING_SLOTS = Set.of(SLOT_SETTING_SLOT, TOGGLE_SETTING_SLOT,
            STONE_SLOT_SETTING_SLOT, STONE_TOGGLE_SETTING_SLOT);

    /**
     * Everything that is plain filler -- all of it except the settings and the two buttons.
     *
     * <p>Built by SET SUBTRACTION, the construction {@code NexusMenuLayout.FILLER_SLOTS} uses:
     * <b>a filler pane painted over a live button is invisible until someone clicks it.</b>
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(SETTING_SLOTS);
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }
}
