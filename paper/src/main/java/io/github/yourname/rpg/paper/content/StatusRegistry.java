package io.github.yourname.rpg.paper.content;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every status the server knows about. */
public final class StatusRegistry {
    private final Map<String, StatusDefinition> byId = new LinkedHashMap<>();

    public void register(StatusDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate status id: " + def.id());
        }
    }

    public Optional<StatusDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<StatusDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }
}
