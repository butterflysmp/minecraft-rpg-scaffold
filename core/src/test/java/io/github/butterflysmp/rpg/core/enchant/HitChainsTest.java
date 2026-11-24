package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WILDFIRE'S CHAIN, PURE (the seat's brief of 2026-11-24, PR E). Ben: "Consecutive hits increase damage by +5/10/15% each
 * hit (+60% cap)" / "Same Target, 3s rolling just like Kinetic Tremors, no" / "B" (a chain kept alive while each hit lands
 * within 3 seconds of the one before, climbing to the cap). The n-th hit of a chain deals x (1 + min(cap, step x (n - 1))).
 * Every expected factor here was EXECUTED in jshell (2026-11-24), never predicted: 1 + min(0.60, 0.15 x 1) is 1.15.
 */
class HitChainsTest {

    private static final EnchantBehaviour.HitChain WILDFIRE = new EnchantBehaviour.HitChain("living_lantern", 60, 60,
            List.of(5.0, 10.0, 15.0));
    private static final UUID HIM = new UUID(0, 1), OTHER = new UUID(0, 2), MOB = new UUID(0, 10), MOB2 = new UUID(0, 11);

    @Test
    void theChainClimbsByTheStepToTheCap() {
        HitChains c = new HitChains();
        double[] want = {1.0, 1.15, 1.3, 1.45, 1.6, 1.6, 1.6};
        for (int n = 0; n < want.length; n++) {
            assertEquals(want[n], c.hit(HIM, "wildfire", MOB, 100 + 16L * n, WILDFIRE, 3), 1e-12, "hit " + (n + 1));
        }
    }

    @Test
    void eachLevelsStep() {
        HitChains c = new HitChains();
        c.hit(HIM, "wildfire", MOB, 0, WILDFIRE, 1);
        assertEquals(1.05, c.hit(HIM, "wildfire", MOB, 10, WILDFIRE, 1), 1e-12, "level I: 5%");
        HitChains d = new HitChains();
        d.hit(HIM, "wildfire", MOB, 0, WILDFIRE, 2);
        assertEquals(1.1, d.hit(HIM, "wildfire", MOB, 10, WILDFIRE, 2), 1e-12, "level II: 10%");
    }

    /** A hit 59 ticks after the last extends; one exactly 60 after starts again (SEAT PLACEHOLDER: the boundary). */
    @Test
    void theWindowIsMeasuredFromThePreviousHitAndItsBoundary() {
        HitChains c = new HitChains();
        c.hit(HIM, "wildfire", MOB, 0, WILDFIRE, 3);
        assertEquals(1.15, c.hit(HIM, "wildfire", MOB, 59, WILDFIRE, 3), 1e-12, "59 after: extends");
        assertEquals(1.3, c.hit(HIM, "wildfire", MOB, 118, WILDFIRE, 3), 1e-12, "59 after the previous, 118 after the first: B");
        assertEquals(1.0, c.hit(HIM, "wildfire", MOB, 178, WILDFIRE, 3), 1e-12, "exactly 60 after: starts again");
    }

    @Test
    void theChainIsPerTargetAndPerPlayer() {
        HitChains c = new HitChains();
        c.hit(HIM, "wildfire", MOB, 0, WILDFIRE, 3);
        c.hit(HIM, "wildfire", MOB, 10, WILDFIRE, 3);
        assertEquals(1.0, c.hit(HIM, "wildfire", MOB2, 20, WILDFIRE, 3), 1e-12, "a second mob starts its own chain");
        assertEquals(1.3, c.hit(HIM, "wildfire", MOB, 30, WILDFIRE, 3), 1e-12, "and the first mob's goes on: its third hit");
        assertEquals(1.0, c.hit(OTHER, "wildfire", MOB, 40, WILDFIRE, 3), 1e-12, "another player's hit is his own chain");
    }

    @Test
    void forgottenOnDeathOnQuitAndWhenIdle() {
        HitChains c = new HitChains();
        c.hit(HIM, "wildfire", MOB, 0, WILDFIRE, 3);
        c.hit(HIM, "wildfire", MOB2, 0, WILDFIRE, 3);
        c.forgetTarget(MOB);
        assertEquals(1.0, c.hit(HIM, "wildfire", MOB, 10, WILDFIRE, 3), 1e-12, "death: the chain is gone");
        c.forgetPlayer(HIM);
        assertEquals(0, c.held(HIM), "quit: everything of his is gone");
        c.hit(HIM, "wildfire", MOB, 100, WILDFIRE, 3);
        c.hit(HIM, "wildfire", MOB2, 100, WILDFIRE, 3);
        assertEquals(1.0, c.hit(HIM, "wildfire", MOB, 160, WILDFIRE, 3), 1e-12, "60 after: MOB's chain starts again");
        assertEquals(1, c.held(HIM), "idle: MOB2's chain, 60 ticks old, is pruned on his next hit; MOB's new one held");
    }

    @Test
    void aMalformedChainIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.HitChain("x", 0, 60, List.of(5.0)));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.HitChain("x", 60, 0, List.of(5.0)));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.HitChain("x", 60, 60, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.HitChain("x", 60, 60, List.of(-5.0)));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.HitChain(" ", 60, 60, List.of(5.0)));
    }
}
