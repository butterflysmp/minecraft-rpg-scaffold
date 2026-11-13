package io.github.butterflysmp.rpg.core.mining;

import java.util.List;

/**
 * One break's payout: each spot's roll, in spot order, and the completion bonus. The trace prints
 * both, so {@link #total()} can be checked by addition on the line itself.
 */
public record Payout(List<Integer> rolls, int bonus) {

    public Payout {
        rolls = List.copyOf(rolls);
    }

    /** The sum of the rolls plus the bonus: the number of items handed over. */
    public int total() {
        int sum = bonus;
        for (int r : rolls) sum += r;
        return sum;
    }
}
