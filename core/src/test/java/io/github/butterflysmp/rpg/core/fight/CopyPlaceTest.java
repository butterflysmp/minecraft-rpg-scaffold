package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9e b: WHERE THE COPY LIES (Ben: "The copy should be placed just inside the ring in both cases"). Where he died if
 * that is at least one block inside the ring; otherwise the nearest point one block inside the edge. With the ring
 * closed no place is inside: the copy stays and cannot be revived. On every ring tick a copy the edge would pass is
 * moved to stay one block inside it (the SEAT's mechanism reading).
 */
class CopyPlaceTest {

    private static final double E = 1e-9;

    private static void at(CopyPlace.Place p, double x, double z) {
        assertEquals(x, p.xBlocks(), E, "x");
        assertEquals(z, p.zBlocks(), E, "z");
    }

    @Test
    void aDeathInsideTheRingLiesWhereHeDied() {
        Ring r = new Ring(0, 0, 20);
        CopyPlace.Place p = CopyPlace.place(5, -7, r);
        at(p, 5, -7);
        assertTrue(p.revivable());
        assertFalse(p.moved());
    }

    @Test
    void exactlyOneBlockInsideIsInsideAndHalfABlockIsNot() {
        Ring r = new Ring(0, 0, 20);
        at(CopyPlace.place(19, 0, r), 19, 0);
        assertFalse(CopyPlace.place(19, 0, r).moved(), "one block inside: where he died");
        at(CopyPlace.place(19.5, 0, r), 19, 0);
        assertTrue(CopyPlace.place(19.5, 0, r).moved(), "half a block inside: moved to one block inside");
    }

    /** Died outside the ring (or in the void beyond the disc): the nearest point one block inside the edge. */
    @Test
    void aDeathOutsideLiesOneBlockInsideTheEdgeOnTheLineToTheCentre() {
        Ring r = new Ring(10, 10, 20);
        CopyPlace.Place p = CopyPlace.place(10 + 60, 10, r);
        at(p, 10 + 19, 10);
        assertTrue(p.moved());
        CopyPlace.Place q = CopyPlace.place(10 + 30, 10 + 40, r); // distance 50 along (3, 4)
        at(q, 10 + 19 * 0.6, 10 + 19 * 0.8);
        assertEquals(19.0, Math.hypot(q.xBlocks() - 10, q.zBlocks() - 10), E, "one block inside the edge");
    }

    @Test
    void aClosingRingCarriesTheCopyInWithItsEdge() {
        double x = 15, z = 0;
        for (double radius = 20; radius >= 2; radius -= 1.5) {
            CopyPlace.Place p = CopyPlace.place(x, z, new Ring(0, 0, radius));
            assertTrue(Math.hypot(p.xBlocks(), p.zBlocks()) <= radius - 1 + E, "one block inside at r=" + radius);
            x = p.xBlocks();
            z = p.zBlocks();
        }
        at(new CopyPlace.Place(x, z, true, false), 1, 0);
    }

    @Test
    void anExpandingRingLeavesTheCopyWhereItLies() {
        CopyPlace.Place p = CopyPlace.place(9, 0, new Ring(0, 0, 10));
        at(p, 9, 0);
        for (double radius = 11; radius <= 30; radius += 2) {
            CopyPlace.Place q = CopyPlace.place(p.xBlocks(), p.zBlocks(), new Ring(0, 0, radius));
            at(q, 9, 0);
            assertFalse(q.moved(), "an expanding ring never moves it, r=" + radius);
        }
    }

    /** A moving ring (the Mage's ring follows him): the copy is pushed ahead of the edge that would pass it. */
    @Test
    void aMovingRingPushesTheCopyAheadOfItsEdge() {
        CopyPlace.Place p = CopyPlace.place(-10, 0, new Ring(0, 0, 12));
        at(p, -10, 0);
        CopyPlace.Place q = CopyPlace.place(p.xBlocks(), p.zBlocks(), new Ring(5, 0, 12)); // centre moves east by 5
        at(q, 5 - 11, 0);
        assertTrue(q.moved());
        CopyPlace.Place s = CopyPlace.place(q.xBlocks(), q.zBlocks(), new Ring(5, 3, 12)); // then north a little
        assertTrue(Math.hypot(s.xBlocks() - 5, s.zBlocks() - 3) <= 11 + E);
    }

    /** With the ring CLOSED (or too small to hold a point one block inside), the copy stays and cannot be revived. */
    @Test
    void aClosedRingLeavesTheCopyWhereItIsAndUnrevivable() {
        CopyPlace.Place closed = CopyPlace.place(4, 2, new Ring(0, 0, 0));
        at(closed, 4, 2);
        assertFalse(closed.revivable());
        assertFalse(closed.moved());
        CopyPlace.Place tiny = CopyPlace.place(4, 2, new Ring(0, 0, 1.0));
        assertFalse(tiny.revivable(), "radius 1 holds no point one block inside");
        assertTrue(CopyPlace.place(0, 0, new Ring(0, 0, 1.5)).revivable(), "radius 1.5: the centre is half a block in");
    }

    @Test
    void aDeathAtTheCentreOfARingStaysThere() {
        at(CopyPlace.place(3, 3, new Ring(3, 3, 5)), 3, 3);
    }
}
