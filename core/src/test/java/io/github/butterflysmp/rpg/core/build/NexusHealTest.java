package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE NEXUS ITEM HEALS ON THE BEAT (the seat's ruling M, 2026-11-22). The mirror makes the item sometimes an arrow, so any
 * path that removes arrows by type can now remove it; {@code /clear} is the one found, and the heal covers the ones not
 * found. It mints ONLY when the star is on, NONE is found, and he is alive. One row per condition, each flipped alone.
 */
class NexusHealTest {

    @Test
    void aSwitchedOnStarWithNoneFoundOnALivePlayerHeals() {
        assertTrue(NexusHeal.heals(true, 0, true));
    }

    @Test
    void aSwitchedOffStarIsNeverMinted() {
        assertFalse(NexusHeal.heals(false, 0, true));
    }

    /** A star found anywhere, in any slot, is not this rule's business: hold_only moves it on purpose (ruling G). */
    @Test
    void anItemFoundAnywhereIsLeftAlone() {
        assertFalse(NexusHeal.heals(true, 1, true));
        assertFalse(NexusHeal.heals(true, 2, true), "duplicates are not this rule's business either");
    }

    /** A dead player is not healed: the respawn path already converges. */
    @Test
    void aDeadPlayerIsNotHealed() {
        assertFalse(NexusHeal.heals(true, 0, false));
    }
}
