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
 * MUT-R6          the +1 dropped: full charge starts a COUNT   -> 5 rows   reading B, ruled against
 * MUT-CAP         the magazine ignored                         -> 1 row    the cap      UNIQUE
 * MUT-PITCH       the ladder made LINEAR instead of geometric  -> 1 row    the ratios   UNIQUE
 * MUT-DIRECTION   the check made two-directional (== not <=)   -> 1 row    cap-silent   UNIQUE
 * </pre>
 *
 * <p><b>{@code MUT-R6} reddens five of eight, and that is the shape of a constant everything depends
 * on</b> rather than a sign of over-coverage: {@code arrowsFor} feeds the cap rows and both
 * disagreement rows as well as its own two.
 *
 * <p><b>{@code MUT-DIRECTION} is the one worth naming.</b> It makes the release check complain when
 * the tracker sits BELOW the time-earned count -- which is R3's cap doing exactly its job on a short
 * magazine, the normal case, and would log a warning on every capped draw a player ever makes. The
 * one-directional comparison is a decision, and this is what guards it.
 */
class DrawChargeTest {

    /**
     * R6: FULL CHARGE IS ARROW ONE. The boundary is asserted on both sides rather than in the
     * middle, because an off-by-one here is the difference between the ruled reading and the one it
     * was ruled against.
     */
    @Test
    void reachingFullDrawGivesTheFirstArrowRatherThanStartingTheCount() {
        assertEquals(0, DrawCharge.arrowsFor(DrawCharge.FULL_DRAW_TICKS - 1),
                "one tick short of full draw has earned nothing");
        assertEquals(1, DrawCharge.arrowsFor(DrawCharge.FULL_DRAW_TICKS),
                "R6: reaching full charge GIVES one arrow -- reading B, where it gives none, was "
                        + "ruled against");
        assertEquals(0, DrawCharge.arrowsFor(0), "and a draw that never started has earned none");
    }

    /** One per charge second after the first, and the same boundary discipline on each step. */
    @Test
    void oneArrowPerChargeSecondAfterFullDraw() {
        int full = DrawCharge.FULL_DRAW_TICKS;
        int c = DrawCharge.CHARGE_SECOND_TICKS;

        assertEquals(1, DrawCharge.arrowsFor(full + c - 1), "one tick short of the second arrow");
        assertEquals(2, DrawCharge.arrowsFor(full + c), "and exactly on it");
        assertEquals(3, DrawCharge.arrowsFor(full + 2 * c));
        assertEquals(DrawCharge.MAX_ARROWS, DrawCharge.arrowsFor(full + (DrawCharge.MAX_ARROWS - 1) * c),
                "five arrows is a 100-tick hold at the ruled numbers");
    }

    /** The ceiling holds however long the bow is held. */
    @Test
    void theCountStopsAtFiveHoweverLongTheDrawIsHeld() {
        assertEquals(DrawCharge.MAX_ARROWS, DrawCharge.arrowsFor(1_000));
        assertEquals(DrawCharge.MAX_ARROWS, DrawCharge.arrowsFor(72_000),
                "the whole vanilla use duration -- the bow never times out, so this is reachable");
    }

    /**
     * R3: THE CAP IS THE MAGAZINE, AND IT BITES BOTH WAYS.
     *
     * <p>The fixture deliberately uses a magazine of TWO against a time-earned FOUR, so the two
     * quantities in the row are different numbers. Equal ones could not tell a cap that works from a
     * cap that is being ignored.
     */
    @Test
    void theCapIsTheLiveMagazineAndTheTicksStopThere() {
        int earnedByTime = DrawCharge.arrowsFor(DrawCharge.FULL_DRAW_TICKS + 3 * DrawCharge.CHARGE_SECOND_TICKS);
        assertEquals(4, earnedByTime, "the fixture stages four earned by time");

        assertEquals(2, DrawCharge.capped(earnedByTime, 2),
                "two rounds left means the ticks stop at two -- the sound never promises an arrow "
                        + "that is not coming");
        assertEquals(4, DrawCharge.capped(earnedByTime, 9),
                "a magazine deeper than the draw does not raise the count");
        assertEquals(0, DrawCharge.capped(earnedByTime, 0),
                "an empty magazine promises nothing at all");
        assertEquals(0, DrawCharge.capped(earnedByTime, -3),
                "and a magazine cannot owe arrows: floored, not thrown, so a bad read gives a dead "
                        + "tracker rather than an exception mid-draw");
    }

    /**
     * R13: THE FIVE STEPS MUST BE TELLABLE APART BY EAR, so the ladder is asserted as a PROPERTY --
     * equal ratios -- rather than as five numbers typed into this file.
     *
     * <p>Pitch is a playback RATE, so an ear hears ratios. Pinning the rounded values from the plan
     * would guard a display rounding; this guards the thing that was ruled.
     */
    @Test
    void thePitchLadderRisesInEqualRATIOSAcrossTheWholeSpan() {
        assertEquals(DrawCharge.PITCH_FLOOR, DrawCharge.pitchFor(1), 1e-6, "the first arrow sits on the floor");
        assertEquals(DrawCharge.PITCH_CEILING, DrawCharge.pitchFor(DrawCharge.MAX_ARROWS), 1e-6,
                "and the last on the ceiling -- the span is used in full");

        double firstRatio = DrawCharge.pitchFor(2) / DrawCharge.pitchFor(1);
        for (int n = 3; n <= DrawCharge.MAX_ARROWS; n++) {
            assertEquals(firstRatio, DrawCharge.pitchFor(n) / DrawCharge.pitchFor(n - 1), 1e-6,
                    "step " + (n - 1) + " -> " + n + " must be the same interval as every other; a "
                            + "LINEAR ladder's top step is 40% smaller than its bottom one, which is "
                            + "the mapping R13 ruled against");
        }
        assertTrue(firstRatio > 1.0, "and it rises");
    }

    /** A miscounted caller cannot walk the pitch off either end of the span. */
    @Test
    void thePitchIsClampedIntoTheSpanRatherThanExtrapolated() {
        assertEquals(DrawCharge.pitchFor(1), DrawCharge.pitchFor(0), 1e-9);
        assertEquals(DrawCharge.pitchFor(1), DrawCharge.pitchFor(-4), 1e-9);
        assertEquals(DrawCharge.pitchFor(DrawCharge.MAX_ARROWS), DrawCharge.pitchFor(99), 1e-9);
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
        int held = DrawCharge.FULL_DRAW_TICKS + 3 * DrawCharge.CHARGE_SECOND_TICKS;   // earns 4

        assertTrue(DrawCharge.disagreement(4, held).isEmpty(), "agreement is silent");
        assertTrue(DrawCharge.disagreement(2, held).isEmpty(),
                "and so is a tracker held down by a two-round magazine -- that IS the cap");
        assertTrue(DrawCharge.disagreement(0, held).isEmpty(), "an empty magazine is not a defect");
    }

    @Test
    void aTrackerAboveTheTimeEarnedCountIsReportedBecauseNoCapCanExplainIt() {
        int held = DrawCharge.FULL_DRAW_TICKS + DrawCharge.CHARGE_SECOND_TICKS;       // earns 2

        var complaint = DrawCharge.disagreement(3, held);
        assertTrue(complaint.isPresent(),
                "three arrows out of a draw that earned two is not something a cap can do");
        assertTrue(complaint.get().contains("3") && complaint.get().contains("2"),
                "and the report carries BOTH numbers, or the reader cannot tell which way it "
                        + "disagreed: " + complaint.get());
    }
}
