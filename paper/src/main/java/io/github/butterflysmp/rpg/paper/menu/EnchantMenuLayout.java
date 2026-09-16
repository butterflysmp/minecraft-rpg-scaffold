package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;

import java.util.Optional;

/**
 * Where everything sits in the enchant table, and what will not fit.
 *
 * <p>Pure arithmetic over a 54-slot chest -- no Bukkit, no {@code Inventory}, no {@code ItemStack}
 * -- for the reason {@code WeaponLore} and {@code EnchantEffectLine} are: an {@code ItemStack}
 * cannot be constructed in a unit test ("No RegistryAccess implementation found") and there is no
 * MockBukkit, so the only way any of this reaches the two-second loop is to be the half that does
 * not touch Minecraft. What is left for the boot gate is then genuinely "look at it", not "does the
 * arithmetic work".
 *
 * <h2>COORDINATES ARE 1-BASED IN PROSE AND 0-BASED IN CODE, AND THIS SECTION SAYS WHICH</h2>
 *
 * <b>Every position described in words on this project is 1-BASED</b> -- "row 2, slot 5" is what an
 * operator says and what a player counts. <b>Every index in the code is 0-BASED</b>, because
 * {@code index = row * 9 + col} is what a chest inventory is.
 *
 * <pre>
 *   index = (row - 1) * 9 + (col - 1)        prose -> code
 * </pre>
 *
 * <p><b>THIS FILE USED TO MIX THEM AND IT WAS A DEFECT WAITING TO BE READ.</b> Its javadoc said the
 * candidates were at <i>"COLUMNS 2, 4 and 6 ... ROWS 2, 3 and 4"</i> -- 0-based -- while every
 * instruction about this screen arrives 1-based. <b>"Rows 2, 3 and 4" is true in both conventions
 * and means different rows in each</b>, so a change implemented from the old javadoc would have
 * moved nothing and passed every gate row.
 *
 * <p>That is the raw-versus-index defect {@code NexusLock} records, one abstraction up: <b>two
 * coordinate spaces, the same numerals legal in both, and nothing in the type system to tell them
 * apart.</b> The remedy is the same -- name the space, once, where the numbers live.
 *
 * <h2>THE LAYOUT, IN 1-BASED PROSE</h2>
 *
 * <pre>
 *   row 1   col 5   the hint line              index  4
 *           col 9   the bookshelf readout      index  8
 *   row 2   col 2   the INPUT slot             index 10
 *   rows 2-4  cols 4, 6, 8   the candidates    indices 12 14 16 / 21 23 25 / 30 32 34
 *   row 6   col 5   Close                      index 49
 * </pre>
 *
 * <p>A COLUMN is one enchant slot's worth of choices and is read top to bottom; the columns advance
 * by two so they are not adjacent.
 */
public final class EnchantMenuLayout {

    private EnchantMenuLayout() {}

    /** A double chest. Six rows of nine. */
    public static final int SIZE = 54;
    public static final int ROWS = 6;
    private static final int COLUMNS = 9;

    /**
     * Closes the menu and returns the weapon -- the same path Esc takes, because it IS that path.
     *
     * <p>Row 6, column 5. Labelled "Close" on a BARRIER, not "Back" with an arrow: this closes
     * rather than navigates, and {@code MenuIcons.close} carries that distinction.
     *
     * <h2>THE DISAGREEMENT IS OVER. THIS WAS 0, AND EVERY OTHER SCREEN SAID 49</h2>
     *
     * <b>{@code CraftingMenuLayout.CLOSE_SLOT}'s javadoc recorded the divergence as DELIBERATE</b>
     * -- <i>"the two menus disagree about where Close lives, deliberately, because only the
     * crafting screen was redesigned"</i> -- and {@code NexusMenuLayout} cited it as settled and
     * not to be re-litigated. <b>Both paragraphs were true when written and are now false.</b>
     *
     * <p>They are rewritten rather than deleted, and the argument they carried is kept: a
     * divergence that exists because one screen was redesigned and another was not is a REASON, and
     * the next person to find two screens disagreeing needs to know it was noticed rather than
     * overlooked. What changed is that the enchant screen has now been redesigned too, so the
     * condition the divergence rested on is gone.
     *
     * <p><b>Close is 49 on every screen in this plugin.</b> Crafting, the hub, settings, and now
     * here.
     */
    public static final int CLOSE_SLOT = 49;

