package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.mob.MobDamagePricing;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.From;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.Resolved;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P-1 (PLAN-summoner 11, R1; ruled 2026-11-25): ONE price for a mob's melee, on a player and on a summon.
 *
 * <p>The player path prices a mob's {@code ENTITY_ATTACK} from the dealer's SEEDED attack stat; the summon path
 * priced the event's amount. They differ whenever the event's amount is not the attribute (a scripted amount,
 * Strength, an armed mob, a crit): SM7 read 10.000 where the same zombie on a player reads 15.000.
 *
 * <p>The staging keeps every quantity the row reads distinct (seeded 15, event 2, its event price 10), so a
 * transposition between the two arms has nowhere to hide.
 */
class SummonHitPriceTest {

    private static final double EPS = 1e-9;
    private static final Resolved ZOMBIE_AT_100 = new Resolved(From.CAUSING, false, true, 100);

    @Test
    void aMobsMeleeOnASummonIsPricedFromItsSeededAttackAsOnAPlayer() {
        assertEquals(15.0, SummonHitPrice.of(true, 15.0, 2.0, ZOMBIE_AT_100), EPS,
                "melee: the seeded attack (SM3A's 15.000), never the event's 2.0 priced");
        // Mutation: return the event price on the melee arm -> 10.0 -> reddens.
    }

    @Test
    void everyOtherMobCauseKeepsTheEventPrice() {
        assertEquals(MobDamagePricing.price(2.0, ZOMBIE_AT_100), SummonHitPrice.of(false, 15.0, 2.0, ZOMBIE_AT_100), EPS);
        assertEquals(10.0, SummonHitPrice.of(false, 15.0, 2.0, ZOMBIE_AT_100), EPS,
                "an arrow or a blast: the event's 2.0 x 5 x 1.00 (M16), SM7's reading");
        // Mutation: return the seeded attack on every arm -> 15.0 -> reddens.
    }
}
