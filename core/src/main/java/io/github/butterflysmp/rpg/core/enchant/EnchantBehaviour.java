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
        EnchantBehaviour.Shatter, EnchantBehaviour.FocusedBeam, EnchantBehaviour.GrantTrigger, EnchantBehaviour.CastTune,
        EnchantBehaviour.HitChain, EnchantBehaviour.ScorchBoost {

    /**
     * {@code effect: scorch_boost} (Mana Burn; the seat's ruling of 2026-11-24 on PR F). Ben: "Increases Scorch and Ignite
     * damage by 10/20/30%". A primary hit by a weapon carrying it STAMPS the scorch it applies with {@link #percentFor}
     * its level ({@code BehaviourHits.scorchStamp}, carried in {@code HitAccrual}); the stamped scorch's TICK and its
     * Ignite are multiplied by {@code 1 + percent / 100} (F2). The cap is not changed. Per level: the percent.
     */
    record ScorchBoost(List<Double> percentByLevel) implements EnchantBehaviour {
        public ScorchBoost {
            percentByLevel = positive("percent_by_level", percentByLevel);
        }

        /** The stamp a hit at {@code level} carries, in percent. */
        public double percentFor(int level) {
            return percentByLevel.get(clamp(level, percentByLevel.size()) - 1);
        }

        public int levels() {
            return percentByLevel.size();
        }
    }

    /**
     * {@code effect: hit_chain} (Wildfire, the Living Lantern's; the seat's brief of 2026-11-24, PR E). Ben: "Consecutive
     * hits increase damage by +5/10/15% each hit (+60% cap)" / "Same Target, 3s rolling just like Kinetic Tremors, no" /
     * "B". A primary hit by the weapon {@code weaponId}'s own cast on a mob extends that mob's chain when it lands within
     * {@code windowTicks} of the previous one ({@link HitChains}); the n-th hit of a chain deals its damage times
     * {@link #factorFor} = {@code 1 + min(capPercent, stepPercent x (n - 1)) / 100}. Per level: the step.
     */
    record HitChain(String weaponId, int windowTicks, double capPercent, List<Double> stepPercentByLevel)
            implements EnchantBehaviour {
        public HitChain {
            if (weaponId == null || weaponId.isBlank()) throw new IllegalArgumentException("hit_chain needs its weapon id");
            if (windowTicks < 1) throw new IllegalArgumentException("hit_chain window_ticks must be 1 or more, was " + windowTicks);
            if (!(capPercent > 0) || Double.isInfinite(capPercent)) {
                throw new IllegalArgumentException("hit_chain cap_percent must be above 0, was " + capPercent);
            }
            stepPercentByLevel = positive("step_percent_by_level", stepPercentByLevel);
        }

        /** The factor of the {@code n}-th hit of a chain (1-based) at {@code level}. */
        public double factorFor(int level, int n) {
            double step = stepPercentByLevel.get(clamp(level, stepPercentByLevel.size()) - 1) / 100.0;
            return 1 + Math.min(capPercent / 100.0, step * (n - 1));
        }

        public int levels() {
            return stepPercentByLevel.size();
        }
    }

    /**
     * {@code effect: cast_tune} (the seat's G4, 2026-11-24; Ben: "This will just be the normal attack of the weapon and will
     * slightly increase the stats."). THE ONE SEAM FOR A NUMERIC CHANGE TO A WEAPON'S OWN RIGHT CLICK, not one behaviour
     * type per enchant: the file names WHICH QUANTITY of the cast it moves and the value per level, and
     * {@code CastTunes.tune} applies it when the weapon {@code weaponId} makes that cast (the weapon in hand at each shot,
     * ruling 1). Four quantities, each the brief's:
     *
     * <ul>
     *   <li>{@link Quantity#DAMAGE}: ADDED to the authored amount of every literal damage in the cast's on_hit, before
     *       every other term (G5), so the gear score scales it exactly as it scales the base.</li>
     *   <li>{@link Quantity#RANGE}: ADDED to a ray's range (a volley's inner ray's too), in blocks.</li>
     *   <li>{@link Quantity#SHOTS}: a volley's shot count, SET to the value (Ben: "Increases volley count to 4/5/6").</li>
     *   <li>{@link Quantity#WINDUP}: SUBTRACTED from a volley's wind-up, in whole ticks.</li>
     * </ul>
     *
     * <p>The record refuses what it can see: a blank weapon, no levels, a damage or range addition not above 0, a shot count
     * that is not a whole number of 1 or more, a wind-up cut that is not a whole number of ticks of 1 or more. Whether the
     * named weapon's cast HAS the quantity, and whether a level would make it invalid, are the loader's (it can see the
     * weapon) and the cast records' own (a wind-up below 0 throws where {@code CastTunes} builds it).
     */
    record CastTune(String weaponId, Quantity quantity, List<Double> valueByLevel) implements EnchantBehaviour {

        /** Which quantity of the weapon's right click it moves. */
        public enum Quantity { DAMAGE, RANGE, SHOTS, WINDUP }

        public CastTune {
            if (weaponId == null || weaponId.isBlank()) throw new IllegalArgumentException("cast_tune needs its weapon id");
            if (quantity == null) throw new IllegalArgumentException("cast_tune quantity is required");
            if (valueByLevel == null || valueByLevel.isEmpty()) {
                throw new IllegalArgumentException("cast_tune value_by_level is required: one value per level");
            }
            for (int i = 0; i < valueByLevel.size(); i++) {
                Double v = valueByLevel.get(i);
                boolean whole = v != null && v == Math.rint(v);
                boolean ok = switch (quantity) {
                    case DAMAGE, RANGE -> v != null && v > 0 && Double.isFinite(v);
                    case SHOTS, WINDUP -> whole && v >= 1 && v <= 10_000;
                };
                if (!ok) {
                    throw new IllegalArgumentException("cast_tune " + quantity.name().toLowerCase(java.util.Locale.ROOT)
                            + " at level " + (i + 1) + " was " + v + "; " + switch (quantity) {
                                case DAMAGE, RANGE -> "it must be above 0";
                                case SHOTS -> "a shot count is a whole number of 1 or more";
                                case WINDUP -> "a wind-up cut is a whole number of ticks, 1 or more";
                            });
                }
            }
            valueByLevel = List.copyOf(valueByLevel);
        }

        /** The value at {@code level} (1-based), clamped into the authored levels. */
        public double valueAt(int level) {
            return valueByLevel.get(clamp(level, valueByLevel.size()) - 1);
        }

        public int levels() {
            return valueByLevel.size();
        }
    }

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
     * once it has TRAVELLED {@code travelBlocks}, and {@code count} bursts leave that point, fanned
     * {@code angleDegrees} apart sideways about its direction of travel ({@code SpreadPattern}: the centre one straight
     * on, the others {@code angleDegrees} either side), each at {@code speed} a tick for a further {@code burstTravelBlocks},
     * each a primary hit of the level's {@code damageByLevel} in {@code element} IN PLACE of the projectile's own damage.
     * A projectile that strikes anything before {@code travelBlocks} lands as authored and never splits.
     *
     * <p>{@code homing} (Ben, 2026-11-23: "I want the Airburst Flames to seek as well"): the bursts' OWN homing block, in
     * the projectile grammar's terms, each burst seeking for itself; null, and they fly straight, as before it.
     */
    record Airburst(double travelBlocks, int count, double angleDegrees, double speed, double burstTravelBlocks,
                    List<Double> damageByLevel, String element,
                    io.github.butterflysmp.rpg.core.ability.CastSpec.Homing homing) implements EnchantBehaviour {
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
     * {@code effect: shatter} (Shatter Caps, the Breacher's; the seat's brief of 2026-11-24). Ben: "Instead of shooting 4
     * cobble right away, it shoots 1 stone that splits into 4 cobble after traveling 5 blocks. The damage of the Stone is
     * 35/60/85" / "1. the usual 40 2. their full reach 3. the same pattern." While it is active on the weapon
     * {@code weaponId}, a press of that weapon's SPREAD projectile launches ONE stone along the aim IN PLACE of the spread:
     * at {@code speed} a tick, its body {@code item}, the weapon's trail, carrying the weapon's own on_hit with every
     * damage replaced by the level's {@code damageByLevel} in {@code element} (a primary hit, through the same damage
     * path as every hit, so the gear score scales it). Once it has TRAVELLED {@code travelBlocks} without a hit it ends
     * and the weapon's OWN cast leaves that point: its spread, speed, item, on_hit and full lifetime, read from the
     * weapon at run time ({@code CastExecutor.shatter}). A stone that hits first lands its damage and never splits.
     *
     * <p>A SECOND BEHAVIOUR TYPE BESIDE {@link Airburst}, NOT A SHARED SEAM: Airburst cuts the AUTHORED shot short and
     * launches the ENCHANT's bursts; this replaces the authored shot with the ENCHANT's stone and launches the WEAPON's
     * shot. A shared seam would have to re-route Airburst's path; a separate type leaves it byte-for-byte as read.
     * {@code weaponId} is checked at the cast (the ability id is {@code <weapon>/<input>}), as a granted trigger's is: the
     * dev command can stage any enchant on any weapon, and the stone must not replace another weapon's spread.
     */
    record Shatter(String weaponId, double travelBlocks, double speed, String item, List<Double> damageByLevel,
                   String element) implements EnchantBehaviour {
        public Shatter {
            if (weaponId == null || weaponId.isBlank()) throw new IllegalArgumentException("shatter needs its weapon id");
            if (!(travelBlocks > 0) || Double.isInfinite(travelBlocks)) {
                throw new IllegalArgumentException("shatter travel_blocks must be above 0, was " + travelBlocks);
            }
            if (!(speed > 0) || Double.isInfinite(speed)) {
                throw new IllegalArgumentException("shatter speed must be above 0, was " + speed);
            }
            if (item != null && item.isBlank()) item = null;
            damageByLevel = positive("damage_by_level", damageByLevel);
            if (element == null || element.isBlank()) throw new IllegalArgumentException("shatter element is required");
        }

        public double damageAt(int level) {
            return damageByLevel.get(clamp(level, damageByLevel.size()) - 1);
        }

        public int levels() {
            return damageByLevel.size();
        }

        /** The ticks the stone takes to travel {@code travelBlocks}: {@link Airburst#ticksToTravel}, the one rule. */
        public int splitTicks() {
            return Airburst.ticksToTravel(travelBlocks, speed);
        }
    }

    /**
     * {@code effect: focused_beam} (Focused Beam, the Cursed Emerald's; the seat's brief of 2026-11-24, PR 2). Ben: "Fires
     * one stronger beam after the wind up." / "1. 108/162/216 2. No piece 3. Everything the same except increase the range
     * to 48 blocks. 4. Yes". While it is active on the weapon {@code weaponId}, a press of that weapon's VOLLEY OF RAYS
     * pays and winds up exactly as the volley does, and then fires ONE ray in place of all its shots: the volley's own
     * inner ray, read at run time, with its range replaced by {@code range} and never piercing, carrying the weapon's
     * on_hit with every damage replaced by the level's {@code damageByLevel} in {@code element} (a primary hit, through
     * the same damage path as every hit, so the gear score scales it), along his aim as it is at the wind-up's end
     * ({@code CastExecutor.focusedBeam}).
     *
     * <p>A FOURTH BEHAVIOUR TYPE, NOT A SEAM SHARED WITH {@link Shatter}: Shatter acts where a SPREAD projectile is
     * expanded ({@code CastExecutor.launch}); this acts where a VOLLEY fires its first shot ({@code CastExecutor.volley}).
     * Sharing a "replace what the weapon launches" seam would route Shatter's (and Airburst's, in {@code launchOne})
     * path through code written for a volley; a separate type leaves both byte-for-byte as their gates read them.
     * {@code weaponId} is checked at the cast (the ability id is {@code <weapon>/<input>}), as Shatter's is: a volley
     * re-reads the held weapon's behaviours at each shot (ruling 1), so a Cursed Emerald swapped in during another
     * weapon's wind-up must not turn that weapon's volley into this beam.
     */
    record FocusedBeam(String weaponId, double range, List<Double> damageByLevel, String element)
            implements EnchantBehaviour {
        public FocusedBeam {
            if (weaponId == null || weaponId.isBlank()) throw new IllegalArgumentException("focused_beam needs its weapon id");
            if (!(range > 0) || Double.isInfinite(range)) {
                throw new IllegalArgumentException("focused_beam range must be above 0, was " + range);
            }
            damageByLevel = positive("damage_by_level", damageByLevel);
            if (element == null || element.isBlank()) throw new IllegalArgumentException("focused_beam element is required");
        }

        public double damageAt(int level) {
            return damageByLevel.get(clamp(level, damageByLevel.size()) - 1);
        }

        public int levels() {
            return damageByLevel.size();
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
