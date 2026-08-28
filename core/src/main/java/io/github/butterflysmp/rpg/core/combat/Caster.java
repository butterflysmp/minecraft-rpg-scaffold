package io.github.butterflysmp.rpg.core.combat;

import java.util.UUID;

/**
 * The caster as a SOURCE of effects: an identity, plus the stats an effect needs when it lands.
 * Frozen at cast time, on the caster's own thread.
 *
 * This deliberately is NOT a {@link CombatantSnapshot}, though it is projected from one. A snapshot
 * describes a combatant as a TARGET -- where it is, whether it is alive, whether it is a player --
 * and those are facts about the cast frame. An effect can outlive that frame by a long way: a
 * projectile flies for up to a hundred ticks, and a lingering {@code Area} pulses for seconds after
 * its caster has logged out. Threading the whole snapshot down would put a stale position and a
 * stale liveness flag in reach of code running much later, where they read as current and are not.
 * This record carries only what stays TRUE about the caster-as-source once frozen.
 *
 * Which is also why the stats live here rather than being re-read at impact. {@code attackDamage}
 * is the caster's resolved ATTACK_DAMAGE, captured under {@code Regions.requireOwned} on the thread
 * that owns the caster. A projectile's impact resolves on the TARGET'S region -- cross-region from
 * the caster on Folia -- so reading the store there would be exactly the off-thread read the
 * snapshot/handle split exists to prevent. Melee reads the same frozen value: cast is effectively
 * hit for a swing within reach, so unifying costs nothing and leaves ONE path rather than two with
 * the race half-present.
 *
 * {@code classDamageBonus} is the class-typed damage modifier pass this record's javadoc used to
 * PREDICT, now landed. It is the sum of the caster's equipped {@code +N <Class> Damage} gear whose
 * class matches the class of the weapon they hold, and {@code EffectApplier} adds it to BOTH direct
 * damage arms -- the {@code WeaponDamage} arm's stat and the literal {@code Damage} arm's authored
 * amount alike. Being a field here rather than a parameter is what the prediction was about: every
 * method that threads a caster already carries it, and the cast-time freeze that makes
 * {@code attackDamage} Folia-safe covers it for free.
 *
 * {@code enchantDamagePercent} is the damage-modifier enchant type, landed one pass later still. It
 * is the sum of the percentages granted by the damage enchants ACTIVE ON THE HELD WEAPON whose class
 * matches that weapon's own -- Sharpness on a sword, Power on a bow, Attunement on a staff -- and
 * {@code EffectApplier} multiplies BOTH damage arms' base by it before adding
 * {@code classDamageBonus}.
 *
 * <p><b>Percent on the base, flat bonus on top.</b> The order is a real design choice and the two
 * candidates give different numbers: an 8-damage sword with Sharpness III and +5 Melee deals
 * {@code 8 * 1.15 + 5 = 14.2}, not {@code (8 + 5) * 1.15 = 14.95}. The enchant scales the WEAPON, so
 * it multiplies what the weapon contributes; gear adds after. If those numbers ever swap, this
 * ordering has been inverted.
 *
 * <p>It is a PERCENT and not a multiplier for the reason recorded on {@code CombatantSnapshot} and
 * {@code HealthState}: it rides an additive {@code Stat}, percentages compose by addition, and 0.0
 * stays the one absent-value convention. The multiplier is computed at the arm by
 * {@code DamageEnchants.multiplier}, which is the only place the {@code 1 + pct/100} formula exists.
 *
 * Two things it deliberately is NOT. It is not the weapon's inherent damage -- that stays in
 * ATTACK_DAMAGE (or in the literal), so the bonus adds on top rather than replacing, and the
 * emberblade's fireball can take +Melee without inheriting the swing's 8. And it is not gated on
 * whether the payload is a basic attack: the gate is the HELD WEAPON'S CLASS, which is what lets the
 * bonus reach a literal-damage weapon like {@code ember_staff} that reads no stat at all.
 */
public record Caster(UUID id, double attackDamage, double classDamageBonus,
                     double enchantDamagePercent, double chargeScale, double critMultiplier) {

    /**
     * Did this cast crit? DERIVED from the frozen multiplier rather than carried beside it, so the
     * two cannot disagree about one swing -- the same reason {@code MeleeHits.landedThisTick} derives
     * its answer from the window stamp instead of storing a second flag.
     *
     * <p>Strictly GREATER than {@link Crit#NO_CRIT}, not merely different from it. A crit whose bonus
     * resolved to 0 multiplies by exactly 1.0 and changed nothing, so it must not flash a particle or
     * colour a number -- there is no hit to celebrate. A negative bonus is refused by the same
     * comparison rather than by a second guard.
     */
    public boolean crit() {
        return critMultiplier > Crit.NO_CRIT;
    }

    /**
     * Project a cast-time snapshot down to what an effect landing later is allowed to read, at
     * FULL charge -- every caller except the basic melee hit.
     *
     * <p>Exact, not approximate: execution confirms {@code AttackCharge.scale(1.0) == 1.0} in
     * binary floating point, so an ability, projectile or area routed through here deals precisely
     * what it dealt before the charge factor existed. Had that identity only been approximate, the
     * whole game's damage would have drifted by a rounding error the day this field landed.
     */
    public static Caster of(CombatantSnapshot snapshot) {
        return of(snapshot, AttackCharge.FULL_CHARGE);
    }

    /**
     * The basic melee entry point: the same projection, carrying how much of the swing was earned.
     *
     * <p>{@code chargeScale} is a MULTIPLIER whose neutral value is 1.0 -- the convention
     * {@code CombatantSnapshot.attackSpeed} uses, and deliberately NOT the 0.0-is-absent convention
     * the three summands above use. The asymmetry is the price of being multiplicative: 0.0 here
     * would mean "this hit lands nothing", which is a real value rather than an absent one, so
     * there is no safe absent encoding and every caller must state its charge.
     *
     * <p>It rides the record rather than travelling as a parameter for the same reason the other
     * three do: {@code EffectApplier} needs it at the damage arms, and threading a fifth argument
     * through every applier method would put it in reach of the untargeted effects that must never
     * scale by it.
     */
    public static Caster of(CombatantSnapshot snapshot, double chargeScale) {
        return new Caster(snapshot.id(), snapshot.attackDamage(), snapshot.classDamageBonus(),
                snapshot.enchantDamagePercent(), chargeScale, snapshot.critMultiplier());
    }
}
