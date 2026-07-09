package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.ability.*;
import io.github.yourname.rpg.core.ability.effect.EffectSpec;
import io.github.yourname.rpg.core.combat.Aim;
import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.core.combat.ResourcePool;
import io.github.yourname.rpg.core.element.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A projectile flies. It does not teleport. */
class ProjectileFlightTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** speed 1 block/tick, no gravity unless asked, generous fuse. */
    private static AbilityDefinition grenade(double speed, double gravity, int lifetime,
                                             EffectSpec... onHit) {
        return new AbilityDefinition("grenade", "Grenade", Element.SOLAR, "hunter",
                0, ResourceCost.FREE, new CastSpec.Projectile(speed, gravity, lifetime),
                List.of(onHit));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def, Aim aim) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster, "grenade", aim));
        new CastExecutor(world).execute(success);
    }

    private static final EffectSpec.Damage HIT = new EffectSpec.Damage(12, Element.SOLAR);

    @Test
    void aProjectileTakesTimeToReachItsTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);

        cast(world, caster, grenade(1.0, 0, 100, HIT), FORWARD);

        // The first segment covers x in [0,1]; the target is at x=5, untouched.
        assertEquals(100, target.health, 1e-9, "must not hit on the launch frame");
        assertTrue(world.pendingTasks() > 0, "it should be in flight");

        world.advanceTicks(3); // ticks carry it to x=4, still short
        assertEquals(100, target.health, 1e-9);

        world.advanceTicks(1); // this segment covers [4,5] and strikes
        assertEquals(88, target.health, 1e-9);
    }

    @Test
    void aProjectileStopsOnceItHits() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(3, 0, 0));
        world.entities.add(target);

        cast(world, caster, grenade(1.0, 0, 100, HIT), FORWARD);
        world.advanceTicks(50);

        assertEquals(88, target.health, 1e-9, "hit exactly once");
        assertEquals(0, world.pendingTasks(), "no further flight ticks are queued");
    }

    /** A fast projectile must not tunnel through a target thinner than its step. */
    @Test
    void aFastProjectileDoesNotTunnelThroughItsTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(10, 0, 0));
        world.entities.add(target);

        // 40 blocks per tick: the target sits mid-segment, never at an endpoint.
        cast(world, caster, grenade(40.0, 0, 100, HIT), FORWARD);
        world.advanceTicks(10);

        assertEquals(88, target.health, 1e-9);
    }

    @Test
    void gravityPullsTheProjectileDown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        // Directly ahead at head height: a straight shot would hit it.
        var straightAhead = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        // Below the flight line: only a falling projectile reaches this.
        var lowerDown = new FakeWorld.Dummy(new Vec3(6, -1.5, 0));
        world.entities.add(straightAhead);
        world.entities.add(lowerDown);

        cast(world, caster, grenade(1.0, 0.1, 100, HIT), FORWARD);
        world.advanceTicks(50);

        assertEquals(100, straightAhead.health, 1e-9, "gravity should carry it under this one");
        assertTrue(lowerDown.health < 100, "and into this one");
    }

    @Test
    void aProjectileIsStoppedByTerrain() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var behindTheWall = new FakeWorld.Dummy(new Vec3(8, 0, 0));
        world.entities.add(behindTheWall);
        world.wallX = 4; // it flies four blocks, then splats

        cast(world, caster, grenade(1.0, 0, 100, HIT), FORWARD);
        world.advanceTicks(50);

        assertEquals(100, behindTheWall.health, 1e-9);
        assertEquals(0, world.pendingTasks());
    }

    @Test
    void aProjectileIgnoresTheCasterItLaunchedFrom() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        cast(world, caster, grenade(1.0, 0, 5, HIT), FORWARD);
        world.advanceTicks(50);

        assertEquals(100, caster.health, 1e-9, "must not detonate in the thrower's face");
    }

    /** The fuse runs out. It still goes off, where it happened to be. */
    @Test
    void aProjectileThatHitsNothingDetonatesWhenItsFuseExpires() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        // Sits just past where a 5-tick, 1-block/tick projectile expires (x=5).
        var nearTheEnd = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        world.entities.add(nearTheEnd);

        cast(world, caster, grenade(1.0, 0, 5,
                new EffectSpec.Visual("boom"),
                new EffectSpec.Area(3.0, 20, 20, List.of(new EffectSpec.Damage(2, Element.SOLAR)))),
                FORWARD);
        world.advanceTicks(100);

        assertEquals(List.of("boom"), world.presented, "the fuse detonation must fire visuals");
        assertTrue(nearTheEnd.health < 100, "and leave an area where it expired");
        assertEquals(0, world.pendingTasks());
    }

    @Test
    void flightIsBoundedByMaxLifetimeTicks() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);

        cast(world, caster, grenade(1.0, 0, 3, HIT), FORWARD);

        // Launch frame plus 2 scheduled ticks, then the fuse expires. Never endless.
        world.advanceTicks(1000);
        assertEquals(0, world.pendingTasks());
    }

    /**
     * A grenade outlives its thrower. It must carry the caster's id, not the
     * Combatant -- otherwise a 100-tick fuse pins a Bukkit entity for five seconds.
     */
    @Test
    void aProjectileKeepsFlyingAfterItsCasterDespawns() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);

        cast(world, caster, grenade(1.0, 0, 100, HIT), FORWARD);
        world.advanceTicks(2); // it is airborne, around x=3

        world.entities.remove(caster); // the thrower logs out

        assertDoesNotThrow(() -> world.advanceTicks(50));
        assertEquals(88, target.health, 1e-9, "the grenade still lands");
    }
}
