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
public sealed interface EnchantBehaviour permits EnchantBehaviour.Shockwave, EnchantBehaviour.Airburst,
        EnchantBehaviour.GrantTrigger {

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

    /**
     * {@code effect: airburst} (Airburst Flames, 2026-11-23): the weapon's projectile, while it has hit nothing, ENDS
     * once it has TRAVELLED {@code travelBlocks}, and {@code count} unguided bursts leave that point, fanned
     * {@code angleDegrees} apart sideways about its direction of travel ({@code SpreadPattern}: the centre one straight
     * on, the others {@code angleDegrees} either side), each at {@code speed} a tick for a further {@code burstTravelBlocks},
     * each a primary hit of the level's {@code damageByLevel} in {@code element} IN PLACE of the projectile's own damage.
     * A projectile that strikes anything before {@code travelBlocks} lands as authored and never splits.
     */
    record Airburst(double travelBlocks, int count, double angleDegrees, double speed, double burstTravelBlocks,
                    List<Double> damageByLevel, String element) implements EnchantBehaviour {
        public Airburst {
            if (!(travelBlocks > 0) || Double.isInfinite(travelBlocks)) {
                throw new IllegalArgumentException("airburst travel_blocks must be above 0, was " + travelBlocks);
            }
            if (count < 2) throw new IllegalArgumentException("airburst count must be 2 or more, was " + count);
            if (!(angleDegrees > 0) || !(angleDegrees < 90)) {
                throw new IllegalArgumentException("airburst angle_degrees must be in (0, 90), was " + angleDegrees);
            }
            if (!(speed > 0) || Double.isInfinite(speed)) {
                throw new IllegalArgumentException("airburst speed must be above 0, was " + speed);
            }
            if (!(burstTravelBlocks > 0) || Double.isInfinite(burstTravelBlocks)) {
                throw new IllegalArgumentException("airburst burst_travel_blocks must be above 0, was " + burstTravelBlocks);
            }
            damageByLevel = positive("damage_by_level", damageByLevel);
            if (element == null || element.isBlank()) throw new IllegalArgumentException("airburst element is required");
        }

        public double damageAt(int level) {
            return damageByLevel.get(clamp(level, damageByLevel.size()) - 1);
        }

        public int levels() {
            return damageByLevel.size();
        }

        /**
         * THE TICKS A BODY AT {@code speedPerTick} TAKES TO TRAVEL {@code blocks}: rounded UP, so the distance is reached
         * and never fallen short of. A tiny epsilon absorbs a quotient that floating point leaves a hair above a whole
         * number. EXECUTED (jshell, 2026-11-23): 14 / 0.35 is exactly 40.0, so Ben's numbers never need it; 2.1 / 0.7 is 3.0000000000000004, whose bare ceil would be 4 (pinned in {@code AirburstTest}). This line said 14 / 0.35 was such a case until that run.
         */
        public static int ticksToTravel(double blocks, double speedPerTick) {
            return Math.max(1, (int) Math.ceil(blocks / speedPerTick - 1e-9));
        }
    }

    /**
     * {@code effect: grant_trigger} (Lapis Crystal's D1, 2026-11-23): while the enchant is the ACTIVE candidate of its
     * slot at level 1 or more (B2), the weapon {@code weaponId} gains a trigger on {@code input} that casts
     * {@code ability}, an ability authored in the EXISTING grammar (its cost, cooldown, cast and effects are content).
     * The ability's id is {@code <enchant id>/<input>}, so its cooldown is its own, apart from the weapon's triggers'.
     * Only on that weapon: the same state staged on another weapon by the dev command grants nothing
     * ({@code GrantedTriggers}).
     */
    record GrantTrigger(String weaponId, String input,
                        io.github.butterflysmp.rpg.core.ability.AbilityDefinition ability) implements EnchantBehaviour {
        public GrantTrigger {
            if (weaponId == null || weaponId.isBlank()) throw new IllegalArgumentException("grant_trigger needs its weapon");
            if (input == null || input.isBlank()) throw new IllegalArgumentException("grant_trigger input is required");
            if (ability == null) throw new IllegalArgumentException("grant_trigger ability is required");
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
