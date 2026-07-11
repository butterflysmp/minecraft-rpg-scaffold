package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.*;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Resolving an aim into an impact: Self, Melee, Ray. */
class CastExecutorTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    private static AbilityDefinition ability(CastSpec cast, EffectSpec... onHit) {
        return new AbilityDefinition("test", "Test", "fire", "none",
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
                service.cast(caster.snapshot(), "test", aim, java.util.Set.of(def.id())));
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
                new EffectSpec.Burst(1.0, List.of(new EffectSpec.Damage(10, "fire")))),
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

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

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

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

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
                new EffectSpec.Damage(12, "fire"), new EffectSpec.Visual("boom")));

        // The miss detonates at the END of the aim, which is now a segment away: the ray
        // crosses x=16 and the second segment resolves a tick later.
        assertEquals(List.of(), world.presented, "the far end has not been reached yet");
        world.advanceTicks(1);

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

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, behindTheWall.health, 1e-9, "terrain must block the ray");
    }

    @Test
    void rayDoesNotReachBeyondItsRange() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var distant = new FakeWorld.Dummy(new Vec3(40, 0, 0));
        world.entities.add(distant);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, distant.health, 1e-9);
    }

    /**
     * A ray no longer resolves in one trace. Its first chunk column runs inline on the cast
     * frame; every column after that costs a tick, because crossing a chunk plane means
     * handing the trace to the region that owns the next chunk.
     */
    @Test
    void rayCrossingAChunkPlaneCostsATick() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var beyondThePlane = new FakeWorld.Dummy(new Vec3(20, 0, 0)); // x=20 is column 1
        world.entities.add(caster);
        world.entities.add(beyondThePlane);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, beyondThePlane.health, 1e-9,
                "the first segment stops at x=16 and cannot see into the next column");

        world.advanceTicks(1);
        assertEquals(88, beyondThePlane.health, 1e-9, "the second segment strikes it");
    }

    /** Two planes crossed, two ticks. The cost of a ray varies with how far it reaches. */
    @Test
    void rayCrossingTwoChunkPlanesCostsTwoTicks() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var far = new FakeWorld.Dummy(new Vec3(35, 0, 0)); // column 2
        world.entities.add(caster);
        world.entities.add(far);

        cast(world, caster, ability(new CastSpec.Ray(40), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, far.health, 1e-9);
        world.advanceTicks(1);
        assertEquals(100, far.health, 1e-9, "still only in column 1");
        world.advanceTicks(1);
        assertEquals(88, far.health, 1e-9, "column 2, on tick 2");
    }

    /** Stopping at the first body is what makes an already-hit set unnecessary. */
    @Test
    void rayStrikesExactlyOneBodyEvenAcrossColumns() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var first = new FakeWorld.Dummy(new Vec3(20, 0, 0));  // column 1
        var second = new FakeWorld.Dummy(new Vec3(35, 0, 0)); // column 2
        world.entities.add(caster);
        world.entities.add(first);
        world.entities.add(second);

        cast(world, caster, ability(new CastSpec.Ray(40), new EffectSpec.Damage(12, "fire")));
        world.advanceTicks(10);

        assertEquals(88, first.health, 1e-9);
        assertEquals(100, second.health, 1e-9, "the walk stops at the first body");
        assertEquals(0, world.pendingTasks(), "and schedules no further segments");
    }

    /**
     * A KNOWN, ACCEPTED DEFECT. This test asserts the bug, not the fix.
     *
     * >>> If you fix this, the assertion INVERTS: expect 88, not 100. <<<
     *
     * Confining a segment to one chunk column means the trace only sees entities that
     * column's region owns. This mob's CENTRE is at x=16.05, in column 1. The ray runs up
     * the z axis at x=15.7, entirely inside column 0, and passes 0.35 blocks from the mob
     * -- comfortably inside the 0.6 hitRadius, so it WOULD be struck if it were visible.
     * It is not: column 0's segment cannot see into column 1, and the ray never enters it.
     *
     * The 0.35 matters. An earlier version of this test put the mob at x=16.3, exactly
     * hitRadius away, where 16.3 - 15.7 = 0.6000000000000014 and the fake skipped it for
     * floating-point reasons. It asserted the right answer for the wrong reason, and passed
     * even with the column filter deleted.
     *
     * So a hitbox straddling a chunk plane can be missed. Fixing it needs a widened trace,
     * or a second query into the neighbouring column. Both are out of scope; what is in
     * scope is that FakeWorld can now SEE this, where before it scanned the whole world and
     * could not.
     */
    @Test
    void rayMissesAnEntityWhoseCentreLiesAcrossAChunkPlane() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(15.7, 0, 0));
        var straddler = new FakeWorld.Dummy(new Vec3(16.05, 0, 5)); // centre in column 1
        world.entities.add(caster);
        world.entities.add(straddler);

        // Straight up +z, staying at x=15.7: never leaves column 0.
        Aim upTheZAxis = new Aim(new Vec3(15.7, 0, 0), new Vec3(0, 0, 1));
        cast(world, caster, ability(new CastSpec.Ray(20),
                new EffectSpec.Damage(12, "fire")), upTheZAxis);
        world.advanceTicks(10);

        assertTrue(straddler.position().subtract(new Vec3(15.7, 0, 5)).length() < world.hitRadius,
                "it is well inside hitRadius, so only the column check can hide it");
        assertEquals(100, straddler.health, 1e-9,
                "MISSED: its centre is in a column the ray never traces. See the javadoc.");
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

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

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

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

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

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

        assertEquals(88, justInside.health, 1e-9);
        assertEquals(100, justOutside.health, 1e-9);
    }

    @Test
    void meleeMissesBeyondItsReach() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var tooFar = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(tooFar);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, tooFar.health, 1e-9);
    }

    @Test
    void meleeIgnoresTheCasterItself() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        cast(world, caster, ability(new CastSpec.Melee(3, 360), new EffectSpec.Damage(12, "fire")));

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
                new EffectSpec.Area(6.0, 20, 20, List.of(new EffectSpec.Damage(2, "fire")))));

        // The ray crosses x=16, so its far end resolves on tick 1, not on the cast frame.
        // The area placed there pulses one tick_interval later: 1 + 20 = 21.
        world.advanceTicks(21);

        assertEquals(98, atTheEnd.health, 1e-9, "the area should land at the ray's end point");
    }
}
