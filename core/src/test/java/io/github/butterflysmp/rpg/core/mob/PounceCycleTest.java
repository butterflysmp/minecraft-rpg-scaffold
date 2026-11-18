package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SKREEL'S CYCLE, the pure state machine (PLAN-dragon-mobs.md step R8): recover, hold the band, wind up, leap, land.
 * Every number is the shipped skreel.yml's (the seat's numbers Ben accepted on 2026-11-17, and cfde822's where those are
 * silent): a band of 8 to 12, a 20-tick wind-up, 120 ticks from one wind-up's start to the next's earliest, a 40-tick
 * leap timeout. Paper supplies the distance and the ground; the cycle says what to do this tick.
 */
class PounceCycleTest {

    private static final OptionalDouble NONE = OptionalDouble.empty();

    private static OptionalDouble at(double d) {
        return OptionalDouble.of(d);
    }

    /** Run {@code n} ticks with the same inputs; the steps in order. */
    private static List<PounceCycle.Step> run(PounceCycle c, int n, boolean canAct, OptionalDouble d, boolean onGround) {
        List<PounceCycle.Step> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(c.tick(canAct, d, onGround));
        return out;
    }

    // --- the clock: no wind-up before the period ------------------------------------------------------------------

    @Test
    void itStartsRecoveringAndItsFirstWindUpIsOnTheHundredAndTwentiethTick() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        assertEquals(PounceCycle.Stage.RECOVER, c.stage());
        List<PounceCycle.Step> steps = run(c, 119, true, at(10), true);
        assertFalse(steps.contains(PounceCycle.Step.BEGIN), "no wind-up in the first 119 ticks, in the band all along");
        assertTrue(steps.stream().allMatch(s -> s == PounceCycle.Step.STAND), "in the band it stands: " + steps);
        assertEquals(PounceCycle.Step.BEGIN, c.tick(true, at(10), true), "the 120th tick begins it");
        assertEquals(PounceCycle.Stage.WINDUP, c.stage());
    }

    @Test
    void outsideTheBandItClosesInAndWithNoTargetItStands() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        assertEquals(PounceCycle.Step.CLOSE_IN, c.tick(true, at(20), true));
        assertEquals(PounceCycle.Step.CLOSE_IN, c.tick(true, at(3), true), "too close: it backs to the band too");
        assertEquals(PounceCycle.Step.STAND, c.tick(true, NONE, true));
        assertEquals(PounceCycle.Step.STAND, c.tick(true, at(8), true), "8 is in the band, inclusive");
        assertEquals(PounceCycle.Step.STAND, c.tick(true, at(12), true), "12 is in the band, inclusive");
        assertEquals(PounceCycle.Step.CLOSE_IN, c.tick(true, at(12.001), true));
        assertEquals(PounceCycle.Step.CLOSE_IN, c.tick(true, at(7.999), true));
    }

    @Test
    void readyButOutOfTheBandItHoldsAndBeginsTheTickTheTargetIsInIt() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        // Walking in from 40 at 0.1 a tick (a block every 10 ticks: progress, so the cannot-reach rule never counts).
        for (int k = 1; k <= 200; k++) c.tick(true, at(40 - 0.1 * k), true);
        assertEquals(PounceCycle.Stage.HOLD, c.stage(), "the clock ran out while it was still closing in");
        assertEquals(PounceCycle.Step.CLOSE_IN, c.tick(true, at(13), true));
        assertEquals(PounceCycle.Step.BEGIN, c.tick(true, at(12), true));
    }

    /** I23, as the Knell's and the Phalanx's: a pair with no AI never begins a wind-up of its own. */
    @Test
    void aFrozenPairNeverBeginsOfItsOwnAndOnlyStands() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        List<PounceCycle.Step> steps = run(c, 400, false, at(10), true);
        assertTrue(steps.stream().allMatch(s -> s == PounceCycle.Step.STAND), "" + steps.stream().distinct().toList());
    }

    // --- the wind-up, the leap, the land ----------------------------------------------------------------------------

    /** The wind-up ROOTS for 19 ticks after its first, and the LAUNCH is exactly 20 ticks after the BEGIN. */
    @Test
    void theLaunchIsExactlyTwentyTicksAfterTheBeginAndTheTicksBetweenRoot() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        assertTrue(c.force());
        assertEquals(PounceCycle.Step.BEGIN, c.tick(true, at(10), true));
        List<PounceCycle.Step> between = run(c, 19, true, at(10), true);
        assertTrue(between.stream().allMatch(s -> s == PounceCycle.Step.ROOT), "" + between);
        assertEquals(PounceCycle.Step.LAUNCH, c.tick(true, at(10), true));
        assertEquals(PounceCycle.Stage.LEAP, c.stage());
    }

    /** Once begun it always leaps: the target leaving, dying or going out of the band changes nothing (the Knell's I15). */
    @Test
    void aWindUpOnceBegunAlwaysLeapsWhateverTheTargetDoes() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        c.force();
        c.tick(true, at(10), true);
        run(c, 10, true, at(40), true);
        run(c, 9, true, NONE, true);
        assertEquals(PounceCycle.Step.LAUNCH, c.tick(false, NONE, true));
    }

    @Test
    void itLandsTheFirstTickItIsOnTheGroundAfterLeavingIt() {
        PounceCycle c = leaping();
        assertEquals(PounceCycle.Step.FLY, c.tick(true, at(10), true), "still on the ground the tick after the launch: not a landing");
        List<PounceCycle.Step> air = run(c, 27, true, at(10), false);
        assertTrue(air.stream().allMatch(s -> s == PounceCycle.Step.FLY), "" + air);
        assertEquals(PounceCycle.Step.LANDED, c.tick(true, at(10), true));
        assertEquals(PounceCycle.Stage.RECOVER, c.stage());
    }

    @Test
    void theTimeoutEndsTheLeapFortyTicksAfterTheLaunchWhereverItIs() {
        PounceCycle c = leaping();
        List<PounceCycle.Step> air = run(c, 39, true, at(10), false);
        assertTrue(air.stream().allMatch(s -> s == PounceCycle.Step.FLY), "" + air);
        assertEquals(PounceCycle.Step.TIMED_OUT, c.tick(true, at(10), false));
        assertEquals(PounceCycle.Stage.RECOVER, c.stage());
    }

    /** A leap that never leaves the ground (a ceiling at its head) times out too: there is no landing without a flight. */
    @Test
    void aLeapThatNeverLeavesTheGroundTimesOutAndNeverLands() {
        PounceCycle c = leaping();
        List<PounceCycle.Step> ground = run(c, 39, true, at(10), true);
        assertTrue(ground.stream().allMatch(s -> s == PounceCycle.Step.FLY), "" + ground);
        assertEquals(PounceCycle.Step.TIMED_OUT, c.tick(true, at(10), true));
    }

    // --- the period: from a wind-up's start to the next's earliest ---------------------------------------------------

    @Test
    void theNextWindUpIsAHundredAndTwentyTicksAfterThePreviousBegan() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        c.force();
        assertEquals(PounceCycle.Step.BEGIN, c.tick(true, at(10), true));                 // tick 0
        run(c, 19, true, at(10), true);                                                     // 1..19 root
        c.tick(true, at(10), true);                                                         // 20 launch
        run(c, 5, true, at(10), false);                                                     // 21..25 air
        assertEquals(PounceCycle.Step.LANDED, c.tick(true, at(10), true));                 // 26 lands
        List<PounceCycle.Step> wait = run(c, 93, true, at(10), true);                       // 27..119
        assertFalse(wait.contains(PounceCycle.Step.BEGIN), "not before 120: " + wait);
        assertEquals(PounceCycle.Step.BEGIN, c.tick(true, at(10), true), "tick 120");
    }

    // --- the band edge and the cannot-reach rule (step R8, the seat's brief after the boot of 45bc33ce) -------------

    /** The steps at which a wind-up begins, counted from 1, over {@code n} ticks with the same inputs. */
    private static List<Integer> begins(PounceCycle c, int n, boolean canAct, OptionalDouble d) {
        List<Integer> out = new ArrayList<>();
        for (int i = 1; i <= n; i++) if (c.tick(canAct, d, true) == PounceCycle.Step.BEGIN) out.add(i);
        return out;
    }

    /**
     * THE DEFECT, REPRODUCED. PS6's mount parked at 12.4459 (the old goal, the far edge, overshot): no wind-up in the band
     * EVER; only the cannot-reach rule's, from where it stands, at 121 (it has made no progress toward the band since its
     * first tick). With the new goal it parks at 10.4459 and winds up on the 120th tick.
     */
    @Test
    void parkedWherePs6ParkedItNeverWindsUpInTheBandAndAtTheNewGoalItDoesAtOneTwenty() {
        PounceCycle old = new PounceCycle(MobPounceTest.pounce());
        assertEquals(List.of(121), begins(old, 240, true, at(12.4459)), "only the cannot-reach wind-up");
        PounceCycle fixed = new PounceCycle(MobPounceTest.pounce());
        assertEquals(List.of(120), begins(fixed, 120, true, at(10.4459)));
    }

    /**
     * THE CANNOT-REACH RULE (the seat's ruling; CHOSEN, for Ben), MEASURED BY PROGRESS (the seat's second diff): a pair
     * with its AI and a target, outside the band, that has not come {@link MobPounce#PROGRESS_BLOCKS} (1) nearer the band
     * for a full period (120 ticks), and is ready, winds up from where it stands. A pair stuck from its first tick: its
     * first tick sets its best, so the 121st is the one that has waited 120. At 20, at 5 (a wall at its back), at 31.
     */
    @Test
    void aPairThatMakesNoProgressTowardTheBandForAPeriodWindsUpFromWhereItStands() {
        for (double d : new double[] {20, 5, 31}) {
            PounceCycle c = new PounceCycle(MobPounceTest.pounce());
            List<PounceCycle.Step> waiting = run(c, 120, true, at(d), true);
            assertTrue(waiting.stream().allMatch(s -> s == PounceCycle.Step.CLOSE_IN), d + ": " + waiting.stream().distinct().toList());
            assertEquals(PounceCycle.Step.BEGIN, c.tick(true, at(d), true), d + ": tick 121");
        }
    }

    /**
     * ITS WAIT NEVER COUNTS WHILE THE PAIR WALKS TO THE BAND IN GOOD FAITH. From 40, closing 0.05 a tick (a block every 20
     * ticks, far slower than a walking mount): no wind-up until it is in the band, and that one is the band's.
     */
    @Test
    void aPairWalkingToTheBandInGoodFaithNeverTriggersIt() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        int first = -1;
        double at = 0;
        for (int k = 1; k <= 1000 && first < 0; k++) {
            double d = 40 - 0.05 * k;
            if (c.tick(true, at(d), true) == PounceCycle.Step.BEGIN) {
                first = k;
                at = d;
            }
        }
        assertTrue(first > 0 && at <= 12 && at >= 8, "its first wind-up is in the band: tick " + first + " at " + at);
    }

    /**
     * A PAIR THAT STALLS: from 30, closing 0.125 a tick to 14, then held there (a wall 2 short of the band). Its best gap
     * last came a whole block nearer on tick 121 (the gap 2.875); 120 ticks without another, it winds up, on tick 241.
     */
    @Test
    void aPairThatStallsShortOfTheBandWindsUpAPeriodAfterItsLastProgress() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        List<Integer> begun = new ArrayList<>();
        for (int k = 1; k <= 400; k++) {
            double d = Math.max(14, 30 - 0.125 * k);
            if (c.tick(true, at(d), true) == PounceCycle.Step.BEGIN) begun.add(k);
            if (!begun.isEmpty()) break;
        }
        assertEquals(List.of(241), begun);
    }

    /** The wait starts again on a tick with no target or no AI; a frozen pair never winds up. */
    @Test
    void theCannotReachWaitStartsAgainWhenTheTargetIsLostOrTheAiIsOff() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        run(c, 100, true, at(20), true);                            // 99 ticks without progress
        assertEquals(PounceCycle.Step.STAND, c.tick(true, NONE, true));
        assertEquals(List.of(121), begins(c, 121, true, at(20)), "a full period again after the lost target");
        PounceCycle f = new PounceCycle(MobPounceTest.pounce());
        run(f, 100, true, at(20), true);
        f.tick(false, at(20), true);
        assertEquals(List.of(121), begins(f, 121, true, at(20)), "and after a tick with no AI");
        PounceCycle frozen = new PounceCycle(MobPounceTest.pounce());
        assertEquals(List.of(), begins(frozen, 1000, false, at(20)), "a frozen pair never");
    }

    /**
     * After a wind-up its wait starts afresh: a pair that lands 20 out on its tick 26 sets its best on 27, and waits a
     * period from there, on 147 after the begin (ready since 120): the 121st tick after the landing.
     */
    @Test
    void afterAWindUpTheWaitStartsAfreshFromTheLanding() {
        PounceCycle c = leaping();                                   // began at its tick 0, launched at 20
        run(c, 5, true, at(20), false);                              // 21..25 in the air
        assertEquals(PounceCycle.Step.LANDED, c.tick(true, at(20), true));   // 26
        assertEquals(List.of(121), begins(c, 214, true, at(20)), "26 + 121 = 147 after the begin");
    }

    /**
     * A wind-up starts the wait afresh even when the pair was stuck before it: one that wound up by the cannot-reach rule
     * at 20 and lands at 20 again waits a full period from its landing (the 121st tick after it), not the 94th, when it is
     * ready.
     */
    @Test
    void aCannotReachWindUpStartsTheWaitAfresh() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        assertEquals(List.of(121), begins(c, 121, true, at(20)));
        run(c, 20, true, at(20), true);                              // 19 ROOT, then the LAUNCH
        run(c, 5, true, at(20), false);                              // in the air
        assertEquals(PounceCycle.Step.LANDED, c.tick(true, at(20), true));
        assertEquals(List.of(121), begins(c, 121, true, at(20)));
    }

    /** A pair inside the band holds; nearer than it, it moves out (the same CLOSE_IN, to the middle). */
    @Test
    void insideTheBandItHoldsAndNearerThanItItMovesOut() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        assertEquals(PounceCycle.Step.STAND, c.tick(true, at(10), true));
        assertEquals(PounceCycle.Step.CLOSE_IN, c.tick(true, at(5), true));
        assertEquals(PounceCycle.Step.STAND, c.tick(true, at(9.5541), true));
    }

    // --- the instrument -----------------------------------------------------------------------------------------------

    @Test
    void aForceBeginsOnTheNextTickWhateverTheClockTheBandOrTheAiAndIsRefusedWhileBusy() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        assertTrue(c.force());
        assertEquals(PounceCycle.Step.BEGIN, c.tick(false, NONE, true), "tick 1 of the loop, no target, no AI: still begins");
        assertFalse(c.force(), "refused during a wind-up");
        run(c, 20, true, NONE, true);
        assertEquals(PounceCycle.Stage.LEAP, c.stage());
        assertFalse(c.force(), "refused during a leap");
        run(c, 3, true, NONE, false);
        c.tick(true, NONE, true);
        assertEquals(PounceCycle.Stage.RECOVER, c.stage());
        assertTrue(c.force(), "accepted while recovering");
        assertEquals(PounceCycle.Step.BEGIN, c.tick(true, NONE, true));
    }

    private static PounceCycle leaping() {
        PounceCycle c = new PounceCycle(MobPounceTest.pounce());
        c.force();
        c.tick(true, at(10), true);
        run(c, 19, true, at(10), true);
        assertEquals(PounceCycle.Step.LAUNCH, c.tick(true, at(10), true));
        return c;
    }
}
