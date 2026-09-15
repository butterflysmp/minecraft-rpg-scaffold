package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.DrawCharge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Dragon's Plume's charge, as arithmetic -- every decision slice H1 makes, reachable without a
 * server.
 *
 * <h2>THE MUTATION MATRIX -- ALL RUN, NONE ASSERTED</h2>
 *
 * <pre>
 * MUT-R6STEP    the +1 dropped: the increment applies to step 1 too  -> 4 rows  reading B again
 * MUT-STRAND    affordableStep -> min(step, rounds), the rejected    -> 1 row   UNIQUE
 *               alternative that strands nothing
 * MUT-LADDER    pitchFor re-indexed by MAX_ARROWS, the "sampled the  -> 2 rows
 *               old five" error
 * MUT-CAP       the magazine ignored                                 -> 1 row   the cap    UNIQUE
 * MUT-PITCH     the ladder made LINEAR instead of geometric          -> 1 row   the ratios UNIQUE
 * MUT-DIRECTION the check made two-directional (== not &lt;=)            -> 1 row   cap-silent UNIQUE
 * </pre>
 *
 * <p><b>{@code MUT-R6STEP} reddens four, and that is the shape of a constant everything depends
 * on</b> rather than a sign of over-coverage: {@code arrowsForStep} feeds both cap rows as well as
 * its own two.
 *
 * <h2>MEASURED: THE OLD CAP ROW IS COMPLETELY BLIND TO THE NEW RULING, AND THAT IS WHY BOTH EXIST</h2>
 *
 * <p>{@code MUT-STRAND} reddens {@code aMagazineThatCannotPayForTheNextSTEPStrands...} and <b>nothing
 * else</b> -- {@code theCapIsTheLiveMagazineAndTheTicksStopThere} stays green under it. Its fixture
 * stages 1, 9, 0 and -3 rounds, and <b>the ruled rule and the rejected one give the same answer at
 * every one of them.</b> They differ only where the cap lands BETWEEN steps, which is 2 and 4.
 *
 * <p>So the stranding ruling has <b>exactly one guard</b>, and it is a row whose name is about
 * stranding rather than about the cap. Said here and in the row itself, because a row that quietly
 * became load-bearing for something outside its name is the row the next tidy-up deletes.
 *
 * <p><b>{@code MUT-DIRECTION} is the other one worth naming.</b> It makes the release check complain
 * when the tracker sits BELOW the time-earned count -- which is R3's cap doing exactly its job on a
 * short magazine, the normal case, and would log a warning on every capped draw a player ever makes.
 * The one-directional comparison is a decision, and this is what guards it.
 */
class DrawChargeTest {

    /**
     * R6: FULL CHARGE IS STEP ONE, WORTH ONE ARROW. The boundary is asserted on both sides rather
     * than in the middle, because an off-by-one here is the difference between the ruled reading and
     * the one it was ruled against.
     *
     * <p><b>R6 SURVIVED R1's AMENDMENT UNCHANGED, WHICH IS WHY THIS ROW DID NOT MOVE.</b> The
     * increment went from +1 to +2 and the first step's yield stayed at one -- so a reader who
     * expects this row to have become {@code 2} is reading the increment into the base, which is
     * exactly what {@code arrowsForStep}'s {@code 1 + (step-1)*2} refuses.
     */
    @Test
    void reachingFullDrawGivesTheFirstArrowRatherThanStartingTheCount() {
        assertEquals(0, DrawCharge.stepsFor(DrawCharge.FULL_DRAW_TICKS - 1),
                "one tick short of full draw has earned nothing");
        assertEquals(1, DrawCharge.stepsFor(DrawCharge.FULL_DRAW_TICKS),
                "R6: reaching full charge GIVES a step -- reading B, where it gives none, was "
                        + "ruled against");
        assertEquals(1, DrawCharge.arrowsFor(DrawCharge.FULL_DRAW_TICKS),
                "and that step is worth ONE arrow, not ARROWS_PER_STEP: the increment does not "
                        + "apply to the first step");
        assertEquals(0, DrawCharge.stepsFor(0), "and a draw that never started has earned none");
    }

