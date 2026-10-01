package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Wither;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Forgetting a LIVE wither strips the vanilla potion; forgetting nothing strips nothing (the seat's
 * ruling 6). The strip itself ({@code removePotionEffect}) and the call sites in {@code RpgListeners} are
 * boot-only; this pins the decision they all go through.
 */
class WitherPotionTest {

    /**
     * Live -> stripped once; a second forget, or an ended wither, strips nothing.
     *
     * <p>Mutations: make {@code DotStatus.forget} return false always -> the live assertion reddens.
     * Strip unconditionally in {@code forgetAndStrip} -> the ended and twice assertions redden.
     */
    @Test
    void onlyALiveWitherHasItsPotionStripped() {
        var wither = new DotStatus(Wither.RATES, "wither");
        var clock = new FakeTickTarget();
        var strips = new AtomicInteger();
        UUID live = UUID.randomUUID();
        UUID ended = UUID.randomUUID();
        UUID never = UUID.randomUUID();

        wither.apply(live, clock, new FakeScorchSink(clock, 100.0), 1, 2.0, null, 200, null);
        wither.apply(ended, clock, new FakeScorchSink(clock, 100.0), 1, 2.0, null, 40, null);
        clock.advance(80);   // `ended`'s 40-tick window is over; `live`'s 200 is not

        assertTrue(WitherPotion.forgetAndStrip(wither, live, strips::incrementAndGet), "a live wither");
        assertEquals(1, strips.get(), "its potion is stripped");
        assertFalse(wither.isActive(live), "and the store forgot it");
        assertFalse(WitherPotion.forgetAndStrip(wither, live, strips::incrementAndGet), "a second forget");
        assertFalse(WitherPotion.forgetAndStrip(wither, ended, strips::incrementAndGet),
                "a wither that ran out: its potion ran out with it");
        assertFalse(WitherPotion.forgetAndStrip(wither, never, strips::incrementAndGet), "never withered");
        assertEquals(1, strips.get(), "only the live one was stripped");
    }
}
