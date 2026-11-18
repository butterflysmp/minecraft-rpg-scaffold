package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SKREEL'S POUNCE, the pure numbers and rules (PLAN-dragon-mobs.md step R8): the band, the stand-off point, the
 * leap's velocity, who the landing reaches, the push, the price. Every number is the shipped skreel.yml's.
 */
class MobPounceTest {

    /** The shipped numbers: 32, 8-12, path 1.6, wind-up 20, period 120, vertical 1.2, timeout 40, reach 3, 55, 0.7/0.4. */
    static MobPounce pounce() {
        return pounce(40);
    }

    static MobPounce pounce(int timeout) {
        return new MobPounce(32, 8, 12, 1.6, 20, 120, 1.2, timeout, 3, 55, 0.7, 0.4, "crit", 15, 5,
                List.of(), List.of(), List.of());
    }

    private static final double G = 0.08;
    private static final double EPS = 1e-9;

    // --- the leap's velocity: the record's vertical, a horizontal speed held each tick that lands on the aim ---------

    /**
     * Replays vanilla's air step on the returned velocity (LivingEntity#travelInAir, READ: move, then y' = (y - g) x
     * 0.98f, the horizontal held by the loop every tick) and checks it is at the aim's height and the aim's horizontal
     * distance on the leap's last tick, and above the aim's height the tick before, falling.
     */
    private static void landsOnTheAim(MobPounce.Leap l, Vec3 from, Vec3 aim) {
        double vy = l.velocity().y();
        double y = 0;
        double prevY = 0;
        double h = Math.hypot(l.velocity().x(), l.velocity().z());
        for (int n = 1; n <= l.ticks(); n++) {
            prevY = y;
            y += vy;
            vy = (vy - G) * MobPounce.AIR_DRAG;
        }
        double dy = aim.y() - from.y();
        double d = Math.hypot(aim.x() - from.x(), aim.z() - from.z());
        assertEquals(d, h * l.ticks(), EPS, "the horizontal distance flown in its ticks is the aim's");
        assertTrue(y <= dy + EPS, "at or below the aim's height on its last tick: " + y);
        assertTrue(prevY > dy, "above it the tick before: " + prevY);
    }

    @Test
    void onLevelGroundTenBlocksAwayItFliesTwentyNineTicksAtTenOverTwentyNine() {
        Vec3 from = new Vec3(0, 64, 0);
        Vec3 aim = new Vec3(10, 64, 0);
        MobPounce.Leap l = pounce().leap(from, aim, G);
        assertEquals(29, l.ticks());
        assertEquals(10.0 / 29, l.velocity().x(), EPS);
        assertEquals(1.2, l.velocity().y(), EPS);
        assertEquals(0.0, l.velocity().z(), EPS);
        assertEquals(10.0, l.horizontal(), EPS);
        landsOnTheAim(l, from, aim);
    }

    @Test
    void itAimsAlongTheHorizontalLineAndOneBlockDownTakesThirtyTicks() {
        Vec3 from = new Vec3(0, 64, 0);
        Vec3 aim = new Vec3(6, 63, 8);
        MobPounce.Leap l = pounce().leap(from, aim, G);
        assertEquals(30, l.ticks());
        assertEquals(6.0 / 30, l.velocity().x(), EPS);
        assertEquals(8.0 / 30, l.velocity().z(), EPS);
        landsOnTheAim(l, from, aim);
    }

    @Test
    void twoBlocksUpTakesTwentySevenTicks() {
        Vec3 from = new Vec3(0, 64, 0);
        Vec3 aim = new Vec3(0, 66, -9);
        MobPounce.Leap l = pounce().leap(from, aim, G);
        assertEquals(27, l.ticks());
        assertEquals(-9.0 / 27, l.velocity().z(), EPS);
        landsOnTheAim(l, from, aim);
    }

    /** An aim above the arc's top (8.19 blocks at 1.2) is unreachable: it uses the first falling tick and lands short. */
    @Test
    void anAimAboveTheTopOfTheArcUsesTheFirstFallingTick() {
        MobPounce.Leap l = pounce().leap(new Vec3(0, 64, 0), new Vec3(3, 73, 4), G);
        assertEquals(15, l.ticks());
        assertEquals(3.0 / 15, l.velocity().x(), EPS);
        assertEquals(4.0 / 15, l.velocity().z(), EPS);
    }

