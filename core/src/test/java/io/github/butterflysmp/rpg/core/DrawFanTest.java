package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.DrawCharge;
import io.github.butterflysmp.rpg.core.combat.DrawFan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The fan: ten degrees, centred, and the ruled five reproduced exactly.
 *
 * <h2>THE FIRST ROW IS THE ONE THAT MATTERS AND IT IS PINNED LITERALLY, WHICH IS UNUSUAL HERE</h2>
 *
 * <p>{@code DrawChargeTest} asserts the pitch ladder as a PROPERTY rather than as numbers, because
 * the ladder's numbers are a rounding of a rule. <b>The fan is the other case: the FIVE ANGLES are
 * what the operator ruled</b>, in those words, and the general rule is what was inferred from them.
 * So the five are pinned literally and the rule is checked against them -- <i>a general form must
 * reproduce the worked values it sits above</i>, and here the worked values came first.
 *
 * <h2>THE MEASURED KILL MATRIX -- ALL RUN, NONE ASSERTED</h2>
 *
 * <pre>
 *                                        ruled5  every  span  width  even  nothing
 * MUT-SPACING   SPACING_DEGREES 10 -> 5     X      X      -     X      X      -    (4)
 * MUT-CENTRE    (middle - i) -> (0 - i)     X      X      X     -      X      -    (4)
 * MUT-EVEN      middle -> arrows / 2.0      X      X      X     -      X      -    (4)
 * MUT-EVENONLY  middle -> floor(middle)     -      X      X     -      X      -    (3)
 * MUT-WIDTH     the REJECTED rule, coded    -      X      X     X      X      -    (4)
 * </pre>
 *
 * <h2>THREE THINGS THE MATRIX SAYS THAT READING THE ROWS WOULD NOT</h2>
 *
 * <p><b>1. {@code MUT-WIDTH} LEAVES {@code theRuledFiveAnglesAreReproducedExactly} GREEN.</b> The
 * rejected rule -- hold the 40-degree width, respace -- was coded up and run, and it reproduces the
 * ruled five exactly. <b>So the operator's own five angles cannot distinguish the two candidate
 * rules</b>, which is the claim {@link DrawFan}'s javadoc makes, now measured rather than asserted.
 * {@code aShorterReleaseIsATIGHTERGroup...} is the row that separates them.
 *
 * <p><b>2. {@code theSpacingAndTheCentringHoldAtEveryCount} IS BLIND TO {@code MUT-SPACING}</b>, and
 * that is correct rather than a defect: it asserts neighbours are {@link DrawFan#SPACING_DEGREES}
 * apart, <i>reading the very constant under test</i>, so changing 10 to 5 keeps it self-consistent.
 * It measures the RELATIONSHIP, and the VALUE is guarded only by the rows carrying literal angles.
 * <b>A row that reads the constant it checks is a control that passes for the wrong reason unless
 * something else pins the value.</b>
 *
 * <p><b>3. {@code theEVENCountsAreCorrect...} HAS NO UNIQUE KILL, AND IS NOT THEREBY REMOVABLE.</b>
 * Every mutation reddening it also reddens {@code everyCountIsTenDegreesApart...}. Its job is not
 * guarding: it RECORDS that the even counts are defined-and-unreached, and it asserts in code that
 * no step yields an even arrow count -- provenance a coverage metric cannot see. A sweep pruning by
 * unique kills would delete exactly it.
 *
 * <blockquote><b>AND A CLAIM THAT WAS WRITTEN HERE AND THEN REFUTED BY RUNNING IT.</b> The first
 * draft of this matrix said {@code MUT-EVEN} killed <i>"the two even rows alone"</i>, making them
 * the only thing between that mutation and a green suite. <b>Measured, it kills four</b> --
 * {@code arrows / 2.0} shifts the centre at ODD counts too ({@code n=5} becomes
 * {@code 25 15 5 -5 -15}), so the ruled-five row catches it first. {@code MUT-EVENONLY}, a
 * {@code floor()} that is a genuine no-op at odd counts, is what actually isolates the even
 * behaviour -- and it still kills three. <b>Characterising a mutation instead of running it is the
 * failure {@code CLAUDE.md} names, and the loop costs less than the sentence.</b></blockquote>
 */
class DrawFanTest {

    /** THE RULING, VERBATIM: a five-arrow release fans at 20 / 10 / 0 / -10 / -20. */
    @Test
    void theRuledFiveAnglesAreReproducedExactly() {
        assertArrayEquals(new double[] {20, 10, 0, -10, -20}, DrawFan.offsetsFor(5), 1e-9);
    }

    /**
     * The rule the ruled five were generalised into: ten degrees between neighbours, centred on the
     * caster's aim, at every count.
     */
    @Test
    void everyCountIsTenDegreesApartAndCentredOnZero() {
        assertArrayEquals(new double[] {0}, DrawFan.offsetsFor(1), 1e-9);
        assertArrayEquals(new double[] {5, -5}, DrawFan.offsetsFor(2), 1e-9);
        assertArrayEquals(new double[] {10, 0, -10}, DrawFan.offsetsFor(3), 1e-9);
        assertArrayEquals(new double[] {15, 5, -5, -15}, DrawFan.offsetsFor(4), 1e-9);
    }

    /**
     * THE PROPERTY, ASSERTED OVER A RANGE RATHER THAN AT THE SHIPPED COUNTS.
     *
     * <p>Every neighbouring pair is exactly {@link DrawFan#SPACING_DEGREES} apart and the set sums
     * to zero -- which is what "centred" means without naming any particular angle. Run past
     * {@code MAX_ARROWS} deliberately: the rule is total, and a future ruling raising the count must
     * not need this file reopened.
     */
    @Test
    void theSpacingAndTheCentringHoldAtEveryCount() {
        for (int n = 1; n <= 9; n++) {
            double[] fan = DrawFan.offsetsFor(n);
            assertEquals(n, fan.length, "one offset per arrow at n=" + n);

            double sum = 0;
            for (double angle : fan) sum += angle;
            assertEquals(0.0, sum, 1e-9, "the fan must be centred on the aim at n=" + n);

            for (int i = 1; i < n; i++) {
                assertEquals(DrawFan.SPACING_DEGREES, fan[i - 1] - fan[i], 1e-9,
                        "neighbours must be one spacing apart at n=" + n + ", pair " + i);
            }
        }
    }

    /**
     * WIDTH GROWS WITH THE COUNT, WHICH IS THE HALF OF THE RULING A COUNT-ONLY TEST WOULD MISS.
     *
     * <p>The rejected alternative held the 40-degree WIDTH constant and respaced -- {@code n=3} at
     * {@code 20 0 -20}, {@code n=2} at {@code 20 -20}. <b>Both rules reproduce the ruled five</b>, so
     * the row above cannot tell them apart. This one can: under the width rule every count spans 40
     * degrees, and under the ruled one only {@code n=5} does.
     *
     * <p><b>THAT IS MEASURED, NOT ARGUED. {@code MUT-WIDTH} codes the rejected rule up and runs it:
     * {@code theRuledFiveAnglesAreReproducedExactly} STAYS GREEN</b> and this row goes red. The
     * operator's five angles are genuinely silent on the question this row answers.
     */
    @Test
    void aShorterReleaseIsATIGHTERGroupRatherThanASparserOne() {
        assertEquals(40.0, width(5), 1e-9, "the ruled five span 40 degrees");
        assertEquals(20.0, width(3), 1e-9,
                "and three arrows span 20, NOT 40 -- the width rule would have given 20 / 0 / -20");
        assertEquals(10.0, width(2), 1e-9,
                "two arrows are a tight pair, not the 40-degree split the width rule gives");
        assertEquals(0.0, width(1), 1e-9);
    }

    private static double width(int arrows) {
        double[] fan = DrawFan.offsetsFor(arrows);
        return fan[0] - fan[fan.length - 1];
    }

    /**
     * THE EVEN COUNTS ARE DEFINED AND UNREACHED, AND THESE TWO ROWS ARE THEIR ONLY EXERCISE.
     *
     * <p>R1' makes the charge yield 1, 3 or 5, and R3a makes a capped release fire <b>exactly the
     * tracked step</b> -- so two rounds gives one arrow and four gives three. <b>Nothing in shipped
     * behaviour produces an even count.</b>
     *
     * <p>They are computed by the same expression as the odd ones, so there is no separate branch to
     * rot. <b>MEASURED: this row has NO UNIQUE KILL</b> -- every mutation that reddens it also
     * reddens {@code everyCountIsTenDegreesApartAndCentredOnZero}, including {@code MUT-EVENONLY},
     * which was written specifically to bite at even counts only.
     *
     * <p><b>IT IS KEPT ANYWAY, AND THE REASON IS NOT COVERAGE.</b> It is the row that RECORDS the
     * even counts as defined-and-unreached, and its loop asserts in code the claim the javadoc above
     * makes in prose -- that no step yields an even arrow count. <b>If R1' is ever amended again,
     * that loop goes red and the note above stops being true in the same instant</b>, which is the
     * one thing a prose paragraph cannot do.
     *
     * <p>The even counts become reachable the moment anything yields an even count: a ruling on the
     * increment, a capacity modifier that subtracts, or an overturn of R3a in favour of
     * {@code min(step, rounds)}.
     */
    @Test
    void theEVENCountsAreCorrectThoughNothingShippedCanProduceOne() {
        assertArrayEquals(new double[] {5, -5}, DrawFan.offsetsFor(2), 1e-9);
        assertArrayEquals(new double[] {15, 5, -5, -15}, DrawFan.offsetsFor(4), 1e-9);

        // The claim in the javadoc above, asserted rather than left as prose: no step yields an
        // even count, so nothing downstream of the charge can ask for one.
        for (int step = 1; step <= DrawCharge.MAX_STEPS; step++) {
            assertEquals(1, DrawCharge.arrowsForStep(step) % 2,
                    "step " + step + " must yield an ODD arrow count, or the note above is false");
        }
    }

    /** A release of nothing points nowhere: an empty loop for the caller, not an exception. */
    @Test
    void aReleaseOfNothingHasNoOffsets() {
        assertEquals(0, DrawFan.offsetsFor(0).length);
        assertEquals(0, DrawFan.offsetsFor(-3).length);
    }
}
