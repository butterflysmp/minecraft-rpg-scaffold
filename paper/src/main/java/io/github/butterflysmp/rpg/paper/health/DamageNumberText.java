package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.CritState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds a floating damage number: an optional element GLYPH, then the rounded amount. Pure
 * Adventure -- no Bukkit, no PacketEvents -- so the format is unit-testable, mirroring
 * {@link NameplateText}. Lives in {@code paper} (not {@code core}) because it depends on Adventure,
 * which {@code core} does not carry.
 *
 * <h2>TWO CHANNELS, AND KEEPING THEM SEPARATE IS THE WHOLE DESIGN</h2>
 *
 * <b>COLOUR means crit.</b> A normal hit is white, a CRIT is yellow, and a swept bystander stays
 * white even though its damage inherits the crit in full. That was already three meanings competing
 * for one channel before elements existed.
 *
 * <p><b>The GLYPH means element</b>, and it carries its own colour from
 * {@code content/elements/*.yml}. It was chosen over colouring the number precisely so the two
 * signals stop competing: nothing goes invisible on a crit, symbol space does not run out at element
 * eight the way colour does at seven, and the glyph still reads for a player who cannot separate
 * orange from yellow.
 *
 * <p><b>SO THE GLYPH MUST NOT INHERIT THE NUMBER'S COLOUR, AND THAT IS A TESTED PROPERTY RATHER THAN
 * A TRUSTED ONE.</b> Adventure lets a child keep an explicit colour, so this works -- but "works by
 * inheritance rules" is not a witness. The assembled root therefore carries NO colour of its own;
 * each child carries its own, and {@code DamageNumberTextTest} asserts both in the same tree. Colour
 * the root instead and the glyph turns yellow on every crit, silently undoing the reason the glyph
 * exists.
 *
 * <p><b>Double-rounding tolerance (accepted):</b> this rounds {@code amount}, while the mob nameplate
 * ({@link NameplateText}) rounds current/max independently, so the visible plate drop is
 * {@code round(before) - round(after)}. For FRACTIONAL damage those can differ from {@code round(amount)}
 * by +/-1. Elements still never multiply, so nothing here made damage fractional; scorch's
 * percent-of-max burn did, and it is dealt through its own seam rather than through this number.
 */
public final class DamageNumberText {

    /** Between the glyph and the number. One space -- the glyph is a mark, not a prefix word. */
    private static final Component GAP = Component.text(" ");

    private DamageNumberText() {}

    /** A normal, elementless hit: the rounded amount in white. */
    public static Component of(double amount) {
        return of(amount, CritState.NORMAL, null);
    }

    /** As above, styled by whether it was a CRIT, and still wearing no element. */
    public static Component of(double amount, CritState crit) {
        return of(amount, crit, null);
    }

    /**
     * The number, styled by whether it was a CRIT, optionally marked with its element's glyph.
     *
     * <p><b>{@code CritState} rather than a bare {@code boolean}.</b> The damage seams carry the type
     * precisely because adjacent transposable booleans were the defect they replaced; unwrapping it
     * back into a boolean at this boundary is vocabulary drift that becomes a real transposition risk
     * the day a second flag arrives. No transposition row is written for the three parameters --
     * {@code double}, {@code CritState} and {@code Component} are type-distinct, so a swap is a
     * compile error, and a mutation that cannot be expressed is a property the type enforces.
     *
     * @param symbol the element's mark, ALREADY PARSED and already carrying its own colour, or null
     *               for damage wearing no element -- a thorns reflect, fall damage, scorch's own
     *               burn tick. Null renders exactly what this method rendered before glyphs existed.
     */
    public static Component of(double amount, CritState crit, Component symbol) {
        Component number = Component.text(Long.toString(Math.round(amount)),
                crit.isCrit() ? NamedTextColor.YELLOW : NamedTextColor.WHITE);
        if (symbol == null) return number;
        // An EMPTY, uncoloured root. Every child keeps the colour it arrived with, which is what lets
        // an orange glyph sit beside a yellow crit number in one line.
        return Component.empty().append(symbol).append(GAP).append(number);
    }
}
