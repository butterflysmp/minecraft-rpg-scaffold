package io.github.butterflysmp.rpg.core.weapon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable-after-load lookup for every armor piece the server knows about.
 *
 * A third registry beside {@link WeaponRegistry} and {@link ShieldRegistry}, for the reason the
 * shield one records: the three hold different record types, and one gear registry would hand back
 * something every caller downcasts. Three registries plus a resolve-order at the one place that
 * needs all three -- {@code /rpg give} -- is the smaller cost.
 *
 * <p>That give path is also why the boot now warns on an id shared across ANY of the three: nothing
 * in here can see the other two to reject it, and a shadowed id looks exactly like a piece that
 * failed to load.
 */
public final class ArmorRegistry {
    private final Map<String, ArmorDefinition> byId = new LinkedHashMap<>();

    public void register(ArmorDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate armor id: " + def.id());
        }
    }

    public Optional<ArmorDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<ArmorDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
