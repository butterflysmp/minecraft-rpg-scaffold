package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** THE QUIVER IN THE NEXUS SLOT, decided (the seat's rulings 1, 2 and 5 of 2026-11-22). */
class QuiverMirrorTest {

    @Test
    void aHeldQuiverShowsItsRoundsAsArrows() {
        assertEquals(new QuiverMirror.Shown.Arrows(8), QuiverMirror.decide(true, OptionalInt.of(8)), "a Boltor at 8");
        assertEquals(new QuiverMirror.Shown.Arrows(7), QuiverMirror.decide(true, OptionalInt.of(7)), "after one shot");
        assertEquals(new QuiverMirror.Shown.Arrows(1), QuiverMirror.decide(true, OptionalInt.of(1)), "the last round");
    }

    /** Ruling 2: zero rounds is the star, never a stack of zero; and anything held without a quiver is the star. */
    @Test
    void zeroRoundsAndNoQuiverShowTheStar() {
        assertEquals(new QuiverMirror.Shown.Star(), QuiverMirror.decide(true, OptionalInt.of(0)));
        assertEquals(new QuiverMirror.Shown.Star(), QuiverMirror.decide(true, OptionalInt.empty()), "a sword");
        assertEquals(new QuiverMirror.Shown.Star(), QuiverMirror.decide(true, null), "nothing in hand");
    }

    /** Ruling 2: more rounds than a stack can show shows the limit (99); the weapon's own count is not touched here. */
    @Test
    void moreRoundsThanAStackShowsTheLimit() {
        assertEquals(new QuiverMirror.Shown.Arrows(99), QuiverMirror.decide(true, OptionalInt.of(99)));
        assertEquals(new QuiverMirror.Shown.Arrows(99), QuiverMirror.decide(true, OptionalInt.of(150)));
        assertEquals(99, QuiverMirror.MAX_SHOWN, "READ: an item's max stack size is 1 to 99 in the pinned jar");
    }

    /** Ruling 5: the star off, nothing is drawn: there is no Nexus item to change. */
    @Test
    void theStarOffShowsNothing() {
        assertEquals(new QuiverMirror.Shown.None(), QuiverMirror.decide(false, OptionalInt.of(8)));
        assertEquals(new QuiverMirror.Shown.None(), QuiverMirror.decide(false, OptionalInt.empty()));
    }

    /** Ruling 5: the warning, only with BOTH a quiver weapon and the star off, and once per event. */
    @Test
    void theWarningIsSentOncePerEventAndOnlyWhenBothHold() {
        // The three events, each warning once:
        assertTrue(QuiverMirror.warnNow(null, null, true, false), "JOIN: the first observation, star off, a quiver weapon");
        assertTrue(QuiverMirror.warnNow(true, true, true, false), "he TURNED THE STAR OFF while carrying one");
        assertTrue(QuiverMirror.warnNow(false, false, true, false), "a quiver weapon first REACHED his inventory");
        // ...and never again for the same state:
        assertFalse(QuiverMirror.warnNow(true, false, true, false), "still off, still carrying one: not again");
        // Never without BOTH:
        assertFalse(QuiverMirror.warnNow(null, null, false, false), "no quiver weapon: no warning");
        assertFalse(QuiverMirror.warnNow(null, null, true, true), "the star on: no warning");
        assertFalse(QuiverMirror.warnNow(false, true, true, true), "the star on: no warning, even on arrival");
        assertFalse(QuiverMirror.warnNow(true, true, false, false), "the star off but the weapon gone: no warning");
    }
}
