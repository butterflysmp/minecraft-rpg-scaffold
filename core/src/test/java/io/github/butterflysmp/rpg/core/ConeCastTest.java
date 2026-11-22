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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CONE CAST, CAUSED (the seat's brief of 2026-11-22, the Living Lantern, rulings 1 to 4). Each row stages the
 * condition it names and reads the outcome off each dummy's own health and damage count, never off an arm's existence.
 *
 * <p><b>THE GEOMETRY.</b> The aim runs from the origin down +x. Every body stands with its FEET 1.62 below the aim's
 * plane, so its SIGHT POINT (feet + the dummy's 1.62 eye height, {@code CombatantSnapshot.sightPoint}) lies ON that
 * plane. A body at feet {@code (x, -1.62, z)} is then exactly {@code sqrt(x^2 + z^2)} from the origin and
 * {@code atan(z/x)} off the aim, which is what the cone judges. The cone here is range 6, 60 degrees (30 either side),
 * travel 6 ticks unless a row says otherwise: the front is at {@code k} blocks on tick {@code k}.
 *
 * <p>Every staged distance and angle sits a margin away from its boundary, so a row cannot pass on a floating-point
 * accident at an edge.
 */
class ConeCastTest {

    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Aim FORWARD = new Aim(Vec3.ZERO, X);
    private static final double DAMAGE = 14;
    private static final double SIGHT_DROP = 1.62;

    private static AbilityDefinition lantern(CastSpec.Cone cone) {
        return new AbilityDefinition("breath", "Breath", "fire", 0, ResourceCost.FREE, cone,
                List.of(new EffectSpec.Visual("hit"), new EffectSpec.Damage(DAMAGE, "fire")));
    }

    private static CastSpec.Cone cone(int travelTicks) {
        return new CastSpec.Cone(6.0, 60.0, travelTicks, "front");
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def, Aim aim) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), aim, Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    /** A body whose SIGHT POINT is at {@code (x, 0, z)}. */
    private static FakeWorld.Dummy body(FakeWorld world, double x, double z) {
        var d = new FakeWorld.Dummy(new Vec3(x, -SIGHT_DROP, z));
        world.entities.add(d);
        return d;
    }

    /** A body {@code distance} from the origin, {@code degrees} off the aim, in the aim's plane. */
    private static FakeWorld.Dummy polar(FakeWorld world, double distance, double degrees) {
        double r = Math.toRadians(degrees);
        return body(world, distance * Math.cos(r), distance * Math.sin(r));
    }

    private static FakeWorld.Dummy caster() {
        return new FakeWorld.Dummy(new Vec3(-50, 0, 0));   // far behind: never in its own cone unless a row says so
    }

    // ============================================================================== ruling 1: the angle and the range

    /**
     * Inside the angle and the range: struck. Just outside each: not. 29 and 31 degrees at 3 blocks straddle the 30
     * either side; 5.9 and 6.1 blocks straddle the range of 6.
     *
     * <p>Forces red: halving the wrong way ({@code cos(arc)} where {@code cos(arc/2)} belongs, so 31 degrees is
     * struck); measuring the range past the front (6.1 struck).
     */
    @Test
    void aBodyInsideTheAngleAndRangeIsStruckAndOneJustOutsideEachIsNot() {
        var world = new FakeWorld();
        var in29 = polar(world, 3.0, 29);
        var out31 = polar(world, 3.0, 31);
        var in59 = polar(world, 5.9, 0);
        var out61 = polar(world, 6.1, 0);
        var below29 = polar(world, 3.0, -29);   // the other side of the aim: the angle is symmetric
        cast(world, caster(), lantern(cone(6)), FORWARD);
        world.advanceTicks(10);
        assertEquals(86.0, in29.health, "29 degrees: inside 30");
        assertEquals(100.0, out31.health, "31 degrees: outside 30");
        assertEquals(86.0, in59.health, "5.9 blocks: inside 6");
        assertEquals(100.0, out61.health, "6.1 blocks: outside 6");
        assertEquals(86.0, below29.health, "-29 degrees: inside, on the other side");
    }

    /** Ruling 1's geometry is the MELEE ARC's angle test, reused: {@code Aim.withinArc}, the full angle halved. */
    @Test
    void theAngleTestIsAimWithinArcTheFullAngleHalved() {
        Aim aim = FORWARD;
        assertTrue(aim.withinArc(new Vec3(Math.cos(Math.toRadians(29)), Math.sin(Math.toRadians(29)), 0), 60));
        assertTrue(!aim.withinArc(new Vec3(Math.cos(Math.toRadians(31)), Math.sin(Math.toRadians(31)), 0), 60));
        assertTrue(aim.withinArc(new Vec3(Math.cos(Math.toRadians(44)), Math.sin(Math.toRadians(44)), 0), 90),
                "a 90-degree swing reaches 45 either side, as meleeTarget's javadoc says");
    }

    // ============================================================================== ruling 2: once each, and the travel

    /**
     * Three bodies inside the cone, at three distances: each struck ONCE, over the whole cast, though the front passes
     * through all six shells. Forces red: dropping the struck set AND the shell's inner bound (each body is struck again
     * on every tick after the front first reaches it).
     */
    @Test
    void threeBodiesInsideTheConeAreEachStruckOnce() {
        var world = new FakeWorld();
        var a = polar(world, 1.5, 10);
        var b = polar(world, 3.5, -15);
        var c = polar(world, 5.5, 20);
        cast(world, caster(), lantern(cone(6)), FORWARD);
        world.advanceTicks(20);
        assertEquals(List.of(1, 1, 1), List.of(a.damageCalls, b.damageCalls, c.damageCalls), "one hit each");
        assertEquals(List.of(86.0, 86.0, 86.0), List.of(a.health, b.health, c.health));
        assertEquals(0, world.pendingTasks(), "the cast ends at its last front tick");
    }

    /**
     * THE STRUCK SET, ON ITS OWN AXIS. For a body standing still the shell's inner bound alone keeps it to one hit, so
     * the row above cannot see the set. Here a body is struck on tick 2 (at 1.5 blocks), then moves OUTWARD ahead of the
     * front (to 4.5), into a shell the front has not reached: only the set of struck ids keeps it from a second hit on
     * tick 5. A second body that starts at 4.5 is the control that tick 5's shell does strike there.
     *
     * <p>Forces red: dropping the struck set (passing an empty set to {@code ConeShell.reached}).
     */
    @Test
    void aBodyStruckOnceIsNotStruckAgainAfterMovingAheadOfTheFront() {
        var world = new FakeWorld();
        var mover = polar(world, 1.5, 0);
        var control = polar(world, 4.5, 10);
        cast(world, caster(), lantern(cone(6)), FORWARD);
        world.advanceTicks(2);
        assertEquals(1, mover.damageCalls, "struck on tick 2");
        mover.moveTo(new Vec3(4.5, -SIGHT_DROP, 0));
        world.advanceTicks(10);
        assertEquals(1, mover.damageCalls, "not again on tick 5");
        assertEquals(1, control.damageCalls, "CONTROL: tick 5's shell strikes a body at 4.5");
    }

    /**
     * THE TRAVEL: the near body is struck on an earlier tick than the far one, by the predicted number of ticks. The
     * front is at {@code k} blocks on tick {@code k} (6 blocks over 6 ticks), so a body at 2.5 is reached on tick 3 and
     * one at 5.5 on tick 6: three ticks apart. Read one tick at a time off each body's health.
     *
     * <p>Forces red: striking everything on the first pass (both on tick 1); an off-by-one in the front's step (2 and 5,
     * or 4 and 7).
     */
    @Test
    void theNearBodyIsStruckThreeTicksBeforeTheFarOne() {
        var world = new FakeWorld();
        var near = polar(world, 2.5, 5);
        var far = polar(world, 5.5, -5);
        cast(world, caster(), lantern(cone(6)), FORWARD);
        int nearTick = -1;
        int farTick = -1;
        for (int t = 0; t <= 8; t++) {
            if (t > 0) world.advanceTicks(1);
            if (nearTick < 0 && near.health < 100) nearTick = t;
            if (farTick < 0 && far.health < 100) farTick = t;
        }
        assertEquals(3, nearTick, "2.5 blocks: the front reaches 3 on tick 3");
        assertEquals(6, farTick, "5.5 blocks: the front reaches 6 on tick 6");
        assertEquals(3, farTick - nearTick);
    }

    /**
     * A body that walks INTO the cone BEHIND the front is not struck: it stood outside the angle until the front had
     * passed its distance, then stepped in. A second body stepping in AHEAD of the front is struck, which is the
     * control that the move itself is read.
     *
     * <p>Forces red: dropping the shell's inner bound (the late body, now at 2 blocks, is struck on tick 4).
     */
    @Test
    void aBodyThatWalksInBehindTheFrontIsNotStruck() {
        var world = new FakeWorld();
        var late = polar(world, 2.0, 70);       // outside the angle
        var early = polar(world, 5.5, 70);      // outside the angle too
        cast(world, caster(), lantern(cone(6)), FORWARD);
        world.advanceTicks(3);                   // the front has reached 3: past 2
        late.moveTo(new Vec3(2.0, -SIGHT_DROP, 0));
        early.moveTo(new Vec3(5.5, -SIGHT_DROP, 0));
        world.advanceTicks(10);
        assertEquals(100.0, late.health, "stepped in behind the front");
        assertEquals(86.0, early.health, "CONTROL: stepped in ahead of the front, and was reached on tick 6");
    }

    /**
     * THE ORIGIN AND DIRECTION ARE CAPTURED AT THE CLICK. After the cast the caster turns round and walks away; the
     * cone still goes down the old line from the old eye. Forces red: re-reading {@code aimOf} per tick (the turned
     * caster's cone would strike the body behind, and miss the one ahead).
     */
    @Test
    void theCasterTurningAfterTheClickDoesNotMoveIt() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(0, -SIGHT_DROP, 0));
        caster.facing = X;
        world.entities.add(caster);
        var ahead = polar(world, 4.5, 0);
        var behind = body(world, -4.5, 0);
        cast(world, caster, lantern(cone(6)), FORWARD);
        world.advanceTicks(1);
        caster.facing = new Vec3(-1, 0, 0);
        caster.moveTo(new Vec3(-20, -SIGHT_DROP, 0));
        world.advanceTicks(10);
        assertEquals(86.0, ahead.health, "the old line");
        assertEquals(100.0, behind.health, "not the new facing");
    }

    /** {@code travel_ticks: 0} strikes every body in the cone on the cast's own tick, and leaves nothing scheduled. */
    @Test
    void travelZeroStrikesAllOnTheCastsTick() {
        var world = new FakeWorld();
        var near = polar(world, 1.0, 0);
        var far = polar(world, 5.8, 25);
        cast(world, caster(), lantern(cone(0)), FORWARD);
        assertEquals(List.of(86.0, 86.0), List.of(near.health, far.health), "before any tick has passed");
        assertEquals(0, world.pendingTasks(), "nothing deferred");
    }

    // ============================================================================== ruling 3: line of sight

    /**
     * A wall stops it: a body behind a block is not struck, one before it is. Read through {@code lineOfSightClear}
     * from the origin to the body's sight point. Forces red: skipping the sight check.
     */
    @Test
    void aWallStopsIt() {
        var world = new FakeWorld();
        world.wallX = 3.0;
        var before = polar(world, 2.0, 0);
        var behind = polar(world, 4.5, 0);
        cast(world, caster(), lantern(cone(6)), FORWARD);
        world.advanceTicks(10);
        assertEquals(86.0, before.health, "before the wall");
        assertEquals(100.0, behind.health, "behind the wall");
        assertTrue(world.sightCheckTo.contains(behind.snapshot().sightPoint()), "traced to its SIGHT point");
    }

    // ============================================================================== ruling 4: never the caster

    /**
     * The cone never strikes its caster. The aim's origin is put BEHIND the caster here, so the caster's own sight
     * point stands 3 blocks down the aim, inside angle and range: only the id excludes it. A second body at the same
     * spot is struck, the control that the spot is inside the cone.
     */
    @Test
    void itNeverStrikesItsCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(0, -SIGHT_DROP, 0));
        world.entities.add(caster);
        var twin = body(world, 0, 0);
        cast(world, caster, lantern(cone(6)), new Aim(new Vec3(-3, 0, 0), X));
        world.advanceTicks(10);
        assertEquals(100.0, caster.health, "the caster");
        assertEquals(86.0, twin.health, "CONTROL: the same spot, another body");
    }

    // ============================================================================== the front's visual

    /**
     * The front is drawn once per tick, as a ring at the front's distance whose radius is the cone's there
     * ({@code distance x tan(30)}), plus its centre. Read off the presented points: on tick 3 the ring's points are 3
     * along the aim and {@code 3 x tan 30} off it.
     */
    @Test
    void theFrontIsDrawnAsARingAtItsDistanceEachTick() {
        var world = new FakeWorld();
        cast(world, caster(), lantern(cone(6)), FORWARD);
        world.advanceTicks(3);
        List<Vec3> tick3 = new ArrayList<>();
        for (int i = 0; i < world.presented.size(); i++) {
            if (world.presented.get(i).equals("front") && Math.abs(world.presentedAt.get(i).x() - 3.0) < 1e-9) {
                tick3.add(world.presentedAt.get(i));
            }
        }
        assertEquals(1 + io.github.butterflysmp.rpg.core.combat.ConeShell.FRONT_RING_POINTS, tick3.size(),
                "the centre and the ring, on tick 3: " + tick3);
        double radius = 3.0 * Math.tan(Math.toRadians(30));
        long onRing = tick3.stream().filter(p -> Math.abs(Math.hypot(p.y(), p.z()) - radius) < 1e-9).count();
        assertEquals(io.github.butterflysmp.rpg.core.combat.ConeShell.FRONT_RING_POINTS, onRing, "every ring point");
        assertEquals(3, world.presented.stream().filter("front"::equals).count()
                / (1 + io.github.butterflysmp.rpg.core.combat.ConeShell.FRONT_RING_POINTS), "three ticks, three fronts");
    }
}
