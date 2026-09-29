package io.github.butterflysmp.rpg.core.progression;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;

import java.util.Map;

/**
 * What a player's LEVEL grants, as a pure function of the effective level (seat ruling L3,
 * {@code PLAN-level-bonuses.md} section 0.5). Nothing is stored: the reconcile loop re-derives it from
 * lifetime XP every pass, so retuning a number here, or raising {@link PlayerLevel#ACTIVE_CAP}, costs no
 * migration.
 *
 * <h2>BEN'S WORDS, AND THE ONE CONVENTION UNDER ALL FIVE</h2>
 *
 * <p>Ben, 2026-09-29, verbatim in the plan. <b>Level 1 grants nothing</b>: every bonus is paid on a
 * LEVEL-UP, so a new player has the base stats and the first +5 HP arrives at level 2.
 *
 * <ul>
 *   <li><b>Max health:</b> +5 per level-up, and the 49 &rarr; 50 level-up pays +10. L1 0, L2 5, L49 240,
 *       L50 250. Custom HP, the unit every gear file authors in.</li>
 *   <li><b>Weapon damage:</b> +1 per level-up, L50 +49. <b>Weapon hits only</b> -- see
 *       {@code Caster.weaponLevelDamage}. It is not a stat source here, because no stat reaches only
 *       weapons (seat ruling L1).</li>
 *   <li><b>Health regen:</b> +1 HP per 5 seconds at each of levels 10, 15, ..., 50 -- nine steps. The
 *       engine's unit is HP per SECOND, so a step is {@code 1 / 5.0}.</li>
 *   <li><b>Defense:</b> +1 point at each of levels 5, 10, ..., 50 -- ten steps.</li>
 *   <li><b>Crit chance:</b> +1 percentage point at each of levels 10, 20, ..., 50 -- five steps. The
 *       stat is a probability, so a point is {@code 1 / 100.0}.</li>
 * </ul>
 *
 * <h2>DIVIDE ONCE, NEVER ACCUMULATE</h2>
 *
 * <p>Regen and crit are {@code steps / 5.0} and {@code steps / 100.0}: one division of two exact
 * integers, which IEEE-754 rounds correctly. Adding {@code 0.01} five times gives
 * {@code 0.20000000000000004} (executed while surveying), so a loop that summed per level would drift in
 * the last digit.
 *
 * <p><b>Defined only for {@code 1..ACTIVE_CAP}.</b> What the bonuses do above 50 is Ben's call when the
 * cap is raised (Q-L15), so a level past the cap is REFUSED rather than extrapolated -- the same loud
 * refusal {@link PlayerLevel#totalForLevel} gives a bad level. Callers pass
 * {@link PlayerLevel#effectiveLevel}, which cannot exceed the cap.
 */
public record LevelBonus(int level, double maxHealth, double weaponDamage, double healthRegenPerSecond,
                         double defense, double critChance) {

    /**
     * The ONE source key every level modifier carries. One key per stat, so a level-up REPLACES the
     * amount ({@code Stat.putModifier}) rather than adding a second entry.
     */
    public static final String SOURCE = "level";

    /** The bonus at {@code level}. Throws outside {@code 1..PlayerLevel.ACTIVE_CAP}. */
    public static LevelBonus at(int level) {
        if (level < 1 || level > PlayerLevel.ACTIVE_CAP) {
            throw new IllegalArgumentException(
                    "no level bonus for level " + level + "; levels are 1.." + PlayerLevel.ACTIVE_CAP);
        }
        return new LevelBonus(level, maxHealth(level), level - 1, regenSteps(level) / 5.0,
                level / 5, (level / 10) / 100.0);
    }

    /** From lifetime XP, through the view clamp. The reconcile loop's entry point. */
    public static LevelBonus forLifetimeXp(long lifetimeXp) {
        return at(PlayerLevel.effectiveLevel(lifetimeXp));
    }

    /** +5 per level-up from 2 to 49, and +10 for the 49 -> 50 level-up. */
    private static double maxHealth(int level) {
        int fivePointLevelUps = Math.min(level, 49) - 1;
        return 5 * fivePointLevelUps + (level >= 50 ? 10 : 0);
    }

    /** Levels 10, 15, ..., 50: one step at 10, and one more every 5 levels after. */
    private static int regenSteps(int level) {
        return level < 10 ? 0 : (level - 10) / 5 + 1;
    }

    /**
     * This bonus as a desired-source map for ONE stat, ready to merge beside gear, accessories and
     * fragments before that stat's single reconcile call. Empty when the bonus is zero, so a level-1
     * player carries no {@code "level"} modifier at all rather than a zero one.
     *
     * <p>Through {@link AccessoryContributions#sourceValue}, the per-stat seam an accessory's amount
     * takes, so a future health-regen cap reaches the level too.
     */
    /**
     * The weapon-damage bonus as a desired-source map for the LEVEL DAMAGE stat
     * ({@code CombatantStats.reconcileLevelDamageModifiers}). Its own stat, not an {@link AccessoryStat},
     * because no accessory stat reaches weapon hits only. Empty at level 1, like {@link #sources}.
     */
    public Map<String, Double> weaponDamageSources() {
        return weaponDamage == 0.0 ? Map.of() : Map.of(SOURCE, weaponDamage);
    }

    public Map<String, Double> sources(AccessoryStat stat) {
        double amount = switch (stat) {
            case MAX_HEALTH -> maxHealth;
            case HEALTH_REGEN -> healthRegenPerSecond;
            case DEFENSE -> defense;
            case CRIT_CHANCE -> critChance;
            case MAX_MANA, MANA_REGEN, CRIT_DAMAGE, CLASS_DAMAGE -> 0.0;
        };
        if (amount == 0.0) return Map.of();
        return Map.of(SOURCE, AccessoryContributions.sourceValue(stat, amount));
    }
}
