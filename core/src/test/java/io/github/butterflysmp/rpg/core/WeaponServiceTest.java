package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.element.Element;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Commit 1a seam proof: a weapon trigger fires end to end with NO Bukkit present.
 *
 * What this covers that AbilityServiceTest structurally cannot:
 *  - selection: an input picks its bound trigger, a bound-less input picks nothing;
 *  - gate bypass: a weapon trigger fires though its ability is NOT registered and no
 *    castable set exists -- weapons are not archetype-gated;
 *  - and it re-proves atomicity for the weapon path: check-and-commit is one core call,
 *    so a fast double-swing cannot double-spend.
 *
 * If any of this needed a server, the seam would be in the wrong place.
 */
class WeaponServiceTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** 100 energy, 1/tick, so a 40-cost trigger is affordable twice. */
    private static ResourcePool pool(LongSupplier tick) {
        return new ResourcePool(tick, 100, 1.0);
    }

    /**
     * A WeaponService over an EMPTY ability registry. That emptiness is the point: the
     * triggers below are registered nowhere and granted to no one, so the only way fire()
     * can succeed is by bypassing the registry lookup and the castable gate cast() applies.
     */
    private static WeaponService serviceWith(LongSupplier tick, ResourcePool resources) {
        var abilities = new AbilityService(new AbilityRegistry(), new CooldownTracker(tick), resources);
        return new WeaponService(abilities);
    }

    /** A trigger's ability, ids synthesized as WeaponLoader will: weaponId/input. */
    private static AbilityDefinition ability(String id, ResourceCost cost, int cooldownTicks) {
        return new AbilityDefinition(id, "Trigger", Element.KINETIC, "none",
                cooldownTicks, cost, new CastSpec.Melee(3.0, 120),
                List.of(new EffectSpec.Damage(8, Element.KINETIC)));
    }

    /** Free left-click only, a basic sword. */
    private static WeaponDefinition ironblade() {
        return new WeaponDefinition("ironblade", "Ironblade", Element.KINETIC, Rarity.COMMON,
                List.of(new TriggerBinding("left_click",
                        ability("ironblade/left_click", ResourceCost.FREE, 10))));
    }

    /** A costed, cooldowned left-click, for the spend/refuse paths. */
    private static WeaponDefinition costedSword() {
        return new WeaponDefinition("staff", "Staff", Element.KINETIC, Rarity.RARE,
                List.of(new TriggerBinding("left_click",
                        ability("staff/left_click", new ResourceCost("energy", 40), 10))));
    }

    /** Two triggers, both cooldowned, for the independence proof. */
    private static WeaponDefinition twoTriggerSword() {
        return new WeaponDefinition("emberblade", "Emberblade", Element.KINETIC, Rarity.EPIC,
                List.of(
                        new TriggerBinding("left_click", ability("emberblade/left_click", ResourceCost.FREE, 200)),
                        new TriggerBinding("right_click", ability("emberblade/right_click", ResourceCost.FREE, 200))));
    }

    private static void dispatch(FakeWorld world, Optional<CastResult> result) {
        var success = assertInstanceOf(CastResult.Success.class, result.orElseThrow());
        new CastExecutor(world).execute(success);
    }

    @Test
    void aWeaponTriggerFiresThroughFakeWorldAndIsNotArchetypeGated() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);

        var service = serviceWith(() -> 0L, pool(() -> 0L));

        dispatch(world, service.fire(caster.snapshot(), ironblade(), "left_click", FORWARD));

        assertEquals(92, target.health, 1e-9);                       // 100 - 8, no multiplier
        assertEquals(caster.id(), target.lastDamageSource, "the swinger must be credited");
    }

    @Test
    void anInputWithNoBindingIsEmptyNotAThrowAndNotTheWrongTrigger() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var service = serviceWith(() -> 0L, pool(() -> 0L));

        // ironblade binds only left_click.
        assertTrue(service.fire(caster.snapshot(), ironblade(), "right_click", FORWARD).isEmpty());
    }

    /**
     * Atomicity: check-and-commit is one core call, so a second swing within the
     * cooldown window spends nothing. Deleting the cooldown check from resolve() lets
     * the second swing succeed and drains 40 more -- this assertion is what catches it.
     */
    @Test
    void twoRapidSwingsSpendEnergyExactlyOnce() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var resources = pool(() -> 0L); // tick frozen: the second swing is on cooldown
        var service = serviceWith(() -> 0L, resources);
        var weapon = costedSword();

        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "left_click", FORWARD).orElseThrow());
        assertInstanceOf(CastResult.OnCooldown.class,
                service.fire(caster.snapshot(), weapon, "left_click", FORWARD).orElseThrow());

        assertEquals(60, resources.current(caster.id(), "energy"), 1e-9); // spent exactly once
    }

    @Test
    void aSwingRefusedForEnergySpendsNothing() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var resources = pool(() -> 0L);
        resources.tryConsume(caster.id(), "energy", 70); // 30 left, trigger costs 40
        var service = serviceWith(() -> 0L, resources);

        var result = service.fire(caster.snapshot(), costedSword(), "left_click", FORWARD);

        assertInstanceOf(CastResult.InsufficientResource.class, result.orElseThrow());
        assertEquals(30, resources.current(caster.id(), "energy"), 1e-9); // untouched
    }

    /**
     * The cooldown key is (player, weaponId, input): left-click on cooldown must leave
     * right-click ready. If the key dropped input and used weaponId alone, the two
     * triggers would share a timer and this right-click would come back OnCooldown.
     */
    @Test
    void leftAndRightClickCooldownIndependently() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var service = serviceWith(() -> 0L, pool(() -> 0L));
        var weapon = twoTriggerSword();

        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "left_click", FORWARD).orElseThrow());
        assertInstanceOf(CastResult.OnCooldown.class,
                service.fire(caster.snapshot(), weapon, "left_click", FORWARD).orElseThrow());

        // Right-click was never fired; it keys on a different input, so it is ready.
        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "right_click", FORWARD).orElseThrow());
    }

    /**
     * emberblade's exact model, proven server-free: one weapon, a FREE left-click and a
     * COSTED right-click, sharing the one energy pool. The free trigger spends nothing; the
     * costed one draws from the shared pool; a drained pool blocks the costed trigger while
     * the free one keeps firing. Cooldowns are 0 here so nothing but energy can gate a
     * trigger -- this isolates the shared-pool interaction, which 1a's tests (a single costed
     * trigger, or two free ones) do not cover on one weapon.
     */
    @Test
    void freeLeftClickCostsNothingWhileCostedRightClickSpendsTheSharedPool() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var resources = pool(() -> 0L); // tick frozen: no regen, and cd 0 means no cooldown gate
        var service = serviceWith(() -> 0L, resources);
        var weapon = new WeaponDefinition("emberblade", "Emberblade", Element.KINETIC, Rarity.RARE,
                List.of(
                        new TriggerBinding("left_click", ability("emberblade/left_click", ResourceCost.FREE, 0)),
                        new TriggerBinding("right_click",
                                ability("emberblade/right_click", new ResourceCost("energy", 40), 0))));

        // Free left-click fires and spends nothing from the shared pool.
        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "left_click", FORWARD).orElseThrow());
        assertEquals(100, resources.current(caster.id(), "energy"), 1e-9);

        // Each costed right-click draws 40 from the SAME pool.
        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "right_click", FORWARD).orElseThrow());
        assertEquals(60, resources.current(caster.id(), "energy"), 1e-9);
        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "right_click", FORWARD).orElseThrow());
        assertEquals(20, resources.current(caster.id(), "energy"), 1e-9);

        // Drained below the 40 cost: the right-click is blocked, and the refusal spends nothing.
        assertInstanceOf(CastResult.InsufficientResource.class,
                service.fire(caster.snapshot(), weapon, "right_click", FORWARD).orElseThrow());
        assertEquals(20, resources.current(caster.id(), "energy"), 1e-9);

        // ...but the FREE left-click still fires -- a drained pool does not block it.
        assertInstanceOf(CastResult.Success.class,
                service.fire(caster.snapshot(), weapon, "left_click", FORWARD).orElseThrow());
        assertEquals(20, resources.current(caster.id(), "energy"), 1e-9);
    }

    /**
     * The bow, proven server-free: a FREE right-click Projectile trigger (the first
     * projectile fired through the weapon path, not just an ability), whose fire rate is a
     * cooldown -- click-to-shoot, no draw-and-charge. The shot spends nothing (the Ranger
     * economy), the projectile flies rather than teleporting, and the next shot is gated by
     * the cooldown, not by any charge state.
     */
    @Test
    void aBowShotIsAFreeProjectileGatedByItsFireRateCooldown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);
        var resources = pool(() -> 0L); // tick frozen: within the 15-tick fire-rate cooldown
        var service = serviceWith(() -> 0L, resources);
        var bow = new WeaponDefinition("hunters_bow", "Bow", Element.SOLAR, Rarity.UNCOMMON, "bow",
                List.of(new TriggerBinding("right_click",
                        new AbilityDefinition("hunters_bow/right_click", "Shot", Element.SOLAR, "none",
                                15, ResourceCost.FREE,
                                new CastSpec.Projectile(1.0, 0, 60),
                                List.of(new EffectSpec.Damage(6, Element.SOLAR))))));

        // The shot fires -- a projectile through the weapon path -- and is free.
        dispatch(world, service.fire(caster.snapshot(), bow, "right_click", FORWARD));
        assertEquals(100, resources.current(caster.id(), "energy"), 1e-9); // the bow carries the damage

        // Fire rate is the cooldown, not a charge: the immediate next shot is OnCooldown.
        assertInstanceOf(CastResult.OnCooldown.class,
                service.fire(caster.snapshot(), bow, "right_click", FORWARD).orElseThrow());

        // The arrow flies rather than teleporting: nothing is hit on the launch frame.
        assertEquals(100, target.health, 1e-9, "no hit on the launch frame");
        world.advanceTicks(5);
        assertEquals(94, target.health, 1e-9); // 100 - 6, struck downrange
    }
}
