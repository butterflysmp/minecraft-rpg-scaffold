package io.github.butterflysmp.rpg.core.build;

/**
 * One {@code modify:} entry of an aspect: a whitelisted field, and a flat and/or a percent change. A record:
 * an immutable value whose fields are fixed at construction.
 *
 * <p>A change that changes nothing (both zero) is refused: it would read as a modification in the lore and
 * do nothing, the "-X that does nothing" case.
 */
public record NumberChange(AspectField field, double flat, double percent) {

    public NumberChange {
        if (field == null) throw new IllegalArgumentException("a modify entry needs a field");
        if (!Double.isFinite(flat) || !Double.isFinite(percent)) {
            throw new IllegalArgumentException(field.token() + ": flat and percent must be finite numbers");
        }
        if (flat == 0 && percent == 0) {
            throw new IllegalArgumentException(field.token() + ": a modify entry must change something (flat or percent)");
        }
    }
}
