package io.github.butterflysmp.rpg.core.weapon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable-after-load lookup for every shield the server knows about.
 *
 * Deliberately a separate registry from {@link WeaponRegistry} rather than one gear registry keyed
 * by type: the two hold different record types, and a shared registry would have to hand back
 * something every caller downcasts. Two registries plus a resolve-order at the one place that needs
 * both -- {@code /rpg give} -- is the smaller cost. That give path is also why the boot warns on an
 * id shared between the two: nothing in here can see the other registry to reject it.
 */
public final class ShieldRegistry {
    private final Map<String, ShieldDefinition> byId = new LinkedHashMap<>();

    public void register(ShieldDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate shield id: " + def.id());
        }
    }

    public Optional<ShieldDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<ShieldDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
