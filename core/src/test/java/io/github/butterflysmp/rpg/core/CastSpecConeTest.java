package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a cone refuses, by name (the seat's brief of 2026-11-22, ruling 1). Each refused value would load and do
 * nothing, or nonsense: no bundled content reaches these arms (the Living Lantern authors 6, 60, 6), so this test, which
 * CAUSES each condition, is their only exercise.
 */
class CastSpecConeTest {

    @Test
    void aSaneConeIsAccepted() {
        assertDoesNotThrow(() -> new CastSpec.Cone(6, 60, 6, "front"));
        assertDoesNotThrow(() -> new CastSpec.Cone(6, 60, 0, null), "instant, and no front visual");
        assertDoesNotThrow(() -> new CastSpec.Cone(6, 179.9, 6, null), "just under a half-space");
    }

    @Test
    void aRangeAtOrBelowZeroIsRefused() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CastSpec.Cone(0, 60, 6, null));
        assertTrue(e.getMessage().contains("range"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Cone(Double.NaN, 60, 6, null));
    }

    /** 0 strikes nothing; 180 and more has no front ring (tan 90) and reaches behind the caster. */
    @Test
    void anArcOutsideZeroToOneEightyIsRefused() {
        for (double arc : new double[] {0, -10, 180, 270, Double.NaN}) {
            var e = assertThrows(IllegalArgumentException.class, () -> new CastSpec.Cone(6, arc, 6, null), "arc " + arc);
            assertTrue(e.getMessage().contains("arc_degrees"), e.getMessage());
        }
    }

    @Test
    void aNegativeTravelIsRefused() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CastSpec.Cone(6, 60, -1, null));
        assertTrue(e.getMessage().contains("travel_ticks"), e.getMessage());
    }
}
