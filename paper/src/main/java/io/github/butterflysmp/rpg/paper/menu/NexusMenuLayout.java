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
     * Crafting. <b>Row 4, column 4 -- the CRAFTING-TYPE band.</b>
     *
     * <p>Its band is picked by its KIND, which is the whole point of the band table above: nobody
     * chose between "next to the head" and "start of a row", because the row means something.
     */
    static final int CRAFTING_SLOT = 31;

    /**
     * Enchanting. <b>Row 4, column 5 -- between crafting and the grindstone.</b>
     *
     * <p>Adjacent to {@link #CRAFTING_SLOT} rather than spread across the row: the stations are a
     * contiguous run and a player reads them as one. {@code NexusMenuLayoutTest} asserts the run,
     * so a later station cannot silently split it.
     */
    static final int ENCHANT_SLOT = 32;

    /**
     * The grindstone. <b>Row 4, column 6 -- the third station, and the right-hand end of the run
     * we have built.</b> Ben's ruling, and the predecessor project had it at 33 too.
     *
     * <h2>THE ROW IS FIVE STATIONS WIDE AND WE HAVE THE RIGHT-HAND THREE. IT IS INCOMPLETE, NOT OFF-CENTRE</h2>
     *
     * The predecessor's hub row, read from its source rather than remembered:
     *
     * <pre>
     *   29 ender chest   30 anvil   31 crafting   32 enchanting   33 grindstone
     * </pre>
     *
     * <b>Five stations spanning 29-33, centred on 31 in a row of 27-35.</b> Ours is <b>31, 32 and
     * 33 -- the RIGHT-HAND three</b>; the two that are missing are <b>29 and 30, both to the
     * LEFT</b>.
     *
     * <p><b>So the block sits right of centre, and it will keep sitting right of centre until an
     * ender chest and an anvil exist.</b> That is the correct appearance of an unfinished row, and
     * <b>re-centring the three we have is a move that would have to be undone twice</b> -- once to
     * add 30, once to add 29.
     *
     * <p>Recorded here so the next person to notice the asymmetry finds the answer instead of
     * fixing it. <b>It is NOT a ruling that 29 and 30 will be built</b> -- neither has been
     * designed -- only that the row was laid out expecting them.
     */
    static final int GRINDSTONE_SLOT = 33;

    /**
     * Every slot that is plain filler -- the whole menu except the buttons and the stations.
     *
     * <p><b>Built by SET SUBTRACTION rather than by a loop with {@code continue} arms</b>, the same
     * construction {@code CraftingMenuLayout.STATUS_SLOTS} and
     * {@code RecipeBrowserLayout.FOOTER_FILLER} use, for the same reason: <b>a filler pane painted
     * over a live button is invisible until someone clicks it</b>, and painting over
     * {@link #CLOSE_SLOT} makes the menu unclosable except with Esc. Adding a third button later
     * means adding one line to the removal list, not remembering to extend a skip condition.
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    /**
     * Every cell {@code NexusMenu.render()} must paint individually.
     *
     * <h2>ONE LIST, USED TWICE -- AND IT IS ONE LIST BECAUSE IT WAS TWO AND THEY DISAGREED</h2>
     *
     * <b>The subtraction below and the paint list in {@code NexusMenu.render()} used to be two
     * hand-maintained lists.</b> The grindstone was added to the first and not the second, so slot
     * 33 was removed from the filler and then painted by nothing: <b>an invisible, clickable hole
     * whose click handler worked perfectly.</b> It reached a screenshot.
     *
     * <p><b>THE INVARIANT NOTHING CHECKED: EVERY SLOT NOT IN {@link #FILLER_SLOTS} MUST BE PAINTED
     * BY SOMETHING.</b> Set subtraction makes the filler correct by construction and says nothing
     * at all about whether anyone paints what it left out.
     *
     * <p>Now {@link #buildFiller} subtracts exactly this set, and
     * {@code NexusMenuLayoutTest} asserts the partition -- so a seventh cell cannot be subtracted
     * without being declared here, and being declared here is what tells the next reader it needs
     * a painter.
     */
    static final Set<Integer> PAINTED_SLOTS = Set.of(
            CLOSE_SLOT, SETTINGS_SLOT, STATS_SLOT, CRAFTING_SLOT, ENCHANT_SLOT, GRINDSTONE_SLOT);

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        // THE SAME SET render() PAINTS. Two lists is what produced the hole at 33.
        slots.removeAll(Set.of(CLOSE_SLOT, SETTINGS_SLOT, STATS_SLOT,
                CRAFTING_SLOT, ENCHANT_SLOT, GRINDSTONE_SLOT));
        return Set.copyOf(slots);
    }
}
