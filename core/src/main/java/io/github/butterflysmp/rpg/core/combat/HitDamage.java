package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;

/**
 * How one hit's damage is composed, in two steps and ONE place.
 *
 * <p>{@code EffectApplier} wrote this formula out twice -- once for an ability's authored literal and
 * once for the basic attack -- token-for-token identical but for the base. Only
 * {@link DamageEnchants#multiplier} was extracted; the shape around it was duplicated. A stat sheet
 * wanting to show "what a swing hits for" would have been the THIRD copy, which is why the extraction
 * came with the sheet rather than before it.
 *
 * <h2>Two methods, not one, and the split is where the risk is</h2>
 *
 * {@link #hitBase} is the part with an ordering hazard in it. {@link #dealt} is a product of two
 * multipliers, where order cannot change the answer. Splitting them lets a display ask for the
 * quantity it actually means -- a nominal hit -- instead of passing neutral {@code 1.0}s for a charge
 * it has not made and a crit it has not rolled. {@code dealt(hitBase, 1, 1)} is exactly
 * {@code hitBase}, so the sheet's number IS a full-charge non-crit swing rather than something that
 * resembles one.
 *
 * <h2>PERCENT ON THE WEAPON'S BASE, FLAT GEAR BONUS ON TOP</h2>
 *
 * <b>This is the one home for that ordering, and the worked example that distinguishes it.</b> An
 * 8-damage sword with Sharpness III and +5 Melee deals {@code 8 * 1.15 + 5 = 14.2}, NOT
 * {@code (8 + 5) * 1.15 = 14.95}. The enchant scales the WEAPON, so it scales what the weapon
 * contributes; the gear bonus is a separate grant added after. Those two numbers are what tell the
 * designs apart -- <b>if anything ever reads 14.95, the multiply has been moved outside the
 * addition.</b>
 *
 * <p>That example previously appeared in three places at once ({@code EffectApplier}'s arm comment,
 * {@code Caster}'s javadoc, and {@code AttackCharge}'s), which is three chances for it to drift from
 * the code. They now point here.
 *
 * <p>The caller passes a PERCENT, not a multiplier, so that 0 stays the one absent-value convention
 * across every summand on the snapshot; {@link DamageEnchants#multiplier} owns the
 * {@code 1 + pct/100} conversion so no caller can disagree about it.
 *
 * <h2>What this does NOT guarantee</h2>
 *
 * It guarantees every caller shares the FORMULA. It cannot guarantee they share the INPUTS. The
 * combat path reads its three summands from a {@code CombatantSnapshot} frozen at cast time; a
 * display reads them live from the store. Those agree only while the projection is a straight read of
 * the same three accessors -- which it is, verified at
 * {@code BukkitCombatant.snapshot} -- and nothing here would catch it if that ever stopped being
 * true. The boot gate's swing-and-compare row is the check for that, not a unit test.
 *
 * <p>Worked: {@code hitBase(8, 15, 5)} is {@code 14.2}; {@code dealt(14.2, 1.0, 1.0)} is
 * {@code 14.2}; {@code dealt(14.2, 0.5, 2.0)} is {@code 14.2}.
 */
public final class HitDamage {

    private HitDamage() {}

    /**
     * The hit before charge and crit: {@code base * multiplier(pct) + classBonus}.
     *
     * <p>The ORDERING lives here. See the class javadoc for the 14.95 witness that distinguishes it
     * from the alternative.
     *
     * @param base                 the weapon's attack damage, or an ability's authored literal
     * @param enchantDamagePercent a PERCENT (0 is neutral), converted by {@link DamageEnchants#multiplier}
     * @param classDamageBonus     a flat summand (0 is neutral), added AFTER the multiply
     */
    public static double hitBase(double base, double enchantDamagePercent, double classDamageBonus) {
        return base * DamageEnchants.multiplier(enchantDamagePercent) + classDamageBonus;
    }

    /**
     * The hit after charge and crit: {@code hitBase * chargeScale * critMultiplier}.
     *
     * <p>Both factors scale the WHOLE combined amount, flat class bonus included. Scaling only the
     * weapon base would leave that bonus as a spam-proof damage floor, and with enough {@code +N}
     * gear more swings would beat timed swings -- the model inverted. See {@link AttackCharge}.
     *
     * <p>Neither factor's neutral value is 0: both are multipliers whose identity is
     * {@link AttackCharge#FULL_CHARGE} and {@link Crit#NO_CRIT}, both exactly {@code 1.0}, so a caller
     * with no charge and no crit gets an EXACT identity rather than a near one.
     */
    public static double dealt(double hitBase, double chargeScale, double critMultiplier) {
        return hitBase * chargeScale * critMultiplier;
    }
}
