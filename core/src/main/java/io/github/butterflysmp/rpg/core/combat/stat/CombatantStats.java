package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom health for every combatant, keyed by id. Shaped like {@link io.github.butterflysmp.rpg.core.combat.ResourcePool}
 * and CooldownTracker on purpose: a concurrent outer map because two players in different regions
 * mutate on different threads at the same instant, while a single combatant's {@link HealthState}
 * is only ever touched on the thread that owns it. Bounded -- {@link #clear} drops a combatant when
 * it leaves, so the map does not grow for the life of the server.
 *
 * This is the source of truth for combat health. Every mutation emits a {@link HealthChange} to the
 * listener so displays follow; nothing downstream reads vanilla health. A store with no display
 * attached passes {@link HealthListener#NONE}.
 */
public final class CombatantStats {

    /** A player's intrinsic max health. Not vanilla's 20 -- the whole point is to leave that scale. */
    public static final double DEFAULT_PLAYER_BASE = 100.0;

    private final Map<UUID, HealthState> states = new ConcurrentHashMap<>();
    private final HealthListener listener;

    public CombatantStats() {
        this(HealthListener.NONE);
    }

    public CombatantStats(HealthListener listener) {
        this.listener = listener;
    }

    /**
     * Register {@code id} fresh at {@code baseMax}, full. Replaces any existing state -- a player
     * rejoining starts clean, with no leaked modifier from a previous session.
     */
    public void register(UUID id, double baseMax, boolean player) {
        // Players base attack at 0 -- weapon-only melee: the held weapon contributes a MAIN_HAND
        // attack modifier via the reconcile loop, so an unarmed player deals nothing.
        states.put(id, new HealthState(baseMax, 0.0, player));
    }

    /**
     * Register {@code id} at {@code baseMax}/{@code baseAttack} only if it is not already tracked, and
     * return its state. The mob path: a mob first touched by a dev command (or real damage) bootstraps
     * its custom health from its vanilla max AND its custom attack damage from its vanilla attack-damage
     * attribute, so its nameplate reads a real number and its hits drain a real amount -- without
     * clobbering state a later phase's content-driven mob stats may have set.
     */
    public HealthState bootstrapIfAbsent(UUID id, double baseMax, double baseAttack, boolean player) {
        return states.computeIfAbsent(id, ignored -> new HealthState(baseMax, baseAttack, player));
    }

    public boolean tracks(UUID id) {
        return states.containsKey(id);
    }

    /** Custom current health. Throws if {@code id} is not tracked -- reading an untracked combatant is a bug. */
    public double current(UUID id) {
        return require(id).current();
    }

    /** Custom max health. Throws if {@code id} is not tracked. */
    public double max(UUID id) {
        return require(id).max();
    }

    /**
     * Resolved attack damage (base + modifiers). Returns {@code 0.0} for an untracked combatant -- unlike
     * {@link #current}/{@link #max}, this is read on the melee hit paths (a weapon swing's WeaponDamage,
     * a mob's melee), where an untracked or unbootstrapped combatant should simply deal nothing rather
     * than throw. Weapon-only melee already makes 0 the correct "no hit" answer.
     */
    public double attackValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.attackValue();
    }

    /**
     * Deal {@code amount} of custom damage to {@code id}, attributed to {@code dealer}. No-op on an
     * untracked combatant. Emits a DAMAGE change carrying the new custom current and max, and the
     * dealer's identity -- the seam the popup hooks next phase.
     */
    public void damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer) {
        HealthState state = states.get(id);
        if (state == null) return;
        boolean reachedZero = state.damage(amount);
        listener.onChange(new HealthChange(id, state.player(), HealthChange.Kind.DAMAGE, amount,
                dealer, dealerIsPlayer, state.current(), state.max(), reachedZero));
    }

    /** Heal {@code id} by {@code amount}, capped at max. No-op on an untracked combatant. */
    public void heal(UUID id, double amount, UUID dealer, boolean dealerIsPlayer) {
        HealthState state = states.get(id);
        if (state == null) return;
        state.heal(amount);
        listener.onChange(new HealthChange(id, state.player(), HealthChange.Kind.HEAL, amount,
                dealer, dealerIsPlayer, state.current(), state.max(), false));
    }

    /**
     * Converge {@code id}'s max modifiers to exactly {@code desired} (source -> amount, from the
     * combatant's currently equipped items). Adds sources newly present, removes sources no longer
     * present, updates changed amounts -- applying headroom on a rise and clamp on a fall through
     * {@link HealthState}. Emits a single MAX_CHANGE only if something actually changed, so a steady
     * state where nothing moved is silent (the transition fires once, at the change, not every tick).
     * No-op on an untracked combatant.
     */
    public void reconcileMaxModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        boolean changed = ModifierReconciler.reconcile(state, desired);
        if (changed) {
            listener.onChange(new HealthChange(id, state.player(), HealthChange.Kind.MAX_CHANGE, 0.0,
                    null, false, state.current(), state.max(), false));
        }
    }

    /**
     * Converge {@code id}'s ATTACK-DAMAGE modifiers to exactly {@code desired} (source -> amount, from
     * the combatant's equipped weapon). Same leak-proof diff as {@link #reconcileMaxModifiers}, on the
     * attack stat. SILENT: attack damage has no display seam (no heart bar, no nameplate) -- the tooltip
     * reads it on demand -- so this emits no {@link HealthChange}. No-op on an untracked combatant.
     */
    public void reconcileAttackModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.attackTarget(), desired);
    }

    /** Drop {@code id}'s state. O(1), safe for an unknown id. Call on logout and on mob removal. */
    public void clear(UUID id) {
        states.remove(id);
    }

    /** Number of combatants holding health state. Bounds check for tests. */
    public int trackedCount() {
        return states.size();
    }

    private HealthState require(UUID id) {
        HealthState state = states.get(id);
        if (state == null) throw new IllegalStateException("no health state tracked for " + id);
        return state;
    }
}
