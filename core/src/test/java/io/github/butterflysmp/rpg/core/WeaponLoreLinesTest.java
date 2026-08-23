package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

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

    // --- triggerDamage: the weapon's static number, recursing into a Burst. ---

    @Test
    void weaponDamageReadsTheWeaponAttackDamage() {
        var onHit = List.<EffectSpec>of(new EffectSpec.WeaponDamage("kinetic"));
        assertEquals(OptionalDouble.of(8), WeaponLoreLines.triggerDamage(onHit, 8));
    }

    @Test
    void literalDamageUsesItsOwnAmount() {
        var onHit = List.<EffectSpec>of(new EffectSpec.Damage(6, "fire"));
        assertEquals(OptionalDouble.of(6), WeaponLoreLines.triggerDamage(onHit, 0));
    }

    @Test
    void damageInsideABurstIsFound() {
        // A cosmetic visual, then a burst carrying the damage -- the number must come from inside it.
        var onHit = List.<EffectSpec>of(
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Burst(3.0, List.of(
                        new EffectSpec.Damage(12, "fire"),
                        new EffectSpec.Status("scorch", 40, 0))));
        assertEquals(OptionalDouble.of(12), WeaponLoreLines.triggerDamage(onHit, 0));
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
        assertEquals(OptionalDouble.of(3), WeaponLoreLines.triggerDamage(onHit, 0));
        assertTrue(true);
    }
}
