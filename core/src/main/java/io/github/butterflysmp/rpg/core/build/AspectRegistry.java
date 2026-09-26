package io.github.butterflysmp.rpg.core.build;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Every loaded aspect, by id. A duplicate id is refused, as the other registries refuse one. */
public final class AspectRegistry {

    private final Map<String, AspectDefinition> byId = new LinkedHashMap<>();

    public void register(AspectDefinition aspect) {
        if (byId.putIfAbsent(aspect.id(), aspect) != null) {
            throw new IllegalStateException("Duplicate aspect id: " + aspect.id());
        }
    }

    public Optional<AspectDefinition> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Collection<AspectDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
