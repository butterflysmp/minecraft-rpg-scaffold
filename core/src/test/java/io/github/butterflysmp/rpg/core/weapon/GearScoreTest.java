package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole of the gear-score arithmetic, at the two-second loop.
 *
 * <h2>NO FIXTURE HERE STAGES TWO EQUAL QUANTITIES, DELIBERATELY</h2>
 *
 * CLAUDE.md's collision rule: <i>"two expected values that collide cannot detect a transposition
 * between them."</i> So the scores below are {@code 175}, {@code 250}, {@code 340}, {@code 380} --
 * never {@code 100} against {@code 200}, and never a score equal to an authored damage. A row staged
 * at 100 against 200 survives a mutation that doubles instead of scaling; these do not.
 *
 * <p><b>The one collision that cannot be staged around is named in {@code GearScore}'s own javadoc</b>
 * -- {@code BASELINE}, {@code MIN} and {@code ABSENT} are all 100, so no row here can tell them apart.
 * That is recorded rather than worked around, because there is no staging that separates them.
 *
 * <h2>FLOATING POINT IS MEASURED, NOT PREDICTED</h2>
 *
 * The scaled figures are asserted with a delta except where the value is exactly representable
 * ({@code 19 * 250 / 100 = 47.5}), and the ratio claim is asserted as a CROSS-MULTIPLICATION rather
 * than as a division, which has no rounding to get wrong.
 */
class GearScoreTest {

    // --- the constants and the two clamps -----------------------------------------------------

    /**
     * The range, asserted as values rather than as relationships.
     *
     * <p>Ordering assertions ({@code MIN < SOFT_CAP < HARD_CAP}) would survive every one of them
     * moving together, which is the change most likely to be made by accident.
     */
    @Test
    void theRangeIsOneHundredToFourHundredToFiveHundred() {
        assertEquals(100, GearScore.MIN);
        assertEquals(400, GearScore.SOFT_CAP);
        assertEquals(500, GearScore.HARD_CAP);
        assertEquals(100, GearScore.BASELINE, "GS 100 is the game as it plays today");
        assertEquals(100, GearScore.ABSENT, "no stamp means baseline, not zero");
        assertEquals(6, GearScore.AVERAGE_SLOTS);
        assertEquals(2, GearScore.HAND_SLOTS);
    }

    @Test
    void theReadClampStopsAtTheHARDCapAndTheFloor() {
        assertEquals(340, GearScore.clamp(340), "an ordinary score passes through untouched");
        assertEquals(100, GearScore.clamp(16), "there is no score below 100");
        assertEquals(100, GearScore.clamp(-9000), "nor a negative one off a hand-edited PDC");
        assertEquals(500, GearScore.clamp(9000), "and none above the hard cap");
        assertEquals(450, GearScore.clamp(450),
                "401..500 is the activity tier -- reachable by the READ clamp, so a value that"
                        + " arrives there is not destroyed, even though nothing can roll it yet");
    }

    /**
     * *** THE DROP CLAMP IS 400, AND IT IS A DIFFERENT CEILING FROM THE READ CLAMP'S 500. ***
     *
     * <p>This is the row the {@code MUT-SOFTCAP} mutation reddens. Collapsing the two clamps would let
     * a drop roll into the activity tier, which does not exist yet -- and it would look like a
     * simplification.
     */
    @Test
    void theDropClampStopsAtTheSOFTCapWhereTheReadClampWouldNot() {
        assertEquals(400, GearScore.clampDrop(450),
                "a DROP clamps at 400 -- 401..500 is activity-based and does not exist yet");
        assertEquals(450, GearScore.clamp(450),
                "while the same value read off an item is kept. TWO CEILINGS, TWO RULINGS.");
        assertEquals(340, GearScore.clampDrop(340));
        assertEquals(100, GearScore.clampDrop(16));
    }

    // --- absence -------------------------------------------------------------------------------

