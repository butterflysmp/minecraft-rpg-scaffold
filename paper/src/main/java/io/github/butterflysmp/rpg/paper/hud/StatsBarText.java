package io.github.butterflysmp.rpg.paper.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

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
 * <h2>Two fields are CONDITIONAL, and between them they make four layouts</h2>
 * The {@code ⛨} field renders ONLY when the player has defense to show -- gated on the ROUNDED
 * value, so the bar can never display {@code ⛨ 0} for a defense of 0.4. An unarmored player sees the
 * original two-field bar. The {@code ➹} field renders ONLY while the main hand holds a weapon with a
 * magazine; see {@link #showsQuiver}.
 *
 * <pre>
 * defense  quiver   children  mana at   layout
 *    -        -         3        2      health                gap mana
 *    -        Y         5        4      health gap quiver      gap mana
 *    Y        -         5        4      health gap defense     gap mana
 *    Y        Y         7        6      health gap quiver gap defense gap mana
 * </pre>
 *
 * <p><b>*** THE TWO 5-CHILD LAYOUTS ARE DIFFERENT BARS WITH THE SAME SHAPE, AND INDEX 2 IS THE
 * FIELD THAT DIFFERS. ***</b> Before the quiver field, 5 children meant "armored" and nothing else,
 * and a test could read {@code part(bar, 2)} and call it defense. <b>That inference is now false.</b>
 * Index 2 is the quiver on one of them and defense on the other, and <b>mana sits at index 4 on
 * both</b>, so a mana assertion cannot tell them apart either.
 *
 * <p><b>A test asserting a child INDEX must therefore state which of the four layouts it is in</b>,
 * and the rows in {@code StatsBarTextTest} are grouped that way. This is the cost of a second
 * conditional field, and it is accepted for the reason the first one was: a player is not told about
 * a stat they do not have.
 *
 * <p>A note for whoever reads the git history: pass 2 predicted that inserting the defense field
 * would redden the child-index assertions in {@code StatsBarTextTest}. That prediction assumed an
 * UNCONDITIONAL field. Because the field is conditional, the pre-existing assertions -- which all
 * pass defense 0 -- kept their indices and stayed green; the only forced signal was the compile break
 * from the new parameter. The 5-child layout is therefore covered by tests written specifically for
 * it, not by the old ones surviving. Do not read their greenness as evidence the armored layout was
 * checked. <b>The quiver field repeated that exactly</b>: it too is conditional, every pre-existing
 * row passes {@code magazineCapacity 0}, and every one of them stayed green on a compile break alone.
 *
 * <p>The number shows raw defense POINTS while the armor bar shows the reduction those points buy
 * (see {@code ArmorBarOverride}). That split is deliberate: the number is the input you can raise,
 * the bar is the effect you get.
 */
public final class StatsBarText {

    private StatsBarText() {}

    /**
     * THE STAT ICONS ARE PUBLIC for the same reason the colours below are.
     *
     * <p>They were package-private until Stats Slice 3, when {@code /rpg stats} became a second
     * surface reporting the same stats this bar does -- and a sheet that spelled its own {@code ❤}
     * would be a copy of a glyph, which is exactly the drift the colour javadoc argues against one
     * paragraph down. Importing them makes it a compile-time link instead of two strings that happen
     * to match today.
     */
    public static final String HEART = "❤";
    /** U+26E8, the defense shield. */
    public static final String SHIELD = "⛨";
    /** U+2726, the mana spark. */
    public static final String SPARK = "✦";
    /**
     * U+2694, the crossed swords. Introduced by the stat sheet, which is the first surface with a
     * damage field; the bar has never shown one.
     */
    public static final String SWORDS = "⚔";
    /**
     * U+27B9, HEAVY BLACK-FEATHERED NORTH EAST ARROW -- the quiver. <b>Ben's suggestion, 2026-09-24,
     * and it is taken because it turns out to be exactly right rather than merely available.</b>
     *
     * <p><b>The Unicode NAME is the argument:</b> {@code Character.getName(0x27B9)} is
     * <i>"HEAVY BLACK-FEATHERED NORTH EAST ARROW"</i> -- a FLETCHED arrow, which is the thing a
     * quiver holds. The neighbouring candidates are the same arrow unfeathered or unweighted
     * ({@code U+27A4} BLACK RIGHTWARDS ARROWHEAD is a bare head; {@code U+27B5} is feathered but
     * light and horizontal), and the fletching is what makes this read as ammunition rather than as a
     * direction.
     *
     * <p><b>And it is BLOCK-CONSISTENT with half the existing set</b>, which is the "consistent with
     * the existing four" test made checkable: {@code ❤ U+2764} and {@code ✦ U+2726} are DINGBATS, as
     * this is; {@code ⛨ U+26E8} and {@code ⚔ U+2694} are MISCELLANEOUS SYMBOLS. So the icon is not
     * merely stylistically similar -- it is drawn from a block the bar already renders two glyphs
     * from, which is the closest thing to evidence available before a boot.
     *
     * <p><b>WHETHER IT RENDERS IS A BOOT QUESTION AND IS NOT SETTLED HERE.</b> The four existing
     * icons are all outside ASCII and all render, so the fallback font covers this region -- but
     * "this region" is not "this codepoint", and a missing glyph shows as a box rather than as an
     * error. {@code GATE-quiver-feedback.md} row {@code H1} is the reading.
     */
    public static final String ARROW = "➹";

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
     * The composed hit. Added by the stat sheet in Stats Slice 3 -- the bar has no damage field, so
     * this colour has no HUD sibling to match yet. Named HERE anyway rather than at the sheet's call
     * site, because the moment a second surface reports damage (a popup, a tooltip) the two must be
     * the same colour, and this is the file that makes that a compile-time fact.
     */
    public static final NamedTextColor DAMAGE_COLOR = NamedTextColor.GOLD;
    /** Crit chance and crit damage, which read as one pair and so share one colour. */
    public static final NamedTextColor CRIT_COLOR = NamedTextColor.YELLOW;
    /**
     * The quiver count. <b>AQUA, and the choice is a process of elimination with one real trade in
     * it.</b>
     *
     * <p><b>WHAT IT COULD NOT BE.</b> Five colours on this file are already spoken for --
     * {@link #HEALTH_COLOR} red, {@link #DEFENSE_COLOR} lime, {@link #MANA_COLOR} blue,
     * {@link #DAMAGE_COLOR} gold, {@link #CRIT_COLOR} yellow -- and reusing one would put two
     * different stats in one colour on surfaces a player reads seconds apart, which is the drift the
     * colour block above exists to prevent. Measured across {@code paper/hud} and {@code paper/health}
     * on 2026-09-24, the only other colour in use anywhere on a HUD surface is
     * {@code WHITE}, on {@code DamageNumberText}'s ordinary (non-crit) number. <b>AQUA is used by
     * nothing</b>, so it arrives carrying no meaning to collide with.
     *
     * <p><b>THE TRADE, STATED RATHER THAN GLOSSED: in the no-defense layout this field is ADJACENT TO
     * MANA</b> -- {@code ❤ red    ➹ aqua    ✦ blue} -- and that is the most common layout there is,
     * because it is what a fresh spawn sees. Aqua {@code #55FFFF} and blue {@code #5555FF} differ in
     * one channel. They are still a well-separated pair (cyan against blue), each field leads with its
     * own icon, and {@link #FIELD_GAP} is four spaces -- but this is the one adjacency on the bar
     * worth looking at on a real screen, and {@code GATE-quiver-feedback.md} row {@code H5} asks for
     * exactly that reading.
     *
     * <p><b>WHITE was the considered alternative and was rejected.</b> It is unambiguous against all
     * four hues and is the conventional colour for an ammunition counter -- but it already means
     * "ordinary damage" on {@code DamageNumberText}, so it is not the uncommitted colour it looks
     * like, and an achromatic field on a bar of coloured ones reads as unstyled rather than as
     * deliberate.
     *
     * <p><b>NOT MATCHED TO THE ITEM TOOLTIP, AND THAT IS DELIBERATE.</b> {@code WeaponLore} renders
     * {@code "Quiver: 8/25"} in {@code GRAY} -- but gray there is the LABEL colour every lore line
     * uses, not a colour that identifies the quiver, and that file's stat VALUES are red. So there is
     * no quiver colour to inherit, and the {@code HEALTH_COLOR}/{@code ArmorLore} argument one
     * paragraph up does not reach this field. Said explicitly because the obvious move is to copy the
     * gray and call it consistency.
     */
    public static final NamedTextColor QUIVER_COLOR = NamedTextColor.AQUA;

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
     * <p>The defense field is omitted entirely when {@code defense} rounds to less than 1; the quiver
     * field is omitted when {@code magazineCapacity} is not positive.
     *
     * <h2>THE PARAMETERS ARE IN DISPLAY ORDER, WHICH IS WHY THE QUIVER PAIR SITS WHERE IT DOES</h2>
     *
     * <p>{@code health, quiver, defense, mana} -- the order the player reads left to right, and the
     * order Ben ruled on 2026-09-24. <b>A signature that listed the quiver last while rendering it
     * second would make every call site a small act of translation</b>, and the two numbers most
     * likely to be swapped by a careless caller are the two the bar shows side by side.
     *
     * @param loadedRounds   rounds in the magazine, from {@code QuiverState.roundsRemaining()}
     * @param magazineCapacity the RESOLVED capacity, from {@code QuiverState.capacity()}. <b>Zero or
     *                       less means NO QUIVER and the field vanishes</b> -- the same contract
     *                       {@code WeaponLoreLines.quiverLine} uses for the same condition, so the
     *                       tooltip and the bar agree about what "has a magazine" means.
     */
    public static Component of(double currentHp, double maxHp,
                               int loadedRounds, int magazineCapacity, double defense,
                               double currentMana, double maxMana) {
        Component health = Component.text(field(HEART, currentHp, maxHp), HEALTH_COLOR);
        Component mana = Component.text(field(SPARK, currentMana, maxMana), MANA_COLOR);
        Component gap = Component.text(FIELD_GAP);

        // BUILT AS A LIST RATHER THAN AS FOUR HAND-WRITTEN BRANCHES. Two conditional fields make
        // FOUR layouts, and the previous shape -- an early return for the unarmored bar and one
        // explicit textOfChildren for the armored one -- does not extend: a third conditional field
        // would make eight. The gap is appended BEFORE each field rather than after, so there is
        // exactly one place a trailing gap could come from and it does not exist.
        List<Component> parts = new ArrayList<>(7);
        parts.add(health);
        if (showsQuiver(magazineCapacity)) {
            parts.add(gap);
            parts.add(Component.text(field(ARROW, loadedRounds, magazineCapacity), QUIVER_COLOR));
        }
        if (showsDefense(defense)) {
            parts.add(gap);
            parts.add(Component.text(field(SHIELD, defense), DEFENSE_COLOR));
        }
        parts.add(gap);
        parts.add(mana);
        return Component.textOfChildren(parts.toArray(new Component[0]));
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

    /**
     * Whether the quiver field renders at all: <b>only when the held weapon HAS a magazine.</b>
     *
     * <h2>ON CAPACITY, NOT ON THE COUNT -- AND THE OPPOSITE RULE TO {@link #showsDefense}</h2>
     *
     * <p>Defense hides at zero because zero defense is nothing to report. <b>A quiver at zero is the
     * most important thing on the bar</b>: it is why the weapon will not fire, and hiding it would
     * remove the readout exactly when the player is looking for it. So an empty magazine renders
     * {@code ➹ 0/25} and only a weapon with NO magazine renders nothing.
     *
     * <p>Two conditional fields on one bar with opposite hiding rules is worth stating plainly,
     * because "conditional field" invites the assumption that they work alike and the natural
     * extension of {@code showsDefense} to this field would hide the empty quiver.
     *
     * <p><b>Ben's ruling, 2026-09-24: it always shows {@code loaded/max}, INCLUDING MID-RELOAD.</b>
     * The field never becomes a timer, a dash or a spinner -- {@code QuiverSweep}'s hotbar overlay is
     * the progress indicator, and a second one here would be a duplicate readout of one fact with two
     * chances to disagree. That is the same argument {@code QuiverNotice} makes for having no live
     * countdown.
     *
     * <p>The contract matches {@code WeaponLoreLines.quiverLine}, whose {@code (anything, 0) -> ""}
     * row is the tooltip's version of this predicate.
     */
    static boolean showsQuiver(int magazineCapacity) {
        return magazineCapacity > 0;
    }

    private static String field(String icon, double current, double max) {
        return icon + " " + Math.round(current) + "/" + Math.round(max);
    }

    /**
     * The INT pair, for a magazine. A separate overload rather than letting the ints widen into the
     * one above: a magazine is a COUNT and is integral at the source
     * ({@code QuiverState.roundsRemaining}/{@code capacity} both return {@code int}), so sending it
     * through {@code double} and {@code Math.round} would be a rounding step applied to a number that
     * can never need rounding. Exact for these magnitudes either way -- this is about the reader,
     * who should not have to satisfy themselves that {@code 25} survives the trip.
     */
    private static String field(String icon, int current, int max) {
        return icon + " " + current + "/" + max;
    }

    /** A single-value field: defense has no maximum to show, only what you are carrying. */
    private static String field(String icon, double value) {
        return icon + " " + Math.round(value);
    }
}
