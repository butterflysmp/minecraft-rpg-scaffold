package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE DRAG (Ben, 2026-11-18: "1. yes 2. yes 3. yes 4. yes" to the seat's four points). The four outcomes are the spec,
 * each run here against {@link PlayerTravelModel}, the client's own travel READ from the jar, with the drag driven as
 * the server drives it ({@link ImplosionDrag}, {@link DragMotion}):
 * <ol type="a">
 *   <li>STANDING STILL at 2, 3, 4 and 5 blocks from a stage-5 blast: dragged to rest within the margin of the origin,
 *       or as near as the second allows.</li>
 *   <li>WALKING DIRECTLY AWAY from 3 blocks: still nearer the origin at the end than at the start.</li>
 *   <li>SPRINTING DIRECTLY AWAY from the outer third of the reach: outside the reach before the second is up.</li>
 *   <li>NEVER PAST THE ORIGIN: a victim that reaches the margin is HELD there; one already inside it is not moved.</li>
 * </ol>
 * The Phage's use: reach 6 (stage 5), vanilla's falloff for the damage, no pull and no lift, explosion knockback
 * resistance, 20 ticks, drag 0.28 BLOCKS PER TICK at the origin. Every expected figure that is not a bound is
 * computed here from the shape's own formula, or EXECUTED (the comment says which); none was worked by hand.
 */
class ImplosionDragTest {

    static final Implosion PHAGE = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 0.0, 0.0,
            Implosion.Resistance.EXPLOSION_KNOCKBACK, 20, 0.28);
    private static final Vec3 O = PlayerTravelModel.ORIGIN;
    private static final double SLIDE = 1.0 / (1.0 - PlayerTravelModel.M);   // travelFactor(0) on stone
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes());
    private static final UUID B = UUID.nameUUIDFromBytes("b".getBytes());

    private static Implosion.Footing west(double h, boolean onGround, double resistance) {
        return new Implosion.Footing(new Vec3(O.x() - h, O.y(), O.z()), PlayerTravelModel.EYE, onGround,
                PlayerTravelModel.STONE, resistance);
    }

    // --- the quantity ----------------------------------------------------------------------------------------------

    @Test
    void oneTickAddsDragTimesTheLinearFactorTowardTheOriginOnTheGround() {
        Implosion.DragStep s = PHAGE.dragStep(O, west(3.0, true, 0.0), Vec3.ZERO).orElseThrow();
        double strength = 0.28 * (1 - 3.0 / 6.0);
        assertEquals(strength, s.strength(), 1e-12, "drag x (1 - d / reach)");
        assertEquals((3.0 - 0.5) / SLIDE, s.cap(), 1e-12, "the speed whose slide on stone ends at the margin");
        assertEquals(strength, s.added(), 1e-12, "the cap does not bind at 3");
        assertFalse(s.held());
        assertEquals(strength, s.add().x(), 1e-12, "toward the origin: +x, he stands west of it");
        assertEquals(0.0, s.add().y(), 0.0, "a drag has no vertical");
        assertEquals(0.0, s.add().z(), 1e-12);
    }

    /** THE ONLY GUARD of the drag's {@code (1 - resistance)}: MUTNORES (2026-11-18) reddened this row and no other. */
    @Test
    void theResistanceCutsTheStrength() {
        Implosion.DragStep s = PHAGE.dragStep(O, west(3.0, true, 0.5), Vec3.ZERO).orElseThrow();
        assertEquals(0.28 * 0.5 * 0.5, s.strength(), 1e-12, "x (1 - resistance)");
    }

    @Test
    void beyondTheReachThereIsNoStepAndAtTheReachTheStrengthIsNothing() {
        assertTrue(PHAGE.dragStep(O, west(6.05, true, 0.0), Vec3.ZERO).isEmpty());
        Implosion.DragStep edge = PHAGE.dragStep(O, west(6.0, true, 0.0), Vec3.ZERO).orElseThrow();
        assertEquals(0.0, edge.added(), 1e-12, "the linear factor is 0 at the reach");
    }

    @Test
    void atOrInsideTheMarginNothingIsAddedAndItIsHeld() {
        for (double h : new double[] {0.5, 0.3, 0.0}) {
            Implosion.DragStep s = PHAGE.dragStep(O, west(h, true, 0.0), Vec3.ZERO).orElseThrow();
            assertEquals(0.0, s.added(), 0.0, "at " + h + ": nothing toward, and never anything outward");
            assertTrue(s.held(), "at " + h);
            assertEquals(Vec3.ZERO, s.add(), "at " + h);
        }
    }

    @Test
    void aVictimWhoseOwnMomentumAlreadyReachesTheMarginIsAddedNothingAndNeverBraked() {
        double cap = (2.0 - 0.5) / SLIDE;
        Implosion.DragStep fast = PHAGE.dragStep(O, west(2.0, true, 0.0), new Vec3(cap + 0.1, 0, 0)).orElseThrow();
        assertEquals(0.0, fast.added(), 0.0, "already toward faster than the cap: no add, and no negative add");
        Implosion.DragStep part = PHAGE.dragStep(O, west(2.0, true, 0.0), new Vec3(cap - 0.05, 0, 0)).orElseThrow();
        assertEquals(0.05, part.added(), 1e-12, "topped up to the cap");
        assertTrue(part.held(), "the margin limited it");
        Implosion.DragStep away = PHAGE.dragStep(O, west(2.0, true, 0.0), new Vec3(-0.2, 0, 0)).orElseThrow();
        assertEquals(away.strength(), away.added(), 1e-12, "walking away: the whole strength");
        assertFalse(away.held());
    }

    @Test
    void inTheAirTheCapUsesTheAirsSlowestDecay() {
        Implosion.DragStep s = PHAGE.dragStep(O, west(1.0, false, 0.0), Vec3.ZERO).orElseThrow();
        assertEquals((1.0 - 0.5) * (1 - Implosion.tickMultiplier(false, 0.6f)), s.cap(), 1e-12);
    }

    // --- what the shape accepts --------------------------------------------------------------------------------------

    @Test
    void theFiveNumberShapeIsASinglePull() {
        Implosion one = new Implosion(5, Implosion.Falloff.FLAT, 1.0, 0.0, Implosion.Resistance.NONE);
        assertEquals(1, one.durationTicks());
        assertEquals(0.0, one.drag(), 0.0);
        assertFalse(one.drags());
        assertTrue(PHAGE.drags());
    }

    @Test
    void aBadDurationOrADragOnAPullIsRefusedByName() {
        var f = Implosion.Falloff.VANILLA_EXPLOSION;
        var r = Implosion.Resistance.NONE;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 0, r, 0, 0.28))
                .getMessage().contains("duration_ticks"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 1.25, 0, r, 20, 0.28))
                .getMessage().contains("pull must be 0"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 1.0, r, 20, 0.28))
                .getMessage().contains("lift must be 0"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 0, r, 20, 0.0))
                .getMessage().contains("drag above 0"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 1.25, 0, r, 1, 0.28))
                .getMessage().contains("single pull"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 0, r, 20, -1))
                .getMessage().contains("drag must be 0 or more"));
        assertThrows(IllegalArgumentException.class, () -> new ImplosionDrag(O,
                new Implosion(6, f, 1.25, 0, r), new DragMotion()), "a single pull is not a drag");
    }

    // --- THE FOUR OUTCOMES, against the client's own travel ----------------------------------------------------------

    @Test
    void theReadMovementFacts() {
        // EXECUTED (jshell drag.jsh, 2026-11-18) from the same READ constants: walking 0.096040 a tick added, 0.21154
        // moved; sprinting 0.124852 added, 0.27500 moved.
        assertEquals(0.096040, PlayerTravelModel.WALK, 5e-7);
        assertEquals(0.124852, PlayerTravelModel.SPRINT, 5e-7);
        assertEquals(0.21154, PlayerTravelModel.terminal(PlayerTravelModel.WALK), 5e-6);
        assertEquals(0.27500, PlayerTravelModel.terminal(PlayerTravelModel.SPRINT), 5e-6);
    }

    /**
     * a. STANDING STILL: from 2, 3, 4 and 5 he is dragged to the margin and comes to rest there. At 0.28 the second is
     * long enough from 5 (EXECUTED, scratch {@code sweep.jsh}: at 0.24 he rested at 1.207, at 0.26 at 0.614). A
     * mutation that weakens the drag reddens the 5.
     */
    @Test
    void aStandingStillHeIsDraggedToTheMarginFromTwoThreeFourAndFive() {
        for (double h0 : new double[] {2.0, 3.0, 4.0, 5.0}) {
            PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, 0.0, 0.0);
            assertTrue(r.rest() >= 0.5 - 1e-9, "from " + h0 + ": never past the margin, rests at " + r.rest());
            assertTrue(r.rest() < 0.51, "from " + h0 + ": at the margin, rests at " + r.rest());
            assertEquals(ImplosionDrag.Outcome.HELD, r.summary().outcome(), "from " + h0);
            assertEquals(20, r.summary().ticks(), "from " + h0 + ": pulled every tick of the second");
        }
    }

    /** b. WALKING AWAY from 3: slowed, not cancelled, from rest and already at walking speed. */
    @Test
    void bWalkingAwayFromThreeHeEndsTheSecondNearerThanHeStarted() {
        PlayerTravelModel.Run fromRest = PlayerTravelModel.run(PHAGE, 3.0, PlayerTravelModel.WALK, 0.0);
        PlayerTravelModel.Run walking = PlayerTravelModel.run(PHAGE, 3.0, PlayerTravelModel.WALK,
                PlayerTravelModel.terminal(PlayerTravelModel.WALK) * PlayerTravelModel.M);
        assertTrue(fromRest.end() < 3.0, "from rest he ends the second at " + fromRest.end());
        assertTrue(walking.end() < 3.0, "already walking he ends the second at " + walking.end());
        assertTrue(walking.end() > fromRest.end(), "his own momentum carries him farther out first");
    }

    /** c. SPRINTING AWAY from the outer third (4 to 6 of 6): out of the reach before the second is up. */
    @Test
    void cSprintingAwayFromTheOuterThirdHeLeavesTheReachInTheSecond() {
        for (double h0 : new double[] {4.0, 4.5, 5.0, 5.9}) {
            PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, PlayerTravelModel.SPRINT, 0.0);
            assertEquals(ImplosionDrag.Outcome.LEFT, r.summary().outcome(), "from " + h0 + " at rest");
            assertTrue(r.end() > 6.0, "from " + h0 + ": beyond the reach at the second's end, " + r.end());
        }
    }

    /** d. NEVER PAST THE ORIGIN: reaching the margin he is held there, with no overshoot at any tick. */
    @Test
    void dReachingTheMarginHeIsHeldWithNoOvershootAtAnyTick() {
        for (double h0 : new double[] {1.0, 2.0, 3.0}) {
            PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, 0.0, 0.0);
            assertTrue(r.minimum() >= 0.5 - 1e-9, "from " + h0 + ": the nearest he ever came, " + r.minimum());
            for (int i = 1; i < r.seen().size(); i++) {
                assertTrue(r.seen().get(i) <= r.seen().get(i - 1) + 1e-12, "from " + h0 + ": no oscillation at "
                        + i + ", " + r.seen());
            }
        }
    }

    /** d's other half: a victim already inside the margin is never moved, outward or in: nothing is ever sent. */
    @Test
    void dInsideTheMarginHeIsNeverMoved() {
        PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, 0.3, 0.0, 0.0);
        assertEquals(0, r.pushes(), "no velocity is ever sent to him");
        assertEquals(0.3, r.rest(), 1e-12);
        assertEquals(ImplosionDrag.Outcome.HELD, r.summary().outcome());
        assertEquals(0, r.summary().sent());
    }

    // --- the session: who, when, and how it ends --------------------------------------------------------------------

    private static ImplosionDrag.Sighting mob(UUID id, double h, Vec3 velocity) {
        return new ImplosionDrag.Sighting(id, west(h, true, 0.0), velocity);
    }

    @Test
    void tickZeroOnlyLooksAndAMobIsPushedFromTickOneByItsOwnVelocityPlusTheAdd() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        Vec3 v = new Vec3(0.01, -0.0784, 0.02);
        assertTrue(d.tick(0, List.of(mob(A, 3.0, v)), Set.of()).isEmpty(), "the blast's own tick pulls nobody");
        List<ImplosionDrag.Push> one = d.tick(1, List.of(mob(A, 3.0, v)), Set.of());
        assertEquals(1, one.size());
        double add = PHAGE.dragStep(O, west(3.0, true, 0.0), v).orElseThrow().added();
        assertEquals(new Vec3(0.01 + add, -0.0784, 0.02).x(), one.get(0).velocity().x(), 1e-12);
        assertEquals(-0.0784, one.get(0).velocity().y(), 0.0, "the server's own vertical, untouched");
        assertEquals(0.02, one.get(0).velocity().z(), 1e-12);
    }

    @Test
    void itRunsTheBlastsTickThenTwentyAndThenRefuses() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        for (int t = 0; t <= 20; t++) {
            assertFalse(d.finished(), "before tick " + t);
            d.tick(t, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of());
        }
        assertTrue(d.finished());
        assertEquals(20, d.summaries().get(0).ticks());
        assertThrows(IllegalStateException.class, () -> d.tick(21, List.of(), Set.of()));
    }

    @Test
    void aPlayerFirstSeenMidwayIsPulledFromHisSecondSighting() {
        DragMotion m = new DragMotion();
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, m);
        for (int t = 0; t < 5; t++) d.tick(t, List.of(), Set.of());
        assertTrue(d.tick(5, List.of(mob(B, 3.0, null)), Set.of()).isEmpty(), "his momentum is not known yet");
        assertEquals(1, d.tick(6, List.of(mob(B, 3.0, null)), Set.of()).size(), "known now: pulled");
        assertEquals(1, d.summaries().get(0).ticks());
    }

    @Test
    void oneWhoLeavesTheReachIsNoLongerPulledAndEndsLeft() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        d.tick(0, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of());
        d.tick(1, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of());
        assertTrue(d.tick(2, List.of(mob(A, 8.0, Vec3.ZERO)), Set.of()).isEmpty(), "outside the reach: nothing");
        ImplosionDrag.Summary s = d.summaries().get(0);
        assertEquals(ImplosionDrag.Outcome.LEFT, s.outcome());
        assertEquals(3.0, s.start(), 1e-12);
        assertEquals(8.0, s.end(), 1e-12);
        assertEquals(1, s.ticks());
    }

    /** THE ONLY GUARD of dropping a victim mid-drag: MUTNODROP (2026-11-18) reddened this row and no other. */
    @Test
    void aDroppedVictimIsNeverPushedAgainAndEndsDropped() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        d.tick(0, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of());
        d.tick(1, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of());
        assertTrue(d.tick(2, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of(A)).isEmpty(), "dropped: not pushed");
        assertTrue(d.tick(3, List.of(mob(A, 3.0, Vec3.ZERO)), Set.of()).isEmpty(), "and never again");
        assertEquals(ImplosionDrag.Outcome.DROPPED, d.summaries().get(0).outcome());
        assertFalse(d.known().contains(A));
    }

    @Test
    void oneNeverInsideTheReachIsNoVictim() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        d.tick(0, List.of(mob(A, 7.0, Vec3.ZERO)), Set.of());
        d.tick(1, List.of(mob(A, 7.0, Vec3.ZERO)), Set.of());
        assertTrue(d.summaries().isEmpty());
    }

    /**
     * Two drags on one body in one tick: the second send carries both adds on the first's base (one velocity). THE ONLY
     * GUARD of the tally: MUTTALLYSUM (2026-11-18) reddened this row and no other.
     */
    @Test
    void twoDragsOnOneBodyInOneTickAddToOneVelocity() {
        DragMotion m = new DragMotion();
        Vec3 east = new Vec3(O.x() + 6, O.y(), O.z());
        ImplosionDrag one = new ImplosionDrag(O, PHAGE, m);
        ImplosionDrag two = new ImplosionDrag(east, PHAGE, m);
        Vec3 v = Vec3.ZERO;
        ImplosionDrag.Sighting s = mob(A, 2.0, v);   // 2 west of O, so 8 west of the second origin: outside it
        one.tick(0, List.of(s), Set.of());
        two.tick(0, List.of(s), Set.of());
        ImplosionDrag.Sighting near = mob(A, -2.0, v);   // 2 EAST of O: 4 west of the second, inside both
        one.tick(1, List.of(near), Set.of());
        two.tick(1, List.of(near), Set.of());
        List<ImplosionDrag.Push> p1 = one.tick(2, List.of(near), Set.of());
        List<ImplosionDrag.Push> p2 = two.tick(2, List.of(near), Set.of());
        double a1 = PHAGE.dragStep(O, near.footing(), v).orElseThrow().add().x();
        double a2 = PHAGE.dragStep(east, near.footing(), v).orElseThrow().add().x();
        assertTrue(a1 < 0 && a2 > 0, "each toward its own origin: one west, one east");
        assertEquals(a1, p1.get(0).velocity().x(), 1e-12, "the first send: its own add");
        assertEquals(a1 + a2, p2.get(0).velocity().x(), 1e-12, "the second send carries both");
    }

    // --- the momentum estimate --------------------------------------------------------------------------------------

    @Test
    void aPlayersMomentumIsHisLastTicksMotionTimesItsFriction() {
        DragMotion m = new DragMotion();
        m.observe(A, 10, west(3.0, true, 0.0));
        m.observe(A, 11, new Implosion.Footing(new Vec3(O.x() - 3.2, O.y() + 0.1, O.z() + 0.05),
                PlayerTravelModel.EYE, true, PlayerTravelModel.STONE, 0.0));
        Vec3 u = m.momentum(A, 11).orElseThrow();
        assertEquals(-0.2 * PlayerTravelModel.M, u.x(), 1e-12, "the ground's friction, the state at the tick's start");
        assertEquals(0.05 * PlayerTravelModel.M, u.z(), 1e-12);
        assertEquals((0.1 - Implosion.GRAVITY) * Implosion.VERTICAL_DRAG, u.y(), 1e-12, "(dy - 0.08) x 0.98f");
        m.observe(A, 11, west(9.0, true, 0.0));
        assertEquals(u, m.momentum(A, 11).orElseThrow(), "a second sighting in the same tick changes nothing");
    }

    @Test
    void noEstimateWithoutBothTicksOrAcrossATeleport() {
        DragMotion m = new DragMotion();
        m.observe(A, 10, west(3.0, true, 0.0));
        assertTrue(m.momentum(A, 10).isEmpty(), "one sighting");
        m.observe(A, 12, west(3.1, true, 0.0));
        assertTrue(m.momentum(A, 12).isEmpty(), "a tick missed");
        m.observe(A, 13, west(3.1 + DragMotion.MAX_STEP + 0.01, true, 0.0));
        assertTrue(m.momentum(A, 13).isEmpty(), "farther than a step: a teleport");
        assertTrue(m.momentum(A, 14).isEmpty(), "asked for a tick it was not seen");
    }

    @Test
    void aBodyNobodyDragsIsForgotten() {
        DragMotion m = new DragMotion();
        m.observe(A, 10, west(3.0, true, 0.0));
        m.observe(B, 13, west(3.0, true, 0.0));
        m.prune(13);
        assertEquals(1, m.size(), "A last seen at 10 < 13 - 2");
    }
}