    /**
     * *** THE ABSENT=100 CLAIM, IN THE ONLY PLACE A TEST CAN REACH IT. ***
     *
     * <p>Whether a REAL item minted before this slice still deals yesterday's damage cannot be
     * witnessed by any test in any module -- no unit test can construct an {@code ItemStack}. This row
     * asserts the ARITHMETIC half; {@code GATE-gearscore.md} row 2 is the only witness to the other.
     */
    @Test
    void anUnstampedItemReadsAsTheBaselineRatherThanZero() {
        assertEquals(100, GearScore.orAbsent(OptionalInt.empty()),
                "NO STAMP MEANS 100. Zero here would make every item minted before this slice deal"
                        + " nothing, silently, on the first reconcile after the update.");
        assertEquals(340, GearScore.orAbsent(OptionalInt.of(340)));
        assertEquals(500, GearScore.orAbsent(OptionalInt.of(9000)),
                "a present value is still clamped -- present does not mean sane");
        assertEquals(100, GearScore.orAbsent(OptionalInt.of(-5)));
    }

    /** An unstamped item scales nothing at all, which is the whole of "no migration". */
    @Test
    void anUnstampedItemScalesNothing() {
        double authored = 19.0;
        assertEquals(authored, GearScore.scaledDamage(authored, GearScore.ABSENT), 0.0,
                "GS 100 is the identity. If this is not exact, every existing weapon changes.");
        assertEquals(8.0, GearScore.scaledDefense(8.0, GearScore.ABSENT), 0.0);
    }

    // --- the scale -----------------------------------------------------------------------------

    @Test
    void damageScalesLinearlyFromTheBaseline() {
        assertEquals(47.5, GearScore.scaledDamage(19.0, 250), 0.0, "19 * 250 / 100, exactly");
        assertEquals(95.0, GearScore.scaledDamage(19.0, 500), 0.0, "the hard cap is 5x");
        assertEquals(76.0, GearScore.scaledDamage(19.0, 400), 0.0, "the soft cap is 4x");
    }

    /**
     * *** THE RATIO CLAIM: TWO SCORES OF ONE DEFINITION DEAL DAMAGE IN THE RATIO OF THEIR SCORES. ***
     *
     * <p>The unit half of {@code GATE-gearscore.md} row 1. Staged at {@code 175} and {@code 340} --
     * not 100 against 200, and neither a multiple of the other -- so a mutation that doubles, halves
     * or squares cannot satisfy it by coincidence.
     *
     * <p>Asserted as a CROSS-MULTIPLICATION: {@code low * 340 == high * 175} has no division and
     * therefore no rounding to predict.
     */
    @Test
    void twoScoresOfOneDefinitionDealDamageInTheRatioOfTheirScores() {
        double authored = 19.0;
        double low = GearScore.scaledDamage(authored, 175);
        double high = GearScore.scaledDamage(authored, 340);

        assertEquals(low * 340.0, high * 175.0, 1e-9,
                "the ratio of the damages is the ratio of the scores");
        assertTrue(high > low, "and the higher score is the harder hit");
    }

    @Test
    void defenseScalesByTheSameArithmeticAsDamage() {
        // A diamond chestplate's own 8 vanilla points.
        assertEquals(27.2, GearScore.scaledDefense(8.0, 340), 1e-9);
        assertEquals(GearScore.scaledDamage(8.0, 340), GearScore.scaledDefense(8.0, 340), 0.0,
                "one arithmetic behind two named doors -- two literals cannot drift if there is one");
    }

    /** A hand-edited PDC can neither zero a weapon nor push it past the ceiling. */
    @Test
    void anAbsurdStampCannotZeroOrInflateAnItem() {
        assertEquals(19.0, GearScore.scaledDamage(19.0, 0), 0.0, "clamped up to the floor, not zeroed");
        assertEquals(19.0, GearScore.scaledDamage(19.0, -9000), 0.0);
        assertEquals(95.0, GearScore.scaledDamage(19.0, 9000), 0.0, "and capped at 5x, not 90x");
    }

    // --- what carries a score ------------------------------------------------------------------

    /**
     * *** TOOLS DO NOT CARRY A SCORE. THE ROW THE {@code MUT-TOOLS} MUTATION REDDENS. ***
     *
     * <p>A pickaxe must never become one of the top two and raise a drop level without contributing
     * anything to a fight. Before this predicate lived in core the only witness was a boot.
     */
    @Test
    void weaponsArmourAndShieldsScoreAndToolsDoNot() {
        assertTrue(GearScore.scoreable(GearClass.MELEE));
        assertTrue(GearScore.scoreable(GearClass.RANGER));
        assertTrue(GearScore.scoreable(GearClass.MAGE));
        assertTrue(GearScore.scoreable(GearClass.SHIELD));
        assertTrue(GearScore.scoreable(GearClass.ARMOR));

        assertFalse(GearScore.scoreable(GearClass.TOOL),
                "A PICKAXE IS NOT GEAR. Admitting it lets a mining tool raise the level of every"
                        + " drop the player takes, contributing nothing to a fight.");
    }

