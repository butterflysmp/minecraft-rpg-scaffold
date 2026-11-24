package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Wither;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MANA BURN'S TICK (F2, the seat's ruling of 2026-11-24 on PR F): "the percent multiplies WHAT A SCORCH TICK DEALS, after
 * the tick is settled as min(5% of max health, cap), so the increase shows on every mob (5.0 becomes 5.5 / 6.0 / 6.5 on a
 * 100-health mob) ... The cap itself is not changed." Expected values EXECUTED in jshell, 2026-11-24: 5.0 x (1 + p/100.0)
 * for p = 10, 20, 30 is 5.5, 6.0, 6.5; 10.0 x 1.3 = 13.0.
 */
class ManaBurnTickTest {

    private static final double EPS = 1e-9;

    private static FakeScorchSink burnOnce(double maxHealth, double cap, double stamp) {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, maxHealth);
        scorch.apply(UUID.randomUUID(), clock, sink, 1, cap, UUID.randomUUID(), 120, "fire", 0, stamp);
        clock.advance(20);
        assertEquals(1, sink.count(), "one tick");
        return sink;
    }

    /** A 100-health mob, the Flint Staff's cap of 10: 5% is 5, the smaller, so the stamp shows on every tick. */
    @Test
    void theTickOnAHundredHealthMobAtEachLevel() {
        assertEquals(5.0, burnOnce(100, 10, 0).burns.get(0).amount(), EPS, "no stamp: min(5, 10) = 5");
        assertEquals(5.5, burnOnce(100, 10, 10).burns.get(0).amount(), EPS);
        assertEquals(6.0, burnOnce(100, 10, 20).burns.get(0).amount(), EPS);
        assertEquals(6.5, burnOnce(100, 10, 30).burns.get(0).amount(), EPS);
    }

    /**
     * THE CAP IS NOT CHANGED: on a 1000-health mob 5% is 50, the cap of 10 binds, and the stamp multiplies the SETTLED tick,
     * 10 x 1.3 = 13. A stamp that raised the cap instead would read 10 here (min(50, 13) = 13 too, so a second row: a
     * raised cap could not show on the 100-health mob above, where 6.5 is read).
     */
    @Test
    void theStampMultipliesTheSettledTickNotTheCap() {
        assertEquals(10.0, burnOnce(1000, 10, 0).burns.get(0).amount(), EPS, "the cap binds");
        assertEquals(13.0, burnOnce(1000, 10, 30).burns.get(0).amount(), EPS, "10 x 1.3");
    }

    /** Every tick of the burn, not only the first; and when the stamp is cleared, the next tick is plain. */
    @Test
    void everyTickIsMultipliedAndAClearedStampStopsIt() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        UUID mob = UUID.randomUUID();
        scorch.apply(mob, clock, sink, 1, 10.0, UUID.randomUUID(), 120, "fire", 0, 30.0);
        clock.advance(40);
        scorch.apply(mob, clock, sink, 1, 10.0, UUID.randomUUID(), 120, "fire", 0, 0.0);   // an unenchanted hit
        clock.advance(20);
        assertEquals(3, sink.count());
        assertEquals(6.5, sink.burns.get(0).amount(), EPS);
        assertEquals(6.5, sink.burns.get(1).amount(), EPS);
        assertEquals(5.0, sink.burns.get(2).amount(), EPS, "the stamp was cleared, so the third tick is plain");
    }

    /**
     * THE DEATH FRAME READS THE STAMP BEFORE THE FORGET, and hands it to Ignite. Read from the source: the listener needs
     * a live death event. A read after {@code forget} returns 0, so the ORDER is what this row pins.
     */
    @Test
    void theDeathListenerReadsTheStampBeforeTheForgetAndHandsItToIgnite() throws java.io.IOException {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/listener/RpgListeners.java"));
        int method = src.indexOf("public void onEntityDeath(EntityDeathEvent event)");
        assertTrue(method > 0, "the death listener was not found");
        int read = src.indexOf("double stamp = adapters.scorch().stamp(id);", method);
        int forget = src.indexOf("adapters.scorch().forget(id);", method);
        int detonate = src.indexOf("applier, id, depth + 1, stamp);", method);
        assertTrue(read > method && forget > read, "the stamp is read before the forget");
        assertTrue(detonate > forget, "and handed to Ignite.detonate");
    }

    /** Withering shares DotStatus and has no stamp: its tick is unchanged (the factor is scorch's alone). */
    @Test
    void witheringIsUntouched() {
        var wither = new DotStatus(Wither.RATES, "wither");
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 100.0);
        assertTrue(wither.apply(UUID.randomUUID(), clock, sink, 1, 10.0, UUID.randomUUID(), 120, "wither"));
        clock.advance(Wither.RATES.periodTicks());
        assertEquals(1, sink.count());
        assertEquals(Wither.RATES.damagePerTick(100.0, 10.0), sink.burns.get(0).amount(), EPS);
    }
}
