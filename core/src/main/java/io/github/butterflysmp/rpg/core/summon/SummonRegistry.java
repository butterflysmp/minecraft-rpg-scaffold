package io.github.butterflysmp.rpg.core.summon;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The loaded summons, by id ({@code content/summons/<id>.yml}). A second file with one id is refused. */
public final class SummonRegistry {

    private final Map<String, SummonDefinition> byId = new LinkedHashMap<>();

    public void register(SummonDefinition def) {
        if (byId.containsKey(def.id())) throw new IllegalArgumentException("summon '" + def.id() + "' is declared twice");
        byId.put(def.id(), def);
    }

    public Optional<SummonDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<SummonDefinition> all() {
        return java.util.List.copyOf(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