    /** An item that is none of ours has no kind, and no score. */
    @Test
    void anItemOfNoKindScoresNothing() {
        assertFalse(GearScore.scoreable(null));
    }

    /**
     * Every {@link GearClass} is answered -- so a fifth constant cannot slip through as a default.
     *
     * <p>The switch has no default arm, so this row cannot fail by throwing; what it guards is that
     * nobody ADDS one. It walks {@code values()} rather than naming five, which is the one shape that
     * keeps covering a sixth constant the day it lands.
     */
    @Test
    void everyGearClassIsAnsweredWithoutADefaultArm() {
        int scoring = 0;
        for (GearClass kind : GearClass.values()) {
            if (GearScore.scoreable(kind)) scoring++;
        }
        assertEquals(GearClass.values().length - 1, scoring,
                "exactly one kind -- TOOL -- is refused. A new GearClass arriving as a silent yes or"
                        + " no is what the missing default arm exists to stop.");
    }

    // --- the hand pool -------------------------------------------------------------------------

    /**
     * *** THE TOP TWO, ACROSS HOTBAR AND OFFHAND TOGETHER. THE {@code MUT-TOPONE} ROW. ***
     *
     * <p>The offhand is IN the pool rather than a slot of its own, which is why this takes one array.
     * Staged with the winner in the middle and the runner-up last, so a mutation that takes the first
     * two, or the last two, fails rather than passing on the ordering.
     */
    @Test
    void theTwoHighestHandsCountAndTheRestDoNot() {
        assertArrayEquals(new int[] {340, 250},
                GearScore.topTwo(new int[] {175, 340, 120, 250}),
                "highest first, and the two losers are dropped");
    }

    @Test
    void aShorterHandPoolPadsWithEmptyRatherThanShrinkingTheDenominator() {
        assertArrayEquals(new int[] {250, 0}, GearScore.topTwo(new int[] {250}),
                "one weapon means one empty hand slot, not a five-slot average");
        assertArrayEquals(new int[] {0, 0}, GearScore.topTwo(new int[] {}));
        assertArrayEquals(new int[] {0, 0}, GearScore.topTwo(null));
    }

    /** The caller's array is its own: a scan that reorders its input is a trap for the next reader. */
    @Test
    void theHandPoolIsNotReorderedByReadingIt() {
        int[] pool = {175, 340, 120};
        GearScore.topTwo(pool);
        assertArrayEquals(new int[] {175, 340, 120}, pool);
    }

    // --- the average ---------------------------------------------------------------------------

    /**
     * *** A NEW PLAYER HOLDING ONE SWORD AVERAGES 16. THE {@code MUT-SKIPEMPTY} ROW. ***
     *
     * <p>{@code 100 / 6 = 16}, below the minimum any item can roll -- so the floor does the early work
     * and drops sit at 100 until slots fill broadly. <b>Ben has seen this and accepted it: it is the
     * intended pressure to gear every slot, not a defect.</b> Do not "fix" it with a threshold rule.
     *
     * <p>Skipping the five empty slots would give <b>100</b>, and the new player's first drop would
     * roll at the top of the early band instead of the bottom.
     */
    @Test
    void aNewPlayerHoldingOneBaselineSwordAveragesSixteen() {
        int[] six = GearScore.sixSlots(new int[] {0, 0, 0, 0}, new int[] {100});
        assertEquals(16, GearScore.averageOf(six),
                "100 / 6 = 16. AN EMPTY SLOT COUNTS 0, ALWAYS -- there is no threshold rule, and"
                        + " skipping empties would read 100 here.");
    }

    @Test
    void theAverageIsOverAllSixSlotsWithEmptiesCounted() {
        assertEquals(0, GearScore.averageOf(new int[] {0, 0, 0, 0, 0, 0}), "a naked player");
        // 340 + 250 + 175 + 0 + 400 + 0 = 1165; 1165 / 6 = 194 (floors).
        assertEquals(194, GearScore.averageOf(new int[] {340, 250, 175, 0, 400, 0}));
    }

