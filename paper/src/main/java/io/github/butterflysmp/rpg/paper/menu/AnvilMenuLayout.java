package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The anvil's geometry: two inputs, one preview, and chrome.
 *
 * <p>Pure static data, no Bukkit types, so it is unit-testable without a server -- the same split
 * {@code GrindstoneMenuLayout}, {@code EnchantMenuLayout} and {@code CraftingMenuLayout} use.
 *
 * <p><b>COORDINATES ARE 1-BASED IN PROSE AND 0-BASED IN CODE.</b> {@code index = (row - 1) * 9 +
 * (col - 1)}.
 *
 * <pre>
 *    0  1  2  3 [i] 5  6  7  8      [i]  4  the screen's own icon
 *    9 10 11 12 13 14 15 16 17
 *   18 19 [T]21 [D]23 [O]25 26      [T] 20  target, the item being upgraded
 *   27 28 29 30 31 32 33 34 35      [D] 22  donor, the item sacrificed
 *   36 37 38 39 40 41 42 43 44      [O] 24  the preview. NEVER TAKEABLE.
 *   45 46 47 [B][C]50 51 52 53      [B] 48  back to the Nexus, hub origin only
 *            ^^^^^                  [C] 49  close
 * </pre>
 *
 * <b>Ben's ruling: the three cells are centred on 22, the screen's midline, one gap between each.</b>
 * They read left to right the way the vanilla anvil does -- what you have, what you give up, what
 * you get -- and the gaps stop three item icons reading as one row of inventory.
 *
 * <h2>*** SLOT 31 IS 13b's CONFIRM CELL, AND IT IS ORDINARY FILLER TODAY ***</h2>
 *
 * Ben has ruled where the confirm button goes. <b>It is recorded here in prose and NOWHERE in
 * code</b>: there is no {@code CONFIRM_SLOT} constant and nothing paints 31, so it is in
 * {@link #FILLER_SLOTS} like any other empty cell and the filler loop covers it.
 *
 * <p><b>The alternative would ship the hub's own defect.</b> {@code NexusMenuLayout} subtracted slot
 * 33 from its filler set and then painted it with nothing -- <b>an invisible, clickable hole whose
 * click handler worked perfectly.</b> A constant reserved here with no painter is that failure
 * exactly, and it would be introduced on purpose. {@code AnvilMenuLayoutTest} asserts 31 IS filler,
 * so the reservation cannot become a hole by being half-implemented.
 *
 * @see AnvilMenu for what paints each of these
 */
final class AnvilMenuLayout {

    private AnvilMenuLayout() {}

    static final int SIZE = 54;
    static final int ROWS = 6;
    private static final int COLUMNS = 9;

    /**
     * The screen's own icon, top-centre, where a screen is read from.
     *
     * <p><b>INFORMATION, never an action</b> -- the constraint {@code GrindstoneMenuLayout.CONFIRM_SLOT}
     * records at length. Slot 4 is a named constant on three other layouts and all three use it for
     * a hint; reusing it for a button would make one cell mean two things across the plugin.
     */
    static final int INFO_SLOT = 4;

    /** The item being upgraded. <b>Its rarity prices the transfer</b>, and its score is replaced. */
    static final int TARGET_SLOT = 20;

    /** The item being sacrificed. 13b consumes this one. */
    static final int DONOR_SLOT = 22;

    /**
     * The preview.
     *
     * <h2>*** IT IS A READOUT THAT NEVER BECOMES CARGO ***</h2>
     *
     * <b>THE PREVIEW IS NEVER TAKEABLE, AND 13b DOES NOT TRANSFER BY MOVING IT.</b> 13b mutates the
     * item resting in {@link #TARGET_SLOT} and consumes the one in {@link #DONOR_SLOT}; this cell
     * is never the route by which anything reaches the player. <b>Say so out loud, because the next
     * reader will assume otherwise</b> -- every other result slot they have seen, in this project
     * and in vanilla, is where you collect the thing you made.
     *
     * <p><b>It is deliberately absent from {@link #INPUT_SLOTS}, and that is load-bearing.</b>
     * {@code Menu.returnEverything} iterates {@code returnedSlots()}, which defaults to
     * {@code inputSlots()}; a preview listed there would be handed to the player on every close,
     * death, disconnect and shutdown, unpaid for. {@code CraftingMenuLayout.RESULT_SLOT} records the
     * same rule -- the slot MECHANICS are borrowed from there and only the never-takeable part is
     * new to this project.
     */
    static final int OUTPUT_SLOT = 24;

    /**
     * The two cells a player may put an item into.
     *
     * <p>{@link #OUTPUT_SLOT} is not here. See its javadoc for why that is the whole safety
     * argument rather than a tidiness one.
     */
    static final Set<Integer> INPUT_SLOTS = Set.of(TARGET_SLOT, DONOR_SLOT);

    /** Back to the Nexus. 48, beside Close -- like every other screen. */
    static final int BACK_SLOT = 48;

    /** Close. 49, like every other screen. */
    static final int CLOSE_SLOT = 49;

    /**
     * The status bar: the bottom row, minus whichever chrome cells are actually drawn in it.
     *
     * <pre>
     *   from the HUB     45 46 47       50 51 52 53     SEVEN   Back at 48, Close at 49
     *   from a BLOCK     45 46 47 48    50 51 52 53     EIGHT   Close at 49
     * </pre>
     *
     * <p><b>The fork is the grindstone's, inherited rather than re-argued.</b> That layout's javadoc
     * records the whole exchange: seven-cells-on-both-origins was agreed on the argument that <i>a
     * readout whose geometry depends on how you got there is not a readout</i>, and <b>Ben overturned
     * it once he had seen the screen</b> -- from a world block there is no Back button, so 48 was a
     * black filler pane sitting in the middle of a row of coloured ones. <b>The argument was about a
     * reader comparing two screens; what a player sees is one screen with a hole in its readout.</b>
     *
     * <p>Taking the same fork here is what keeps the two screens looking like one plugin. The
     * exclusion is by construction rather than by care: the bar takes 48 only on the path where no
     * button is drawn there.
     */
    static Set<Integer> statusSlots(boolean fromHub) {
        return fromHub ? STATUS_FROM_HUB : STATUS_FROM_BLOCK;
    }

    /** Seven cells: the bottom row less Back and Close. */
    static final Set<Integer> STATUS_FROM_HUB = buildStatus(true);

    /** Eight cells: the bottom row less Close. From a block, 48 is bar rather than chrome. */
    static final Set<Integer> STATUS_FROM_BLOCK = buildStatus(false);

    /**
     * The cells this screen paints INDIVIDUALLY -- neither input, bar, nor filler.
     *
     * <p>{@link #OUTPUT_SLOT} is here because something has to paint it and it is not filler. That
     * does NOT make it chrome in the "player cannot touch it" sense only -- it is chrome in the
     * strict sense this set means: <b>a cell whose content this menu decides</b>.
     *
     * <p>Forks with the bar, and the two are complements within the bottom row: from the hub that
     * row is {@code bar(7) + Back + Close}, from a block {@code bar(8) + Close}. Nine either way,
     * which is why {@link #FILLER_SLOTS} does not fork.
     */
    static Set<Integer> chromeSlots(boolean fromHub) {
        return fromHub
                ? Set.of(INFO_SLOT, OUTPUT_SLOT, BACK_SLOT, CLOSE_SLOT)
                : Set.of(INFO_SLOT, OUTPUT_SLOT, CLOSE_SLOT);
    }

    /**
     * Every slot that is plain filler -- the whole menu except the inputs, the preview and the
     * chrome.
     *
     * <p><b>Built by SET SUBTRACTION</b>, the construction {@code NexusMenuLayout.FILLER_SLOTS} and
     * {@code GrindstoneMenuLayout.FILLER_SLOTS} use, for the reason those pages record: <b>a filler
     * pane painted over a live button is invisible until someone clicks it.</b> Adding 13b's confirm
     * later means one more line here, not remembering to extend a skip condition.
     *
     * <p><b>And painting filler over an INPUT CELL would destroy an item rather than hide a
     * button</b>, which is why the two inputs are removed as a set rather than skipped in a loop.
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    private static Set<Integer> buildStatus(boolean fromHub) {
        Set<Integer> slots = new LinkedHashSet<>();
        int firstOfBottomRow = (ROWS - 1) * COLUMNS;
        for (int slot = firstOfBottomRow; slot < firstOfBottomRow + COLUMNS; slot++) slots.add(slot);
        // SET SUBTRACTION, not a skip inside the loop. Close always leaves the bar; Back leaves it
        // ONLY when Back is actually drawn.
        slots.remove(CLOSE_SLOT);
        if (fromHub) slots.remove(BACK_SLOT);
        return Set.copyOf(slots);
    }

    /**
     * <b>IT DOES NOT FORK, AND THAT IS ARITHMETIC RATHER THAN A DECISION.</b> The whole bottom row
     * is bar-or-chrome on both paths -- nine cells either way -- so what the filler excludes is the
     * same set regardless of origin. <b>Subtracting the ROW rather than the individual buttons is
     * what makes that true by construction</b>, and it is why 48 can move between bar and chrome
     * without this set noticing.
     */
    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(INPUT_SLOTS);
        slots.remove(OUTPUT_SLOT);

        int firstOfBottomRow = (ROWS - 1) * COLUMNS;
        for (int slot = firstOfBottomRow; slot < firstOfBottomRow + COLUMNS; slot++) {
            slots.remove(slot);
        }
        slots.remove(INFO_SLOT);
        return Set.copyOf(slots);
    }
}
