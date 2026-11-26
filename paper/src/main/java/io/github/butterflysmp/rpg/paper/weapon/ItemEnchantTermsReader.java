package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.ItemEnchantTerms;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * THE EFFECTIVE ITEM'S ENCHANT TERMS, READ OFF ONE STACK'S STATE (true tooltips, the seat's T1 to T3, 2026-11-26).
 *
 * <p>Every term comes from the resolver the ENGINE uses for the same quantity, and from nothing written for the lore:
 * <ul>
 *   <li>behaviours: {@link BehaviourItems#activeOf}, what {@code PaperCombatWorld.behavioursOf} hands a press;</li>
 *   <li>damage percent: {@link DamageEnchantItems#percentOn}, the grants and class gate the stat reconcile uses;</li>
 *   <li>crit points: {@link EnchantValues#totalFor} on {@code CRIT_CHANCE}, as {@code CritChanceEnchantItems} sums them.</li>
 * </ul>
 * ACTIVE only (T3): each reads {@link EnchantState#effective()}, so an inactive, locked or level-0 candidate changes
 * nothing, and one enchant acts per slot. Pure over plain values: no stack, no player, so a test holds it.
 */
public final class ItemEnchantTermsReader {

    private ItemEnchantTermsReader() {}

    public static ItemEnchantTerms of(EnchantState state, WeaponDefinition weapon, EnchantRegistry enchants) {
        if (state == null || weapon == null || enchants == null || state.effective().isEmpty()) return ItemEnchantTerms.NONE;
        List<ActiveBehaviour> behaviours = BehaviourItems.activeOf(state, weapon.id(), enchants::find);
        Map<String, List<String>> prose = new HashMap<>();
        for (ActiveBehaviour active : behaviours) {
            enchants.find(active.enchantId())
                    .filter(def -> !def.abilityDescription().isEmpty())
                    .ifPresent(def -> prose.put(def.id(), def.abilityDescription()));
        }
        return new ItemEnchantTerms(behaviours,
                DamageEnchantItems.percentOn(state, weapon, enchants),
                EnchantValues.totalFor(state, enchants, EnchantEffect.CRIT_CHANCE),
                prose);
    }
}
