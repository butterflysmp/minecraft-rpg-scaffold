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
     * The tray: rows 3 and 4, columns 2-8. <b>Fourteen cells.</b> Ben's ruling.
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
     * Strip. <b>Row 5, centre -- under the tray and above the chrome.</b>
     *
     * <p><b>NOT BEN'S RULING.</b> The tray and the hub slot are his; this cell is the author's and
     * is flagged in the PR as owed a decision.
     *
     * <p><b>IT IS NOT AN INPUT SLOT, AND THAT IS LOAD-BEARING RATHER THAN INCIDENTAL.</b> The arming
     * countdown repaints this one cell twice a second. If it were ever inside {@link #INPUT_SLOTS},
     * that repaint would overwrite a player's weapon -- and with a full tray, up to fourteen of
     * them, unrecoverably. {@code GrindstoneMenuLayoutTest} asserts the disjointness so the LAYOUT
     * is off the list of ways that can happen; the tick's own discipline is the other half.
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
        for (int row = 2; row <= 3; row++) {                 // 0-based rows 2 and 3 = prose 3 and 4
            for (int column = 1; column <= 7; column++) {     // 0-based 1..7 = prose columns 2..8
                slots.add(row * COLUMNS + column);
            }
        }
        return Set.copyOf(slots);
    }

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(INPUT_SLOTS);
        slots.remove(INFO_SLOT);
        slots.remove(CONFIRM_SLOT);
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }
}
