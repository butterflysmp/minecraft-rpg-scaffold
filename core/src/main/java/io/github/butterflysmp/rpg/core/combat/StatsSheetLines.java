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
 * wrong for rates in the other direction: it falls back to {@code String.valueOf}, so a gear-modified
 * rate of {@code 0.2 + 0.1} per second prints over five seconds as <b>{@code 1.5000000000000002}</b>.
 * Executed, not guessed. So capacities take {@code trimNumber} (a whole 100 stays {@code "100"}) and
 * rates and damage take two decimals.
 *
 * <p><b>Both shipped bases happen to land on whole numbers over five seconds</b> -- health regen
 * reads {@code 1.00/5s} and mana {@code 5.00/5s} -- which makes it tempting to conclude the trimmer
 * would do. It would not: the moment gear moves either rate off a round value the sixteen digits are
 * back. The example above is reachable with a {@code +0.1/s} bonus.
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

    /** The display window for a regen rate. Rates are STORED per second and SHOWN per five. */
    public static final int RATE_WINDOW_SECONDS = 5;

    /**
     * A rate, shown over {@link #RATE_WINDOW_SECONDS}: {@code 0.2/s} reads {@code "1.00/5s"}.
     *
     * <p>Five seconds, not one, because at one second the interesting rates are all fractions -- base
     * health regen is 0.2 and reads as noise. Over five it is a whole 1, which is also how the stat
     * was designed ("1 HP every 5 seconds") and how a player counts it.
     *
     * <p><b>Takes a value ALREADY in per-second, and multiplies only for display.</b> Health regen is
     * stored that way; mana regen is stored per tick and the caller converts with
     * {@code ManaRegen.perSecond}, which is the one home for THAT conversion. The x5 here is a
     * presentation choice and never leaves this method -- nothing downstream sees a per-5s number.
     *
     * <p>Still two decimals, even though both shipped bases land on whole numbers over five seconds.
     * A gear-modified rate does not: {@code 0.2 + 0.1} is {@code 0.30000000000000004}, which over five
     * seconds is {@code 1.5000000000000002}. Executed. That is exactly what {@code trimNumber} would
     * print and what two decimals renders as {@code "1.50"}.
     */
    public static String perFiveSeconds(double perSecond) {
        return two(perSecond * RATE_WINDOW_SECONDS) + "/" + RATE_WINDOW_SECONDS + "s";
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
