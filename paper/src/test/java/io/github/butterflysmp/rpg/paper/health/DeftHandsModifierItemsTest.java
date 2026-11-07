package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Deft Hands' scanner decision (PLAN-deft-hands.md; the seat's rulings, 2026-11-06), Player-free. The tiers
 * themselves are {@code ReloadTimeTest}'s; this pins what the SCANNER does with them: the authored reload it is
 * handed, the {@code declares} gate, the key, and the merge with the dev instrument.
 */
class DeftHandsModifierItemsTest {

    /**
     * THE GATE ROW. Every Deft Hands contribution is NEGATIVE, so the scanner must gate on
     * {@code ReloadTime.declares} ({@code != NONE}). Mutation: {@code > NONE} declares nothing -- the map comes back
     * EMPTY and the enchant does nothing, silently. This row reddens on it.
     */
    @Test
    void aBoltorWithDeftHandsIIIDeclaresANegativeEighteenTickCut() {
        assertEquals(Map.of("defthands:HAND", -18.0), DeftHandsModifierItems.desired(60, 30.0));
    }

    @Test
    void theCutIsOfTheHeldWeaponsAuthoredReload() {
        assertEquals(Map.of("defthands:HAND", -4.0), DeftHandsModifierItems.desired(40, 10.0), "dragons_breath I");
        assertEquals(Map.of("defthands:HAND", -11.0), DeftHandsModifierItems.desired(34, 30.0),
                "quiver_stone III: 10.2 floors to an 11-tick cut");
    }

    /** hunters_bow (no reload) can carry the enchant, and contributes NOTHING: the finding, not an error. */
    @Test
    void aWeaponWithNoReloadContributesNothing() {
        assertEquals(Map.of(), DeftHandsModifierItems.desired(0, 30.0));
    }

    @Test
    void aWeaponWithoutTheEnchantContributesNothing() {
        assertEquals(Map.of(), DeftHandsModifierItems.desired(60, 0.0));
    }

    /** The instrument's bare-slot keys and the enchant's namespaced one both survive the ONE reconcile. */
    @Test
    void theInstrumentAndTheEnchantMergeWithoutEitherWipingTheOther() {
        Map<String, Double> merged = DeftHandsModifierItems.mergedSources(Map.of("reloadtime:HAND", -30.0),
                DeftHandsModifierItems.desired(34, 30.0));
        assertEquals(Map.of("reloadtime:HAND", -30.0, "defthands:HAND", -11.0), merged);
    }
}
