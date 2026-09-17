package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The grindstone's geometry: a fourteen-cell tray, one confirm button, and chrome.
 *
 * <p>Pure static data, no Bukkit types, so it is unit-testable without a server -- the same split
 * {@code EnchantMenuLayout} and {@code CraftingMenuLayout} use.
 *
 * <p><b>COORDINATES ARE 1-BASED IN PROSE AND 0-BASED IN CODE.</b> {@code index = (row - 1) * 9 +
 * (col - 1)}.
 */
public final class GrindstoneMenuLayout {

    private GrindstoneMenuLayout() {}

    public static final int SIZE = 54;
    public static final int ROWS = 6;
    private static final int COLUMNS = 9;

    /** The hint line, top-centre, where a screen is read from. Matches every other screen. */
    public static final int INFO_SLOT = 4;

    /**
     * The tray: rows 2, 3 and 4, columns 2-8. <b>Twenty-one cells.</b> Ben's ruling.
     *
     * <p><b>Column 1 and column 9 stay filler on both rows</b>, so the tray reads as a block with a
     * margin rather than running into the screen edge.
     *
     * <p><b>THE WEAPONS IN THE TRAY ARE THE WEAPONS STRIPPED -- edited where they sit.</b> There is
     * no output slot and no "take your result" step: the predecessor had an input/output pair, a
     * {@code hasStrippedOutput} flag and a second close branch, and all three are state for a step
     * that was ruled away.
     */
    public static final Set<Integer> INPUT_SLOTS = trayCells();

    /**
     * The status bar: the bottom row, minus whichever chrome cells are actually drawn in it.
     *
     * <pre>
     *   from the HUB     45 46 47       50 51 52 53     SEVEN   Back at 48, Close at 49
     *   from a BLOCK     45 46 47 48    50 51 52 53     EIGHT   Close at 49
     * </pre>
     *
     * <h2>*** THE GEOMETRY FORKS BY ORIGIN, AND THAT OVERTURNS AN ARGUMENT MADE AT LENGTH HERE ***</h2>
     *
     * <b>This javadoc used to read "SEVEN CELLS, BOTH ORIGINS", on the argument that <i>a readout
     * whose geometry depends on how you got there is not a readout</i></b> -- borrowed from
     * {@code CraftingMenuLayout.STATUS_SLOTS}, where it is still true and still the rule.
     *
     * <p><b>Ben agreed to seven-both-origins on that argument BEFORE HE HAD SEEN IT, and ruled the
     * other way once he had.</b> From a world block there is no Back button, so 48 was a black
     * filler pane sitting in the middle of a row of colour-changing ones. <b>The argument was about
     * a reader comparing two screens; what a player actually sees is one screen with a hole in its
     * readout.</b>
     *
     * <p><b>OVERTURNED BY LOOKING, and it is the second layout rule of mine to die that way</b> --
     * the first was <i>"column 8 is the navigation column"</i>, which had two instances until Ben
     * moved one of them. <b>Kept rather than deleted, because the argument is sound and will be
     * made again by anyone comparing this file to {@code CraftingMenuLayout}.</b>
     *
     * <p><b>{@code MUTS5-BACK} IS STILL OUT OF SCOPE, AND THE FORK IS WHY RATHER THAN IN SPITE OF
     * IT.</b> That defect was a bar painting over a button that was there. <b>Here the bar takes 48
     * only on the path where no button is drawn</b> -- the two are mutually exclusive by
     * construction, not by care, and {@code GrindstoneMenuLayoutTest} asserts the exclusion on both
     * origins.
     *
     * @param fromHub was this screen opened from the Nexus? If so, Back occupies 48 and the bar
     *                does not.
     */
    public static Set<Integer> statusSlots(boolean fromHub) {
        return fromHub ? STATUS_FROM_HUB : STATUS_FROM_BLOCK;
    }

    /** Seven cells: the bottom row less Back and Close. */
    public static final Set<Integer> STATUS_FROM_HUB = buildStatus(true);

    /** Eight cells: the bottom row less Close. From a block, 48 is bar rather than chrome. */
    public static final Set<Integer> STATUS_FROM_BLOCK = buildStatus(false);

    /**
     * The cells this screen paints INDIVIDUALLY -- neither tray, bar, nor filler.
     *
     * <p><b>Forks with the bar, and the two are complements within the bottom row.</b> From the hub
     * that row is {@code bar(7) + Back + Close}; from a block it is {@code bar(8) + Close}. Nine
     * either way, which is why {@link #FILLER_SLOTS} does not fork.
     */
    public static Set<Integer> chromeSlots(boolean fromHub) {
        return fromHub
                ? Set.of(INFO_SLOT, CONFIRM_SLOT, BACK_SLOT, CLOSE_SLOT)
                : Set.of(INFO_SLOT, CONFIRM_SLOT, CLOSE_SLOT);
    }

