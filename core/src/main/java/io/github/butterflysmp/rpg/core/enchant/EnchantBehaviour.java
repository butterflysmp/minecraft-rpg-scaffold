package io.github.butterflysmp.rpg.core.enchant;

import java.util.List;

/**
 * WHAT A BEHAVIOURAL ENCHANT DOES (the seat's B1, 2026-11-23): an effect type the engine knows, with its parameters, the
 * per-level ones as lists, all declared in the enchant's YAML. No weapon id and no enchant id is written in Java; the
 * loader ({@code EnchantLoader}) builds one of these from a file's block and refuses a malformed one by name.
 *
 * <p>A {@code sealed} interface: only the records listed in {@code permits} may implement it, so a {@code switch} over
 * it must answer for every kind (one line for a reader rusty at Java: the compiler checks the list is complete).
 */
public sealed interface EnchantBehaviour permits EnchantBehaviour.Shockwave {

    /**
     * {@code effect: shockwave}: when the weapon carrying it lands {@code hits} hits on ONE target within the last
     * {@code windowTicks}, and the player's own shockwave cooldown ({@code cooldownTicks}) is clear, a shockwave leaves
     * that target's position and damages every mob within the level's radius (the struck one included), as SECONDARY
     * damage through the same damage path as an ability hit. Per level: {@code damageByLevel} and {@code radiusByLevel},
     * each exactly one entry per level.
     *
     * @param element the damage's element
     * @param visual  a visual id presented once at the shockwave's origin, or null for none
     */
    record Shockwave(int hits, int windowTicks, int cooldownTicks, List<Double> damageByLevel,
                     List<Double> radiusByLevel, String element, String visual) implements EnchantBehaviour {
        public Shockwave {
            if (hits < 1) throw new IllegalArgumentException("shockwave hits must be 1 or more, was " + hits);
            if (windowTicks < 1) throw new IllegalArgumentException("shockwave window_ticks must be 1 or more, was " + windowTicks);
            if (cooldownTicks < 0) throw new IllegalArgumentException("shockwave cooldown_ticks must be 0 or more, was " + cooldownTicks);
            damageByLevel = positive("damage_by_level", damageByLevel);
            radiusByLevel = positive("radius_by_level", radiusByLevel);
            if (damageByLevel.size() != radiusByLevel.size()) {
                throw new IllegalArgumentException("shockwave damage_by_level has " + damageByLevel.size()
                        + " entries and radius_by_level " + radiusByLevel.size() + "; one each per level");
            }
            if (element == null || element.isBlank()) throw new IllegalArgumentException("shockwave element is required");
            if (visual != null && visual.isBlank()) visual = null;
        }

        /** The damage at {@code level} (1-based), clamped into the authored levels. */
        public double damageAt(int level) {
            return damageByLevel.get(clamp(level, damageByLevel.size()) - 1);
        }

        /** The radius at {@code level} (1-based), clamped into the authored levels. */
        public double radiusAt(int level) {
            return radiusByLevel.get(clamp(level, radiusByLevel.size()) - 1);
        }

        /** How many levels it authors. */
        public int levels() {
            return damageByLevel.size();
        }
    }

    private static List<Double> positive(String key, List<Double> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(key + " is required: one value per level");
        }
        for (int i = 0; i < values.size(); i++) {
            Double v = values.get(i);
            if (v == null || !(v > 0) || v.isInfinite()) {
                throw new IllegalArgumentException(key + " at level " + (i + 1) + " must be above 0, was " + v);
            }
        }
        return List.copyOf(values);
    }

    private static int clamp(int level, int levels) {
        return Math.max(1, Math.min(level, levels));
    }
}
