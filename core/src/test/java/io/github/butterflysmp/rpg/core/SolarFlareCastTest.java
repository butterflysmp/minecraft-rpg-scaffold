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
import io.github.butterflysmp.rpg.core.combat.PulseLine;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A FAN THAT BURSTS, CAUSED (Solar Flare, the seat's F1 to F8 and F13, 2026-11-27). Each row stages the condition it names
 * and reads the outcome off each dummy's health, the lines and the bodies, never off an arm's existence.
 *
 * <p>Solar Flare's shape: three shots at 30 degrees, speed 15/16, gravity 0, 16 ticks, each bursting into ONE pulse front
 * of radius 6 over 15 ticks, landing {@code DAMAGE} (100 here, so a body's health counts its rings: 1000 is none, 900
 * one, 700 three). The caster stands at the origin facing EAST; his aim's origin is his position (the fake's eye is his
 * feet), so a shot's distance is measured from there. The clock starts at 0: the cast is tick 0.
 */
class SolarFlareCastTest {

    private static final double DAMAGE = 100;
    static final CastSpec.Pulses BURST = new CastSpec.Pulses(1, 1, 6.0, 15, "ring", "boom");
    static final CastSpec.Projectile FLARE = new CastSpec.Projectile(15.0 / 16.0, 0.0, 16, "trail", "fire_charge",
            null, null, new CastSpec.Spread(3, 30), 0, BURST, 2.5);

    private static AbilityDefinition ability(CastSpec.Projectile spec) {
        return new AbilityDefinition("solar_flare", "Solar Flare", "fire", 0, ResourceCost.FREE, spec,
                List.of(new EffectSpec.Damage(DAMAGE, "fire")));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster) {
        AbilityDefinition def = ability(FLARE);
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), new Aim(caster.position(), new Vec3(1, 0, 0)), Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    private static FakeWorld.Dummy body(FakeWorld world, double x, double z) {
        var d = new FakeWorld.Dummy(new Vec3(x, 0, z));
        d.health = 1000;
        world.entities.add(d);
        return d;
    }

    private static FakeWorld.Dummy caster(FakeWorld world) {
        var c = new FakeWorld.Dummy(Vec3.ZERO);
        c.player = true;
        c.health = 1000;
        world.entities.add(c);
        return c;
    }

    private static List<String> texts(FakeWorld world, PulseLine.Event event) {
        return world.pulseLines.stream().filter(p -> p.line().event() == event)
                .map(p -> p.text().replaceAll(" caster=\\S+", "").replaceAll(" target=\\S+ \\S+", "")).toList();
    }

    private static List<String> hitsOn(FakeWorld world, FakeWorld.Dummy d) {
        return world.pulseLines.stream()
                .filter(p -> p.line().event() == PulseLine.Event.HIT && p.line().target().equals(d.id()))
                .map(p -> String.format(java.util.Locale.ROOT, "n=%d tick=%d dist=%.3f", p.line().n(), p.tick(), p.line().dist()))
                .toList();
    }

    // ====================================================================== F1: the fan, the flight, the range

