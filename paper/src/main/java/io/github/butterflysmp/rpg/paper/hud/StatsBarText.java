package io.github.butterflysmp.rpg.paper.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds the action-bar stats line: {@code ❤ 100/100    ✦ 40/100}, each field an icon followed by
 * its custom cur/max, coloured as a whole so the two read as separate units on a bar with no
 * background.
 *
 * Pure Adventure -- no Bukkit, no server state, no side effects -- so the format is unit-testable in
 * the fast suite rather than boot-witnessed. It lives in {@code paper} and not {@code core} for the
 * same reason {@link io.github.butterflysmp.rpg.paper.health.DamageNumberText} and
 * {@code RarityColors} do: it depends on Adventure, and core carries zero dependencies. Purity is
 * the property that mattered, and it is kept.
 *
 * The health glyph and colour are deliberately the same U+2764 red the mob nameplate uses, so HP
 * reads identically wherever it appears. The nameplate trails its heart after a name because it is a
 * different widget; here the icon LEADS, because a leading icon labels its field and keeps each stat
 * an unambiguous icon-then-value unit.
 *
 * <h2>Defense is absent on purpose</h2>
 * Pass 3 inserts a {@code ⛨} defense field BETWEEN health and mana. There is deliberately no
 * placeholder field reading 0 today: no defense stat exists to read, and a readout reporting zero
 * when nothing is measured is indistinguishable from a working readout that measured zero. The field
 * does not exist until the stat does. Inserting it will shift the child indices
 * {@code StatsBarTextTest} pins -- that reddening is the point, not a nuisance.
 */
public final class StatsBarText {

    private StatsBarText() {}

    /** U+2764, the same glyph and colour the mob nameplate uses. */
    static final String HEART = "❤";
    /** U+2726, the mana spark. */
    static final String SPARK = "✦";

    static final NamedTextColor HEALTH_COLOR = NamedTextColor.RED;
    static final NamedTextColor MANA_COLOR = NamedTextColor.BLUE;

    /**
     * The gap between fields. Wide enough that two fields read as two on a background-less bar --
     * the only thing separating them, since neither carries a bracket or a divider.
     */
    static final String FIELD_GAP = "    ";

    /**
     * The bar's text. Numbers are whatever the custom stores hold, rendered as integers: a
     * gear-raised {@code 100/400} is representable, and the raw doubles never reach a player's
     * screen.
     */
    public static Component of(double currentHp, double maxHp, double currentMana, double maxMana) {
        return Component.textOfChildren(
                Component.text(field(HEART, currentHp, maxHp), HEALTH_COLOR),
                Component.text(FIELD_GAP),
                Component.text(field(SPARK, currentMana, maxMana), MANA_COLOR));
    }

    private static String field(String icon, double current, double max) {
        return icon + " " + Math.round(current) + "/" + Math.round(max);
    }
}
