package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
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

    /** The nine grid slots, in matrix order. Iteration order is 0..8, not a hash order. */
    public static final Set<Integer> GRID_SLOTS = gridSlots();

    private static Set<Integer> gridSlots() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int index = 0; index < MATRIX_LENGTH; index++) {
            slots.add(rawSlotForMatrix(index));
        }
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
