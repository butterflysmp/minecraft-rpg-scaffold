package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHALANX'S BLINK, step R8's two changes (PLAN-dragon-mobs.md): Ben's cooldown, "Say 12 seconds" (240 ticks),
 * counted from a blink that LANDS; and R3, a blink decided off the ground failing every draw.
 */
class MobBlinkCooldownTest {

    /**
     * F21 (the seat's queue, item 4, 2026-11-19): A COOLDOWN RESUMES ACROSS AN UNLOAD. A Phalanx loaded again mid-cooldown
     * resumes what was left, clamped to [0, length]: a stale value from a past server run cannot hold it past one length.
     */
    @Test
    void aCooldownResumesWhatWasLeftClampedToItsLength() {
        MobBlink.Cooldown c = blink(240).cooldown();
        c.resume(100);
        assertEquals(100, c.remaining());
        assertTrue(c.running());
        c.tick();
        assertEquals(99, c.remaining());
        MobBlink.Cooldown big = blink(240).cooldown();
        big.resume(10_000);
        assertEquals(240, big.remaining(), "never more than one length");
        MobBlink.Cooldown done = blink(240).cooldown();
        done.resume(-5);
        assertEquals(0, done.remaining(), "an end already passed: no cooldown");
        assertFalse(done.running());
    }

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
     * BQ1, tick 2647, THE RECORD: decided from feet at 63.305, every height tried was 63.305 + k, and none sat on the
     * floor's top. k = -1, -2, -3 were inside the floor (blocked=3), k = 0..3 hung 0.305 above a whole block
     * (ungrounded=4): exactly the logged "NO_FOOTING blocked=3 ungrounded=4". Until 2026-11-17 this row passed the
     * fractional height and asserted those counts.
     *
     * <p><b>F19, 2026-11-17:</b> the floor now happens INSIDE the shared check ({@link BeamTeleport#foot} anchors its
     * column on {@link BeamTeleport#anchor}), so the Knell's teleport has the fix too, and a caller can no longer
     * produce the defect by passing a fractional height. The same eight draws from the same feet now land on the floor.
     */
    @Test
    void aFractionalHeightPassedAsTheAnchorIsFlooredByTheSharedCheckAndLands() {
        MobBlink b = blink(240);
        Vec3 feet = new Vec3(-76.631, 63.305, 8.430);
        List<MobBlink.Draw> draws = Collections.nCopies(8, new MobBlink.Draw(297.823, 9.626));
        MobBlink.Decision d = b.decide(feet, feet.y(), draws, FLOOR_63);
        assertEquals(63.0, d.landing().orElseThrow().y(), 1e-12);
        assertEquals(1, d.tried().size(), "the first draw lands");
    }

    /**
     * THE FIX: anchored on the WHOLE BLOCK its feet are in (63), every height tried is a whole number, and the same draw
     * lands on the floor at its first height.
     */
    @Test
    void anchoredOnTheWholeBlockItsFeetAreInTheSameDrawLands() {
        MobBlink b = blink(240);
        Vec3 feet = new Vec3(-76.631, 63.305, 8.430);
        double anchor = MobBlink.anchor(feet.y());
        assertEquals(63.0, anchor);
        MobBlink.Decision d = b.decide(feet, anchor, List.of(new MobBlink.Draw(90.0, 8.0)), FLOOR_63);
        assertEquals(1, d.tried().size());
        Vec3 l = d.landing().orElseThrow();
        assertEquals(63.0, l.y(), 1e-12);
        assertEquals(-76.631, l.x(), 1e-9);
        assertEquals(16.430, l.z(), 1e-9);
    }

    /**
     * THE SAME DEFECT ON THE GROUND: a Phalanx standing on a bottom slab (feet 63.5) blinking onto a full-block floor. From
     * 63.5 every height is 63.5 + k and none rests on 63; from its block, 63, the first height does. This is the case a
     * "ground beneath it" anchor would have missed (it IS on the ground), and the gate's row stages it, because a slab is
     * a fractional height that holds still. Until 2026-11-17 it also asserted "from its feet: nowhere"; since F19 the
     * shared check floors any anchor, so from its feet it lands too, at the same height.
     */
    @Test
    void aPhalanxOnASlabLandsFromItsFeetAndFromItsBlockAlike() {
        MobBlink b = blink(240);
        Vec3 feet = new Vec3(0.5, 63.5, 0.5);
        List<MobBlink.Draw> draws = Collections.nCopies(8, new MobBlink.Draw(90.0, 8.0));
        assertEquals(63.0, b.decide(feet, feet.y(), draws, FLOOR_63).landing().orElseThrow().y(), 1e-12);
        assertEquals(63.0, b.decide(feet, MobBlink.anchor(feet.y()), draws, FLOOR_63).landing().orElseThrow().y(), 1e-12);
    }

    /** Whole numbers stay; a near-whole height from floating-point drift snaps to its own block, not the one below. */
    @Test
    void theAnchorIsTheWholeBlockAndANearWholeHeightIsItsOwn() {
        assertEquals(63.0, MobBlink.anchor(63.0));
        assertEquals(63.0, MobBlink.anchor(63.5));
        assertEquals(70.0, MobBlink.anchor(70.25));
        assertEquals(63.0, MobBlink.anchor(62.9999999999));
        assertEquals(-2.0, MobBlink.anchor(-1.5));
    }
}
