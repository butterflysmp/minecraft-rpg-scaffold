package io.github.butterflysmp.rpg.core.build;

import java.util.Optional;

/**
 * The numbers an aspect's {@code modify:} may change: PLAN-build-system.md section 2.4.1's whitelist, and
 * nothing else. Anything off this list is refused by the loader, naming the field.
 *
 * <p>Every effect field means <b>every</b> such effect in the target's {@code on_hit}, nested ones included
 * ({@code burst}, {@code area}, {@code throw_embers.burst}). There is no per-index addressing: it breaks
 * silently when the base ability's effect list is reordered.
 *
 * <p>Deliberately NOT here: {@code weapon_damage} (no amount), {@code area.tick_interval} (two quantities in
 * one field), every {@code cast:} field (a changed cast shape is out of v1), {@code throw_embers}'
 * geometry, and {@code status.amplifier} (no shipped content names its quantity).
 */
public enum AspectField {
    COOLDOWN_TICKS("cooldown_ticks", true),
    COST("cost", false),
    DAMAGE_AMOUNT("damage.amount", false),
    HEAL_AMOUNT("heal.amount", false),
    KNOCKBACK_STRENGTH("knockback.strength", false),
    BURST_RADIUS("burst.radius", false),
    AREA_RADIUS("area.radius", false),
    AREA_DURATION_TICKS("area.duration_ticks", true),
    STATUS_DURATION_TICKS("status.duration_ticks", true);

    private final String token;
    private final boolean integer;

    AspectField(String token, boolean integer) {
        this.token = token;
        this.integer = integer;
    }

    /** The YAML spelling, e.g. {@code damage.amount}. */
    public String token() { return token; }

    /** Whether the resolved value must be a whole number (ticks). */
    public boolean integer() { return integer; }

    /** Whether this field addresses effects in {@code on_hit} (the rest address the ability itself). */
    public boolean addressesEffects() {
        return this != COOLDOWN_TICKS && this != COST;
    }

    public static Optional<AspectField> fromToken(String token) {
        for (AspectField field : values()) {
            if (field.token.equals(token)) return Optional.of(field);
        }
        return Optional.empty();
    }
}
