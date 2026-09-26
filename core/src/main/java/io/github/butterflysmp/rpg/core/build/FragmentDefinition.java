package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment: a small stat bonus slotted into one of a cell's four fragment slots (PLAN-build-system.md
 * section 2.3). Content, from {@code content/fragments/<id>.yml}. It is never an item: {@code icon} names a
 * material for the Build screen's icon and nothing else.
 *
 * <p><b>POSITIVE-ONLY (ruling 21).</b> Every modifier must be a finite amount above zero. Only a negative
 * can carry a stat to its floor, so a fragment adds no negative source to the combined bound
 * ({@link StatSourceBound}), and the shipped accessories' drawbacks keep the bound they were ruled under.
 *
 * <p><b>{@code class_damage} is refused</b> (section 2.3): a fragment is already class-scoped by its pool,
 * so it would be the same number arriving by a second door.
 *
 * <p>A record: an immutable value type, whose fields are fixed at construction.
 */
public record FragmentDefinition(String id, String displayName, String icon, List<String> description,
                                 Map<AccessoryStat, Double> modifiers) {

    public FragmentDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("fragment id required");
        if (displayName == null || displayName.isBlank()) displayName = id;
        description = description == null ? List.of() : List.copyOf(description);
        if (modifiers == null || modifiers.isEmpty()) {
            throw new IllegalArgumentException("fragment " + id + " modifies nothing");
        }
        Map<AccessoryStat, Double> copy = new EnumMap<>(AccessoryStat.class);
        for (Map.Entry<AccessoryStat, Double> e : modifiers.entrySet()) {
            AccessoryStat stat = e.getKey();
            double amount = e.getValue();
            if (stat == AccessoryStat.CLASS_DAMAGE) {
                throw new IllegalArgumentException("fragment " + id + ": class_damage is refused on a fragment"
                        + " (a fragment is already class-scoped by its pool)");
            }
            if (!(amount > 0) || Double.isInfinite(amount)) {
                throw new IllegalArgumentException("fragment " + id + ": " + stat.token() + " is " + amount
                        + " -- fragments are positive-only (ruling 21): a finite amount above zero");
            }
            copy.put(stat, amount);
        }
        modifiers = Collections.unmodifiableMap(copy);
    }
}
