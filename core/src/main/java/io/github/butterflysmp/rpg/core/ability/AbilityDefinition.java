package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.element.Element;
import java.util.List;

/**
 * One ability, fully described. Constructed from YAML by the content loader
 * in the paper module -- core never reads files.
 */
public record AbilityDefinition(
        String id,
        String displayName,
        Element element,
        String archetypeId,
        int cooldownTicks,
        ResourceCost cost,
        CastSpec cast,
        List<EffectSpec> onHit
) {
    public AbilityDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ability id required");
        onHit = List.copyOf(onHit);
    }
}
