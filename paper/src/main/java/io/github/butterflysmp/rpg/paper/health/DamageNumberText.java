package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds a floating damage number: the rounded amount in one colour. Pure Adventure -- no Bukkit, no
 * PacketEvents -- so the format is unit-testable, mirroring {@link NameplateText}. Lives in {@code paper}
 * (not {@code core}) because it depends on Adventure, which {@code core} does not carry.
 *
 * <p>TWO styles, since crit: a normal hit is white, a CRIT is YELLOW. Colour is the whole signal in
 * the number itself -- an earlier revision also appended a "!" so the crit survived greyscale, and it
 * was dropped on the call that the number should stay a number. The redundancy that argument wanted
 * has not vanished, it MOVED: the crit particle burst is the second channel, and it is the one a
 * player who cannot separate yellow from white still reads.
 *
 * <p><b>Double-rounding tolerance (accepted):</b> this rounds {@code amount}, while the mob nameplate
 * ({@link NameplateText}) rounds current/max independently, so the visible plate drop is
 * {@code round(before) - round(after)}. For FRACTIONAL damage those can differ from {@code round(amount)}
 * by +/-1. Damage is integer-valued this phase, so the skew is latent; revisit (round both off one basis)
 * only if element multipliers make fractional damage visible.
 */
public final class DamageNumberText {

    private DamageNumberText() {}

    /** A normal hit: the rounded amount in white. Kept so non-crit callers read unchanged. */
    public static Component of(double amount) {
        return of(amount, false);
    }

    /**
     * The number, styled by whether it was a CRIT.
     *
     * <p>A crit reads {@code "28"} in yellow; a normal hit reads the same text in white. Pure, so the
     * distinction is unit-testable without a server -- worth testing precisely BECAUSE the two now
     * differ only by colour, which no other test would catch going wrong.
     */
    public static Component of(double amount, boolean wasCrit) {
        String text = Long.toString(Math.round(amount));
        return wasCrit
                ? Component.text(text, NamedTextColor.YELLOW)
                : Component.text(text, NamedTextColor.WHITE);
    }

}
