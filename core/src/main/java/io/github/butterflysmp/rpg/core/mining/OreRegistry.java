package io.github.butterflysmp.rpg.core.mining;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Loaded ores, by id, in load order. A duplicate id is refused. */
public final class OreRegistry {

    private final Map<String, OreDefinition> byId = new LinkedHashMap<>();

    public void register(OreDefinition ore) {
        if (byId.putIfAbsent(ore.id(), ore) != null) {
            throw new IllegalStateException("Duplicate ore id: " + ore.id());
        }
    }

    public Optional<OreDefinition> find(String id) { return Optional.ofNullable(byId.get(id)); }

    public Collection<OreDefinition> all() { return Collections.unmodifiableCollection(byId.values()); }

    public int size() { return byId.size(); }
}
