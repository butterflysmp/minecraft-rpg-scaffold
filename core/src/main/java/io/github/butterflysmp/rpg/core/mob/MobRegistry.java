package io.github.butterflysmp.rpg.core.mob;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every custom mob the server knows about. */
public final class MobRegistry {
    private final Map<String, MobDefinition> byId = new LinkedHashMap<>();

    public void register(MobDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate mob id: " + def.id());
        }
    }

    public Optional<MobDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<MobDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
