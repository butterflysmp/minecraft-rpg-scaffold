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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CRYSTAL SHARD STAFF'S HOMING, CAUSED (the seat's brief of 2026-11-21, PR A): a shard fired at the staff's own
 * speed and homing values finds a mob standing OFF the aim line, where the same shard without homing misses.
 *
 * <p><b>THE NUMBERS ARE THE STAFF'S, CARRIED HERE BY HAND</b> because core cannot read YAML.
 * {@code CrystalShardStaffContentTest.theFlightIsTheSeatsPlaceholders} pins the shipped file to the same six values;
 * change one and change both. Speed 3.0, gravity 0 and lifetime 7 are SEAT PLACEHOLDERS; search radius 18 is the old
 * repo's {@code CRYSTAL_SHARD_STAFF_HOMING_RADIUS} (cfde822); lerp 0.45 is a SEAT PLACEHOLDER (2026-11-21), raised from
 * the old {@code CRYSTAL_SHARD_STAFF_HOMING_TURN} 0.18, which caught only 3.0 degrees at 10 blocks at this speed;
 * activation 0 is the
 * seat's choice for a field the old weapon had no number for (it steered from its first tick).
 *
 * <p><b>THE GEOMETRY IS THE FAKE'S, AND IT IS NOT A ZOMBIE.</b> {@code FakeWorld.castRay} hits a combatant whose FEET
 * point lies within {@code hitRadius} of the traced segment, and the aim origin here is level with the mob's feet. The
 * radius is set to 0.3, half a zombie's width. On a server the shard leaves the eye (1.62 up), steers at the mob's FEET
 * ({@code ProjectileFlight.steer} uses {@code position()}), and hits a 0.6 x 1.95 box (INFERRED: a target no harder to
 * catch sideways than this one). So the reported angle is a property of the flight in this fixture, for the gate to
 * stage inside of with margin, not a server measurement.
 */
class CrystalShardCatchAngleTest {

    static final double SPEED = 3.0;
    static final double GRAVITY = 0.0;
    static final int LIFETIME = 7;
    static final double LERP = 0.45;
    /** The old repo's turn, which this staff carried until the seat's follow-up of 2026-11-21. */
    static final double OLD_LERP = 0.18;
    static final double ACTIVATION = 0.0;
    static final double SEARCH_RADIUS = 18.0;

    static final double DISTANCE = 10.0;
    static final double HALF_WIDTH = 0.3;

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** One cast at a mob {@code degrees} off the aim line, {@link #DISTANCE} out, level. Did it take the 16? */
    static boolean hits(double degrees, boolean homing) {
        var world = new FakeWorld();
        world.hitRadius = HALF_WIDTH;
        double r = Math.toRadians(degrees);
        var mob = new FakeWorld.Dummy(new Vec3(DISTANCE * Math.cos(r), 0, DISTANCE * Math.sin(r)));
        world.entities.add(mob);
        var def = new AbilityDefinition("crystal_shard", "Crystal Shard", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Projectile(SPEED, GRAVITY, LIFETIME, null, null,
                        homing ? new CastSpec.Homing(LERP, ACTIVATION, SEARCH_RADIUS) : null),
                List.of(new EffectSpec.Damage(16, "kinetic")));
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(new FakeWorld.Dummy(Vec3.ZERO).snapshot(), def.id(), FORWARD, Set.of(def.id())));
        new CastExecutor(world).execute(success);
        world.advanceTicks(LIFETIME + 5);
        return mob.health < 100;
    }

    /**
     * The cause: 5 degrees off (GATE-crystal-shard-staff.md CS3's angle), homing hits and the same shard without
     * homing misses.
     *
     * <p><b>IT WAS 2.5 UNTIL THE SEAT'S FOLLOW-UP OF 2026-11-21 RAISED THE TURN TO 0.45, AND 2.5 WAS ITSELF A
     * FINDING.</b> This row was first written at 10 degrees and went red: at speed 3.0
     * the shard is 10 blocks out by its fourth tick, and a turn of 0.18 a tick cannot bend it that far in time. The
     * band homing adds at 10 blocks is reported by the row below, and it is narrow.
     */
    @Test
    void homingTurnsAMissIntoAHit() {
        assertTrue(hits(0, false), "CONTROL: dead ahead, even an unsteered shard hits");
        assertTrue(!hits(5, false), "5 degrees off, an unsteered shard passes "
                + String.format(Locale.ROOT, "%.2f", DISTANCE * Math.sin(Math.toRadians(5))) + " blocks wide");
        assertTrue(hits(5, true), "5 degrees off, the homing shard finds it");
    }

