package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.build.LockedSlots.Refusal;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LockedSlotsTest {

    private static final int STAR_DEFAULT = 8;

    @Test
    void stonePicksOffTheHotbarAreRefused() {
        for (int slot = 9; slot <= 35; slot++) {
            assertEquals(Refusal.OUT_OF_RANGE, LockedSlots.refuseStonePick(slot, STAR_DEFAULT), "slot " + slot);
        }
        assertEquals(Refusal.OUT_OF_RANGE, LockedSlots.refuseStonePick(-1, STAR_DEFAULT));
        for (int slot = 0; slot <= 7; slot++) assertNull(LockedSlots.refuseStonePick(slot, STAR_DEFAULT), "slot " + slot);
    }

    @Test
    void theStarKeepsAllThirtySixSlots() {
        for (int slot = 0; slot <= 35; slot++) {
            if (slot == 7) continue;   // the stone's
            assertNull(LockedSlots.refuseStarPick(slot, 7), "slot " + slot);
        }
        assertEquals(Refusal.OUT_OF_RANGE, LockedSlots.refuseStarPick(36, 7));
    }

    @Test
    void eachRefusesTheOthersSlotAsReserved() {
        assertEquals(Refusal.RESERVED, LockedSlots.refuseStonePick(8, 8));
        assertEquals(Refusal.RESERVED, LockedSlots.refuseStarPick(7, 7));
    }

    @Test
    void theDefaultStoneSlotIsSevenBesideTheStarsEight() {
        assertEquals(7, LockedSlots.stoneSlot(null, STAR_DEFAULT));
    }

    /** The one case no pick produces: the star was moved to 7 before the stone existed. */
    @Test
    void aStarAlreadyInSevenPushesTheDefaultStoneToTheHighestFreeHotbarSlot() {
        assertEquals(8, LockedSlots.stoneSlot(null, 7));
    }

    @Test
    void aStoredSlotIsHonouredUnlessItIsOffTheHotbarOrTheStars() {
        assertEquals(3, LockedSlots.stoneSlot(3, STAR_DEFAULT));
        assertEquals(7, LockedSlots.stoneSlot(20, STAR_DEFAULT));   // hand-edited: off the hotbar
        assertEquals(7, LockedSlots.stoneSlot(3, 3));               // hand-edited: the star's
        assertEquals(8, LockedSlots.stoneSlot(7, 7));
    }

    /**
     * THE CLAIM THE CLASS EXISTS FOR, over sequences rather than one case: after any run of picks,
     * the star and the stone's EFFECTIVE slot differ. Every pick goes through the refusal rule, as the
     * picker does; the star's start is every legal slot, the stone's stored slot starts unset.
     * Seeded, so a failure reproduces.
     */
    @Test
    void theTwoItemsNeverShareASlotAfterAnySequenceOfPicks() {
        Random random = new Random(20260925L);
        for (int starStart = 0; starStart <= 35; starStart++) {
            int star = starStart;
            Integer storedStone = null;
            for (int step = 0; step < 400; step++) {
                int stone = LockedSlots.stoneSlot(storedStone, star);
                assertNotEquals(star, stone, "start " + starStart + " step " + step);
                int candidate = random.nextInt(40) - 2;
                if (random.nextBoolean()) {
                    if (LockedSlots.refuseStarPick(candidate, stone) == null) star = candidate;
                } else {
                    if (LockedSlots.refuseStonePick(candidate, star) == null) storedStone = candidate;
                }
            }
        }
    }
}
