package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;

import java.util.List;

/** Test access to {@link BehaviourBlocks}' package-private switch, for a test outside this package (ClusterBombsTest). */
public final class BehaviourBlocksAccess {

    private BehaviourBlocksAccess() {}

    /** The rows {@code active} gives {@code ability} on a weapon whose only trigger it is; empty when it does not change it. */
    public static List<BehaviourBlocks.DamageRow> rows(AbilityDefinition ability, ActiveBehaviour active) {
        String weaponId = ability.id().substring(0, ability.id().indexOf('/'));
        var weapon = new WeaponDefinition(weaponId, weaponId, ability.element(), Rarity.UNCOMMON,
                List.of(new TriggerBinding("right_click", ability)));
        return BehaviourBlocks.rowsFor(weapon, ability, active, List.of(ability)).orElse(List.of());
    }
}
