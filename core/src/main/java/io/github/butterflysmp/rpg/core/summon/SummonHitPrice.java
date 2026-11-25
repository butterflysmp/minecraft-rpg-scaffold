package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.mob.MobDamagePricing;

/**
 * P-1 (PLAN-summoner 11, R1; ruled 2026-11-25): what a mob's hit on a SUMMON costs it, with the melee priced exactly
 * as a mob's melee on a player.
 *
 * <ul>
 *   <li><b>Melee ({@code ENTITY_ATTACK})</b>: the dealer's SEEDED attack stat, the number
 *       {@code RpgListeners.onMobMeleeAttack} prices a player's hit from. The event's own amount is not a term.</li>
 *   <li><b>Every other mob cause</b> (an arrow, a blast, magic): {@link MobDamagePricing#price} of the event's amount,
 *       which is how M16 prices those causes on a player too.</li>
 * </ul>
 *
 * <p>Its own class rather than a method on {@link MobDamagePricing}, whose test pins that no public method there takes
 * a second double (the door a player max would come in by).
 */
public final class SummonHitPrice {

    private SummonHitPrice() {}

    /**
     * @param melee        the cause is {@code ENTITY_ATTACK}
     * @param seededAttack the dealer's seeded attack stat ({@code CombatantStats.attackValue})
     * @param eventAmount  the vanilla event's amount
     * @param source       the resolved mob source, for the non-melee price
     */
    public static double of(boolean melee, double seededAttack, double eventAmount, MobDamagePricing.Resolved source) {
        return melee ? seededAttack : MobDamagePricing.price(eventAmount, source);
    }
}
