package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.*;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A projectile flies. It does not teleport. */
class ProjectileFlightTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** speed 1 block/tick, no gravity unless asked, generous fuse. */
    private static AbilityDefinition grenade(double speed, double gravity, int lifetime,
                                             EffectSpec... onHit) {
        return new AbilityDefinition("grenade", "Grenade", "fire", "hunter",
                0, ResourceCost.FREE, new CastSpec.Projectile(speed, gravity, lifetime),
                List.of(onHit));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def, Aim aim) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "grenade", aim, java.util.Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    private static final EffectSpec.Damage HIT = new EffectSpec.Damage(12, "fire");

    /** A basic-attack payload: no literal, deals whatever the caster froze at cast time. */
    private static final EffectSpec.WeaponDamage WEAPON_HIT = new EffectSpec.WeaponDamage("fire");

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

    /**
     * FakeWorld.castRay only shows entities in chunk columns the segment passes through.
     * A projectile's segment is NOT confined to one column -- unlike a ray's, it is one
     * tick of flight and may cross several planes -- so it must still see targets in every
     * column it traverses.
     *
     * This is the clause of the prediction that the port-split commit could not check,
     * because the column filter did not exist yet. The target sits at x=25, in column 1;
     * the 40-block segment starts in column 0. A filter that looked only at the segment's
     * starting column would hide it, and every other projectile test would still pass,
     * because their targets all sit in column 0.
     */
    @Test
    void aProjectileSegmentSeesTargetsInEveryColumnItCrosses() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(25, 0, 0)); // column 1, mid-segment
        world.entities.add(target);

        cast(world, caster, grenade(40.0, 0, 100, HIT), FORWARD);

        assertEquals(88, target.health, 1e-9,
                "a 40-block segment crosses x=16 and must still see into column 1");
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
                new EffectSpec.Area(3.0, 20, 20, List.of(new EffectSpec.Damage(2, "fire")))),
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

    /**
     * THE test this pass exists for. A projectile's payload is resolved at IMPACT, on the region
     * that owns the TARGET -- which on Folia is not the caster's region. So a WeaponDamage payload
     * cannot ask the world for the caster's attack damage when it lands; it must use the value
     * frozen when the shot was fired.
     *
     * Proven by making the two answers differ: the caster's attack damage is 6 at launch and 99
     * while the arrow is still in the air. Anything that re-reads live caster state at impact deals
     * 99. The frozen carry deals 6.
     *
     * This is the core-side proof of the freeze, and it is the only one there can be: Paper is
     * single-region, so no boot can make the two regions actually differ.
     */
    @Test
    void aProjectileDealsTheAttackDamageFrozenAtLaunchNotAtImpact() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);
        caster.attackDamage = 6.0;

        cast(world, caster, grenade(1.0, 0, 100, WEAPON_HIT), FORWARD);
        assertEquals(100, target.health, 1e-9, "no hit on the launch frame");

        // Mid-flight, the caster's stat changes -- a weapon swap, a modifier expiring, a buff.
        caster.attackDamage = 99.0;

        world.advanceTicks(10);
        assertEquals(94, target.health, 1e-9,
                "the arrow deals the 6 frozen at launch, not the 99 the caster has at impact");
        // Mutation: build the Caster inside the impact lambda from a live world.combatant(id) read
        // instead of at launch -> "expected: <94> but was: <1>" -> reddens.
    }

    /**
     * The freeze also makes a WeaponDamage shot survive its own thrower. Under the retired hit-time
     * read, a despawned caster resolved to 0 attack damage and the amt>0 guard swallowed the hit
     * entirely -- an arrow already in the air did nothing because the archer logged out. The frozen
     * value has nothing left to look up, so it simply lands.
     */
    @Test
    void aWeaponDamageProjectileStillLandsAfterItsCasterDespawns() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);
        caster.attackDamage = 6.0;

        cast(world, caster, grenade(1.0, 0, 100, WEAPON_HIT), FORWARD);
        world.advanceTicks(2); // airborne, around x=3

        world.entities.remove(caster); // the archer logs out mid-flight

        assertDoesNotThrow(() -> world.advanceTicks(50));
        assertEquals(94, target.health, 1e-9, "the shot still lands its frozen 6");
        assertEquals(caster.id(), target.lastDamageSource, "and is still attributed to the archer");
    }

    /**
     * The class-damage bonus is frozen at launch too, and for exactly the reason the attack-damage
     * freeze exists: a projectile's payload resolves on the TARGET'S region, cross-region from the
     * caster on Folia, so nothing at impact may read the caster's store.
     *
     * This costs no new plumbing -- the bonus rides the same Caster projection -- and this test is
     * what proves it rather than assuming it. The bonus is 5 at launch and 99 in the air; anything
     * re-reading live caster state deals the 99.
     *
     * The payload is a LITERAL Damage, deliberately: it is the ember_staff shape, the case that had
     * no stat to grip before this pass. So this single test covers the freeze AND the literal arm on
     * the ranged path at once. Paper is single-region, so no boot can substitute for it.
     */
    @Test
    void aProjectileDealsTheClassDamageBonusFrozenAtLaunchNotAtImpact() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);
        caster.classDamageBonus = 5.0;   // +5 Magic, active because a mage weapon is held

        cast(world, caster, grenade(1.0, 0, 100, HIT), FORWARD);   // HIT is a literal Damage(12)
        assertEquals(100, target.health, 1e-9, "no hit on the launch frame");

        // Mid-flight the wielder swaps to a sword, so +Magic would stop matching -- or a bigger
        // modifier lands. Either way the shot in the air was paid for at launch.
        caster.classDamageBonus = 99.0;

        world.advanceTicks(10);
        assertEquals(83, target.health, 1e-9,
                "12 literal + the 5 frozen at launch, not the 99 the caster has at impact");
        // Mutation: read the bonus from a live world.combatant(id) at impact -> 100 - 111 -> reddens.
    }

    /**
     * And it survives its own caster, like the attack-damage freeze: a frozen value has nothing left
     * to look up, so a despawned wielder cannot silently zero a shot already in the air.
     */
    @Test
    void aClassBoostedProjectileStillLandsAfterItsCasterDespawns() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);
        caster.classDamageBonus = 5.0;

        cast(world, caster, grenade(1.0, 0, 100, HIT), FORWARD);
        world.advanceTicks(2);
        world.entities.remove(caster);

        assertDoesNotThrow(() -> world.advanceTicks(50));
        assertEquals(83, target.health, 1e-9, "the shot still lands its frozen 12 + 5");
    }
}
