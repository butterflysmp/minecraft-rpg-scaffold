package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingGuardTest {

    /** The measured case: hotbar Q is drop-then-swing, inventory Q is swing-then-click, both one tick. */
    @Test
    void aSwingInTheSameTickAsADropAttemptDoesNotCast() {
        assertFalse(SwingGuard.castsActive1(15041, 15041));
    }

    /** The edges both sides: the guard is ONE tick wide, so a real click a tick away still casts. */
    @Test
    void aSwingOneTickEitherSideCasts() {
        assertTrue(SwingGuard.castsActive1(15042, 15041));
        assertTrue(SwingGuard.castsActive1(15040, 15041));
    }

    @Test
    void noDropEverRecordedCasts() {
        assertTrue(SwingGuard.castsActive1(0, SwingGuard.NEVER));
        assertTrue(SwingGuard.castsActive1(15041, SwingGuard.NEVER));
    }
}
