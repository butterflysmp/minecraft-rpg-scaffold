package io.github.butterflysmp.rpg.core.ability.effect;

import java.util.List;
import java.util.Optional;

/**
 * What a trigger's on_hit payload actually DEALS, and -- the load-bearing part -- where the number
 * comes from.
 *
 * This lives here, next to {@link EffectSpec}, rather than in the lore package where it started,
 * because TWO systems now key off the same distinction and they must never disagree about the same
 * weapon:
 *
 *  - the TOOLTIP renders a basic attack as a stat block (class-labelled damage + attack speed) and
 *    an ability as an ability block (name, prose, element-labelled damage, cadence);
 *  - the COOLDOWN scales a basic attack by the caster's attack-speed stat and leaves an ability's
 *    declared cadence alone.
 *
 * A weapon that rendered as an ability but swung like a basic attack would be exactly the kind of
 * quiet divergence this codebase keeps writing comments about, so the rule is written ONCE, here,
 * and both callers ask it. {@code core.weapon} already depends on this package, and putting it here
 * (rather than in {@code core.weapon}) keeps {@code core.ability} from having to import the weapon
 * package to make a decision about cooldowns -- which would invert the existing direction, since
 * {@code WeaponService} imports {@code AbilityService}.
 */
public final class DamagePayload {

    private DamagePayload() {}

    /**
     * Where a trigger's damage number comes from. This is the line between a BASIC ATTACK and an
     * ABILITY, modelled rather than guessed at from the input name.
     *
     * {@code WEAPON_STAT} means the effect reads the wielder's ATTACK_DAMAGE stat (which the weapon
     * contributes) -- a plain swing. A future class-typed modifier ("+N Melee Damage") genuinely
     * reaches it, so labelling it with the weapon's CLASS is honest, and the attack-speed stat
     * scales its cooldown.
     *
     * {@code ABILITY_LITERAL} means the effect carries its own hardcoded amount and reads no stat at
     * all. No class-typed modifier can ever touch it, so it is labelled by its ELEMENT instead, and
     * its declared cadence is left alone.
     */
    public enum DamageSource { WEAPON_STAT, ABILITY_LITERAL }

    /** A trigger's headline damage: how much, of what element, and where the number came from. */
    public record TriggerDamage(double amount, String element, DamageSource source) {}

    /**
     * The damage a trigger deals, from static content: the FIRST damage-bearing effect found,
     * recursing into a Burst/Area's inner effects. A {@code WeaponDamage} reads the weapon's declared
     * {@code attackDamage}; a literal {@code Damage} uses its own amount. Empty when the trigger deals
     * no direct damage (a pure heal/status, or a cosmetic-only payload).
     */
    public static Optional<TriggerDamage> of(List<EffectSpec> onHit, double weaponAttackDamage) {
        return firstDamage(onHit, weaponAttackDamage);
    }

    /**
     * True when this payload READS the attack-damage stat -- a basic attack rather than an ability
     * literal.
     *
     * FIRST-WINS, not contains-anywhere, and the difference is deliberate. For
     * {@code [Damage(5), WeaponDamage]} a "contains" rule would call this a basic attack while the
     * tooltip renders an ability block, and the two systems would disagree about one weapon. This
     * asks the same question the tooltip asks, so they cannot. No shipped content has a mixed
     * payload; this is about which rule is written down for the one that eventually does.
     *
     * The attack-damage figure is irrelevant to the question, so it passes 0 rather than making
     * every caller supply a number it does not need.
     */
    public static boolean isBasicAttack(List<EffectSpec> onHit) {
        return of(onHit, 0.0)
                .map(damage -> damage.source() == DamageSource.WEAPON_STAT)
                .orElse(false);
    }

    private static Optional<TriggerDamage> firstDamage(List<? extends EffectSpec> effects,
                                                       double weaponAttackDamage) {
        for (EffectSpec effect : effects) {
            Optional<TriggerDamage> damage = damageOf(effect, weaponAttackDamage);
            if (damage.isPresent()) return damage;
        }
        return Optional.empty();
    }

    /** The direct damage a single effect contributes, if any. Exhaustive over the sealed EffectSpec. */
    private static Optional<TriggerDamage> damageOf(EffectSpec effect, double weaponAttackDamage) {
        return switch (effect) {
            case EffectSpec.WeaponDamage w ->
                    Optional.of(new TriggerDamage(weaponAttackDamage, w.element(), DamageSource.WEAPON_STAT));
            case EffectSpec.Damage d ->
                    Optional.of(new TriggerDamage(d.amount(), d.element(), DamageSource.ABILITY_LITERAL));
            case EffectSpec.Burst b        -> firstDamage(b.effects(), weaponAttackDamage);
            case EffectSpec.Area a         -> firstDamage(a.effects(), weaponAttackDamage);
            case EffectSpec.Heal h         -> Optional.empty();
            case EffectSpec.Knockback k    -> Optional.empty();
            case EffectSpec.Status s       -> Optional.empty();
            case EffectSpec.Visual v       -> Optional.empty();
            case EffectSpec.ThrowEmbers t  -> Optional.empty();
        };
    }
}
