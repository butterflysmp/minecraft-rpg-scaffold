package io.github.butterflysmp.rpg.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One player's saved loadout for ONE (class, element) cell (PLAN-build-system.md section 2.1). Ability,
 * aspect and fragment ids only -- never items -- so there is no encoded item and no opaque slot.
 *
 * <p>Every slot may be {@code null}, meaning empty. The list lengths are FIXED, and a wrong length is a
 * structural fault that refuses the whole file ({@code FileBuildRepository}): 2 actives, 2 aspects
 * (slice 5), 4 fragments (slice 4, ruling 4).
 *
 * <p><b>A duplicate is NOT a structural fault; it keeps the first and blanks the rest.</b> Ruling 11 (one
 * of each fragment) and the Build screen's swap rule (one ability in one Active slot) mean no gesture
 * produces a duplicate, so only a hand-edited file can -- and losing the other slots over it would be the
 * worse trade. The same rule for the actives and the aspects.
 *
 * <p>The lists hold nulls, so they are built with {@code Collections.unmodifiableList} rather than
 * {@code List.copyOf}, which refuses a null element.
 */
public record CellLoadout(
        String classId,
        String elementId,
        String ultimate,
        List<String> actives,
        List<String> aspects,
        List<String> fragments
) {
    public static final int ACTIVES = 2;
    public static final int ASPECTS = 2;
    public static final int FRAGMENTS = 4;

    public CellLoadout {
        if (classId == null || classId.isBlank()) throw new IllegalArgumentException("cell loadout class required");
        if (elementId == null || elementId.isBlank()) throw new IllegalArgumentException("cell loadout element required");
        actives = fixed(actives, ACTIVES, "actives", classId, elementId);
        aspects = fixed(aspects, ASPECTS, "aspects", classId, elementId);
        fragments = fixed(fragments, FRAGMENTS, "fragments", classId, elementId);
    }

    /** An empty loadout for a cell: every slot empty. */
    public static CellLoadout empty(String classId, String elementId) {
        return new CellLoadout(classId, elementId, null, null, null, null);
    }

    public CellLoadout withUltimate(String abilityId) {
        return new CellLoadout(classId, elementId, abilityId, actives, aspects, fragments);
    }

    /** @param index 0 for Active 1 (left click), 1 for Active 2 (right click) */
    public CellLoadout withActive(int index, String abilityId) {
        if (index < 0 || index >= ACTIVES) throw new IllegalArgumentException("active index " + index);
        List<String> next = new ArrayList<>(actives);
        next.set(index, abilityId);
        return new CellLoadout(classId, elementId, ultimate, next, aspects, fragments);
    }

    private static List<String> fixed(List<String> source, int size, String what, String classId, String elementId) {
        List<String> slots = new ArrayList<>(Collections.nCopies(size, (String) null));
        if (source != null) {
            if (source.size() != size) {
                throw new IllegalArgumentException("loadout " + classId + "/" + elementId + " has " + source.size()
                        + " " + what + "; exactly " + size + " slots are stored (empty ones as null)");
            }
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < size; i++) {
                String id = source.get(i);
                // Blank reads as empty; a duplicate keeps the FIRST and blanks the rest (ruling 11).
                if (id == null || id.isBlank() || !seen.add(id)) continue;
                slots.set(i, id);
            }
        }
        return Collections.unmodifiableList(slots);
    }

    /** True when this entry is for the given cell. Compared exactly -- nothing lowercases a cell id. */
    public boolean isFor(String classId, String elementId) {
        return Objects.equals(this.classId, classId) && Objects.equals(this.elementId, elementId);
    }
}
