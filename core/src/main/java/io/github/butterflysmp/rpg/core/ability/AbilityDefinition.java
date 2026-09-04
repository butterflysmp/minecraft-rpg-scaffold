package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import java.util.List;

/**
 * One ability, fully described. Constructed from YAML by the content loader
 * in the paper module -- core never reads files.
 *
 * <p>{@code onCast} is what fires the MOMENT the cast is committed, at the aim's origin, as
 * against {@code onHit} which fires where the cast RESOLVES. For a self, melee or dash cast the
 * two are near enough the same instant that the repo has always faked a cast visual by putting an
 * untargeted one in {@code onHit} -- {@code rekindle.yml} and {@code ability_stone.yml} both do,
 * and it works because a Dash's untargeted effects fire once at the origin whether or not it
 * catches anyone. A PROJECTILE is where that idiom breaks: its {@code onHit} fires at the impact
 * point, after flight, so there was nowhere to hang "you hear the staff fire". This is that place.
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
        List<String> description,
        List<EffectSpec.Visual> onCast
) {
    public AbilityDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ability id required");
        onHit = List.copyOf(onHit);
        // Optional authored prose for the tooltip (weapon triggers carry it; standalone abilities
        // leave it empty). Absent -> empty, never null.
        description = description == null ? List.of() : List.copyOf(description);
        onCast = onCast == null ? List.of() : List.copyOf(onCast);
    }

    /**
     * An ability with nothing on cast -- every call site that predates the hook.
     *
     * <p>{@code onCast} is the tail of the ladder {@code description} started: each convenience
     * constructor drops the LAST field, so a reader counting arguments never has to work out which
     * middle one was omitted. That is the whole reason the component is appended rather than
     * placed beside {@code onHit}, where it belongs by meaning.
     */
    public AbilityDefinition(String id, String displayName, String element, String archetypeId,
                             int cooldownTicks, ResourceCost cost, CastSpec cast,
                             List<EffectSpec> onHit, List<String> description) {
        this(id, displayName, element, archetypeId, cooldownTicks, cost, cast, onHit, description,
                List.of());
    }

    /** An ability with no authored description: standalone abilities and the older test/call sites. */
    public AbilityDefinition(String id, String displayName, String element, String archetypeId,
                             int cooldownTicks, ResourceCost cost, CastSpec cast, List<EffectSpec> onHit) {
        this(id, displayName, element, archetypeId, cooldownTicks, cost, cast, onHit, List.of());
    }
}
