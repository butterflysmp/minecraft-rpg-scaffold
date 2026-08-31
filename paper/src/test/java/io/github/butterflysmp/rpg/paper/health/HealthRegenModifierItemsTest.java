package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of the health-regen fixture scan a unit test can reach: its source keys, and the boost
 * the fixture actually resolves to.
 *
 * <p>{@code desiredModifiers} needs a live {@code Player} and is boot-witnessed, like every scanner
 * in this package. What is offline is the prefix and the default amount.
 *
 * <p>Like Mana Bank's and unlike Growth's, <b>this prefix guards nothing that exists yet</b>, and
 * saying so is the point: nothing else feeds the health-regen target today. It is carried so the
 * scanner that RETIRES this fixture -- the Health Regen enchant -- cannot silently overwrite it
 * during whatever overlap the two have.
 */
class HealthRegenModifierItemsTest {

    private static final double EPS = 1e-9;

    @Test
    void everyHealthRegenSourceKeyIsDisjointFromABareSlotNameAndFromTheOtherScannersPrefixes() {
        assertFalse(HealthRegenModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix is what makes a future second scanner able to erase this one");

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String regenKey = HealthRegenModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), regenKey,
                    "the key for " + slot + " must not be the bare slot name -- the crit and "
                            + "attack-speed fixtures use those, and this scanner walks the same slots");
            assertNotEquals(GrowthModifierItems.SOURCE_PREFIX + slot.name(), regenKey,
                    "nor collide with Growth's, in case the targets ever merge");
            assertNotEquals(ManaBankModifierItems.SOURCE_PREFIX + slot.name(), regenKey,
                    "nor with Mana Bank's");
            assertTrue(regenKey.startsWith(HealthRegenModifierItems.SOURCE_PREFIX));
        }
        // The bare-slot-name row is the one with teeth here, and it is stronger than Mana Bank's
        // equivalent: this scanner walks EquipmentSlot.values() exactly as CritModifierItems does,
        // on a player who can hold both fixtures at once. Different targets today, so still not a
        // live collision -- but it is one slot-scan away from being one.
        // Mutation: set SOURCE_PREFIX to "" -> the bare-slot row reddens.
    }

    @Test
    void theFixtureBoostResolvesToFiveTimesBaseSoAShortBootCanCOUNTIt() {
        assertTrue(HealthRegen.boosts(HealthRegenModifierItems.DEFAULT_BOOST),
                "a fixture that declared nothing would leave the reconcile surface unwitnessed");
        assertEquals(1.0, HealthRegen.BASE_PER_SECOND + HealthRegenModifierItems.DEFAULT_BOOST, EPS,
                "0.2 base + 0.8 bonus resolves to 1.0 HP/s -- 1 HP a second is countable at a "
                        + "glance, where a rate you must time with a stopwatch proves nothing");
        // Mutation: DEFAULT_BOOST -> 0.05 (a resolved 0.25 HP/s, a quarter more than base) -> the
        // gate row becomes untellable from base by eye -> reddens.
    }
}
