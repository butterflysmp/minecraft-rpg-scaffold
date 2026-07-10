package io.github.butterflysmp.rpg.core.weapon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every weapon the server knows about. */
public final class WeaponRegistry {
    private final Map<String, WeaponDefinition> byId = new LinkedHashMap<>();

    public void register(WeaponDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate weapon id: " + def.id());
        }
    }

    public Optional<WeaponDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<WeaponDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