    /** Floors rather than rounds, and the two differ: 17 would be the rounded answer below. */
    @Test
    void theAverageFloorsRatherThanRounding() {
        // 105 * 6 = 630 exactly, so the fixture is staged one off it: 631 / 6 = 105.17.
        assertEquals(105, GearScore.averageOf(new int[] {106, 105, 105, 105, 105, 105}),
                "631 / 6 = 105.17 -> 105, not 106");
    }

    @Test
    void anAbsurdStampCannotInflateTheAverageAndAnEmptySlotStaysBelowTheFloor() {
        assertEquals(500, GearScore.averageOf(new int[] {9000, 9000, 9000, 9000, 9000, 9000}),
                "every slot clamps to the hard cap");
        assertEquals(0, GearScore.averageOf(new int[] {-9000, 0, 0, 0, 0, 0}),
                "a negative stamp reads as empty, not as a subtraction");
    }

    /**
     * *** AN EMPTY SLOT MUST NOT GO THROUGH THE READ CLAMP. ***
     *
     * <p>{@link GearScore#clamp} raises anything below 100 TO 100, so clamping the six slots with it
     * would turn every empty slot into a baseline item -- a naked player would average 100 and the
     * ruling above would be silently deleted. This row is what separates the two clamps' jobs.
     */
    @Test
    void theAverageUsesItsOwnFloorOfZeroRatherThanTheReadClampsHundred() {
        assertEquals(0, GearScore.averageOf(new int[] {0, 0, 0, 0, 0, 0}),
                "if this reads 100, the six slots were put through clamp() and EMPTY=0 is gone");
    }

    // --- the six-slot assembly -----------------------------------------------------------------

    @Test
    void theSixSlotsAreFourArmourThenTheTwoHighestHands() {
        assertArrayEquals(new int[] {340, 250, 175, 120, 400, 380},
                GearScore.sixSlots(new int[] {340, 250, 175, 120}, new int[] {200, 400, 380}),
                "armour in the order given, then the hands, highest first");
    }

    /**
     * A wrong-length armour array is refused LOUDLY rather than averaged.
     *
     * <p>A five-element armour array would average over seven slots and read as a slightly generous
     * ladder forever, with nothing red. This is the discovery rule: finding the wrong number of things
     * is a defect, not a quiet no-op.
     */
    @Test
    void aWrongLengthArmourArrayIsRefusedRatherThanAveraged() {
        assertThrows(IllegalArgumentException.class,
                () -> GearScore.sixSlots(new int[] {100, 100, 100}, new int[] {100}));
        assertThrows(IllegalArgumentException.class,
                () -> GearScore.sixSlots(new int[] {100, 100, 100, 100, 100}, new int[] {100}));
        assertThrows(IllegalArgumentException.class,
                () -> GearScore.sixSlots(null, new int[] {100}));
    }

    @Test
    void aWrongLengthAverageIsRefusedRatherThanDividedBySix() {
        assertThrows(IllegalArgumentException.class,
                () -> GearScore.averageOf(new int[] {100, 100, 100}));
        assertThrows(IllegalArgumentException.class, () -> GearScore.averageOf(null));
    }

    // --- the roll ------------------------------------------------------------------------------

    /**
     * The band is centred on the average and uniform across it, inclusive at both ends.
     *
     * <p>Staged at a spread the shipped constant does not carry, deliberately: {@link GearScoreBand}
     * ships zero because the number is OWED, and the arithmetic must be reddened at a real width
     * regardless. The band is a PARAMETER for exactly this reason.
     */
    @Test
    void aRollIsUniformAcrossTheBandAndInclusiveAtBothEnds() {
        assertEquals(150, GearScore.roll(200, 50, 0, 0.0), "the bottom of the band is reachable");
        assertEquals(200, GearScore.roll(200, 50, 0, 0.5), "the centre is the average");
        assertEquals(250, GearScore.roll(200, 50, 0, Math.nextDown(1.0)),
                "AND SO IS THE TOP. Measured with the largest draw below 1.0 rather than predicted:"
                        + " an off-by-one here is invisible everywhere except at the cap.");
    }

