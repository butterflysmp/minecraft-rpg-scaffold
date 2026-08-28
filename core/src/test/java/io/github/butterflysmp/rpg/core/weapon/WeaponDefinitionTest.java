package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attack-speed half of a weapon's contract: what content must declare, and what that declaration
 * means once resolved.
 *
 * Both halves exist because their failures are SILENT. A vanilla-driven melee weapon with no
 * declared speed would load, mint, swing and damage -- only the cadence would be quietly the
 * bare-fist 4.0. And a bow whose declared speed leaked through to the wielder's attribute would pace
 * a bow like a sword, with nothing anywhere reporting a problem.
 *
 * Each test names the mutation it forces red.
 */
class WeaponDefinitionTest {

    private static AbilityDefinition trigger(CastSpec cast, EffectSpec payload) {
        return new AbilityDefinition("w/left_click", "T", "kinetic", "none",
                0, ResourceCost.FREE, cast, List.of(payload));
    }

    private static final CastSpec.Melee MELEE = new CastSpec.Melee(3.0, 120);
    private static final CastSpec.Projectile PROJECTILE = new CastSpec.Projectile(2.5, 0.05, 60);

    private static WeaponDefinition weapon(double attackDamage, double attackSpeed,
                                           CastSpec cast, EffectSpec payload) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.MELEE,
                WeaponDefinition.DEFAULT_MATERIAL, attackDamage, attackSpeed,
                List.of(new TriggerBinding("left_click", trigger(cast, payload))), List.of());
    }

    // --- What must be declared ---

    @Test
    void aVanillaDrivenMeleeWeaponMustDeclareAPositiveSpeed() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> weapon(8.0, 0.0, MELEE, new EffectSpec.WeaponDamage("kinetic")));
        assertTrue(thrown.getMessage().contains("attack_speed"), thrown.getMessage());
        // Mutation: drop the guard -> the weapon constructs, loads, and swings at the bare-fist
        // 4.0 with nothing reporting it -> reddens.
    }

    @Test
    void aNegativeSpeedIsRejectedLikeANegativeDamage() {
        assertThrows(IllegalArgumentException.class,
                () -> weapon(8.0, -1.6, MELEE, new EffectSpec.WeaponDamage("kinetic")));
        assertThrows(IllegalArgumentException.class,
                () -> weapon(0.0, -1.0, PROJECTILE, new EffectSpec.Damage(5, "kinetic")));
        // Mutation: drop the attackSpeed < 0 guard -> a negative reaches the wielder's attribute.
    }

    @Test
    void theRequirementKeysOnTheTriggerShapeAndNotOnTheWeaponClass() {
        // Same class: melee, same attack_damage, no declared speed -- but a LITERAL payload, so it
        // is an ability rather than a basic attack and vanilla delivers nothing. It must construct.
        // This is the case that proves the guard is not just "class == MELEE".
        assertEquals(0.0, weapon(8.0, 0.0, MELEE, new EffectSpec.Damage(8, "kinetic")).attackSpeed());
        // And a bow: a weapon_damage basic attack, but a projectile cast.
        assertEquals(0.0, weapon(6.0, 0.0, PROJECTILE, new EffectSpec.WeaponDamage("kinetic")).attackSpeed());
        // Mutation: widen the guard to any MELEE-class weapon, or to any basic attack -> both of
        // these throw and shipped content stops loading -> reddens.
    }

    @Test
    void aWeaponDealingNoDamageNeedsNoSpeedEvenWithAMeleeTrigger() {
        // attack_damage 0 means the swing deals nothing anyway (EffectApplier's amount>0 guard), so
        // there is no vanilla attack to pace. Gated on the SAME condition WeaponItems.mint uses --
        // if the two ever disagree, an item is minted as vanilla-driven that never validated as one.
        assertEquals(0.0, weapon(0.0, 0.0, MELEE, new EffectSpec.WeaponDamage("kinetic")).attackSpeed());
    }

    // --- What the declaration resolves to ---

    @Test
    void meleeCadenceIsTheDeclaredSpeedWhenVanillaActuallyDeliversTheHit() {
        assertEquals(1.6, weapon(8.0, 1.6, MELEE, new EffectSpec.WeaponDamage("kinetic")).meleeCadence());
    }

    @Test
    void meleeCadenceIsZeroForAWeaponVanillaDeliversNothingFor() {
        // THE LEAK THIS CLOSES. A bow may legitimately declare a speed one day; if that number
        // reached the wielder's attack-speed attribute it would pace their sword-less hand as
        // though it were a sword, and nothing would report it. 0.0 is the "write no modifier"
        // signal, not a speed.
        assertEquals(0.0, weapon(6.0, 1.6, PROJECTILE, new EffectSpec.WeaponDamage("kinetic")).meleeCadence(),
                "a declared speed on a RANGED basic attack governs nothing and must not leak out");
        assertEquals(0.0, weapon(8.0, 1.6, MELEE, new EffectSpec.Damage(8, "kinetic")).meleeCadence(),
                "nor may one on a melee-cast ABILITY, which vanilla also does not deliver");
        // Mutation: return attackSpeed unconditionally -> both reddens. This is the exact mutation
        // that previously slipped through with no test catching it.
    }
}
