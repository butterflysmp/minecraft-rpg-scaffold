package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The HURT line's {@code cause=void}: a mark is the void's once, and only within two ticks of being made. */
class VoidHitsTest {

    private final UUID ben = UUID.randomUUID();

    @Test
    void aMarkIsTakenOnceWithinTwoTicks() {
        VoidHits hits = new VoidHits();
        hits.mark(ben, 100);
        assertTrue(hits.take(ben, 102));
        assertFalse(hits.take(ben, 102), "taken once");
    }

    @Test
    void aStaleMarkLabelsNothingAndIsDropped() {
        VoidHits hits = new VoidHits();
        hits.mark(ben, 100);
        assertFalse(hits.take(ben, 103), "three ticks later is a different hit");
        hits.mark(ben, 200);
        assertTrue(hits.take(ben, 200), "the control: a fresh mark is taken");
    }

    @Test
    void anUnmarkedPlayerIsNotTheVoids() {
        VoidHits hits = new VoidHits();
        hits.mark(ben, 100);
        assertFalse(hits.take(UUID.randomUUID(), 100));
        hits.forget(ben);
        assertFalse(hits.take(ben, 100), "forgotten on quit");
    }
}
