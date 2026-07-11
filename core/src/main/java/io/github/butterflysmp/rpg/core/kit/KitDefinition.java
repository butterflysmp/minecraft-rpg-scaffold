package io.github.butterflysmp.rpg.core.kit;

import java.util.List;

/**
 * One cell of the (class, element) grid, fully described. Constructed from YAML by the
 * content loader in paper -- core never reads files, and never learns what a class or an
 * element "means"; it only carries the composite key and the ids this cell grants.
 *
 * A kit generalizes Commit F's Archetype: an archetype granted abilities to a class alone,
 * a kit grants weapons AND abilities to a (class, element) pair. classId and elementId are
 * the two identity axes a player picks; together they are the kit's key.
 */
public record KitDefinition(
        String classId,
        String elementId,
        String displayName,
        List<WeaponGrant> weapons,
        List<String> abilityIds
) {
    public KitDefinition {
        if (classId == null || classId.isBlank()) throw new IllegalArgumentException("kit class required");
        if (elementId == null || elementId.isBlank()) throw new IllegalArgumentException("kit element required");
        weapons = List.copyOf(weapons);
        abilityIds = List.copyOf(abilityIds);
        if (weapons.isEmpty() && abilityIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "kit '" + classId + "/" + elementId + "' grants nothing");
        }
    }
}
