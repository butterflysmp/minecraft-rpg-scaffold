package io.github.butterflysmp.rpg.core.kit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable-after-load lookup for every kit the server knows about.
 *
 * Keyed on the COMPOSITE (classId, elementId) -- a record, never a concatenated string.
 * Deriving the pair by splitting "ranger_fire" invites the ("ranger_f","ire") vs
 * ("ranger","fire") collision; a two-field key cannot collide. Same care the cooldown key
 * got in 1a: free now, a debugging session later.
 */
public final class KitRegistry {

    /** The composite key. A record, so equals/hashCode are the pair, structurally. */
    public record KitKey(String classId, String elementId) {}

    private final Map<KitKey, KitDefinition> byKey = new LinkedHashMap<>();

    public void register(KitDefinition def) {
        KitKey key = new KitKey(def.classId(), def.elementId());
        if (byKey.putIfAbsent(key, def) != null) {
            throw new IllegalStateException("Duplicate kit for " + def.classId() + "/" + def.elementId());
        }
    }

    public Optional<KitDefinition> find(String classId, String elementId) {
        return Optional.ofNullable(byKey.get(new KitKey(classId, elementId)));
    }

    public Collection<KitDefinition> all() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    /** The distinct classes any kit offers -- for /rpg class tab completion. */
    public Set<String> classes() {
        Set<String> ids = new LinkedHashSet<>();
        for (KitKey key : byKey.keySet()) ids.add(key.classId());
        return Collections.unmodifiableSet(ids);
    }

    public int size() { return byKey.size(); }
}
