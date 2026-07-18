package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds a floating damage number: the rounded amount in one colour. Pure Adventure -- no Bukkit, no
 * PacketEvents -- so the format is unit-testable, mirroring {@link NameplateText}. Lives in {@code paper}
 * (not {@code core}) because it depends on Adventure, which {@code core} does not carry.
 *
 * <p>ONE colour this pass (white): crit isn't in the engine and the seam carries no crit bit, so there
 * is no crit/normal branch yet -- that lands when attack-damage/crit become custom stats.
 *
 * <p><b>Double-rounding tolerance (accepted):</b> this rounds {@code amount}, while the mob nameplate
 * ({@link NameplateText}) rounds current/max independently, so the visible plate drop is
 * {@code round(before) - round(after)}. For FRACTIONAL damage those can differ from {@code round(amount)}
 * by +/-1. Damage is integer-valued this phase, so the skew is latent; revisit (round both off one basis)
 * only if element multipliers make fractional damage visible.
 */
public final class DamageNumberText {

    private DamageNumberText() {}

    public static Component of(double amount) {
        return Component.text(Long.toString(Math.round(amount)), NamedTextColor.WHITE);
    }
}