    /**
     * SF2, IN THE FAKE: no body in the flight paths. Three shots, LEFT TO RIGHT at yaw -120, -90, -60 (east is -90), each
     * bursting at its range on tick 15 (the 16th step: tick 0 is the first, on the cast's frame), 15.0 blocks out along
     * its direction. A body 4 past the centre origin is struck once by ring 2, at 15 + 10 (6 x 10 / 15 = 4); one 7 past
     * it never.
     *
     * <p>Forces red: the spread's own order for n; a lifetime counted one tick long or short; a speed that is not 15/16;
     * a ring timed from the cast rather than the burst; the outside body admitted.
     */
    @Test
    void threeShotsLeftToRightEachBurstingAtFifteenBlocksOnTickFifteen() {
        var world = new FakeWorld();
        var him = caster(world);
        var past4 = body(world, 19, 0);
        var past7 = body(world, 22, 0);

        cast(world, him);
        world.advanceTicks(60);

        assertEquals(List.of("SOLAR_FLARE shot n=1 dir=-120.000 -0.000 tick=0", "SOLAR_FLARE shot n=2 dir=-90.000 -0.000 tick=0",
                "SOLAR_FLARE shot n=3 dir=-60.000 -0.000 tick=0"), texts(world, PulseLine.Event.SHOT));
        double c = 15 * Math.cos(Math.toRadians(30)), s = 15 * Math.sin(Math.toRadians(30));
        assertEquals(List.of(
                String.format(java.util.Locale.ROOT, "SOLAR_FLARE burst n=1 cause=range origin=%.3f 0.000 %.3f tick=15", c, -s),
                "SOLAR_FLARE burst n=2 cause=range origin=15.000 0.000 0.000 tick=15",
                String.format(java.util.Locale.ROOT, "SOLAR_FLARE burst n=3 cause=range origin=%.3f 0.000 %.3f tick=15", c, s)),
                texts(world, PulseLine.Event.BURST));
        assertEquals(List.of("n=2 tick=25 dist=4.000"), hitsOn(world, past4));
        assertEquals(900.0, past4.health);
        assertEquals(List.of(), hitsOn(world, past7));
        assertEquals(1000.0, past7.health);
        assertEquals(1000.0, him.health, "never the caster");
    }

    // ====================================================================== F2, F3, F5: what stops it, and where it bursts

    /**
     * SF3: a body on the centre line 8 out stops shot 2 on the step that reaches 8 (7.5 to 8.4375, tick 8). The burst's
     * origin is THAT BODY's position, so ring 2 strikes it at distance 0 on tick 8 and nothing else from shot 2 does
     * (no direct hit is added: 900, one ring). The trace meets it at 7.4 (its surface), so this row tells the body's
     * position from the hit point. A body 5 beyond it, 13 out, is struck by the same ring at 8 + 13; the side
     * shots still burst at range.
     */
    @Test
    void aBodyStopsItsShotAndTheRingStrikesItAtDistanceZeroOnce() {
        var world = new FakeWorld();
        world.surfaceHits = true;   // the trace meets it at 7.4, not at its position: only this can tell F3's origin apart
        var him = caster(world);
        var stopper = body(world, 8, 0);
        var beyond = body(world, 13, 0);

        cast(world, him);
        world.advanceTicks(60);

        List<String> bursts = texts(world, PulseLine.Event.BURST);
        assertEquals("SOLAR_FLARE burst n=2 cause=mob origin=8.000 0.000 0.000 tick=8", bursts.get(0));
        assertTrue(bursts.contains(bursts.stream().filter(b -> b.startsWith("SOLAR_FLARE burst n=1 cause=range")).findFirst().orElseThrow()));
        assertTrue(bursts.stream().anyMatch(b -> b.startsWith("SOLAR_FLARE burst n=3 cause=range") && b.endsWith("tick=15")));
        assertEquals(List.of("n=2 tick=8 dist=0.000"), hitsOn(world, stopper));
        assertEquals(900.0, stopper.health, "one ring, no direct hit");
        assertEquals(List.of("n=2 tick=21 dist=5.000"), hitsOn(world, beyond));
    }

    /**
     * SF4 and SF5's three-way: a wall at x = 3 stops all three shots (the centre at 3, tick 3; each side 3 / cos 30 =
     * 3.464 out, tick 3). The bursts are where the trace met it. A body at 4 behind the wall is within 6 of all three
     * origins (1 from the centre, 2 from each side) and takes ALL THREE rings; the caster, 3 from the centre origin, none.
     */
    @Test
    void aWallStopsEveryShotAndABodyInsideThreeRingsTakesEachOne() {
        var world = new FakeWorld();
        world.wallX = 3;
        var him = caster(world);
        var behind = body(world, 4, 0);

        cast(world, him);
        world.advanceTicks(60);

        double side = 3 * Math.tan(Math.toRadians(30));
        assertEquals(List.of(
                String.format(java.util.Locale.ROOT, "SOLAR_FLARE burst n=1 cause=block origin=3.000 0.000 %.3f tick=3", -side),
                "SOLAR_FLARE burst n=2 cause=block origin=3.000 0.000 0.000 tick=3",
                String.format(java.util.Locale.ROOT, "SOLAR_FLARE burst n=3 cause=block origin=3.000 0.000 %.3f tick=3", side)),
                texts(world, PulseLine.Event.BURST));
        assertEquals(List.of("n=2 tick=6 dist=1.000", "n=1 tick=8 dist=2.000", "n=3 tick=8 dist=2.000"), hitsOn(world, behind));
        assertEquals(700.0, behind.health, "three rings, three hits (F5)");
        assertEquals(1000.0, him.health, "never the caster");
    }

