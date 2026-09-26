package io.github.butterflysmp.rpg.core.build;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable-after-load lookup of every pool, keyed on the composite {@link CellKey}.
 *
 * <p>Ruling 7 falls out of this: the cells a player can be are exactly the cells that have a pool.
 */
public final class PoolRegistry {

    private final Map<CellKey, PoolDefinition> byCell = new LinkedHashMap<>();

    public void register(PoolDefinition pool) {
        if (byCell.putIfAbsent(pool.cell(), pool) != null) {
            throw new IllegalStateException("Duplicate pool for "
                    + pool.cell().classId() + "/" + pool.cell().elementId());
        }
    }

    public Optional<PoolDefinition> find(String classId, String elementId) {
        if (classId == null || elementId == null || classId.isBlank() || elementId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCell.get(new CellKey(classId, elementId)));
    }

    public Collection<PoolDefinition> all() {
        return Collections.unmodifiableCollection(byCell.values());
    }

    public int size() { return byCell.size(); }
}
