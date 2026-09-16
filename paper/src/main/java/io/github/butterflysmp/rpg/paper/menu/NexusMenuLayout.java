package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Where everything sits in the Nexus hub: a 54-slot menu that is entirely chrome.
 *
 * <p>Six rows with two buttons in the bottom row and filler everywhere else. It is deliberately
 * the emptiest layout in the project -- slice 2 is the FIRST SCREEN, and what it proves is that
 * the star opens something, not that the something does anything.
 *
 * <h2>THE SIZE IS RULED, NOT DERIVED</h2>
 *
 * 54 is the operator's call, not a consequence of what is on the screen -- two buttons would fit
 * in nine slots. It is written here rather than argued because the hub is about to gain rows and
 * a menu that changes size between slices re-teaches the player where everything is.
 */
final class NexusMenuLayout {

    private NexusMenuLayout() {}

    /** Six rows, like the crafting menu and the recipe browser. */
    static final int SIZE = 54;

    /** The first slot of the bottom row. */
    static final int BOTTOM_ROW_START = 45;

    /**
     * Close. <b>49, the same slot the crafting menu uses</b>, so the button a player has learned
     * does not move between our screens.
     *
     * <p><b>ALL FOUR SCREENS NOW AGREE.</b> This paragraph used to say
     * {@code EnchantMenuLayout.CLOSE_SLOT} was still 0 and that the divergence was recorded in
     * {@code CraftingMenuLayout} and not re-litigated here. <b>The enchant screen was redesigned on
     * 2026-09-16 and moved to 49</b>, so there is no divergence left to defer to -- crafting, the
     * hub, settings and the enchant table all put Close in the same cell.
     */
    static final int CLOSE_SLOT = 49;

    /**
     * Settings. Adjacent to {@link #CLOSE_SLOT} by the operator's layout ruling, and
     * {@code NexusMenuLayoutTest} asserts the adjacency rather than leaving it to the two literals
     * happening to sit beside each other.
     */
    static final int SETTINGS_SLOT = 50;

    /**
     * The stats head, and <b>the first FEATURE on this screen rather than more chrome.</b>
     *
     * <h2>THE HUB IS BANDS WITH MEANINGS, NOT CHROME PLUS A LIST OF FEATURES</h2>
     *
     * <b>Operator's ruling. Rows are 1-based, as every layout instruction on this project is.</b>
     *
     * <pre>
     *   row 2   indices  9-17   THE HEADER. The player head, and nothing else.
     *   row 3   indices 18-26   other features
     *   row 4   indices 27-35   crafting-type menus
     *   row 5   indices 36-44   unassigned
     *   row 6   indices 45-53   chrome. Close 49, Settings 50.
     * </pre>
     *
     * <p><b>A BAND ANSWERS "WHERE DOES THIS GO" BY ITSELF, which is why there is no fill order
     * inside one and must not be.</b> Do not propose one, and do not treat the next feature as an
     * open question: its KIND picks its band.
     *
     * <h2>TWO WITHDRAWN RULES, KEPT BECAUSE THE WAY THEY FAILED IS THE USEFUL PART</h2>
     *
     * This javadoc has been wrong twice, and both times it was <b>an assistant proposal derived
     * from the placements that existed at the time</b>:
     *
     * <ol>
     *   <li><i>"Features fill left to right from 20; the next feature is 21."</i> Withdrawn when the
     *       head moved to 13 -- 13 is a centre, not the start of a run.
     *   <li><i>"Where the second feature goes is UNRULED."</i> Withdrawn by the bands above, which
     *       had been the rule the whole time.
     * </ol>
     *
     * <p><b>THE MODEL WAS WRONG, NOT THE NUMBERS.</b> Both rules described the hub as chrome plus a
     * flat list of features, so the player head read as <i>the first feature</i> -- and a flat list
     * has a fill order, so one had to be invented. It is a HEADER, which is why it sits alone and
     * centred and why no fill-order rule ever agreed with a placement the operator actually made.
     *
     * <p><b>A rule derived from two placements described the placements and not the rule</b> -- the
     * same shape as two points looking like a line. When a layout instruction arrives, ask what the
     * ROW MEANS before generalising from where the icon landed.
     */
    static final int STATS_SLOT = 13;

    /**
     * Every slot that is plain filler -- the whole menu except the two buttons.
     *
     * <p><b>Built by SET SUBTRACTION rather than by a loop with {@code continue} arms</b>, the same
     * construction {@code CraftingMenuLayout.STATUS_SLOTS} and
     * {@code RecipeBrowserLayout.FOOTER_FILLER} use, for the same reason: <b>a filler pane painted
     * over a live button is invisible until someone clicks it</b>, and painting over
     * {@link #CLOSE_SLOT} makes the menu unclosable except with Esc. Adding a third button later
     * means adding one line to the removal list, not remembering to extend a skip condition.
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.remove(CLOSE_SLOT);
        slots.remove(SETTINGS_SLOT);
        slots.remove(STATS_SLOT);
        return Set.copyOf(slots);
    }
}
