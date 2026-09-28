package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.mob.MobDamagePricing;

import java.util.Optional;

/**
 * The price of one rerouted vanilla hit (PLAN-mob-scaling.md, slice 2). It CHOOSES between two prices
 * and never chains them:
 *
 * <pre>
 *   a mob-sourced hit on a PLAYER    MobDamagePricing.price   flat, x5 x GS/100 (M16); no player max
 *   everything else                  DamageScale.toCustom     the victim's proportion, as before
 * </pre>
 *
 * <p>"Everything else" includes a mob-sourced hit on a MOB (§6 F12: proportional to the victim's bar,
 * with no attacker GS) and every sourceless cause: fall, lava, and the damage-over-time ticks that name
 * no entity (§6 F5).
 *
 * <p><b>Why a chooser rather than a branch in the listener:</b> "one or the other, never both" is the
 * slice's load-bearing rule, and a branch in an event handler cannot be unit-tested.
 * {@code RerouteDamagePriceTest} asserts every mob row at max 100 AND max 400; at 100 alone the stacked
 * price and the flat one differ only by a further x5, and the old unstacked pricing is identical.
 */
public final class RerouteDamagePrice {

    private RerouteDamagePrice() {}

    /**
     * @param vanillaAmount       the event's raw vanilla amount, after the damage window and the shield
     * @param mobSource           {@link MobDamagePricing#resolve}'s answer; empty when not mob-sourced
     * @param victimIsPlayer      the victim is a player. M16 is about mob-to-PLAYER hits only
     * @param customMax           the victim's custom max, for {@link DamageScale} only
     * @param vanillaMaxAttribute the victim's real MAX_HEALTH attribute, for {@link DamageScale} only
     * @param barIsPuppeted       for {@link DamageScale} only
     */
    public static double of(double vanillaAmount, Optional<MobDamagePricing.Resolved> mobSource,
                            boolean victimIsPlayer, double customMax, double vanillaMaxAttribute,
                            boolean barIsPuppeted) {
        if (victimIsPlayer && mobSource.isPresent()) {
            return MobDamagePricing.price(vanillaAmount, mobSource.get());
        }
        return DamageScale.toCustom(vanillaAmount, customMax, vanillaMaxAttribute, barIsPuppeted);
    }
}
