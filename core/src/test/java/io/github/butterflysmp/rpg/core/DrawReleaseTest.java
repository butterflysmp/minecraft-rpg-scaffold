package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.DrawCharge;
import io.github.butterflysmp.rpg.core.combat.DrawRelease;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The release decision: nothing, a tap, or a charged step.
 *
 * <h2>THE MUTATION MATRIX -- ALL RUN, NONE ASSERTED</h2>
 *
 * <pre>
 * MUT-FLOOR    MIN_RELEASE_TICKS 3 -> 0, R10's floor removed     -> 2 rows
 * MUT-ORDER    the two refusals swapped                          -> 1 row   UNIQUE
 * MUT-TAP      step &lt; 1 -> step &lt; 0, so a partial draw charges   -> 2 rows
 * MUT-CURVE    vanillaPowerFor's /3 -> /2                        -> 1 row   UNIQUE
 * </pre>
 *
 * <p><b>{@code MUT-FLOOR} is the one this file was written for.</b> R10 was ruled as <i>keep
 * vanilla's behaviour</i> on the assumption the platform would supply it; H1's {@code
 * clearActiveItem()} means {@code BowItem.releaseUsing} never runs and the power gate never
 * executes. <b>The floor is ours now and nothing else in the tree enforces it</b> -- so if this
 * mutation goes green, a sub-floor twitch fires a live arrow.
 */
class DrawReleaseTest {

