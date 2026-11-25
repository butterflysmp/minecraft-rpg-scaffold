package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.SweptLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * PLAYER -> PLAYER, TODAY'S BEHAVIOUR, UNRULED: OPEN FOR BEN (PvP). The seat's H2 (2026-11-24): every PLAYER -> PLAYER
 * cell of the sides table is filled with what the code does TODAY, per arm, pinned BEFORE the Summoner's refactor and green
 * before and after it. These rows assert what IS, not what should be: a red row after the refactor means the refactor
 * changed PvP, which this slice must not do.
 *
 * <p>Two exclusions are already pinned elsewhere and are not repeated: the shockwave ({@code BehaviourHitsTest}, "never a
 * player") and the Lapis Crystal's shot ({@code OrbBeamTest}, "never a player"). The direct ray's landing on a player is
 * pinned by {@code PlayerHitTraceTest.aPlayerTargetIsNotTraced} ("the hit landed").
 */
class PvpTodayCharacterisationTest {

    private static final double EYE = 1.62;

    private static FakeWorld.Dummy at(FakeWorld world, Vec3 pos, boolean player) {
        var d = new FakeWorld.Dummy(pos);
        d.player = player;
        world.entities.add(d);
        return d;
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, CastSpec spec, List<EffectSpec> onHit) {
        var def = new AbilityDefinition("pvp_today", "Test", "kinetic", 0, ResourceCost.FREE, spec, onHit);
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), new Aim(new Vec3(0, EYE, 0), new Vec3(1, 0, 0)),
                        Set.of(def.id())));
        new CastExecutor(world).execute(success);
        world.advanceTicks(40);
    }

    private static List<EffectSpec> hit() {
        return List.of(new EffectSpec.Damage(10, "kinetic"));
    }

    /** DIRECT, the swing ({@code CastSpec.Melee}): the nearest body in the arc, no side filter. TODAY: lands. */
    @Test
    void directMeleeLandsOnAPlayer() {
        var world = new FakeWorld();
        var him = at(world, Vec3.ZERO, true);
        var other = at(world, new Vec3(2, 0, 0), true);
        cast(world, him, new CastSpec.Melee(3.0, 90), hit());
        assertEquals(1, other.damageCalls, "TODAY'S BEHAVIOUR: a player's swing lands on a player");
    }

    /** DIRECT, the ray ({@code castRay}'s first body): TODAY, lands. The control row for the others' shape. */
    @Test
    void directRayLandsOnAPlayer() {
        var world = new FakeWorld();
        var him = at(world, Vec3.ZERO, true);
        var other = at(world, new Vec3(5, EYE, 0), true);
        cast(world, him, new CastSpec.Ray(30), hit());
        assertEquals(1, other.damageCalls, "TODAY'S BEHAVIOUR: a player's ray lands on a player");
    }

    /** The cone ({@code ConeShell.reached}): every body in the arc and range. TODAY: lands. */
    @Test
    void coneLandsOnAPlayer() {
        var world = new FakeWorld();
        var him = at(world, Vec3.ZERO, true);
        var other = at(world, new Vec3(3, 0, 0), true);
        cast(world, him, new CastSpec.Cone(5.0, 60, 0, null), hit());
        assertEquals(1, other.damageCalls, "TODAY'S BEHAVIOUR: a player's cone lands on a player");
    }

    /** INDIRECT, a burst ({@code EffectApplier.applyToNearby}): everything in the radius but the caster. TODAY: lands. */
    @Test
    void indirectBurstLandsOnAPlayer() {
        var world = new FakeWorld();
        var him = at(world, Vec3.ZERO, true);
        var other = at(world, new Vec3(2, 0, 0), true);
        cast(world, him, new CastSpec.Self(),
                List.of(new EffectSpec.Burst(3.0, List.of(new EffectSpec.Damage(10, "kinetic")))));
        assertEquals(1, other.damageCalls, "TODAY'S BEHAVIOUR: a player's burst lands on a player");
    }

    /** The dash's set ({@code SweptLine.enemiesAlong}): "mob-only: players are not hit". TODAY: spared. */
    @Test
    void theDashsLineSparesAPlayer() {
        var world = new FakeWorld();
        var him = at(world, Vec3.ZERO, true);
        var player = at(world, new Vec3(2, 0, 0), true);
        var mob = at(world, new Vec3(3, 0, 0), false);
        List<Combatant> hits = SweptLine.enemiesAlong(Vec3.ZERO, new Vec3(1, 0, 0), 5.0, 1.0,
                world.combatantsNear(Vec3.ZERO, 10), him.id());
        assertEquals(List.of(mob.id()), hits.stream().map(Combatant::id).toList(),
                "TODAY'S BEHAVIOUR: the dash spares a player (and takes the mob, the control): " + player.id());
    }
}
