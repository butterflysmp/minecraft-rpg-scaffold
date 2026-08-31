package io.github.butterflysmp.rpg.paper.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds the action-bar stats line: {@code ❤ 100/100    ⛨ 20    ✦ 40/100}, each field an icon
 * followed by its value, coloured as a whole so the fields read as separate units on a bar with no
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
 * <h2>The defense field appears only when there is defense</h2>
 * The {@code ⛨} field reserved by pass 2 now exists, and it renders ONLY when the player has defense
 * to show -- gated on the ROUNDED value, so the bar can never display {@code ⛨ 0} for a defense of
 * 0.4. An unarmored player sees the original two-field bar.
 *
 * <p>So {@link #children()} on the returned component is 3 parts unarmored and 5 armored, and the
 * mana field sits at index 2 or 4 accordingly. That is a real cost -- the mana field slides sideways
 * the moment a helmet goes on -- accepted so a fresh spawn is not told about a stat they do not have.
 *
 * <p>A note for whoever reads the git history: pass 2 predicted that inserting this field would
 * redden the child-index assertions in {@code StatsBarTextTest}. That prediction assumed an
 * UNCONDITIONAL field. Because the field is conditional, the pre-existing assertions -- which all
 * pass defense 0 -- kept their indices and stayed green; the only forced signal was the compile break
 * from the new parameter. The 5-child layout is therefore covered by tests written specifically for
 * it, not by the old ones surviving. Do not read their greenness as evidence the armored layout was
 * checked.
 *
 * <p>The number shows raw defense POINTS while the armor bar shows the reduction those points buy
 * (see {@code ArmorBarOverride}). That split is deliberate: the number is the input you can raise,
 * the bar is the effect you get.
 */
public final class StatsBarText {

    private StatsBarText() {}

    /** U+2764, the same glyph and colour the mob nameplate uses. */
    static final String HEART = "❤";
    /** U+26E8, the defense shield. */
    static final String SHIELD = "⛨";
    /** U+2726, the mana spark. */
    static final String SPARK = "✦";

    /**
     * THE STAT COLOURS ARE PUBLIC because item tooltips read them.
     *
     * <p>An armor piece's "Defense: 8" and its "+30 Max Health" bonus line report the SAME stats
     * this bar does, two seconds apart on the same screen, so a player glancing between them must
     * not see two colours. { ArmorLore} used to restate the values with a comment saying they
     * matched; naming them here and importing them makes that a compile-time link instead of a
     * promise, which is the difference between a colour that is READ off the HUD and one that is
     * merely the same today.
     */
    public static final NamedTextColor HEALTH_COLOR = NamedTextColor.RED;
    /**
     * Lime. Adventure's {@code GREEN} IS Minecraft's lime ({@code §a}, {@code #55FF55}); Minecraft's
     * darker green is Adventure's {@code DARK_GREEN}. Named here because picking the wrong one of the
     * two ships the wrong colour and nothing would fail.
     */
    public static final NamedTextColor DEFENSE_COLOR = NamedTextColor.GREEN;
    /** Public with its siblings, ready for Mana Bank's "+N Max Mana" bonus line in Slice 2b. */
    public static final NamedTextColor MANA_COLOR = NamedTextColor.BLUE;

    /**
     * The gap between fields. Wide enough that the fields read as separate on a background-less bar --
     * the only thing separating them, since none carries a bracket or a divider.
     */
    static final String FIELD_GAP = "    ";

    /**
     * The bar's text. Numbers are whatever the custom stores hold, rendered as integers: a
     * gear-raised {@code 100/400} is representable, and the raw doubles never reach a player's
     * screen.
     *
     * The defense field is omitted entirely when {@code defense} rounds to less than 1.
     */
    public static Component of(double currentHp, double maxHp, double defense,
                               double currentMana, double maxMana) {
        Component health = Component.text(field(HEART, currentHp, maxHp), HEALTH_COLOR);
        Component mana = Component.text(field(SPARK, currentMana, maxMana), MANA_COLOR);
        Component gap = Component.text(FIELD_GAP);
        if (!showsDefense(defense)) {
            return Component.textOfChildren(health, gap, mana);
        }
        return Component.textOfChildren(
                health, gap, Component.text(field(SHIELD, defense), DEFENSE_COLOR), gap, mana);
    }

    /**
     * Whether the defense field renders at all. Gated on the ROUNDED value, not the raw one: the
     * field displays {@code Math.round(defense)}, so anything that would print as 0 must not print.
     * A field reading zero is exactly the placeholder this bar refused to carry before the stat
     * existed.
     */
    static boolean showsDefense(double defense) {
        return Math.round(defense) >= 1;
    }

    private static String field(String icon, double current, double max) {
        return icon + " " + Math.round(current) + "/" + Math.round(max);
    }

    /** A single-value field: defense has no maximum to show, only what you are carrying. */
    private static String field(String icon, double value) {
        return icon + " " + Math.round(value);
    }
}
