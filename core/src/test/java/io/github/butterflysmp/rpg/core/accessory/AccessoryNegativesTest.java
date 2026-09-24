package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ruling Q7: max-HP drawbacks are refused, and other negatives load only under the 4x bound. The
 * bound is checked at the boundary on both sides, per stat, because the error in a comparison lands
 * in exactly one cell.
 */
class AccessoryNegativesTest {

    private static final double MANA = 100.0;

    /** *** THE MAX-HP REFUSAL. The row the max-HP mutation must redden. *** */
    @Test
    void aNegativeMaxHealthIsRefused_atAnySize() {
        for (double amount : new double[] {-0.001, -1, -10, -99}) {
            String why = AccessoryNegatives.refusal(AccessoryStat.MAX_HEALTH, amount, MANA);
            assertNotNull(why, "max HP has no floor -- " + amount + " must be refused");
            assertTrue(why.contains("max_health") && why.contains("no floor"), why);
        }
        assertNull(AccessoryNegatives.refusal(AccessoryStat.MAX_HEALTH, 10, MANA), "a bonus is fine");
        // Mutation: MAX_HEALTH -> Negatives.BOUNDED (with a base) or remove its REFUSED arm -> reddens.
    }

    @Test
    void negativesOnTheOtherRefusedStatsAreRefused() {
        for (AccessoryStat stat : new AccessoryStat[] {
                AccessoryStat.MANA_REGEN, AccessoryStat.DEFENSE, AccessoryStat.CLASS_DAMAGE}) {
            assertNotNull(AccessoryNegatives.refusal(stat, -0.01, MANA), stat.token());
            assertNull(AccessoryNegatives.refusal(stat, 1, MANA), stat.token() + " bonus");
        }
    }

    @Test
    void theBoundedStatsUseTheRealPlayerBases() {
        assertEquals(MANA, AccessoryNegatives.base(AccessoryStat.MAX_MANA, MANA));
        assertEquals(Crit.BASE_CHANCE, AccessoryNegatives.base(AccessoryStat.CRIT_CHANCE, MANA));
        assertEquals(Crit.BASE_DAMAGE, AccessoryNegatives.base(AccessoryStat.CRIT_DAMAGE, MANA));
        assertEquals(HealthRegen.BASE_PER_SECOND, AccessoryNegatives.base(AccessoryStat.HEALTH_REGEN, MANA));
        assertThrows(IllegalArgumentException.class,
                () -> AccessoryNegatives.base(AccessoryStat.MAX_HEALTH, MANA));
    }

    /**
     * The 4x bound at the boundary: {@code 4 x |a| < base}. The exact quarter is REFUSED (equal is
     * not below), a hair under it loads. Crit chance's quarter is 0.0375; the shipped Sage's Scroll
     * carries -0.03 and must load.
     */
    @Test
    void theFourTimesBound_atTheBoundary() {
        assertNull(AccessoryNegatives.refusal(AccessoryStat.MAX_MANA, -24.99, MANA));
        assertNotNull(AccessoryNegatives.refusal(AccessoryStat.MAX_MANA, -25, MANA), "4 x 25 = 100, not below");
        assertNotNull(AccessoryNegatives.refusal(AccessoryStat.MAX_MANA, -30, MANA));

        assertNull(AccessoryNegatives.refusal(AccessoryStat.HEALTH_REGEN, -0.04, MANA), "shipped: fletcher's quiver");
        assertNotNull(AccessoryNegatives.refusal(AccessoryStat.HEALTH_REGEN, -0.05, MANA), "4 x 0.05 = 0.2");

        assertNull(AccessoryNegatives.refusal(AccessoryStat.CRIT_CHANCE, -0.03, MANA), "shipped: sage's scroll");
        assertNotNull(AccessoryNegatives.refusal(AccessoryStat.CRIT_CHANCE, -0.04, MANA), "4 x 0.04 = 0.16 > 0.15");

        assertNull(AccessoryNegatives.refusal(AccessoryStat.CRIT_DAMAGE, -0.2, MANA));
        assertNotNull(AccessoryNegatives.refusal(AccessoryStat.CRIT_DAMAGE, -0.25, MANA));

        assertNull(AccessoryNegatives.refusal(AccessoryStat.MAX_MANA, -10, MANA), "shipped: brawler's gauntlet");
    }

    @Test
    void aNonNegativeAmountIsNeverRefusedHere() {
        for (AccessoryStat stat : AccessoryStat.values()) {
            assertNull(AccessoryNegatives.refusal(stat, 0.5, MANA), stat.token());
        }
    }
}
