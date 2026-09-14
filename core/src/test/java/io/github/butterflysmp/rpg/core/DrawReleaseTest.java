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
        assertInstanceOf(DrawRelease.Tap.class, DrawRelease.decide(3, 25),
                "and exactly on it the release becomes a tap");
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
     * R4': A PARTIAL DRAW IS A TAP, AND THE BOUNDARY WITH A ONE-ARROW CHARGED RELEASE IS ONE TICK
     * WIDE.
     *
     * <p><b>Both fire a single arrow and they are different shots</b> -- 12 and no homing against 48
     * and homing. That is R4 overturned rather than amended, and this row is where the overturn is
     * observable at all: a test asserting only "a partial draw fires one arrow" would pass under the
     * ruling it replaced.
     */
    @Test
    void nineteenTicksIsATapAndTwentyIsAChargedReleaseOfONEArrow() {
        assertInstanceOf(DrawRelease.Tap.class, DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS - 1, 25),
                "one tick short of full draw is R4's tap: one arrow, no homing, 12");
        assertEquals(new DrawRelease.Charged(1, 1),
                DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS, 25),
                "and full draw is R6's FIRST STEP: one arrow, homing, 48 -- the same count, a "
                        + "different shot");
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
                "and so is a tap -- R4' costs a round like any other shot");
        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.NO_ROUNDS),
                DrawRelease.decide(DrawCharge.FULL_DRAW_TICKS, -2),
                "a magazine cannot owe arrows: floored, not thrown");

        assertEquals(new DrawRelease.Nothing(DrawRelease.Reason.BELOW_VANILLA_FLOOR),
                DrawRelease.decide(2, 0),
                "BELOW THE FLOOR ON AN EMPTY MAGAZINE IS SILENT, NOT A NOTICE -- nothing happened, "
                        + "so nothing is said. This row is the whole content of the ordering.");
    }
}
