package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.FakeWorld;
import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MANA BURN'S IGNITE (F2, the seat's ruling of 2026-11-24 on PR F): "it multiplies Ignite's damage (6 becomes 6.6 / 7.2 /
 * 7.8)". The stamp is the scorch's the corpse died with. Expected values EXECUTED in jshell, 2026-11-24:
 * 6.0 x (1 + 10/100.0) = 6.6000000000000005, x (1 + 20/100.0) = 7.199999999999999, x (1 + 30/100.0) = 7.800000000000001.
 */
class ManaBurnIgniteTest {

    private static final double EPS = 1e-9;

    private static double blast(double stamp) {
        var world = new FakeWorld();
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);
        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1, stamp);
        world.advanceTicks(Ignite.DELAY_TICKS);
        assertEquals(1, neighbour.damageCalls, "one blast");
        assertEquals(0.0, neighbour.lastDamageStamp, 0.0, "the blast's own hit carries no stamp");
        return 100 - neighbour.health;
    }

    @Test
    void theStampMultipliesTheBlastAtEachLevel() {
        assertEquals(6.0, blast(0), EPS, "no stamp: Ignite's 6");
        assertEquals(6.6000000000000005, blast(10), EPS);
        assertEquals(7.199999999999999, blast(20), EPS);
        assertEquals(7.800000000000001, blast(30), EPS);
    }

    /** The five-argument form (the unscorched-kill clause's, and every caller before F2) is unstamped. */
    @Test
    void theOldFormIsUnstamped() {
        var world = new FakeWorld();
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);
        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);
        assertEquals(6.0, 100 - neighbour.health, EPS);
    }

    /** A stamped blast still cannot hurt a player (the skip is untouched). */
    @Test
    void aStampedBlastStillSparesAPlayer() {
        var world = new FakeWorld();
        var player = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        player.player = true;
        world.entities.add(player);
        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1, 30);
        world.advanceTicks(Ignite.DELAY_TICKS);
        assertEquals(0, player.damageCalls);
        assertEquals(100, player.health, EPS);
    }
}
