package io.github.butterflysmp.rpg.core.progression;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain-text half of the level screen's bonus lines: what ONE level-up grants, and what the level
 * grants in TOTAL. Pure strings, no Adventure, so the 2-second loop pins them -- the split
 * {@link PlayerLevelLines} makes for the level readout.
 *
 * <h2>THE WORDING IS A DEFAULT, AND IT IS BEN'S TO CHANGE</h2>
 *
 * <p>Ben ruled the numbers (2026-09-29), not the words. These labels reuse the stats sheet's own names
 * ({@code Max Health}, {@code Health Regen}, {@code Defense}, {@code Crit Chance}) so one stat has one
 * name, and say {@code Weapon Damage} for the fifth because the bonus reaches weapon hits only.
 *
 * <h2>REGEN AND CRIT ARE SHOWN IN BEN'S UNITS, NOT THE ENGINE'S</h2>
 *
 * <p>Ben said "+1 per 5 seconds" and "+1% crit". The engine holds HP per SECOND and a crit
 * PROBABILITY, so the lines convert back -- {@code x 5} and {@code x 100} -- and ROUND, because
 * {@code 0.2 x 5} is not guaranteed to print as {@code 1} in binary floating point. Every value here is a
 * whole number by construction (Ben's steps are whole), so rounding loses nothing.
 */
public final class LevelBonusLines {

    private LevelBonusLines() {}

    /**
     * What the level-up INTO {@code level} grants, one line per stat that moved, in a fixed order.
     * Empty at level 1: nothing is paid for being level 1.
     */
    public static List<String> gainedAt(int level) {
        if (level == 1) {
            LevelBonus.at(level);   // the same range check every other entry point applies
            return List.of();
        }
        LevelBonus now = LevelBonus.at(level);
        LevelBonus before = LevelBonus.at(level - 1);
        return lines(now.maxHealth() - before.maxHealth(),
                now.weaponDamage() - before.weaponDamage(),
                now.healthRegenPerSecond() - before.healthRegenPerSecond(),
                now.defense() - before.defense(),
                now.critChance() - before.critChance());
    }

    /** Everything the level grants in total, in the same order. Empty at level 1. */
    public static List<String> totals(LevelBonus bonus) {
        return lines(bonus.maxHealth(), bonus.weaponDamage(), bonus.healthRegenPerSecond(),
                bonus.defense(), bonus.critChance());
    }

    private static List<String> lines(double maxHealth, double weaponDamage, double regenPerSecond,
                                      double defense, double critChance) {
        List<String> lines = new ArrayList<>();
        long hp = Math.round(maxHealth);
        long damage = Math.round(weaponDamage);
        long regenPerFive = Math.round(regenPerSecond * 5);
        long def = Math.round(defense);
        long critPercent = Math.round(critChance * 100);
        if (hp != 0) lines.add("+" + hp + " Max Health");
        if (damage != 0) lines.add("+" + damage + " Weapon Damage");
        if (regenPerFive != 0) lines.add("+" + regenPerFive + " Health Regen per 5s");
        if (def != 0) lines.add("+" + def + " Defense");
        if (critPercent != 0) lines.add("+" + critPercent + "% Crit Chance");
        return List.copyOf(lines);
    }
}
