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
 * Pure over its two inputs, so a test can hold it; {@code PaperCombatWorld.behavioursOf} reads the held item into it.
 */
public final class BehaviourItems {

    private BehaviourItems() {}

    public static List<ActiveBehaviour> activeOf(EnchantState state, Function<String, Optional<EnchantDefinition>> find) {
        if (state == null) return List.of();
        List<ActiveBehaviour> out = new ArrayList<>();
        for (ActiveEnchant active : state.effective()) {
            find.apply(active.enchantId())
                    .filter(EnchantDefinition::isBehavioural)
                    .ifPresent(def -> out.add(new ActiveBehaviour(def.id(), active.level(), def.behaviour())));
        }
        return List.copyOf(out);
    }
}
