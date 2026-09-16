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
     * The status bar: the bottom row, MINUS the two chrome cells in it.
     *
     * <p><b>SEVEN CELLS, BOTH ORIGINS</b> -- 45, 46, 47, 50, 51, 52, 53. Identical to
     * {@code CraftingMenuLayout.STATUS_SLOTS}, and subtracted the same way for the reason that file
     * records: <b>a readout whose geometry depends on how you got there is not a readout.</b>
     * {@link #BACK_SLOT} leaves the bar <b>permanently</b>, though Back is drawn only from the hub;
     * from a block that cell holds filler instead.
     *
     * <p><b>{@code MUTS5-BACK} IS IN SCOPE AGAIN, AND THE PERMANENT SUBTRACTION IS WHAT MAKES 48
     * SAFE.</b> A bar on the bottom row beside Back and Close is the exact arrangement that produced
     * that defect -- a button the bar painted straight over, whose only symptom was "Back doesn't
     * work sometimes". The subtraction is what fixed it on the crafting screen and it is what makes
     * this screen safe by construction rather than by care.
     */
    public static final Set<Integer> STATUS_SLOTS = statusSlots();

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

    private static Set<Integer> statusSlots() {
        Set<Integer> slots = new LinkedHashSet<>();
        int firstOfBottomRow = (ROWS - 1) * COLUMNS;
        for (int slot = firstOfBottomRow; slot < firstOfBottomRow + COLUMNS; slot++) slots.add(slot);
        // SET SUBTRACTION, not a skip inside the loop. Both removals are UNCONDITIONAL even though
        // Back is drawn only from the hub -- making either conditional is what makes the bar's
        // width depend on the origin.
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }

    /**
     * The four cells that are neither tray, bar, nor filler -- the chrome this screen paints
     * individually.
     *
     * <p>Named as a set so the coverage test can state the invariant without re-listing them, and
     * so a fifth button cannot be added without appearing here.
     */
    public static final Set<Integer> CHROME_SLOTS =
            Set.of(INFO_SLOT, CONFIRM_SLOT, BACK_SLOT, CLOSE_SLOT);

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(INPUT_SLOTS);
        slots.removeAll(STATUS_SLOTS);
        slots.remove(INFO_SLOT);
        slots.remove(CONFIRM_SLOT);
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }
}
