package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.*;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AbilityServiceTest {

    /** Straight down the positive x axis from the origin. */
    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** The caster is permitted the grenade. The gate is exercised on its own below. */
    private static final Set<String> GRANTED = Set.of("solar_grenade");

    private static AbilityDefinition solarGrenade() {
        return new AbilityDefinition(
                "solar_grenade", "Solar Grenade", "fire", "hunter",
                200, new ResourceCost("energy", 40),
                new CastSpec.Projectile(1.2, 0.03, 100),
                List.of(
                        new EffectSpec.Damage(12, "fire"),
                        new EffectSpec.Area(4.0, 100, 20,
                                List.of(new EffectSpec.Damage(2, "fire")))
                ));
    }

    /** 100 energy, 1 per tick, so the grenade's cost of 40 is affordable twice. */
    private static ResourcePool pool(java.util.function.LongSupplier tick) {
        return new ResourcePool(tick, 100, 1.0);
    }

    private static AbilityService serviceWith(AbilityDefinition def, java.util.function.LongSupplier tick) {
        return serviceWith(def, tick, pool(tick));
    }

    private static AbilityService serviceWith(AbilityDefinition def,
                                              java.util.function.LongSupplier tick,
                                              ResourcePool resources) {
        var registry = new AbilityRegistry();
        registry.register(def);
        return new AbilityService(registry, new CooldownTracker(tick), resources);
    }

    /**
     * cast() only describes the cast. Resolving and running it is the caller's
     * job, and on Paper it happens inside Scheduler.onRegion(...). This helper is
     * that dispatch, minus the thread hop.
     */
    private static void dispatch(FakeWorld world, AbilityService.CastResult result) {
        var success = assertInstanceOf(AbilityService.CastResult.Success.class, result);
        new CastExecutor(world).execute(success);
    }

    @Test
    void directHitAppliesDamage() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(target);

        var service = serviceWith(solarGrenade(), () -> 0L);

        dispatch(world, service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));

        assertEquals(88, target.health, 0.001); // 100 - 12 direct
    }

    @Test
    void lingeringAreaTicksOverTime() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var bystander = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        world.entities.add(bystander);

        var service = serviceWith(solarGrenade(), () -> 0L);
        dispatch(world, service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));

        // The opening flight segment covers x in [0, 1.2], short of the bystander at
        // x=2, so the grenade strikes on tick 1 and its pulses land at 21/41/61/81/101.
        world.advanceTicks(101);
        // 12 direct + 5 area ticks x 2 damage
        assertEquals(78, bystander.health, 0.001);
    }

    /**
     * The reported bug: "mobs hit by it ignite shortly after the initial explosion."
     *
     * scorch lived only inside the area, whose first pulse is one tick_interval (a full
     * second) after impact. A burst puts the ignition on the detonation frame, where a
     * player expects to see it.
     */
    @Test
    void solarGrenadeIgnitesOnDetonationNotOneIntervalLater() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var bystander = new FakeWorld.Dummy(new Vec3(3, 0, 0));
        world.entities.add(target);
        world.entities.add(bystander);

        var grenade = new AbilityDefinition(
                "solar_grenade", "Solar Grenade", "fire", "hunter",
                200, new ResourceCost("energy", 40),
                new CastSpec.Projectile(1.2, 0.03, 100),
                List.of(
                        new EffectSpec.Damage(8, "fire"),
                        new EffectSpec.Burst(4.0, List.of(
                                new EffectSpec.Damage(6, "fire"),
                                new EffectSpec.Status("scorch", 40, 0))),
                        new EffectSpec.Area(4.0, 100, 20,
                                List.of(new EffectSpec.Damage(2, "fire")))));

        dispatch(world, serviceWith(grenade, () -> 0L).cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));

        // No advanceTicks: everything below happened on the detonation frame itself.
        assertEquals(List.of("scorch"), target.statuses, "the struck mob must ignite at once");
        assertEquals(List.of("scorch"), bystander.statuses, "so must a bystander in radius");

        assertEquals(86, target.health, 0.001);      // 8 direct + 6 splash
        assertEquals(94, bystander.health, 0.001);   // 6 splash, no direct hit
    }

    /**
     * The lingering damage Area is a field: it starts pulsing one tick_interval after it
     * is placed, not on the frame it lands. A target on the impact point takes the direct
     * hit that frame and nothing from the Area until the first pulse.
     *
     * Immediate splash, when wanted, is a Burst -- an explicit effect -- not an accident
     * of Area timing.
     */
    @Test
    void lingeringAreaFirstPulseLandsOneIntervalAfterImpact() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        // x=1 lies inside the opening 1.2-block segment, so this detonates at tick 0.
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(target);

        var service = serviceWith(solarGrenade(), () -> 0L);

        dispatch(world, service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));
        assertEquals(88, target.health, 0.001); // launch-frame detonation: direct hit only

        world.advanceTicks(19);
        assertEquals(88, target.health, 0.001); // the first pulse is at 20, not yet

        world.advanceTicks(1);
        assertEquals(86, target.health, 0.001); // pulse @ 20

        world.advanceTicks(80);                 // pulses @ 40/60/80/100
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
        world.blockDistance = 0; // detonates at the caster's feet, hitting nobody directly

        var service = serviceWith(solarGrenade(), () -> 0L);
        dispatch(world, service.cast(caster.snapshot(), "solar_grenade", new Aim(Vec3.ZERO, new Vec3(0, -1, 0)), GRANTED));

        world.advanceTicks(20); // first pulse @ 20: caster is present and excluded
        assertEquals(100, caster.health, 0.001);
        assertEquals(98, victim.health, 0.001);

        world.entities.remove(caster); // thrower logs out mid-duration

        assertDoesNotThrow(() -> world.advanceTicks(80)); // pulses @ 40/60/80/100
        assertEquals(90, victim.health, 0.001); // 4 further pulses x 2
        assertEquals(100, caster.health, 0.001); // never resurrected, never hit
    }

    @Test
    void abilityRespectsCooldown() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);

        var tick = new AtomicLong(0);
        var service = serviceWith(solarGrenade(), tick::get);

        service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED);
        var second = service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED);
        assertInstanceOf(AbilityService.CastResult.OnCooldown.class, second);

        tick.set(200);
        var third = service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED);
        assertInstanceOf(AbilityService.CastResult.Success.class, third);
    }

    /**
     * The cooldown is spent by deciding to cast, not by the effects landing.
     * Otherwise a player could cast repeatedly during the hop onto the region
     * thread, before the first cast's effects had run.
     */
    @Test
    void cooldownIsConsumedAtCallTimeNotAtExecutionTime() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var service = serviceWith(solarGrenade(), () -> 0L);

        // Note: the returned cast is deliberately never executed.
        assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));
        assertInstanceOf(AbilityService.CastResult.OnCooldown.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));
    }

    @Test
    void castingAnAbilityNotInTheGrantedSetIsLocked() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var service = serviceWith(solarGrenade(), () -> 0L);

        var result = service.cast(caster.snapshot(), "solar_grenade", FORWARD, Set.of());

        assertInstanceOf(AbilityService.CastResult.Locked.class, result);
    }

    /**
     * Order, not just outcome. "Locked is returned" and "cooldown/energy work" are
     * tested separately above; neither proves Locked comes FIRST. A reorder that
     * landed the access check after the energy spend would keep every other test
     * green and the sealed switch still exhaustive -- and silently drain a locked
     * caster's energy and start their cooldown. This is the only test that fails
     * on that reorder: a locked cast must touch neither.
     */
    @Test
    void aLockedCastConsumesNoCooldownOrEnergy() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var tick = new AtomicLong(0);
        var resources = pool(tick::get);
        var service = serviceWith(solarGrenade(), tick::get, resources);

        assertInstanceOf(AbilityService.CastResult.Locked.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, Set.of()));

        // Energy untouched...
        assertEquals(100, resources.current(caster.id(), "energy"), 1e-9);
        // ...and the cooldown never started: the instant the grant appears, the very
        // same tick, the ability casts. A consumed cooldown would make this OnCooldown.
        assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));
    }

    @Test
    void unknownAbilityIsReportedNotThrown() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var service = new AbilityService(new AbilityRegistry(), new CooldownTracker(() -> 0L),
                pool(() -> 0L));
        assertInstanceOf(AbilityService.CastResult.UnknownAbility.class,
                service.cast(caster.snapshot(), "nope", FORWARD, GRANTED));
    }

    @Test
    void castingSpendsTheAbilitysEnergyCost() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var resources = pool(() -> 0L);
        var service = serviceWith(solarGrenade(), () -> 0L, resources);

        service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED);

        assertEquals(60, resources.current(caster.id(), "energy"), 1e-9); // 100 - 40
    }

    @Test
    void castingWithoutEnoughEnergyIsRefused() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var resources = pool(() -> 0L);
        resources.tryConsume(caster.id(), "energy", 70); // 30 left, grenade costs 40
        var service = serviceWith(solarGrenade(), () -> 0L, resources);

        var result = service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED);

        var refused = assertInstanceOf(AbilityService.CastResult.InsufficientResource.class, result);
        assertEquals("energy", refused.resourceId());
        assertEquals(40, refused.required(), 1e-9);
        assertEquals(30, refused.available(), 1e-9);
    }

    /**
     * A refused cast must not eat the cooldown. Otherwise running out of energy
     * would also lock the ability out for its full cooldown, having fired nothing.
     */
    @Test
    void aCastRefusedForEnergyDoesNotConsumeTheCooldown() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var tick = new AtomicLong(0);
        var resources = pool(tick::get);
        resources.tryConsume(caster.id(), "energy", 70); // 30 left, grenade costs 40
        var service = serviceWith(solarGrenade(), tick::get, resources);

        assertInstanceOf(AbilityService.CastResult.InsufficientResource.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));

        // Let energy regenerate. The grenade's 200-tick cooldown has NOT started,
        // so the same service must cast successfully well before it would elapse.
        tick.set(40);
        assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));
    }

    /** Being on cooldown must not spend energy either. */
    @Test
    void aCastRefusedForCooldownDoesNotSpendEnergy() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var resources = pool(() -> 0L);
        var service = serviceWith(solarGrenade(), () -> 0L, resources);

        service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED); // 100 -> 60
        assertInstanceOf(AbilityService.CastResult.OnCooldown.class,
                service.cast(caster.snapshot(), "solar_grenade", FORWARD, GRANTED));

        assertEquals(60, resources.current(caster.id(), "energy"), 1e-9);
    }
}
