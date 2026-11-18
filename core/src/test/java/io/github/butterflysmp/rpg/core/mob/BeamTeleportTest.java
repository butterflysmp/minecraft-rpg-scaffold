package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the Knell lands after its beam hits (PLAN-dragon-mobs.md step R6). Every expected coordinate below was
 * EXECUTED in jshell ({@code 8 * Math.cos(Math.toRadians(50))} and the like), never worked by hand.
 *
 * <p>The fixture's target stands at (100, 64, 200) and its Knell 8 blocks due EAST of it, so the Knell's offset is
 * (8, 0) and a RIGHT turn (seen from the target, facing the Knell) swings it toward +z, south.
 */
class BeamTeleportTest {

    /** Ben's 30 and 70, the seat's 5 a side, 3 blocks, sight required. */
    private static final MobTeleport T = new MobTeleport(12, 30, 70, 5, 3, true);
    private static final Vec3 TARGET = new Vec3(100, 64, 200);
    private static final Vec3 KNELL = new Vec3(108, 64, 200);

    /** A world where every point is loaded, fits, is grounded and sees: the first candidate always wins. */
    private static final BeamTeleport.Footing OPEN = footing(p -> true, p -> true, p -> true, p -> true);

    private static BeamTeleport.Footing footing(Predicate<Vec3> loaded, Predicate<Vec3> fits, Predicate<Vec3> grounded,
                                                Predicate<Vec3> sees) {
        return new BeamTeleport.Footing() {
            @Override public boolean loaded(Vec3 f) { return loaded.test(f); }
            @Override public boolean fits(Vec3 f) { return fits.test(f); }
            @Override public boolean grounded(Vec3 f) { return grounded.test(f); }
            @Override public boolean sees(Vec3 f) { return sees.test(f); }
        };
    }