    /**
     * The one slot in the whole menu a player may put an item into or take one out of.
     *
     * <p>Row 3, column 2 -- left of the candidate block, on its first row.
     *
     * <h2>THIS SLOT HAS BEEN RIGHT TWICE AND SUPERSEDED TWICE. THE WHOLE TRAIL IS KEPT</h2>
     *
     * Per {@code MenuIcons.close}'s precedent: an argument can outlive the thing it argued for, and
     * deleting it along with its position loses the only reason anyone would have to reconsider.
     * <b>The next reader should find the trail rather than the latest state.</b>
     *
     * <p><b>49, BOTTOM-CENTRE -- the first position.</b> Its javadoc said: <i>"Bottom-centre,
     * directly under the three candidate columns, and directly above the player's own inventory --
     * so the item travels the shortest distance and the thing being enchanted sits nearest the
     * choices being made about it."</i> Still true, and still what was given up: from anywhere in
     * the upper rows the item travels further from the hotbar than it did from 49.
     *
     * <p><b>10, TOP-LEFT -- the second.</b> Two reasons: the screen reads as a FORM, input first
     * and results below; and <b>CLOSE takes 49 because every other screen in this project has it
     * there.</b> One of the two had to move, and <b>consistency across screens outranks travel
     * within one</b> -- a player learns Close once and uses it on four screens, and learns the
     * input slot once per screen anyway. <b>Both of those reasons survive intact at 19</b>; the
     * form still reads downwards and Close still has 49.
     *
     * <p><b>19, HERE -- the third, and it is a consequence rather than a new argument.</b> Ben
     * ruled the candidate block down one row; the input keeps its place beside the block's first
     * row and moves with it. Nothing about 10 was found wrong.
     *
     * <p>So: if someone proposes moving the input nearer the player again, the answer is that it
     * costs the Close convergence above, not that nobody thought of it.
     */
    public static final int INPUT_SLOT = 19;

    /**
     * Back to the Nexus. <b>Slot 48, beside {@link #CLOSE_SLOT} -- 48/49, like every other
     * screen.</b>
     *
     * <h2>IT WAS MISSING, NOT MISPLACED, AND NO ROW ASKED</h2>
     *
     * The slice that gave the Nexus a crafting station and an enchanting station gave <b>crafting
     * a Back button and enchanting none</b>. Not a wrong position -- the constant did not exist and
     * nothing rendered. A player who opened this screen from the hub could only reach it again by
     * closing and re-opening the star.
     *
     * <p><b>What let it ship: the gate asked the crafting screen and never asked this one.</b>
     * There was a row for Back-from-the-hub on crafting and a control for its absence from a world
     * block; the enchant screen had no equivalent row, so the omission had nothing to fail. <b>A
     * feature missing from one of two parallel surfaces is invisible to a gate that only stages
     * the other.</b>
     *
     * <h2>FREE HERE, UNLIKE CRAFTING</h2>
     *
     * This screen has <b>no status bar</b>, so 48 displaces nothing and needs no exclusion set.
     * {@code CraftingMenuLayout.BACK_SLOT} had to buy the same slot from its bottom row; here it
     * was simply empty.
     */
    public static final int BACK_SLOT = 48;

    /** The bookshelf readout. A labelled placeholder this pass -- see EnchantMenu. */
    public static final int BOOKSHELF_SLOT = 8;

    /** The hint line, top-centre, where a screen is read from. */
    public static final int INFO_SLOT = 4;

    /**
     * The bound: three enchant slots of three candidates each.
     *
     * <p>A UI-SIDE bound, exactly as {@code RpgCommand.MAX_DEV_SLOT = 2} is a command-side one.
     * {@link EnchantState} deliberately does not cap slot count -- whether an item gets a fixed 3 or
     * a rolled 1--3 is the roster pass's decision, and the kernel does not pre-empt it. So this
     * guards the reachable surface rather than the model.
     *
     * <p>It is NOT a truncation. See {@link #overflow}.
     */
    public static final int SLOTS = 3;
    public static final int CANDIDATES = 3;

    /**
     * The first row of candidates, 0-BASED -- prose row 3. Row 0 carries the hint and the
     * bookshelf; row 1 is filler.
     *
     * <p>The candidates share row 2 with the input slot, which sits to their left at column 1.
     *
     * <p><b>It was 1, and Ben ruled the block down one row.</b> The measurable consequence is
     * that the LOWEST candidate rank now lands in prose row 5 (indices 36-44), which is still
     * clear of the chrome row at 45-53 -- so {@link #BACK_SLOT} and {@link #CLOSE_SLOT} are
     * untouched by the shift, and so are {@link #INFO_SLOT} and {@link #BOOKSHELF_SLOT} above it.
     */
    private static final int FIRST_CANDIDATE_ROW = 2;
    /**
     * The leftmost enchant-slot column, 0-BASED -- prose column 4. Columns advance by
     * {@link #COLUMN_STRIDE} so they are not adjacent.
     */
    private static final int FIRST_SLOT_COLUMN = 3;
    private static final int COLUMN_STRIDE = 2;

