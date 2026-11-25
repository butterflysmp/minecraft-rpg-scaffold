package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Q2 (the seat, 2026-11-25): the quiver's percent term, combined with the flat one in ONE place, HALF-UP (Ben: ".5s
 * round up"). The seat's pins, every one executed here rather than predicted; the existing {@code QuiverSizeTest} is
 * untouched and is the "at percent 0 every existing result is unchanged" half.
 */
class QuiverSizePercentTest {

    @Test
    void theSeatsPinsAtFiftyPercent() {
        assertEquals(3, QuiverSize.resolve(2, 0, 50), "2 -> 3 (the Breacher)");
        assertEquals(8, QuiverSize.resolve(5, 0, 50), "5 -> 8 (7.5 up; Dragon's Breath)");
        assertEquals(12, QuiverSize.resolve(8, 0, 50), "8 -> 12 (the Boltor)");
        assertEquals(8, QuiverSize.resolve(2, 3, 50), "2 with +3 flat -> 8 (7.5 up): percent AFTER flat");
        assertEquals(5, QuiverSize.resolve(3, 0, 50), "3 -> 5 (4.5 up)");
        assertEquals(2, QuiverSize.resolve(1, 0, 50), "1 -> 2 (1.5 up)");
        // Mutation: ceil instead of half-up -> no pin moves at 50% (every value is whole or a half), see below.
    }

    @Test
    void halfUpIsNotCeiling() {
        // At 10%, 3 x 1.1 = 3.3: half-up gives 3, a ceiling 4. At 30%, 7 x 1.3 = 9.1: 9 against 10.
        assertEquals(3, QuiverSize.resolve(3, 0, 10));
        assertEquals(9, QuiverSize.resolve(7, 0, 30));
        // And x.4 goes down, x.5 up: 8 x 1.3 = 10.4 -> 10; 5 x 1.3 = 6.5 -> 7.
        assertEquals(10, QuiverSize.resolve(8, 0, 30));
        assertEquals(7, QuiverSize.resolve(5, 0, 30));
        // Mutation: Math.ceil -> 4, 10, 11 -> reddens.
    }

    @Test
    void percentZeroIsTheAdditiveResolveExactly() {
        for (int authored = 1; authored <= 64; authored++) {
            for (int flat = -70; flat <= 30; flat++) {
                assertEquals(Math.max(QuiverSize.MIN_CAPACITY, authored + flat), QuiverSize.resolve(authored, flat, 0),
                        authored + " + " + flat);
                assertEquals(QuiverSize.resolve(authored, flat), QuiverSize.resolve(authored, flat, 0));
            }
        }
    }

    @Test
    void theFloorStillHolds() {
        assertEquals(QuiverSize.MIN_CAPACITY, QuiverSize.resolve(2, -10, 50));
    }
}
