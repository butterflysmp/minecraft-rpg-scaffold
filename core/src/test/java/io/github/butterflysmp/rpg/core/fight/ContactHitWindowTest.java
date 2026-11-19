package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9b, fix A (the seat's brief of 2026-11-19): ONE CONTACT HIT PER WINDOW. A player takes at most one priced contact hit
 * (bite or wing, together) from the fight's dragon per {@code contact_hit_interval_ticks}. The first boot's RA3: two
 * bites a tick for four ticks, 727 against his 350.
 *
 * <p>READ, the pinned server jar, why the bite landed twice a tick: {@code EnderDragon.aiStep} calls {@code hurt} for
 * the head's box and again for the neck's every tick the dragon is not flinching, and {@code LivingEntity.hurtServer}'s
 * invulnerability window refuses a hit only if it is no greater than {@code lastHurt}, which it then sets from the event's
 * FINAL damage: our rider's 0.01 token. So every later 10 passed. The window here holds whichever way they arrive.
 */
class ContactHitWindowTest {

    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Test
    void theFirstHitLandsAndTheSecondInTheSameTickDoesNot() {
        ContactHitWindow w = new ContactHitWindow(20);
        assertTrue(w.hit(BEN, 31).lands());
        assertFalse(w.hit(BEN, 31).lands(), "the neck's call in the head's tick");
    }

    @Test
    void nothingLandsUntilTheWindowHasPassedAndThenOneDoes() {
        ContactHitWindow w = new ContactHitWindow(20);
        assertTrue(w.hit(BEN, 100).lands());
        assertFalse(w.hit(BEN, 101).lands());
        assertFalse(w.hit(BEN, 119).lands(), "19 ticks on: still inside");
        assertTrue(w.hit(BEN, 120).lands(), "20 ticks on: the window has passed");
    }

    @Test
    void theNextLandingHitReportsHowManyTheWindowBefore() {
        ContactHitWindow w = new ContactHitWindow(20);
        w.hit(BEN, 10);
        w.hit(BEN, 10);
        w.hit(BEN, 11);
        w.hit(BEN, 12);
        ContactHitWindow.Result next = w.hit(BEN, 40);
        assertTrue(next.lands());
        assertEquals(3, next.suppressedBefore(), "three suppressed in the window that opened at 10");
        assertEquals(10, next.windowFrom());
        assertEquals(0, w.hit(BEN, 60).suppressedBefore(), "and the next window had none");
    }

    @Test
    void eachPlayerHasHisOwnWindow() {
        ContactHitWindow w = new ContactHitWindow(20);
        assertTrue(w.hit(BEN, 50).lands());
        assertTrue(w.hit(OTHER, 50).lands(), "another player's window is not his");
    }

    @Test
    void whatIsStillOpenCanBeFlushedAtTheFightsEnd() {
        ContactHitWindow w = new ContactHitWindow(20);
        w.hit(BEN, 5);
        w.hit(BEN, 6);
        w.hit(BEN, 7);
        assertEquals(2, w.flush(BEN).suppressedBefore());
        assertEquals(0, w.flush(BEN).suppressedBefore(), "flushed once");
    }

    @Test
    void aWindowOfLessThanOneTickIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ContactHitWindow(0));
    }
}
