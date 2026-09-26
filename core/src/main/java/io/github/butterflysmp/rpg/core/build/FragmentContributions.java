package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a cell's four fragment slots contribute to the stats, keyed {@code fragment:<slot>}
 * (PLAN-build-system.md section 1.6). The fragment parallel of {@code AccessoryContributions}: the reconcile
 * loop merges these into each stat's ONE desired map, beside gear and accessories -- never a second
 * reconcile call, which would wipe the first.
 *
 * <p>Values go through {@link AccessoryContributions#sourceValue}, the same per-stat conversion an
 * accessory's value takes, so a fragment and an accessory of the same amount move a stat the same way.
 */
public record FragmentContributions(Map<AccessoryStat, Map<String, Double>> byStat) {

    public static final FragmentContributions NONE = new FragmentContributions(Map.of());

    public FragmentContributions {
        Map<AccessoryStat, Map<String, Double>> copy = new EnumMap<>(AccessoryStat.class);
        byStat.forEach((stat, sources) -> copy.put(stat, Map.copyOf(sources)));
        byStat = Collections.unmodifiableMap(copy);
    }

    /**
     * From the four slots' definitions, in slot order; null is an empty slot. A fragment in two slots
     * contributes ONCE, from the first (ruling 11) -- only a hand-edited file can hold a duplicate.
     */
    public static FragmentContributions of(List<FragmentDefinition> slots) {
        if (slots.size() != FragmentSlots.COUNT) {
            throw new IllegalArgumentException("expected " + FragmentSlots.COUNT + " fragment slots, got " + slots.size());
        }
        Map<AccessoryStat, Map<String, Double>> byStat = new EnumMap<>(AccessoryStat.class);
        Set<String> seen = new HashSet<>();
        for (int slot = 0; slot < FragmentSlots.COUNT; slot++) {
            FragmentDefinition fragment = slots.get(slot);
            if (fragment == null || !seen.add(fragment.id())) continue;
            String key = FragmentSlots.sourceKey(slot);
            for (Map.Entry<AccessoryStat, Double> modifier : fragment.modifiers().entrySet()) {
                byStat.computeIfAbsent(modifier.getKey(), s -> new HashMap<>())
                        .put(key, AccessoryContributions.sourceValue(modifier.getKey(), modifier.getValue()));
            }
        }
        return new FragmentContributions(byStat);
    }

    public Map<String, Double> sources(AccessoryStat stat) {
        return byStat.getOrDefault(stat, Map.of());
    }
}