    /**
     * Strip. <b>Row 5, dead centre -- under the tray and above the chrome.</b>
     *
     * <p><b>Ben's ruling.</b> The screen reads <b>tray, then action, then chrome</b>, top to bottom.
     *
     * <h2>WHAT WAS REFUSED, BECAUSE THE REASONS OUTLIVE THE CHOICE</h2>
     *
     * <ul>
     *   <li><b>Beside Back and Close in the chrome row -- REFUSED.</b> It would put a
     *       <b>destructive, irreversible</b> action immediately next to the two navigation buttons.
     *       The arming delay exists precisely because a misclick here costs a player thousands of
     *       XP, and placing it where the hand already goes to leave the screen works against that.
     *   <li><b>Slot 4, top centre -- REFUSED, and THIS IS THE CONSTRAINT THAT WILL STILL BIND WHEN
     *       SOMEONE ADDS THE NEXT SCREEN.</b> <b>Every screen that uses slot 4 at all uses it for
     *       INFORMATION</b> -- measured, it is a named constant on exactly three layouts:
     *       {@code CraftingMenuLayout.INDICATOR_SLOT}, {@code EnchantMenuLayout.INFO_SLOT} and
     *       {@link #INFO_SLOT} here. (The hub and the settings screen leave it filler, so they
     *       neither support nor contradict it -- <b>stated that way rather than as "every screen",
     *       which is what the first draft of this note claimed.</b>)
     *       <br><b>Reusing it for an ACTION would make one cell mean two things</b>, and a player
     *       who has learned "4 is the hint" would learn otherwise by pressing it.
     * </ul>
     *
     * <p><b>IT IS NOT AN INPUT SLOT, AND THAT IS LOAD-BEARING RATHER THAN INCIDENTAL.</b> The arming
     * countdown repaints this cell and the seven bar cells twice a second. If any of those eight
     * were ever inside {@link #INPUT_SLOTS}, that repaint would overwrite a player's gear -- and
     * with a full tray, <b>up to twenty-one items</b>, unrecoverably.
     * {@code GrindstoneMenuLayoutTest} asserts the disjointness so the LAYOUT is off the list of
     * ways that can happen; the tick's own discipline is the other half.
     */
    public static final int CONFIRM_SLOT = 40;

    /** Back to the Nexus. 48, beside Close -- like every other screen. */
    public static final int BACK_SLOT = 48;

    /** Close. 49, like every other screen. */
    public static final int CLOSE_SLOT = 49;

    /**
     * Every slot that is plain filler -- the whole menu except the tray and the three buttons.
     *
     * <p><b>Built by SET SUBTRACTION</b>, the construction {@code NexusMenuLayout.FILLER_SLOTS} and
     * {@code CraftingMenuLayout.STATUS_SLOTS} use, for the reason that page records: <b>a filler
     * pane painted over a live button is invisible until someone clicks it.</b> Adding a fourth
     * button later means one more line here, not remembering to extend a skip condition.
     *
     * <p><b>And painting filler over a TRAY CELL would destroy an item rather than hide a button</b>,
     * which is why the tray is removed as a set rather than skipped in a loop.
     */
    public static final Set<Integer> FILLER_SLOTS = buildFiller();

    private static Set<Integer> trayCells() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int row = 1; row <= 3; row++) {                 // 0-based rows 1-3 = prose rows 2-4
            for (int column = 1; column <= 7; column++) {     // 0-based 1..7 = prose columns 2..8
                slots.add(row * COLUMNS + column);
            }
        }
        return Set.copyOf(slots);
    }

    private static Set<Integer> buildStatus(boolean fromHub) {
        Set<Integer> slots = new LinkedHashSet<>();
        int firstOfBottomRow = (ROWS - 1) * COLUMNS;
        for (int slot = firstOfBottomRow; slot < firstOfBottomRow + COLUMNS; slot++) slots.add(slot);
        // SET SUBTRACTION, not a skip inside the loop. Close always leaves the bar; Back leaves it
        // ONLY when Back is actually drawn, which is the fork Ben ruled after seeing the screen.
        slots.remove(CLOSE_SLOT);
        if (fromHub) slots.remove(BACK_SLOT);
        return Set.copyOf(slots);
    }

    /**
     * Every slot that is plain filler.
     *
     * <p><b>IT DOES NOT FORK, AND THAT IS ARITHMETIC RATHER THAN A DECISION.</b> The whole bottom
     * row is bar-or-chrome on both paths -- nine cells either way -- so what the filler excludes is
     * the same set regardless of origin. <b>Subtracting the ROW rather than the individual buttons
     * is what makes that true by construction</b>, and it is why 48 can move between bar and chrome
     * without this set noticing.
     */
    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(INPUT_SLOTS);

        int firstOfBottomRow = (ROWS - 1) * COLUMNS;
        for (int slot = firstOfBottomRow; slot < firstOfBottomRow + COLUMNS; slot++) {
            slots.remove(slot);
        }
        slots.remove(INFO_SLOT);
        slots.remove(CONFIRM_SLOT);
        return Set.copyOf(slots);
    }
}
