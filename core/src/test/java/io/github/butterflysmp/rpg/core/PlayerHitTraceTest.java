package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE {@code PLAYERHIT} TRACE (PLAN-melee-class.md, the seat's ruling 2 on Q8): {@code EffectApplier}
 * reports each landed direct hit on a non-player, carrying the ability id, the score and the amount sent.
 *
 * <p>Like {@code CastExecutorTriggerScoreTest}, every row drives a REAL {@code AbilityService} into a real
 * {@code CastExecutor}, so the {@code Caster} (and its {@code source}) is built by production code, never
 * by {@code asCaster()}. A fixture that set the source itself would pin the arithmetic and not the wiring.
 *
 * <p>STAGING: authored {@code 16} at score {@code 250} sends {@code 40}; no two quantities a row reads
 * are equal, so a transposition (sent for score, caster for target) has nowhere to hide.
 */
class PlayerHitTraceTest {

    private static final double EPS = 1e-9;
    private static final double EYE = 1.62;
    private static final double AUTHORED = 16.0;
    private static final int SCORE = 250;
    private static final double SCALED = 40.0;   // 16 * 250 / 100

    private static AbilityDefinition ray(String id, double amount) {
        return new AbilityDefinition(id, "Test", "fire", 0, ResourceCost.FREE, new CastSpec.Ray(30),
                List.of(new EffectSpec.Damage(amount, "fire")));
    }

    private static FakeWorld.Dummy at(FakeWorld world, Vec3 pos) {
        var d = new FakeWorld.Dummy(pos);
        world.entities.add(d);
        return d;
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), new Aim(new Vec3(0, EYE, 0), new Vec3(1, 0, 0)),
                        Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    @Test
    void aLandedHitOnAMobTracesItsAbilityScoreAndSentAmount() {
        var world = new FakeWorld();
        var caster = at(world, Vec3.ZERO);
        var mob = at(world, new Vec3(5, EYE, 0));
        caster.triggerScore = SCORE;

        cast(world, caster, ray("arc_test", AUTHORED));

        assertEquals(1, world.traced.size(), "one landed hit, one line");
        TracedHit hit = world.traced.get(0);
        assertEquals("arc_test", hit.source(), "the ABILITY id, set by the cast path");
        assertEquals(caster.id(), hit.casterId());
        assertEquals(mob.id(), hit.targetId());
        assertEquals("fire", hit.element());
        assertEquals(SCALED, hit.sent(), EPS, "the amount the port was sent: 16 at score 250");
        assertEquals(SCORE, hit.triggerScore());
        assertFalse(hit.crit());
        assertEquals(100 - SCALED, mob.health, EPS, "and the line is the hit that landed");
    }

    /** A crit changes what is sent, so the line must carry it: 16 x 1.5 = 24 at the baseline score. */
    @Test
    void aCritIsOnTheLine() {
        var world = new FakeWorld();
        var caster = at(world, Vec3.ZERO);
        at(world, new Vec3(5, EYE, 0));
        caster.critMultiplier = 1.5;

        cast(world, caster, ray("arc_test", AUTHORED));

        assertTrue(world.traced.get(0).crit());
        assertEquals(24.0, world.traced.get(0).sent(), EPS);
    }

    /** A burst reports each body it lands on, once, and never the caster it excludes. */
    @Test
    void eachBodyInABurstIsTracedOnce() {
        var world = new FakeWorld();
        var caster = at(world, Vec3.ZERO);
        var near = at(world, new Vec3(2, 0, 0));
        var far = at(world, new Vec3(0, 0, -2.5));
        var def = new AbilityDefinition("slam_test", "Test", "kinetic", 0, ResourceCost.FREE, new CastSpec.Self(),
                List.of(new EffectSpec.Burst(3.0, List.of(new EffectSpec.Damage(7, "kinetic")))));

        cast(world, caster, def);

        assertEquals(Set.of(near.id(), far.id()),
                Set.copyOf(world.traced.stream().map(TracedHit::targetId).toList()));
        assertEquals(2, world.traced.size(), "once each");
        assertTrue(world.traced.stream().allMatch(h -> h.source().equals("slam_test")));
    }

    /** Player-to-mob only. The hit still LANDS -- the positive control that the row staged a hit at all. */
    @Test
    void aPlayerTargetIsNotTraced() {
        var world = new FakeWorld();
        var caster = at(world, Vec3.ZERO);
        var other = at(world, new Vec3(5, EYE, 0));
        other.player = true;

        cast(world, caster, ray("arc_test", AUTHORED));

        assertEquals(1, other.damageCalls, "the hit landed");
        assertTrue(world.traced.isEmpty(), "and was not traced: " + world.traced);
    }

    /** Inside the gate: a hit the applier refuses (it resolves to 0) reports nothing. */
    @Test
    void aRefusedHitTracesNothing() {
        var world = new FakeWorld();
        var caster = at(world, Vec3.ZERO);
        var mob = at(world, new Vec3(5, EYE, 0));

        cast(world, caster, ray("arc_test", 0.0));

        assertEquals(0, mob.damageCalls, "the applier refused it");
        assertTrue(world.traced.isEmpty());
    }

    /**
     * The weapon_damage arm reports too, from landBasicMelee, with the WEAPON's trigger id. The score is on
     * the line although this arm does not apply it: sent is the attack stat, 19, not 47.5.
     */
    @Test
    void aWeaponSwingIsTracedWithItsTriggerId() {
        var world = new FakeWorld();
        var caster = at(world, Vec3.ZERO);
        var mob = at(world, new Vec3(2, EYE, 0));
        caster.attackDamage = 19;
        caster.triggerScore = SCORE;
        var swing = new AbilityDefinition("blade/left_click", "Swing", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Melee(3.0, 90.0), List.of(new EffectSpec.WeaponDamage("kinetic")));

        new CastExecutor(world).landBasicMelee(swing, caster.snapshot(), new Combatant(mob.snapshot(), mob),
                AttackCharge.FULL_CHARGE);

        assertEquals(1, world.traced.size());
        assertEquals("blade/left_click", world.traced.get(0).source());
        assertEquals(19.0, world.traced.get(0).sent(), EPS);
        assertEquals(SCORE, world.traced.get(0).triggerScore());
    }

    /** The line the gate reads verbatim. A null source prints as "-", never as a made-up id. */
    @Test
    void theLineIsExact() {
        UUID c = new UUID(0, 1);
        UUID t = new UUID(0, 2);
        assertEquals("PLAYERHIT caster=00000000-0000-0000-0000-000000000001 source=sunder"
                        + " target=00000000-0000-0000-0000-000000000002 zombie element=kinetic sent=12.500"
                        + " crit=false triggerScore=100",
                new TracedHit(c, "sunder", t, "kinetic", 12.5, false, 100).line("zombie"));
        assertTrue(new TracedHit(c, null, t, "fire", 1, true, 300).line("husk").contains(" source=- "));
    }
}