    /** SF5's two-way at range: a body 4.123 from burst 2 and 3.643 from burst 3 takes both, each on its own tick (executed). */
    @Test
    void aBodyInsideTwoRingsAtRangeTakesBoth() {
        var world = new FakeWorld();
        var him = caster(world);
        var between = body(world, 14, 4);

        cast(world, him);
        world.advanceTicks(60);

        assertEquals(List.of("n=3 tick=25 dist=3.643", "n=2 tick=26 dist=4.123"), hitsOn(world, between));
        assertEquals(800.0, between.health);
    }

    // ====================================================================== F7: summons and players

    /** SF6: a summon and another player inside a ring are never struck; a summon does not stop a shot (Q3). */
    @Test
    void aSummonOrAPlayerInsideARingIsNeverStruck_andASummonDoesNotStopAShot() {
        var world = new FakeWorld();
        var him = caster(world);
        var wolf = body(world, 8, 0);
        wolf.summon = true;
        var other = body(world, 16, 2);
        other.player = true;

        cast(world, him);
        world.advanceTicks(60);

        assertTrue(texts(world, PulseLine.Event.BURST).contains("SOLAR_FLARE burst n=2 cause=range origin=15.000 0.000 0.000 tick=15"),
                "the wolf on the centre line did not stop shot 2");
        assertEquals(List.of(), hitsOn(world, wolf));
        assertEquals(List.of(), hitsOn(world, other));
        assertEquals(1000.0, wolf.health);
        assertEquals(1000.0, other.health);
    }

    // ====================================================================== F8: cancel, and the body

    /** SF8: the run ended at tick 5 (as PulseRemovals does): no burst follows, and no body is left. */
    @Test
    void endingTheRunInFlightEndsEveryShotWithItsBody() {
        var world = new FakeWorld();
        var him = caster(world);
        cast(world, him);
        world.advanceTicks(5);
        assertEquals(3, world.markers.size(), "three bodies in flight");
        var ended = world.pulseRuns().end(him.id()).orElseThrow();
        assertEquals(3, ended.remaining(), "none had burst");
        world.advanceTicks(60);
        assertEquals(List.of(), texts(world, PulseLine.Event.BURST));
        assertEquals(0, world.markers.size(), "every body removed");
    }

    /** F9: each shot's body is the scaled item, spawned at his eye on the cast's frame, and removed where it bursts. */
    @Test
    void eachShotsBodyIsTheScaledItemAndItGoesWithItsBurst() {
        var world = new FakeWorld();
        var him = caster(world);
        cast(world, him);
        assertEquals(3, world.displayScalesEver.size());
        assertTrue(world.displayScalesEver.values().stream().allMatch(sc -> sc == 2.5));
        assertTrue(world.displayScalesEver.keySet().stream().allMatch(id -> "fire_charge".equals(world.markerItemsEver.get(id))));
        world.advanceTicks(60);
        assertEquals(0, world.markers.size());
        assertEquals(0, world.pulseRuns().size(), "three bursts land the run");
    }

    // ====================================================================== the record's rules

    @Test
    void aBurstIsOneFront_neverFused_andOnlyAnItemIsScaled() {
        var two = new CastSpec.Pulses(2, 60, 6.0, 15, null, null);
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 0, two, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 40, BURST, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, null, null,
                "fireball", null, 0, BURST, 2.5));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 0, BURST, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 0, BURST, -1));
        assertEquals(0.0, new CastSpec.Projectile(1, 0, 16).bodyScale(), "every older projectile: unscaled, no burst");
    }
}
