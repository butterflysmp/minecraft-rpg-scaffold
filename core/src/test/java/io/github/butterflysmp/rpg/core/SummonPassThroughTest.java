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
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Q3 (the seat, 2026-11-24; SEAT'S CALL ON FEEL, OPEN FOR BEN): A PLAYER'S RAY AND PROJECTILE PASS THROUGH SUMMONS. A fake
 * summon stands ON THE LINE between him and a mob. The mob being hit AT FULL VALUE is the discriminator: had the shot
 * stopped on the summon, the mob would be untouched. The summon is untouched either way.
 */
class SummonPassThroughTest {

    private static final double EYE = 1.62;
    private static final double HIT = 10.0;   // at score 100 with no other term: the full value

    private record Stage(FakeWorld world, FakeWorld.Dummy him, FakeWorld.Dummy summon, FakeWorld.Dummy mob) {}

    private static Stage stage() {
        var world = new FakeWorld();
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        var summon = new FakeWorld.Dummy(new Vec3(4, EYE, 0));   // on the line, nearer
        summon.summon = true;
        var mob = new FakeWorld.Dummy(new Vec3(8, EYE, 0));      // behind it, on the same line
        world.entities.addAll(List.of(him, summon, mob));
        return new Stage(world, him, summon, mob);
    }

    private static void cast(Stage s, CastSpec spec) {
        var def = new AbilityDefinition("pass_q3", "Test", "kinetic", 0, ResourceCost.FREE, spec,
                List.of(new EffectSpec.Damage(HIT, "kinetic")));
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(s.him().snapshot(), def.id(), new Aim(new Vec3(0, EYE, 0), new Vec3(1, 0, 0)), Set.of(def.id())));
        new CastExecutor(s.world()).execute(success);
        s.world().advanceTicks(200);
    }

    private static void assertPassedThrough(String what, Stage s, int shots) {
        assertEquals(100.0, s.summon().health, 1e-9, what + ": the summon is untouched");
        assertEquals(0, s.summon().damageCalls, what + ": and was not hit for zero");
        assertEquals(100.0 - HIT * shots, s.mob().health, 1e-9, what + ": the mob behind it, at full value");
    }

    @Test
    void theRay() {
        Stage s = stage();
        cast(s, new CastSpec.Ray(30));
        assertPassedThrough("RAY", s, 1);
    }

    @Test
    void theVolleyShot() {
        Stage s = stage();
        cast(s, new CastSpec.Volley(0, 3, 4, new CastSpec.Ray(30, "test_beam")));
        assertPassedThrough("VOLLEY", s, 3);
    }

    @Test
    void theProjectile() {
        Stage s = stage();
        cast(s, new CastSpec.Projectile(1.0, 0.0, 40));
        assertPassedThrough("PROJECTILE", s, 1);
    }

    @Test
    void theHomingProjectile() {
        Stage s = stage();
        cast(s, new CastSpec.Projectile(1.0, 0.0, 40, null, null, new CastSpec.Homing(0.3, 1.0, 16.0), null, null));
        assertPassedThrough("HOMING PROJECTILE", s, 1);
    }

    /** The Breacher's pellets: a spread of projectiles. The one on the aim passes through and lands on the mob. */
    @Test
    void theBreacherPellets() {
        Stage s = stage();
        cast(s, new CastSpec.Projectile(1.0, 0.0, 40, null, null, null, null, new CastSpec.Spread(4, 3)));
        assertEquals(100.0, s.summon().health, 1e-9, "PELLETS: the summon is untouched");
        double dealt = 100.0 - s.mob().health;
        assertTrue(dealt >= HIT && Math.abs(dealt / HIT - Math.rint(dealt / HIT)) < 1e-9,
                "PELLETS: the mob takes whole pellets at full value: " + dealt);
    }

    /** The crystal throw: READ, it asks sweepRay for a BLOCK only; a summon on the line cannot stop it. */
    @Test
    void theCrystalThrowIsNotStoppedByASummon() {
        Vec3[] rest = new Vec3[2];
        for (int i = 0; i < 2; i++) {
            Stage s = stage();
            if (i == 0) s.world().entities.remove(s.summon());
            cast(s, new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea"));
            rest[i] = s.world().orbs().of(s.him().id()).orElseThrow().restPoint();
        }
        assertEquals(rest[0], rest[1], "the orb rests where it would with no summon on its line");
    }

    /** A MOB's ray still stops on a summon (MOB -> SUMMON: yes): the port's own pick, with the attacker named. */
    @Test
    void aMobsPickStillStopsOnASummon() {
        Stage s = stage();
        Optional<RayHit> mobs = s.world().castRay(new Vec3(0, EYE, 0), new Vec3(20, EYE, 0), s.him().id(),
                MobHostility.Side.MOB);
        assertEquals(s.summon().id(), mobs.orElseThrow().combatant().id(), "a mob's ray ends on the summon");
        Optional<RayHit> players = s.world().castRay(new Vec3(0, EYE, 0), new Vec3(20, EYE, 0), s.him().id(),
                MobHostility.Side.PLAYER);
        assertEquals(s.mob().id(), players.orElseThrow().combatant().id(), "a player's passes it, to the mob");
    }
}
