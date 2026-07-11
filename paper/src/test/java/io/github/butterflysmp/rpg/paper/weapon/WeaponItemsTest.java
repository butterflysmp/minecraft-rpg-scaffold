package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The vanilla-melee suppressor, tested at the DECISION rather than on a constructed item.
 *
 * An ItemStack cannot be built in a unit test -- new ItemStack(...) throws "No RegistryAccess
 * implementation found" without a running server, and the project has no MockBukkit. So the
 * item actually carrying the modifier, and the absence of a double-hit, are witnessed on the
 * real-server boot. What IS unit-testable, and what actually matters, is the decision: the
 * suppressor targets the VANILLA attack-damage path -- not the trigger's -- and zeroes it.
 *
 * "Zeroed the wrong damage path" is the failure that passes review and fails in the world:
 * a suppressor pointed at the trigger would leave vanilla melee intact (double-hit) while
 * looking correct. mint() derives the Bukkit Attribute from ATTACK_DAMAGE_ATTRIBUTE, so this
 * assertion governs what the item receives -- the constant and the item cannot drift.
 */
class WeaponItemsTest {

    @Test
    void theSuppressorTargetsVanillaAttackDamageNotTheTriggerPath() {
        // The vanilla melee attribute -- a separate path from the trigger, which flows
        // EffectSpec.Damage -> CombatantHandle.applyDamage and never touches an attribute.
        assertEquals("attack_damage", WeaponItems.ATTACK_DAMAGE_ATTRIBUTE);
    }

    @Test
    void theSuppressorZeroesTheBaseSwingRatherThanAddingToIt() {
        // A player's base attack_damage is 1.0; -1.0 brings a held swing to a flat 0.
        // Negative, not positive: it must REDUCE vanilla melee, never buff it.
        assertEquals(-1.0, WeaponItems.VANILLA_MELEE_SUPPRESSION, 1e-9);
        assertTrue(WeaponItems.VANILLA_MELEE_SUPPRESSION <= 0.0,
                "a suppressor must cancel vanilla melee, not add to it");
    }
}
