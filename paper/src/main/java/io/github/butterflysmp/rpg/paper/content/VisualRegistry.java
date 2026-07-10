package io.github.butterflysmp.rpg.paper.content;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every visual the server knows about. */
public final class VisualRegistry {
    private final Map<String, VisualDefinition> byId = new LinkedHashMap<>();

    public void register(VisualDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate visual id: " + def.id());
        }
    }

    public Optional<VisualDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<VisualDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