    /**
     * R1 AS AMENDED: 1 / 3 / 5, TWO ARROWS PER STEP, THREE STEPS.
     *
     * <p>The row this replaced read <i>one arrow per charge second</i> and asserted
     * {@code 1, 2, 3, ... 5} over four seconds. <b>The amendment is the yield and the count, not
     * the second</b> -- {@code c} is untouched, so every boundary below is still at the same tick
     * it was.
     */
    @Test
    void twoArrowsPerChargeSecondAfterFullDrawAndThreeStepsInAll() {
        int full = DrawCharge.FULL_DRAW_TICKS;
        int c = DrawCharge.CHARGE_SECOND_TICKS;

        assertEquals(1, DrawCharge.arrowsFor(full + c - 1), "one tick short of the second step");
        assertEquals(3, DrawCharge.arrowsFor(full + c), "and exactly on it: +2, not +1");
        assertEquals(5, DrawCharge.arrowsFor(full + 2 * c), "the third step, and the maximum");

        assertEquals(3, DrawCharge.stepsFor(full + 2 * c), "which is three steps, not five");
        assertEquals(52, full + 2 * c,
                "R14: a full charge is 52 ticks -- 2.6 SECONDS. This row asserted 60 until c went "
                        + "20 -> 16, and it is the ONLY row in this file that reddened, because "
                        + "every other one is written against the constants rather than the clock");
    }

    /**
     * THE DERIVED FIVE REPRODUCES THE RULED FIVE.
     *
     * <p>{@code MAX_ARROWS} stopped being authored and became {@code 1 + (MAX_STEPS-1)*
     * ARROWS_PER_STEP}. <b>A general form must reproduce the worked values it sits above</b>, and
     * the cheapest way to find out that it does not is this line.
     */
    @Test
    void theDerivedMaxArrowsIsStillTheRuledFive() {
        assertEquals(5, DrawCharge.MAX_ARROWS);
        assertEquals(3, DrawCharge.MAX_STEPS);
        assertEquals(2, DrawCharge.ARROWS_PER_STEP);
        assertEquals(DrawCharge.MAX_ARROWS, DrawCharge.arrowsForStep(DrawCharge.MAX_STEPS),
                "and the top step is worth exactly the maximum -- no step yields an arrow the "
                        + "magazine model does not know about");
    }

    /** The ceiling holds however long the bow is held. */
    @Test
    void theCountStopsAtThreeStepsHoweverLongTheDrawIsHeld() {
        assertEquals(DrawCharge.MAX_STEPS, DrawCharge.stepsFor(1_000));
        assertEquals(DrawCharge.MAX_ARROWS, DrawCharge.arrowsFor(1_000));
        assertEquals(DrawCharge.MAX_STEPS, DrawCharge.stepsFor(72_000),
                "the whole vanilla use duration -- the bow never times out, so this is reachable");
    }

    /**
     * R3: THE CAP IS THE MAGAZINE, AND IT BITES BOTH WAYS.
     *
     * <p>The fixture stages a full THREE steps earned by time against magazines that cannot pay for
     * them, so no two quantities in a row are equal. Equal ones could not tell a cap that works from
     * a cap that is being ignored.
     */
    @Test
    void theCapIsTheLiveMagazineAndTheTicksStopThere() {
        int earned = DrawCharge.stepsFor(DrawCharge.FULL_DRAW_TICKS + 2 * DrawCharge.CHARGE_SECOND_TICKS);
        assertEquals(3, earned, "the fixture stages a full three steps earned by time");

        assertEquals(1, DrawCharge.affordableStep(earned, 1),
                "one round pays for step 1 and no more -- the sound never promises an arrow that "
                        + "is not coming");
        assertEquals(DrawCharge.MAX_STEPS, DrawCharge.affordableStep(earned, 9),
                "a magazine deeper than the draw does not raise the count");
        assertEquals(0, DrawCharge.affordableStep(earned, 0),
                "an empty magazine promises nothing at all");
        assertEquals(0, DrawCharge.affordableStep(earned, -3),
                "and a magazine cannot owe arrows: floored, not thrown, so a bad read gives a dead "
                        + "tracker rather than an exception mid-draw");
    }

