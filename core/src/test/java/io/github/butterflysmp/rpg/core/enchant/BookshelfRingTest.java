package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bookshelf ring, pinned from every side at once.
 *
 * The count, the skip, the layers and the bounds are each asserted separately because they fail
 * differently and a single one of them can be wrong while the others look right. A skip that is one
 * character out counts the eight cells touching the table; a layer bound that is one out counts the
 * floor or the ceiling; a footprint bound that is one out reaches a block further than a player
 * would ever think to look. None of the four is visible on a server without building a ring and
 * counting it by hand, which is the entire reason this class is offsets and not a nested loop
 * inside a block scan.
 */
class BookshelfRingTest {

    @Test
    void aFullRingOffersThirtyTwoPositions() {
        // DERIVED, not typed -- 5x5 minus 3x3 is 16, twice over is 32. Asserted because a ring that
        // silently came out smaller is indistinguishable from a table with fewer shelves on it.
        assertEquals(32, BookshelfRing.offsets().size());
        assertEquals(BookshelfRing.SIZE, BookshelfRing.offsets().size());
        // And it must exceed the 30 cap, or the cap is unreachable and boot row 13 cannot be run.
        assertTrue(BookshelfRing.offsets().size() > EnchantCost.MAX_POWER,
                "a full ring has to be able to reach the cap, or nothing witnesses it");
        // Mutations RUN, all by ERROR rather than assertion: the class-load self-check throws before
        // any test body runs. skip <= to < -> 48; skip AND to OR -> 8; TOP_LAYER 1 to 2 -> 48;
        // FOOTPRINT_RADIUS 2 to 3 -> 80. Each reads "the bookshelf ring built N positions; it must
        // be 32". Guarding by erroring is stronger here, not weaker -- the message names the defect.
    }

    @Test
    void nothingInTheThreeByThreeCoreIsEverLookedAt() {
        // THE SKIP, on its own. This is the assertion that catches <= against <: with < only the
        // table's own column is skipped and the eight cells touching it start counting, so a table
        // with shelves shoved against it would read 8/30 instead of 0/30.
        int checked = 0;
        for (BookshelfRing.Offset o : BookshelfRing.offsets()) {
            assertFalse(Math.abs(o.dx()) <= 1 && Math.abs(o.dz()) <= 1,
                    "the core cell " + o + " must not be in the ring");
            checked++;
        }
        assertEquals(32, checked, "the property has to have actually run");
    }

    @Test
    void theTableSOwnBlockIsNotInTheRing() {
        // The specific case of the above worth naming: the ring must never look at the table itself.
        assertFalse(BookshelfRing.offsets().contains(new BookshelfRing.Offset(0, 0, 0)));
        assertFalse(BookshelfRing.offsets().contains(new BookshelfRing.Offset(0, 1, 0)),
                "nor the column directly overhead");
    }

    @Test
    void onlyTheTablesOwnLayerAndTheOneAboveItCount() {
        // Not below, and not two up. A shelf on the floor beneath the table is not power, and neither
        // is one on a shelf-height second storey.
        Set<Integer> layers = new HashSet<>();
        for (BookshelfRing.Offset o : BookshelfRing.offsets()) layers.add(o.dy());
        assertEquals(Set.of(0, 1), layers);
        // Mutation, RUN: TOP_LAYER 1 -> 2 -> the self-check fires first, at 48 positions. This
        // assertion is what would catch a layer change that KEPT the count.
    }

    // Mutation, RUN and the reason this file has per-cell assertions at all: shift the dx window by
    // one (-RADIUS+1 .. RADIUS+1). The count stays at EXACTLY 32, so the self-check and every size
    // assertion stay green -- and this test reddens alone: "Offset[dx=3, dy=0, dz=-2] is outside the
    // 5x5 footprint". A ring can be entirely the wrong ring and still be the right size.
    @Test
    void theRingNeverReachesPastTheFiveByFiveFootprint() {
        int checked = 0;
        for (BookshelfRing.Offset o : BookshelfRing.offsets()) {
            assertTrue(Math.abs(o.dx()) <= 2 && Math.abs(o.dz()) <= 2,
                    o + " is outside the 5x5 footprint");
            checked++;
        }
        assertEquals(32, checked, "the property has to have actually run");
    }

    @Test
    void bothLayersOfferTheSameSixteenPositions() {
        // The two layers are the same ring, one above the other. If a bound applied to only one of
        // them the total could still be 32 by accident, which is what this rules out.
        long bottom = BookshelfRing.offsets().stream().filter(o -> o.dy() == 0).count();
        long top = BookshelfRing.offsets().stream().filter(o -> o.dy() == 1).count();
        assertEquals(16, bottom);
        assertEquals(16, top);
    }

    @Test
    void noPositionIsLookedAtTwice() {
        // A duplicate would count one shelf as two power, which reads as a working discount that is
        // simply wrong -- the hardest kind of defect to notice from inside the game.
        Set<BookshelfRing.Offset> seen = new HashSet<>(BookshelfRing.offsets());
        assertEquals(BookshelfRing.offsets().size(), seen.size(), "every offset must be distinct");
    }

    @Test
    void theFourCornersAreInAndTheFourEdgeMidpointsAreToo() {
        // Spot literals, so the ring is pinned by something other than its own arithmetic. Every
        // assertion above reaches the offsets through a rule; these name actual positions, the way
        // EnchantMenuLayout pins its slot literals beside its round trip.
        assertTrue(BookshelfRing.offsets().contains(new BookshelfRing.Offset(2, 0, 2)), "a corner");
        assertTrue(BookshelfRing.offsets().contains(new BookshelfRing.Offset(-2, 1, -2)));
        assertTrue(BookshelfRing.offsets().contains(new BookshelfRing.Offset(2, 0, 0)), "an edge midpoint");
        assertTrue(BookshelfRing.offsets().contains(new BookshelfRing.Offset(0, 1, -2)));
        assertFalse(BookshelfRing.offsets().contains(new BookshelfRing.Offset(1, 0, 1)), "a core cell");
        assertFalse(BookshelfRing.offsets().contains(new BookshelfRing.Offset(3, 0, 0)), "past the footprint");
    }

    @Test
    void theRingCannotBeEditedByWhoeverReadsIt() {
        // It is a shared static. A caller that could add to it could inflate every table on the
        // server at once.
        List<BookshelfRing.Offset> offsets = BookshelfRing.offsets();
        assertThrows(UnsupportedOperationException.class,
                () -> offsets.add(new BookshelfRing.Offset(9, 9, 9)));
    }
}
