package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHAGE'S OWN FUSE (the seat's ruling 1 on #204, route (b)): a pure state machine, every number from content,
 * vanilla's rules read from the jar (PLAN-dragon-mobs.md section 7 F-C; {@code SwellGoal#canUse}, {@code #tick},
 * {@code Creeper#tick}):
 * <ul>
 *   <li>each tick the swell first moves by its direction (+1 or -1, floored at 0) and detonates at the fuse;</li>
 *   <li>then a DOWN fuse turns UP when a target is STRICTLY inside the trigger ({@code distanceToSqr < 9.0}) with
 *       sight; an UP fuse turns DOWN when the target is gone, STRICTLY beyond the cancel ({@code > 49.0}), or out of
 *       sight.</li>
 * </ul>
 * The stage-5 distances, 3 and 7, and the 30 ticks; stage 1's are 1 and 7/3.
 */
class MobFuseTest {

    private static final double TRIGGER = 3.0;
    private static final double CANCEL = 7.0;
    private static final int FUSE = 30;

    private static MobFuse.Event tick(MobFuse f, double d, boolean sight) {
        return f.tick(OptionalDouble.of(d), sight, TRIGGER, CANCEL, FUSE);
    }

    @Test
    void itDoesNotStartAtOrJustOutsideTheTriggerAndDoesJustInside() {
        MobFuse f = new MobFuse();
        assertEquals(MobFuse.Event.NONE, tick(f, 3.0, true), "exactly at the trigger: vanilla's < is strict");
        assertEquals(MobFuse.Event.NONE, tick(f, 3.001, true));
        assertFalse(f.fusing());
        assertEquals(MobFuse.Event.START, tick(f, 2.999, true));
        assertTrue(f.fusing());
        assertEquals(1, f.direction());
    }

    @Test
    void itNeedsSightToStart() {
        MobFuse f = new MobFuse();
        assertEquals(MobFuse.Event.NONE, tick(f, 1.0, false));
        assertFalse(f.fusing());
    }

    @Test
    void itNeedsATarget() {
        MobFuse f = new MobFuse();
        assertEquals(MobFuse.Event.NONE, f.tick(OptionalDouble.empty(), true, TRIGGER, CANCEL, FUSE));
        assertFalse(f.fusing());
    }

    @Test
    void itDetonatesThirtyTicksAfterItStarts() {
        MobFuse f = new MobFuse();
        assertEquals(MobFuse.Event.START, tick(f, 2.0, true));
        assertEquals(0, f.swell(), "the start tick's swell has already moved; it rises from the next");
        for (int t = 1; t < FUSE; t++) {
            assertEquals(MobFuse.Event.NONE, tick(f, 2.0, true), "tick " + t);
            assertEquals(t, f.swell());
        }
        assertEquals(MobFuse.Event.DETONATE, tick(f, 2.0, true), "tick 30");
        assertEquals(FUSE, f.swell());
    }

    @Test
    void itKeepsFusingAtTheCancelAndResetsJustBeyondIt() {
        MobFuse f = new MobFuse();
        tick(f, 2.0, true);
        assertEquals(MobFuse.Event.NONE, tick(f, 7.0, true), "exactly at the cancel: vanilla's > is strict");
        assertTrue(f.fusing());
        assertEquals(MobFuse.Event.RESET, tick(f, 7.001, true));
        assertFalse(f.fusing());
        assertEquals(-1, f.direction());
    }

    @Test
    void itResetsWhenSightIsLostOrTheTargetIsGone() {
        MobFuse a = new MobFuse();
        tick(a, 2.0, true);
        assertEquals(MobFuse.Event.RESET, tick(a, 2.0, false), "sight lost, close or not");
        MobFuse b = new MobFuse();
        tick(b, 2.0, true);
        assertEquals(MobFuse.Event.RESET, b.tick(OptionalDouble.empty(), true, TRIGGER, CANCEL, FUSE));
    }

    @Test
    void aResetSwellFallsATickAtATimeAndARestartResumesFromWhereItIs() {
        // Vanilla's swell does not snap to 0: it counts down by one a tick, as the client's animation does.
        MobFuse f = new MobFuse();
        tick(f, 2.0, true);
        for (int t = 0; t < 10; t++) tick(f, 2.0, true);
        assertEquals(10, f.swell());
        tick(f, 8.0, true);                                       // swell 11, then RESET
        assertEquals(11, f.swell());
        tick(f, 8.0, true);
        tick(f, 8.0, true);
        assertEquals(9, f.swell(), "down one a tick");
        assertEquals(MobFuse.Event.START, tick(f, 2.0, true));   // swell 8, then START
        assertEquals(8, f.swell());
        for (int t = 9; t < FUSE; t++) assertEquals(MobFuse.Event.NONE, tick(f, 2.0, true));
        assertEquals(MobFuse.Event.DETONATE, tick(f, 2.0, true), "22 ticks after the restart, from 8");
    }

    /**
     * THE INSTRUMENT'S LIGHT (GATE-implosion-drag.md's Part D row, 2026-11-18): a down fuse lit with NO target in
     * reach, then left lit however far its target is, out of sight or gone, and it detonates 30 ticks after it was lit,
     * as a START does.
     */
    @Test
    void aLitFuseIgnoresItsTargetAndDetonatesThirtyTicksLater() {
        MobFuse f = new MobFuse();
        assertTrue(f.light(), "a down fuse lights");
        assertTrue(f.fusing());
        assertTrue(f.forced());
        for (int t = 1; t < FUSE; t++) {
            assertEquals(MobFuse.Event.NONE, f.tick(OptionalDouble.empty(), false, TRIGGER, CANCEL, FUSE),
                    "no target, no sight: still no reset at " + t);
        }
        assertEquals(MobFuse.Event.DETONATE, f.tick(OptionalDouble.of(50.0), false, TRIGGER, CANCEL, FUSE));
    }

    /** Lighting a fuse already lit changes nothing that shows, but holds it lit from then on. */
    @Test
    void lightingALitFuseHoldsItWithoutANewStart() {
        MobFuse f = new MobFuse();
        assertEquals(MobFuse.Event.START, tick(f, 2.0, true));
        assertFalse(f.light(), "already lit: no new start to show");
        assertTrue(f.forced());
        assertEquals(MobFuse.Event.NONE, tick(f, 50.0, true), "beyond the cancel, and no reset");
    }

    @Test
    void theSwellNeverGoesBelowZero() {
        MobFuse f = new MobFuse();
        for (int t = 0; t < 5; t++) tick(f, 10.0, true);
        assertEquals(0, f.swell());
    }

    @Test
    void aStageOneFuseUsesItsOwnShorterDistances() {
        // Stage 1: trigger 1, cancel 7/3 (StagedDistanceTest). 1.5 away starts nothing at stage 1; at stage 5 it would.
        MobFuse f = new MobFuse();
        assertEquals(MobFuse.Event.NONE, f.tick(OptionalDouble.of(1.5), true, 1.0, 7.0 / 3, FUSE));
        assertEquals(MobFuse.Event.START, f.tick(OptionalDouble.of(0.999), true, 1.0, 7.0 / 3, FUSE));
        assertEquals(MobFuse.Event.RESET, f.tick(OptionalDouble.of(2.334), true, 1.0, 7.0 / 3, FUSE));
    }
}
