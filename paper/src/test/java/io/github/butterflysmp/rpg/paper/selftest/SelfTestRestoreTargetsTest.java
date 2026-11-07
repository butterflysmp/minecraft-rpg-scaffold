package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the restore removes (the seat, 2026-11-06): the tagged test mobs AND every experience orb, never a player.
 * GATE-selftest.md ST2's 38cc9903 reading is why the orbs are here: a staged kill's orbs were picked up after the
 * restore and moved the next run's snapshot to {@code xp=4}. The removal itself needs a live world; ST2 reads it.
 */
class SelfTestRestoreTargetsTest {

    @Test
    void aTaggedMobIsRemoved() {
        assertTrue(SelfTestSnapshot.removedOnRestore(false, true, false));
    }

    @Test
    void anExperienceOrbIsRemovedThoughNothingTagsIt() {
        assertTrue(SelfTestSnapshot.removedOnRestore(false, false, true));
    }

    @Test
    void anUntaggedEntityThatIsNotAnOrbIsLeftAlone() {
        assertFalse(SelfTestSnapshot.removedOnRestore(false, false, false));
    }

    @Test
    void aPlayerIsNeverRemovedEvenIfTagged() {
        assertFalse(SelfTestSnapshot.removedOnRestore(true, true, false));
        assertFalse(SelfTestSnapshot.removedOnRestore(true, false, false));
    }
}
