package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CASCADING LOOT RULE (PLAN-loot.md; Ben, 2026-11-19: "First the rarest rarity is rolled. then if that doesn't land,
 * the next rarest rarity is rolled and so on ... Exotic 1% then Legendary 3% then Epic 8% then Rare 12%").
 */
class CascadeTableTest {

    private static final CascadeTable BEN = new CascadeTable(List.of(
            new CascadeTable.Tier(Rarity.EXOTIC, 0.01),
            new CascadeTable.Tier(Rarity.LEGENDARY, 0.03),
            new CascadeTable.Tier(Rarity.EPIC, 0.08),
            new CascadeTable.Tier(Rarity.RARE, 0.12)));

    /** A scripted random source that also records how many draws were taken. */
    private static final class Script implements DoubleSupplier {
        final Deque<Double> next;
        int taken;

        Script(double... draws) {
            next = new ArrayDeque<>();
            for (double d : draws) next.add(d);
        }

        @Override public double getAsDouble() {
            taken++;
            return next.removeFirst();
        }
    }

    @Test
    void theRarestIsRolledFirstAndTheFirstToLandWins() {
        Script s = new Script(0.009);
        assertEquals(Optional.of(Rarity.EXOTIC), BEN.rollRarity(s), "0.009 < 0.01: exotic on the first roll");
        assertEquals(1, s.taken, "and nothing below it is rolled");
        Script t = new Script(0.5, 0.029);
        assertEquals(Optional.of(Rarity.LEGENDARY), BEN.rollRarity(t), "exotic misses, legendary lands");
        assertEquals(2, t.taken);
        Script u = new Script(0.5, 0.5, 0.5, 0.119);
        assertEquals(Optional.of(Rarity.RARE), BEN.rollRarity(u), "each tier rolled in turn, rarest to commonest");
        assertEquals(4, u.taken);
    }

    /** A draw equal to the chance does not land: a draw lands when it is below its chance (a 0 chance never lands). */
    @Test
    void aDrawLandsOnlyBelowItsChance() {
        assertEquals(Optional.of(Rarity.LEGENDARY), BEN.rollRarity(new Script(0.01, 0.0)));
        CascadeTable never = new CascadeTable(List.of(new CascadeTable.Tier(Rarity.RARE, 0.0)));
        assertEquals(Optional.empty(), never.rollRarity(new Script(0.0)), "a 0 chance never lands, even on 0.0");
    }

    @Test
    void whenNothingLandsTheResultIsNothing() {
        Script s = new Script(0.99, 0.99, 0.99, 0.99);
        assertEquals(Optional.empty(), BEN.rollRarity(s));
        assertEquals(4, s.taken, "every tier was rolled");
    }

    /** SEAT READING: only the LAST tier may be guaranteed (1.0), and it always lands when the rest miss. */
    @Test
    void aGuaranteedLastTierAlwaysLandsAndOnlyTheLastMayBeGuaranteed() {
        CascadeTable g = new CascadeTable(List.of(new CascadeTable.Tier(Rarity.RARE, 0.12),
                new CascadeTable.Tier(Rarity.COMMON, 1.0)));
        assertTrue(g.guaranteed());
        assertFalse(BEN.guaranteed());
        assertEquals(Optional.of(Rarity.COMMON), g.rollRarity(new Script(0.99, 0.999999)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of(
                new CascadeTable.Tier(Rarity.EPIC, 1.0), new CascadeTable.Tier(Rarity.COMMON, 0.5))));
        assertTrue(ex.getMessage().contains("only the last tier"), ex.getMessage());
    }

    @Test
    void aTableRefusesNonsenseByItsReason() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of()))
                .getMessage().contains("at least one tier"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of(
                new CascadeTable.Tier(Rarity.RARE, 1.5)))).getMessage().contains("[0, 1]"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of(
                new CascadeTable.Tier(Rarity.RARE, -0.1)))).getMessage().contains("[0, 1]"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of(
                new CascadeTable.Tier(Rarity.RARE, Double.NaN)))).getMessage().contains("[0, 1]"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of(
                new CascadeTable.Tier(Rarity.RARE, 0.1), new CascadeTable.Tier(Rarity.EPIC, 0.1))))
                .getMessage().contains("rarest to commonest"), "out of order");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CascadeTable(List.of(
                new CascadeTable.Tier(Rarity.RARE, 0.1), new CascadeTable.Tier(Rarity.RARE, 0.1))))
                .getMessage().contains("rarest to commonest"), "a rarity twice");
    }

    /** e: THE TRUE ODDS, against Ben's four numbers: tier n's chance times every miss above it. */
    @Test
    void theTrueOddsAreTheChanceTimesTheMissesAbove() {
        CascadeTable.Odds o = BEN.trueOdds();
        assertEquals(0.01, o.of(Rarity.EXOTIC), 1e-12);
        assertEquals(0.99 * 0.03, o.of(Rarity.LEGENDARY), 1e-12, "the written 3% is really 2.97%");
        assertEquals(0.99 * 0.97 * 0.08, o.of(Rarity.EPIC), 1e-12);
        assertEquals(0.99 * 0.97 * 0.92 * 0.12, o.of(Rarity.RARE), 1e-12);
        assertEquals(0.99 * 0.97 * 0.92 * 0.88, o.nothing(), 1e-12);
        double sum = o.nothing();
        for (CascadeTable.Tier t : BEN.tiers()) sum += o.of(t.rarity());
        assertEquals(1.0, sum, 1e-12, "the tiers and nothing make one");
        assertEquals(List.of(Rarity.EXOTIC, Rarity.LEGENDARY, Rarity.EPIC, Rarity.RARE), List.copyOf(o.byTier().keySet()));
        CascadeTable g = new CascadeTable(List.of(new CascadeTable.Tier(Rarity.RARE, 0.5),
                new CascadeTable.Tier(Rarity.COMMON, 1.0)));
        assertEquals(0.0, g.trueOdds().nothing(), 1e-12, "a guaranteed last tier leaves no nothing");
    }

    /** f: 100,000 seeded rolls, each tier's count within 4 standard deviations of the true odds (binomial). */
    @Test
    void aHundredThousandSeededRollsFallWithinFourSigmaOfTheTrueOdds() {
        SplittableRandom rng = new SplittableRandom(20261119L);
        int n = 100_000;
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        int nothing = 0;
        for (int i = 0; i < n; i++) {
            Optional<Rarity> r = BEN.rollRarity(rng::nextDouble);
            if (r.isPresent()) counts.merge(r.get(), 1, Integer::sum);
            else nothing++;
        }
        CascadeTable.Odds o = BEN.trueOdds();
        List<String> report = new ArrayList<>();
        for (CascadeTable.Tier t : BEN.tiers()) {
            double p = o.of(t.rarity());
            double sigma = Math.sqrt(n * p * (1 - p));
            int c = counts.getOrDefault(t.rarity(), 0);
            report.add(t.rarity() + " " + c + " vs " + (n * p));
            assertTrue(Math.abs(c - n * p) <= 4 * sigma, t.rarity() + ": " + c + " against " + n * p + " +- " + 4 * sigma);
        }
        double p = o.nothing();
        assertTrue(Math.abs(nothing - n * p) <= 4 * Math.sqrt(n * p * (1 - p)), "nothing: " + nothing + " " + report);
    }
}
