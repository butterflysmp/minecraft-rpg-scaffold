package io.github.butterflysmp.rpg.core.mining;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Loaded vein sizes, by id, in load order. A duplicate id is refused. */
public final class VeinSizeRegistry {

    private final Map<String, VeinSize> byId = new LinkedHashMap<>();

    public void register(VeinSize size) {
        if (byId.putIfAbsent(size.id(), size) != null) {
            throw new IllegalStateException("Duplicate vein size id: " + size.id());
        }
    }

    public Optional<VeinSize> find(String id) { return Optional.ofNullable(byId.get(id)); }

    public Collection<VeinSize> all() { return Collections.unmodifiableCollection(byId.values()); }

    public int size() { return byId.size(); }
}
