package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9, THE DRAGON'S PHASES (the F9 brief, section 3 f; the SEAT MECHANISM RULING: crossings QUEUE). Ben's thresholds,
 * 80, 60, 40 and 20 percent of the dragon's maximum. A crossing is a fraction STRICTLY BELOW a threshold. Every
 * crossing is one Mage; one hit across two thresholds is two Mages, one after the other; the next lands only when no
 * Mage is alive and the ring is home (the cycle is IDLE).
 */
class DragonCrossingsTest {

    private static final List<Double> BENS = List.of(0.80, 0.60, 0.40, 0.20);

    @Test
    void aHitThatStaysAtOrAboveEightyCrossesNothing() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertEquals(List.of(), c.observe(0.95));
        assertEquals(List.of(), c.observe(0.80), "exactly 80 percent is not below it");
        assertEquals(0, c.pending());
        assertFalse(c.due(true));
    }

    @Test
    void aHitBelowEightyQueuesOneMageWhoIsDueWhenIdle() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertEquals(List.of(0.80), c.observe(0.7999));
        assertEquals(1, c.pending());
        assertFalse(c.due(false), "not while a Mage is alive or the ring is away");
        assertTrue(c.due(true));
        c.landed();
        assertEquals(0, c.pending());
        assertFalse(c.due(true), "one crossing, one Mage");
    }

    @Test
    void oneHitAcrossTwoThresholdsQueuesTwoMagesThatLandOneAfterTheOther() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertEquals(List.of(0.80, 0.60), c.observe(0.55));
        assertEquals(2, c.pending());
        assertTrue(c.due(true));
        c.landed();
        // The first Mage is alive: the cycle is not IDLE, so the second waits.
        assertFalse(c.due(false));
        assertEquals(1, c.pending());
        // He died and the ring came home: the second is due.
        assertTrue(c.due(true));
        c.landed();
        assertEquals(0, c.pending());
    }

    @Test
    void aThresholdIsCrossedOnceEvenIfTheDragonHealsAboveItAndFallsAgain() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertEquals(List.of(0.80), c.observe(0.75));
        assertEquals(List.of(), c.observe(0.90), "a heal un-crosses nothing");
        assertEquals(List.of(), c.observe(0.75), "and the fall again is not a second crossing");
        assertEquals(1, c.crossed());
    }

    @Test
    void allFourInOneHitQueueFourAndNothingMoreEverCrosses() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertEquals(BENS, c.observe(0.0));
        assertEquals(4, c.pending());
        assertEquals(List.of(), c.observe(0.0));
        for (int i = 0; i < 4; i++) {
            assertTrue(c.due(true));
            c.landed();
        }
        assertFalse(c.due(true));
    }

    @Test
    void aRefusedLandingStaysQueued() {
        DragonCrossings c = new DragonCrossings(BENS);
        c.observe(0.5);
        assertTrue(c.due(true));
        // The caller found nowhere to stand and did not call landed(): the crossing is still owed.
        assertTrue(c.due(true));
        assertEquals(2, c.pending());
    }

    @Test
    void landedWithNothingQueuedIsRefused() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertThrows(IllegalStateException.class, c::landed);
    }

    @Test
    void thresholdsMustFallStrictlyAndLieStrictlyBetweenZeroAndOne() {
        assertThrows(IllegalArgumentException.class, () -> new DragonCrossings(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DragonCrossings(List.of(0.6, 0.8)));
        assertThrows(IllegalArgumentException.class, () -> new DragonCrossings(List.of(0.8, 0.8)));
        assertThrows(IllegalArgumentException.class, () -> new DragonCrossings(List.of(1.0)));
        assertThrows(IllegalArgumentException.class, () -> new DragonCrossings(List.of(0.0)));
        assertThrows(IllegalArgumentException.class, () -> new DragonCrossings(List.of(Double.NaN)));
    }

    @Test
    void aFractionThatIsNotANumberIsRefused() {
        DragonCrossings c = new DragonCrossings(BENS);
        assertThrows(IllegalArgumentException.class, () -> c.observe(Double.NaN));
    }
}
