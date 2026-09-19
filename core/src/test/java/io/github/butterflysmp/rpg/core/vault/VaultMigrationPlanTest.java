package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the ender chest lands, and what it refuses to land on. */
class VaultMigrationPlanTest {

    /**
     * Slot-for-slot, into an empty page.
     *
     * <p>Chest slots 0, 13 and 26 -- the two ends and a middle. <b>26 is the load-bearing one</b>:
     * it is the last cell of a 27-slot chest, and an off-by-one in the loop bound drops exactly that
     * stack and nothing else, which no row staged in the middle can see.
     */
    @Test
    void anEmptyPageTakesTheChestSlotForSlot() {
        VaultMigrationPlan.Placement placed =
                VaultMigrationPlan.plan(Set.of(), Set.of(0, 13, 26));

        assertEquals(3, placed.moves().size(), placed.moves().toString());
        assertEquals(0, (int) placed.moves().get(0));
        assertEquals(13, (int) placed.moves().get(13));
        assertEquals(26, (int) placed.moves().get(26));
        assertTrue(placed.skipped().isEmpty(), placed.skipped().toString());
    }

    /** An empty chest is a real case -- a player who never used one -- and it is not an error. */
    @Test
    void anEmptyChestPlansNothingAndIsNotAFailure() {
        VaultMigrationPlan.Placement placed = VaultMigrationPlan.plan(Set.of(), Set.of());

        assertTrue(placed.isEmpty());
        assertTrue(placed.skipped().isEmpty());
    }

    /**
     * *** AN OCCUPIED PAGE CELL IS SKIPPED, AND THE STACK STAYS IN THE CHEST. ***
     *
     * <p>Staged with the occupied cell in the MIDDLE of three filled chest slots, so a build that
     * bailed out on the first collision would still place slot 0 and lose slot 26 -- a partial
     * migration, which is the failure that looks most like a success.
     */
    @Test
    void anOccupiedPageCellIsSkippedAndTheRestStillMove() {
        VaultMigrationPlan.Placement placed =
                VaultMigrationPlan.plan(Set.of(13), Set.of(0, 13, 26));

        assertEquals(List.of(13), placed.skipped());
        assertEquals(2, placed.moves().size(), placed.moves().toString());
        assertTrue(placed.moves().containsKey(0));
        assertTrue(placed.moves().containsKey(26));
        assertFalse(placed.moves().containsKey(13));
    }

    /**
     * An occupied cell OUTSIDE the chest's range changes nothing.
     *
     * <p>Page slot 31 is in the fourth row, which no chest slot maps to. A build that tested
     * "is the page empty" rather than "is this cell occupied" would refuse the whole migration.
     */
    @Test
    void anOccupiedCellPastTheChestRangeDoesNotBlockAnything() {
        VaultMigrationPlan.Placement placed =
                VaultMigrationPlan.plan(Set.of(31), Set.of(0, 26));

        assertEquals(2, placed.moves().size());
        assertTrue(placed.skipped().isEmpty());
    }

    /** Skipped slots ascend, so two runs of one migration read the same way. */
    @Test
    void skippedSlotsAreAscending() {
        VaultMigrationPlan.Placement placed =
                VaultMigrationPlan.plan(Set.of(26, 0, 13), Set.of(26, 13, 0));

        assertEquals(List.of(0, 13, 26), placed.skipped());
        assertTrue(placed.isEmpty());
    }

    /**
     * The chest fits inside a page, with room left over.
     *
     * <p><b>Two constants from two different authorities</b> -- vanilla's chest and our page -- and
     * nothing makes them agree. Asserted here rather than left to the defensive throw inside
     * {@code plan}, which no shipped call can reach while this holds.
     */
    @Test
    void aVanillaEnderChestFitsInOneVaultPage() {
        assertEquals(27, VaultMigrationPlan.ENDER_CHEST_SLOTS);
        assertTrue(VaultMigrationPlan.ENDER_CHEST_SLOTS < VaultShape.SLOTS_PER_PAGE,
                "the migration assumes the chest is SMALLER than a page, with the fourth row free");
    }

    /** A chest slot past the chest's own size is ignored rather than mapped onto the page. */
    @Test
    void aFilledSlotPastTheChestSizeIsIgnored() {
        VaultMigrationPlan.Placement placed =
                VaultMigrationPlan.plan(Set.of(), Set.of(0, VaultMigrationPlan.ENDER_CHEST_SLOTS));

        assertEquals(1, placed.moves().size(), placed.moves().toString());
        assertTrue(placed.moves().containsKey(0));
    }
}
