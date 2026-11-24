package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SHOCKWAVE'S DECISION (the seat's rule for Kinetic Tremors, 2026-11-23), at Ben's numbers: 3 hits, 60 ticks
 * (3 seconds), a 5-tick (0.25 s) per-player cooldown. Each boundary pinned at an exact tick.
 */
class ShockwaveStreaksTest {

    private static final EnchantBehaviour.Shockwave BENS = new EnchantBehaviour.Shockwave(3, 60, 5,
            List.of(10.0, 15.0, 20.0), List.of(3.0, 4.0, 5.0), "kinetic", null);
    private static final UUID HIM = new UUID(0, 1);
    private static final UUID A = new UUID(0, 2);
    private static final UUID B = new UUID(0, 3);
    private static final String E = "tremors";

    @Test
    void theThirdHitOnOneTargetInsideTheWindowFiresAndNotBefore() {
        var s = new ShockwaveStreaks();
        assertFalse(s.hit(HIM, E, A, 0, BENS));
        assertFalse(s.hit(HIM, E, A, 10, BENS));
        assertTrue(s.hit(HIM, E, A, 20, BENS), "the third, 20 ticks after the first");
    }

    /** The window's boundary: a hit 59 ticks old still counts, one 60 ticks old does not (now - t < 60). */
    @Test
    void theWindowIsSixtyTicksAndItsBoundaryIsPinned() {
        var inside = new ShockwaveStreaks();
        inside.hit(HIM, E, A, 0, BENS);
        inside.hit(HIM, E, A, 1, BENS);
        assertTrue(inside.hit(HIM, E, A, 59, BENS), "the first is 59 old: three in the window");
        var outside = new ShockwaveStreaks();
        outside.hit(HIM, E, A, 0, BENS);
        outside.hit(HIM, E, A, 1, BENS);
        assertFalse(outside.hit(HIM, E, A, 60, BENS), "the first is 60 old: two in the window");
    }

    /** The gate's row: two hits, 61 ticks, one hit: none. */
    @Test
    void twoHitsThenSixtyOneTicksThenOneFiresNothing() {
        var s = new ShockwaveStreaks();
        assertFalse(s.hit(HIM, E, A, 0, BENS));
        assertFalse(s.hit(HIM, E, A, 0, BENS));
        assertFalse(s.hit(HIM, E, A, 61, BENS));
        assertEquals(1, s.held(HIM, E, A), "the two old ones were pruned; the new one is held");
    }

    /** Ben: "Multiple pellets count so one shot triggering it is fine". Four in one tick fire exactly one. */
    @Test
    void fourPelletsInOneTickFireExactlyOne() {
        var s = new ShockwaveStreaks();
        int fired = 0;
        for (int pellet = 0; pellet < 4; pellet++) if (s.hit(HIM, E, A, 100, BENS)) fired++;
        assertEquals(1, fired);
    }

    /** "Same target": hits split across two mobs never make three on either. */
    @Test
    void hitsSplitAcrossTwoTargetsFireNothing() {
        var s = new ShockwaveStreaks();
        assertFalse(s.hit(HIM, E, A, 0, BENS));
        assertFalse(s.hit(HIM, E, B, 1, BENS));
        assertFalse(s.hit(HIM, E, A, 2, BENS));
        assertFalse(s.hit(HIM, E, B, 3, BENS));
        assertEquals(2, s.held(HIM, E, A));
        assertEquals(2, s.held(HIM, E, B));
    }

    /** Ben's .25 s: the cooldown is per PLAYER. 4 ticks after a shockwave nothing fires; 5 ticks after, the next hit does. */
    @Test
    void theCooldownIsFiveTicksPerPlayerAndTheCountIsNotCleared() {
        var s = new ShockwaveStreaks();
        s.hit(HIM, E, A, 0, BENS);
        s.hit(HIM, E, A, 1, BENS);
        assertTrue(s.hit(HIM, E, A, 2, BENS), "fires at 2");
        assertFalse(s.hit(HIM, E, A, 6, BENS), "4 ticks later: the cooldown");
        assertTrue(s.hit(HIM, E, A, 7, BENS), "5 ticks later: the count was never cleared, so it fires again");
        // Per PLAYER, not per target: three fresh hits on B inside the cooldown fire nothing.
        s.hit(HIM, E, B, 8, BENS);
        s.hit(HIM, E, B, 9, BENS);
        assertFalse(s.hit(HIM, E, B, 10, BENS), "B's third, 3 ticks after the last shockwave on A");
        assertTrue(s.hit(HIM, E, B, 12, BENS), "B's fourth, 5 ticks after it");
    }

    /** Another player's hits are his own record. */
    @Test
    void twoPlayersKeepSeparateCounts() {
        var s = new ShockwaveStreaks();
        UUID other = new UUID(0, 9);
        s.hit(HIM, E, A, 0, BENS);
        s.hit(other, E, A, 1, BENS);
        assertFalse(s.hit(HIM, E, A, 2, BENS), "his two, not three");
    }

    /** Records forgotten: when the target dies, after the window idle, and on his quit. */
    @Test
    void recordsAreForgottenOnDeathIdleAndQuit() {
        var s = new ShockwaveStreaks();
        s.hit(HIM, E, A, 0, BENS);
        s.hit(HIM, E, B, 0, BENS);
        s.forgetTarget(A);
        assertEquals(0, s.held(HIM, E, A), "A died");
        assertEquals(1, s.held(HIM, E, B), "the control: B is kept");
        s.hit(HIM, E, A, 70, BENS);
        assertEquals(1, s.targets(HIM, E), "B, idle 70 ticks, was pruned by the next hit; A is the new one");
        s.forgetPlayer(HIM);
        assertEquals(0, s.targets(HIM, E), "his quit");
    }

    @Test
    void aMalformedShockwaveIsRefusedByItsKey() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shockwave(3, 60, 5,
                List.of(10.0, 15.0), List.of(3.0, 4.0, 5.0), "kinetic", null)).getMessage().contains("one each per level"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shockwave(3, 60, 5,
                List.of(10.0, 0.0, 20.0), List.of(3.0, 4.0, 5.0), "kinetic", null)).getMessage().contains("damage_by_level at level 2"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shockwave(0, 60, 5,
                List.of(10.0), List.of(3.0), "kinetic", null)).getMessage().contains("hits"));
        assertEquals(15.0, BENS.damageAt(2), 0.0);
        assertEquals(5.0, BENS.radiusAt(3), 0.0);
    }
}
