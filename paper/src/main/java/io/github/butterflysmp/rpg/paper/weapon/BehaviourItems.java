package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.ActiveEnchant;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * THE BEHAVIOURS A WEAPON'S ENCHANTS GIVE IT (the seat's B2, 2026-11-23): every enchant that is the ACTIVE candidate of
 * its slot at level 1 or more ({@link EnchantState#effective()}, which already resolves a repeat to its highest level) and
 * whose definition declares a behaviour. A stat enchant, an unknown id, and a locked or inactive candidate give nothing.
 * Pure over its inputs, so a test can hold it; {@code PaperCombatWorld.behavioursOf} reads the held item into it.
 *
 * <p><b>THE WEAPON FILTER (the seat's ruling recorded 2026-11-24, PLAN-enchant-columns.md section 16; built in the
 * brief of that day, PR 1).</b> A behaviour whose enchant names a weapon ({@code weapon:}, W1) is handed on ONLY when the
 * item it is read from IS that weapon. Enforced ONCE, here, for every behaviour type: every cast's read (the held
 * weapon, at commit, at a swing and per volley shot), the tooltip's ({@code WeaponItems.applyLore}) and a vanilla shot's
 * stamp all come through this method. No roll can put a weapon's enchant on another weapon (W2), but the dev command's
 * {@code candidate} op can stage any enchant in any slot, and without this Airburst Flames so staged on another
 * projectile weapon split its shot. A behaviour that names no weapon (a unique, element enchant) is untouched.
 */
public final class BehaviourItems {

    private BehaviourItems() {}

    /**
     * @param weaponId the id of the minted weapon the state was read from, or null when it is not one (then only
     *                 behaviours that name no weapon pass)
     */
    public static List<ActiveBehaviour> activeOf(EnchantState state, String weaponId,
                                                 Function<String, Optional<EnchantDefinition>> find) {
        if (state == null) return List.of();
        List<ActiveBehaviour> out = new ArrayList<>();
        for (ActiveEnchant active : state.effective()) {
            find.apply(active.enchantId())
                    .filter(EnchantDefinition::isBehavioural)
                    .filter(def -> def.weapon() == null || def.weapon().equals(weaponId))
                    .ifPresent(def -> out.add(new ActiveBehaviour(def.id(), active.level(), def.behaviour())));
        }
        return List.copyOf(out);
    }
}
