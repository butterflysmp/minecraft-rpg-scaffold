package io.github.butterflysmp.rpg.paper.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A {@link ScorchSink} that RECORDS every tick rather than dealing one, so a test can assert the
 * amount, the credit and -- critically -- the TIMING of each.
 *
 * Records the fake clock's tick at each burn, which is what makes the lifetime assertions possible.
 * This is the FakeWorld-discarded-delayTicks lesson applied forward: a sink that captured only
 * "how many" and "how much" would let an 8-second status that lives 9 seconds pass green, because the
 * tick COUNT is identical in both.
 */
final class FakeScorchSink implements ScorchSink {

    /** The victim's custom max. Mutable so a test can move it mid-burn. */
    double maxHealth;

    private final FakeTickTarget clock;

    /** One recorded burn: what it dealt, who it credited, WHEN, and the element it was MARKED with. */
    record Burn(double amount, UUID applierId, long atTick, String element) {}

    final List<Burn> burns = new ArrayList<>();

    FakeScorchSink(FakeTickTarget clock, double maxHealth) {
        this.clock = clock;
        this.maxHealth = maxHealth;
    }

    @Override public double victimMaxHealth() { return maxHealth; }

    @Override public void deal(double amount, UUID applierId, String element) {
        burns.add(new Burn(amount, applierId, clock.now(), element));
    }

    int count() { return burns.size(); }

    double totalDealt() { return burns.stream().mapToDouble(Burn::amount).sum(); }

    /** The tick the last burn landed on -- the lifetime assertions read this. */
    long lastBurnTick() { return burns.get(burns.size() - 1).atTick(); }
}
