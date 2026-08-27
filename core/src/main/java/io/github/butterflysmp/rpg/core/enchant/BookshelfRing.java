package io.github.butterflysmp.rpg.core.enchant;

import java.util.ArrayList;
import java.util.List;

/**
 * Which blocks around an enchanting table are looked at when counting bookshelf power.
 *
 * <p>The outer ring of a 5x5 footprint, at the table's own Y and one layer above it: 16 positions
 * per layer, 32 in all. The inner 3x3 is skipped at both layers, which excludes the table itself,
 * the eight cells touching it, and the column directly overhead.
 *
 * <p><b>Offsets only. No world, no block, no material.</b> The half of a bookshelf count that can be
 * got wrong is the geometry -- one {@code <=} against a {@code <} in the skip and the eight cells
 * touching the table start counting, which is invisible on a server until someone builds a ring and
 * counts it by hand. That half is pure arithmetic and belongs in the two-second loop. The half that
 * genuinely needs a live world -- reading a block and comparing its material -- is three lines in
 * {@code BookshelfPower}, and is all the boot gate is left owing. Same split as {@code EnchantRoll},
 * whose draw stays in paper while its decisions do not.
 *
 * <p>32 is DERIVED, not typed: the count falls out of the bounds and the skip. It is asserted anyway,
 * because a scan that quietly finds fewer positions than it should looks exactly like a table with
 * fewer shelves on it.
 *
 * <p><b>No air-gap rule.</b> Vanilla requires the block between table and shelf to be transparent;
 * this does not, so a shelf walled in behind stone still counts. Deliberate -- it halves the reads,
 * it drops a rule players already find opaque, and it makes a full ring something a gate can
 * actually build. If it is ever wanted, it is one occlusion check on the midpoint of an offset,
 * which is a reason for offsets to be a first-class thing rather than a nested loop.
 */
public final class BookshelfRing {

    private BookshelfRing() {}

    /** Half-width of the footprint: dx and dz run -2..2, which is the 5x5. */
    private static final int FOOTPRINT_RADIUS = 2;

    /** Everything within this of the table is skipped -- the 3x3 core, at both layers. */
    private static final int INNER_RADIUS = 1;

    /** The table's own layer and the one above it. Not below, and not two up. */
    private static final int TOP_LAYER = 1;

    /** How many positions a full ring offers. Two more than the cap, so the cap is reachable. */
    public static final int SIZE = 32;

    /**
     * One position to look at, relative to the table.
     *
     * <p>A record rather than an {@code int[]} for the reason {@code EnchantRoll.Rollable} is one:
     * it is immutable, and a test reads {@code o.dx()} instead of {@code o[0]}.
     */
    public record Offset(int dx, int dy, int dz) {}

    private static final List<Offset> OFFSETS = build();

    /** Every position, in a fixed order. Unmodifiable; built once. */
    public static List<Offset> offsets() {
        return OFFSETS;
    }

    private static List<Offset> build() {
        List<Offset> offsets = new ArrayList<>();
        for (int dy = 0; dy <= TOP_LAYER; dy++) {
            for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS; dx++) {
                for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS; dz++) {
                    // THE SKIP. Both bounds, and AND rather than OR: a cell is in the core only when
                    // it is close on BOTH axes. With OR this would keep only the four corners.
                    if (Math.abs(dx) <= INNER_RADIUS && Math.abs(dz) <= INNER_RADIUS) continue;
                    offsets.add(new Offset(dx, dy, dz));
                }
            }
        }
        if (offsets.size() != SIZE) {
            // A ring that came out the wrong size is a defect, not a smaller ring. Thrown at class
            // load, where it stops the plugin, rather than discovered as a table that discounts less
            // than it should -- which no player would report as a bug.
            throw new IllegalStateException("the bookshelf ring built " + offsets.size()
                    + " positions; it must be " + SIZE);
        }
        return List.copyOf(offsets);
    }
}
