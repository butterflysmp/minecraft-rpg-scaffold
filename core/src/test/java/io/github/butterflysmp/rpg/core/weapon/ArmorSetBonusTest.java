package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** B1 (the seat, 2026-11-25): the set bonus entry, its refusals, and its ONE unit conversion. */
class ArmorSetBonusTest {

    @Test
    void healthRegenIsAuthoredPerFiveSecondsAndReachesTheEnginePerSecond() {
        // Ben: "+2 regen (this is to the every 5s number)". 1 per 5 s (0.2/s, the base) becomes 3 per 5 s (0.6/s).
        assertEquals(0.4, new ArmorSetBonus(ArmorSetBonus.Type.HEALTH_REGEN, 2).engineValue(), 1e-12);
        // Mutation: drop the division -> 2.0 -> reddens.
    }

    @Test
    void everyOtherTypeIsAuthoredInTheEnginesUnit() {
        assertEquals(2.0, new ArmorSetBonus(ArmorSetBonus.Type.MANA_REGEN, 2).engineValue());
        assertEquals(1.0, new ArmorSetBonus(ArmorSetBonus.Type.MAX_SUMMONS, 1).engineValue());
        assertEquals(100.0, new ArmorSetBonus(ArmorSetBonus.Type.MAX_HEALTH, 100).engineValue());
        assertEquals(40.0, new ArmorSetBonus(ArmorSetBonus.Type.DEFENSE, 40).engineValue());
    }

    @Test
    void zeroAndNonFiniteValuesAreRefused() {
        for (double bad : new double[] {0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ArmorSetBonus(ArmorSetBonus.Type.DEFENSE, bad),
                    "value " + bad);
        }
    }

    @Test
    void aFractionalSummonIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ArmorSetBonus(ArmorSetBonus.Type.MAX_SUMMONS, 1.5));
    }

    @Test
    void tokensAreTheLowerCaseNamesAndAnUnknownOneIsNull() {
        assertEquals(ArmorSetBonus.Type.HEALTH_REGEN, ArmorSetBonus.Type.fromToken("health_regen"));
        assertEquals(ArmorSetBonus.Type.MAX_SUMMONS, ArmorSetBonus.Type.fromToken("MAX_SUMMONS"));
        assertNull(ArmorSetBonus.Type.fromToken("quiver_size_percent"), "STOPPED (B3): not a type, so refused");
        assertNull(ArmorSetBonus.Type.fromToken("regen"));
        assertNull(ArmorSetBonus.Type.fromToken(null));
    }
}
