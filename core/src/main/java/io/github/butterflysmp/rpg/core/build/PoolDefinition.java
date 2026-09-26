package io.github.butterflysmp.rpg.core.build;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything one cell OFFERS: its Ultimates and Actives, and the loadout a player of that cell casts
 * until they choose their own (PLAN-build-system.md section 2.2).
 *
 * <p>Ultimate-ness is a property of THIS LISTING, not of the ability file: the same ability could be an
 * Active here and absent from another cell. Fragments joined in slice 4 ({@link #fragments}) and aspects in
 * slice 5 ({@link #aspects}); either may be empty, which is simply empty slots.
 *
 * <p>The checks here are the ones that need no registry. Whether each id exists, and whether each aspect's
 * target is one of this pool's abilities, is the loader's question, answered after the content loads.
 */
public record PoolDefinition(
        CellKey cell,
        String displayName,
        List<String> ultimates,
        List<String> actives,
        Loadout defaultLoadout,
        List<String> fragments,
        List<String> aspects
) {
    /** A pool with no fragments or aspects -- every pool before slice 4, and most test fixtures. */
    public PoolDefinition(CellKey cell, String displayName, List<String> ultimates, List<String> actives,
                          Loadout defaultLoadout) {
        this(cell, displayName, ultimates, actives, defaultLoadout, List.of(), List.of());
    }

    /** A pool with fragments and no aspects -- slice 4's form. */
    public PoolDefinition(CellKey cell, String displayName, List<String> ultimates, List<String> actives,
                          Loadout defaultLoadout, List<String> fragments) {
        this(cell, displayName, ultimates, actives, defaultLoadout, fragments, List.of());
    }

    public PoolDefinition {
        if (cell == null) throw new IllegalArgumentException("pool cell required");
        if (defaultLoadout == null) throw new IllegalArgumentException(where(cell) + "default loadout required");
        ultimates = List.copyOf(ultimates);
        actives = List.copyOf(actives);
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        requireNoDuplicates(cell, "fragments", fragments);
        aspects = aspects == null ? List.of() : List.copyOf(aspects);
        requireNoDuplicates(cell, "aspects", aspects);
        // At least enough to fill a loadout, or the Build screen and the stone have nothing to offer.
        if (ultimates.isEmpty()) throw new IllegalArgumentException(where(cell) + "needs at least 1 ultimate");
        if (actives.size() < 2) throw new IllegalArgumentException(where(cell) + "needs at least 2 actives");
        requireNoDuplicates(cell, "ultimates", ultimates);
        requireNoDuplicates(cell, "actives", actives);
        for (String id : ultimates) {
            if (actives.contains(id)) {
                throw new IllegalArgumentException(where(cell) + "'" + id + "' is both an ultimate and an active");
            }
        }
        if (!ultimates.contains(defaultLoadout.ultimate())) {
            throw new IllegalArgumentException(where(cell) + "default ultimate '"
                    + defaultLoadout.ultimate() + "' is not in its ultimates");
        }
        for (String id : List.of(defaultLoadout.active1(), defaultLoadout.active2())) {
            if (!actives.contains(id)) {
                throw new IllegalArgumentException(where(cell) + "default active '" + id + "' is not in its actives");
            }
        }
    }

    private static void requireNoDuplicates(CellKey cell, String list, List<String> ids) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) throw new IllegalArgumentException(where(cell) + "'" + id + "' listed twice in " + list);
        }
    }

    private static String where(CellKey cell) {
        return cell == null ? "pool: " : "pool " + cell.classId() + "/" + cell.elementId() + ": ";
    }
}
