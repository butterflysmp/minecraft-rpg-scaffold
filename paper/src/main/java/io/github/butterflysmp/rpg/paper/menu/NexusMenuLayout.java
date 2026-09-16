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
     * <p>{@code EnchantMenuLayout.CLOSE_SLOT} is still 0 and the two disagree -- that divergence is
     * recorded in {@code CraftingMenuLayout} and is not re-litigated here. This matches the newer
     * pair, which is the majority and the one a player meets first.
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
     * <h2>CHROME IN THE BOTTOM ROW. FEATURES IN THE BODY, FILLING LEFT TO RIGHT FROM 20</h2>
     *
     * <b>Stated now, before a third button makes it accidental.</b> Close and Settings are chrome
     * and live in the bottom row; everything a player came here to USE goes in the body and
     * APPENDS. The alternative -- centring whatever features exist -- looks best with one icon and
     * <b>moves every icon the moment a second one arrives</b>, which is churn in a screen players
     * learn by position.
     *
     * <p>So 20 is a starting point, not a centre. The next feature is 21.
     *
     * <p><b>THIS IS THE ASSISTANT'S PROPOSAL, NOT BEN'S RULING</b>, unlike {@link #SIZE} and the
     * 49/50 pair. It is a number a player experiences, so it went to him with the slice rather than
     * being buried -- and it is one constant, so one word changes it.
     */
    static final int STATS_SLOT = 20;

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
