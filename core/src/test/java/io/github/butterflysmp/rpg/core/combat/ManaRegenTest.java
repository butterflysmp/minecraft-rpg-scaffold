package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The mana-regeneration bonus surface, and the one unit conversion in the system.
 *
 * <p><b>Every value here was EXECUTED and pasted, never derived</b>, and this class is the reason
 * that rule earns its keep twice over: the two conversions are NOT exact inverses, and the shipped
 * base rate cannot be reproduced from a hand-written per-second literal.
 *
 * <p>Assertions here are EXACT ({@code ==} via a zero delta) wherever the point is bit equality.
 * An epsilon would hide precisely the defect these tests exist to catch.
 *
 * <p>Each test names the mutation it forces red.
 */
class ManaRegenTest {

    /** The shipped base rate, written exactly as {@code RpgPlugin} writes it: a full bar in 100s. */
    private static final double BASE_PER_TICK = 100.0 / (100 * 20);

    /**
     * The PREVIOUS base, kept deliberately.
     *
     * <p>Stats Slice 3 rebalanced mana to a full bar in 100 seconds, and at that base the two
     * division orderings agree exactly -- so the ULP hazard Slice 2 was built around has no live
     * example any more. It is still real; this base is just kind. Keeping the 60-second case as a
     * standing witness is what stops the rule being deleted as unmotivated the next time someone
     * reads the file.
     */
    private static final double RETIRED_60S_BASE_PER_TICK = 100.0 / (60 * 20);

    // --- The conversion --------------------------------------------------------------------------

    @Test
    void perTickDividesByTwentyAndPerSecondMultipliesBackOnROUNDTRIPPABLEValues() {
        assertEquals(0.05, ManaRegen.perTick(1.0), 0.0, "1 mana per second is 0.05 per tick");
        assertEquals(0.25, ManaRegen.perTick(5.0), 0.0);
        assertEquals(1.0, ManaRegen.perTick(20.0), 0.0, "20/s is exactly 1/tick");
        assertEquals(0.0, ManaRegen.perTick(0.0), 0.0);

        assertEquals(1.0, ManaRegen.perSecond(0.05), 0.0);
        assertEquals(20.0, ManaRegen.perSecond(1.0), 0.0);
        // Mutation: divide/multiply by anything but TICKS_PER_SECOND -> every row reddens.
    }

    @Test
    void theTwoConversionsAreNOTExactInversesAndNoTestMayPretendTheyAre() {
        // THE test that stops someone writing `perSecond(perTick(x)) == x` as a law. It is not one.
        // Measured across a grid: the round trip holds for 0.0, 0.5, 1.0, 2.0, 5.0, 20.0 and for the
        // DERIVED per-second base -- and fails for 1.6666666666666667, which is exactly the figure a
        // person would write by hand for "a hundred mana in sixty seconds".
        for (double roundTrips : new double[]{0.0, 0.5, 1.0, 2.0, 5.0, 20.0}) {
            assertEquals(roundTrips, ManaRegen.perSecond(ManaRegen.perTick(roundTrips)), 0.0,
                    roundTrips + " survives a per-second round trip");
        }

        double handWritten = 1.6666666666666667;          // what 100.0/60 gives
        assertNotEquals(handWritten, ManaRegen.perSecond(ManaRegen.perTick(handWritten)),
                "1.6666666666666667 does NOT survive the round trip -- it comes back as "
                        + "1.666666666666667. Asserting the round trip as a general identity would "
                        + "be asserting something false.");
        // Mutation: none needed -- this test's job is to be a standing refutation of a law someone
        // will otherwise write. If it ever goes green in the other direction, the conversion changed.
    }

    // --- The floating-point trap this slice was nearly caught by ---------------------------------

    @Test
    void theSHIPPEDBaseRateSurvivesGoingToSecondsAndBackAgain() {
        assertEquals(BASE_PER_TICK, ManaRegen.perTick(ManaRegen.perSecond(BASE_PER_TICK)), 0.0,
                "tick -> second -> tick is bit-for-bit lossless for the shipped base");
        assertEquals(1.0, ManaRegen.perSecond(BASE_PER_TICK), 0.0,
                "a full bar in 100 seconds is a round 1 mana per second");
        // Mutation: have perTick or perSecond scale by anything but 20 -> reddens.
    }

