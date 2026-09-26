package io.github.butterflysmp.rpg.core.build;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Every loaded fragment, by id. A duplicate id is refused, as the other registries refuse one. */
public final class FragmentRegistry {

    private final Map<String, FragmentDefinition> byId = new LinkedHashMap<>();

    public void register(FragmentDefinition fragment) {
        if (byId.putIfAbsent(fragment.id(), fragment) != null) {
            throw new IllegalStateException("Duplicate fragment id: " + fragment.id());
        }
    }

    public Optional<FragmentDefinition> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Collection<FragmentDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
