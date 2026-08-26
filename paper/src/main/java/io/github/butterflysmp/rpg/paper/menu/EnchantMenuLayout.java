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
 * <p>Row-major, as chest inventories are: {@code index = row * 9 + col}. The three enchant slots are
 * COLUMNS 2, 4 and 6, and a slot's candidates stack down ROWS 2, 3 and 4 -- so a column is one
 * enchant slot's worth of choices and is read top to bottom.
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
     * <p>Labelled "Close" with a door, not "Back" with an arrow: there is no parent menu yet, and a
     * back-arrow promises somewhere to go back to. When a hub menu exists this slot becomes the real
     * Back with no layout change.
     */
    public static final int CLOSE_SLOT = 0;

    /** The one slot in the whole menu a player may put an item into or take one out of. */
    public static final int INPUT_SLOT = 4;

    /** The bookshelf readout. A labelled placeholder this pass -- see EnchantMenu. */
    public static final int BOOKSHELF_SLOT = 8;

    /** The hint line, and where a refusal that needs more than a chat message is shown. */
    public static final int INFO_SLOT = 49;

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

    /** The first row of candidates. Rows 0 and 1 carry the input and chrome. */
    private static final int FIRST_CANDIDATE_ROW = 2;
    /** The leftmost enchant-slot column. Columns advance by two so the columns are not adjacent. */
    private static final int FIRST_SLOT_COLUMN = 2;
    private static final int COLUMN_STRIDE = 2;

    /**
     * The chest index a candidate cell occupies: slot 0 -> {20, 29, 38}, 1 -> {22, 31, 40},
     * 2 -> {24, 33, 42}.
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
