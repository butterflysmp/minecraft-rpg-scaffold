package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import java.util.List;

/**
 * One ability, fully described. Constructed from YAML by the content loader
 * in the paper module -- core never reads files.
 */
public record AbilityDefinition(
        String id,
        String displayName,
        String element,
        String archetypeId,
        int cooldownTicks,
        ResourceCost cost,
        CastSpec cast,
        List<EffectSpec> onHit,
        List<String> description
) {
    public AbilityDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ability id required");
        onHit = List.copyOf(onHit);
        // Optional authored prose for the tooltip (weapon triggers carry it; standalone abilities
        // leave it empty). Absent -> empty, never null.
        description = description == null ? List.of() : List.copyOf(description);
    }

    /** An ability with no authored description: standalone abilities and the older test/call sites. */
    public AbilityDefinition(String id, String displayName, String element, String archetypeId,
                             int cooldownTicks, ResourceCost cost, CastSpec cast, List<EffectSpec> onHit) {
        this(id, displayName, element, archetypeId, cooldownTicks, cost, cast, onHit, List.of());
    }
}
