package io.github.butterflysmp.rpg.core.build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What EVERY cell of one class offers: {@code content/builds/<class>.yml}, merged into each
 * {@code <class>_<element>.yml} at load (PLAN-build-system.md section 7.1, ruling 24). Only abilities: fragments,
 * aspects and a default belong to a cell.
 *
 * <p><b>An id both here and in a cell, in the same role, is REFUSED, not de-duplicated.</b> One fact would have
 * two homes, and removing it from the class file would silently leave it offered to that one cell. An id in
 * different roles is refused by {@link PoolDefinition}'s own check, which runs on the merged lists.
 *
 * <p>A record: an immutable value whose fields are fixed at construction.
 */
public record ClassPool(String classId, List<String> ultimates, List<String> actives) {

    public ClassPool {
        if (classId == null || classId.isBlank()) throw new IllegalArgumentException("class pool needs a class");
        ultimates = ultimates == null ? List.of() : List.copyOf(ultimates);
        actives = actives == null ? List.of() : List.copyOf(actives);
        if (ultimates.isEmpty() && actives.isEmpty()) {
            throw new IllegalArgumentException("class pool " + classId + " offers nothing");
        }
        Set<String> seen = new HashSet<>();
        for (String id : ultimates) {
            if (!seen.add(id)) throw new IllegalArgumentException(where(classId) + "'" + id + "' listed twice");
        }
        for (String id : actives) {
            if (!seen.add(id)) {
                throw new IllegalArgumentException(where(classId) + "'" + id + "' listed twice, or in both roles");
            }
        }
    }

    /** The cell's ultimates, then this class's. Refuses an id in both. */
    public List<String> mergeUltimates(List<String> cell) {
        return merge("ultimates", cell, ultimates);
    }

    /** The cell's actives, then this class's. Refuses an id in both. */
    public List<String> mergeActives(List<String> cell) {
        return merge("actives", cell, actives);
    }

    private List<String> merge(String role, List<String> cell, List<String> mine) {
        List<String> merged = new ArrayList<>(cell);
        for (String id : mine) {
            if (cell.contains(id)) {
                throw new IllegalArgumentException("'" + id + "' is class-wide (" + classId + ".yml's " + role
                        + ") and also listed in this cell's " + role + " -- list it in one place");
            }
            merged.add(id);
        }
        return List.copyOf(merged);
    }

    private static String where(String classId) {
        return "class pool " + classId + ": ";
    }
}
