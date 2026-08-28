package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.BasicMelee;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one predicate deciding who owns a trigger: the vanilla attack event, or the arm-swing packet.
 *
 * Two consumers read it with OPPOSITE senses -- WeaponFire skips when it is true, the melee rider
 * selects when it is true -- so a wrong answer does not merely misfire, it either resurrects the
 * 120-degree cone or processes one click twice. That is why it is a named predicate with its own
 * test rather than an inline conjunction at two call sites.
 *
 * Every fixture below mirrors a SHIPPED trigger, named in its comment. The paper-side companion
 * (WeaponLoaderTest) asserts the real yml still parses into these shapes, so content drift reddens
 * there; this file pins the logic given the shapes.
 */
class BasicMeleeTest {

    private static AbilityDefinition trigger(CastSpec cast, List<EffectSpec> onHit) {
        return new AbilityDefinition("t", "T", "kinetic", "none", 10, ResourceCost.FREE, cast, onHit);
    }

    private static final CastSpec.Melee MELEE = new CastSpec.Melee(3.5, 120);
    private static final CastSpec.Projectile PROJECTILE = new CastSpec.Projectile(2.5, 0.05, 60);

    @Test
    void aMeleeWeaponDamageTriggerIsTheHitVanillaNowDrives() {
        // ironblade / emberblade left_click: cast melee 3.5/120, on_hit weapon_damage.
        assertTrue(BasicMelee.isVanillaDriven(
                trigger(MELEE, List.of(new EffectSpec.WeaponDamage("kinetic")))));
        // Mutation: invert the result -> the swing path and the rider BOTH refuse the hit, and a
        // basic melee swing deals nothing at all -> reddens.
    }

    @Test
    void aBasicAttackThatIsNotMeleeStaysOnTheSwingPath() {
        // hunters_bow right_click: a weapon_damage BASIC ATTACK, but its cast is a projectile.
        // Vanilla's melee attack cannot deliver a bow shot, so this must stay ours to fire.
        assertFalse(BasicMelee.isVanillaDriven(
                trigger(PROJECTILE, List.of(new EffectSpec.WeaponDamage("kinetic")))));
        // Mutation: drop the `cast instanceof CastSpec.Melee` half -> true -> WeaponFire skips the
        // bow and it never shoots again -> reddens.
    }

    @Test
    void aMeleeAbilityWithALiteralPayloadStaysOnTheSwingPath() {
        // void_slash: cast melee 3.5/120, but its payload is a LITERAL damage plus a burst. It is a
        // costed ability, not a basic attack, and the arc IS its feature.
        assertFalse(BasicMelee.isVanillaDriven(trigger(MELEE, List.of(
                new EffectSpec.Visual("void_slash"),
                new EffectSpec.Damage(7, "void"),
                new EffectSpec.Burst(3.5, List.of(new EffectSpec.Damage(4, "void")))))));
        // Mutation: drop the `isBasicAttack` half -> true -> void_slash loses its arc and its burst,
        // and fires off a crosshair hit it never spent mana for -> reddens.
        //
        // This case and the bow above are what make BOTH halves load-bearing: neither mutation is
        // caught by the other's test.
    }

    @Test
    void aCostedProjectileAbilityIsNeitherHalf() {
        // emberblade right_click (Fireball): literal damage, projectile cast. The control that
        // shows a false answer is not just "whatever fails one half".
        assertFalse(BasicMelee.isVanillaDriven(
                trigger(PROJECTILE, List.of(new EffectSpec.Damage(12, "fire")))));
    }

    @Test
    void anEmptyOrEffectlessPayloadIsNotABasicAttack() {
        // A trigger with no damage at all resolves no payload, so it cannot be the basic hit -- the
        // guard that keeps a visual-only or status-only melee trigger off the rider.
        assertFalse(BasicMelee.isVanillaDriven(trigger(MELEE, List.of())));
        assertFalse(BasicMelee.isVanillaDriven(
                trigger(MELEE, List.of(new EffectSpec.Visual("void_slash")))));
    }
}
