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
    void theTableIsExactlyTheSixRangedEntries() {
        // Read from the pinned paper-26.1.2.jar: AbstractSkeleton.populateDefaultEquipmentSlots gives
        // BOW (skeleton, stray, bogged, parched declare no override), Illusioner.finalizeSpawn gives BOW,
        // Pillager.populateDefaultEquipmentSlots gives CROSSBOW. Each is unconditional.
        // Mutation: drop the skeleton's bow -> reddens.
        assertEquals(Map.of(
                        "skeleton", "bow",
                        "stray", "bow",
                        "bogged", "bow",
                        "parched", "bow",
                        "illusioner", "bow",
                        "pillager", "crossbow"),
                DefaultMainHand.table());
    }

    @Test
    void aMobWithNoGuaranteedWeaponGetsNothing() {
        // Rolled in vanilla, so OFF: the drowned's trident, a zombie's sword, the zombified piglin's
        // golden weapon. Melee held back by ruling: the wither skeleton, vindicator, vex, piglin brute.
        for (String mob : new String[] {"drowned", "zombie", "zombified_piglin", "piglin",
                "wither_skeleton", "vindicator", "vex", "piglin_brute", "cow"}) {
            assertEquals(Optional.empty(), DefaultMainHand.of(mob), mob);
        }
    }

    @Test
    void lookupIsByTheVanillaKey() {
        assertEquals(Optional.of("bow"), DefaultMainHand.of("skeleton"));
        assertEquals(Optional.of("crossbow"), DefaultMainHand.of("pillager"));
    }
}