    /**
     * The chest index a candidate cell occupies: slot 0 -> {21, 30, 39}, 1 -> {23, 32, 41},
     * 2 -> {25, 34, 43}.
     */
    public static int rawSlotFor(int slot, int candidate) {
        if (slot < 0 || slot >= SLOTS) {
            throw new IllegalArgumentException("slot " + slot + " is outside 0.." + (SLOTS - 1));
        }
        if (candidate < 0 || candidate >= CANDIDATES) {
            throw new IllegalArgumentException(
                    "candidate " + candidate + " is outside 0.." + (CANDIDATES - 1));
        }
        return (FIRST_CANDIDATE_ROW + candidate) * COLUMNS
                + (FIRST_SLOT_COLUMN + COLUMN_STRIDE * slot);
    }

    /**
     * The (slot, candidate) a chest index addresses, or empty for chrome, filler, and anything
     * outside the menu.
     *
     * <p>The exact inverse of {@link #rawSlotFor}, and the reason a click handler never has to
     * carry a slot->cell table of its own: the click arrives as an index and leaves as a cell.
     *
     * <h2>IT NEEDED NO EDIT WHEN THE LAYOUT MOVED, AND THAT IS A PROPERTY WORTH NAMING</h2>
     *
     * <b>Both directions are computed from the SAME THREE CONSTANTS</b> --
     * {@link #FIRST_CANDIDATE_ROW}, {@link #FIRST_SLOT_COLUMN} and {@link #COLUMN_STRIDE} -- so
     * re-ruling the grid moves the forward function and this one together, by construction. The
     * 2026-09-16 move (rows 2-4 to columns 4-6-8 in prose) changed two constants and neither
     * function body, and <b>the later drop of the whole block down one row changed exactly one
     * constant and neither function body</b> -- two independent re-rulings, no edit here for
     * either.
     *
     * <p><b>That is a claim about today's code, not a guarantee about tomorrow's</b>, which is why
     * {@code EnchantMenuLayoutTest} round-trips all NINE positions:
     * {@code rawSlotFor(cellAt(n)) == n}. A future change that touched one direction and not the
     * other would compile, would move the screen, and would be caught only there -- a click landing
     * on one cell and resolving to another is silent, and the player sees the wrong enchant
     * selected.
     */
    public static Optional<Cell> cellAt(int rawSlot) {
        if (rawSlot < 0 || rawSlot >= SIZE) return Optional.empty();
        int row = rawSlot / COLUMNS;
        int column = rawSlot % COLUMNS;

        int candidate = row - FIRST_CANDIDATE_ROW;
        if (candidate < 0 || candidate >= CANDIDATES) return Optional.empty();

        int offset = column - FIRST_SLOT_COLUMN;
        // The stride check is what keeps the gaps between columns as gaps. Without it, column 3
        // would round into slot 0 and the filler between two columns would become clickable.
        if (offset < 0 || offset % COLUMN_STRIDE != 0) return Optional.empty();
        int slot = offset / COLUMN_STRIDE;
        if (slot >= SLOTS) return Optional.empty();

        return Optional.of(new Cell(slot, candidate));
    }

    /** One candidate cell: which enchant slot, and which choice within it. */
    public record Cell(int slot, int candidate) {}

    /**
     * What this item carries that the table CANNOT SHOW, or empty when it all fits.
     *
     * <p><b>The table refuses an oversized item rather than rendering the first nine cells and
     * saying nothing.</b> Truncating would be display-only -- the extra slots survive every
     * transition and keep working -- and that is exactly what makes it dangerous: an enchant that is
     * ACTIVE and INVISIBLE. It is the same defect {@code EnchantLore} refuses to create when it
     * renders an unknown id rather than hiding it, and the same rule as "a scan that discovers
     * nothing must say so": silently dropping what you found is worse than finding nothing.
     *
     * <p>Returns a sentence rather than a boolean because the refusal is shown to a player, and
     * "this weapon has 4 enchant slots" is actionable where "cannot open" is not.
     */
    public static Optional<String> overflow(EnchantState state) {
        if (state.slots().size() > SLOTS) {
            return Optional.of("this weapon has " + state.slots().size()
                    + " enchant slots and the table shows " + SLOTS);
        }
        for (int slot = 0; slot < state.slots().size(); slot++) {
            EnchantSlot candidates = state.slots().get(slot);
            if (candidates.candidates().size() > CANDIDATES) {
                return Optional.of("enchant slot " + (slot + 1) + " offers "
                        + candidates.candidates().size() + " candidates and the table shows "
                        + CANDIDATES);
            }
        }
        return Optional.empty();
    }
}
