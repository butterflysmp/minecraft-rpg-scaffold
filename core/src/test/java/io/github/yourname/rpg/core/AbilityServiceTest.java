package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.ability.*;
import io.github.yourname.rpg.core.ability.effect.EffectSpec;
import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.core.element.Element;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AbilityServiceTest {

    private static AbilityDefinition solarGrenade() {
        return new AbilityDefinition(
                "solar_grenade", "Solar Grenade", Element.SOLAR, "hunter",
                200, new ResourceCost("energy", 40),
                new CastSpec.Projectile(1.2, 0.03, 100),
                List.of(
                        new EffectSpec.Damage(12, Element.SOLAR),
                        new EffectSpec.Area(4.0, 100, 20,
                                List.of(new EffectSpec.Damage(2, Element.SOLAR)))
                ));
    }

    @Test
    void directHitAppliesDamage() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(target);

        var registry = new AbilityRegistry();
        registry.register(solarGrenade());
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), world);

        var result = service.cast(caster, "solar_grenade", target, target.position());

        assertInstanceOf(AbilityService.CastResult.Success.class, result);
        assertEquals(88, target.health, 0.001); // 100 - 12 direct
    }

    @Test
    void solarDamageIsAmplifiedAgainstSolarShields() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        target.shield = Element.SOLAR;

        var registry = new AbilityRegistry();
        registry.register(solarGrenade());
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), world);
        service.cast(caster, "solar_grenade", target, target.position());

        assertEquals(82, target.health, 0.001); // 100 - (12 * 1.5)
    }

    @Test
    void lingeringAreaTicksOverTime() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var bystander = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        world.entities.add(bystander);

        var registry = new AbilityRegistry();
        registry.register(solarGrenade());
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), world);
        service.cast(caster, "solar_grenade", bystander, Vec3.ZERO);

        world.runScheduled(20);
        // 12 direct + 5 area ticks x 2 damage
        assertEquals(78, bystander.health, 0.001);
    }

    /**
     * A target standing on the impact point must take the direct hit only. The
     * lingering area starts pulsing one tick_interval later, not on the landing
     * frame -- otherwise a point-blank grenade silently deals bonus damage.
     */
    @Test
    void impactTargetDoesNotTakeAreaPulseOnLandingFrame() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(target);

        var registry = new AbilityRegistry();
        registry.register(solarGrenade());
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), world);

        service.cast(caster, "solar_grenade", target, target.position());
        assertEquals(88, target.health, 0.001); // 12 direct, no area pulse yet

        world.runScheduled(20);
        // then exactly 5 pulses x 2 damage, at ticks 20/40/60/80/100
        assertEquals(78, target.health, 0.001);
    }

    /**
     * A grenade does not stop burning because the thrower died or logged out.
     * The area must keep pulsing, and must not hold the caster alive to do it.
     */
    @Test
    void areaKeepsBurningAfterCasterDespawns() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);

        var registry = new AbilityRegistry();
        registry.register(solarGrenade());
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), world);
        service.cast(caster, "solar_grenade", null, Vec3.ZERO); // lands near nobody

        world.runScheduled(1); // first pulse: caster is present and excluded
        assertEquals(100, caster.health, 0.001);
        assertEquals(98, victim.health, 0.001);

        world.entities.remove(caster); // thrower logs out mid-duration

        assertDoesNotThrow(() -> world.runScheduled(20));
        assertEquals(90, victim.health, 0.001); // 4 further pulses x 2
        assertEquals(100, caster.health, 0.001); // never resurrected, never hit
    }

    @Test
    void abilityRespectsCooldown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));

        var tick = new AtomicLong(0);
        var registry = new AbilityRegistry();
        registry.register(solarGrenade());
        var service = new AbilityService(registry, new CooldownTracker(tick::get), world);

        service.cast(caster, "solar_grenade", target, Vec3.ZERO);
        var second = service.cast(caster, "solar_grenade", target, Vec3.ZERO);
        assertInstanceOf(AbilityService.CastResult.OnCooldown.class, second);

        tick.set(200);
        var third = service.cast(caster, "solar_grenade", target, Vec3.ZERO);
        assertInstanceOf(AbilityService.CastResult.Success.class, third);
    }

    @Test
    void unknownAbilityIsReportedNotThrown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var service = new AbilityService(new AbilityRegistry(), new CooldownTracker(() -> 0L), world);
        assertInstanceOf(AbilityService.CastResult.UnknownAbility.class,
                service.cast(caster, "nope", null, Vec3.ZERO));
    }
}
