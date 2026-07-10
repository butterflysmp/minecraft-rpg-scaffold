package io.github.butterflysmp.rpg.core.archetype;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every class the server knows about. */
public final class ArchetypeRegistry {
    private final Map<String, Archetype> byId = new LinkedHashMap<>();

    public void register(Archetype archetype) {
        if (byId.putIfAbsent(archetype.id(), archetype) != null) {
            throw new IllegalStateException("Duplicate archetype id: " + archetype.id());
        }
    }

    public Optional<Archetype> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<Archetype> all() {
        return java.util.Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