    private static void assertNear(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x(), actual.x(), 1e-9, "x");
        assertEquals(expected.y(), actual.y(), 1e-9, "y");
        assertEquals(expected.z(), actual.z(), 1e-9, "z");
    }

    // --- the trigonometry ----------------------------------------------------------------------------------------

    @Test
    void aFixedSideAndAngleLandsWhereTrigonometrySays() {
        // jshell: 8 * cos(50 deg) = 5.142300877492, 8 * sin(50 deg) = 6.128355544952
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), OPEN);
        assertNear(new Vec3(100 + 5.142300877492, 64, 200 + 6.128355544952), d.landing().orElseThrow());
        assertEquals(1, d.tried().size(), "the first candidate is valid, so only one is tried");
    }

    @Test
    void theLeftSideMirrorsTheRight() {
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.LEFT, 50), OPEN);
        assertNear(new Vec3(100 + 5.142300877492, 64, 200 - 6.128355544952), d.landing().orElseThrow());
    }

    @Test
    void theRadiusIsPreservedAndTheAngleIsTurnedAsDrawn() {
        Vec3 col = BeamTeleport.column(TARGET, new Vec3(103, 70, 204), BeamTeleport.Side.RIGHT, 90);
        // offset (3, 4), radius 5, turned right 90: jshell (-4, 3)
        assertNear(new Vec3(100 - 4, 64, 200 + 3), col);
        assertEquals(5.0, Math.hypot(col.x() - 100, col.z() - 200), 1e-9, "the horizontal radius is unchanged");
        assertEquals(64.0, col.y(), 0.0, "a column is at the TARGET's feet height, not the Knell's");
        // The decision's own radius and bearing, off-axis so that neither x nor z alone equals them.
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, new Vec3(103, 70, 204),
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 90), OPEN);
        assertEquals(5.0, d.radius(), 1e-9);
        assertEquals(Math.toDegrees(Math.atan2(4, 3)), d.bearingDegrees(), 1e-9, "53.13 degrees: south of east");
    }

    @Test
    void theRadiusIsHorizontalOnly() {
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, new Vec3(108, 90, 200),
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), OPEN);
        assertEquals(8.0, d.radius(), 1e-9, "26 blocks of height do not count");
        assertEquals(0.0, d.bearingDegrees(), 1e-9, "due east is bearing 0 from the target");
    }

    @Test
    void bothArcBoundsAreInclusive() {
        // jshell: right 30 -> (6.928203230276, 4.0); right 70 -> (2.736161146605, 7.517540966287)
        assertNear(new Vec3(100 + 6.928203230276, 64, 204),
                BeamTeleport.decide(T, TARGET, KNELL, new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 30), OPEN)
                        .landing().orElseThrow());
        assertNear(new Vec3(100 + 2.736161146605, 64, 200 + 7.517540966287),
                BeamTeleport.decide(T, TARGET, KNELL, new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 70), OPEN)
                        .landing().orElseThrow());
        assertEquals(List.of(30.0, 30.0, 43.333333333333336, 56.66666666666667, 70.0), BeamTeleport.angles(T, 30),
                "the spread includes both bounds");
    }

    // --- the order ----------------------------------------------------------------------------------------------

    @Test
    void theOrderIsTheDrawnAngleThenTheArcSpreadEvenly_thenTheOtherSideTheSame() {
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 51.25), footing(p -> true, p -> false, p -> true, p -> true));
        List<String> order = new ArrayList<>();
        for (BeamTeleport.Verdict v : d.tried()) order.add(v.candidate().side() + " " + v.candidate().angle());
        assertEquals(List.of("RIGHT 51.25", "RIGHT 30.0", "RIGHT 43.333333333333336", "RIGHT 56.66666666666667",
                "RIGHT 70.0", "LEFT 51.25", "LEFT 30.0", "LEFT 43.333333333333336", "LEFT 56.66666666666667",
                "LEFT 70.0"), order, "five a side (T-a), the chosen side's all before the other's");
    }

    @Test
    void theChosenSideIsExhaustedBeforeTheOther_andTheFirstValidWins() {
        // Every RIGHT spot (z > 200) is walled; every LEFT spot is open.
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), footing(p -> true, p -> p.z() < 200, p -> true, p -> true));
        assertEquals(6, d.tried().size(), "five refused on the right, then the first on the left");
        for (int i = 0; i < 5; i++) assertEquals(BeamTeleport.Outcome.NO_FOOTING, d.tried().get(i).outcome());
        assertEquals(BeamTeleport.Outcome.OK, d.tried().get(5).outcome());
        assertNear(new Vec3(100 + 5.142300877492, 64, 200 - 6.128355544952), d.landing().orElseThrow());
    }

    @Test
    void aLaterValidSpotOnTheChosenSideWinsOverTheOtherSide() {
        // Only the right side's 30-degree spot (x > 106) is open.
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), footing(p -> true, p -> p.x() > 106, p -> true, p -> true));
        assertEquals(2, d.tried().size());
        assertNear(new Vec3(100 + 6.928203230276, 64, 204), d.landing().orElseThrow());
    }

    @Test
    void noneValidStaysWithEveryCandidateRefused() {
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.LEFT, 40), footing(p -> true, p -> false, p -> true, p -> true));
        assertTrue(d.landing().isEmpty());
        assertEquals(10, d.tried().size());
        assertEquals(BeamTeleport.Side.LEFT, d.tried().get(0).candidate().side());
    }

    // --- the level search -------------------------------------------------------------------------------------

    @Test
    void theHeightsAreNearestFirstUpsBeforeDownsOutToTheTolerance() {
        assertEquals(List.of(0, 1, -1, 2, -2, 3, -3), BeamTeleport.heights(3));
        assertEquals(List.of(0), BeamTeleport.heights(0));
    }

    @Test
    void theNearestStandingHeightWins() {
        // Ground two blocks down (a pit) AND three up (a ledge): the pit is nearer.
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50),
                footing(p -> true, p -> true, p -> p.y() == 62 || p.y() == 67, p -> true));
        assertEquals(62.0, d.landing().orElseThrow().y(), 0.0);
        BeamTeleport.Verdict v = d.tried().get(0);
        assertEquals(0, v.blocked());
        assertEquals(0, v.ungrounded(), "an OK verdict counts nothing");
    }

    // --- F19 (PLAN-dragon-mobs.md section 8): the search is anchored on the WHOLE block the target's feet are in --------

    /** A flat floor whose top is y = 64: a body fits at 64 and above, and stands only at exactly 64. */
    private static final BeamTeleport.Footing FLOOR_64 =
            footing(p -> true, p -> p.y() >= 64, p -> p.y() == 64, p -> true);

    @Test
    void aTargetOnASlabStillGivesALandingOnTheFloorBesideIt() {
        // F19: a target on a bottom slab stands at 64.5. Anchored there, every height tried is x.5: 64.5 fits with no
        // ground, 63.5 does not fit, so all seven were NO_FOOTING and the Knell never moved. Anchored on floor(64.5).
        BeamTeleport.Decision d = BeamTeleport.decide(T, new Vec3(100, 64.5, 200), new Vec3(108, 64, 200),
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), FLOOR_64);
        assertEquals(64.0, d.landing().orElseThrow().y(), 0.0);
        assertEquals(1, d.tried().size(), "the first candidate stands on the floor");
    }

    @Test
    void aTargetMidJumpStillGivesALandingOnTheFloorBelowIt() {
        // F19: the move falls due while the target is 0.83 into a jump (64.83, a fraction that is no slab's).
        BeamTeleport.Decision d = BeamTeleport.decide(T, new Vec3(100, 64.83, 200), new Vec3(108, 64, 200),
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), FLOOR_64);
        assertEquals(64.0, d.landing().orElseThrow().y(), 0.0);
    }

    @Test
    void floatingPointDriftJustBelowAFloorAnchorsOnThatFloorNotTheBlockBelow() {
        // The blink's ANCHOR_SNAP, now shared: 63.9999999999 is the floor at 64, not 0.9999999999 into block 63.
        // Tolerance 0, so ONLY the anchored height is tried: anchored on 63 it would not fit, and nothing would land.
        MobTeleport flat = new MobTeleport(12, 30, 70, 5, 0, true);
        BeamTeleport.Decision d = BeamTeleport.decide(flat, new Vec3(100, 63.9999999999, 200),
                new Vec3(108, 64, 200), new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), FLOOR_64);
        assertEquals(64.0, d.landing().orElseThrow().y(), 0.0);
    }

    @Test
    void theCostOfTheAnchorASlabTopIsNeverTried() {
        // STATED COST (as the blink's): a floor made only of slab tops (stands at exactly 64.5) is never landed on,
        // because every height tried from floor(64.5) = 64 is whole. Pinned so the cost is a decision, not a surprise.
        BeamTeleport.Decision d = BeamTeleport.decide(T, new Vec3(100, 64.5, 200), new Vec3(108, 64.5, 200),
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50),
                footing(p -> true, p -> p.y() >= 64.5, p -> p.y() == 64.5, p -> true));
        assertTrue(d.landing().isEmpty());
    }

    @Test
    void aRaisedStepWithinTheToleranceIsStoodOn() {
        // A step two high: the body does not fit at 64 or 65, the floor stops 63 down, and 66 stands on the step.
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50),
                footing(p -> true, p -> p.y() >= 66, p -> p.y() == 66, p -> true));
        assertEquals(66.0, d.landing().orElseThrow().y(), 0.0);
    }

    @Test
    void groundBeyondTheToleranceIsRefusedAndCounted() {
        // Ground only 4 down: every height within 3 fits and is ungrounded.
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50),
                footing(p -> true, p -> true, p -> p.y() == 60, p -> true));
        assertTrue(d.landing().isEmpty());
        BeamTeleport.Verdict v = d.tried().get(0);
        assertEquals(BeamTeleport.Outcome.NO_FOOTING, v.outcome());
        assertEquals(0, v.blocked());
        assertEquals(7, v.ungrounded(), "all seven heights fit and none has ground");
    }

    @Test
    void theToleranceIsTheTeleportsOwn() {
        MobTeleport four = new MobTeleport(12, 30, 70, 5, 4, true);
        BeamTeleport.Decision d = BeamTeleport.decide(four, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50),
                footing(p -> true, p -> true, p -> p.y() == 60, p -> true));
        assertEquals(60.0, d.landing().orElseThrow().y(), 0.0, "at a tolerance of 4, 4 down is in reach");
    }

    // --- the other checks -------------------------------------------------------------------------------------

    @Test
    void aSpotWithGroundButNoSightIsRefusedBySight() {
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50),
                footing(p -> true, p -> true, p -> p.y() == 64, p -> p.x() > 106));
        BeamTeleport.Verdict first = d.tried().get(0);
        assertEquals(BeamTeleport.Outcome.NO_SIGHT, first.outcome());
        assertEquals(64.0, first.landing().orElseThrow().y(), 0.0, "the refused footing is named");
        assertNear(new Vec3(100 + 6.928203230276, 64, 204), d.landing().orElseThrow());
    }

    @Test
    void withSightNotRequiredTheSameSpotIsTaken() {
        MobTeleport blind = new MobTeleport(12, 30, 70, 5, 3, false);
        BeamTeleport.Decision d = BeamTeleport.decide(blind, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), footing(p -> true, p -> true, p -> true, p -> false));
        assertEquals(BeamTeleport.Outcome.OK, d.tried().get(0).outcome());
    }

    @Test
    void anUnloadedSpotIsRefusedBeforeAnythingElseIsAsked() {
        List<String> asked = new ArrayList<>();
        BeamTeleport.Footing f = new BeamTeleport.Footing() {
            @Override public boolean loaded(Vec3 p) { return false; }
            @Override public boolean fits(Vec3 p) { asked.add("fits"); return true; }
            @Override public boolean grounded(Vec3 p) { asked.add("grounded"); return true; }
            @Override public boolean sees(Vec3 p) { asked.add("sees"); return true; }
        };
        BeamTeleport.Decision d = BeamTeleport.decide(T, TARGET, KNELL,
                new BeamTeleport.Draw(BeamTeleport.Side.RIGHT, 50), f);
        assertEquals(BeamTeleport.Outcome.UNLOADED, d.tried().get(0).outcome());
        assertEquals(List.of(), asked, "an unloaded point reads as free (F-T), so nothing else is trusted");
    }

    @Test
    void spotsPerSideIsTheTeleportsOwn() {
        MobTeleport three = new MobTeleport(12, 30, 70, 3, 3, true);
        assertEquals(List.of(44.0, 30.0, 70.0), BeamTeleport.angles(three, 44));
        MobTeleport one = new MobTeleport(12, 30, 70, 1, 3, true);
        assertEquals(List.of(44.0), BeamTeleport.angles(one, 44));
        MobTeleport two = new MobTeleport(12, 30, 70, 2, 3, true);
        assertEquals(List.of(44.0, 50.0), BeamTeleport.angles(two, 44), "one more spot: the arc's middle");
    }

    // --- the injected source -----------------------------------------------------------------------------------

    @Test
    void theProductionSourceDrawsBothSidesAndAnglesInsideTheArcIncludingNearItsEnds() {
        BeamTeleport.Source s = BeamTeleport.uniform(new Random(20261117L));
        int right = 0;
        double lo = 999, hi = -999;
        for (int i = 0; i < 20000; i++) {
            BeamTeleport.Draw d = s.draw(30, 70);
            if (d.side() == BeamTeleport.Side.RIGHT) right++;
            lo = Math.min(lo, d.firstAngle());
            hi = Math.max(hi, d.firstAngle());
        }
        assertTrue(right > 9000 && right < 11000, "a fair coin: " + right);
        assertTrue(lo >= 30 && lo < 30.1, "lo " + lo);
        assertTrue(hi <= 70 && hi > 69.9, "hi " + hi);
    }

    // --- why a due move does not happen -------------------------------------------------------------------------

    @Test
    void aDueMoveIsSkippedForEachReasonInOrder() {
        assertEquals(Optional.of(BeamTeleport.Skip.KNELL_GONE),
                BeamTeleport.skip(false, true, true, MobHostility.Side.PLAYER, false));
        assertEquals(Optional.of(BeamTeleport.Skip.TARGET_GONE),
                BeamTeleport.skip(true, false, true, MobHostility.Side.PLAYER, false));
        assertEquals(Optional.of(BeamTeleport.Skip.TARGET_WORLD),
                BeamTeleport.skip(true, true, false, MobHostility.Side.PLAYER, false));
        assertEquals(Optional.of(BeamTeleport.Skip.TARGET_EXEMPT),
                BeamTeleport.skip(true, true, true, MobHostility.Side.PLAYER, true));
        assertEquals(Optional.of(BeamTeleport.Skip.TARGET_EXEMPT),
                BeamTeleport.skip(true, true, true, MobHostility.Side.MOB, false));
        assertEquals(Optional.empty(), BeamTeleport.skip(true, true, true, MobHostility.Side.PLAYER, false));
        assertFalse(BeamTeleport.skip(true, true, true, MobHostility.Side.PLAYER, false).isPresent());
    }
}
