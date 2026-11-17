package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHALANX'S BLINK, step R8's two changes (PLAN-dragon-mobs.md): Ben's cooldown, "Say 12 seconds" (240 ticks),
 * counted from a blink that LANDS; and R3, a blink decided off the ground failing every draw.
 */
class MobBlinkCooldownTest {

    private static MobBlink blink(int cooldown) {
        return new MobBlink(5, 6, 16, 8, 3, List.of(), "squid_ink", 12, cooldown);
    }

    // --- the cooldown -----------------------------------------------------------------------------------------------

    @Test
    void afterALandedBlinkItRunsTwoHundredAndThirtyNineTicksAndEndsOnTheTwoHundredAndFortieth() {
        MobBlink.Cooldown c = blink(240).cooldown();
        assertFalse(c.running(), "a fresh Phalanx has no cooldown");
        c.landed();
        for (int i = 1; i <= 239; i++) {
            c.tick();
            assertTrue(c.running(), "tick " + i);
        }
        c.tick();
        assertFalse(c.running(), "tick 240: it may blink again");
        assertEquals(0, c.remaining());
    }

    /** A failed attempt does not spend it: only landed() starts it, and nothing else is offered. */
    @Test
    void onlyALandingStartsItAndZeroMeansNone() {
        MobBlink.Cooldown c = blink(240).cooldown();
        for (int i = 0; i < 500; i++) c.tick();
        assertFalse(c.running());
        MobBlink.Cooldown none = blink(0).cooldown();
        none.landed();
        assertFalse(none.running(), "cooldown_ticks 0: no cooldown");
    }

    @Test
    void aNegativeCooldownIsRefusedByName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> blink(-1));
        assertTrue(e.getMessage().contains("blink.cooldown_ticks"), e.getMessage());
    }

    @Test
    void theEightNumberFormHasNoCooldown() {
        assertEquals(0, new MobBlink(5, 6, 16, 8, 3, List.of(), "squid_ink", 12).cooldownTicks());
    }

    // --- R3: the level the search is anchored on ----------------------------------------------------------------------

    /**
     * A flat stone floor whose top is y 63, as the port answers it (MobTeleporter.footing): a body FITS when its feet are
     * at or above 63 (the box is inset 0.001, so resting on the face fits); it is GROUNDED when the slab from 0.05 to
     * 0.001 below its feet is inside the floor, so only feet in [63, 63.05) are.
     */
    private static final BeamTeleport.Footing FLOOR_63 = new BeamTeleport.Footing() {
        @Override public boolean loaded(Vec3 f) { return true; }
        @Override public boolean fits(Vec3 f) { return f.y() + 1.0E-3 >= 63.0; }
        @Override public boolean grounded(Vec3 f) { return f.y() - 0.05 < 63.0 && f.y() - 1.0E-3 >= 62.0; }
        @Override public boolean sees(Vec3 f) { throw new AssertionError("a blink never asks for sight"); }
    };

    /**
     * BQ1, tick 2647, reproduced: decided from feet at 63.305, every height tried is 63.305 + k, and none sits on the
     * floor's top. k = -1, -2, -3 are inside the floor (blocked=3), k = 0..3 hang 0.305 above a whole block
     * (ungrounded=4): exactly the logged "NO_FOOTING blocked=3 ungrounded=4".
     */
    @Test
    void anchoredOnItsOwnFractionalHeightEveryDrawFailsAsTheBootLogged() {
        MobBlink b = blink(240);
        Vec3 feet = new Vec3(-76.631, 63.305, 8.430);
        List<MobBlink.Draw> draws = Collections.nCopies(8, new MobBlink.Draw(297.823, 9.626));
        MobBlink.Decision d = b.decide(feet, feet.y(), draws, FLOOR_63);
        assertTrue(d.landing().isEmpty());
        assertEquals(8, d.tried().size());
        for (MobBlink.Verdict v : d.tried()) {
            assertEquals(BeamTeleport.Outcome.NO_FOOTING, v.outcome());
            assertEquals(3, v.blocked());
            assertEquals(4, v.ungrounded());
        }
    }

    /** THE FIX: anchored on the ground beneath it (63.000), the same draw lands on the floor at its first height. */
    @Test
    void anchoredOnTheGroundBeneathItTheSameDrawLands() {
        MobBlink b = blink(240);
        Vec3 feet = new Vec3(-76.631, 63.305, 8.430);
        double anchor = MobBlink.anchor(feet, false, OptionalDouble.of(63.0));
        assertEquals(63.0, anchor);
        MobBlink.Decision d = b.decide(feet, anchor, List.of(new MobBlink.Draw(90.0, 8.0)), FLOOR_63);
        assertEquals(1, d.tried().size());
        Vec3 l = d.landing().orElseThrow();
        assertEquals(63.0, l.y(), 1e-12);
        assertEquals(-76.631, l.x(), 1e-9);
        assertEquals(16.430, l.z(), 1e-9);
    }

    @Test
    void onTheGroundTheAnchorIsItsFeetAndWithNoGroundWithinReachItIsItsFeetToo() {
        Vec3 slab = new Vec3(0, 63.5, 0);
        assertEquals(63.5, MobBlink.anchor(slab, true, OptionalDouble.of(63.0)), "on the ground it stands where it stands");
        assertEquals(70.25, MobBlink.anchor(new Vec3(0, 70.25, 0), false, OptionalDouble.empty()));
    }
}