    @Test
    void theULPHazardHasNoLiveExampleAtTHISBaseAndTheWitnessIsKeptANYWAY() {
        // THE test most likely to be deleted as pointless, so it says why it is not.
        //
        // Slice 2 built the derive-from-ticks rule on a measurement: at the 60-second base,
        // 100/(60*20) and (100/60.0)/20.0 are ONE ULP apart, so reaching the value the other way
        // would have silently re-rated every player on the server. Slice 3 rebalanced to 100
        // seconds -- and at that base the two orderings agree EXACTLY. The hazard did not go away;
        // this base is simply kind.
        assertEquals(BASE_PER_TICK, (100.0 / 100.0) / 20.0, 0.0,
                "at the CURRENT base both orderings give the same double -- which is luck, "
                        + "not a property of the code, and must not be relied on");

        assertNotEquals(RETIRED_60S_BASE_PER_TICK, (100.0 / 60.0) / 20.0,
                "but at the PREVIOUS base they differ by one ULP. The hazard is a property of "
                        + "division ordering, not of any particular divisor -- the next retune "
                        + "picks one at random as far as this is concerned.");

        assertEquals(1.6666666666666665, ManaRegen.perSecond(RETIRED_60S_BASE_PER_TICK), 0.0,
                "the retired base derived ...665, not the ...667 a hand-written 100.0/60 gives");
        // Mutation: none available -- this test asserts a fact about doubles, not about our code.
        // It exists so the single-division form in RpgPlugin keeps a visible reason after the
        // rebalance removed its live example.
    }

    @Test
    void addingAZeroBonusToTheBaseIsBITIDENTICALSoAnUnenchantedPlayerIsUNCHANGED() {
        // The composition the resolver performs, for a player wearing nothing. This is the assertion
        // that says the whole slice is behaviour-preserving for everyone who owns no mana-regen gear.
        assertEquals(BASE_PER_TICK, BASE_PER_TICK + ManaRegen.perTick(ManaRegen.NONE), 0.0,
                "base + perTick(0.0) is exactly base -- perTick(0) is 0.0 and x + 0.0 == x");
        // Mutation: have perTick add or subtract anything -> reddens. Also reddens if NONE is not 0.
    }

    @Test
    void aBonusComposesOntoTheBaseInTICKS() {
        assertEquals(0.1, BASE_PER_TICK + ManaRegen.perTick(1.0), 0.0,
                "the shipped base plus a +1/s piece, executed");
        assertEquals(2.0, ManaRegen.perSecond(BASE_PER_TICK + ManaRegen.perTick(1.0)), 0.0,
                "which the fixture doubles to 2 mana per second -- 10.00/5s on the sheet");
        // Mutation: compose in seconds instead -- perTick(perSecond(base) + 1.0) -> a different
        // double -> reddens.
    }

    // --- The bonus surface, shaped like ManaBank --------------------------------------------------

    @Test
    void boostsIsSTRICTSoAZeroBonusDeclaresNothing() {
        assertFalse(ManaRegen.boosts(ManaRegen.NONE), "0.0 is absent, not a grant");
        assertFalse(ManaRegen.boosts(-0.1), "and a negative certainly is not");
        assertTrue(ManaRegen.boosts(0.0001), "anything above zero is");
        // Mutation: boosts uses >= -> a zero bonus becomes a modifier source and the scan writes a
        // no-op entry every reconcile -> reddens.
    }

    @Test
    void contributionIsTheBonusItselfButNamedSoAFutureCurveHasSomewhereToLive() {
        assertEquals(1.0, ManaRegen.contribution(1.0), 0.0, "identity today");
        assertEquals(0.0, ManaRegen.contribution(0.0), 0.0);
        // Mutation: contribution halves its input -> the fixture's +1.0/s resolves to 0.5 -> reddens.
    }
}
