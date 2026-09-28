package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PLAN-mob-scaling.md §6 F16: {@code /rpg spawn} gives the mobs whose vanilla weapon is GUARANTEED that
 * weapon, because {@code randomizeData = false} skips the {@code finalizeSpawn} that would have.
 *
 * <p><b>The table is asserted WHOLE.</b> An entry added here moves a gate row, and an entry dropped
 * leaves a skeleton punching where G9 expects an arrow. RANGED ONLY, by the seat's ruling: the melee
 * weapons wait on the seed-timing question F16 records.
 */
class DefaultMainHandTest {

    @Test
    void theTableIsExactlyTheTenGuaranteedEntries() {
        // Read from the pinned paper-26.1.2.jar. RANGED: AbstractSkeleton.populateDefaultEquipmentSlots
        // gives BOW (skeleton, stray, bogged, parched declare no override), Illusioner.finalizeSpawn gives
        // BOW, Pillager.populateDefaultEquipmentSlots gives CROSSBOW. MELEE (the seat's ruling of
        // 2026-09-28, once the seed counts the weapon deterministically): WitherSkeleton STONE_SWORD, Vex
        // IRON_SWORD, PiglinBrute GOLDEN_AXE, all unconditional; Vindicator IRON_AXE, only outside a raid,
        // and a /rpg spawn is never in one. Mutations: drop the skeleton's bow, or the wither skeleton's
        // sword -> reddens.
        assertEquals(Map.of(
                        "skeleton", "bow",
                        "stray", "bow",
                        "bogged", "bow",
                        "parched", "bow",
                        "illusioner", "bow",
                        "pillager", "crossbow",
                        "wither_skeleton", "stone_sword",
                        "vindicator", "iron_axe",
                        "vex", "iron_sword",
                        "piglin_brute", "golden_axe"),
                DefaultMainHand.table());
    }

    @Test
    void aMobWithNoGuaranteedWeaponGetsNothing() {
        // Rolled in vanilla, so OFF: the drowned's trident, a zombie's sword, the zombified piglin's and
        // piglin's golden weapons.
        for (String mob : new String[] {"drowned", "zombie", "zombified_piglin", "piglin", "husk", "cow"}) {
            assertEquals(Optional.empty(), DefaultMainHand.of(mob), mob);
        }
    }

    @Test
    void lookupIsByTheVanillaKey() {
        assertEquals(Optional.of("bow"), DefaultMainHand.of("skeleton"));
        assertEquals(Optional.of("crossbow"), DefaultMainHand.of("pillager"));
    }
}
