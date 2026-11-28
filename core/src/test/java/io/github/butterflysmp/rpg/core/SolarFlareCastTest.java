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
 * THREE FIREBALLS ONE AFTER ANOTHER, EACH BURSTING AT ONCE, CAUSED (Solar Flare as Ben changed it on 2026-11-28, the
 * seat's N1 to N3 and N6). Each row stages the condition it names and reads the outcome off each dummy's health, the
 * lines and the bodies, never off an arm's existence.
 *
 * <p>The shape: a salvo of 3 shots 10 ticks apart, speed 2.0, gravity 0, 16 ticks (32.0 blocks), each bursting into ONE
 * INSTANT front of radius 6 (travel 0), landing {@code DAMAGE} (100 here, so a body's health counts its bursts: 1000 is
 * none, 700 three). The caster stands at the origin facing EAST with his eye 1.62 up (the dummy's own); every body stands at
 * that height, so a shot flies level with them and its distance is measured from his eye. The clock starts at 0: the cast is tick 0, and it runs in the GLOBAL phase on
 * the server's phases ({@link FakeWorld#serverPhases}, X6 (a)), so the shots land on 0, 10, 20 only if the salvo is
 * booked exactly.
 */
class SolarFlareCastTest {

    private static final double DAMAGE = 100;
    static final CastSpec.Pulses BURST = new CastSpec.Pulses(1, 1, 6.0, 0, null, "boom");
    static final CastSpec.Projectile FLARE = new CastSpec.Projectile(2.0, 0.0, 16, "trail", "fire_charge",
            null, null, null, 0, BURST, 2.5, new CastSpec.Projectile.Salvo(3, 10));

    private static AbilityDefinition ability(ResourceCost cost) {
        return new AbilityDefinition("solar_flare", "Solar Flare", "fire", 4200, cost, FLARE,
                List.of(new EffectSpec.Damage(DAMAGE, "fire")));
    }

    private static AbilityService cast(FakeWorld world, FakeWorld.Dummy caster, ResourcePool pool) {
        AbilityDefinition def = ability(pool == null ? ResourceCost.FREE : new ResourceCost("mana", 80));
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(world::now),
                pool == null ? new ResourcePool(() -> 0L, 100, 1) : pool);
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), new Aim(caster.position().add(new Vec3(0, caster.eyeHeight, 0)), caster.facing), Set.of(def.id())));
        new CastExecutor(world).execute(success);
        return service;
    }

    private static FakeWorld world() {
        var world = new FakeWorld();
        world.serverPhases = true;
        return world;
    }

    private static FakeWorld.Dummy body(FakeWorld world, double x, double z) {
        var d = new FakeWorld.Dummy(new Vec3(x, 1.62, z));
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
                .map(p -> String.format(java.util.Locale.ROOT, "n=%d tick=%d dist=%.3f sent=%.1f", p.line().n(), p.tick(),
                        p.line().dist(), p.line().sent()))
                .toList();
    }

    // ============================================================== N1, N2, N6: one after another, 32 blocks each

    /**
     * NO BODY IN THE WAY: three shot lines at ticks 0, 10, 20 (N6), each due east (yaw -90), and each burst at range
     * 32.0 blocks from the eye point it left, on its own launch tick + 15 (the 16th step, the launch tick's inline step
     * the first): 15, 25, 35. Forces red: the salvo booked from the cast's phase (shot 2 at 9), a speed or lifetime off
     * by one step, a fan.
     */
    @Test
    void threeShotsTenTicksApartEachBurstingThirtyTwoBlocksOutOnItsLaunchTickPlusFifteen() {
        var world = world();
        var him = caster(world);
        cast(world, him, null);
        world.advanceTicks(60);
        assertEquals(List.of("SOLAR_FLARE shot n=1 dir=-90.000 -0.000 tick=0", "SOLAR_FLARE shot n=2 dir=-90.000 -0.000 tick=10",
                "SOLAR_FLARE shot n=3 dir=-90.000 -0.000 tick=20"), texts(world, PulseLine.Event.SHOT));
        assertEquals(List.of("SOLAR_FLARE burst n=1 cause=range origin=32.000 1.620 0.000 tick=15",
                "SOLAR_FLARE burst n=2 cause=range origin=32.000 1.620 0.000 tick=25",
                "SOLAR_FLARE burst n=3 cause=range origin=32.000 1.620 0.000 tick=35"), texts(world, PulseLine.Event.BURST));
        assertEquals(0, world.pulseRuns().size(), "three bursts land the run");
    }

    /**
     * EACH SHOT FOLLOWS HIS AIM AT THE MOMENT IT LEAVES (Ben: "yes"): he faces east for shot 1; at tick 5 he is moved 2
     * east and turned SOUTH, so shot 2 leaves from (2, 0, 0) due south (yaw 0, printed -0.000, EXECUTED); at tick 15 he is turned NORTH, so shot 3
     * leaves north (yaw 180, printed -180.000, EXECUTED). Three directions, three burst origins, each 32.0 from the eye point it left. Forces red:
     * the cast's aim reused for every shot (the locked aim).
     */
    @Test
    void eachShotLeavesDownHisAimAndFromHisEyeOnItsOwnTick() {
        var world = world();
        var him = caster(world);
        cast(world, him, null);
        world.advanceTicks(5);
        him.moveTo(new Vec3(2, 0, 0));
        him.facing = new Vec3(0, 0, 1);
        world.advanceTicks(10);
        him.facing = new Vec3(0, 0, -1);
        world.advanceTicks(60);
        assertEquals(List.of("SOLAR_FLARE shot n=1 dir=-90.000 -0.000 tick=0", "SOLAR_FLARE shot n=2 dir=-0.000 -0.000 tick=10",
                "SOLAR_FLARE shot n=3 dir=-180.000 -0.000 tick=20"), texts(world, PulseLine.Event.SHOT));
        assertEquals(List.of("SOLAR_FLARE burst n=1 cause=range origin=32.000 1.620 0.000 tick=15",
                "SOLAR_FLARE burst n=2 cause=range origin=2.000 1.620 32.000 tick=25",
                "SOLAR_FLARE burst n=3 cause=range origin=2.000 1.620 -32.000 tick=35"), texts(world, PulseLine.Event.BURST));
    }

    /**
     * N2: A 2-BLOCK STEP CANNOT PASS THROUGH A THIN WALL OR A BODY BETWEEN TWO STEP ENDS. Each step traces its whole
     * segment (the fake's {@code castRay}, as the server's {@code World#rayTrace} over the step's length). A wall at x = 3
     * (between the step ends 2 and 4) stops shot 1 on its second step, tick 1, at 3.0; a body at x = 5 (between 4 and 6)
     * stops it on its third step, tick 2, and the burst is at the body.
     */
    @Test
    void aTwoBlockStepIsSweptSoAThinWallOrABodyBetweenStepEndsStopsTheShot() {
        var walled = world();
        walled.wallX = 3;
        cast(walled, caster(walled), null);
        walled.advanceTicks(1);
        assertEquals("SOLAR_FLARE burst n=1 cause=block origin=3.000 1.620 0.000 tick=1",
                texts(walled, PulseLine.Event.BURST).get(0));

        var bodied = world();
        var him = caster(bodied);
        body(bodied, 5, 0);
        cast(bodied, him, null);
        bodied.advanceTicks(2);
        assertEquals("SOLAR_FLARE burst n=1 cause=mob origin=5.000 1.620 0.000 tick=2",
                texts(bodied, PulseLine.Event.BURST).get(0));
    }

    // ============================================================== N3: the burst is instant; three in one place

    /**
     * THREE BURSTS IN ONE PLACE, EACH INSTANT (N3; Ben's earlier "Yes it takes all the damage"): a wall at x = 10 stops
     * every shot at (10, 0, 0) (the fifth step, 8 to 10: its launch tick + 4), so the bursts are at 4, 14, 24. A body 5.9
     * beyond the wall is struck by each burst ON ITS OWN TICK (no travel), 100 three times; one 6.1 beyond is never
     * struck. Forces red: a travelling front (the hits late), one struck set for the salvo (one hit only).
     */
    @Test
    void threeBurstsInOnePlaceStrikeABodyWithinSixOnEachBurstsOwnTickAndNeverBeyond() {
        var world = world();
        world.wallX = 10;
        var him = caster(world);
        var in = body(world, 15.9, 0);
        var out = body(world, 16.1, 0);
        cast(world, him, null);
        world.advanceTicks(60);
        assertEquals(List.of("n=1 tick=4 dist=5.900 sent=100.0", "n=2 tick=14 dist=5.900 sent=100.0",
                "n=3 tick=24 dist=5.900 sent=100.0"), hitsOn(world, in));
        assertEquals(700.0, in.health, 1e-9);
        assertEquals(List.of(), hitsOn(world, out));
        assertEquals(1000.0, out.health);
        assertEquals(1000.0, him.health, "never the caster");
    }

    /**
     * ONE CRIT, AT THE CAST (N1): his crit multiplier is 2.0 when he casts and 1.0 from tick 1 on; every one of the three
     * bursts still strikes for 200. Forces red: a Caster rebuilt per shot (the volley's re-read).
     */
    @Test
    void theCritRolledAtTheCastHoldsForEveryShot() {
        var world = world();
        world.wallX = 10;
        var him = caster(world);
        him.critMultiplier = 2.0;
        var in = body(world, 12, 0);
        cast(world, him, null);
        world.advanceTicks(1);
        him.critMultiplier = 1.0;
        world.advanceTicks(60);
        assertEquals(List.of("n=1 tick=4 dist=2.000 sent=200.0", "n=2 tick=14 dist=2.000 sent=200.0",
                "n=3 tick=24 dist=2.000 sent=200.0"), hitsOn(world, in));
    }

    /**
     * THE COST AND THE COOLDOWN, ONCE (N1): 80 of his 100 mana at the cast, nothing more at shots 2 and 3; and a second
     * press at tick 30 is refused on its cooldown. Forces red: a salvo that re-casts per shot.
     */
    @Test
    void manaAndTheCooldownAreTakenOnceAtTheCast() {
        var world = world();
        var him = caster(world);
        var pool = new ResourcePool(world::now, 100, 0);
        var service = cast(world, him, pool);
        assertEquals(20.0, pool.current(him.id(), "mana"), 1e-9);
        world.advanceTicks(30);
        assertEquals(20.0, pool.current(him.id(), "mana"), 1e-9, "shots 2 and 3 cost nothing");
        assertEquals(3, texts(world, PulseLine.Event.SHOT).size());
        assertInstanceOf(AbilityService.CastResult.OnCooldown.class, service.cast(him.snapshot(), "solar_flare",
                new Aim(him.position().add(new Vec3(0, him.eyeHeight, 0)), him.facing), Set.of("solar_flare")));
    }

    // ============================================================== F7: summons and players

    /** A summon and another player inside a burst are never struck; a summon does not stop a shot (Q3). */
    @Test
    void aSummonOrAPlayerInsideABurstIsNeverStruck_andASummonDoesNotStopAShot() {
        var world = world();
        var him = caster(world);
        var wolf = body(world, 8, 0);
        wolf.summon = true;
        var other = body(world, 30, 2);
        other.player = true;
        cast(world, him, null);
        world.advanceTicks(60);
        assertEquals("SOLAR_FLARE burst n=1 cause=range origin=32.000 1.620 0.000 tick=15",
                texts(world, PulseLine.Event.BURST).get(0), "the wolf on the line did not stop shot 1");
        assertEquals(List.of(), hitsOn(world, wolf));
        assertEquals(List.of(), hitsOn(world, other));
        assertEquals(1000.0, wolf.health);
        assertEquals(1000.0, other.health);
    }

    // ============================================================== F8 with the salvo: the cancel, and the body

    /**
     * ENDED BETWEEN SHOTS 2 AND 3 (tick 12, as {@code PulseRemovals} does: the run taken out, its pending link cancelled):
     * remaining 3 (none has burst), shot 3 never launches, shots 1 and 2 stop in flight with no burst, and no body is
     * left. Forces red: the next shot's link not held by the run; a flight that never asks its run.
     */
    @Test
    void endingTheRunBetweenShotsLaunchesNoMoreAndLeavesNoBody() {
        var world = world();
        var him = caster(world);
        cast(world, him, null);
        world.advanceTicks(12);
        assertEquals(2, world.markers.size(), "two bodies in flight");
        var ended = world.pulseRuns().end(him.id()).orElseThrow();
        world.cancelScheduled(ended.pendingHandle());
        assertEquals(3, ended.remaining(), "none had burst");
        world.advanceTicks(60);
        assertEquals(2, texts(world, PulseLine.Event.SHOT).size(), "shot 3 never launched");
        assertEquals(List.of(), texts(world, PulseLine.Event.BURST));
        assertEquals(0, world.markers.size(), "every body removed");
    }

    /** Ended after shot 1 burst (tick 17): remaining 2. */
    @Test
    void endedAfterTheFirstBurstTwoRemain() {
        var world = world();
        var him = caster(world);
        cast(world, him, null);
        world.advanceTicks(17);
        var ended = world.pulseRuns().end(him.id()).orElseThrow();
        world.cancelScheduled(ended.pendingHandle());
        assertEquals(2, ended.remaining());
        world.advanceTicks(60);
        assertEquals(1, texts(world, PulseLine.Event.BURST).size());
        assertEquals(2, texts(world, PulseLine.Event.SHOT).size());
    }

    /** F9: each shot's body is the scaled item, spawned at his eye on its own launch, and removed where it bursts. */
    @Test
    void eachShotsBodyIsTheScaledItemAndItGoesWithItsBurst() {
        var world = world();
        var him = caster(world);
        cast(world, him, null);
        assertEquals(1, world.displayScalesEver.size(), "one body on the cast's tick");
        world.advanceTicks(60);
        assertEquals(3, world.displayScalesEver.size());
        assertTrue(world.displayScalesEver.values().stream().allMatch(sc -> sc == 2.5));
        assertTrue(world.displayScalesEver.keySet().stream().allMatch(id -> "fire_charge".equals(world.markerItemsEver.get(id))));
        assertEquals(0, world.markers.size());
    }

    // ============================================================== the record's rules

    @Test
    void aBurstIsOneFront_neverFused_onlyAnItemIsScaled_andASalvoNeedsABurstAndNoSpread() {
        var two = new CastSpec.Pulses(2, 60, 6.0, 15, null, null);
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 0, two, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 40, BURST, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, null, null,
                "fireball", null, 0, BURST, 2.5));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 0, BURST, Double.NaN));
        var salvo = new CastSpec.Projectile.Salvo(3, 10);
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, null, 0, null, 0.0, salvo), "a salvo needs a burst");
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(1, 0, 16, null, "fire_charge", null,
                null, new CastSpec.Spread(3, 30), 0, BURST, 0.0, salvo), "a salvo cannot also spread");
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile.Salvo(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile.Salvo(3, 0));
        assertEquals(null, new CastSpec.Projectile(1, 0, 16).salvo(), "every older projectile: no salvo");
    }
}