    @Test
    void theFlightIsCappedAtTheTimeout() {
        MobPounce.Leap l = pounce(20).leap(new Vec3(0, 64, 0), new Vec3(10, 64, 0), G);
        assertEquals(20, l.ticks());
        assertEquals(0.5, l.velocity().x(), EPS);
    }

    @Test
    void anAimStraightBelowGivesNoHorizontalSpeed() {
        MobPounce.Leap l = pounce().leap(new Vec3(5, 64, 5), new Vec3(5, 64, 5), G);
        assertEquals(0.0, l.velocity().x(), EPS);
        assertEquals(0.0, l.velocity().z(), EPS);
        assertEquals(1.2, l.velocity().y(), EPS);
        assertEquals(29, l.ticks());
    }

    // --- the band and the stand-off ---------------------------------------------------------------------------------

    @Test
    void theBandIsEightToTwelveBothIncluded() {
        MobPounce p = pounce();
        assertTrue(p.inBand(8) && p.inBand(12) && p.inBand(10));
        assertTrue(!p.inBand(7.999) && !p.inBand(12.001));
    }

    /**
     * THE BAND EDGE, FIXED (step R8, the seat's brief of 2026-11-17 after the boot of 45bc33ce): the stand-off point is
     * the band's MIDDLE, (near + far) / 2 = 10 at the shipped numbers, along the target-to-mount line at the target's
     * height. A mount nearer than the band moves OUT to the same point. Until this fix it was the far edge, 12
     * (cfde822's "the point 12 blocks from the player", READ).
     */
    @Test
    void theStandOffPointIsTheBandsMiddleAlongTheTargetToMountLineAtTheTargetsHeight() {
        MobPounce p = pounce();
        assertEquals(10.0, p.standOffDistance(), EPS);
        assertVec(new Vec3(10, 64, 0), p.standOff(new Vec3(0, 64, 0), new Vec3(20, 65, 0)));
        assertVec(new Vec3(0, 64, -10), p.standOff(new Vec3(0, 64, 0), new Vec3(0, 60, -3)));
        assertVec(new Vec3(10, 64, 0), p.standOff(new Vec3(0, 64, 0), new Vec3(0, 70, 0)));
        // A mount 5 away, too near, is sent OUT to the same middle point; one 30 away IN to it.
        assertVec(new Vec3(-6, 64, 8), p.standOff(new Vec3(0, 64, 0), new Vec3(-3, 64, 4)));
        assertVec(new Vec3(-6, 64, 8), p.standOff(new Vec3(0, 64, 0), new Vec3(-18, 64, 24)));
        // A band of 6 to 7 has its middle at 6.5: the rule is the middle, not the number 10.
        MobPounce narrow = new MobPounce(32, 6, 7, 1.6, 20, 120, 1.2, 40, 3, 55, 0.7, 0.4, "crit", 15, 5,
                List.of(), List.of(), List.of());
        assertVec(new Vec3(6.5, 64, 0), narrow.standOff(new Vec3(0, 64, 0), new Vec3(9, 64, 0)));
    }

    /**
     * THE DEFECT, REPRODUCED WITH PS6'S READING. PS6's mount died 12.4459 from him: 0.4459 past a goal at the far edge.
     * Stopping that far past its goal, a goal at the far edge leaves the mount OUTSIDE the band; a goal at the middle
     * leaves it inside, and so does stopping that far SHORT of it.
     */
    @Test
    void aMountStoppingWherePs6StoppedIsOutsideTheBandWithTheOldGoalAndInsideWithTheNew() {
        MobPounce p = pounce();
        double past = 12.4459 - 12;
        assertTrue(!p.inBand(p.bandFar() + past), "the old goal, the far edge: 12.4459 is outside");
        assertTrue(p.inBand(p.standOffDistance() + past), "the new goal: 10.4459 is inside");
        assertTrue(p.inBand(p.standOffDistance() - past), "and 9.5541, short of it, is inside");
    }

