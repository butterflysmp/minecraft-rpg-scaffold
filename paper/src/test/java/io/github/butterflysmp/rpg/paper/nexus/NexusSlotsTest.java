package io.github.butterflysmp.rpg.paper.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one decidable thing in {@code NexusSlots}: the slot bound.
 *
 * <h2>WHY THIS FILE IS ONE METHOD WIDE, AND WHY IT EXISTS AT ALL</h2>
 *
 * {@code NexusSlots} is the adapter. {@code converge}, {@code lockedSlotOf}, {@code starAt} and
 * {@code touchedOf} all need a live {@code Player}, {@code PlayerInventory} or {@code InventoryView},
 * none of which exist without a server -- the class javadoc says so and points at
 * {@code GATE-nexus.md} rows 4-5 as its coverage. <b>That is unchanged and this file does not
 * pretend otherwise.</b>
 *
 * <p><b>{@code validSlotOr} is the exception, and it was MADE the exception on purpose.</b> The
 * bound lived inline in two places until a mutation removing the range check killed NOTHING --
 * measured, not assumed. It could not be reached by any test, and two copies of a bound is how the
 * lock and the placement come to disagree about which slot is protected, which is the defect this
 * slice exists to prevent.
 *
 * <p>So the bound is one pure function now, and this is what holds it.
 */
class NexusSlotsTest {

    /** Something distinguishable from every real slot and from the sentinel. */
    private static final int FALLBACK = 99;

    @Test
    void everyMAININVENTORYSlotIsAccepted_allTHIRTYSIXOfThem() {
        // WIDENED FROM THE HOTBAR. This loop ran 0..8 while the star was a held item only; the
        // star now lives anywhere in the main inventory, and THIS ONE BOUND is the whole of that
        // change -- slice 4a had already put NexusLock in PlayerInventory index space.
        for (int slot = 0; slot <= 35; slot++) {
            assertEquals(slot, NexusSlots.validSlotOr(slot, FALLBACK),
                    "inventory slot " + slot + " is a legal choice and must be returned unchanged");
        }
        // AND THE OLD RANGE IS STILL ACCEPTED, WHICH IS WHY THERE IS NO MIGRATION. Every stored
        // value today is 0-8; a widened bound cannot invalidate anything it previously accepted.
        for (int slot = 0; slot <= 8; slot++) {
            assertEquals(slot, NexusSlots.validSlotOr(slot, FALLBACK),
                    "a slot stored before the widening is still legal after it");
        }
        // SLOT 0 IS THE ONE WORTH NAMING. It is what Gson leaves for an absent int, so it is also
        // the value a pre-v3 profile would carry if the migration ever stopped setting it -- and it
        // is nonetheless a perfectly legal choice for a player who made it. This function must not
        // be the place that tries to tell those apart; ProfileMigrations does that, by the stamp.
    }

    @Test
    void anythingOUTSIDETheMainInventoryFallsBack() {
        assertEquals(FALLBACK, NexusSlots.validSlotOr(-1, FALLBACK),
                "NO_LOCKED_SLOT itself -- an unknown slot is not a slot");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(36, FALLBACK),
                "36 is BOOTS, the first armour cell -- the off-by-one that matters now that "
                        + "storage is legal. The star has no business in an armour slot");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(40, FALLBACK), "the offhand");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(41, FALLBACK),
                "one past the end of the inventory -- setItem would throw from a join handler");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(Integer.MAX_VALUE, FALLBACK),
                "a hand-edited JSON file reaches this code without passing through any setter");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(Integer.MIN_VALUE, FALLBACK),
                "and so does a negative one");
        // Mutation MUTNOVALIDATE: drop the range check -> kill set RECORDED in the PR body.
    }

    @Test
    void theBOUNDARIESAreInclusive_bothOfThem() {
        // Stated separately from the loop above because an off-by-one at either end is the whole
        // failure mode, and a loop that happens to cover 0..35 does not SAY that 35 is the last one.
        assertEquals(35, NexusSlots.validSlotOr(35, FALLBACK), "35 is IN -- the last storage cell");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(36, FALLBACK), "36 is OUT -- boots");
        assertEquals(0, NexusSlots.validSlotOr(0, FALLBACK), "0 is IN");
        assertEquals(FALLBACK, NexusSlots.validSlotOr(-1, FALLBACK), "-1 is OUT");

        // THE OLD BOUNDARY IS NOW INTERIOR, AND SAYING SO IS THE POINT: 8 and 9 used to be the
        // in/out pair, and a build that had not been widened would still pass every assertion above
        // that names 0 or -1. This is the pair that reddens on it.
        assertEquals(8, NexusSlots.validSlotOr(8, FALLBACK), "8 is IN -- it is still the default");
        assertEquals(9, NexusSlots.validSlotOr(9, FALLBACK),
                "and 9, the first STORAGE cell, is IN NOW -- it was the boundary before the widening");
    }

    @Test
    void theFALLBACKIsTheCallersAndIsNotInterpreted() {
        // The two callers pass DIFFERENT fallbacks for different reasons -- lockedSlotOf passes
        // NO_LOCKED_SLOT so the lock protects nothing, converge passes DEFAULT_LOCKED_SLOT so the
        // star still gets placed. A function that picked one for them would be wrong for the other.
        assertEquals(NexusLock.NO_LOCKED_SLOT,
                NexusSlots.validSlotOr(50, NexusLock.NO_LOCKED_SLOT),
                "lockedSlotOf's fallback: unknown, so the locked-slot arm goes inert");
        assertEquals(NexusLock.DEFAULT_LOCKED_SLOT,
                NexusSlots.validSlotOr(50, NexusLock.DEFAULT_LOCKED_SLOT),
                "converge's fallback: the star must still land somewhere");
    }
}
