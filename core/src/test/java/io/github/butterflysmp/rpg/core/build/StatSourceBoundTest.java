package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessoryNegatives;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ONE combined negative bound across accessories and fragments (section 2.3, amendment 3, ruling 21).
 */
class StatSourceBoundTest {

    private static final double MANA = 100.0;

    /** The arithmetic, stated as numbers: 4 accessory slots + 4 fragment slots x 0 negatives = 4. */
    @Test
    void theNegativeSourcesAreEveryAccessorySlotAndNoFragmentSlot() {
        assertEquals(AccessorySlots.COUNT, StatSourceBound.ACCESSORY_NEGATIVE_SOURCES);
        assertEquals(0, StatSourceBound.FRAGMENT_NEGATIVE_SOURCES, "ruling 21: fragments are positive-only");
        assertEquals(4, StatSourceBound.NEGATIVE_SOURCES);
        assertEquals(8, AccessorySlots.COUNT + FragmentSlots.COUNT,
                "eight sources can move a stat; only four of them can move it DOWN");
    }

    /** N x |amount| < base, at the boundary, both sides. */
    @Test
    void theBoundAtTheBoundaryBothSides() {
        assertTrue(StatSourceBound.withinBound(-24.99, MANA));
        assertTrue(!StatSourceBound.withinBound(-25, MANA), "4 x 25 = 100, not below 100");
        assertTrue(StatSourceBound.withinBound(0.5, MANA), "a positive amount is always within");
    }

    /**
     * THE PAIR CASE: four accessories each at the largest negative the bound accepts, and four fragments at
     * the most negative a fragment can be (zero -- a negative is refused), cannot carry a bounded stat to its
     * floor. Checked for every bounded stat, at the largest accepted amount found by search, not by hand.
     */
    @Test
    void fourAccessoriesAtTheBoundAndFourFragmentsCannotReachTheFloor() {
        for (AccessoryStat stat : AccessoryStat.values()) {
            if (stat.negatives() != AccessoryStat.Negatives.BOUNDED) continue;
            double base = AccessoryNegatives.base(stat, MANA);
            double worst = largestAcceptedNegative(stat, base);
            assertNull(AccessoryNegatives.refusal(stat, worst, MANA), stat.token() + " " + worst + " is accepted");
            double fromAccessories = AccessorySlots.COUNT * worst;
            double fromFragments = FragmentSlots.COUNT * 0.0;
            assertTrue(base + fromAccessories + fromFragments > 0,
                    stat.token() + ": base " + base + " + " + fromAccessories + " + " + fromFragments + " reached the floor");
        }
    }

    /** And the fragment side of the pair really is zero: a negative fragment cannot be built. */
    @Test
    void aFragmentCannotCarryANegative() {
        assertThrows(IllegalArgumentException.class, () -> new FragmentDefinition("f", "F", "red_dye", List.of(),
                Map.of(AccessoryStat.CRIT_CHANCE, -0.001)));
    }

    /** The shipped accessories' drawbacks all pass (the measurement that produced ruling 21). */
    @Test
    void everyShippedAccessoryDrawbackIsWithinTheCombinedBound() {
        assertNull(AccessoryNegatives.refusal(AccessoryStat.MAX_MANA, -10, MANA), "brawler's gauntlet");
        assertNull(AccessoryNegatives.refusal(AccessoryStat.HEALTH_REGEN, -0.04, MANA), "fletcher's quiver");
        assertNull(AccessoryNegatives.refusal(AccessoryStat.CRIT_CHANCE, -0.03, MANA), "sage's scroll");
        assertNotNull(AccessoryNegatives.refusal(AccessoryStat.CRIT_CHANCE, -0.04, MANA), "the control: 4 x 0.04 > 0.15");
    }

    /** The most negative amount, on a 1e-6 grid, that AccessoryNegatives accepts for this stat. */
    private static double largestAcceptedNegative(AccessoryStat stat, double base) {
        double step = base / 1_000_000.0;
        double amount = -base;
        while (AccessoryNegatives.refusal(stat, amount, MANA) != null) amount += step;
        return amount;
    }
}