    /** The widest contiguous hitting angle, 0.5-degree steps, for any speed, lifetime and lerp. */
    static double widest(double speed, int lifetime, double lerp) {
        double widest = -1;
        for (double d = 0; d <= 90; d += 0.5) {
            var world = new FakeWorld();
            world.hitRadius = HALF_WIDTH;
            double r = Math.toRadians(d);
            var mob = new FakeWorld.Dummy(new Vec3(DISTANCE * Math.cos(r), 0, DISTANCE * Math.sin(r)));
            world.entities.add(mob);
            var def = new AbilityDefinition("probe", "Probe", "kinetic", 0, ResourceCost.FREE,
                    new CastSpec.Projectile(speed, GRAVITY, lifetime, null, null,
                            new CastSpec.Homing(lerp, ACTIVATION, SEARCH_RADIUS)),
                    List.of(new EffectSpec.Damage(16, "kinetic")));
            var registry = new AbilityRegistry();
            registry.register(def);
            var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                    new ResourcePool(() -> 0L, 100, 1));
            var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                    service.cast(new FakeWorld.Dummy(Vec3.ZERO).snapshot(), def.id(), FORWARD, Set.of(def.id())));
            new CastExecutor(world).execute(success);
            world.advanceTicks(lifetime + 5);
            if (mob.health < 100) widest = d; else break;
        }
        return widest;
    }

    /**
     * FOR THE SEAT, REPORTED, NOT JUDGED: what "fast" costs the homing. The old weapon (speed 1.1, about 18 ticks to
     * its 20-block range) against this one, and the lerp this speed would need for a few wider catches. Asserted only
     * that the old, slow shard catches wider than the fast one, so the comparison cannot be blind.
     */
    @Test
    void reportsWhatSpeedCostsTheHoming() {
        double now = widest(SPEED, LIFETIME, LERP);
        double fastOldTurn = widest(SPEED, LIFETIME, OLD_LERP);
        double old = widest(1.1, 19, OLD_LERP);
        StringBuilder lerps = new StringBuilder();
        for (double l : new double[] {0.18, 0.3, 0.45, 0.65, 1.0}) {
            lerps.append(String.format(Locale.ROOT, " lerp %.2f -> %.1f deg;", l, widest(SPEED, LIFETIME, l)));
        }
        System.out.printf(Locale.ROOT, "CATCH-COMPARE at %.0f blocks: speed 3.0 lerp %.2f -> %.1f deg; speed 3.0 old"
                + " lerp 0.18 -> %.1f deg; old speed 1.1 lerp 0.18 -> %.1f deg; at speed 3.0:%s%n", DISTANCE, LERP, now,
                fastOldTurn, old, lerps);
        assertTrue(old > fastOldTurn, "the slow shard has more ticks to turn: " + old + " vs " + fastOldTurn);
        assertTrue(now > fastOldTurn, "the raised turn widens the catch: " + now + " vs " + fastOldTurn);
    }

    /**
     * THE FIGURE FOR THE GATE, REPORTED, NOT JUDGED: the widest angle, in 0.5-degree steps, at which the shard still
     * hits a mob 10 blocks out, and the first angle beyond it that misses. Asserted only so that it cannot be blind:
     * every angle up to it hits (no gap), and it is wider than an unsteered shard's own catch.
     */
    @Test
    void reportsTheWidestAngleThatStillHitsAtTenBlocks() {
        double widest = -1;
        for (double d = 0; d <= 90; d += 0.5) {
            if (hits(d, true)) widest = d; else break;
        }
        double unsteered = -1;
        for (double d = 0; d <= 90; d += 0.5) {
            if (hits(d, false)) unsteered = d; else break;
        }
        // Past the edge: any angle that hits again would make "the widest" a band, not an edge. Reported, never hidden.
        List<Double> beyond = new java.util.ArrayList<>();
        for (double d = widest + 0.5; d <= 90; d += 0.5) if (hits(d, true)) beyond.add(d);
        System.out.printf(Locale.ROOT, "CATCH-ANGLE crystal_shard_staff at %.0f blocks: hits to %.1f deg, misses at %.1f;"
                + " unsteered hits to %.1f deg; hits beyond the edge: %s%n", DISTANCE, widest, widest + 0.5, unsteered,
                beyond);
        assertTrue(widest > unsteered, "homing must widen the catch: " + widest + " vs " + unsteered);
        assertEquals(List.of(), beyond, "the catch is one band from the aim line out; a hit past its edge is a second band");
    }
}
