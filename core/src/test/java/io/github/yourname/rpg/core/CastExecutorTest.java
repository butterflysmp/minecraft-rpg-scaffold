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

/** Resolving an aim into an impact: Self, Melee, Ray. */
class CastExecutorTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    private static AbilityDefinition ability(CastSpec cast, EffectSpec... onHit) {
        return new AbilityDefinition("test", "Test", Element.SOLAR, "none",
                0, ResourceCost.FREE, cast, List.of(onHit));
    }

    /** Run an ability with no cooldown and no cost, so only resolution is under test. */
    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        cast(world, caster, def, FORWARD);
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def, Aim aim) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster, "test", aim));
        new CastExecutor(world).execute(success);
    }

    /**
     * A Self cast detonates at the caster's FEET, not at the aim's origin -- which in
     * production is their eye, a metre and a half higher (RpgCommand builds the Aim from
     * getEyeLocation(); a Combatant's position is getLocation()).
     *
     * Nothing pinned this before. Every other Self test heals, and Heal never reads the
     * origin; and in this fake the dummy's position happens to equal the aim origin. So a
     * refactor that resolved the Self origin from the Aim would have moved the detonation
     * 1.62 blocks upward in silence.
     *
     * The bystander is 0.5 blocks from the feet (inside a radius-1 burst) and 1.69 blocks
     * from the eye (outside it). It is damaged if and only if we detonate at the feet.
     */
    @Test
    void selfCastDetonatesAtTheCastersFeetNotTheirEye() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var bystander = new FakeWorld.Dummy(new Vec3(0.5, 0, 0));
        world.entities.add(caster);
        world.entities.add(bystander);

        Aim fromTheEye = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));

        cast(world, caster, ability(new CastSpec.Self(),
                new EffectSpec.Burst(1.0, List.of(new EffectSpec.Damage(10, Element.SOLAR)))),
                fromTheEye);

        assertEquals(90, bystander.health, 1e-9,
                "the burst must be centred on the caster's feet, not on their eye");
        assertEquals(100, caster.health, 1e-9, "a burst never splashes its own caster");
    }

    @Test
    void selfCastTargetsTheCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(5, 64, 5));
        caster.health = 50;
        var bystander = new FakeWorld.Dummy(new Vec3(6, 64, 5));
        world.entities.add(caster);
        world.entities.add(bystander);

        cast(world, caster, ability(new CastSpec.Self(), new EffectSpec.Heal(20)));

        assertEquals(70, caster.health, 1e-9);
        assertEquals(100, bystander.health, 1e-9, "a self cast must not touch anyone else");
    }

    @Test
    void rayHitsTheFirstCombatantAlongIt() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var near = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        var far = new FakeWorld.Dummy(new Vec3(10, 0, 0));
        world.entities.add(caster);
        world.entities.add(near);
        world.entities.add(far);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(88, near.health, 1e-9);
        assertEquals(100, far.health, 1e-9, "the ray must stop at the first body");
    }

    @Test
    void rayIgnoresTheCasterStandingAtItsOrigin() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(100, caster.health, 1e-9);
        assertEquals(88, target.health, 1e-9);
    }

    @Test
    void rayMissesWhenNothingIsInTheWay() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var offToTheSide = new FakeWorld.Dummy(new Vec3(5, 9, 0));
        world.entities.add(offToTheSide);

        cast(world, caster, ability(new CastSpec.Ray(30),
                new EffectSpec.Damage(12, Element.SOLAR), new EffectSpec.Visual("boom")));

        assertEquals(100, offToTheSide.health, 1e-9);
        assertEquals(List.of("boom"), world.presented, "an untargeted effect still fires on a miss");
    }

    @Test
    void rayStopsAtAWall() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var behindTheWall = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(behindTheWall);
        world.blockDistance = 3; // wall between caster and target

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(100, behindTheWall.health, 1e-9, "terrain must block the ray");
    }

    @Test
    void rayDoesNotReachBeyondItsRange() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var distant = new FakeWorld.Dummy(new Vec3(40, 0, 0));
        world.entities.add(distant);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(100, distant.health, 1e-9);
    }

    @Test
    void meleeHitsTheNearestTargetInsideTheArc() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var near = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var alsoInArc = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        world.entities.add(caster);
        world.entities.add(near);
        world.entities.add(alsoInArc);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(88, near.health, 1e-9);
        assertEquals(100, alsoInArc.health, 1e-9, "melee strikes one target, the nearest");
    }

    @Test
    void meleeMissesSomeoneBehindTheCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var behind = new FakeWorld.Dummy(new Vec3(-1, 0, 0));
        world.entities.add(caster);
        world.entities.add(behind);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(100, behind.health, 1e-9);
    }

    /** A 90-degree swing reaches 45 degrees either side, so (1,0,1) is on the edge. */
    @Test
    void meleeArcWidthIsTheFullConeNotTheHalfAngle() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var justInside = new FakeWorld.Dummy(new Vec3(1, 0, 0.9)); // ~42 degrees off aim
        var justOutside = new FakeWorld.Dummy(new Vec3(1, 0, 1.1)); // ~48 degrees off aim
        world.entities.add(justInside);
        world.entities.add(justOutside);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(88, justInside.health, 1e-9);
        assertEquals(100, justOutside.health, 1e-9);
    }

    @Test
    void meleeMissesBeyondItsReach() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var tooFar = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(tooFar);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(100, tooFar.health, 1e-9);
    }

    @Test
    void meleeIgnoresTheCasterItself() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        cast(world, caster, ability(new CastSpec.Melee(3, 360), new EffectSpec.Damage(12, Element.SOLAR)));

        assertEquals(100, caster.health, 1e-9);
    }

    @Test
    void aMissStillDetonatesAtTheEndOfTheAim() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        // Nothing in the ray's path, but something sitting where it ends.
        var atTheEnd = new FakeWorld.Dummy(new Vec3(30, 5, 0));
        world.entities.add(atTheEnd);

        cast(world, caster, ability(new CastSpec.Ray(30),
                new EffectSpec.Area(6.0, 20, 20, List.of(new EffectSpec.Damage(2, Element.SOLAR)))));

        // A ray resolves on the frame it is cast, but its area is a field: the single
        // pulse is one tick_interval later. Advancing 10 ticks would stop short of it.
        world.advanceTicks(20);

        assertEquals(98, atTheEnd.health, 1e-9, "the area should land at the ray's end point");
    }
}
