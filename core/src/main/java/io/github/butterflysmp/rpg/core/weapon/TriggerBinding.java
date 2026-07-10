package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;

/**
 * One input bound to one trigger. A trigger IS an AbilityDefinition -- the same
 * cast/cost/cooldown/effects grammar an ability uses -- plus the input that fires
 * it ("left_click", "right_click"). The ability's id is synthesized by the loader
 * as weaponId + "/" + input, so its cooldown keys on (player, weaponId, input):
 * a weapon's triggers cooldown independently, and two weapons that share ability
 * content do not share a cooldown.
 *
 * A List<TriggerBinding> rather than a fixed two-key map keeps the input set open:
 * a third input, or a right-click added later, needs no schema change.
 */
public record TriggerBinding(String input, AbilityDefinition ability) {
    public TriggerBinding {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("trigger input required");
        }
        if (ability == null) {
            throw new IllegalArgumentException("trigger ability required for input '" + input + "'");
        }
    }
}
