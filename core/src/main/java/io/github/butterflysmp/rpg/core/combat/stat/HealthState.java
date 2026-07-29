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
    private final Stat attack;
    private final boolean player;
    private double current;

    /**
     * A combatant starting full at {@code baseMax}, with attack-damage base {@code baseAttack}.
     * {@code player} is frozen faction, as on the snapshot.
     *
     * Attack damage is the second {@link Stat} on the same combatant -- {@code base + Σ(modifiers)},
     * exactly like max HP, but with no current and no clamp, because attack damage is read on demand,
     * never depleted. A player bases at 0 (weapon-only: no weapon, no hit); a mob bases from its
     * vanilla attack-damage attribute (the mirror of bootstrapping mob HP from vanilla max).
     */
    public HealthState(double baseMax, double baseAttack, boolean player) {
        this.max = new Stat(baseMax);
        this.attack = new Stat(baseAttack);
        this.player = player;
        this.current = baseMax;
    }

    /** A combatant with attack base 0 -- the player/default case, and what HP-focused tests construct. */
    public HealthState(double baseMax, boolean player) {
        this(baseMax, 0.0, player);
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

    // --- Attack damage: a second Stat, resolved on demand, no current, no clamp --------------------

    /** The resolved attack damage: {@code base + Σ(modifiers)}. Read on demand; never depleted. */
    public double attackValue() {
        return attack.value();
    }

    /** Set (or replace) the attack modifier from {@code source}; true if the resolved value changed. */
    public boolean setAttackModifier(String source, double amount) {
        return attack.putModifier(source, amount);
    }

    /** Remove {@code source}'s attack modifier; true if one was actually removed. */
    public boolean clearAttackModifier(String source) {
        return attack.removeModifier(source);
    }

    public double attackModifierAmount(String source) {
        return attack.amountOf(source);
    }

    public Set<String> attackModifierSources() {
        return attack.sources();
    }

    public int attackModifierCount() {
        return attack.modifierCount();
    }

    // --- Modifier targets: one per stat, so the reconcile diff is written once (see ModifierTarget) --

    /**
     * The max-HP modifier surface. Its {@code setModifier} routes through {@link #setMaxModifier}, so a
     * reconcile that raises max gives headroom and one that lowers it clamps current -- the transition
     * rules stay here, the diff stays in {@link ModifierReconciler}.
     */
    ModifierTarget maxTarget() {
        return new ModifierTarget() {
            @Override public Set<String> sources() { return max.sources(); }
            @Override public boolean setModifier(String source, double amount) {
                return setMaxModifier(source, amount);   // clamps current on a decrease
            }
            @Override public boolean clearModifier(String source) { return clearMaxModifier(source); }
        };
    }

    /** The attack-damage modifier surface. A plain {@link Stat} -- no current, so nothing to clamp. */
    ModifierTarget attackTarget() {
        return new ModifierTarget() {
            @Override public Set<String> sources() { return attack.sources(); }
            @Override public boolean setModifier(String source, double amount) {
                return attack.putModifier(source, amount);
            }
            @Override public boolean clearModifier(String source) { return attack.removeModifier(source); }
        };
    }

    /**
     * Reduce current by {@code amount}, never below 0. The custom health is what damage touches.
     *
     * Custom current FLOORS at 0 and does NOT kill -- an entity at 0 custom HP is a deliberate,
     * documented TEMPORARY state this phase (death is the next pass; it sits at the display floor,
     * alive). Do not add a kill here; the death system consumes {@link #damage}'s return instead.
     *
     * @return true only when this hit CROSSED to 0 (was above 0, now exactly 0) -- the death hook,
     *         fired once on the transition, never on a subsequent hit to an already-0 target.
     */
    public boolean damage(double amount) {
        if (amount <= 0) return false;
        double before = current;
        current = Math.max(0.0, current - amount);
        return before > 0.0 && current == 0.0;
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
