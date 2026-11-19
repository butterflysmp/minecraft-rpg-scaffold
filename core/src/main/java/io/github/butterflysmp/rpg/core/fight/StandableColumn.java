package io.github.butterflysmp.rpg.core.fight;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * F6: WHERE IN ONE COLUMN THE MAGE CAN STAND. The highest block within {@code band} of C's y that is solid with
 * {@code clearance} non-solid blocks above it, or none. Pure: the paper side supplies "is the block at this y solid".
 */
public final class StandableColumn {

    private StandableColumn() {}

    /**
     * @param centreY   C's block y
     * @param band      search {@code centreY - band} to {@code centreY + band}, both included
     * @param clearance how many blocks above the floor must be free
     * @param solid     whether the block at a y in this column is solid
     * @return the floor block's y; he stands on {@code y + 1}
     */
    public static OptionalInt highest(int centreY, int band, int clearance, IntPredicate solid) {
        for (int y = centreY + band; y >= centreY - band; y--) {
            if (!solid.test(y)) continue;
            boolean clear = true;
            for (int h = 1; h <= clearance; h++) {
                if (solid.test(y + h)) {
                    clear = false;
                    break;
                }
            }
            if (clear) return OptionalInt.of(y);
        }
        return OptionalInt.empty();
    }
}
