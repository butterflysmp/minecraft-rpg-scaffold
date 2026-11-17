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
        run(c, 200, true, at(20), true);
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