    /** A skew moves the centre off the average without changing the width. */
    @Test
    void aSkewMovesTheCentreUpwardWithoutWideningTheBand() {
        assertEquals(230, GearScore.roll(200, 50, 30, 0.5), "centre = average + skew");
        assertEquals(180, GearScore.roll(200, 50, 30, 0.0), "the bottom rises by the same 30");
        assertEquals(280, GearScore.roll(200, 50, 30, Math.nextDown(1.0)), "and so does the top");
    }

    /**
     * *** A DROP NEVER EXCEEDS 400, HOWEVER HIGH THE AVERAGE OR THE SKEW. THE {@code MUT-SOFTCAP}
     * ROW, FROM THE ROLL SIDE. ***
     */
    @Test
    void aRollNeverExceedsTheSoftCap() {
        assertEquals(400, GearScore.roll(380, 50, 0, Math.nextDown(1.0)),
                "430 would be the unclamped band top; 401..500 does not exist yet");
        assertEquals(400, GearScore.roll(380, 50, 100, 0.5));
        assertTrue(GearScore.roll(500, 50, 50, Math.nextDown(1.0)) <= GearScore.SOFT_CAP);
    }

    /**
     * *** AND NEVER FALLS BELOW 100, WHICH IS WHAT THE 16-AVERAGE NEW PLAYER MEETS. ***
     *
     * <p>The floor is doing the early work here: at an average of 16 the whole band is under the
     * minimum, so every drop lands at exactly 100 until the player's slots fill.
     */
    @Test
    void aRollNeverFallsBelowTheMinimumSoTheNewPlayersFirstDropIsBaseline() {
        assertEquals(100, GearScore.roll(16, 50, 0, 0.0));
        assertEquals(100, GearScore.roll(16, 50, 0, 0.5));
        assertEquals(100, GearScore.roll(16, 50, 0, Math.nextDown(1.0)),
                "the entire band is below the floor at a 16 average, so every roll is 100");
    }

    /**
     * A zero spread is degenerate on purpose -- it is what {@link GearScoreBand} ships while the
     * number is owed, and it must still produce a legal score rather than dividing by a zero width.
     */
    @Test
    void aZeroSpreadLandsEveryRollOnTheCentre() {
        assertEquals(250, GearScore.roll(250, 0, 0, 0.0));
        assertEquals(250, GearScore.roll(250, 0, 0, 0.5));
        assertEquals(250, GearScore.roll(250, 0, 0, Math.nextDown(1.0)));
        assertEquals(100, GearScore.roll(16, 0, 0, 0.5), "and the floor still applies");
    }

    @Test
    void theShippedBandIsTheOWEDZeroRatherThanAGuess() {
        assertEquals(0, GearScoreBand.SPREAD_OWED,
                "OWED -- Ben's number. A plausible spread here would become a precedent; see"
                        + " GearScoreBand on why zero is the only value that reads as absent.");
        assertEquals(0, GearScoreBand.SKEW_OWED);
    }

    /** A draw outside {@code [0, 1)} is a programming error and is refused loudly. */
    @Test
    void aDrawOutsideTheUnitIntervalIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> GearScore.roll(200, 50, 0, 1.0),
                "1.0 would roll one past the top of the band, which the clamp hides at the cap");
        assertThrows(IllegalArgumentException.class, () -> GearScore.roll(200, 50, 0, -0.1));
        assertThrows(IllegalArgumentException.class, () -> GearScore.roll(200, 50, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> GearScore.roll(200, -1, 0, 0.5));
    }

    /**
     * Every draw in a swept unit interval lands inside the band, and the two ends are both hit.
     *
     * <p>A single mid-band draw would pass against an implementation that ignored the draw entirely.
     * This sweeps, so a band that collapsed to its centre or overran an end is visible.
     */
    @Test
    void aSweptDrawCoversTheWholeBandAndNothingOutsideIt() {
        int average = 250;
        int spread = 20;
        boolean sawBottom = false;
        boolean sawTop = false;
        for (int i = 0; i < 1000; i++) {
            int rolled = GearScore.roll(average, spread, 0, i / 1000.0);
            assertTrue(rolled >= average - spread && rolled <= average + spread,
                    "draw " + (i / 1000.0) + " rolled " + rolled + ", outside the band");
            if (rolled == average - spread) sawBottom = true;
            if (rolled == average + spread) sawTop = true;
        }
        assertTrue(sawBottom, "the bottom of the band must be reachable");
        assertTrue(sawTop, "and the top -- a band that never reaches its top is narrower than stated");
    }
}
