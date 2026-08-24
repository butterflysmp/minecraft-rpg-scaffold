package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The basic-attack discriminator, which two systems now depend on agreeing: the tooltip picks a stat
 * block over an ability block by it, and the cooldown scaler picks whether to apply attack speed by
 * it. If these two ever answered differently about one weapon, a sword could render as an ability
 * and swing like a basic attack -- so the rule is asked in one place and pinned here.
 */
class DamagePayloadTest {

    @Test
    void aWeaponDamagePayloadIsABasicAttack() {
        assertTrue(DamagePayload.isBasicAttack(List.of(new EffectSpec.WeaponDamage("kinetic"))));
    }

    @Test
    void aLiteralDamagePayloadIsNot() {
        assertFalse(DamagePayload.isBasicAttack(List.of(new EffectSpec.Damage(12, "fire"))));
    }

    @Test
    void aCosmeticOnlyPayloadIsNot() {
        // No damage at all -- ability_stone's shape. Not a basic attack, and must not throw.
        assertFalse(DamagePayload.isBasicAttack(List.of(new EffectSpec.Visual("rekindle_cast"))));
        assertFalse(DamagePayload.isBasicAttack(List.of()));
    }

    @Test
    void aWeaponDamageBuriedInABurstIsStillABasicAttack() {
        // The recursion has to hold, or a weapon whose swing bursts would quietly stop scaling.
        assertTrue(DamagePayload.isBasicAttack(List.of(
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Burst(3.0, List.of(new EffectSpec.WeaponDamage("fire"))))));
    }

    @Test
    void aCosmeticBeforeTheWeaponDamageDoesNotHideIt() {
        assertTrue(DamagePayload.isBasicAttack(List.of(
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.WeaponDamage("kinetic"))));
    }

    /**
     * FIRST-WINS, not contains-anywhere -- the case that pins which rule is written down.
     *
     * A "contains" rule would call this a basic attack. The tooltip, which takes the first
     * damage-bearing effect, renders it as an ability block. Answering differently here is exactly
     * the drift this class exists to prevent, so the discriminator must agree with the tooltip: the
     * literal comes first, so this is an ability.
     *
     * No shipped content mixes payloads this way. That is the point -- the rule is settled before
     * something does.
     */
    @Test
    void aLiteralAheadOfAWeaponDamageMakesItAnAbilityNotABasicAttack() {
        List<EffectSpec> mixed = List.of(
                new EffectSpec.Damage(5, "fire"),
                new EffectSpec.WeaponDamage("kinetic"));

        assertFalse(DamagePayload.isBasicAttack(mixed),
                "first damage-bearing effect wins, matching what the tooltip renders");
        // ...and the tooltip genuinely does read it that way, which is the agreement being pinned.
        assertFalse(DamagePayload.of(mixed, 8).orElseThrow().source()
                == DamagePayload.DamageSource.WEAPON_STAT);
    }

    /**
     * A non-damage effect ahead of a weapon_damage does NOT shadow it. "First wins" is first
     * DAMAGE-BEARING, and a visual bears none.
     *
     * This is shipped content now, not a hypothetical: hunters_bow's on_hit is [visual,
     * weapon_damage], the only shipped basic attack whose payload does not lead with its damage. If
     * the walk stopped at the first effect outright, the bow would render as an ability, lose its
     * attack-speed scaling, and have no stat for a "+N Ranged Damage" modifier to reach.
     */
    @Test
    void aVisualAheadOfAWeaponDamageDoesNotShadowIt() {
        List<EffectSpec> bowPayload = List.of(
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.WeaponDamage("fire"));

        assertTrue(DamagePayload.isBasicAttack(bowPayload),
                "a leading visual bears no damage, so the weapon_damage still wins");
        var damage = DamagePayload.of(bowPayload, 6).orElseThrow();
        assertEquals(6, damage.amount());
        assertEquals(DamagePayload.DamageSource.WEAPON_STAT, damage.source());
    }
}
