package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The yield: one roll per spot, each drawn independently from {@code per_spot} inclusive, plus the
 * size's completion bonus. Ben, 2026-11-13: "2-4 per spot with a 5 completion bonus". For Small iron
 * (2 to 3 spots) that is 9 to 17 raw iron per break (PLAN-mining.md 5.2, executed).
 */
class PayoutTest {

    private static final PayoutRule IRON_SMALL = new PayoutRule(2, 4, 5);

    @Test
    void theLowEndIsNineAndTheHighEndSeventeen() {
        Payout low = IRON_SMALL.roll(2, ScriptedRandom.alwaysMin());
        assertEquals(List.of(2, 2), low.rolls());
        assertEquals(9, low.total(), "2 + 2 + 5");

        Payout high = IRON_SMALL.roll(3, ScriptedRandom.alwaysMax());
        assertEquals(List.of(4, 4, 4), high.rolls());
        assertEquals(17, high.total(), "4 + 4 + 4 + 5");
        // Mutation: per_spot bound max -> max - 1 (an exclusive upper bound) -> 14 -> reddens.
    }

    @Test
    void eachSpotIsRolledIndependently() {
        // Three DIFFERENT scripted rolls, so a rule that rolled once and multiplied by the spot count
        // (3 x 2 = 6, or 3 x 4 = 12) cannot produce the same total.
        Payout p = IRON_SMALL.roll(3, ScriptedRandom.of(2, 3, 4));
        assertEquals(List.of(2, 3, 4), p.rolls());
        assertEquals(5, p.bonus());
        assertEquals(14, p.total(), "2 + 3 + 4 + 5");
        // Mutation: roll once, repeat it for every spot -> rolls [2, 2, 2] -> reddens.
    }

    @Test
    void tenThousandSeededBreaksStayInNineToSeventeenAndReachBothEnds() {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (long seed = 0; seed < 10_000; seed++) {
            SplittableRandom rng = new SplittableRandom(seed);
            int spots = 2 + rng.nextInt(2);
            Payout p = IRON_SMALL.roll(spots, rng);
            assertEquals(spots, p.rolls().size());
            for (int r : p.rolls()) assertTrue(r >= 2 && r <= 4, "seed " + seed + ": " + p.rolls());
            assertEquals(p.rolls().stream().mapToInt(Integer::intValue).sum() + 5, p.total());
            min = Math.min(min, p.total());
            max = Math.max(max, p.total());
        }
        assertEquals(9, min);
        assertEquals(17, max);
    }

    // --- S2: every size's executed range (the seat's ruling 5), both ends pinned ------------------
    //
    // per_spot 2..4 for every size (Ben: "yes"); bonus small 5, medium 6, large 8, extra_large 10
    // (Ben: "This is fine for now"); spots from the design doc, relayed. Executed in jshell (PLAN 5.12):
    //   small 9..17, medium 14..26, large 20..36, extra_large 26..46.

    private static void assertEnds(VeinSize size, int low, int high) {
        PayoutRule rule = MiningFixtures.iron().payoutRule(size.id());
        assertEquals(low, rule.roll(size.minSpots(), ScriptedRandom.alwaysMin()).total(), size.id() + " low end");
        assertEquals(high, rule.roll(size.maxSpots(), ScriptedRandom.alwaysMax()).total(), size.id() + " high end");
    }

    @Test
    void eachSizesLowAndHighEndsAreTheExecutedOnes() {
        assertEnds(MiningFixtures.small(), 9, 17);
        assertEnds(MiningFixtures.medium(), 14, 26);
        assertEnds(MiningFixtures.large(), 20, 36);
        assertEnds(MiningFixtures.extraLarge(), 26, 46);
        // Mutations: a bonus off by one in the fixture's ore, or the spot range off by one, moves one end.
    }

    @Test
    void aSeededSweepOfEverySizeStaysInItsRangeAndReachesBothEnds() {
        int[][] ends = {{9, 17}, {14, 26}, {20, 36}, {26, 46}};
        List<VeinSize> sizes = List.of(MiningFixtures.small(), MiningFixtures.medium(),
                MiningFixtures.large(), MiningFixtures.extraLarge());
        for (int i = 0; i < sizes.size(); i++) {
            VeinSize size = sizes.get(i);
            PayoutRule rule = MiningFixtures.iron().payoutRule(size.id());
            SplittableRandom rng = new SplittableRandom(20261114L + i);
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            // 400,000 breaks: the rarest end, extra_large's 46 (9 spots, every roll 4), is 1 in 39,366.
            for (int k = 0; k < 400_000; k++) {
                int spots = rng.nextInt(size.minSpots(), size.maxSpots() + 1);
                int total = rule.roll(spots, rng).total();
                min = Math.min(min, total);
                max = Math.max(max, total);
            }
            assertEquals(ends[i][0], min, size.id());
            assertEquals(ends[i][1], max, size.id());
        }
    }

    @Test
    void aPayoutRuleRefusesNegativeOrInvertedCounts() {
        assertThrows(IllegalArgumentException.class, () -> new PayoutRule(-1, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> new PayoutRule(4, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> new PayoutRule(2, 4, -1));
        // 0 is legal: "0 <= min <= max" (the seat's ruling 1).
        assertEquals(0, new PayoutRule(0, 0, 0).roll(2, ScriptedRandom.alwaysMax()).total());
    }
}