    /**
     * HOW NEAR THE PATHFINDER TAKES A MOB TO ITS GOAL, READ (javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar
     * net.minecraft.world.entity.ai.navigation.PathNavigation, and com.destroystokyo.paper.entity.PaperPathfinder):
     * Paper's moveTo(Location) builds its path with {@code createPath(x, y, z, 0)}, to the goal's own block;
     * {@code followThePath} sets {@code maxDistanceToWaypoint} = width / 2 when the width is over 0.75, else 0.75 - width
     * / 2, and passes a node when the mob is under that from the node's block CENTRE on x AND on z (and under 1 on y);
     * the last node is never passed early ({@code shouldTargetNextNodeInDirection} returns false on it). So the path is
     * done with the mob under (width / 2 + 0.5) from the goal on each axis: the node's centre is within 0.5 of the goal.
     * The mount is a spider (1.4 wide, READ: EntityType) at scale 1.5: 2.1 wide, so 1.55 on an axis and 1.55 x sqrt 2 =
     * 2.192 on a diagonal. THE BAND'S HALF-WIDTH, 2, EXCEEDS THE FIRST AND NOT THE SECOND: a mount whose line to him
     * runs near a diagonal can still stop outside the band. The cannot-reach rule (PounceCycle) is its backstop.
     */
    @Test
    void theBandsHalfWidthExceedsTheMountsStopOnAnAxisAndNotOnADiagonal() {
        MobPounce p = pounce();
        double width = 1.4 * 1.5;
        double waypoint = width > 0.75 ? width / 2 : 0.75 - width / 2;
        double axis = waypoint + 0.5;
        double diagonal = axis * Math.sqrt(2);
        double halfWidth = (p.bandFar() - p.bandNear()) / 2;
        assertEquals(1.55, axis, EPS);
        assertEquals(2.0, halfWidth, EPS);
        assertTrue(halfWidth > axis, "on an axis, a margin of " + (halfWidth - axis));
        assertTrue(halfWidth < diagonal, "on a diagonal it does not: " + diagonal);
        // The rider alone (a wither skeleton, 0.7 wide, READ: EntityType) would stop under 0.9 on an axis.
        assertEquals(0.9, (0.7 > 0.75 ? 0.35 : 0.75 - 0.35) + 0.5, EPS);
    }

    // --- the pair ends INSIDE its band from any bearing (step R8, the seat's ruling after its second diff) -----------

    /** The mount: a spider (1.4 wide, READ: EntityType) at scale 1.5. */
    private static final double MOUNT_WIDTH = 1.4 * 1.5;

    /**
     * THE STOPPING RULE, READ, AS PURE FUNCTIONS: {@code PathNavigation#followThePath}'s tolerance (width / 2 over 0.75
     * wide, else 0.75 - width / 2), and a path to a goal done when the mob is under it from the goal BLOCK's centre on x
     * and on z, and under 1 from the block's floor on y.
     */
    @Test
    void theStoppingRuleIsPerAxisFromTheGoalBlocksCentre() {
        assertEquals(1.05, MobPounce.waypointTolerance(MOUNT_WIDTH), EPS);
        assertEquals(0.4, MobPounce.waypointTolerance(0.7), EPS, "a wither skeleton: 0.75 - 0.35");
        Vec3 goal = new Vec3(10.2, 64, 3.9);                                  // its block's centre is (10.5, 3.5)
        assertTrue(MobPounce.pathDone(new Vec3(11.54, 64, 4.54), goal, 1.05), "1.04 off on both axes: done");
        assertTrue(!MobPounce.pathDone(new Vec3(11.55, 64, 3.5), goal, 1.05), "1.05 off on x: not yet (strictly under)");
        assertTrue(!MobPounce.pathDone(new Vec3(10.5, 65, 3.5), goal, 1.05), "a block up: not done");
    }

    /** How far a walking mount moves in a tick and slides after it stops (the sweep's two allowances). */
    private static final double STEP = 0.65;
    private static final double SLIDE = STEP * 0.546 / (1 - 0.546);

