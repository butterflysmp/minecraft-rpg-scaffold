package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.Ignite;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.SweptLine;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2a's PROOF (the seat's H7, 2026-11-24): THE ONE TABLE against every core path, with a FAKE summon. Each path is run
 * TWICE on the same staging: the victim as a MOB (the control: the path reaches it) and as a SUMMON (PLAYER -> SUMMON:
 * no). A path that never reached the victim cannot pass. The shockwave, the crystal shot and the homing pick are proven
 * beside their own player rows ({@code BehaviourHitsTest}, {@code OrbBeamTest}, {@code ProjectileHomingTest}).
 */
class SummonSidesCoreTest {

    private static final double EYE = 1.62;

    private static FakeWorld.Dummy at(FakeWorld world, Vec3 pos) {
        var d = new FakeWorld.Dummy(pos);
        world.entities.add(d);
        return d;
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy him, CastSpec spec, List<EffectSpec> onHit) {
        var def = new AbilityDefinition("sides_2a", "Test", "kinetic", 0, ResourceCost.FREE, spec, onHit);
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(him.snapshot(), def.id(), new Aim(new Vec3(0, EYE, 0), new Vec3(1, 0, 0)), Set.of(def.id())));
        new CastExecutor(world).execute(success);
        world.advanceTicks(40);
    }

    private static List<EffectSpec> hit() {
        return List.of(new EffectSpec.Damage(10, "kinetic"));
    }

    /** Runs the path with the victim as a mob, then as a summon; returns {damageCalls as mob, as summon}. */
    private static int[] mobThenSummon(Vec3 victimAt, BiConsumer<FakeWorld, FakeWorld.Dummy> path) {
        int[] calls = new int[2];
        for (int i = 0; i < 2; i++) {
            var world = new FakeWorld();
            var him = at(world, Vec3.ZERO);
            him.player = true;
            var victim = at(world, victimAt);
            victim.summon = i == 1;
            path.accept(world, him);
            calls[i] = victim.damageCalls;
        }
        return calls;
    }

    private static void assertMobHitSummonSpared(String path, int[] calls) {
        assertTrue(calls[0] >= 1, path + ": the control, a mob in the same place, is hit: " + calls[0]);
        assertEquals(0, calls[1], path + ": a summon is never hit by a player's ability");
    }

    @Test
    void theSwing() {
        assertMobHitSummonSpared("SWING", mobThenSummon(new Vec3(2, 0, 0),
                (w, him) -> cast(w, him, new CastSpec.Melee(3.0, 90), hit())));
    }

    @Test
    void thePlainRay() {
        assertMobHitSummonSpared("RAY", mobThenSummon(new Vec3(5, EYE, 0),
                (w, him) -> cast(w, him, new CastSpec.Ray(30), hit())));
    }

    @Test
    void theSweptRay() {
        assertMobHitSummonSpared("RAY (swept)", mobThenSummon(new Vec3(5, EYE, 0),
                (w, him) -> cast(w, him, new CastSpec.Ray(30, null, false, 0.5), hit())));
    }

    @Test
    void theProjectile() {
        assertMobHitSummonSpared("PROJECTILE", mobThenSummon(new Vec3(6, EYE, 0),
                (w, him) -> cast(w, him, new CastSpec.Projectile(1.0, 0.0, 40, null, null, null), hit())));
    }

    @Test
    void theCone() {
        assertMobHitSummonSpared("CONE", mobThenSummon(new Vec3(3, 0, 0),
                (w, him) -> cast(w, him, new CastSpec.Cone(5.0, 60, 0, null), hit())));
    }

    @Test
    void theBurst() {
        assertMobHitSummonSpared("BURST", mobThenSummon(new Vec3(2, 0, 0),
                (w, him) -> cast(w, him, new CastSpec.Self(), List.of(new EffectSpec.Burst(3.0, hitTargeted())))));
    }

    @Test
    void theArea() {
        assertMobHitSummonSpared("AREA", mobThenSummon(new Vec3(2, 0, 0),
                (w, him) -> cast(w, him, new CastSpec.Self(), List.of(new EffectSpec.Area(3.0, 20, 10, hitTargeted())))));
    }

    @Test
    void theTravelEndBurst() {
        assertMobHitSummonSpared("TRAVEL_END_BURST", mobThenSummon(new Vec3(2, 0, 0), (w, him) -> {
            var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 20,
                    new EffectSpec.Burst(4.0, hitTargeted()), "ember_burst", null);
            new EffectApplier(w).applyAll(List.of(embers), him.asCaster(), null, Vec3.ZERO);
            w.advanceTicks(20);
        }));
    }

    @Test
    void theDashLine() {
        for (boolean summon : new boolean[] {false, true}) {
            var world = new FakeWorld();
            var him = at(world, Vec3.ZERO);
            him.player = true;
            var victim = at(world, new Vec3(2, 0, 0));
            victim.summon = summon;
            List<Combatant> hits = SweptLine.enemiesAlong(Vec3.ZERO, new Vec3(1, 0, 0), 5.0, 1.0,
                    world.combatantsNear(Vec3.ZERO, 10), him.id(), MobHostility.ENGINE_ATTACKER);
            assertEquals(summon ? List.of() : List.of(victim.id()), hits.stream().map(Combatant::id).toList(),
                    "DASH_LINE, as a " + (summon ? "summon" : "mob (the control)"));
        }
    }

    @Test
    void theIgniteSplash() {
        assertMobHitSummonSpared("IGNITE_SPLASH", mobThenSummon(new Vec3(1, 0, 0),
                (w, him) -> {
                    Ignite.detonate(w, Vec3.ZERO, him.id(), UUID.randomUUID(), 1);
                    w.advanceTicks(Ignite.DELAY_TICKS);
                }));
    }

    /** IMPLOSION: neither setting pulls a summon when a player casts it; the control is the side each setting admits. */
    @Test
    void anImplosionOfEitherSettingNeverPullsASummon() {
        var shape = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 1.0, 0.0, Implosion.Resistance.NONE);
        for (EffectSpec.Affects affects : EffectSpec.Affects.values()) {
            for (boolean summon : new boolean[] {false, true}) {
                var world = new FakeWorld();
                var him = at(world, new Vec3(-20, 64, 0));
                him.player = true;
                var victim = at(world, new Vec3(2, 64, 0));
                victim.player = !summon && affects == EffectSpec.Affects.PLAYERS;   // the control: the side admitted
                victim.summon = summon;
                new EffectApplier(world).applyAll(List.of(new EffectSpec.Implosion(shape, affects, null, null)),
                        him.asCaster(), null, new Vec3(0, 64, 0));
                assertEquals(summon ? 0 : 1, victim.pullCalls, "IMPLOSION " + affects + (summon ? ", a summon" : ", the control"));
            }
        }
    }

    private static List<EffectSpec.Targeted> hitTargeted() {
        return List.of(new EffectSpec.Damage(10, "kinetic"));
    }
}
