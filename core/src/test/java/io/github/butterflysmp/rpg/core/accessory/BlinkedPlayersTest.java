package io.github.butterflysmp.rpg.core.accessory;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** K4: the blinked state is one per player, ends once, and the expiry ends only the blink it was scheduled for. */
class BlinkedPlayersTest {

    private static final Blinks.InvisibilityAtBlink NONE = new Blinks.InvisibilityAtBlink(true, Optional.empty());

    @Test
    void beginEndOnce() {
        BlinkedPlayers b = new BlinkedPlayers();
        UUID him = UUID.randomUUID();
        assertFalse(b.isBlinked(him));
        assertFalse(b.isBlinked(null), "nobody is not blinked");
        b.begin(him, new BlinkedPlayers.State(100, 260, NONE));
        assertTrue(b.isBlinked(him));
        assertEquals(260, b.state(him).orElseThrow().endTick());
        assertTrue(b.end(him).isPresent());
        assertTrue(b.end(him).isEmpty(), "a second end finds nothing: ended once");
        assertFalse(b.isBlinked(him));
    }

    @Test
    void theExpiryEndsOnlyItsOwnBlink() {
        BlinkedPlayers b = new BlinkedPlayers();
        UUID him = UUID.randomUUID();
        b.begin(him, new BlinkedPlayers.State(100, 260, NONE));
        assertTrue(b.endIfStarted(him, 99).isEmpty(), "an older blink's expiry");
        assertTrue(b.isBlinked(him));
        assertTrue(b.endIfStarted(him, 100).isPresent());
        assertTrue(b.endIfStarted(him, 100).isEmpty(), "ended once");
        // A blink cancelled at 140 and a new one at 1400: the first expiry (due 260) must not end the second.
        b.begin(him, new BlinkedPlayers.State(1400, 1560, NONE));
        assertTrue(b.endIfStarted(him, 100).isEmpty());
        assertTrue(b.isBlinked(him));
    }
}
