package io.github.yourname.rpg.core.ability;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every ability the server knows about. */
public final class AbilityRegistry {
    private final Map<String, AbilityDefinition> byId = new LinkedHashMap<>();

    public void register(AbilityDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate ability id: " + def.id());
        }
    }

    public Optional<AbilityDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<AbilityDefinition> all() {
        return java.util.Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
