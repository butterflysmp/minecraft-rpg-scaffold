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
    /**
     * Attack speed: a MULTIPLIER on a basic attack's authored cadence, so it bases at 1.0 (neutral)
     * for every combatant rather than being handed in. Nothing in content sets a base -- a weapon's
     * cadence is its {@code cooldown_ticks}, and this scales it -- so unlike max HP and attack
     * damage there is no constructor parameter and no bootstrap-from-vanilla story.
     */
    private final Stat attackSpeed = new Stat(io.github.butterflysmp.rpg.core.ability.AttackSpeed.BASE);
    /**
     * Class-typed damage: the sum of equipped {@code +N <Class> Damage} sources whose class matches
     * the class of the weapon the combatant is HOLDING. Base 0.0, and -- like attack speed -- there
     * is no constructor parameter, because nothing in content declares a base for it: the entire
     * value is gear-contributed, converged by the same reconcile loop as the other three.
     *
     * It is a SEPARATE stat rather than a class-gated set of attack-damage modifiers, and that is
     * load-bearing. The held weapon already contributes its declared attack_damage as a MAIN_HAND
     * modifier on the attack stat; folding the class bonus in there too would double-count it
     * through the same source. Keeping them apart also keeps the weapon's INHERENT damage (the bow's
     * 6, the sword's 8) distinct from the gear bonus added on top -- which is why the emberblade's
     * fireball can take +Melee without inheriting the swing's 8.
     */
    private final Stat classDamage = new Stat(0.0);
    /**
     * Enchant damage: the sum of the PERCENTAGES granted by the damage enchants active on the weapon
     * this combatant is HOLDING, whose class matches that weapon's own. Base 0.0, no constructor
     * parameter -- the entire value is item-contributed, like attack speed and the class bonus.
     *
     * <p><b>It carries a PERCENT, not a multiplier, and that is the load-bearing decision.</b>
     * {@link Stat#value()} is {@code base + Sum(modifiers)}. Percentages are genuinely additive, so
     * summing is the correct composition and 0.0 is the correct neutral -- the same neutral as the
     * class bonus, which is what lets an untracked combatant resolve to x1.0 for free. A
     * multiplier-valued stat would have to base at 1.0, and Stat would then resolve TWO sources to
     * 2.0 rather than 1.0; worse, any 0.0 default on it would silently zero all damage instead of
     * leaving it alone. {@code DamageEnchants.multiplier} converts, once, at the damage arm.
     *
     * <p>Distinct from {@link #classDamage} because they compose differently, not merely because
     * they come from different items: the percent multiplies the weapon's BASE and the class bonus
     * adds on top of the product. Folding them into one stat would make that ordering unexpressible.
     */
    private final Stat enchantDamagePercent = new Stat(0.0);
    /**
     * Defense: the sum of the vanilla armor values of the pieces this combatant is WEARING. Base
     * 0.0, no constructor parameter -- the entire value is gear-contributed, like attack speed, the
     * class bonus and the enchant percent.
     *
     * <p>A SUMMAND holding raw armor POINTS, not a reduction fraction, and that is the load-bearing
     * decision. Armor points add correctly (a helmet and boots are 3 + 3); damage-reduction
     * fractions do NOT (two 50% sources are not 100%). Keeping the stat linear and converting once,
     * at the point of use via {@link io.github.butterflysmp.rpg.core.combat.Defense}, is what makes
     * the diminishing-returns curve composable -- and it keeps the absent-value convention the same
     * 0.0 as the three summands above rather than adding a fourth rule.
     *
     * <p>The bar the player reads is a FRACTION and the stat is POINTS on purpose: the number shows
     * the input, the bar shows the effect.
     */
    private final Stat defense = new Stat(0.0);

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

    // --- Attack speed: a third Stat, a multiplier, base 1.0 -----------------------------------------

    /**
     * The resolved attack-speed multiplier: {@code 1.0 + Σ(modifiers)}. A source granting "+50%
     * attack speed" contributes {@code 0.5}, so the additive {@link Stat} resolves to the multiplier
     * without needing multiplicative stacking -- which also keeps two +50% sources at 2.0 rather than
     * 2.25, the saner balance answer.
     */
    public double attackSpeedValue() {
        return attackSpeed.value();
    }

    /** Set (or replace) the attack-speed modifier from {@code source}; true if the value changed. */
    public boolean setAttackSpeedModifier(String source, double amount) {
        return attackSpeed.putModifier(source, amount);
    }

    /** Remove {@code source}'s attack-speed modifier; true if one was actually removed. */
    public boolean clearAttackSpeedModifier(String source) {
        return attackSpeed.removeModifier(source);
    }

    public double attackSpeedModifierAmount(String source) {
        return attackSpeed.amountOf(source);
    }

    public Set<String> attackSpeedModifierSources() {
        return attackSpeed.sources();
    }

    public int attackSpeedModifierCount() {
        return attackSpeed.modifierCount();
    }

    // --- Class-typed damage: a fourth Stat, a summand, base 0.0 -------------------------------------

    /**
     * The resolved class-damage bonus: {@code 0.0 + Sum(modifiers)}. Added to every direct damage
     * effect this combatant deals -- both the {@code WeaponDamage} arm and the literal {@code Damage}
     * arm -- on top of whatever that effect already deals.
     *
     * A SUMMAND, so 0 is the correct "contributes nothing" value, exactly like attack damage and
     * unlike attack speed (a divisor, which must default to 1.0).
     */
    public double classDamageValue() {
        return classDamage.value();
    }

    /** Set (or replace) the class-damage modifier from {@code source}; true if the value changed. */
    public boolean setClassDamageModifier(String source, double amount) {
        return classDamage.putModifier(source, amount);
    }

    /** Remove {@code source}'s class-damage modifier; true if one was actually removed. */
    public boolean clearClassDamageModifier(String source) {
        return classDamage.removeModifier(source);
    }

    public double classDamageModifierAmount(String source) {
        return classDamage.amountOf(source);
    }

    public Set<String> classDamageModifierSources() {
        return classDamage.sources();
    }

    public int classDamageModifierCount() {
        return classDamage.modifierCount();
    }

    // --- Enchant damage: a fifth Stat, a PERCENT, base 0.0 ------------------------------------------

    /**
     * The resolved enchant damage percent: {@code 0.0 + Sum(modifiers)}. A weapon carrying Sharpness
     * III (15%) resolves to {@code 15.0}, which {@code DamageEnchants.multiplier} turns into
     * {@code 1.15} at the damage arm.
     *
     * A PERCENT, so 0 is the correct "scales nothing" value -- and note this is 0 for the SAME reason
     * attack damage and the class bonus are, even though its effect is multiplicative: the conversion
     * to a multiplier happens downstream, precisely so that this side can keep the summand rule and
     * the codebase can keep ONE absent-value convention rather than two.
     */
    public double enchantDamagePercentValue() {
        return enchantDamagePercent.value();
    }

    /** Set (or replace) the enchant-damage modifier from {@code source}; true if the value changed. */
    public boolean setEnchantDamageModifier(String source, double amount) {
        return enchantDamagePercent.putModifier(source, amount);
    }

    /** Remove {@code source}'s enchant-damage modifier; true if one was actually removed. */
    public boolean clearEnchantDamageModifier(String source) {
        return enchantDamagePercent.removeModifier(source);
    }

    public double enchantDamageModifierAmount(String source) {
        return enchantDamagePercent.amountOf(source);
    }

    public Set<String> enchantDamageModifierSources() {
        return enchantDamagePercent.sources();
    }

    public int enchantDamageModifierCount() {
        return enchantDamagePercent.modifierCount();
    }

    // --- Defense: a sixth Stat, a SUMMAND in armor points, base 0.0 --------------------------------

    /**
     * The resolved defense: {@code 0.0 + Sum(modifiers)}, in vanilla armor points. A full diamond set
     * resolves to {@code 20.0}, which {@code Defense.applyDefense} turns into about a sixth off an
     * incoming hit.
     *
     * POINTS, so 0 is the correct "turns nothing away" value -- and 0 is neutral here for the same
     * reason it is for the class bonus: {@code Defense.applyDefense(x, 0)} returns x untouched.
     */
    public double defenseValue() {
        return defense.value();
    }

    /** Set (or replace) the defense modifier from {@code source}; true if the value changed. */
    public boolean setDefenseModifier(String source, double amount) {
        return defense.putModifier(source, amount);
    }

    /** Remove {@code source}'s defense modifier; true if one was actually removed. */
    public boolean clearDefenseModifier(String source) {
        return defense.removeModifier(source);
    }

    public double defenseModifierAmount(String source) {
        return defense.amountOf(source);
    }

    public Set<String> defenseModifierSources() {
        return defense.sources();
    }

    public int defenseModifierCount() {
        return defense.modifierCount();
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

    /** The attack-speed modifier surface. A plain {@link Stat}, like attack damage. */
    ModifierTarget attackSpeedTarget() {
        return new ModifierTarget() {
            @Override public Set<String> sources() { return attackSpeed.sources(); }
            @Override public boolean setModifier(String source, double amount) {
                return attackSpeed.putModifier(source, amount);
            }
            @Override public boolean clearModifier(String source) { return attackSpeed.removeModifier(source); }
        };
    }

    /** The class-damage modifier surface. A plain {@link Stat}, like attack damage. */
    ModifierTarget classDamageTarget() {
        return new ModifierTarget() {
            @Override public Set<String> sources() { return classDamage.sources(); }
            @Override public boolean setModifier(String source, double amount) {
                return classDamage.putModifier(source, amount);
            }
            @Override public boolean clearModifier(String source) { return classDamage.removeModifier(source); }
        };
    }

    /** The enchant-damage modifier surface. A plain {@link Stat}, like attack damage. */
    ModifierTarget enchantDamageTarget() {
        return new ModifierTarget() {
            @Override public Set<String> sources() { return enchantDamagePercent.sources(); }
            @Override public boolean setModifier(String source, double amount) {
                return enchantDamagePercent.putModifier(source, amount);
            }
            @Override public boolean clearModifier(String source) {
                return enchantDamagePercent.removeModifier(source);
            }
        };
    }

    /** The defense modifier surface. A plain {@link Stat}, like attack damage. */
    ModifierTarget defenseTarget() {
        return new ModifierTarget() {
            @Override public Set<String> sources() { return defense.sources(); }
            @Override public boolean setModifier(String source, double amount) {
                return defense.putModifier(source, amount);
            }
            @Override public boolean clearModifier(String source) { return defense.removeModifier(source); }
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
