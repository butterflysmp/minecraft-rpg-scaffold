package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reddening tests for the pure tooltip formatters. Each asserts the exact string/number a tooltip
 * clause should read, so breaking a divisor, dropping the recursion, or mis-labelling a clause
 * fails here in the 2-second loop rather than surfacing only at boot.
 */
class WeaponLoreLinesTest {

    // --- cooldownLabel: divide by 20, one decimal. Redden by changing the /20. ---

    @Test
    void cooldownLabelHalfSecond() {
        assertEquals("0.5s", WeaponLoreLines.cooldownLabel(10));
    }

    @Test
    void cooldownLabelKeepsTrailingDecimal() {
        assertEquals("3.0s", WeaponLoreLines.cooldownLabel(60));
    }

    // --- inputLabel: "left_click" -> "Left-Click". ---

    @Test
    void inputLabelLeftClick() {
        assertEquals("Left-Click", WeaponLoreLines.inputLabel("left_click"));
    }

    @Test
    void inputLabelRightClick() {
        assertEquals("Right-Click", WeaponLoreLines.inputLabel("right_click"));
    }

    // --- cadenceLine: cooldown, folded with the cost clause when costed. ---

    @Test
    void cadenceCooldownOnlyWhenFree() {
        assertEquals("Cooldown: 0.5s", WeaponLoreLines.cadenceLine(10, ResourceCost.FREE));
    }

    @Test
    void cadenceFoldsCostAfterCooldown() {
        assertEquals("Cooldown: 3.0s | Energy Cost: 40",
                WeaponLoreLines.cadenceLine(60, new ResourceCost("energy", 40)));
    }

    @Test
    void cadenceResourceNameComesFromResourceId() {
        assertEquals("Cooldown: 1.0s | Mana Cost: 30",
                WeaponLoreLines.cadenceLine(20, new ResourceCost("mana", 30)));
    }

    @Test
    void cadenceBlankWhenFreeAndInstant() {
        assertEquals("", WeaponLoreLines.cadenceLine(0, ResourceCost.FREE));
    }

    // --- attackSpeedLabel: ATTACKS PER SECOND (20/ticks), not seconds. Redden by changing the 20. ---

    @Test
    void attackSpeedIsAttacksPerSecondNotSeconds() {
        // 10 ticks between swings is TWO attacks a second. The number must be 2.0, not 0.5 --
        // higher is better here, the opposite of the cooldown lines.
        assertEquals("2.0", WeaponLoreLines.attackSpeedLabel(10));
    }

    @Test
    void attackSpeedRoundsToOneDecimal() {
        assertEquals("1.3", WeaponLoreLines.attackSpeedLabel(15));
    }

    @Test
    void attackSpeedOfAZeroCooldownIsBlankRatherThanInfinity() {
        // The guard that keeps a divide-by-zero off a player's tooltip; the caller drops the line.
        assertEquals("", WeaponLoreLines.attackSpeedLabel(0));
        assertEquals("", WeaponLoreLines.attackSpeedLabel(-5));
    }

    // --- triggerDamage: the number, its element, and WHERE IT CAME FROM. ---
    // The source discriminates a basic attack from an ability payload, which is what decides
    // whether the tooltip renders a stat block or an ability block. Redden by collapsing it.

    @Test
    void weaponDamageReadsTheWeaponAttackDamageAndIsAWeaponStat() {
        var onHit = List.<EffectSpec>of(new EffectSpec.WeaponDamage("kinetic"));

        var damage = WeaponLoreLines.triggerDamage(onHit, 8).orElseThrow();

        assertEquals(8, damage.amount());
        assertEquals("kinetic", damage.element());
        assertEquals(DamagePayload.DamageSource.WEAPON_STAT, damage.source(),
                "weapon_damage READS the attack-damage stat, so it is a basic attack");
    }

    @Test
    void literalDamageUsesItsOwnAmountAndIsAnAbilityLiteral() {
        var onHit = List.<EffectSpec>of(new EffectSpec.Damage(6, "fire"));

        var damage = WeaponLoreLines.triggerDamage(onHit, 99).orElseThrow();

        assertEquals(6, damage.amount(), "the literal wins; the weapon's attack damage is ignored");
        assertEquals("fire", damage.element());
        assertEquals(DamagePayload.DamageSource.ABILITY_LITERAL, damage.source(),
                "a literal reads no stat, so no class-typed modifier can reach it");
    }

    @Test
    void damageInsideABurstIsFoundWithItsElementAndSource() {
        // A cosmetic visual, then a burst carrying the damage -- the number must come from inside it,
        // and the element and source must survive the recursion rather than being lost on the way up.
        var onHit = List.<EffectSpec>of(
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Burst(3.0, List.of(
                        new EffectSpec.Damage(12, "fire"),
                        new EffectSpec.Status("scorch", 40, 0))));

        var damage = WeaponLoreLines.triggerDamage(onHit, 0).orElseThrow();

        assertEquals(12, damage.amount());
        assertEquals("fire", damage.element());
        assertEquals(DamagePayload.DamageSource.ABILITY_LITERAL, damage.source());
    }

    @Test
    void noDamageEffectYieldsEmpty() {
        var onHit = List.<EffectSpec>of(new EffectSpec.Heal(6));
        assertFalse(WeaponLoreLines.triggerDamage(onHit, 5).isPresent());
    }

    @Test
    void firstDamageBearingEffectWins() {
        var onHit = List.<EffectSpec>of(
                new EffectSpec.Damage(3, "fire"),
                new EffectSpec.Damage(99, "fire"));
        assertEquals(3, WeaponLoreLines.triggerDamage(onHit, 0).orElseThrow().amount());
    }
}
