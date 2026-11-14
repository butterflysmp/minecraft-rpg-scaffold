package io.github.butterflysmp.rpg.core.hud;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who owns a player's action bar at tick t (the seat's mechanism rulings 1-3, 2026-11-14, for Ben's
 * "Is it possible to have the pickaxe message over ride the action bar for 1.5s").
 *
 * <p>Staging: the hold starts at 100 for 30 ticks, so it ends at 130. No two quantities a row reads
 * are equal (start 100, until 130, a replacing hold at 117 ending 147), so a start/until or an
 * old/new swap has nowhere to hide.
 */
class ActionBarHoldsTest {

    private static final UUID BEN = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);

    private final ActionBarHolds<String> holds = new ActionBarHolds<>();

    @Test
    void aHoldOwnsTheBarUpToButNotIncludingItsEndTick() {
        ActionBarHolds.Hold<String> h = holds.hold(BEN, "DENIED", "denied", 100, 30, true);
        assertEquals(100L, h.start());
        assertEquals(130L, h.until(), "start + ticks");

        assertEquals("DENIED", assertInstanceOf(ActionBarHolds.Beat.Held.class, holds.beat(BEN, 100)).hold().message());
        assertInstanceOf(ActionBarHolds.Beat.Held.class, holds.beat(BEN, 129), "one tick before the end: still held");
        ActionBarHolds.Beat<String> atEnd = holds.beat(BEN, 130);
        assertEquals(130L, assertInstanceOf(ActionBarHolds.Beat.Released.class, atEnd).hold().until(),
                "AT the end tick the stats line is drawn again, and the end is reported once");
        assertInstanceOf(ActionBarHolds.Beat.Stats.class, holds.beat(BEN, 131), "released once, then plain stats");
        // Mutation: '<' -> '<=' in beat -> still Held at 130 -> reddens.
    }

    @Test
    void theFirstBeatAfterTheEndReleasesItEvenIfNoBeatLandedExactlyOnIt() {
        // The bar beats every 10 ticks at its own phase, so the first beat at or after T may be later.
        holds.hold(BEN, "DENIED", "denied", 100, 30, false);
        assertInstanceOf(ActionBarHolds.Beat.Held.class, holds.beat(BEN, 125));
        assertEquals(130L, assertInstanceOf(ActionBarHolds.Beat.Released.class, holds.beat(BEN, 135)).hold().until());
    }

    @Test
    void aNewerHoldReplacesTheOlderOne() {
        holds.hold(BEN, "first", "denied", 100, 30, true);
        ActionBarHolds.Hold<String> second = holds.hold(BEN, "second", "denied", 117, 30, true);
        assertEquals(147L, second.until(), "the hold restarts from the newer click");

        ActionBarHolds.Beat<String> at135 = holds.beat(BEN, 135);
        assertEquals("second", assertInstanceOf(ActionBarHolds.Beat.Held.class, at135).hold().message(),
                "past the first hold's end (130) the newer one still owns the bar");
        assertInstanceOf(ActionBarHolds.Beat.Held.class, holds.beat(BEN, 146));
        assertEquals(147L, assertInstanceOf(ActionBarHolds.Beat.Released.class, holds.beat(BEN, 147)).hold().until());
        // Mutation: hold() keeps the older hold (putIfAbsent) -> released at 135 -> reddens.
    }

    @Test
    void aQuitEndsTheHold() {
        holds.hold(BEN, "DENIED", "denied", 100, 30, true);
        holds.clear(BEN);
        assertInstanceOf(ActionBarHolds.Beat.Stats.class, holds.beat(BEN, 110), "nothing is held after a quit");
        // Mutation: clear() does nothing -> Held at 110 -> reddens.
    }

    @Test
    void playersAreIndependentAndNoHoldMeansStats() {
        holds.hold(BEN, "DENIED", "denied", 100, 30, true);
        assertInstanceOf(ActionBarHolds.Beat.Stats.class, holds.beat(OTHER, 110));
        assertInstanceOf(ActionBarHolds.Beat.Held.class, holds.beat(BEN, 110));
    }

    @Test
    void aHoldOfNoTicksIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> holds.hold(BEN, "x", "denied", 100, 0, true));
        assertThrows(IllegalArgumentException.class, () -> holds.hold(BEN, "x", "denied", 100, -1, true));
    }

    // --- S2: is a hold LIVE at tick t (the seat's ruling 8: the DENIED sound plays only when none is) --

    @Test
    void aHoldIsLiveFromItsStartUpToButNotIncludingItsEnd() {
        assertFalse(holds.isLive(BEN, 100), "no hold");
        holds.hold(BEN, "DENIED", "denied", 100, 30, true);
        assertTrue(holds.isLive(BEN, 100), "the start tick");
        assertTrue(holds.isLive(BEN, 129), "one tick before the end");
        assertFalse(holds.isLive(BEN, 130), "AT the end tick it is over, even before a beat releases it");
        assertFalse(holds.isLive(OTHER, 110), "per player");
        // Mutation: '<' -> '<=' in isLive -> live at 130 -> reddens.
    }

    @Test
    void aReleasedOrClearedHoldIsNotLive() {
        holds.hold(BEN, "DENIED", "denied", 100, 30, true);
        holds.beat(BEN, 135);
        assertFalse(holds.isLive(BEN, 120), "released: gone, whatever tick is asked about");
        holds.hold(BEN, "DENIED", "denied", 200, 30, true);
        holds.clear(BEN);
        assertFalse(holds.isLive(BEN, 210));
    }

    @Test
    void theEndLineIsPinned() {
        ActionBarHolds.Hold<String> h = holds.hold(BEN, "DENIED", "denied", 100, 30, true);
        assertEquals("HUD hold-end player=Ben reason=denied start=100 until=130 tick=134",
                ActionBarHolds.endLine("Ben", h, 134));
    }
}
