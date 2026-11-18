package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE DRAG (Ben, 2026-11-18: "1. yes 2. yes 3. yes 4. yes" to the seat's four points), ADDITIVE ON A PLAYER (the seat's
 * ruling of 2026-11-18: no design may need the server to know his momentum). The four outcomes are the spec, each run
 * here against {@link PlayerTravelModel}, the client's own travel READ from the jar, with the server's view of him
 * LAGGING 0 to 4 ticks:
 * <ol type="a">
 *   <li>STANDING STILL at 2, 3, 4 and 5 blocks from a stage-5 blast: dragged in and never past the margin.</li>
 *   <li>WALKING DIRECTLY AWAY from 3 blocks: still nearer the origin at the end than at the start.</li>
 *   <li>SPRINTING DIRECTLY AWAY from the outer part of the reach: outside the reach before the second is up.</li>
 *   <li>NEVER PAST THE MARGIN: for a victim not himself moving toward the origin, at a lag within
 *       {@link Implosion#LAG_ALLOWANCE_TICKS}; inside the margin nothing is ever added.</li>
 * </ol>
 * The Phage's use: reach 6 (stage 5), no pull and no lift, explosion knockback resistance, 20 ticks, drag 0.33 BLOCKS
 * PER TICK at the origin (the seat's number). Every expected figure that is not a bound is computed here from the
 * shape's own formula, or EXECUTED (the comment says which); none was worked by hand.
 */
class ImplosionDragTest {

    static final Implosion PHAGE = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 0.0, 0.0,
            Implosion.Resistance.EXPLOSION_KNOCKBACK, 20, 0.33);
    private static final Vec3 O = PlayerTravelModel.ORIGIN;
    private static final double SLIDE = 1.0 / (1.0 - PlayerTravelModel.M);   // travelFactor(0) on stone
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes());
    private static final UUID B = UUID.nameUUIDFromBytes("b".getBytes());
    private static final double GAIN = Implosion.holdGain(true, PlayerTravelModel.STONE);

    private static Implosion.Footing west(double h, boolean onGround, double resistance) {
        return new Implosion.Footing(new Vec3(O.x() - h, O.y(), O.z()), PlayerTravelModel.EYE, onGround,
                PlayerTravelModel.STONE, resistance);
    }

    private static Implosion.Footing westOn(double h, float friction) {
        return new Implosion.Footing(new Vec3(O.x() - h, O.y(), O.z()), PlayerTravelModel.EYE, true, friction, 0.0);
    }

    // --- THE ADD ON A PLAYER: a function of his distance and footing alone ------------------------------------------

    @Test
    void farOutThePlayersAddIsTheDragsStrengthTowardTheOrigin() {
        Implosion.DragStep s = PHAGE.addStep(O, west(5.0, true, 0.0)).orElseThrow();
        double strength = 0.33 * (1 - 5.0 / 6.0);
        assertEquals(strength, s.strength(), 1e-12, "drag x (1 - d / reach)");
        assertEquals(GAIN * (5.0 - 0.5), s.cap(), 1e-12, "the hold: gain x (h - margin)");
        assertTrue(s.cap() > strength, "at 5 the strength is the smaller");
        assertEquals(strength, s.added(), 1e-12);
        assertFalse(s.held());
        assertEquals(strength, s.add().x(), 1e-12, "toward the origin: +x, he stands west of it");
        assertEquals(0.0, s.add().y(), 0.0, "a drag has no vertical");
        assertEquals(0.0, s.add().z(), 1e-12);
        assertTrue(Double.isNaN(s.toward()), "his momentum is never read, estimated or rebuilt");
    }

    @Test
    void nearerTheHoldIsTheSmallerAndTheMarginLimitsTheAdd() {
        Implosion.DragStep s = PHAGE.addStep(O, west(3.0, true, 0.0)).orElseThrow();
        assertEquals(0.33 * 0.5, s.strength(), 1e-12);
        assertEquals(GAIN * 2.5, s.cap(), 1e-12);
        assertEquals(GAIN * 2.5, s.added(), 1e-12, "the hold binds at 3");
        assertTrue(s.held());
    }

    /** THE ONLY GUARD of the add's {@code (1 - resistance)} on a player: see the mutation list in the gate. */
    @Test
    void theResistanceCutsTheStrength() {
        Implosion.DragStep s = PHAGE.addStep(O, west(5.0, true, 0.5)).orElseThrow();
        assertEquals(0.33 * (1 - 5.0 / 6.0) * 0.5, s.strength(), 1e-12, "x (1 - resistance)");
        assertEquals(s.strength(), s.added(), 1e-12);
    }

    @Test
    void beyondTheReachThereIsNoStepAndAtTheReachTheStrengthIsNothing() {
        assertTrue(PHAGE.addStep(O, west(6.05, true, 0.0)).isEmpty());
        assertEquals(0.0, PHAGE.addStep(O, west(6.0, true, 0.0)).orElseThrow().added(), 1e-12);
    }

    @Test
    void atOrInsideTheMarginNothingIsAddedEver() {
        for (double h : new double[] {0.5, 0.3, 0.0}) {
            Implosion.DragStep s = PHAGE.addStep(O, west(h, true, 0.0)).orElseThrow();
            assertEquals(0.0, s.added(), 0.0, "at " + h);
            assertTrue(s.held(), "at " + h);
            assertEquals(Vec3.ZERO, s.add(), "at " + h);
        }
    }

    // --- THE HOLD'S GAIN: the largest that never carries a still victim past the margin, lag within the allowance --

    /**
     * EXECUTED (scratch {@code gain.jsh}, 2026-11-18, the same impulse response): stone 0.04067, slime 0.01609, ice
     * 0.00278, air 0.00195 at an allowance of 2 ticks. The ice and the air are why the gain is the FOOTING'S: a slower
     * decay carries an add much farther.
     */
    @Test
    void theGainIsTheFootingsAndItsFiguresAreTheExecutedOnes() {
        assertEquals(2, Implosion.LAG_ALLOWANCE_TICKS);
        assertEquals(0.04067, GAIN, 5e-6, "stone");
        assertEquals(0.01609, Implosion.holdGain(true, 0.8f), 5e-6, "slime");
        assertEquals(0.00278, Implosion.holdGain(true, 0.98f), 5e-6, "ice");
        assertEquals(0.00195, Implosion.holdGain(false, PlayerTravelModel.STONE), 5e-6, "in the air");
        assertEquals(GAIN, Implosion.holdGain(true, PlayerTravelModel.STONE), 0.0, "the same answer twice");
    }

    /** TIGHT: at the gain no lag within the allowance overshoots; one percent more, one of them does. */
    @Test
    void theGainIsTheLargestThatHolds() {
        double m = PlayerTravelModel.M;
        for (int lag = 0; lag <= Implosion.LAG_ALLOWANCE_TICKS; lag++) {
            assertTrue(Implosion.holdsWithoutOvershoot(GAIN, m, lag), "lag " + lag);
        }
        boolean someFails = false;
        for (int lag = 0; lag <= Implosion.LAG_ALLOWANCE_TICKS; lag++) {
            someFails |= !Implosion.holdsWithoutOvershoot(GAIN * 1.01, m, lag);
        }
        assertTrue(someFails, "one percent more and a lag within the allowance overshoots");
        assertFalse(Implosion.holdsWithoutOvershoot(GAIN, m, Implosion.LAG_ALLOWANCE_TICKS + 1),
                "WHAT IT DOES NOT GUARANTEE: one tick more lag than the allowance can overshoot");
    }

    /**
     * THE CAP WITHOUT HIS MOMENTUM, SWEPT: every drag strength and reach the shape could be authored with, every start
     * distance, every lag within the allowance, and seeded jitter within it: a still victim is never carried past the
     * margin, at any tick.
     */
    @Test
    void aStillVictimIsNeverCarriedPastTheMarginAtAnyLagWithinTheAllowance() {
        for (double drag : new double[] {0.1, 0.28, 0.33, 0.6, 1.0, 5.0}) {
            for (double reach : new double[] {2, 3, 4, 6}) {
                Implosion shape = new Implosion(reach, Implosion.Falloff.VANILLA_EXPLOSION, 0, 0,
                        Implosion.Resistance.NONE, 20, drag);
                for (double h0 = 0.55; h0 <= reach; h0 += 0.05) {
                    for (int lag = 0; lag <= Implosion.LAG_ALLOWANCE_TICKS; lag++) {
                        PlayerTravelModel.Run r = PlayerTravelModel.run(shape, h0, 0.0, 0.0, lag);
                        assertTrue(r.minimum() >= 0.5 - 1e-9, "drag " + drag + " reach " + reach + " from " + h0
                                + " lag " + lag + ": nearest " + r.minimum());
                    }
                }
            }
        }
        for (long seed = 0; seed < 200; seed++) {
            Random rnd = new Random(seed);
            int[] lags = new int[200];
            for (int i = 0; i < lags.length; i++) lags[i] = rnd.nextInt(Implosion.LAG_ALLOWANCE_TICKS + 1);
            for (double h0 = 0.6; h0 <= 6.0; h0 += 0.2) {
                PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, 0.0, 0.0, n -> lags[n], null, 0);
                assertTrue(r.minimum() >= 0.5 - 1e-9, "jitter seed " + seed + " from " + h0 + ": " + r.minimum());
            }
        }
    }

    /** WHAT IT DOES NOT GUARANTEE, PINNED: a player moving INTO the origin passes it by his own legs. */
    @Test
    void aPlayerSprintingIntoTheOriginPassesItByHisOwnLegs() {
        PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, 3.0, -PlayerTravelModel.SPRINT, 0.0, 0);
        assertTrue(r.minimum() < 0.0, "he runs past the centre: " + r.minimum());
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
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 0, r, 0, 0.33))
                .getMessage().contains("duration_ticks"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 1.25, 0, r, 20, 0.33))
                .getMessage().contains("pull must be 0"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 1.0, r, 20, 0.33))
                .getMessage().contains("lift must be 0"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 0, r, 20, 0.0))
                .getMessage().contains("drag above 0"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 1.25, 0, r, 1, 0.33))
                .getMessage().contains("single pull"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Implosion(6, f, 0, 0, r, 20, -1))
                .getMessage().contains("drag must be 0 or more"));
        assertThrows(IllegalArgumentException.class, () -> new ImplosionDrag(O,
                new Implosion(6, f, 1.25, 0, r), new DragMotion()), "a single pull is not a drag");
    }

    // --- THE FOUR OUTCOMES, against the client's own travel, at every lag 0 to 4 ------------------------------------

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
     * a. STANDING STILL: from 2, 3, 4 and 5 he is dragged in, every tick of the second, never past the margin within
     * the allowance. The figures (where he ends the second and rests, at each lag) are the gate's, EXECUTED by
     * {@link #theOutcomeFiguresAsExecuted}.
     */
    @Test
    void aStandingStillHeIsDraggedInFromTwoThreeFourAndFive() {
        for (double h0 : new double[] {2.0, 3.0, 4.0, 5.0}) {
            for (int lag = 0; lag <= 4; lag++) {
                PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, 0.0, 0.0, lag);
                assertTrue(r.rest() < h0 - 0.75 * (h0 - 0.5), "from " + h0 + " lag " + lag
                        + ": three quarters of the way to the margin at least, rests at " + r.rest());
                assertEquals(20, r.summary().ticks(), "from " + h0 + ": pulled every tick of the second");
                if (lag <= Implosion.LAG_ALLOWANCE_TICKS) {
                    assertTrue(r.minimum() >= 0.5 - 1e-9, "from " + h0 + " lag " + lag + ": " + r.minimum());
                }
            }
        }
    }

    /**
     * b. WALKING AWAY from 3: slowed, not cancelled, from rest and already at walking speed, at lags 0 to 3. NARROWLY:
     * he ends 2.92 to 2.97 (EXECUTED, {@link #theOutcomeFiguresAsExecuted}), and at a lag of 4, already walking, he
     * ends 3.001, no nearer (pinned there). Near the margin the hold is small and his walking is nearly its match.
     */
    @Test
    void bWalkingAwayFromThreeHeEndsTheSecondNearerThanHeStarted() {
        for (int lag = 0; lag <= 3; lag++) {
            PlayerTravelModel.Run fromRest = PlayerTravelModel.run(PHAGE, 3.0, PlayerTravelModel.WALK, 0.0, lag);
            PlayerTravelModel.Run walking = PlayerTravelModel.run(PHAGE, 3.0, PlayerTravelModel.WALK,
                    PlayerTravelModel.terminal(PlayerTravelModel.WALK) * PlayerTravelModel.M, lag);
            assertTrue(fromRest.end() < 3.0, "lag " + lag + ": from rest he ends the second at " + fromRest.end());
            assertTrue(walking.end() < 3.0, "lag " + lag + ": already walking, at " + walking.end());
        }
    }

    /**
     * c. SPRINTING AWAY from 4.4, 4.5, 5 and 5.9 of 6, from rest: out of the reach before the second is up, at every lag
     * 0 to 4. From 3.5 he is NOT, at any. Where it starts to work moves with the lag (EXECUTED,
     * {@link #theOutcomeFiguresAsExecuted}): from 3.9 at lag 0, 4.0 at 1, 4.1 at 2, 4.2 at 3, 4.3 at 4; at drag 0.28
     * and lag 1 it was 3.8. The sprint-out distance moved outward with 0.33, as the seat said.
     */
    @Test
    void cSprintingAwayFromTheOuterPartHeLeavesTheReachInTheSecond() {
        for (double h0 : new double[] {4.4, 4.5, 5.0, 5.9}) {
            for (int lag = 0; lag <= 4; lag++) {
                PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, PlayerTravelModel.SPRINT, 0.0, lag);
                assertEquals(ImplosionDrag.Outcome.LEFT, r.summary().outcome(), "from " + h0 + " lag " + lag);
                assertTrue(r.exitTick() >= 0 && r.exitTick() <= 20, "from " + h0 + " lag " + lag);
            }
        }
        for (int lag = 0; lag <= 4; lag++) {
            PlayerTravelModel.Run held = PlayerTravelModel.run(PHAGE, 3.5, PlayerTravelModel.SPRINT, 0.0, lag);
            assertTrue(held.exitTick() < 0, "lag " + lag + ": from 3.5 a sprinter is still inside, " + held.end());
        }
    }

    /** d. NEVER PAST THE MARGIN: from 1, 2 and 3, within the allowance, at no tick nearer than the margin. */
    @Test
    void dReachingTheMarginHeIsNeverCarriedThrough() {
        for (double h0 : new double[] {1.0, 2.0, 3.0}) {
            for (int lag = 0; lag <= Implosion.LAG_ALLOWANCE_TICKS; lag++) {
                PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, h0, 0.0, 0.0, lag);
                assertTrue(r.minimum() >= 0.5 - 1e-9, "from " + h0 + " lag " + lag + ": " + r.minimum());
            }
        }
    }

    /**
     * d's other half, and IM4R2's miss closed: inside the margin nothing is ever sent, even to a victim drifting
     * OUTWARD (the old cap braked his outward drift; this one never reads it).
     */
    @Test
    void dInsideTheMarginNothingIsEverSentEvenToOneDriftingOut() {
        PlayerTravelModel.Run still = PlayerTravelModel.run(PHAGE, 0.3, 0.0, 0.0, 0);
        assertEquals(0, still.pushes());
        assertEquals(0.3, still.rest(), 1e-12);
        PlayerTravelModel.Run drifting = PlayerTravelModel.run(PHAGE, 0.3, 0.0, 0.05, 0);
        assertEquals(0, drifting.pushes(), "drifting out: still nothing sent");
        assertEquals(0, drifting.summary().sent());
        assertEquals(ImplosionDrag.Outcome.HELD, drifting.summary().outcome());
    }

    /**
     * THE GATE'S FIGURES, EXECUTED AND PINNED: outcome a's ends of the second and rest points at lag 0 and 4, the
     * consistency row's spread from 4, b's ends and c's exit ticks. The values were printed by this same arithmetic
     * (scratch {@code figures.jsh} over the compiled test classes, 2026-11-18) and are copied into
     * {@code GATE-drag-additive.md}; a change to the rule moves them and reddens this row first.
     */
    @Test
    void theOutcomeFiguresAsExecuted() {
        // a, standing still at a stage 5: {end of the second as the server sees it, rest}, lag 0 to 4.
        double[][][] a = {
            {{0.727, 0.679}, {0.697, 0.631}, {0.659, 0.578}, {0.610, 0.512}, {0.561, 0.429}},   // from 2
            {{0.878, 0.796}, {0.829, 0.715}, {0.764, 0.626}, {0.683, 0.515}, {0.602, 0.379}},   // from 3
            {{1.056, 0.935}, {1.008, 0.832}, {0.947, 0.711}, {0.888, 0.565}, {0.868, 0.389}},   // from 4
            {{1.658, 1.397}, {1.796, 1.346}, {2.028, 1.293}, {2.385, 1.267}, {2.795, 1.281}},   // from 5
        };
        double[] from = {2.0, 3.0, 4.0, 5.0};
        for (int i = 0; i < from.length; i++) {
            for (int lag = 0; lag <= 4; lag++) {
                PlayerTravelModel.Run r = PlayerTravelModel.run(PHAGE, from[i], 0.0, 0.0, lag);
                assertEquals(a[i][lag][0], r.end(), 5e-4, "a from " + from[i] + " lag " + lag + ": end");
                assertEquals(a[i][lag][1], r.rest(), 5e-4, "a from " + from[i] + " lag " + lag + ": rest");
            }
        }
        // b, walking away from 3 (from rest; already walking), lag 0 to 4.
        double[][] b = {{2.920, 2.964}, {2.918, 2.964}, {2.916, 2.964}, {2.917, 2.972}, {2.928, 3.001}};
        for (int lag = 0; lag <= 4; lag++) {
            assertEquals(b[lag][0], PlayerTravelModel.run(PHAGE, 3.0, PlayerTravelModel.WALK, 0.0, lag).end(), 5e-4);
            assertEquals(b[lag][1], PlayerTravelModel.run(PHAGE, 3.0, PlayerTravelModel.WALK,
                    PlayerTravelModel.terminal(PlayerTravelModel.WALK) * PlayerTravelModel.M, lag).end(), 5e-4);
        }
        // c, sprinting from rest: the nearest start, to a tenth, that gets out in the second, lag 0 to 4.
        double[] out = {3.9, 4.0, 4.1, 4.2, 4.3};
        for (int lag = 0; lag <= 4; lag++) {
            assertTrue(PlayerTravelModel.run(PHAGE, out[lag], PlayerTravelModel.SPRINT, 0.0, lag).exitTick() >= 0);
            assertTrue(PlayerTravelModel.run(PHAGE, out[lag] - 0.1, PlayerTravelModel.SPRINT, 0.0, lag).exitTick() < 0);
        }
        Implosion old = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 0.0, 0.0,
                Implosion.Resistance.EXPLOSION_KNOCKBACK, 20, 0.28);
        assertTrue(PlayerTravelModel.run(old, 3.8, PlayerTravelModel.SPRINT, 0.0, 1).exitTick() >= 0, "0.28: 3.8");
        assertTrue(PlayerTravelModel.run(old, 3.7, PlayerTravelModel.SPRINT, 0.0, 1).exitTick() < 0, "0.28: not 3.7");
    }

    // --- the session: who, when, and how it ends --------------------------------------------------------------------

    private static ImplosionDrag.Sighting mob(UUID id, double h, Vec3 velocity) {
        return new ImplosionDrag.Sighting(id, west(h, true, 0.0), velocity);
    }

    private static ImplosionDrag.Sighting player(UUID id, double h) {
        return new ImplosionDrag.Sighting(id, west(h, true, 0.0), null);
    }

    @Test
    void tickZeroOnlyLooksAndAMobIsSetToItsOwnVelocityPlusTheAdd() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        Vec3 v = new Vec3(0.01, -0.0784, 0.02);
        assertTrue(d.tick(0, List.of(mob(A, 3.0, v)), Set.of()).isEmpty(), "the blast's own tick pulls nobody");
        List<ImplosionDrag.Push> one = d.tick(1, List.of(mob(A, 3.0, v)), Set.of());
        assertEquals(1, one.size());
        assertEquals(ImplosionDrag.Mode.SET, one.get(0).mode(), "the server owns a mob's motion");
        double add = PHAGE.dragStep(O, west(3.0, true, 0.0), v).orElseThrow().added();
        assertEquals(0.01 + add, one.get(0).velocity().x(), 1e-12);
        assertEquals(-0.0784, one.get(0).velocity().y(), 0.0, "the server's own vertical, untouched");
        assertEquals(0.02, one.get(0).velocity().z(), 1e-12);
    }

    /**
     * A PLAYER IS ONLY EVER ADDED TO, AND THE ADD HAS NO HISTORY: the same footing on two ticks, whatever came before,
     * gives the same push, and it is exactly {@link Implosion#addStep}'s. THE ONLY GUARD that no momentum reaches a
     * player's push: see the mutation list in the gate.
     */
    @Test
    void aPlayersPushIsTheAddAloneWithNoMomentumInIt() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        d.tick(0, List.of(player(A, 3.0)), Set.of());
        List<ImplosionDrag.Push> first = d.tick(1, List.of(player(A, 3.0)), Set.of());
        d.tick(2, List.of(player(A, 4.2)), Set.of());
        List<ImplosionDrag.Push> again = d.tick(3, List.of(player(A, 3.0)), Set.of());
        Vec3 add = PHAGE.addStep(O, west(3.0, true, 0.0)).orElseThrow().add();
        assertEquals(ImplosionDrag.Mode.ADD, first.get(0).mode());
        assertEquals(add, first.get(0).velocity());
        assertEquals(add, again.get(0).velocity(), "a tick at 4.2 between changes nothing");
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

    /** No momentum is needed now, so a player who walks into the reach midway is pulled from his FIRST sighting. */
    @Test
    void aPlayerFirstSeenMidwayIsPulledFromHisFirstSighting() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        for (int t = 0; t < 5; t++) d.tick(t, List.of(), Set.of());
        assertEquals(1, d.tick(5, List.of(player(B, 3.0)), Set.of()).size());
        assertEquals(1, d.summaries().get(0).ticks());
    }

    @Test
    void oneWhoLeavesTheReachIsNoLongerPulledAndEndsLeft() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        d.tick(0, List.of(player(A, 3.0)), Set.of());
        d.tick(1, List.of(player(A, 3.0)), Set.of());
        assertTrue(d.tick(2, List.of(player(A, 8.0)), Set.of()).isEmpty(), "outside the reach: nothing");
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
     * Two drags on one MOB in one tick: the second set carries both adds on the first's base (one velocity). THE ONLY
     * GUARD of the tally: MUTTALLYSUM (2026-11-18) reddened this row and no other.
     */
    @Test
    void twoDragsOnOneMobInOneTickAddToOneVelocity() {
        DragMotion m = new DragMotion();
        Vec3 east = new Vec3(O.x() + 6, O.y(), O.z());
        ImplosionDrag one = new ImplosionDrag(O, PHAGE, m);
        ImplosionDrag two = new ImplosionDrag(east, PHAGE, m);
        Vec3 v = Vec3.ZERO;
        ImplosionDrag.Sighting near = mob(A, -2.0, v);   // 2 EAST of O: 4 west of the second, inside both
        one.tick(0, List.of(near), Set.of());
        two.tick(0, List.of(near), Set.of());
        List<ImplosionDrag.Push> p1 = one.tick(1, List.of(near), Set.of());
        List<ImplosionDrag.Push> p2 = two.tick(1, List.of(near), Set.of());
        double a1 = PHAGE.dragStep(O, near.footing(), v).orElseThrow().add().x();
        double a2 = PHAGE.dragStep(east, near.footing(), v).orElseThrow().add().x();
        assertTrue(a1 < 0 && a2 > 0, "each toward its own origin: one west, one east");
        assertEquals(a1, p1.get(0).velocity().x(), 1e-12, "the first set: its own add");
        assertEquals(a1 + a2, p2.get(0).velocity().x(), 1e-12, "the second set carries both");
    }

    /**
     * Two drags on one PLAYER in one tick: each sends ITS OWN add, and the client adds both. A tally here would send
     * the first add twice. Each is cut by its own margin only (the two-origin case stays UNRULED).
     */
    @Test
    void twoDragsOnOnePlayerInOneTickEachSendItsOwnAdd() {
        DragMotion m = new DragMotion();
        Vec3 east = new Vec3(O.x() + 6, O.y(), O.z());
        ImplosionDrag one = new ImplosionDrag(O, PHAGE, m);
        ImplosionDrag two = new ImplosionDrag(east, PHAGE, m);
        ImplosionDrag.Sighting near = player(A, -2.0);
        one.tick(0, List.of(near), Set.of());
        two.tick(0, List.of(near), Set.of());
        List<ImplosionDrag.Push> p1 = one.tick(1, List.of(near), Set.of());
        List<ImplosionDrag.Push> p2 = two.tick(1, List.of(near), Set.of());
        assertEquals(PHAGE.addStep(O, near.footing()).orElseThrow().add(), p1.get(0).velocity());
        assertEquals(PHAGE.addStep(east, near.footing()).orElseThrow().add(), p2.get(0).velocity(),
                "its own add, not the sum");
    }

    /** The instrument's window: every sighting of the last tick, pulled or not, with its step. */
    @Test
    void theLastTicksStepsNameEverySightingAndItsAdd() {
        ImplosionDrag d = new ImplosionDrag(O, PHAGE, new DragMotion());
        d.tick(0, List.of(player(A, 3.0)), Set.of());
        assertEquals(1, d.steps().size());
        assertEquals(null, d.steps().get(0).step(), "tick 0 only looks");
        d.tick(1, List.of(player(A, 3.0), player(B, 9.0)), Set.of());
        assertEquals(1, d.steps().size(), "B was never inside the reach: not a victim");
        ImplosionDrag.Step s = d.steps().get(0);
        assertEquals(A, s.id());
        assertEquals(1, s.tick());
        assertEquals(3.0, s.distance(), 1e-12);
        assertEquals(PHAGE.addStep(O, west(3.0, true, 0.0)).orElseThrow(), s.step());
    }

    @Test
    void aTallyNobodyUsesIsForgotten() {
        DragMotion m = new DragMotion();
        m.tally(A, 10, Vec3.ZERO, Vec3.ZERO);
        m.tally(B, 13, Vec3.ZERO, Vec3.ZERO);
        m.prune(13);
        assertEquals(1, m.size(), "A last tallied at 10 < 13 - 2");
    }
}
