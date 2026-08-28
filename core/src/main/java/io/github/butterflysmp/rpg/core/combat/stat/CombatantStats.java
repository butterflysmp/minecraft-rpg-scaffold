package io.github.butterflysmp.rpg.core.combat.stat;

import io.github.butterflysmp.rpg.core.ability.AttackSpeed;
import io.github.butterflysmp.rpg.core.combat.Defense;

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
     * Resolved attack-speed MULTIPLIER (1.0 + modifiers), for scaling a basic attack's cooldown.
     *
     * An untracked combatant returns {@link AttackSpeed#BASE} (1.0), NOT 0.0 -- and the difference
     * from {@link #attackValue} above is deliberate, not an inconsistency. Attack damage is a
     * summand, so 0 is the correct "deals nothing" answer for an untracked caster. Attack speed is a
     * DIVISOR: 0 would mean an infinite cooldown, so an untracked caster would silently never swing
     * again. Neutral is the only safe absent value for a divisor.
     */
    public double attackSpeedValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? AttackSpeed.BASE : state.attackSpeedValue();
    }

    /**
     * Resolved CLASS-DAMAGE bonus (0.0 + modifiers): the sum of equipped {@code +N <Class> Damage}
     * sources whose class matches the class of the weapon this combatant is holding. Added on top of
     * every direct damage effect they deal.
     *
     * An untracked combatant returns {@code 0.0}, matching {@link #attackValue} and NOT
     * {@link #attackSpeedValue}. The asymmetry is the same one documented there and must not be
     * flattened: this is a SUMMAND, so 0 correctly means "adds nothing", where attack speed is a
     * DIVISOR whose only safe absent value is neutral.
     *
     * Mobs are never reconciled, so a mob's bonus stays at base 0 -- class-typed gear is a player
     * concern, and a mob has no held-weapon class to gate on.
     */
    public double classDamageValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.classDamageValue();
    }

    /**
     * Resolved ENCHANT-DAMAGE percent (0.0 + modifiers): the sum of the percentages granted by the
     * damage enchants active on the weapon this combatant holds, whose class matches that weapon's.
     * Multiplies the base of every direct damage effect they deal.
     *
     * An untracked combatant returns {@code 0.0} -- which is NEUTRAL here, because the value is a
     * percent and {@code DamageEnchants.multiplier(0.0)} is exactly {@code 1.0}. That is the whole
     * reason the stat carries a percent rather than a multiplier: it keeps the absent-value rule the
     * same as {@link #attackValue} and {@link #classDamageValue}, instead of adding a second
     * convention beside {@link #attackSpeedValue}'s 1.0. Returning 1.0 here would be a 1% damage buff
     * to every untracked combatant; returning 0.0 from a multiplier-valued stat would zero all
     * damage. The percent has no such failure mode.
     *
     * Mobs are never reconciled, so a mob's percent stays at base 0 -- enchants live on player-held
     * items, and a mob has no held-weapon class to gate on.
     */
    public double enchantDamagePercentValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.enchantDamagePercentValue();
    }

    /**
     * Resolved DEFENSE (0.0 + modifiers), in vanilla armor points: the sum of the armor values of the
     * pieces this combatant is wearing. Feeds both the mitigation in {@link #damage} and the DR armor
     * bar.
     *
     * An untracked combatant returns {@code 0.0}, matching {@link #attackValue} and
     * {@link #classDamageValue} and NOT {@link #attackSpeedValue}. This is a SUMMAND in points, so 0
     * correctly means "turns nothing away" -- and it is load-bearing far beyond the usual reason:
     * MOBS ARE NEVER RECONCILED, so every mob in the game resolves to 0 here. If this returned
     * anything else, every hit dealt to every mob would be silently reduced.
     */
    public double defenseValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.defenseValue();
    }

    /**
     * Resolved CRIT CHANCE for {@code id}: the probability one of its hits crits.
     *
     * <p>An untracked combatant returns {@code 0.0} -- NEVER CRITS -- and that default is the
     * fail-closed one on purpose. Returning the player base here would hand a crit chance to anything
     * the store has not seen, mobs included, and the "a mob never crits" rule would depend on which
     * entities happened to be tracked rather than on what they are.
     */
    public double critChanceValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.critChanceValue();
    }

    /**
     * Resolved CRIT DAMAGE (the bonus) for {@code id}. Untracked returns {@code 0.0}, which is inert:
     * an untracked dealer never crits, so there is no multiplier for this to size.
     */
    public double critDamageValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.critDamageValue();
    }

    /**
     * Deal {@code amount} of custom damage to {@code id}, attributed to {@code dealer}. No-op on an
     * untracked combatant. Emits a DAMAGE change carrying the new custom current and max, and the
     * dealer's identity -- the seam the popup hooks next phase.
     */
    public void damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer) {
        damage(id, amount, dealer, dealerIsPlayer, false);
    }

    /**
     * As above, carrying whether the hit was a CRIT. The flag is passed straight through to the seam
     * and never touches the arithmetic: the crit multiplier was already applied to {@code amount} by
     * {@code EffectApplier}, so multiplying here as well would double it. This carries a fact for the
     * displays, not a factor for the maths.
     */
    public void damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer, boolean wasCrit) {
        HealthState state = states.get(id);
        if (state == null) return;
        double dealt = Defense.applyDefense(amount, state.defenseValue());
        boolean reachedZero = state.damage(dealt);
        listener.onChange(new HealthChange(id, state.player(), HealthChange.Kind.DAMAGE, dealt,
                dealer, dealerIsPlayer, state.current(), state.max(), reachedZero, wasCrit));
    }

    /**
     * Converge {@code id}'s CRIT-CHANCE modifiers to exactly {@code desired}. Same leak-proof diff as
     * every other stat; no-op on an untracked combatant.
     */
    public void reconcileCritChanceModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.critChanceTarget(), desired);
    }

    /** Converge {@code id}'s CRIT-DAMAGE modifiers to exactly {@code desired}. See above. */
    public void reconcileCritDamageModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.critDamageTarget(), desired);
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

    /**
     * Converge {@code id}'s ATTACK-SPEED modifiers to exactly {@code desired}. Same leak-proof diff as
     * the two above, on the attack-speed stat. SILENT, like attack damage: there is no display seam
     * for it -- the effect is felt as a faster swing, and the tooltip deliberately shows the weapon's
     * BASE speed rather than the resolved stat. No-op on an untracked combatant.
     */
    public void reconcileAttackSpeedModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.attackSpeedTarget(), desired);
    }

    /**
     * Converge {@code id}'s CLASS-DAMAGE modifiers to exactly {@code desired}. Same leak-proof diff as
     * the three above, on the class-damage stat. SILENT, like attack damage and attack speed: there is
     * no display seam for it -- the effect is felt as a bigger number, and the weapon tooltip
     * deliberately shows the weapon's BASE damage rather than the holder's resolved total. No-op on an
     * untracked combatant.
     *
     * The class GATE is not here. {@code desired} arrives already filtered to the grants matching the
     * held weapon's class (see {@code ClassDamageModifiers.matching}), so this reconciler stays as
     * stat-agnostic as the other three -- it never learns what a weapon class is.
     */
    public void reconcileClassDamageModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.classDamageTarget(), desired);
    }

    /**
     * Converge the enchant-damage percent modifiers to {@code desired}, the same leak-proof diff as
     * the four above. SILENT, like attack damage, attack speed and the class bonus: there is no
     * display seam for it, and the weapon tooltip deliberately shows the weapon's base numbers rather
     * than the holder's resolved total. No-op on an untracked combatant.
     *
     * The class GATE is not here, exactly as it is not in the class-damage reconciler. {@code desired}
     * arrives already filtered to the damage enchants matching the held weapon's own class (see
     * {@code DamageEnchants.matching}), keyed by enchant id, so this reconciler never learns what a
     * weapon class or an enchant is. Two damage enchants on one weapon arrive as two sources and
     * {@link Stat} sums their percentages, which is the correct composition for percentages and the
     * reason the value is not a multiplier.
     */
    public void reconcileEnchantDamageModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.enchantDamageTarget(), desired);
    }

    /**
     * Converge {@code id}'s DEFENSE modifiers to exactly {@code desired} (slot -> armor points, from
     * the pieces the combatant is wearing). Same leak-proof diff as the five above. SILENT: defense
     * has no {@link HealthChange} of its own -- its two display seams, the action-bar number and the
     * DR armor bar, are both polled by the 5-tick reconcile loop that calls this, so an event would
     * be a second, redundant route to the same redraw.
     *
     * Removal is by ABSENCE, like the other five: a slot whose piece left by any route -- swap, drop,
     * break, death, {@code /clear} -- simply has no entry on the next scan and the reconciler drops
     * its source. There is no departure event to miss.
     */
    public void reconcileDefenseModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.defenseTarget(), desired);
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
