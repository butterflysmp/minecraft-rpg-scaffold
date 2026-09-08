package io.github.butterflysmp.rpg.core.combat.stat;

import io.github.butterflysmp.rpg.core.ability.AttackSpeed;
import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.Defense;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;

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

    /**
     * Is this combatant's VANILLA max-health attribute a PUPPET — written from the custom numbers
     * rather than being its own truth?
     *
     * <h2>It reads the faction bit, and that is a COUPLING, named here rather than left implicit</h2>
     *
     * {@code HealthState.player} is documented as <i>"frozen faction, as on the snapshot"</i>. That
     * answers <b>"is this combatant on the player faction"</b> — which is what {@code dealerIsPlayer}
     * reads for attribution and kill credit. <b>This method asks a different question</b>, and the two
     * agree only because the same registration call sets the bit and decides the puppeting:
     * {@code PlayerHealthSystem} renders the bar for exactly the combatants registered as players.
     *
     * <p><b>A shared value is a coupling, and the coupling is invisible at both ends.</b> A puppeted
     * non-player, or a player-faction entity with a real bar, would break one reader while the other's
     * tests stayed green. The accessor is therefore named for the property it is USED for, not for the
     * field it happens to read — and {@code HealthState}'s field carries the matching note.
     *
     * <p>What would separate them: anything that puppets a mob's bar (a boss health display), or that
     * registers a player-faction entity whose vanilla max is its own. On that day this method needs
     * its own field and the two uses must not be untangled by guesswork.
     *
     * <p>Consumed by {@code DamageScale.toCustom} to choose the conversion denominator: a puppeted bar
     * divides by vanilla's own 20, a real one by the entity's actual attribute.
     *
     * @return false for an untracked combatant — it has no custom numbers, so nothing is puppeting
     *         its bar and its vanilla max is its own
     */
    public boolean isBarPuppeted(UUID id) {
        HealthState state = states.get(id);
        return state != null && state.player();
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
     * dealer's identity.
     *
     * @return what the hit did: the POST-MITIGATION amount that landed, and where it left the
     *         target. See {@link DamageOutcome} for why the second fact is the post-hit CURRENT
     *         rather than a transition bit -- a player at zero custom health is never killed or
     *         removed, so "did this hit cause the transition" and "is the target still standing"
     *         are different questions with a reachable difference.
     */
    public DamageOutcome damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer) {
        return damage(id, amount, dealer, dealerIsPlayer, CritState.NORMAL);
    }

    /**
     * As above, carrying whether the hit was a CRIT. The flag is passed straight through to the seam
     * and never touches the arithmetic: the crit multiplier was already applied to {@code amount} by
     * {@code EffectApplier}, so multiplying here as well would double it. This carries a fact for the
     * displays, not a factor for the maths.
     */
    public DamageOutcome damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer, CritState crit) {
        return damage(id, amount, dealer, dealerIsPlayer, crit, DefenseRule.APPLIES);
    }

    /**
     * As above, and {@code bypassesDefense} skips {@link Defense#applyDefense} entirely.
     *
     * <p><b>THIS OVERLOAD WEARS NO ELEMENT, AND THAT IS A GUARD RATHER THAN AN OMISSION.</b> It is the
     * form {@code EntityScorchSink} reaches through, so scorch's own burn tick has no element to pass
     * and therefore <b>cannot accrue more scorch</b>. The loop is unrepresentable, not checked. Do not
     * "tidy" this into a delegation that passes some default element; see {@link
     * io.github.butterflysmp.rpg.core.combat.CombatantHandle#applyDamage}, which carries the same note
     * at the other end of the port.
     */
    public DamageOutcome damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer, CritState crit,
                         DefenseRule defense) {
        return damage(id, amount, dealer, dealerIsPlayer, crit, defense, null);
    }

    /**
     * As above, naming the ELEMENT this damage wears, or null for none.
     *
     * <p><b>This line was unconditional from the day it was written, and that was the defect.</b>
     * {@code NEXT.md}'s standing question -- "which causes should {@code Defense} touch?" -- names this
     * exact call as the reason there is no route to "ignores defense", and the operator's drowning rule
     * ("REGARDLESS of defense") has been unimplementable because of it. Scorch is the first consumer;
     * see {@code CombatantHandle.applyDamage}'s javadoc for why the flag is named for the PROPERTY
     * rather than for scorch.
     *
     * <p><b>The bypass is total, not a reduced cut.</b> A percent-of-max burn that armour trims is no
     * longer percent-of-max damage -- it is ordinary damage wearing a percentage, which defeats the
     * one thing the shape was chosen for. Armour still reaches scorch, through stack ACCRUAL: fewer
     * points landed is fewer stacks, so armour delays the burn rather than blunting it.
     *
     * <p><b>AND THAT SENTENCE IS ONLY TRUE BECAUSE OF THE RETURN VALUE.</b> It was written one slice
     * before anything delivered it -- accrual was planned for {@code EffectApplier}, which sits
     * UPSTREAM of the curve below and could only ever have seen the pre-mitigation figure. The
     * post-mitigation number exists exactly here, was computed and discarded, and is now returned so
     * {@code BukkitCombatant} can feed it to {@code Scorch.stacksFor} -- whose parameter has been
     * named {@code dealtPostMitigation} since the day it was written. <b>Recomputing
     * {@code Defense.applyDefense} in the caller instead would be a SECOND site applying the curve,</b>
     * which would have to stay in sync with this one forever with nothing to catch a divergence.
     *
     * <p><b>{@code crit} and {@code defense} are TYPES, and {@code dealerIsPlayer} is deliberately
     * still a boolean.</b> These three sat adjacent as {@code boolean dealerIsPlayer, boolean wasCrit,
     * boolean bypassesDefense} -- six orderings, five wrong, all six compiling, and
     * {@code BukkitCombatant} passes all three positionally. Lifting two of them out leaves one lone
     * boolean, which has nothing to be transposed with. See {@link DefenseRule}.
     *
     * <p><b>AND THAT SAFETY IS A PROPERTY OF THIS SIGNATURE, NOT OF THE PARAMETER.</b>
     * {@code dealerIsPlayer} is allowed to stay a {@code boolean} <i>because it is the only one</i>.
     * <b>ADD A SECOND BOOLEAN ANYWHERE IN THIS PARAMETER LIST AND THE TRANSPOSITION HAZARD IS REOPEN
     * FOR BOTH OF THEM</b> -- and whoever adds it has no reason to look here first, which is exactly
     * how a condition nobody wrote down gets built past. So: a new flag on this method is a new TYPE,
     * or it converts {@code dealerIsPlayer} to one as well. It is not a third boolean. {@code element}
     * is a {@code String} and so cannot be transposed with anything here, which is why it was allowed
     * to ride as one rather than needing a wrapper type of its own.
     *
     * <p><b>Enforced, not merely stated</b>, because this file's own rule is that a rule living only
     * in a comment gets built past: {@code DamageSignatureTest} reflects over this class and
     * {@link io.github.butterflysmp.rpg.core.combat.CombatantHandle} and FAILS THE BUILD if any method
     * declares two or more {@code boolean} parameters.
     *
     * @param element the element this damage wears, or null. Never a factor -- it multiplies nothing.
     *                It rides for the damage number's glyph and for stack accrual, neither of which
     *                anything downstream can derive.
     */
    public DamageOutcome damage(UUID id, double amount, UUID dealer, boolean dealerIsPlayer, CritState crit,
                         DefenseRule defense, String element) {
        HealthState state = states.get(id);
        if (state == null) return DamageOutcome.UNTRACKED;
        double dealt = defense == DefenseRule.BYPASSED
                ? amount
                : Defense.applyDefense(amount, state.defenseValue());
        boolean reachedZero = state.damage(dealt);
        listener.onChange(new HealthChange(id, state.player(), HealthChange.Kind.DAMAGE, dealt,
                dealer, dealerIsPlayer, state.current(), state.max(), reachedZero, crit.isCrit(),
                element));
        return new DamageOutcome(dealt, state.current());
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

    /**
     * The resolved MAX-MANA BONUS this combatant's gear grants, or {@code 0.0} if untracked.
     *
     * <p>A bonus, not a ceiling: {@code ResourcePool}'s resolver adds it to the base pool. Returning
     * 0.0 rather than throwing is load-bearing, and the difference from {@link #max} is deliberate --
     * this is read from inside {@code tryConsume}, on whatever thread is casting, for any owner
     * including a mob firing a costed trigger. A throw there would come out of a cast.
     */
    public double maxManaBonusValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.maxManaBonusValue();
    }

    /**
     * Converge {@code id}'s MAX-MANA modifiers to exactly {@code desired}. Same leak-proof diff as
     * the others. SILENT, by the defense precedent: the mana field is POLLED by the action bar, so
     * an event would be a second route to the same redraw.
     *
     * <p><b>Returns whether anything changed, unlike its void siblings</b>, and that is not
     * symmetry for its own sake. Mana's current lives in {@code ResourcePool}, so the max-change
     * transition -- headroom up, clamp down -- cannot happen inside {@code HealthState} the way max
     * health's does. The caller needs to know a transition occurred so it can pin the pre-change
     * reading in the pool. {@code reconcileMaxModifiers} reads the same boolean; it just spends it
     * on an event instead.
     *
     *  true if a source was added, removed, or its amount altered
     */
    public boolean reconcileMaxManaModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return false;
        return ModifierReconciler.reconcile(state.maxManaTarget(), desired);
    }

    /**
     * The resolved passive regeneration rate in HP PER SECOND, or {@code 0.0} if untracked.
     *
     * <p>Total, never throwing, like every accessor here except {@link #current} and {@link #max}.
     * And 0.0 is the RIGHT neutral rather than merely a safe one: it is what a mob reads, and
     * {@code HealthRegen.healAmount} returns nothing for a rate of 0, so an untracked or non-player
     * combatant reaching the regeneration tick heals nothing by the same arithmetic that makes a mob
     * heal nothing. There is no second "is this a player" check anywhere to disagree with it.
     */
    public double healthRegenValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.healthRegenValue();
    }

    /**
     * Converge {@code id}'s HEALTH-REGEN modifiers to exactly {@code desired}. Same leak-proof diff
     * as the others. SILENT, by the defense precedent -- and doubly so here, since nothing displays
     * the rate yet and the regeneration loop reads it fresh on every fire rather than being told.
     *
     * <p><b>void, unlike {@link #reconcileMaxManaModifiers}</b>, and that asymmetry is the point of
     * both. Mana's boolean exists because its current lives in {@code ResourcePool} and a ceiling
     * change has to be pinned there -- {@code NEXT.md} records that a reconcile which always reported
     * "changed" would re-stamp the pool's tick four times a second and stop mana regenerating
     * entirely. This stat is a rate with no current anywhere: there is no transition to pin, so there
     * is nothing for a caller to do with the answer and no way for a wrong one to do damage.
     */
    public void reconcileHealthRegenModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return;
        ModifierReconciler.reconcile(state.healthRegenTarget(), desired);
    }

    /**
     * The resolved MANA-REGEN BONUS this combatant's gear grants, in mana per second, or {@code 0.0}
     * if untracked.
     *
     * <p>A bonus, not the whole rate: {@code ResourcePool}'s rate resolver adds it to the base.
     * Returning 0.0 rather than throwing is load-bearing for the same reason
     * {@link #maxManaBonusValue} gives -- this is read from inside {@code tryConsume}, on whatever
     * thread is casting, for any owner including a mob firing a costed trigger.
     */
    public double manaRegenBonusValue(UUID id) {
        HealthState state = states.get(id);
        return state == null ? 0.0 : state.manaRegenBonusValue();
    }

    /**
     * Converge {@code id}'s MANA-REGEN modifiers to exactly {@code desired}. Same leak-proof diff as
     * the others, and SILENT like every reconcile but max health's.
     *
     * <p><b>Returns whether anything changed, like {@link #reconcileMaxManaModifiers} and unlike
     * {@link #reconcileHealthRegenModifiers}</b> -- and the reason is NOT that this stat has a current
     * (it does not). It is that mana regenerates LAZILY: {@code ResourcePool} evaluates
     * {@code amount + elapsed * rate} at read time, so a rate change re-prices ticks that already
     * elapsed. The caller needs to know a transition occurred so it can pin the pre-change reading,
     * exactly as it does for the ceiling.
     *
     * <p><b>An implementation that always reported true would be worse than useless here.</b>
     * {@code NEXT.md} records the failure for max mana and it applies unchanged: the reconcile loop
     * runs four times a second, and a pin every tick re-stamps the entry's {@code asOfTick}, so the
     * elapsed count never grows and mana stops regenerating ENTIRELY -- silently, with a stat block
     * that still reads correctly.
     *
     * @return true if a source was added, removed, or its amount altered
     */
    public boolean reconcileManaRegenModifiers(UUID id, Map<String, Double> desired) {
        HealthState state = states.get(id);
        if (state == null) return false;
        return ModifierReconciler.reconcile(state.manaRegenTarget(), desired);
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
