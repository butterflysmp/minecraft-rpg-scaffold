package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Wither;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shared DoT store at WITHER's numbers, and the one property that keeps a withered mob from
 * exploding: wither and scorch are two INSTANCES (WITHER-STATUS; the seat's WS1 and WS2 rulings).
 *
 * <p>The clock, refresh and credit rows Scorch already has ({@code ScorchStatusTest}) run on the same
 * class; the rows here are the ones whose answer differs at a 40-tick period, plus the wiring.
 */
class DotStatusTest {

    private static final double EPS = 1e-9;

    /**
     * WS2's STORE HALF (PLAN-wither.md 4.2): A WITHER APPLICATION NEVER MAKES ANYTHING SCORCHED -- read
     * through the REAL {@code AdapterContext} wiring, because the failure is in the wiring, not in the
     * store. {@code onEntityDeath} ignites on {@code scorch().isScorched}, so one instance in both slots
     * would make every withered death explode.
     *
     * <p>Mutation: in {@code AdapterContext}'s convenience constructor, hand the wither slot the scorch
     * instance ({@code ScorchStatus} is a {@code DotStatus}, so it compiles) -> this reddens.
     */
    @Test
    void aWitherApplicationNeverScorches() {
        var ctx = new AdapterContext(null, null, null, null, null, null, null, null, 0.0, null, null,
                null, null, () -> false);
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID id = UUID.randomUUID();

        ctx.wither().apply(id, clock, sink, 1, 2.0, UUID.randomUUID(),
                Wither.DEFAULT_DURATION_TICKS, "wither");

        assertTrue(ctx.wither().isActive(id), "the wither store holds it");
        assertFalse(ctx.scorch().isScorched(id), "and Ignite's store never sees it (WS2)");
        assertNotSame(ctx.scorch(), ctx.wither(), "two instances, by construction");

        // AND THE CONTEXT BUILT IT AT WITHER'S RATES. The clock rows below build their own store, so
        // without this a context wired with Scorch.RATES would pass every one of them.
        clock.advance(400);
        assertEquals(List.of(40L, 80L, 120L, 160L, 200L),
                sink.burns.stream().map(FakeScorchSink.Burn::atTick).toList(),
                "the context's wither ticks every 40, five times");
        // Mutation: build the context's wither instance from Scorch.RATES -> six burns at 20 -> reddens.
    }

    /**
     * WS1, AS RULED: A TICK EVERY 40, FIVE OF THEM, THE FIRST ONE PERIOD IN, AT {@code min(5%, cap)} --
     * on the WS2b control mob (max 20, the percent arm: 1.0) and the WS2b binding mob (max 100, cap 2.0
     * under /rpg apply: 2.0, not 5.0).
     *
     * <p>Mutations: build the wither instance from {@code Scorch.RATES} -> six burns at 20-tick spacing
     * -> reddens. Drop the {@code min} -> the max-100 row reads 5.0 -> reddens.
     */
    @Test
    void witherBurnsEveryFortyTicksFiveTimesAtMinOfFivePercentAndTheCap() {
        var wither = new DotStatus(Wither.RATES, "wither");
        var clock = new FakeTickTarget();
        var small = new FakeScorchSink(clock, 20.0);
        var big = new FakeScorchSink(clock, 100.0);

        wither.apply(UUID.randomUUID(), clock, small, 1, 2.0, UUID.randomUUID(), 200, "wither");
        wither.apply(UUID.randomUUID(), clock, big, 1, 2.0, UUID.randomUUID(), 200, "wither");
        clock.advance(400);

        assertEquals(List.of(40L, 80L, 120L, 160L, 200L),
                small.burns.stream().map(FakeScorchSink.Burn::atTick).toList(),
                "Q-W1/Q-W3: every 40 ticks, five times, the first one period in");
        assertEquals(5, Wither.RATES.damageTicksFor(200), "and core's arithmetic agrees");
        small.burns.forEach(b -> assertEquals(1.0, b.amount(), EPS, "max 20: the percent arm, 1.0"));
        big.burns.forEach(b -> assertEquals(2.0, b.amount(), EPS, "max 100: the cap binds, 2.0"));
        assertEquals(5, big.count());
    }

    /**
     * Q-W4: A RE-HIT RESETS THE WINDOW AND DOES NOT RESTART THE CLOCK -- at period 40 against a re-hit
     * every 10 ticks (the shape PLAN-wither.md 9.1 names: the bow refreshes 2-3 times a period). A
     * restarted clock would re-phase forever and never tick.
     *
     * <p>Mutation: restart the task in the refresh arm -> no burn lands while re-hitting -> reddens.
     */
    @Test
    void reHittingFasterThanTheFortyTickPeriodStillTicksAndResetsTheWindow() {
        var wither = new DotStatus(Wither.RATES, "wither");
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 20.0);
        UUID id = UUID.randomUUID();

        for (int t = 0; t <= 100; t += 10) {
            wither.apply(id, clock, sink, 1, 2.0, UUID.randomUUID(), 200, "wither");
            clock.advance(10);
        }
        assertEquals(List.of(40L, 80L), sink.burns.stream().map(FakeScorchSink.Burn::atTick).toList(),
                "ticks keep landing on the clock while re-hit every 10");
        clock.advance(400);
        // MEASURED, AND THE FIRST PREDICTION WAS WRONG: it said t=300 (100 + 200). The refresh at t=100
        // rewrites `remaining` to 200 and the clock KEEPS ITS PHASE, so the window is counted in the
        // clock's own edges: 200 / 40 = five more burns, at 120..280. Scorch has the same shape.
        assertEquals(List.of(40L, 80L, 120L, 160L, 200L, 240L, 280L),
                sink.burns.stream().map(FakeScorchSink.Burn::atTick).toList(),
                "the last re-hit at t=100 bought five more ticks on the unchanged 40-tick phase");
    }

    /**
     * THE NEWEST APPLIER OWNS THE CREDIT AND THE CAP (Q-W4's "as Scorch"; WS4's death message names the
     * applier).
     */
    @Test
    void theNewestApplierOwnsTheCreditAndTheCap() {
        var wither = new DotStatus(Wither.RATES, "wither");
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID id = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        wither.apply(id, clock, sink, 1, 8.0, first, 200, "wither");
        clock.advance(10);
        wither.apply(id, clock, sink, 1, 2.0, second, 200, "wither");
        clock.advance(30);

        assertEquals(second, wither.applier(id));
        assertEquals(second, sink.burns.get(0).applierId(), "the tick credits the newest applier");
        assertEquals(2.0, sink.burns.get(0).amount(), EPS, "at the newest cap");
    }
}
