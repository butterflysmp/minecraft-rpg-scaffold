package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * THE BURNS A RUN OF FIRE HITS GIVES, ON THE SERVER'S ORDER AS READ (X6 (a), 2026-11-28). Run through the REAL store
 * ({@link ScorchStatus}) on a fake clock. It replaces the mapping {@code SmolderGateArithmeticTest} used, whose first
 * burn at hit + 19 the boot of {@code 01be9d3b} contradicted (it read + 20). That mapping's other half, a burn before a
 * later hit's refresh on a shared tick, was RIGHT, and is kept here for the reason below.
 *
 * <p><b>THE ORDER, READ by javap</b> ({@code PLAN-owed-fixes.md}, X6 AS BUILT (a)): a ring's hit is a GLOBAL-phase task;
 * its damage hops onto the mob ({@code onEntity}: later in the same tick, the ENTITY phase) and there accrues Scorch and,
 * on a first application, books the burn task 20 ticks on, from the entity phase: exactly hit + 20. On a tick that has
 * both a later hit and a burn, the hit's LINE is written first (global phase) but its Scorch REFRESH runs AFTER the burn:
 * both are tasks of the mob's own entity scheduler due that tick, and {@code EntityScheduler}'s due list is an
 * {@code ArrayList} run in insertion order ({@code lambda$schedule$0} makes it; {@code executeTick} offsets 121 to 161
 * walk it from 0), so the burn, booked 20 ticks earlier, runs before the refresh booked this tick. So here: the first
 * hit is applied at fake 0 (its burn at exactly 20); each later hit at offset {@code h} is applied after every burn due
 * at or before {@code h}. A refresh uses no clock, so only that order matters.
 */
final class ScorchBurnsReadOrder {

    private ScorchBurnsReadOrder() {}

    /** Burn offsets from the first hit, each burn's amount, and the total. */
    record Burns(List<Long> offsets, double each, double total) {}

    static Burns burns(double maxHealth, double cap, int... hitOffsets) {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, maxHealth);
        UUID id = UUID.randomUUID();
        UUID applier = UUID.randomUUID();
        scorch.apply(id, clock, sink, 80, cap, applier, Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
        for (int i = 1; i < hitOffsets.length; i++) {
            clock.advance((int) (hitOffsets[i] - clock.now()));   // every burn due AT OR BEFORE this hit's tick
            scorch.apply(id, clock, sink, 80, cap, applier, Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
        }
        clock.advance(1000);
        List<Long> offsets = new ArrayList<>();
        for (var b : sink.burns) offsets.add(b.atTick());
        return new Burns(offsets, sink.burns.isEmpty() ? 0 : sink.burns.get(0).amount(), sink.totalDealt());
    }
}
