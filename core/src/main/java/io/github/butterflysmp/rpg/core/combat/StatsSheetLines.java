package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.weapon.GearLoreLines;

import java.util.Locale;

/**
 * The plain-text half of the {@code /rpg stats} sheet: pure String/number formatters over the eight
 * stats. No Adventure, no Bukkit -- lives in core so it runs in the 2-second test loop, and the paper
 * side only wraps these in colour and layout. Same split, and same reason, as {@code ArmorLoreLines}.
 *
 * <h2>THREE UNIT CONVENTIONS MEET HERE, and that is the whole reason this class exists</h2>
 *
 * The eight stats arrive in units that do not match, and the mismatch is the single most likely way
 * to ship a wrong number:
 *
 * <ul>
 *   <li><b>Capacities and points</b> -- max health, max mana, defense. Whole-ish; {@link #capacity}.
 *   <li><b>Rates</b> -- health regen is ALREADY per second; mana regen comes out of
 *       {@code ResourcePool.regen} per TICK and the caller must put it through
 *       {@code ManaRegen.perSecond} first. {@link #perSecond} formats, it does not convert.
 *   <li><b>Fractions</b> -- crit chance is a probability in {@code [0,1]}, NOT a percent; crit damage
 *       is a BONUS whose multiplier is {@code 1 + value}. {@link #critChance} and
 *       {@link #critDamage} own both conversions so no caller performs them.
 * </ul>
 *
 * <h2>Why not {@code Math.round}, and why not {@code trimNumber} either</h2>
 *
 * {@code StatsBarText} rounds with {@code Math.round} because it is a glanceable HUD with no room for
 * decimals. This sheet is the PRECISE view, so it does not. But {@code GearLoreLines.trimNumber} is
 * wrong for rates in the other direction: it falls back to {@code String.valueOf}, and the base mana
 * rate would print as <b>{@code 1.6666666666666665}</b>. Executed, not guessed. So capacities take
 * {@code trimNumber} (a whole 100 stays {@code "100"}) and rates and damage take two decimals.
 *
 * <p>That means the sheet and the action bar CAN disagree in the last digit -- a 137.5 max prints
 * {@code "137.5"} here and {@code "138"} there. Deliberate: they are answering different questions,
 * one exactly and one at a glance.
 */
public final class StatsSheetLines {

    private StatsSheetLines() {}

    /** The header the eight lines sit under. */
    public static final String HEADER = "Your Stats";

    // --- Labels. No trailing punctuation: the paper side supplies the separator, exactly as
    // --- GearLore.appendFlatBonus does, so one place owns it and the eight cannot drift apart.

    public static final String MAX_HEALTH_LABEL = "Max Health";
    public static final String HEALTH_REGEN_LABEL = "Health Regen";
    public static final String MAX_MANA_LABEL = "Max Mana";
    public static final String MANA_REGEN_LABEL = "Mana Regen";
    public static final String DEFENSE_LABEL = "Defense";
    public static final String DAMAGE_LABEL = "Damage";
    public static final String CRIT_CHANCE_LABEL = "Crit Chance";
    public static final String CRIT_DAMAGE_LABEL = "Crit Damage";

    /** Width the labels are padded to, so the value column starts in one place. */
    static final int LABEL_WIDTH = 13;

    /**
     * A label padded to {@link #LABEL_WIDTH} so the values line up.
     *
     * <p><b>Approximate by nature.</b> Minecraft chat is a PROPORTIONAL font, so equal character
     * counts are not equal pixel widths and the column will be slightly ragged. Pinned here anyway
     * because a consistent character count is what makes the raggedness small and predictable rather
     * than arbitrary; the boot gate confirms it is acceptable on screen.
     */
    public static String label(String label) {
        if (label.length() >= LABEL_WIDTH) return label;
        return label + " ".repeat(LABEL_WIDTH - label.length());
    }

    /** A capacity or a points value: {@code 100}, {@code 137.5}, {@code 20}. */
    public static String capacity(double value) {
        return GearLoreLines.trimNumber(value);
    }

    /**
     * A rate, in units per second, to two decimals with the unit attached: {@code "0.20/s"}.
     *
     * <p>Takes a value ALREADY in per-second. Health regen is stored that way; mana regen is stored
     * per tick and the caller converts with {@code ManaRegen.perSecond}, which is the one home for
     * that conversion. This method must never convert, or there would be two.
     */
    public static String perSecond(double perSecond) {
        return two(perSecond) + "/s";
    }

    /** A composed hit, to two decimals: {@code "14.20"}. */
    public static String damage(double amount) {
        return two(amount);
    }

    /**
     * A crit chance, from a {@code [0,1]} PROBABILITY to a whole percent: {@code 0.15 -> "15%"}.
     *
     * <p>Clamped through {@link Crit#chance} rather than by a second rule here, so the sheet reports
     * the ceiling the combat path actually applies: gear stacked past 100% shows {@code "100%"}
     * because every hit crits, not {@code "150%"}.
     */
    public static String critChance(double probability) {
        return Math.round(Crit.chance(probability) * 100) + "%";
    }

    /**
     * A crit multiplier, from the stored BONUS: {@code 1.0 -> "2.00x"}.
     *
     * <p>The stat is a bonus, not a multiplier -- {@code Crit.multiplier} returns {@code 1 + bonus} --
     * so printing the raw value would tell a player their crits do nothing. This is the same
     * {@code 1 +} the combat path multiplies by.
     */
    public static String critDamage(double bonus) {
        return two(1.0 + bonus) + "x";
    }

    /** Two decimals, {@code Locale.ROOT} so a comma-decimal locale cannot change what ships. */
    private static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
