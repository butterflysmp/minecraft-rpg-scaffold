package io.github.butterflysmp.rpg.paper.menu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Where the crafting grid sits, as pure arithmetic.
 *
 * <p>Its own class for the reason {@code EnchantMenuLayout} is: if these numbers lived in
 * {@code CraftingMenu} the only way to check them would be to boot Paper and count squares by eye.
 * Here they are a two-second test. No Bukkit imports, deliberately -- this is geometry.
 *
 * <p><b>The matrix index order is not ours to choose.</b> {@code Server.getCraftingRecipe} documents
 * the array it takes as
 *
 * <pre>
 * [ 0 1 2 ]
 * [ 3 4 5 ]
 * [ 6 7 8 ]
 * </pre>
 *
 * so {@link #matrixIndexOf} is row-major over the 3x3 block and {@link #rawSlotForMatrix} is its
 * exact inverse. Getting this transposed would hand the server a mirrored matrix, which silently
 * matches the wrong recipe for every shaped one and matches correctly for every shapeless one --
 * a bug that looks like "some recipes are broken" rather than like a transpose.
 */
public final class CraftingMenuLayout {

    private CraftingMenuLayout() {}

    /** Six rows, as a double chest is. */
    public static final int SIZE = 54;

    public static final int ROWS = 6;
    private static final int COLUMNS = 9;

    /**
     * A barrier, centred in the bottom row.
     *
     * <p><b>MOVED from slot 0, and the enchant table did NOT move with it.</b> This constant used to
     * carry the line "same slot the enchant table uses, so the two agree" -- and that is now false:
     * {@code EnchantMenuLayout.CLOSE_SLOT} is still 0. The two menus disagree about where Close
     * lives, deliberately, because only the crafting screen was redesigned. Said out loud rather
     * than left as a stale claim, which is the failure this repo keeps recording.
     *
     * <p><b>It sits INSIDE the status bar's row and the bar must never paint over it.</b> That is
     * not left to a loop remembering to skip it -- see {@link #STATUS_SLOTS}.
     */
    public static final int CLOSE_SLOT = 49;

    /** The 3x3 block's width and height, and the length of the matrix the server wants. */
    public static final int GRID = 3;
    public static final int MATRIX_LENGTH = GRID * GRID;

    private static final int FIRST_GRID_ROW = 1;
    private static final int FIRST_GRID_COLUMN = 1;

    /**
     * The result. Row 2, column 5 -- vertically centred on the grid, one cell right of the arrow,
     * mirroring the vanilla screen this replaces.
     *
     * <p><b>Not an input slot, and that is load-bearing.</b> {@code Menu.returnEverything} iterates
     * {@code inputSlots()}; a preview listed there would be handed to the player on every close,
     * death, disconnect and shutdown, unpaid for. See {@code CraftingMenu.inputSlots}.
     */
    public static final int RESULT_SLOT = 23;

    /**
     * The arrow between the grid and the result. Row 2, column 4. Pure decoration.
     *
     * <p>Painted once and never repainted, so it is not a state indicator -- the STATUS BAR is
     * ({@link #STATUS_SLOTS}). An arrow that changed with the recipe would be a second, competing
     * answer to "did it match", and two of those drift.
     */
    public static final int ARROW_SLOT = 22;

    /**
     * The screen's own icon, row 0 column 4: a crafting table, so the menu says what it is.
     *
     * <p>Decoration, like {@link #ARROW_SLOT}. It is centred over the grid rather than over the
     * whole window because it labels the crafting half, not the suggestion column.
     */
    public static final int INDICATOR_SLOT = 4;

    /**
     * The nine grid slots.
     *
     * <p><b>ITERATION ORDER IS UNSPECIFIED, and a caller that needs one must sort.</b> This is built
     * through a {@code LinkedHashSet} in matrix order and then handed to {@code Set.copyOf}, which
     * returns an immutable set whose iteration order the JDK explicitly does not define -- the
     * insertion order is discarded. An earlier version of this javadoc promised "0..8, not a hash
     * order", which was simply false.
     *
     * <p>It matters because iteration order must never decide which of a player's slots gets drained
     * or filled. {@code MenuRouting} already sorts this through a {@code TreeSet} in four places for
     * exactly that reason -- the shift-click target search, the collect's grid tier, and both
     * empty-slot walks.
     */
    public static final Set<Integer> GRID_SLOTS = gridSlots();

    private static Set<Integer> gridSlots() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int index = 0; index < MATRIX_LENGTH; index++) {
            slots.add(rawSlotForMatrix(index));
        }
        return Set.copyOf(slots);
    }

    private static final int SUGGESTION_COLUMN = 7;
    private static final int FIRST_SUGGESTION_ROW = 1;

    /**
     * How many suggestions the inline column shows. <b>THREE, and the consequence is severe.</b>
     *
     * <p>With thirty definitions claiming a {@code craft_result} and the tier ordering
     * ({@code WEAPON -> ACCESSORY -> TOOL -> ARMOR -> MATERIAL -> VANILLA}), three slots means the
     * shield and two tools. <b>ARMOR NEVER APPEARS HERE. VANILLA NEVER APPEARS HERE.</b>
     *
     * <p>So the browser is not a convenience: <b>it is the only route to anything below tier 2.</b>
     * Shipping the column without it leaves most craftable things unreachable from this menu
     * entirely. That is a scope fact, not a tuning detail, and it is why gate row Q16 expects an
     * all-gear column rather than treating it as a defect.
     */
    public static final int SUGGESTIONS = 3;

    /**
     * The Quick Craft suggestions: column 7, rows 1-3, beside the grid.
     *
     * <p><b>THE MISCLICK REASONING SURVIVED THE MOVE -- it was satisfied, not abandoned.</b> These
     * were originally on row 4 to keep a materials-spending button away from the player's own
     * inventory boundary, which is the edge they cross most often coming up off the hotbar. Column
     * 7 rows 1-3 sits beside the grid at eye level and is FURTHER from that boundary still. The
     * concern that put them low is better served by putting them high.
     *
     * <p><b>Shift-clicking a suggestion cannot move it, and that is already true rather than
     * arranged.</b> {@code MenuRouting.shiftMove} only ever moves an item out of a slot in
     * {@code inputSlots()}, and these are not input slots -- the same reason the result slot needed
     * {@code shiftClickDispatches} to be heard at all.
     *
     * <p><b>Ordered, unlike {@link #GRID_SLOTS}.</b> A {@code List}, because suggestion index N must
     * always render in the same cell -- a ranking whose cells shuffled between recomputes would be
     * unclickable. {@code GRID_SLOTS} is a {@code Set} whose iteration order the JDK leaves
     * undefined, and that difference is the whole reason this is a different type.
     */
    public static final List<Integer> SUGGESTION_SLOTS = suggestionSlots();

    /** The browser button: row 2, column 8, at the foot of the suggestion column it overflows. */
    public static final int BROWSER_SLOT = 26;

    private static List<Integer> suggestionSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int index = 0; index < SUGGESTIONS; index++) {
            slots.add(rawSlotForSuggestion(index));
        }
        return List.copyOf(slots);
    }

    /**
     * The raw slot showing suggestion {@code index}.
     *
     * @param index 0..2, top to bottom.
     */
    public static int rawSlotForSuggestion(int index) {
        if (index < 0 || index >= SUGGESTIONS) {
            throw new IllegalArgumentException(
                    "suggestion index " + index + " is outside 0.." + (SUGGESTIONS - 1));
        }
        return (FIRST_SUGGESTION_ROW + index) * COLUMNS + SUGGESTION_COLUMN;
    }

    /**
     * The suggestion a raw slot shows, or empty for anything that is not a suggestion cell.
     *
     * <p>Empty for the grid, the result, the chrome and the browser button, so a click that is not a
     * suggestion simply is not one -- no bounds check at the call site, the same contract
     * {@link #matrixIndexOf} has.
     */
    public static OptionalInt suggestionIndexOf(int rawSlot) {
        if (rawSlot < 0 || rawSlot >= SIZE) return OptionalInt.empty();
        if (rawSlot % COLUMNS != SUGGESTION_COLUMN) return OptionalInt.empty();

        int index = rawSlot / COLUMNS - FIRST_SUGGESTION_ROW;
        if (index < 0 || index >= SUGGESTIONS) return OptionalInt.empty();

        return OptionalInt.of(index);
    }

    /**
     * The status bar: the bottom row, MINUS the close button that sits in it.
     *
     * <p><b>THE EXCLUSION IS STRUCTURAL, NOT A SKIP.</b> The bar spans row 5, and
     * {@link #CLOSE_SLOT} is slot 49, inside it. Painting over the close button makes the menu
     * unclosable except by Esc -- and <b>Esc works</b>, so the symptom is "the X disappeared", not
     * anything obviously broken.
     *
     * <p>Written as a SET THAT CANNOT CONTAIN IT rather than a {@code continue} inside a 45..53
     * loop, because a {@code continue} is a line someone tidies into a clean range later and the
     * bug it prevents is invisible. A set that never held the slot has nothing to tidy away.
     *
     * <p>Pinned by {@code CraftingMenuLayoutTest} -- a unit witness rather than a boot-gate-only
     * one, because this class is pure and that is the cheapest guard available for it.
     */
    public static final Set<Integer> STATUS_SLOTS = statusSlots();

    private static Set<Integer> statusSlots() {
        Set<Integer> slots = new LinkedHashSet<>();
        int firstOfBottomRow = (ROWS - 1) * COLUMNS;
        for (int slot = firstOfBottomRow; slot < firstOfBottomRow + COLUMNS; slot++) {
            slots.add(slot);
        }
        // SET SUBTRACTION, not a skip inside the loop above. The difference is what survives a
        // later tidy-up: a per-iteration condition reads as noise and invites simplification, while
        // "the row, minus the button" is the whole specification in one line.
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }

    /**
     * The raw slot holding matrix index {@code index}.
     *
     * @param index 0..8, row-major, as the server's matrix is.
     */
    public static int rawSlotForMatrix(int index) {
        if (index < 0 || index >= MATRIX_LENGTH) {
            throw new IllegalArgumentException(
                    "matrix index " + index + " is outside 0.." + (MATRIX_LENGTH - 1));
        }
        int row = index / GRID;
        int column = index % GRID;
        return (FIRST_GRID_ROW + row) * COLUMNS + (FIRST_GRID_COLUMN + column);
    }

    /**
     * The matrix index a raw slot holds, or empty for anything that is not a grid slot.
     *
     * <p>Empty for the chrome, the filler, the result slot, and for every raw slot in the player's
     * own inventory -- so a click that is not a grid cell simply is not one, with no bounds check
     * at the call site.
     */
    public static OptionalInt matrixIndexOf(int rawSlot) {
        if (rawSlot < 0 || rawSlot >= SIZE) return OptionalInt.empty();

        int row = rawSlot / COLUMNS - FIRST_GRID_ROW;
        int column = rawSlot % COLUMNS - FIRST_GRID_COLUMN;
        if (row < 0 || row >= GRID || column < 0 || column >= GRID) return OptionalInt.empty();

        return OptionalInt.of(row * GRID + column);
    }
}
