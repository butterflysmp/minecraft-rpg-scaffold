package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.build.Touchdown;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sunder (PLAN-melee-class.md section 8.2, S1; the seat's rulings 4 and 5): a dash whose {@code on_land} detonates
 * where the leap COMES DOWN, not at take-off. The fake has no physics, so each row ends the leap by calling the hook
 * the dash armed, at the feet the paper side would read -- which is exactly the seam the paper side owns.
 */
class SunderSlamTest {

    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));
    private static final Vec3 LANDING = new Vec3(5, 0, 0);

    /** The shape Sunder ships, with its PROVISIONAL numbers: a burst r3.0 of damage 20 and knockback 0.6. */
    private static final List<EffectSpec> SLAM = List.of(new EffectSpec.Burst(3.0,
            List.of(new EffectSpec.Damage(20, "kinetic"), new EffectSpec.Knockback(0.6))));

    private static AbilityDefinition sunder(boolean safeLanding, List<EffectSpec> onLand) {
        return new AbilityDefinition("sunder", "Sunder", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Dash(5, 1.1, 0.5, CastSpec.DashDirection.FACING, safeLanding, onLand), List.of());
    }

    private record Scene(FakeWorld world, FakeWorld.Dummy caster, FakeWorld.Dummy atStart, FakeWorld.Dummy atLanding) {}

    /** A caster at the origin, one mob at the take-off point and one beside the landing, 4.5 blocks apart. */
    private static Scene castAt(AbilityDefinition def) {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var atStart = new FakeWorld.Dummy(new Vec3(0.5, 0, 0));
        var atLanding = new FakeWorld.Dummy(new Vec3(5.5, 0, 0));
        world.entities.add(caster);
        world.entities.add(atStart);
        world.entities.add(atLanding);
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), EAST, Set.of(def.id())));
        new CastExecutor(world).execute(success);
        return new Scene(world, caster, atStart, atLanding);
    }

    @Test
    void aSlamDashArmsOneLandingMarkAndNotTheBareSafeLanding() {
        var s = castAt(sunder(false, SLAM));
        assertEquals(1, s.caster().landingArms);
        assertEquals(0, s.caster().safeLandingArms, "one mark does both jobs (ruling 5): never two arms");
        assertNotNull(s.caster().landingHook);
    }

    /** Ruling 5 holds whatever safe_landing says: with a slam, the landing mark is the immunity. */
    @Test
    void aSlamDashWithSafeLandingTrueStillArmsOnlyTheLandingMark() {
        var s = castAt(sunder(true, SLAM));
        assertEquals(1, s.caster().landingArms);
        assertEquals(0, s.caster().safeLandingArms);
    }

    @Test
    void aDashWithoutASlamArmsNoLandingMark() {
        var s = castAt(sunder(true, List.of()));
        assertEquals(0, s.caster().landingArms);
        assertEquals(1, s.caster().safeLandingArms, "a bare safe_landing dash is unchanged (ruling 31)");
    }

    /** NOTHING AT TAKE-OFF: the opposite of every dash before Sunder, whose on_hit fires on the cast frame. */
    @Test
    void nothingIsHitAtTakeOff() {
        var s = castAt(sunder(false, SLAM));
        assertEquals(100, s.atStart().health, 1e-9);
        assertEquals(100, s.atLanding().health, 1e-9);
        assertEquals(List.of(), s.world().landings);
    }

    @Test
    void theSlamHitsAtTheLiveLandingAndNotAtTheTakeOffPoint() {
        var s = castAt(sunder(false, SLAM));
        s.caster().landingHook.landed(LANDING, Touchdown.LANDED);

        assertTrue(s.atLanding().health < 100, "the mob beside the landing takes the slam");
        assertEquals(100, s.atStart().health, 1e-9, "the mob where the leap began, 4.5 from the landing, takes nothing");
        assertEquals(1, s.atLanding().knockbackCalls);
        assertEquals(0, s.atStart().knockbackCalls);
        // Pushed AWAY from the landing point: the mob is at +0.5 x of it.
        assertTrue(s.atLanding().lastKnockbackDir.x() > 0, "away from where the caster came down");
    }

    @Test
    void theLandingIsTracedWithItsCauseAndFeetBeforeTheHits() {
        var s = castAt(sunder(false, SLAM));
        s.caster().landingHook.landed(LANDING, Touchdown.ABSORBED);
        assertEquals(List.of(new FakeWorld.TracedLanding(s.caster().id(), "sunder", Touchdown.ABSORBED, LANDING)),
                s.world().landings);
        assertFalse(s.world().traced.isEmpty(), "the slam's hit reaches the PLAYERHIT trace too");
    }

    /** The backstop, death or quit, a re-arm: a LANDING line that says so, and no burst at all. */
    @Test
    void anEndingThatDoesNotSlamIsTracedAndHitsNothing() {
        for (Touchdown how : List.of(Touchdown.BACKSTOP, Touchdown.FORGOTTEN, Touchdown.SUPERSEDED)) {
            var s = castAt(sunder(false, SLAM));
            s.caster().landingHook.ended(how);
            assertEquals(100, s.atLanding().health, 1e-9, how.name());
            assertEquals(100, s.atStart().health, 1e-9, how.name());
            assertEquals(List.of(new FakeWorld.TracedLanding(s.caster().id(), "sunder", how, null)),
                    s.world().landings, how.name());
        }
    }

    /** The caster is never caught by their own slam, though they stand at its centre. */
    @Test
    void theCasterIsExcludedFromTheirOwnSlam() {
        var s = castAt(sunder(false, SLAM));
        s.caster().moveTo(LANDING);
        s.caster().landingHook.landed(LANDING, Touchdown.LANDED);
        assertEquals(100, s.caster().health, 1e-9);
        assertEquals(0, s.caster().knockbackCalls);
    }
}
