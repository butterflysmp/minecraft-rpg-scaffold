package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAN-build-system.md section 7.6: immune from the leap to the first landing after it, and only that one. */
class SafeLandingTest {

    /** Steps a fresh mark through (onGround, fallDistance) pairs, one per tick. */
    private static SafeLanding run(Object... pairs) {
        SafeLanding s = SafeLanding.armed();
        for (int i = 0; i < pairs.length; i += 2) s = s.step((Boolean) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        return s;
    }

    @Test
    void armedIsImmune() {
        assertTrue(SafeLanding.armed().immune());
    }

    @Test
    void immuneThroughTheAscentAndTheFall() {
        assertTrue(run(true, 0, false, 0, false, 0, false, 1.5, false, 3.2).immune());
    }

    /** The landing tick is still immune: the FALL event and our reading of the ground are in an untraced order. */
    @Test
    void theLandingTickIsStillImmune() {
        assertTrue(run(false, 0, false, 2.0, true, 0).immune());
    }

    /** THE "CLEARED ON LANDING" ROW: the tick after the first ground contact, a later fall is an ordinary fall. */
    @Test
    void clearedTheTickAfterTheLanding() {
        assertFalse(run(false, 0, false, 2.0, true, 0, true, 0).immune());
    }

    /** Water, a ladder, a cobweb: vanilla zeroes the fall distance mid-air, so there is no landing to protect. */
    @Test
    void clearedWhenTheFallIsAbsorbedWithoutALanding() {
        assertFalse(run(false, 0, false, 2.0, false, 0).immune());
    }

    /** The ascent has a fall distance of 0 too; only a fall that STARTED and was then absorbed clears. */
    @Test
    void theAscentsZeroFallDistanceDoesNotClear() {
        assertTrue(run(false, 0, false, 0, false, 0).immune());
    }

    /** A leap that never leaves the ground (a ceiling) does not keep a mark for a later fall. */
    @Test
    void clearedIfItNeverTakesOff() {
        SafeLanding s = SafeLanding.armed();
        for (int i = 0; i <= SafeLanding.TAKE_OFF_TICKS; i++) s = s.step(true, 0);
        assertFalse(s.immune());
    }

    /** The backstop: a mark nothing else cleared (gliding, flying) cannot eat a fall minutes later. */
    @Test
    void clearedByTheBackstop() {
        SafeLanding s = SafeLanding.armed().step(false, 0);
        for (int i = 1; i < SafeLanding.BACKSTOP_TICKS; i++) s = s.step(false, 0);
        assertTrue(s.immune(), "still airborne on the backstop's last tick");
        assertFalse(s.step(false, 0).immune());
    }

    @Test
    void aClearedMarkStaysCleared() {
        SafeLanding s = run(false, 0, false, 2.0, true, 0, true, 0);
        assertFalse(s.step(false, 0).immune());
        assertTrue(s.cleared());
    }
}
