package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Douse;
import io.github.butterflysmp.rpg.core.combat.DouseLine;
import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.combat.Wither;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WATER PUTS OUT SCORCH, ON THE STORE (the seat's Y1 to Y3, Y6, Y7, A4 and E2, 2026-11-27). The body's wetness is
 * staged on the fake sink; what the server reads as wet is {@code EntityScorchSink.wetness}'s, and the death half is
 * {@code DouseTest}'s table and {@code DouseWiringTest}'s sites.
 *
 * <p>Staged at max health 400 and cap 50, so the burn is min(5% of 400 = 20, 50) = 20 and no two figures this class
 * reads are equal: the cap (50), the burn (20), the window (120) and the period (20 ticks).
 */
class ScorchDouseTest {

    private final UUID zombie = UUID.randomUUID();
    private final UUID him = UUID.randomUUID();
    private final List<DouseLine> lines = new ArrayList<>();

    private ScorchStatus scorch() {
        return new ScorchStatus("scorch", DotTrace.NONE, lines::add);
    }

    private boolean light(ScorchStatus s, FakeTickTarget clock, FakeScorchSink sink) {
        return s.apply(zombie, clock, sink, 3, 50.0, him, Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
    }

    @Test
    void aWetBodyGainsNoScorchAndTheRefusalIsTraced() {
        var s = scorch();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 400);
        sink.wet = Optional.of(Douse.Cause.WATER);

        assertFalse(light(s, clock, sink), "refused, so the caller sets no flame");
        assertFalse(s.isScorched(zombie));
        clock.advance(200);
        assertEquals(0, sink.count(), "no burn ever");
        assertEquals(0, clock.pending(), "no task was started");
        assertEquals(List.of(new DouseLine(DouseLine.Event.REFUSED, "zombie", zombie, Douse.Cause.WATER)), lines);
    }

    @Test
    void anApplicationThatBuysNoStacksIsNotARefusal() {
        var s = scorch();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 400);
        sink.wet = Optional.of(Douse.Cause.RAIN);
        assertFalse(s.apply(zombie, clock, sink, 0, 50.0, him, 120, "fire", 0));
        assertTrue(lines.isEmpty(), "nothing would have applied, so nothing is refused");
    }

    @Test
    void aBurnTickOnAWetBodyDealsNothingEndsTheBurnAndClearsTheFlame() {
        var s = scorch();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 400);
        assertTrue(light(s, clock, sink));

        clock.advance(20);
        assertEquals(1, sink.count(), "the dry first burn, at tick 20");
        assertEquals(20.0, sink.totalDealt(), 1e-9);

        sink.wet = Optional.of(Douse.Cause.RAIN);   // into the rain between burns 1 and 2
        clock.advance(20);
        assertEquals(1, sink.count(), "the doused tick (40) deals none");
        assertFalse(s.isScorched(zombie), "the burn has ended");
        assertEquals(1, sink.flameClears, "and its flame is cleared (A4)");
        assertEquals(List.of(new DouseLine(DouseLine.Event.DOUSED, "zombie", zombie, Douse.Cause.RAIN)), lines);

        sink.wet = Optional.empty();
        clock.advance(200);
        assertEquals(1, sink.count(), "none after, even dry again");
        assertEquals(0, clock.pending());
        assertEquals(0, s.trackedVictims(), "the entry went with the task");
    }

    @Test
    void aSplashDousesAScorchedBodyAndLeavesNoImmunity() {
        var s = scorch();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 400);
        assertTrue(light(s, clock, sink));
        clock.advance(30);
        assertEquals(1, sink.count());

        assertTrue(s.douse(zombie, sink, Douse.Cause.SPLASH));
        assertFalse(s.isScorched(zombie));
        assertEquals(1, sink.flameClears);
        clock.advance(200);
        assertEquals(1, sink.count(), "no burn after the splash");
        assertEquals(List.of(new DouseLine(DouseLine.Event.DOUSED, "zombie", zombie, Douse.Cause.SPLASH)), lines);

        assertTrue(light(s, clock, sink), "a moment, not a state (E2): the next fire hit scorches it again");
        assertTrue(s.isScorched(zombie));
    }

    @Test
    void aSplashOnABodyThatIsNotScorchedDoesNothingAndWritesNothing() {
        var s = scorch();
        var sink = new FakeScorchSink(new FakeTickTarget(), 400);
        assertFalse(s.douse(zombie, sink, Douse.Cause.SPLASH));
        assertEquals(0, sink.flameClears);
        assertTrue(lines.isEmpty());
    }

    @Test
    void witherIsNotPutOutByWaterBecauseTheRuleIsScorchsAlone() {
        // Y6: no other status. The shared store's hook answers false; only ScorchStatus overrides it.
        var wither = new DotStatus(Wither.RATES, "wither");
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 400);
        sink.wet = Optional.of(Douse.Cause.WATER);
        assertTrue(wither.apply(zombie, clock, sink, 3, 50.0, him, 120, "decay"));
        clock.advance(40);
        assertTrue(sink.count() >= 1, "a wet withered body still burns");
        assertEquals(0, sink.flameClears);
    }

    @Test
    void theIgniteRefusalIsWrittenByTheStoresTrace() {
        var s = scorch();
        s.igniteRefused(zombie, "zombie", Douse.Cause.WATER);
        assertEquals(List.of(new DouseLine(DouseLine.Event.IGNITE_REFUSED, "zombie", zombie, Douse.Cause.WATER)),
                lines);
    }
}
