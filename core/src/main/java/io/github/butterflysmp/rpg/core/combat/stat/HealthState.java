package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.Set;

/**
 * One combatant's health: a custom max ({@code base + modifiers}, via {@link Stat}) and a current
 * value that is always {@code <= max}. Custom, not vanilla-backed, so nothing here is bound by
 * vanilla's 1024 cap -- a boss can hold 5000. All combat health math is against these numbers; the
 * vanilla heart bar and the nameplate are downstream displays, never the source of truth.
 *
 * The hard part is the TRANSITION when max changes, not the steady state -- exactly the Soaked
 * cleanup lesson. The two rules are stated here as decisions, not left to emerge:
 *
 *  - Max INCREASES (equip a +HP source): current is UNCHANGED. You gain headroom, not health --
 *    100/100 with +300 max becomes 100/400 (now 25%, hurt-looking until you heal). Equip is never
 *    a free heal.
 *  - Max DECREASES (unequip): current is CLAMPED to the new max, never left above it --
 *    400/400 losing 300 max becomes 100/100. If current was already below the new max it is left
 *    alone (50/400 -> unequip -> 50/100).
 *
 * Not thread-safe: touched only on the owning combatant's thread, like a Soaked entry.
 */
public final class HealthState {

    private final Stat max;
    private final boolean player;
    private double current;

    /** A combatant starting full at {@code baseMax}. {@code player} is frozen faction, as on the snapshot. */
    public HealthState(double baseMax, boolean player) {
        this.max = new Stat(baseMax);
        this.player = player;
        this.current = baseMax;
    }

    public double max() {
        return max.value();
    }

    public double current() {
        return current;
    }

    public boolean player() {
        return player;
    }

    /**
     * Set (or replace) the max modifier from {@code source}. Headroom on the way up (current
     * untouched), clamp on the way down (current never left above the new max). One call covers
     * both: raising max leaves {@code current <= max} already, so the clamp is a no-op and headroom
     * is preserved; lowering it below current pulls current down.
     *
     * @return true if the resolved max actually changed -- so the caller emits a change once, on a
     *         real transition, not on an idempotent re-apply every reconcile tick.
     */
    public boolean setMaxModifier(String source, double amount) {
        boolean changed = max.putModifier(source, amount);
        clampCurrentToMax();
        return changed;
    }

    /**
     * Remove {@code source}'s max modifier and clamp current to the new max.
     *
     * @return true if a modifier was actually removed.
     */
    public boolean clearMaxModifier(String source) {
        boolean removed = max.removeModifier(source);
        if (removed) clampCurrentToMax();
        return removed;
    }

    public boolean hasMaxModifier(String source) {
        return max.hasModifier(source);
    }

    public double maxModifierAmount(String source) {
        return max.amountOf(source);
    }

    public Set<String> maxModifierSources() {
        return max.sources();
    }

    public int maxModifierCount() {
        return max.modifierCount();
    }

    /** Reduce current by {@code amount}, never below 0. The custom health is what damage touches. */
    public void damage(double amount) {
        if (amount <= 0) return;
        current = Math.max(0.0, current - amount);
    }

    /** Raise current by {@code amount}, never above max. Healing cannot exceed the ceiling. */
    public void heal(double amount) {
        if (amount <= 0) return;
        current = Math.min(max.value(), current + amount);
    }

    private void clampCurrentToMax() {
        double m = max.value();
        if (current > m) current = m;
    }
}