    /**
     * THE RULING R1's AMENDMENT MADE NECESSARY: THE CAP CAN NOW LAND BETWEEN STEPS.
     *
     * <p>Under one arrow per step every round count was some step's exact yield, so the cap always
     * landed ON a step and there was nothing to rule. <b>Under 1/3/5 two rounds pays for step 1 and
     * not step 3, and four pays for step 2 and not step 3.</b>
     *
     * <p><b>RULED: the ticks stop when the next step is unaffordable, and the release fires exactly
     * the tracked step.</b> The remainder is STRANDED until a reload. The alternative --
     * {@code min(stepYield, rounds)}, stranding nothing -- would fire more arrows than the sound
     * announced and wider than the fan showed, putting three readouts in disagreement to save the
     * player a round they clear with a reload they were going to make anyway.
     *
     * <p><b>THIS ROW IS THE WHOLE DIFFERENCE BETWEEN THE TWO RULES, AND IT IS WHY THE FIXTURE USES
     * 2 AND 4 RATHER THAN 1 AND 3.</b> At one round and three rounds both rules agree, so a row
     * staged there would pass under either and measure nothing.
     */
    @Test
    void aMagazineThatCannotPayForTheNextSTEPStrandsTheRemainderRatherThanFiringIt() {
        int earned = DrawCharge.MAX_STEPS;

        assertEquals(1, DrawCharge.affordableStep(earned, 2),
                "two rounds: step 1 (one arrow), ONE STRANDED -- not two arrows, which no step is "
                        + "worth and no tick announced");
        assertEquals(1, DrawCharge.arrowsForStep(DrawCharge.affordableStep(earned, 2)),
                "so the release fires ONE arrow on two rounds");

        assertEquals(2, DrawCharge.affordableStep(earned, 4),
                "four rounds: step 2 (three arrows), ONE STRANDED");
        assertEquals(3, DrawCharge.arrowsForStep(DrawCharge.affordableStep(earned, 4)),
                "so the release fires THREE arrows on four rounds");

        assertEquals(2, DrawCharge.affordableStep(earned, 3), "three rounds pays step 2 exactly");
        assertEquals(DrawCharge.MAX_STEPS, DrawCharge.affordableStep(earned, 5),
                "and five pays the top step exactly, stranding nothing");
    }

    /**
     * THE CAP NEVER RAISES THE STEP ABOVE WHAT TIME EARNED, WHICH THE SEARCH DOWNWARD COULD HAVE
     * GOT WRONG.
     *
     * <p>{@code affordableStep} walks DOWN from the earned step, so a deep magazine cannot push it
     * up -- but a mutation that walked up from zero to the affordable step would pass every row
     * above, because those all stage a FULL three steps earned. This one stages a half-charged draw
     * against a full magazine, which is the common case in play and the only staging where the two
     * implementations differ.
     */
    @Test
    void aDeepMagazineDoesNotRaiseTheStepAboveWhatTheDrawEarned() {
        assertEquals(1, DrawCharge.affordableStep(1, 25),
                "a one-step draw on a full 25-round magazine is still one step");
        assertEquals(2, DrawCharge.affordableStep(2, 25), "and a two-step draw is still two");
    }

    /**
     * R13: THE STEPS MUST BE TELLABLE APART BY EAR, so the ladder is asserted as a PROPERTY --
     * equal ratios, both endpoints -- rather than as numbers typed into this file.
     *
     * <p>Pitch is a playback RATE, so an ear hears ratios. Pinning the rounded values from the plan
     * would guard a display rounding; this guards the thing that was ruled.
     *
     * <p><b>AND IT IS WHY R1's AMENDMENT COST NO RENUMBERING.</b> The row is indexed by
     * {@link DrawCharge#MAX_STEPS}, so going from five steps to three re-evaluated the rule and this
     * row followed it. A row that had pinned {@code 0.80 1.01 1.27 1.59 2.00} would have gone red
     * for the right reason and been repaired with the wrong numbers.
     */
    @Test
    void thePitchLadderRisesInEqualRATIOSAcrossTheWholeSpan() {
        assertEquals(DrawCharge.PITCH_FLOOR, DrawCharge.pitchFor(1), 1e-6, "the first step sits on the floor");
        assertEquals(DrawCharge.PITCH_CEILING, DrawCharge.pitchFor(DrawCharge.MAX_STEPS), 1e-6,
                "and the last on the ceiling -- the span is used in full");

        double firstRatio = DrawCharge.pitchFor(2) / DrawCharge.pitchFor(1);
        for (int n = 3; n <= DrawCharge.MAX_STEPS; n++) {
            assertEquals(firstRatio, DrawCharge.pitchFor(n) / DrawCharge.pitchFor(n - 1), 1e-6,
                    "step " + (n - 1) + " -> " + n + " must be the same interval as every other; a "
                            + "LINEAR ladder's top step is smaller than its bottom one, which is "
                            + "the mapping R13 ruled against");
        }
        assertTrue(firstRatio > 1.0, "and it rises");
    }

