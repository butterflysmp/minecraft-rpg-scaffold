package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * THE LIVING LANTERN'S HOMING SHOT, FLOWN (the seat's brief of 2026-11-23, PR B): Ben's numbers, through the real
 * {@link CastExecutor} and {@code ProjectileFlight} on the fake world, so the gate's rows stand on an executed flight and
 * not on a reasoned one. The numbers are copied from {@code living_lantern.yml}, whose own test
 * ({@code LivingLanternContentTest}) pins them there: speed 0.35, gravity 0, lifetime 80, homing 0.45 / 0 / 7, 45 fire.
 *
 * <p>The body's half-width is the fake world's {@code hitRadius}, 0.3 (a zombie's is 0.3, INFERRED from its 0.6 width).
 */
class LivingLanternFlightTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** One cast at a mob standing at {@code at}; returns the mob's health once the flight is long over. */
    static double flyAt(Vec3 at) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var mob = new FakeWorld.Dummy(at);
        world.entities.add(mob);
        var def = new AbilityDefinition("living_lantern", "Lantern's Breath", "fire", 0, ResourceCost.FREE,
                new CastSpec.Projectile(0.35, 0.0, 80, null, null, new CastSpec.Homing(0.45, 0, 7)),
                List.of(new EffectSpec.Damage(45, "fire")));
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(new FakeWorld.Dummy(Vec3.ZERO).snapshot(), def.id(), FORWARD, Set.of(def.id())));
        new CastExecutor(world).execute(success);
        world.advanceTicks(100);
        return mob.health;
    }

    /** The gate's row: a mob 10 blocks out and 3 off his aim line, inside 7 of the path. ONE hit of exactly 45. */
    @Test
    void aMobOffTheAimLineButInsideSevenOfThePathTakesOneHitOfFortyFive() {
        assertEquals(55.0, flyAt(new Vec3(10, 0, 3)), 1e-9, "100 - 45: one hit, none more (the shot ends on it)");
    }

    /** The control: 8 off the line, never inside 7 of the path, so the shot never turns and passes it. */
    @Test
    void aMobEightOffTheLineIsNeverSought() {
        assertEquals(100.0, flyAt(new Vec3(10, 0, 8)), 1e-9);
    }

    /** Ben's reach: dead ahead at 27 the shot arrives; at 29, past its 28.0 of travel, it does not. */
    @Test
    void theReachEndsAtTwentyEightBlocks() {
        assertEquals(55.0, flyAt(new Vec3(27, 0, 0)), 1e-9, "inside the reach");
        assertEquals(100.0, flyAt(new Vec3(29, 0, 0)), 1e-9, "beyond 28 blocks of travel: no hit");
    }
}
