package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * How a break pays: one roll per spot, each drawn uniformly and INDEPENDENTLY from
 * {@code perSpotMin..perSpotMax} inclusive, plus the vein size's completion bonus. The seat's
 * rulings 1 to 3, 2026-11-13, on Ben's "2-4 per spot with a 5 completion bonus".
 *
 * <p>NAME THE QUANTITY: every term is a whole number of items. Flat, with no percentages and no
 * multipliers. For Small iron that is 9 to 17 raw iron per break (PLAN-mining.md 5.2).
 *
 * @param perSpotMin fewest items one spot pays, at least 0
 * @param perSpotMax most items one spot pays, at least {@code perSpotMin}
 * @param bonus      items added once per break, at least 0
 */
public record PayoutRule(int perSpotMin, int perSpotMax, int bonus) {

    public PayoutRule {
        if (perSpotMin < 0) throw new IllegalArgumentException("per_spot.min must be >= 0, got " + perSpotMin);
        if (perSpotMax < perSpotMin) {
            throw new IllegalArgumentException("per_spot.min " + perSpotMin + " is above per_spot.max " + perSpotMax);
        }
        if (bonus < 0) throw new IllegalArgumentException("completion bonus must be >= 0, got " + bonus);
    }

    /** Rolls the payout for a patch of {@code spots} spots, at the break. */
    public Payout roll(int spots, RandomGenerator rng) {
        List<Integer> rolls = new ArrayList<>(spots);
        for (int i = 0; i < spots; i++) {
            rolls.add(rng.nextInt(perSpotMin, perSpotMax + 1));
        }
        return new Payout(rolls, bonus);
    }
}
