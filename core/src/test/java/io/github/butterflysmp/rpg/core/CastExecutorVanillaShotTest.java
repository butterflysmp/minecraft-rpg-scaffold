package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * THE VANILLA SHOT'S HIT (the seat's ruling 10, 2026-11-22): a stamped arrow lands its weapon's {@code shot} on_hit through
 * the same sum as every weapon hit, with the force as the charge term and the terms SNAPSHOTTED AT THE SHOT. The weapon's
 * wear is vanilla's, at the shot, so the basic-attack wear hook must NOT run here.
 *
 * <p>READ, EffectApplier's {@code WeaponDamage} arm: a weapon_damage hit reads the caster's attack damage, which the store
 * already holds SCALED by the held weapon's score; the trigger score scales only a literal {@code damage} effect. So the
 * score that must be the shot's and not the world's is tested on a literal-damage row, and the attack damage on the
 * weapon_damage row.
 */
class CastExecutorVanillaShotTest {

    private static final double EPS = 1e-9;

    private static AbilityDefinition shot(EffectSpec payload) {
        return new AbilityDefinition("bow/shot", "Shot", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Vanilla(), List.of(payload));
    }

    private static FakeWorld.Dummy[] stage(FakeWorld world) {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);
        return new FakeWorld.Dummy[] {caster, victim};
    }

    /** weapon_damage: the stamped (already scaled) attack damage x the force. */
    @Test
    void weaponDamageLandsTheStampedAttackDamageTimesTheForce() {
        var world = new FakeWorld();
        var d = stage(world);
        d[0].attackDamage = 35.0;   // 14 at score 250, as the store holds it at the shot

        new CastExecutor(world, () -> fail("vanilla wears the bow at the shot; our wear hook must not run"))
                .landVanillaShot(shot(new EffectSpec.WeaponDamage("kinetic")), d[0].snapshot(), 250,
                        new Combatant(d[1].snapshot(), d[1]), 0.5);

        assertEquals(100 - 17.5, d[1].health, EPS, "35 x 0.5");
    }

    /** A literal damage: scaled by the score STAMPED at the shot, never the world's score at the hit. */
    @Test
    void aLiteralDamageIsScaledByTheStampedScoreNotTheWorldsAtTheHit() {
        var world = new FakeWorld();
        var d = stage(world);
        d[0].triggerScore = 999;    // what he holds AT THE HIT; must not be read

        new CastExecutor(world, () -> { })
                .landVanillaShot(shot(new EffectSpec.Damage(16.0, "kinetic")), d[0].snapshot(), 250,
                        new Combatant(d[1].snapshot(), d[1]), 0.5);

        // 16 x 250/100 x 0.5 = 20. The world's 999 would deal 79.92; no score at all, 8.
        assertEquals(100 - 20.0, d[1].health, EPS);
    }

    @Test
    void fullForceIsTheAuthoredFigure() {
        var world = new FakeWorld();
        var d = stage(world);
        d[0].attackDamage = 37.0;

        new CastExecutor(world, () -> { }).landVanillaShot(shot(new EffectSpec.WeaponDamage("kinetic")),
                d[0].snapshot(), 100, new Combatant(d[1].snapshot(), d[1]), 1.0);

        assertEquals(100 - 37.0, d[1].health, EPS);
    }
}
