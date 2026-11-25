package io.github.butterflysmp.rpg.core.weapon;

import java.util.Locale;

/**
 * One entry of a tier file's {@code set_bonus:} list (the seat's B1, 2026-11-25): what wearing all four pieces of that
 * tier grants. FLAT (B4, Ben: "No it's flat"): no entry scales with gear score.
 *
 * <p><b>THE TYPE ENUM IS THE ONLY STAT NAME IN JAVA.</b> Which set grants what, and how much, is content.
 *
 * <p><b>ONE UNIT CONVERSION, HERE.</b> {@code health_regen} is authored in the per-5-seconds unit Ben used ("+2 regen,
 * this is to the every 5s number") and the engine's health regen is per SECOND, so {@link #engineValue} divides it by
 * {@link #HEALTH_REGEN_AUTHORED_SECONDS}, once. Every other type is authored in the engine's own unit: mana regen per
 * second (Ben: "this is every second"), max health and defense in points, max summons in summons.
 *
 * <p><b>{@code quiver_size_percent} IS NOT A TYPE, AND THAT IS A STOP, NOT AN OMISSION</b> (PLAN-armor-sets.md 3.3): the
 * quiver's per-player stat is additive only, so a percent has no per-player modifier path, which the seat's B3 names as
 * a STOP for that stat alone. Absent from the enum, an authored {@code quiver_size_percent} is refused as unknown, so no
 * chainmail tooltip can advertise a bonus nothing delivers.
 */
public record ArmorSetBonus(Type type, double value) {

    /** Ben's unit for health regen: "the every 5s number". */
    public static final double HEALTH_REGEN_AUTHORED_SECONDS = 5.0;

    public enum Type {
        HEALTH_REGEN, MANA_REGEN, MAX_SUMMONS, MAX_HEALTH, DEFENSE;

        /** The YAML token: {@code health_regen}. */
        public String token() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** By token, case-insensitively; null for an unknown type, which the loader refuses. */
        public static Type fromToken(String token) {
            if (token == null) return null;
            for (Type t : values()) if (t.token().equalsIgnoreCase(token.trim())) return t;
            return null;
        }
    }

    public ArmorSetBonus {
        if (type == null) throw new IllegalArgumentException("a set bonus needs a type");
        // Negated, so NaN is refused with the infinities and zero (the seat's B1: "a non-finite or zero value").
        if (!Double.isFinite(value) || value == 0) {
            throw new IllegalArgumentException("set bonus " + type.token() + " has value " + value
                    + "; it must be a finite, non-zero number");
        }
        if (type == Type.MAX_SUMMONS && value != Math.rint(value)) {
            throw new IllegalArgumentException("set bonus max_summons has value " + value + "; it must be a whole number");
        }
    }

    /** The value in the engine's unit: health regen per second, everything else as authored. */
    public double engineValue() {
        return type == Type.HEALTH_REGEN ? value / HEALTH_REGEN_AUTHORED_SECONDS : value;
    }
}