    /**
     * THE STEP IS WIDER THAN IT WAS, AND THAT IS THE AMENDMENT'S ONE EFFECT ON R13.
     *
     * <p>Three steps across the same span is a ratio of {@code (2.0/0.8)^(1/2) = 1.5811} against the
     * old {@code (2.0/0.8)^(1/4) = 1.2574} -- about EIGHT semitones a step instead of four.
     *
     * <p><b>Asserted as an inequality against the old ratio rather than as 1.5811</b>, because the
     * ruled thing is the rule and not the value: what H-1b's difficulty depends on is that the steps
     * got FURTHER APART, which is true for any reduction in the count.
     */
    @Test
    void threeStepsAcrossTheSameSpanAreFurtherApartThanFiveWere() {
        double ratio = DrawCharge.pitchFor(2) / DrawCharge.pitchFor(1);
        double oldFiveStepRatio = Math.pow(DrawCharge.PITCH_CEILING / DrawCharge.PITCH_FLOOR, 1.0 / 4);

        assertTrue(ratio > oldFiveStepRatio,
                "fewer steps across an unchanged span must be further apart: " + ratio + " vs "
                        + oldFiveStepRatio);
        // 1e-6, not 1e-9: pitchFor returns a FLOAT, so a ratio of two of them carries about 1e-8
        // of rounding. Measured, not guessed -- 1e-9 failed at 1.5811388492584229 against
        // 1.5811388300841898, which is the float/double seam and not a defect in the ladder.
        assertEquals(Math.sqrt(DrawCharge.PITCH_CEILING / DrawCharge.PITCH_FLOOR), ratio, 1e-6,
                "and at three steps the ratio is the SQUARE ROOT of the whole span, which is the "
                        + "rule re-evaluated rather than three of the old five sampled");
    }

    /** A miscounted caller cannot walk the pitch off either end of the span. */
    @Test
    void thePitchIsClampedIntoTheSpanRatherThanExtrapolated() {
        assertEquals(DrawCharge.pitchFor(1), DrawCharge.pitchFor(0), 1e-9);
        assertEquals(DrawCharge.pitchFor(1), DrawCharge.pitchFor(-4), 1e-9);
        assertEquals(DrawCharge.pitchFor(DrawCharge.MAX_STEPS), DrawCharge.pitchFor(99), 1e-9);
        assertEquals(DrawCharge.pitchFor(DrawCharge.MAX_STEPS), DrawCharge.pitchFor(DrawCharge.MAX_ARROWS),
                "AND A CALLER STILL PASSING AN ARROW COUNT IS CLAMPED RATHER THAN EXTRAPOLATED -- "
                        + "pitchFor(5) is pitchFor(3). That is a safety net, NOT a licence: it "
                        + "would play the top of the ladder on every step from 3 up, silently");
    }

    /**
     * THE TWO MEASURES, AND THE CHECK IS ONE-DIRECTIONAL BECAUSE THE CAP EXPLAINS ONE SIDE.
     *
     * <p>A tracker BELOW the time-earned count is R3 doing its job on a short magazine -- the normal
     * case, and silent. A tracker ABOVE it cannot be explained by any cap, because no cap
     * manufactures an arrow: that is the tracker and the platform disagreeing about the length of
     * the draw, and it has no other detector.
     */
    @Test
    void aTrackerBelowTheTimeEarnedCountIsTheCapAndIsSilent() {
        int held = DrawCharge.FULL_DRAW_TICKS + 2 * DrawCharge.CHARGE_SECOND_TICKS;   // earns step 3

        assertTrue(DrawCharge.disagreement(3, held).isEmpty(), "agreement is silent");
        assertTrue(DrawCharge.disagreement(1, held).isEmpty(),
                "and so is a tracker held down to step 1 by a short magazine -- that IS the cap");
        assertTrue(DrawCharge.disagreement(0, held).isEmpty(), "an empty magazine is not a defect");
    }

    @Test
    void aTrackerAboveTheTimeEarnedCountIsReportedBecauseNoCapCanExplainIt() {
        int held = DrawCharge.FULL_DRAW_TICKS + DrawCharge.CHARGE_SECOND_TICKS;       // earns step 2

        var complaint = DrawCharge.disagreement(3, held);
        assertTrue(complaint.isPresent(),
                "step 3 out of a draw that earned step 2 is not something a cap can do");
        assertTrue(complaint.get().contains("step 3") && complaint.get().contains("step 2"),
                "and the report carries BOTH steps, or the reader cannot tell which way it "
                        + "disagreed: " + complaint.get());
        assertTrue(complaint.get().contains("5 arrows"),
                "AND IT SPELLS THE STEP OUT IN ARROWS, because the operator reading the log is "
                        + "looking at a bow that fired something: " + complaint.get());
    }
}
