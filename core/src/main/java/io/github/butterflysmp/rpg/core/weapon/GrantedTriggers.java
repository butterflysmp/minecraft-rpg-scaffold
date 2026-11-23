package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * WHICH TRIGGER A GESTURE FIRES, ENCHANT-GRANTED ONES INCLUDED (Lapis Crystal's D1, 2026-11-23). ONE place answers it for
 * the firing path ({@code WeaponFire}) and the tooltip ({@code WeaponLore}), so the two cannot disagree.
 *
 * <ul>
 *   <li>The weapon's OWN binding for the input wins. The loader already refuses a grant on a gesture the weapon binds,
 *       so this only matters for content that slipped past it; it never lets an enchant replace the authored trigger.</li>
 *   <li>Otherwise an ACTIVE {@link EnchantBehaviour.GrantTrigger} for that input, and only one whose {@code weaponId} is
 *       THIS weapon's id: the same enchant state staged on another weapon (the dev command can) grants nothing.</li>
 * </ul>
 *
 * <p>{@code behaviours} is the held weapon's active list (B2: active candidate, level 1 or more), as
 * {@code CombatWorld.behavioursOf} reads it.
 */
public final class GrantedTriggers {

    private GrantedTriggers() {}

    public static Optional<TriggerBinding> resolve(WeaponDefinition weapon, String input, List<ActiveBehaviour> behaviours) {
        Optional<TriggerBinding> own = weapon.trigger(input);
        if (own.isPresent()) return own;
        return granted(weapon, behaviours).stream().filter(b -> b.input().equals(input)).findFirst();
    }

    /** Every trigger the active behaviours grant THIS weapon, in the order they are active; none it already binds. */
    public static List<TriggerBinding> granted(WeaponDefinition weapon, List<ActiveBehaviour> behaviours) {
        List<TriggerBinding> out = new ArrayList<>();
        if (behaviours == null) return out;
        for (ActiveBehaviour active : behaviours) {
            if (!(active.behaviour() instanceof EnchantBehaviour.GrantTrigger grant)) continue;
            if (!grant.weaponId().equals(weapon.id())) continue;
            if (weapon.trigger(grant.input()).isPresent()) continue;
            if (out.stream().anyMatch(b -> b.input().equals(grant.input()))) continue;
            out.add(new TriggerBinding(grant.input(), grant.ability()));
        }
        return out;
    }
}
