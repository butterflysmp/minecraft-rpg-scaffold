package io.github.butterflysmp.rpg.paper.content;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every element the server knows about. */
public final class ElementRegistry {
    private final Map<String, ElementDefinition> byId = new LinkedHashMap<>();

    public void register(ElementDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate element id: " + def.id());
        }
    }

    public Optional<ElementDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<ElementDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
