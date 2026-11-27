package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S P2 AND F2 (2026-11-27): on the pad, every world-touching cleanup is bounded to THE PAD'S OWN REGION, never a
 * 48-block sphere: its shell (the interior x -2..45, z -13..13, and the one-block rim round it, so x -3..46, z -14..14)
 * and, in height, one block below the floor to {@value SelfTestPadShape#HEADROOM} above it (the headroom the rows use:
 * CC'S CALL). An item, a projectile, a cloud or a work chain outside it is never touched, whatever its distance.
 */
class SelfTestPadRegionTest {

    private static final SelfTestPadShape.Stand S = new SelfTestPadShape.Stand(104, 200, -4);

    /** Mutation: any bound moved by one, or the region widened back to a sphere. */
    @Test
    void theRegionIsTheShellAndTheHeadroom_inclusive() {
        assertTrue(SelfTestPadShape.inRegion(0, 1, 0), "the stand");
        assertTrue(SelfTestPadShape.inRegion(-3, 0, 0) && SelfTestPadShape.inRegion(46, 0, 0), "the rim's x, both ends");
        assertFalse(SelfTestPadShape.inRegion(-4, 0, 0) || SelfTestPadShape.inRegion(47, 0, 0), "one past the rim");
        assertTrue(SelfTestPadShape.inRegion(0, 0, -14) && SelfTestPadShape.inRegion(0, 0, 14), "the rim's z");
        assertFalse(SelfTestPadShape.inRegion(0, 0, -15) || SelfTestPadShape.inRegion(0, 0, 15), "one past it");
        assertTrue(SelfTestPadShape.inRegion(0, -1, 0) && SelfTestPadShape.inRegion(0, SelfTestPadShape.HEADROOM, 0),
                "one below the floor to the headroom");
        assertFalse(SelfTestPadShape.inRegion(0, -2, 0) || SelfTestPadShape.inRegion(0, SelfTestPadShape.HEADROOM + 1, 0),
                "beyond either");
        assertEquals(16, SelfTestPadShape.HEADROOM);
    }

    /** P6: things outside the box but within 48 of the stand are untouched. Mutation: the sphere put back. */
    @Test
    void aPointWithin48OfTheStandButOutsideTheBox_isNotInIt() {
        // the stand's block centre in world coordinates: (104.5, 201, -3.5)
        assertTrue(SelfTestPadShape.inRegion(S, 104.5, 201.0, -3.5));
        assertFalse(SelfTestPadShape.inRegion(S, 104.5 - 10, 201.0, -3.5), "10 west of the stand: 10 blocks, outside the box");
        assertFalse(SelfTestPadShape.inRegion(S, 104.5, 201.0, -3.5 + 20), "20 south");
        assertFalse(SelfTestPadShape.inRegion(S, 104.5, 201.0 + 30, -3.5), "30 up");
        assertTrue(SelfTestPadShape.inRegion(S, 104 + 46.99, 200.0, -4 - 13.99), "the rim's far corner, fractional");
        assertFalse(SelfTestPadShape.inRegion(S, 104 + 47.0, 200.0, -4.0), "one block past it");
    }

    @Test
    void theLinesNameTheBound() {
        assertEquals("ITEMS cleared=0 pad []", SelfTestStaging.itemsClearedLine(List.of()));
        assertEquals("ITEMS cleared=2 pad [bone x1, rotten_flesh x1]",
                SelfTestStaging.itemsClearedLine(List.of("rotten_flesh", "bone")));
        assertEquals("LINGER work=0 entities=0 pad", SelfTestStaging.lingerLine(0, List.of()));
        assertEquals("LINGER work=3 entities=2 pad [arrow 1, item 2]", SelfTestStaging.lingerLine(3, List.of("arrow 1", "item 2")));
        assertEquals("LINGER skipped", SelfTestStaging.LINGER_SKIPPED, "P1: before a pad step nothing in the world is touched");
    }

    /** P3: an XP orb is removed at the run's end only on the pad and inside its box; a tagged mob as before. */
    @Test
    void theRestoreRemovesAnOrbOnlyOnThePad() {
        assertTrue(SelfTestSnapshot.removedOnRestore(false, false, true, true), "an orb on the pad");
        assertFalse(SelfTestSnapshot.removedOnRestore(false, false, true, false), "an orb on his ground, or outside the box");
        assertTrue(SelfTestSnapshot.removedOnRestore(false, true, false, false), "a selftest-tagged mob: tag-bound, as before");
        assertFalse(SelfTestSnapshot.removedOnRestore(true, true, true, true), "never a player");
        assertFalse(SelfTestSnapshot.removedOnRestore(false, false, false, true), "anything else: never");
    }
}
