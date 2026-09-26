package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An aspect: it changes ONE ability, its {@code target}, by ADDING behaviour (ruling 3) and, under the
 * whitelist, by changing numbers the target already has (ruling 9). PLAN-build-system.md sections 2.4 and
 * 2.4.1. Content, from {@code content/aspects/<id>.yml}.
 *
 * <ul>
 *   <li>{@code addOnHit}: effects appended to the target's {@code on_hit} -- any effect an ability may carry;</li>
 *   <li>{@code addOnCast}: visuals appended to the target's {@code on_cast};</li>
 *   <li>{@code modify}: whitelisted number changes to the target's OWN effects ({@link AspectField}).</li>
 * </ul>
 *
 * <p>A record: an immutable value whose fields are fixed at construction.
 */
public record AspectDefinition(String id, String displayName, List<String> description, String target,
                               List<EffectSpec> addOnHit, List<EffectSpec.Visual> addOnCast,
                               List<NumberChange> modify) {

    public AspectDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("aspect id required");
        if (target == null || target.isBlank()) throw new IllegalArgumentException("aspect " + id + " needs a target");
        if (displayName == null || displayName.isBlank()) displayName = id;
        description = description == null ? List.of() : List.copyOf(description);
        addOnHit = addOnHit == null ? List.of() : List.copyOf(addOnHit);
        addOnCast = addOnCast == null ? List.of() : List.copyOf(addOnCast);
        modify = modify == null ? List.of() : List.copyOf(modify);
        if (addOnHit.isEmpty() && addOnCast.isEmpty() && modify.isEmpty()) {
            throw new IllegalArgumentException("aspect " + id + " adds and changes nothing");
        }
        Set<AspectField> seen = new HashSet<>();
        for (NumberChange change : modify) {
            if (!seen.add(change.field())) {
                throw new IllegalArgumentException("aspect " + id + " modifies " + change.field().token()
                        + " twice; write one entry with both flat and percent");
            }
        }
    }
}
