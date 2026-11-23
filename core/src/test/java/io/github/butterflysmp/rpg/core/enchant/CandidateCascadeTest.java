package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HOW MANY CANDIDATES A SLOT OFFERS: Ben's cascade, 2026-11-23 (the seat's ruling V3).
 *
 * <p>Ben, verbatim: <i>"1 enchant = 100%; 2 enchants = 70%; 3 enchants = 40%; 4 enchants = 10%. I want this roll to be
 * cascading like we did with the loot tables. (First it rolls a 10% chance, if that misses it rolls the 40%, if that
 * misses it rolls the 70% and so on)"</i>. His numbers live in {@code content/enchant_candidates.yml}; the fixture below
 * repeats them so these rows are exact without reading content (the paper test reads the file).
 */
class CandidateCascadeTest {

    private static final CandidateCascade BENS = new CandidateCascade(List.of(
            new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)));

    /** Literal draws in order; running off the end throws, so a draw consumed for a tier NOT rolled is loud. */
    private static DoubleSupplier draws(double... values) {
        int[] cursor = {0};
        return () -> values[cursor[0]++];
    }

    // ---- one decision per already-drawn double: each boundary, just under and at ----------------------------------

    @Test
    void aTierLandsOnADrawJustUnderItsChanceAndMissesAtIt() {
        for (Tier tier : BENS.tiers().subList(0, 3)) {
            assertTrue(CandidateCascade.lands(tier, Math.nextDown(tier.chance())), tier + " just under");
            assertFalse(CandidateCascade.lands(tier, tier.chance()), tier + " at its chance");
        }
        assertTrue(CandidateCascade.lands(new Tier(4, 0.10), 0.0), "0.0 lands any tier above 0");
        assertFalse(CandidateCascade.lands(new Tier(4, 0.0), 0.0), "a 0 chance never lands");
    }

    // ---- the cascade: rarest first, a draw only for a tier rolled --------------------------------------------------

    @Test
    void fourOnTheFirstDrawUnderTenPercent_oneDrawConsumed() {
        assertEquals(4, BENS.count(9, draws(Math.nextDown(0.10))));
    }

    @Test
    void threeWhenTheTenMissesAndTheFortyLands_twoDrawsConsumed() {
        assertEquals(3, BENS.count(9, draws(0.10, Math.nextDown(0.40))));
    }

    @Test
    void twoWhenTenAndFortyMissAndTheSeventyLands_threeDrawsConsumed() {
        assertEquals(2, BENS.count(9, draws(0.10, 0.40, Math.nextDown(0.70))));
    }

    /** The guaranteed tier takes NO draw: three misses and the supplier is never asked a fourth time. */
    @Test
    void oneWhenAllThreeMiss_theGuaranteedTierTakesNoDraw() {
        assertEquals(1, BENS.count(9, draws(0.10, 0.40, 0.70)));
        assertEquals(1, BENS.count(9, draws(0.99, 0.99, 0.99)));
    }

    // ---- the cap and the empty pool --------------------------------------------------------------------------------

    @Test
    void theCountIsCappedAtThePoolsSize() {
        assertEquals(2, BENS.count(2, draws(0.0)), "four rolled, two in the pool");
        assertEquals(1, BENS.count(1, draws(0.0)), "four rolled, one in the pool");
        assertEquals(3, BENS.count(3, draws(0.0)));
        assertEquals(4, BENS.count(4, draws(0.0)));
    }

    /** A pool of 0 gives 0 and consumes NO draw (the supplier here throws if asked). */
    @Test
    void anEmptyPoolGivesZeroAndDrawsNothing() {
        assertEquals(0, BENS.count(0, draws()));
    }

    // ---- the true odds, as the loot table prints them ---------------------------------------------------------------

    /**
     * Four 10%, three 36%, two 37.8%, one 16.2% (the seat's figures, "for the record"). EXECUTED, not predicted: the
     * products are compared within 1e-12, because 0.9 x 0.4 is not exactly 0.36 in binary.
     */
    @Test
    void theTrueOddsAreTheProductOfEachLandAndEveryMissAboveIt() {
        List<Double> odds = BENS.trueOdds();
        assertEquals(4, odds.size());
        assertEquals(0.10, odds.get(0), 1e-12);
        assertEquals(0.36, odds.get(1), 1e-12);
        assertEquals(0.378, odds.get(2), 1e-12);
        assertEquals(0.162, odds.get(3), 1e-12);
        assertEquals(1.0, odds.stream().mapToDouble(Double::doubleValue).sum(), 1e-12, "the guaranteed tier closes it");
    }

    // ---- the content rules, refused by name ------------------------------------------------------------------------

    @Test
    void notRarestFirstIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(3, 0.40), new Tier(4, 0.10), new Tier(1, 1.0))));
        assertTrue(ex.getMessage().contains("rarest first"), ex.getMessage());
    }

    @Test
    void aChanceOutsideZeroToOneIsRefused() {
        var low = assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(2, -0.1), new Tier(1, 1.0))));
        assertTrue(low.getMessage().contains("[0, 1]"), low.getMessage());
        var high = assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(2, 1.5), new Tier(1, 1.0))));
        assertTrue(high.getMessage().contains("[0, 1]"), high.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(2, Double.NaN), new Tier(1, 1.0))));
    }

    @Test
    void aLastTierThatIsNotGuaranteedIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(2, 0.70), new Tier(1, 0.9))));
        assertTrue(ex.getMessage().contains("guaranteed"), ex.getMessage());
    }

    /** A guaranteed tier above another leaves every tier after it unreachable. */
    @Test
    void aGuaranteedTierAboveAnotherIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(3, 1.0), new Tier(1, 1.0))));
        assertTrue(ex.getMessage().contains("unreachable"), ex.getMessage());
    }

    @Test
    void aCountOutsideOneToTheTablesBoundIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(
                new Tier(5, 0.05), new Tier(1, 1.0))));
        assertTrue(ex.getMessage().contains("1.." + EnchantRoll.MAX_CANDIDATES), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of(new Tier(0, 1.0))));
    }

    @Test
    void noTiersIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CandidateCascade(null));
    }

    /** The fail-closed fallback when the content file is refused: one candidate, always. NOT Ben's numbers. */
    @Test
    void theFallbackOffersOneCandidateAndDrawsNothing() {
        assertEquals(1, CandidateCascade.ONE.count(4, draws()));
        assertEquals(List.of(1.0), CandidateCascade.ONE.trueOdds());
    }
}
