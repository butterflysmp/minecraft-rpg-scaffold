package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of the mana-regen fixture scan a unit test can reach: its source keys, and what the
 * fixture actually resolves to.
 *
 * <p>{@code desiredModifiers} needs a live {@code Player} and is boot-witnessed, like every scanner
 * in this package.
 *
 * <p><b>This prefix guards something real, unlike Mana Bank's.</b> Three fixtures now walk
 * {@code EquipmentSlot.values()} on the same player -- crit, health regen and this one -- and a
 * player can hold all of them at once. Bare slot names would collide outright.
 */
class ManaRegenModifierItemsTest {

    private static final double EPS = 1e-9;

    /** The shipped base rate, per second, derived the safe direction: from the per-tick constant. */
    private static final double BASE_PER_SECOND = ManaRegen.perSecond(100.0 / (60 * 20));

    @Test
    void everyManaRegenSourceKeyIsDisjointFromABareSlotNameAndFromEveryOtherScannersPrefix() {
        assertFalse(ManaRegenModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix would collide with the crit and health-regen fixtures outright -- "
                        + "they walk the same slots on the same player");

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String key = ManaRegenModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), key,
                    "the key for " + slot + " must not be the bare slot name");
            assertNotEquals(HealthRegenModifierItems.SOURCE_PREFIX + slot.name(), key,
                    "nor collide with the health-regen fixture's, which scans the same slots");
            assertNotEquals(GrowthModifierItems.SOURCE_PREFIX + slot.name(), key, "nor Growth's");
            assertNotEquals(ManaBankModifierItems.SOURCE_PREFIX + slot.name(), key,
                    "nor Mana Bank's -- and this pair matters most, because both feed the SAME "
                            + "ManaTransition call");
            assertTrue(key.startsWith(ManaRegenModifierItems.SOURCE_PREFIX));
        }
        // The Mana Bank row has real teeth: both scanners' outputs are handed to ManaTransition on
        // the same tick, against two different targets. They do not share a target today, so this is
        // not a live collision -- but they are the two closest-related maps in the codebase.
        // Mutation: set SOURCE_PREFIX to "" -> the bare-slot row reddens. Set it to "manabank:" ->
        // the Mana Bank row reddens.
    }

    @Test
    void theFixtureBoostIsBigEnoughToWATCHWithoutAStopwatch() {
        assertTrue(ManaRegen.boosts(ManaRegenModifierItems.DEFAULT_BOOST),
                "a fixture that declared nothing would leave the reconcile surface unwitnessed");
        assertEquals(1.6666666666666665, BASE_PER_SECOND, 0.0,
                "the base rate per second, DERIVED from the per-tick constant -- not the "
                        + "1.6666666666666667 that writing 100.0/60 by hand would give");
        assertEquals(2.6666666666666665, BASE_PER_SECOND + ManaRegenModifierItems.DEFAULT_BOOST, 0.0,
                "+1.0/s resolves to about 2.67/s -- a bare bar fills in ~37s instead of 60, which "
                        + "is a difference you can see rather than time");
        // Mutation: DEFAULT_BOOST -> 0.05 -> the resolved rate is indistinguishable from base by eye
        // and the gate row stops discriminating -> reddens.
    }
}
