package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.paper.weapon.GearLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * The colour half of {@link StatsSheetLines}. Every string comes from core; nothing here formats a
 * number.
 *
 * <p>Same split as {@code ArmorLore}, and the icons and colours are {@link StatsBarText}'s own
 * constants rather than copies of them -- so a player glancing between this sheet and the action bar
 * cannot see two colours for one stat, as a compile-time fact rather than a promise.
 *
 * <h2>Eight explicit lines, and no generic helper</h2>
 *
 * {@code GearLore}'s javadoc records why: a "generic stat line" would have to take the label, the
 * value, the colour AND a composition rule, which is every part of it -- an abstraction that costs
 * more than it saves. So the eight are written out, each naming its own label constant, formatter and
 * colour, and the test asserts them line by line.
 *
 * <h2>Grouping</h2>
 *
 * Health pair, mana pair, defense, then the damage trio. A regen line wears its PARENT stat's colour
 * -- health regen is red, mana regen is blue -- which is what makes the pairing read without a
 * separator. Only the first line of a group carries an icon; the second is indented under it.
 */
public final class StatsSheet {

    private StatsSheet() {}

    /** The indent under a group's icon, so a regen line sits beneath its capacity. */
    private static final String INDENT = "  ";

    /**
     * The eight lines, in order, ready to send.
     *
     * <p>Every argument is already in the unit its formatter expects. In particular {@code manaRegen}
     * must ALREADY be per second -- {@code ResourcePool.regen} returns per tick, and
     * {@code ManaRegen.perSecond} is the one home for that conversion. Passing a per-tick value here
     * would silently report a rate twenty times too small.
     */
    public static List<Component> build(double maxHealth, double healthRegenPerSecond,
                                        double maxMana, double manaRegenPerSecond,
                                        double defense, double damage,
                                        double critChance, double critDamageBonus) {
        List<Component> lines = new ArrayList<>();
        lines.add(header());

        lines.add(line(StatsBarText.HEART, StatsSheetLines.MAX_HEALTH_LABEL,
                StatsSheetLines.capacity(maxHealth), StatsBarText.HEALTH_COLOR));
        lines.add(line(null, StatsSheetLines.HEALTH_REGEN_LABEL,
                StatsSheetLines.perFiveSeconds(healthRegenPerSecond), StatsBarText.HEALTH_COLOR));

        lines.add(line(StatsBarText.SPARK, StatsSheetLines.MAX_MANA_LABEL,
                StatsSheetLines.capacity(maxMana), StatsBarText.MANA_COLOR));
        lines.add(line(null, StatsSheetLines.MANA_REGEN_LABEL,
                StatsSheetLines.perFiveSeconds(manaRegenPerSecond), StatsBarText.MANA_COLOR));

        lines.add(line(StatsBarText.SHIELD, StatsSheetLines.DEFENSE_LABEL,
                StatsSheetLines.capacity(defense), StatsBarText.DEFENSE_COLOR));

        lines.add(line(StatsBarText.SWORDS, StatsSheetLines.DAMAGE_LABEL,
                StatsSheetLines.damage(damage), StatsBarText.DAMAGE_COLOR));
        lines.add(line(null, StatsSheetLines.CRIT_CHANCE_LABEL,
                StatsSheetLines.critChance(critChance), StatsBarText.CRIT_COLOR));
        lines.add(line(null, StatsSheetLines.CRIT_DAMAGE_LABEL,
                StatsSheetLines.critDamage(critDamageBonus), StatsBarText.CRIT_COLOR));

        return lines;
    }

    /** The header, in the GOLD every other {@code /rpg} report opens with. */
    static Component header() {
        return GearLore.plain(StatsSheetLines.HEADER, NamedTextColor.GOLD);
    }

    /**
     * One line: an optional icon in the stat's colour, a GRAY padded label, and the value in the
     * stat's colour.
     *
     * <p>The label is gray and the VALUE wears the colour, which is {@code GearLore.appendFlatBonus}'
     * arrangement -- so a stat reads the same way on an item tooltip and here.
     */
    private static Component line(String icon, String label, String value, NamedTextColor color) {
        String lead = icon == null ? INDENT : icon + " ";
        return GearLore.plain(lead, color)
                .append(GearLore.plain(StatsSheetLines.label(label), NamedTextColor.GRAY))
                .append(GearLore.plain(value, color));
    }
}