    /**
     * WHERE THE PAIR COMES TO REST, walking from {@code start} at the goal the pair chooses, along the straight line to
     * the goal block's centre (open ground). The cycle stands it the tick it is in the band, so it rests where it first
     * enters the band plus up to a tick's walk and a slide; or, if its path ends first (inside the goal's box), there plus
     * a slide. STEP is READ from PS3 at 45bc33ce (the mount moved 7.2 blocks in the 11 ticks after its landing: 0.65 a
     * tick); the slide is that speed carried under ground friction 0.6 x 0.91f (the 0.91f READ in
     * {@code LivingEntity#travelInAir}; the 0.6, a block's default friction, INFERRED), v x f / (1 - f) = 0.78. This model
     * gives about 12.27 for the OLD goal on PS6's axis (read: 12.4459). Returns the {min, max} distance at rest.
     */
    private static double[] rest(MobPounce p, Vec3 target, Vec3 start) {
        double tol = MobPounce.waypointTolerance(MOUNT_WIDTH);
        Vec3 goal = p.goal(target, start, tol);
        Vec3 c = new Vec3(Math.floor(goal.x()) + 0.5, goal.y(), Math.floor(goal.z()) + 0.5);
        double dx = c.x() - start.x();
        double dz = c.z() - start.z();
        double len = Math.hypot(dx, dz);
        double ux = len == 0 ? 0 : dx / len;
        double uz = len == 0 ? 0 : dz / len;
        double ds = 0.005;
        double cross = -1;
        double box = -1;
        for (double s = 0; s <= len + 3; s += ds) {
            Vec3 at = new Vec3(start.x() + ux * s, start.y(), start.z() + uz * s);
            if (cross < 0 && p.inBand(dist(at, target))) cross = s;
            if (box < 0 && MobPounce.pathDone(at, goal, tol)) box = s;
            if (cross >= 0 && box >= 0) break;
        }
        double from;
        double to;
        if (cross >= 0 && (box < 0 || cross <= box)) {
            from = cross;
            to = cross == 0 ? 0 : (box < 0 ? cross + STEP + SLIDE : Math.min(cross + STEP + SLIDE, box + SLIDE));
        } else {
            from = box;
            to = box + SLIDE;
        }
        double min = Double.MAX_VALUE;
        double max = 0;
        for (double s = from; s <= to + 1e-9; s += ds) {
            double d = dist(new Vec3(start.x() + ux * s, start.y(), start.z() + uz * s), target);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        return new double[] {min, max};
    }

    private static double dist(Vec3 a, Vec3 b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }

    /**
     * THE PROPERTY THE SEAT RULED: from any bearing (every 5 degrees) and any start distance within the acquire range,
     * inside and outside the band, for a target on a whole block and at fractional positions, the pair comes to rest
     * INSIDE 8 to 12. {@link MobPounce#goal} chooses the goal block so that its stopping box lies in the band, so the walk
     * crosses into the band before its path can end.
     */
    @Test
    void fromEveryBearingAndDistanceThePairRestsInsideTheBand() {
        MobPounce p = pounce();
        double worstMin = Double.MAX_VALUE;
        double worstMax = 0;
        String worstAt = "";
        List<String> outside = new ArrayList<>();
        int cases = 0;
        for (Vec3 t : TARGETS) {
            for (int deg = 0; deg < 360; deg += 5) {
                double a = Math.toRadians(deg);
                for (double r : STARTS) {
                    Vec3 start = new Vec3(t.x() + Math.cos(a) * r, 64, t.z() + Math.sin(a) * r);
                    double[] mm = rest(p, t, start);
                    cases++;
                    if (mm[0] < worstMin || mm[1] > worstMax) worstAt = t + " " + deg + " deg from " + r;
                    worstMin = Math.min(worstMin, mm[0]);
                    worstMax = Math.max(worstMax, mm[1]);
                    if (mm[0] < p.bandNear() || mm[1] > p.bandFar()) {
                        outside.add(String.format(java.util.Locale.ROOT, "%s %d deg from %s: %.4f to %.4f", t, deg, r, mm[0], mm[1]));
                    }
                }
            }
        }
        System.out.printf("SWEEP cases=%d rest min=%.4f max=%.4f worst at %s%n", cases, worstMin, worstMax, worstAt);
        assertEquals(4 * 72 * 12, cases);
        assertEquals(List.of(), outside.stream().limit(6).toList(), outside.size() + " of " + cases + " rest outside the band");
    }

    /** The targets the sweep stands at: on a whole block's corner, at a block centre, and at two fractional points. */
    private static final List<Vec3> TARGETS = List.of(new Vec3(0, 64, 0), new Vec3(0.5, 64, 0.5), new Vec3(0.3, 64, 0.77),
            new Vec3(-7.91, 64, 12.12));

    /** The start distances: inside the near edge, inside the band, and out to the acquire range. */
    private static final double[] STARTS = {0.5, 3, 6, 7.9, 9, 11, 12.1, 12.5, 14, 18, 24, 31.9};

    /**
     * And the reason it holds: in every one of those cases the chosen block's stopping box, grown by the margin, lies
     * inside the band, so the fallback (the ideal point's own block) is never taken. Also: how far the chosen block's
     * centre lies from the ideal point, the price of the choice (the pair circles a little off its bearing).
     */
    @Test
    void inEverySweptCaseTheChosenBlocksStoppingBoxLiesInsideTheBand() {
        MobPounce p = pounce();
        double tol = MobPounce.waypointTolerance(MOUNT_WIDTH);
        double h = tol + MobPounce.GOAL_MARGIN;
        double worstOff = 0;
        for (Vec3 t : TARGETS) {
            for (int deg = 0; deg < 360; deg += 5) {
                double a = Math.toRadians(deg);
                for (double r : STARTS) {
                    Vec3 start = new Vec3(t.x() + Math.cos(a) * r, 64, t.z() + Math.sin(a) * r);
                    Vec3 g = p.goal(t, start, tol);
                    double ax = Math.abs(g.x() - t.x());
                    double az = Math.abs(g.z() - t.z());
                    assertTrue(Math.hypot(ax + h, az + h) <= p.bandFar()
                            && Math.hypot(Math.max(ax - h, 0), Math.max(az - h, 0)) >= p.bandNear(), "a qualifying block: " + g);
                    worstOff = Math.max(worstOff, dist(g, p.standOff(t, start)));
                }
            }
        }
        System.out.printf("GOAL at most %.4f from the ideal point%n", worstOff);
        assertTrue(worstOff < 4, "within the 3-block search round the ideal point's block: " + worstOff);
    }

    /**
     * THE DEFECT, IN THE MODEL: the OLD goal (the far edge, 12, the ideal point's block) rests OUTSIDE the band from PS6's
     * axis, about 12.27 against the read 12.4459; the model is not kinder than the server.
     */
    @Test
    void theOldFarEdgeGoalRestsOutsideTheBandInTheModel() {
        MobPounce old = pounce();
        double tol = MobPounce.waypointTolerance(MOUNT_WIDTH);
        // The far edge's block on +x from a target at a block centre: (12.5, z) centre; the walk from 20 east enters its
        // box at x = 12.5 + 1.05 = 13.55, 13.05 from him, and slides 0.78: about 12.27, outside.
        Vec3 t = new Vec3(0.5, 64, 0.5);
        Vec3 goal = new Vec3(12.5, 64, 0.5);
        double entry = 0;
        for (double x = 20.5; x > 0; x -= 0.005) {
            if (MobPounce.pathDone(new Vec3(x, 64, 0.5), goal, tol)) { entry = x; break; }
        }
        double restAt = entry - SLIDE - t.x();
        assertTrue(restAt > old.bandFar(), "the old goal rests at " + restAt);
    }

    /** The band's gap: how far outside 8 to 12 a distance is; 0 inside. The cannot-reach rule measures progress by it. */
    @Test
    void theBandGapIsTheDistanceOutsideTheBand() {
        MobPounce p = pounce();
        assertEquals(0.0, p.bandGap(10), EPS);
        assertEquals(0.0, p.bandGap(8), EPS);
        assertEquals(0.0, p.bandGap(12), EPS);
        assertEquals(3.0, p.bandGap(5), EPS);
        assertEquals(0.4459, p.bandGap(12.4459), 1e-12);
    }

    // --- the landing: who it reaches --------------------------------------------------------------------------------

    private static MobBlast.Bystander who(MobHostility.Side side, boolean exempt, String world, Vec3 at) {
        return new MobBlast.Bystander(UUID.randomUUID(), side, exempt, world, at);
    }

    @Test
    void theLandingReachesEveryCombatantWithinThreeFeetToFeetAndNoMob() {
        MobPounce p = pounce();
        Vec3 at = new Vec3(0, 64, 0);
        MobBlast.Bystander edge = who(MobHostility.Side.PLAYER, false, "w", new Vec3(3, 64, 0));
        MobBlast.Bystander near = who(MobHostility.Side.PLAYER, false, "w", new Vec3(0, 65, 1));
        MobBlast.Bystander past = who(MobHostility.Side.PLAYER, false, "w", new Vec3(3.001, 64, 0));
        MobBlast.Bystander husk = who(MobHostility.Side.MOB, false, "w", new Vec3(1, 64, 0));
        MobBlast.Bystander creative = who(MobHostility.Side.PLAYER, true, "w", new Vec3(2, 64, 0));
        MobBlast.Bystander elsewhere = who(MobHostility.Side.PLAYER, false, "other", new Vec3(0, 64, 0));
        MobPounce.Landing l = p.landing(at, "w", List.of(edge, near, past, husk, creative, elsewhere));
        assertEquals(List.of(near.id(), edge.id()), l.victims().stream().map(s -> s.who().id()).toList(), "nearest first");
        assertEquals(Math.sqrt(2), l.victims().get(0).distance(), EPS);
        assertEquals(List.of(husk.id(), creative.id()), l.spared().stream().map(s -> s.who().id()).toList());
    }

    // --- the push ---------------------------------------------------------------------------------------------------

    @Test
    void thePushIsPointSevenAwayFromTheLandingAndPointFourUpCutByResistance() {
        MobPounce p = pounce();
        Vec3 at = new Vec3(0, 64, 0);
        assertVec(new Vec3(0.42, 0.4, 0.56), p.push(at, new Vec3(3, 64, 4), 0.0));
        assertVec(new Vec3(0.21, 0.2, 0.28), p.push(at, new Vec3(3, 65, 4), 0.5));
        assertVec(new Vec3(0, 0.4, 0), p.push(at, new Vec3(0, 64, 0), 0.0));
        assertVec(Vec3.ZERO, p.push(at, new Vec3(3, 64, 4), 1.5));
    }

    private static void assertVec(Vec3 want, Vec3 got) {
        assertEquals(want.x(), got.x(), EPS, "x of " + got);
        assertEquals(want.y(), got.y(), EPS, "y of " + got);
        assertEquals(want.z(), got.z(), EPS, "z of " + got);
    }

    // --- the price --------------------------------------------------------------------------------------------------

    @Test
    void theHitIsFiftyFiveTimesGearScoreOverAHundredBeforeDefence() {
        MobPounce p = pounce();
        assertEquals(55.0, p.price(true, 100), EPS);
        assertEquals(165.0, p.price(true, 300), EPS);
    }

    // --- the numbers refused by name --------------------------------------------------------------------------------

    @Test
    void aBadNumberIsRefusedByName() {
        assertThrows(IllegalArgumentException.class, () -> new MobPounce(32, 12, 8, 1.6, 20, 120, 1.2, 40, 3, 55, 0.7,
                0.4, "crit", 15, 5, List.of(), List.of(), List.of()), "near past far");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new MobPounce(32, 8, 12, 1.6, 0,
                120, 1.2, 40, 3, 55, 0.7, 0.4, "crit", 15, 5, List.of(), List.of(), List.of()));
        assertTrue(e.getMessage().contains("pounce.windup_ticks"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> new MobPounce(32, 8, 12, 1.6, 20, 120, 1.2, 40, 3, 55,
                -0.1, 0.4, "crit", 15, 5, List.of(), List.of(), List.of()));
        assertTrue(e.getMessage().contains("pounce.knockback.horizontal"), e.getMessage());
    }

    /** The cycle is the PAIR's: a pounce with no mount to ride is refused by name. */
    @Test
    void aPounceWithNoMountIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new MobDefinition("skreel",
                "wither_skeleton", "Skreel", 120, MobBody.VANILLA, "void", null, null, null, null, null, null, null,
                "iron_spear", null, null, null, pounce(), null));
        assertTrue(e.getMessage().contains("mount"), e.getMessage());
        IllegalArgumentException s = assertThrows(IllegalArgumentException.class, () -> new MobDefinition("m",
                "spider", "M", 160, MobBody.VANILLA, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0.0));
        assertTrue(s.getMessage().contains("ridden_speed"), s.getMessage());
    }
}
