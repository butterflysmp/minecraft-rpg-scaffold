package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * B3 (the seat, 2026-11-25): an active set's bonuses as desired-source maps, ready to MERGE into each stat's existing
 * single reconcile -- the shape {@code LevelBonus} and {@link AccessoryContributions} already have. No second
 * accumulator: the stat paths are the ones gear and accessories use, and the set is one more source on each.
 *
 * <p><b>ONE SOURCE KEY PER SET</b>, {@code armorset:<tier>}, so a set coming off is ABSENT from the next pass and the
 * reconciler drops it (removal by absence, the tick the set is no longer worn). The prefix is disjoint from every other
 * (see {@code AccessorySourceKeysTest}).
 *
 * <p>{@code max_summons} is not a {@code Stat}: it is the {@code MaxSummons} gear-grants term, read at the cast (B6), so
 * it is carried as a number for that read rather than as a source map.
 */
public record ArmorSetContributions(Map<AccessoryStat, Map<String, Double>> byStat, int maxSummons,
                                    Map<String, Double> quiverPercent) {

    public static final String SOURCE_PREFIX = "armorset:";

    public static final ArmorSetContributions NONE = new ArmorSetContributions(Map.of(), 0, Map.of());

    public ArmorSetContributions {
        Map<AccessoryStat, Map<String, Double>> copy = new EnumMap<>(AccessoryStat.class);
        byStat.forEach((stat, sources) -> copy.put(stat, Map.copyOf(sources)));
        byStat = Collections.unmodifiableMap(copy);
        quiverPercent = Map.copyOf(quiverPercent);
    }

    public static String sourceKey(String setId) {
        return SOURCE_PREFIX + setId;
    }

    public static ArmorSetContributions of(Optional<ArmorSetDecision.ActiveSet> active) {
        if (active.isEmpty()) return NONE;
        String key = sourceKey(active.get().id());
        Map<AccessoryStat, Map<String, Double>> byStat = new EnumMap<>(AccessoryStat.class);
        int summons = 0;
        Map<String, Double> quiverPercent = new java.util.HashMap<>();
        for (ArmorSetBonus bonus : active.get().bonuses()) {
            AccessoryStat stat = switch (bonus.type()) {
                case HEALTH_REGEN -> AccessoryStat.HEALTH_REGEN;
                case MANA_REGEN -> AccessoryStat.MANA_REGEN;
                case MAX_HEALTH -> AccessoryStat.MAX_HEALTH;
                case DEFENSE -> AccessoryStat.DEFENSE;
                case MAX_SUMMONS, QUIVER_SIZE_PERCENT -> null;
            };
            if (bonus.type() == ArmorSetBonus.Type.QUIVER_SIZE_PERCENT) {
                // Q1: the quiver's own percent term, under the same one key, merged nowhere else.
                quiverPercent.merge(key, bonus.engineValue(), Double::sum);
                continue;
            }
            if (stat == null) {
                summons += (int) bonus.engineValue();
                continue;
            }
            // Through the same per-stat seam an accessory's modifier goes through (HealthRegen.contribution and
            // ManaRegen.contribution for the two rates), so a future cap or curve on either reaches the set too.
            byStat.computeIfAbsent(stat, s -> new java.util.HashMap<>())
                    .merge(key, AccessoryContributions.sourceValue(stat, bonus.engineValue()), Double::sum);
        }
        return new ArmorSetContributions(byStat, summons, quiverPercent);
    }

    public Map<String, Double> sources(AccessoryStat stat) {
        return byStat.getOrDefault(stat, Map.of());
    }

    /** B6: the grant for {@code MaxSummons.effective}'s gear term; empty when the set grants none. */
    public List<Integer> summonGrants() {
        return maxSummons == 0 ? List.of() : List.of(maxSummons);
    }
}