    /**
     * R10'S FLOOR, ON BOTH SIDES OF THE BOUNDARY.
     *
     * <p>Asserted at 2 and 3 rather than at 0 and 10, because an off-by-one is the whole content of
     * a floor and a fixture staged far from it would pass under any threshold at all.
     */
    @Test
    void underThreeTicksFiresNothingAtAll() {
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.BELOW_VANILLA_FLOOR),
                DrawRelease.decide(2, 25), "two ticks is below the floor -- vanilla would not fire either");
        assertEquals(new DrawRelease.Tap(1), DrawRelease.decide(3, 25),
                "and exactly on it the release becomes a tap -- band 1, the floor band");
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.BELOW_VANILLA_FLOOR),
                DrawRelease.decide(0, 25), "a release with no hold at all is the same refusal");
    }

    /**
     * THE CONSTANT IS CHECKED AGAINST THE CURVE IT CAME FROM, NOT MERELY ANNOTATED WITH IT.
     *
     * <p>§3.1 measured that {@code BowItem.releaseUsing} refuses below {@code getPowerForTime < 0.1}
     * over {@code ((t/20)^2 + 2t/20)/3}, giving {@code t=2 -> 0.0700} and {@code t=3 -> 0.1075}.
     * <b>The floor used to be inherited and cannot be any more</b>, so this row is what stops the
     * transcription and the constant drifting apart.
     *
     * <p><b>IT COVERS HALF THE DRIFT.</b> If a future Paper changes the curve, the transcription is
     * stale too and this row stays green while both are wrong together. There is no automatic
     * detector for that, and the class javadoc says so rather than letting this row look like more
     * protection than it is.
     */
    @Test
    void theFloorIsTheSmallestTickCountVanillaSCurveWouldHaveSHOTAt() {
        assertEquals(0.0700, DrawRelease.vanillaPowerFor(2), 1e-4, "§3.1's measured t=2");
        assertEquals(0.1075, DrawRelease.vanillaPowerFor(3), 1e-4, "§3.1's measured t=3");

        assertTrue(DrawRelease.vanillaPowerFor(DrawRelease.MIN_RELEASE_TICKS) >= DrawRelease.MIN_RELEASE_POWER,
                "the floor must be a tick vanilla WOULD have shot at");
        assertTrue(DrawRelease.vanillaPowerFor(DrawRelease.MIN_RELEASE_TICKS - 1) < DrawRelease.MIN_RELEASE_POWER,
                "and the tick below it must be one vanilla would have refused -- otherwise the "
                        + "constant is not the SMALLEST such tick and the floor sits too high");
    }

    /**
     * R4''': A PARTIAL DRAW IS A TAP, AND THE BOUNDARY WITH A ONE-ARROW CHARGED RELEASE IS ONE TICK
     * WIDE.
     *
     * <p><b>Both fire a single arrow and they are different shots</b> -- 26 and no homing against 34
     * and homing. That is R4 overturned rather than amended, and this row is where the overturn is
     * observable at all: a test asserting only "a partial draw fires one arrow" would pass under the
     * ruling it replaced.
     *
     * <p><b>THIS JAVADOC READ "12 and no homing against 48" UNTIL R14's SWEEP</b> -- two figures
     * from before R8', in a row whose whole subject is the difference between those two numbers.
     * Found by grepping the digits, which is the only thing that finds a stale figure sitting in
     * prose that is otherwise still correct.
     */
    @Test
    void nineteenTicksIsATapAndTwentyIsAChargedReleaseOfONEArrow() {
        assertEquals(new DrawRelease.Tap(3), DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS - 1, 25),
                "one tick short of full draw is R4''''s tap at its TOP band: one arrow, no homing, "
                        + "26 -- asserted as the band rather than as `some Tap`, because 19 is the "
                        + "one tick where a band-3 tap and a one-arrow charged release are hardest "
                        + "to tell apart");
        assertEquals(new DrawRelease.Charged(1, 1),
                DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS, 25),
                "and full draw is R6's FIRST STEP: one arrow, homing, 34 -- the same count, a "
                        + "different shot");
    }

    /**
     * R4''': THE THREE TAP BANDS, AND THE ROWS ARE AT THE BOUNDARIES RATHER THAN IN THE MIDDLES.
     *
     * <p>The bands are vanilla's power curve cut in thirds: the roots are {@code t = 8.2843} and
     * {@code t = 14.6410}, so the integer bands are {@code 3..8}, {@code 9..14}, {@code 15..19}.
     *
     * <p><b>EVERY ASSERTION HERE IS ON A BOUNDARY PAIR, because a row staged in the middle of a
     * band passes under ANY boundary at all.</b> 8-vs-9 and 14-vs-15 are the only two ticks in the
     * whole range where a wrong cut is observable, and a fixture at, say, 5 and 11 would be green
     * for a version that split at 6 and 10.
     */
    @Test
    void theTapBandsCutAtTheCURVESThirdsAndTheRowsSitOnTheBOUNDARIES() {
        assertEquals(1, DrawRelease.bandFor(3), "the floor tick is band 1");
        assertEquals(1, DrawRelease.bandFor(8), "8 is the last tick of band 1 -- root 8.2843");
        assertEquals(2, DrawRelease.bandFor(9), "and 9 is the first of band 2");
        assertEquals(2, DrawRelease.bandFor(14), "14 is the last of band 2 -- root 14.6410");
        assertEquals(3, DrawRelease.bandFor(15), "and 15 is the first of band 3");
        assertEquals(3, DrawRelease.bandFor(19), "19 is the last tick that is a tap at all");
    }

    /**
     * THE BOUNDARIES ARE THE CURVE'S THIRDS, RE-DERIVED HERE RATHER THAN PINNED AS TWO INTEGERS.
     *
     * <p>{@code bandFor} hard-codes 8 and 14. <b>This row is what stops those being two numbers
     * nobody can check:</b> it solves the curve for the thirds and asserts the integers agree.
     * Change the curve transcription and this reddens, where {@code bandFor}'s own rows would not.
     */
    @Test
    void theBandBoundariesAgreeWithTheCURVETheyWereDerivedFrom() {
        double third = 1.0 / 3, twoThirds = 2.0 / 3;

        assertTrue(DrawRelease.vanillaPowerFor(8) < third, "8 is below the first third");
        assertTrue(DrawRelease.vanillaPowerFor(9) > third, "9 is above it");
        assertTrue(DrawRelease.vanillaPowerFor(14) < twoThirds, "14 is below the second third");
        assertTrue(DrawRelease.vanillaPowerFor(15) > twoThirds, "15 is above it");

        // The roots themselves, so the derivation is executable rather than only described:
        // (x^2 + 2x)/3 = 1/3  ->  x = sqrt(2) - 1;  = 2/3  ->  x = sqrt(3) - 1.
        assertEquals(8.2843, (Math.sqrt(2) - 1) * DrawCharge.FULL_DRAW_TICKS, 1e-4);
        assertEquals(14.6410, (Math.sqrt(3) - 1) * DrawCharge.FULL_DRAW_TICKS, 1e-4);
    }

    /** A release below the floor is not a band-1 tap -- it is not a tap at all. */
    @Test
    void theFloorIsCheckedBeforeTheBandSoASubFloorTwitchIsNotBandOne() {
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.BELOW_VANILLA_FLOOR),
                DrawRelease.decide(2, 25),
                "bandFor(2) would answer 1, and decide must never ask it -- the floor comes first");
        assertEquals(new DrawRelease.Tap(1), DrawRelease.decide(3, 25),
                "and the very next tick is band 1");
    }

    /** R1': the three steps, each carrying the step it announced as well as the arrows it fires. */
    @Test
    void theThreeStepsFireOneThreeAndFiveOnAFullMagazine() {
        int full = DrawCharge.FULL_DRAW_TICKS;
        int c = DrawCharge.CHARGE_SECOND_TICKS;

        assertEquals(new DrawRelease.Charged(1, 1), DrawRelease.decide(full, 25));
        assertEquals(new DrawRelease.Charged(2, 3), DrawRelease.decide(full + c, 25));
        assertEquals(new DrawRelease.Charged(3, 5), DrawRelease.decide(full + 2 * c, 25));
        assertEquals(new DrawRelease.Charged(3, 5), DrawRelease.decide(72_000, 25),
                "and holding forever is still the top step");
    }

    /**
     * R3a AT THE RELEASE: THE MAGAZINE CAPS THE STEP, AND THE REMAINDER IS STRANDED.
     *
     * <p>Staged at 2 and 4 rounds for {@code DrawChargeTest}'s reason -- at 1 and 3 the ruled rule
     * and the rejected {@code min(step, rounds)} give the same answer, so a row staged there passes
     * under either implementation.
     */
    @Test
    void aCappedReleaseFiresExactlyTheTrackedStepAndStrandsTheRest() {
        int held = DrawCharge.FULL_DRAW_TICKS + 2 * DrawCharge.CHARGE_SECOND_TICKS;   // earns step 3

        assertEquals(new DrawRelease.Charged(1, 1), DrawRelease.decide(held, 2),
                "two rounds buys step 1 and strands one -- NOT two arrows, which no step is worth");
        assertEquals(new DrawRelease.Charged(2, 3), DrawRelease.decide(held, 4),
                "four rounds buys step 2 and strands one");
        assertEquals(new DrawRelease.Charged(3, 5), DrawRelease.decide(held, 5),
                "and five buys the top step exactly");
    }

    /**
     * AN EMPTY MAGAZINE IS A SEPARATE REFUSAL FROM THE FLOOR, AND THE ORDER OF THE TWO IS RULED.
     *
     * <p><b>The floor is checked FIRST</b>, so a sub-floor twitch on an empty magazine is silent
     * rather than a "you are out of ammunition" notice. Below the floor nothing happened -- the
     * player has not asked for a shot, and answering a question they did not ask is how a notice
     * becomes noise.
     *
     * <p><b>The last row here is the only thing that distinguishes the two orderings</b>, and it is
     * the reason the two reasons are an enum rather than one {@code Nothing}. Swap the checks and
     * everything else in this file stays green.
     */
    @Test
    void anEmptyMagazineIsItsOwnRefusalAndTheFloorIsCheckedFirst() {
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.NO_ROUNDS),
                DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS, 0),
                "a full draw on an empty magazine: the weapon is dead until reloaded");
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.NO_ROUNDS),
                DrawRelease.decide(DrawRelease.MIN_RELEASE_TICKS, 0),
                "and so is a tap -- R4''' costs a round like any other shot, at every band");
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.NO_ROUNDS),
                DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS, -2),
                "a magazine cannot owe arrows: floored, not thrown");

        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.BELOW_VANILLA_FLOOR),
                DrawRelease.decide(2, 0),
                "BELOW THE FLOOR ON AN EMPTY MAGAZINE IS SILENT, NOT A NOTICE -- nothing happened, "
                        + "so nothing is said. This row is the whole content of the ordering.");
    }
}
