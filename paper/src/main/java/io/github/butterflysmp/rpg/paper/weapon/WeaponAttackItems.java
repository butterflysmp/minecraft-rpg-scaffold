package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading a player's main-hand weapon attack-damage as a reconcile "desired" modifier -- the attack
 * analogue of {@link io.github.butterflysmp.rpg.paper.health.HealthModifierItems}. The held weapon's
 * declared {@code attack_damage} becomes a single MAIN_HAND modifier on the player's ATTACK_DAMAGE
 * stat, which the basic melee hit (a {@code weapon_damage} effect) reads back. This is the real-weapon
 * replacement HealthModifierItems foretold: the same slot-keyed, diff-converged lifecycle, now sourcing
 * a stat from actual content instead of a _TEMP fixture.
 *
 * Keyed by the MAIN_HAND slot, not by weapon: one slot holds one item, so it is a single stable source
 * that add/replaces/removes cleanly as the hand's contents change -- and whatever route the weapon left
 * by (swap, drop, break, die, /clear) it is simply absent from the desired map next scan, so no
 * departure event can leak the modifier. A weapon with {@code attack_damage} 0 (a bow, a staff -- no
 * melee) contributes nothing: weapon-only melee, and unarmed deals nothing.
 */
public final class WeaponAttackItems {

    private WeaponAttackItems() {}

    /** The single source key the main-hand weapon's attack contributes under. */
    public static final String MAIN_HAND_SOURCE = "MAIN_HAND";

    /**
     * The attack-damage modifier the player's main-hand weapon justifies RIGHT NOW, keyed by
     * {@link #MAIN_HAND_SOURCE}, or an empty map if the hand holds no weapon of ours (or one with no
     * melee). This is the "desired" set the attack reconciler converges to.
     */
    public static Map<String, Double> desiredAttackModifiers(Player player, Keys keys, WeaponRegistry weapons) {
        Map<String, Double> desired = new HashMap<>();
        WeaponItems.heldWeaponId(player, keys)
                .flatMap(weapons::find)
                .filter(weapon -> weapon.attackDamage() > 0)
                .ifPresent(weapon -> desired.put(MAIN_HAND_SOURCE, weapon.attackDamage()));
        return desired;
    }
}
