package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MANA BURN'S STAMP ON THE SCORCH (F1, the seat's ruling of 2026-11-24 on PR F). The scorch keeps the stamp the hit that
 * applied it carried; when scorches meet, THE STAMP FOLLOWS THE CREDIT, the newest applier's (the seat accepted the
 * counter-proposal), while the cap stays the larger. So an unenchanted hit on a stamped scorch CLEARS the stamp.
 */
class ScorchStampStatusTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void theScorchKeepsTheStampOfTheHitThatAppliedIt() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        UUID mob = UUID.randomUUID();
        assertEquals(0.0, scorch.stamp(mob), 0.0, "not scorched: no stamp");
        scorch.apply(mob, clock, new FakeScorchSink(clock, 100.0), 1, 10.0, ALICE, 120, "fire", 0, 30.0);
        assertEquals(30.0, scorch.stamp(mob), 0.0);
        assertEquals(ALICE, scorch.applier(mob));
    }

    /** The seat's ruling: an unenchanted hit on a stamped scorch clears the stamp; the cap stays the larger. */
    @Test
    void anUnenchantedRefreshClearsTheStampAndTheCapStaysTheLarger() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 1000.0);   // 5% is 50, so the cap binds and can be read off the burn
        UUID mob = UUID.randomUUID();
        scorch.apply(mob, clock, sink, 1, 22.5, ALICE, 120, "fire", 0, 30.0);
        scorch.apply(mob, clock, sink, 1, 10.0, BOB, 120, "fire", 0, 0.0);
        assertEquals(0.0, scorch.stamp(mob), 0.0, "the stamp follows the credit, the newest applier's");
        assertEquals(BOB, scorch.applier(mob), "and the credit is Bob's");
        clock.advance(20);
        assertEquals(22.5, sink.burns.get(0).amount(), 1e-9, "the cap stayed the larger, 22.5, unstamped");
    }

    /** And the other way: a Mana Burn hit on a plain scorch stamps it. */
    @Test
    void anEnchantedRefreshStampsAPlainScorch() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID mob = UUID.randomUUID();
        scorch.apply(mob, clock, sink, 1, 10.0, BOB, 120, "fire", 0);
        assertEquals(0.0, scorch.stamp(mob), 0.0, "the nine-argument apply carries no stamp");
        scorch.apply(mob, clock, sink, 1, 10.0, ALICE, 120, "fire", 0, 20.0);
        assertEquals(20.0, scorch.stamp(mob), 0.0);
    }

    /** The nine-argument apply (the dev command's) is an applier with no stamp: it clears one too. */
    @Test
    void aNineArgumentRefreshClearsTheStamp() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID mob = UUID.randomUUID();
        scorch.apply(mob, clock, sink, 1, 10.0, ALICE, 120, "fire", 0, 30.0);
        scorch.apply(mob, clock, sink, 1, 10.0, BOB, 120, null, 0);
        assertEquals(0.0, scorch.stamp(mob), 0.0);
    }

    /** A hit that buys no stacks applies nothing, so it changes no stamp. */
    @Test
    void aHitThatBuysNoStacksLeavesTheStamp() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID mob = UUID.randomUUID();
        scorch.apply(mob, clock, sink, 1, 10.0, ALICE, 120, "fire", 0, 30.0);
        scorch.apply(mob, clock, sink, 0, 10.0, BOB, 120, "fire", 0, 0.0);
        assertEquals(30.0, scorch.stamp(mob), 0.0);
    }

    /**
     * THE PORT HANDS THE HIT'S STAMP TO THE SCORCH, read from the source because the site is the paper adapter's (it
     * needs a live entity). Anchored at both ends: the accrual-driven scorch apply ends with the depth and then the stamp.
     */
    @Test
    void thePortHandsTheHitsStampToTheScorchItApplies() throws java.io.IOException {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/adapter/BukkitCombatant.java"));
        String needle = "accrued.durationTicks(), element, accrual.depth() + 1,";
        int at = src.indexOf(needle);
        assertTrue(at > 0, "the accrual-driven scorch apply was not found");
        // The apply is the CONDITION of the flame's `if` since water puts out Scorch (A4, 2026-11-27: a refused
        // application sets no flame), so the call ends at the `{` that opens the flame, not at a `;`.
        String after = src.substring(at + needle.length(), src.indexOf("{", at));
        assertTrue(after.trim().endsWith("accrual.scorchStampPercent()))"), "the stamp is not handed on: " + after);
    }

    /** The burn ended or was forgotten: the stamp goes with it, so a later plain scorch carries none. */
    @Test
    void theStampEndsWithTheBurn() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID mob = UUID.randomUUID();
        scorch.apply(mob, clock, sink, 1, 10.0, ALICE, 120, "fire", 0, 30.0);
        assertTrue(scorch.forget(mob));
        assertEquals(0.0, scorch.stamp(mob), 0.0, "forgotten");
        scorch.apply(mob, clock, sink, 1, 10.0, ALICE, 40, "fire", 0, 30.0);
        clock.advance(40);
        assertEquals(0.0, scorch.stamp(mob), 0.0, "ran out");
        scorch.apply(mob, clock, sink, 1, 10.0, BOB, 120, "fire", 0);
        assertEquals(0.0, scorch.stamp(mob), 0.0, "a later plain scorch carries none");
    }
}
