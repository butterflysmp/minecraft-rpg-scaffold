package io.github.butterflysmp.rpg.core.archetype;

import java.util.List;

/**
 * One class, fully described. Constructed from YAML by the content loader in the
 * paper module -- core never reads files, and never learns what an archetype
 * "means"; it only carries the ids of the abilities this class grants.
 *
 * The set of elements is an enum (a design decision). The set of classes is
 * content, exactly like abilities -- so an Archetype is a data record, not a type.
 */
public record Archetype(
        String id,
        String displayName,
        List<String> abilityIds
) {
    public Archetype {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("archetype id required");
        abilityIds = List.copyOf(abilityIds); // defensive + immutable
    }
}
