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

    /** A barrier, bottom-left of the chrome. Same slot the enchant table uses, so the two agree. */
    public static final int CLOSE_SLOT = 0;

    /** The 3x3 block's width and height, and the length of the matrix the server wants. */
    public static final int GRID = 3;
    public static final int MATRIX_LENGTH = GRID * GRID;

    private static final int FIRST_GRID_ROW = 1;
    private static final int FIRST_GRID_COLUMN = 2;

    /**
     * The result. Row 2, column 6 -- vertically centred on the grid and to its right, where the
     * arrow points in the vanilla screen this replaces.
     *
     * <p><b>Not an input slot, and that is load-bearing.</b> {@code Menu.returnEverything} iterates
     * {@code inputSlots()}; a preview listed there would be handed to the player on every close,
     * death, disconnect and shutdown, unpaid for. See {@code CraftingMenu.inputSlots}.
     */
    public static final int RESULT_SLOT = 24;

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

    private static final int SUGGESTION_ROW = 4;

    /** How many suggestions the inline column shows. The rest live in the browser. */
    public static final int SUGGESTIONS = COLUMNS;

    /**
     * The Quick Craft suggestions: the whole of row 4.
     *
     * <p><b>ROW 4, NOT ROW 5, AND THAT IS A SAFETY DECISION RATHER THAN A LOOK.</b> A suggestion is
     * a button that spends materials the instant it is clicked -- no confirmation, no undo. Row 5
     * sits directly above the player's own inventory, which is the boundary they cross most often
     * coming up from the hotbar, so a control that consumes ingredients would be one row of travel
     * from an ordinary misclick. Row 5 is left as chrome deliberately; it is a buffer, not waste.
     *
     * <p><b>Shift-clicking a suggestion cannot move it, and that is already true rather than
     * arranged.</b> {@code MenuRouting.shiftMove} only ever moves an item out of a slot in
     * {@code inputSlots()}, and these are not input slots -- the same reason the result slot needed
     * {@code shiftClickDispatches} to be heard at all. Said here because "why is there an empty row
     * under the suggestions" is exactly the question someone answers by filling it in.
     *
     * <p><b>Ordered, unlike {@link #GRID_SLOTS}.</b> A {@code List}, because suggestion index N must
     * always render in the same cell -- a ranking whose cells shuffled between recomputes would be
     * unclickable. {@code GRID_SLOTS} is a {@code Set} whose iteration order the JDK leaves
     * undefined, and that difference is the whole reason this is a different type.
     */
    public static final List<Integer> SUGGESTION_SLOTS = suggestionSlots();

    /**
     * The browser button, centred in the chrome row below the suggestions.
     *
     * <p>Row 5 column 4. The only functional cell in that row, so the buffer above the player's
     * inventory stays a buffer everywhere it matters.
     */
    public static final int BROWSER_SLOT = (SUGGESTION_ROW + 1) * COLUMNS + 4;

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
     * @param index 0..8, left to right.
     */
    public static int rawSlotForSuggestion(int index) {
        if (index < 0 || index >= SUGGESTIONS) {
            throw new IllegalArgumentException(
                    "suggestion index " + index + " is outside 0.." + (SUGGESTIONS - 1));
        }
        return SUGGESTION_ROW * COLUMNS + index;
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

        int row = rawSlot / COLUMNS;
        if (row != SUGGESTION_ROW) return OptionalInt.empty();

        return OptionalInt.of(rawSlot % COLUMNS);
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
