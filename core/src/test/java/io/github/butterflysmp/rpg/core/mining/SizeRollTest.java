package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's S2 rulings 2 and 3 (2026-11-14): a patch rolls its size from the ore's weights,
 * RESTRICTED to the sizes that fit the slot (width AND height both at most the maximum's), with the
 * weights RENORMALISED over those. NAME THE QUANTITY: a weight is a relative integer, and a size's
 * chance is its weight over the sum of the eligible weights.
 */
class SizeRollTest {

    /** Records the bound of every {@code nextInt(0, bound)}, and answers a scripted value. */
    private static final class Recording implements RandomGenerator {
        final List<Integer> bounds = new java.util.ArrayList<>();
        private final int answer;
        Recording(int answer) { this.answer = answer; }
        @Override public int nextInt(int origin, int bound) {
            assertEquals(0, origin, "a weighted roll draws from 0");
            bounds.add(bound);
            return answer;
        }
        @Override public long nextLong() { throw new UnsupportedOperationException(); }
    }

    private static List<String> ids(List<VeinSize> sizes) {
        return sizes.stream().map(VeinSize::id).toList();
    }

    // --- The fit rule ---------------------------------------------------------------------------

    @Test
    void aSizeFitsWhenBothItsWidthAndItsHeightAreAtMostTheMaximums() {
        assertTrue(SizeRoll.fits(MiningFixtures.small(), MiningFixtures.extraLarge()));
        assertTrue(SizeRoll.fits(MiningFixtures.large(), MiningFixtures.extraLarge()), "7 x 3 in 7 x 4");
        assertTrue(SizeRoll.fits(MiningFixtures.medium(), MiningFixtures.medium()), "a size fits itself");
        assertFalse(SizeRoll.fits(MiningFixtures.extraLarge(), MiningFixtures.large()),
                "7 x 4 in 7 x 3: the width fits and the HEIGHT does not");
        assertFalse(SizeRoll.fits(MiningFixtures.large(), MiningFixtures.medium()),
                "7 x 3 in 5 x 3: the height fits and the WIDTH does not");
        // Mutations: drop the height half -> the first assertFalse reddens; drop the width half -> the
        // second reddens. Each half has a row in which it is the ONLY half that refuses.
    }

    @Test
    void theEligibleSizesForEachShippedMaximum() {
        OreDefinition iron = MiningFixtures.iron();
        VeinSizeRegistry sizes = MiningFixtures.sizes();
        assertEquals(List.of("small"), ids(SizeRoll.eligible(iron, MiningFixtures.small(), sizes)));
        assertEquals(List.of("small", "medium"), ids(SizeRoll.eligible(iron, MiningFixtures.medium(), sizes)));
        assertEquals(List.of("small", "medium", "large"), ids(SizeRoll.eligible(iron, MiningFixtures.large(), sizes)));
        assertEquals(List.of("small", "medium", "large", "extra_large"),
                ids(SizeRoll.eligible(iron, MiningFixtures.extraLarge(), sizes)));
    }

    @Test
    void onlySizesTheOreListsAreEligible() {
        Map<String, Integer> w = new LinkedHashMap<>();
        w.put("small", 50);
        w.put("large", 15);
        OreDefinition ore = MiningFixtures.ironListing(w);
        assertEquals(List.of("small", "large"),
                ids(SizeRoll.eligible(ore, MiningFixtures.extraLarge(), MiningFixtures.sizes())));
    }

    // --- The renormalised roll ------------------------------------------------------------------

    @Test
    void theDrawIsOverTheSumOfTheEligibleWeightsOnly() {
        OreDefinition iron = MiningFixtures.iron();
        VeinSizeRegistry sizes = MiningFixtures.sizes();
        int[][] maxAndBound = {{0, 50}, {1, 80}, {2, 95}, {3, 100}};
        List<VeinSize> maxes = List.of(MiningFixtures.small(), MiningFixtures.medium(),
                MiningFixtures.large(), MiningFixtures.extraLarge());
        for (int[] row : maxAndBound) {
            Recording rng = new Recording(0);
            SizeRoll.roll(iron, maxes.get(row[0]), sizes, rng);
            assertEquals(List.of(row[1]), rng.bounds, "max " + maxes.get(row[0]).id());
        }
        // Mutation: draw over the sum of ALL the ore's weights (100) -> every row but extra_large reddens.
    }

    @Test
    void eachEligibleSizeOwnsItsWeightsSliceOfTheDraw() {
        OreDefinition iron = MiningFixtures.iron();
        VeinSizeRegistry sizes = MiningFixtures.sizes();
        VeinSize xl = MiningFixtures.extraLarge();
        // [0, 50) small, [50, 80) medium, [80, 95) large, [95, 100) extra_large: both ends of each slice.
        int[][] drawAndSize = {{0, 0}, {49, 0}, {50, 1}, {79, 1}, {80, 2}, {94, 2}, {95, 3}, {99, 3}};
        List<String> names = List.of("small", "medium", "large", "extra_large");
        for (int[] row : drawAndSize) {
            assertEquals(names.get(row[1]), SizeRoll.roll(iron, xl, sizes, ScriptedRandom.of(row[0])).id(),
                    "draw " + row[0]);
        }
        // A medium slot: the same draw 79 lands on medium, never on large.
        assertEquals("medium", SizeRoll.roll(iron, MiningFixtures.medium(), sizes, ScriptedRandom.of(79)).id());
        // ScriptedRandom refuses 80 there: it is outside [0, 80), which is the renormalisation.
        assertThrows(IllegalStateException.class,
                () -> SizeRoll.roll(iron, MiningFixtures.medium(), sizes, ScriptedRandom.of(80)));
        // Mutation: "<" -> "<=" in the walk -> draw 49 lands on medium -> reddens.
    }

    @Test
    void aSeededSweepOfAMediumSlotRollsOnlySmallAndMediumNearFiveEighthsSmall() {
        OreDefinition iron = MiningFixtures.iron();
        VeinSizeRegistry sizes = MiningFixtures.sizes();
        Map<String, Integer> counts = new HashMap<>();
        SplittableRandom rng = new SplittableRandom(20261114L);
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            counts.merge(SizeRoll.roll(iron, MiningFixtures.medium(), sizes, rng).id(), 1, Integer::sum);
        }
        assertEquals(java.util.Set.of("small", "medium"), counts.keySet(), counts.toString());
        double smallShare = counts.get("small") / (double) n;
        assertEquals(50.0 / 80.0, smallShare, 0.01, "renormalised: 50 / (50 + 30)");
    }

    @Test
    void noEligibleSizeIsAnErrorThatShippedContentCannotReach() {
        // UNREACHABLE from shipped content: /rpg mining place refuses a maximum the ore does not list,
        // and a listed maximum always fits itself. This row is the guard's only exercise.
        Map<String, Integer> w = new LinkedHashMap<>();
        w.put("large", 15);
        OreDefinition ore = MiningFixtures.ironListing(w);
        assertThrows(IllegalStateException.class,
                () -> SizeRoll.roll(ore, MiningFixtures.small(), MiningFixtures.sizes(), ScriptedRandom.of(0)));
    }
}
