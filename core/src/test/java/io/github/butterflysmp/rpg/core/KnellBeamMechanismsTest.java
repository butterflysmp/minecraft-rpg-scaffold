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
import io.github.butterflysmp.rpg.core.combat.RaySweep;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.mob.BeamBox;
import io.github.butterflysmp.rpg.core.mob.BeamLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE KNELL BEAM'S FOUR MECHANISMS, CAUSED (the seat's brief of 2026-11-21, rulings 1 to 4): PIERCE and TOLERANCE on a
 * ray, LOCKED AIM and a WARNING LINE on a volley. Each row stages the condition it names and reads the outcome off the
 * fake's own records (each dummy's health, the presented points), never off an arm's existence.
 *
 * <p>The caster aims down +x from the origin. Every body stands with its FEET 1 below the line, so the line crosses its
 * 0.6 x 1.95 box at height 1: a body's lateral offset is the only thing that moves whether it is struck.
 */
class KnellBeamMechanismsTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));
    private static final double DAMAGE = 10;

    private static AbilityDefinition beam(CastSpec cast) {
        return new AbilityDefinition("beam", "Beam", "void", 0, ResourceCost.FREE, cast,
                List.of(new EffectSpec.Visual("hit"), new EffectSpec.Damage(DAMAGE, "void")));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def, Aim aim) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), aim, Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    /** A body whose feet are 1 below the line, {@code x} along it and {@code z} to the side. */
    private static FakeWorld.Dummy body(FakeWorld world, double x, double z) {
        var d = new FakeWorld.Dummy(new Vec3(x, -1, z));
        world.entities.add(d);
        return d;
    }

    private static boolean struck(FakeWorld.Dummy d) {
        return d.health < 100;
    }

    // ===================================================================================== ruling 1: PIERCE

    /** Three bodies in a line: all three struck, once each, nearest first. */
    @Test
    void aPiercingRayStrikesThreeBodiesInALineOnceEachNearestFirst() {
        var world = new FakeWorld();
        var far = body(world, 12, 0);       // registered first, so registration order cannot pass for distance order
        var near = body(world, 5, 0);
        var mid = body(world, 8, 0);
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.0)), FORWARD);
        world.advanceTicks(10);
        assertEquals(List.of(90.0, 90.0, 90.0), List.of(near.health, mid.health, far.health), "each struck exactly once");
        List<Double> order = world.presentedAt.stream().map(Vec3::x).toList();
        assertEquals(3, order.size(), "one on_hit visual per body, and no empty landing: " + order);
        assertTrue(order.get(0) < order.get(1) && order.get(1) < order.get(2), "nearest first: " + order);
    }

    /**
     * CONTROL: two bodies in a line, a plain ray (no pierce, no tolerance): the first ends it, as every ray did. Their
     * FEET stand on the line here, because the fake's castRay (which a plain ray still uses) hits within its hitRadius of
     * a body's feet, not on the box the sweep uses. First written with feet 1 below, like the rows above, it missed both.
     */
    @Test
    void aPlainRayStillStopsAtTheFirstBody() {
        var world = new FakeWorld();
        var near = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        var mid = new FakeWorld.Dummy(new Vec3(8, 0, 0));
        world.entities.add(near);
        world.entities.add(mid);
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null)), FORWARD);
        world.advanceTicks(10);
        assertTrue(struck(near), "the first body");
        assertEquals(100.0, mid.health, "a plain ray does not pierce");
        assertTrue(world.sweepFrom.isEmpty(), "a plain ray never asks the sweep port: it stays on castRay");
    }

    /** A body standing ON a chunk-column plane is met by both segments and struck ONCE. */
    @Test
    void aBodyAcrossAChunkColumnHopIsStruckOnce() {
        var world = new FakeWorld();
        var straddling = body(world, 16.0, 0);   // its box spans x 15.7 to 16.3, across the x = 16 plane
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.0)), FORWARD);
        world.advanceTicks(10);
        assertTrue(world.sweepFrom.size() >= 2, "the walk hopped a chunk column: " + world.sweepFrom);
        assertEquals(90.0, straddling.health, "struck once, not once per segment that met it");
    }

    /** A block ends the walk: the body before it is struck, the one beyond it is not. */
    @Test
    void aBlockEndsAPiercingWalk() {
        var world = new FakeWorld();
        world.wallX = 10;
        var before = body(world, 5, 0);
        var beyond = body(world, 15, 0);
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.0)), FORWARD);
        world.advanceTicks(10);
        assertTrue(struck(before), "before the wall");
        assertEquals(100.0, beyond.health, "beyond the wall");
    }

    // ===================================================================================== ruling 2: TOLERANCE

    /**
     * Tolerance 0.3: a body whose box is 0.2 off the line is struck; 0.5 off is not; 0.9 off, which the old 1.0 would
     * have struck, is not. "Off" is the gap from the line to the box's near side: the box is 0.6 wide, so the body's
     * centre stands 0.3 further out (0.5, 0.8, 1.2).
     */
    @Test
    void aBodyInsideTheToleranceIsStruckAndOneOutsideIsNot() {
        var world = new FakeWorld();
        var gap02 = body(world, 6, 0.2 + 0.3);
        var gap05 = body(world, 8, 0.5 + 0.3);
        var gap09 = body(world, 10, 0.9 + 0.3);
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.3)), FORWARD);
        world.advanceTicks(10);
        assertTrue(struck(gap02), "0.2 off: inside 0.3");
        assertEquals(100.0, gap05.health, "0.5 off: outside 0.3");
        assertEquals(100.0, gap09.health, "0.9 off: outside 0.3, though inside the old 1.0");
    }

    /** The same 0.9-off body IS struck at the old 1.0: the control that the row above measures the tolerance. */
    @Test
    void theOldToleranceWouldHaveStruckTheNinthOff() {
        var world = new FakeWorld();
        var gap09 = body(world, 10, 0.9 + 0.3);
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 1.0)), FORWARD);
        world.advanceTicks(10);
        assertTrue(struck(gap09), "0.9 off is inside 1.0");
    }

    /** A MARGIN hit the line cannot see (a block between them) is refused; the same body with sight clear is struck. */
    @Test
    void aToleranceDoesNotReachRoundAWall() {
        var blocked = new FakeWorld();
        var hidden = body(blocked, 6, 0.2 + 0.3);
        blocked.sightBlocked = (from, to) -> true;
        cast(blocked, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.3)), FORWARD);
        blocked.advanceTicks(10);
        assertEquals(100.0, hidden.health, "a margin hit behind a block is refused");

        var clear = new FakeWorld();
        var seen = body(clear, 6, 0.2 + 0.3);
        cast(clear, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.3)), FORWARD);
        clear.advanceTicks(10);
        assertTrue(struck(seen), "CONTROL: with sight clear, the same body is struck");
    }

    /** An EXACT hit (the line crosses the box itself) needs no sight check: the clipped line already reached it. */
    @Test
    void anExactHitIsNotSightChecked() {
        var world = new FakeWorld();
        var onLine = body(world, 6, 0);
        world.sightBlocked = (from, to) -> true;
        cast(world, new FakeWorld.Dummy(Vec3.ZERO), beam(new CastSpec.Ray(40, null, true, 0.3)), FORWARD);
        world.advanceTicks(10);
        assertTrue(struck(onLine), "the line crosses its box");
    }

    /** The geometry is the Knell mob's: BeamLine.meets on the box grown by the tolerance, nearest first. */
    @Test
    void theSweepIsTheMobBeamsGeometry() {
        var line = new BeamLine(Vec3.ZERO, new Vec3(1, 0, 0), 20);
        var a = new FakeWorld.Dummy(new Vec3(9, -1, 0.5));
        var b = new FakeWorld.Dummy(new Vec3(3, -1, 0));
        var bodies = List.of(new RaySweep.Body(pairOf(a), BeamBox.standing(a.position(), 0.6, 1.95)),
                new RaySweep.Body(pairOf(b), BeamBox.standing(b.position(), 0.6, 1.95)));
        var struck = RaySweep.struck(line, 0.3, bodies);
        assertEquals(List.of(b.id(), a.id()), struck.stream().map(s -> s.combatant().id()).toList(), "nearest first");
        assertTrue(struck.get(0).exact(), "b's box is on the line");
        assertTrue(!struck.get(1).exact(), "a is struck in its margin only");
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Ray(40, null, true, -0.1), "a negative tolerance");
    }

    private static io.github.butterflysmp.rpg.core.combat.Combatant pairOf(FakeWorld.Dummy d) {
        var world = new FakeWorld();
        world.entities.add(d);
        return world.combatantsNear(d.position(), 0.1).iterator().next();
    }

    // ===================================================================================== rulings 3 and 4: the volley

    private static CastSpec.Volley locked(boolean lockedAim, CastSpec.Volley.Warning warning) {
        return new CastSpec.Volley(24, 1, 1, new CastSpec.Ray(40, null, true, 0.3), lockedAim, warning);
    }

    /** The caster turns and moves during the windup: a LOCKED shot goes down the old line. */
    @Test
    void aLockedShotGoesDownTheLineCapturedAtTheCast() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(0, -1.62, 0));
        world.entities.add(caster);
        var a = body(world, 10, 0);       // down the line at the cast
        var b = body(world, 3, 10);       // where he turns to
        cast(world, caster, beam(locked(true, null)), FORWARD);
        world.advanceTicks(10);
        caster.moveTo(new Vec3(3, -1.62, 0));
        caster.facing = new Vec3(0, 0, 1);
        world.advanceTicks(30);
        assertTrue(struck(a), "the old line");
        assertEquals(100.0, b.health, "not where he turned");
    }

    /** CONTROL: the same, NOT locked: the shot re-aims at where he looks at the shot, as every volley did. */
    @Test
    void anUnlockedShotReAims() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(0, -1.62, 0));
        world.entities.add(caster);
        var a = body(world, 10, 0);
        var b = body(world, 3, 10);
        cast(world, caster, beam(locked(false, null)), FORWARD);
        world.advanceTicks(10);
        caster.moveTo(new Vec3(3, -1.62, 0));
        caster.facing = new Vec3(0, 0, 1);
        world.advanceTicks(30);
        assertTrue(struck(b), "re-aimed at where he looks");
        assertEquals(100.0, a.health, "not the old line");
    }

    /** The warning line: drawn on the cast frame and again on tick 20, along the LOCKED line, though he turned. */
    @Test
    void theWarningIsDrawnAlongTheLockedLineOnItsFlashTicks() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(0, -1.62, 0));
        world.entities.add(caster);
        cast(world, caster, beam(locked(true, new CastSpec.Volley.Warning("warn", List.of(0, 20)))), FORWARD);
        long atCast = world.presentedAlong.stream().filter(p -> p.visualId().equals("warn")).count();
        assertTrue(atCast >= 1, "drawn on the cast frame");
        caster.facing = new Vec3(0, 0, 1);
        world.advanceTicks(21);
        var warns = world.presentedAlong.stream().filter(p -> p.visualId().equals("warn")).toList();
        assertEquals(2 * atCast, warns.size(), "drawn again at tick 20, the same segments");
        for (var w : warns) {
            assertEquals(0.0, w.from().z(), 1e-9, "on the locked line");
            assertEquals(0.0, w.to().z(), 1e-9, "on the locked line");
        }
        assertEquals(1.0, warns.get(0).from().x(), 1e-9, "from the muzzle gap, as the ray's beam");
        assertEquals(40.0, warns.get((int) atCast - 1).to().x(), 1e-9, "to the ray's range: not clipped, as the mob's");
    }

    /** A warning line without locked aim is refused, by name. */
    @Test
    void aWarningWithoutLockedAimIsRefused() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> locked(false, new CastSpec.Volley.Warning("warn", List.of(0))));
        assertTrue(e.getMessage().contains("locked_aim"), e.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(24, 1, 1, new CastSpec.Ray(40, null, true, 0.3), true,
                        new CastSpec.Volley.Warning("warn", List.of(24))), "a flash at the shot's own tick");
    }
}
