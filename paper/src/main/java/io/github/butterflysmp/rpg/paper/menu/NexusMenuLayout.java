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
     * <b>Five stations spanning 29-33, centred on 31 in a row of 27-35.</b>
     *
     * <h2>*** THE ROW IS COMPLETE AS OF SLICE 13a, AND THIS NOTE IS KEPT RATHER THAN DELETED ***</h2>
     *
     * <b>Ours was 31, 32 and 33 -- the RIGHT-HAND three</b>, and this javadoc said the block
     * <i>"will keep sitting right of centre until an ender chest and an anvil exist."</i> The vault
     * took 29 and <b>the anvil has now taken 30</b>, so the run is the contiguous 29-33 the
     * predecessor had and the asymmetry is gone.
     *
     * <p><b>The argument it records is the part worth keeping.</b> Re-centring the three we had
     * would have been a move undone twice -- once to add 30, once to add 29 -- and the row reaching
     * its designed shape without a single icon moving is that call paying off. <b>The next person
     * to propose re-centring a partial row should find this rather than re-derive it.</b>
     *
     * <p><b>What is NOT kept is the prediction attached to it.</b> This note used to end <i>"It is
     * NOT a ruling that 29 and 30 will be built -- neither has been designed"</i>, which was true
     * when written and is now false twice over. A prediction left standing after its subject ships
     * reads as a live open question, and the next reader would spend the time finding out it is
     * closed.
     */
    static final int GRINDSTONE_SLOT = 33;

    /**
     * The Equipment screen (accessories Slice B): row 3, "other features" -- the first thing in that
     * band. A feature, not a crafting-type menu, so row 3 and not row 4; and centred under the stats
     * head (13) because it is the other half of "you": what you wear, under what it adds up to.
     * Ungated: the screen shows armour a player can wear from level 1.
     */
    static final int EQUIPMENT_SLOT = 22;

    /**
     * The vault. <b>Row 4, column 2 -- and it is 29, which the row above says was reserved for it.</b>
     *
     * <h2>THE ROW IS FIVE OF FIVE, AND THE GAP THIS JAVADOC WARNED ABOUT IS CLOSED</h2>
     *
     * The predecessor's row, quoted in {@link #GRINDSTONE_SLOT}'s javadoc:
     *
     * <pre>
     *   29 ender chest   30 anvil   31 crafting   32 enchanting   33 grindstone
     * </pre>
     *
     * <b>The block is contiguous again: 29-33, centred on 31.</b>
     *
     * <p><b>This entry earned its keep and is kept for that reason.</b> It said <i>"Do not close the
     * gap by moving the vault to 30: the anvil's cell is 30, and a vault sitting in it would have to
     * move the day an anvil ships."</i> <b>That day is slice 13a, and nothing moved.</b> A cell held
     * empty on a prediction, by a note nobody could falsify at the time, is the cheapest kind of
     * correctness there is -- and the next person tempted to fill a reserved hole should find this
     * worked rather than assume it was over-caution.
     *
     * @see #ANVIL_SLOT the cell this note was holding
     */
    static final int VAULT_SLOT = 29;

    /**
     * The anvil. <b>Row 4, column 3 -- the cell {@link #VAULT_SLOT}'s note has been holding open.</b>
     *
     * <p>Slice 13a. Ben's ruling on its level is in {@code NexusStationGate.Station}; the screen
     * itself is {@code AnvilMenu}.
     *
     * <p><b>Read this constant, never the literal 30.</b> {@code NexusStationGate.at} says why: the
     * hub has already paid for two hand-maintained lists checked against each other, and a third
     * copy of a slot number is how a gate comes to protect a cell nothing renders.
     */
    static final int ANVIL_SLOT = 30;

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
    static final Set<Integer> FILLER_SLOTS;

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
            CLOSE_SLOT, SETTINGS_SLOT, STATS_SLOT,
            VAULT_SLOT, ANVIL_SLOT, CRAFTING_SLOT, ENCHANT_SLOT, GRINDSTONE_SLOT,
            EQUIPMENT_SLOT);

    /**
     * *** THE SUBTRACTION READS {@link #PAINTED_SLOTS}. IT USED TO RESTATE IT BY HAND. ***
     *
     * <p>{@code FILLER_SLOTS}' javadoc has claimed since slice 6 that <i>"{@code buildFiller}
     * subtracts exactly this set"</i>. <b>It did not.</b> It subtracted a second
     * {@code Set.of(...)} literal listing the same six constants -- the two-hand-maintained-lists
     * shape whose autopsy is in that very javadoc, rebuilt one line below the account of it.
     *
     * <p>It was never wrong, because the two literals agreed. <b>Adding the vault is the first edit
     * that had to change both</b>, which is exactly the moment the defect was designed to bite, and
     * it is why this is fixed here rather than left as a tidy-up.
     *
     * <p><b>The declaration order is load-bearing now.</b> A static field initialises in source
     * order, so {@code PAINTED_SLOTS} must be declared ABOVE the block that reads it or the
     * subtraction would see {@code null}. {@code FILLER_SLOTS} is therefore declared uninitialised
     * above and assigned here, which keeps it where a reader expects to find it while making the
     * dependency explicit rather than positional.
     */
    static {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(PAINTED_SLOTS);
        FILLER_SLOTS = Set.copyOf(slots);
    }
}
