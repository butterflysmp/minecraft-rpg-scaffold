package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.vault.VaultShape;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the vault screen's cells are, and the two conversions that have an off-by-one in them. */
class NexusVaultLayoutTest {

    /**
     * *** SEVEN PAGES, SEVEN BUTTONS -- THE EQUALITY {@code VaultShape} ASKED FOR. ***
     *
     * <p>{@code VaultShape}'s javadoc names what happens without it: an eighth page would exist in
     * storage, hold the player's items, and have <b>no way to be selected</b>. Not a compile error
     * and not an exception -- an unreachable page, indistinguishable from an empty one.
     */
    @Test
    void thereIsExactlyOneButtonPerPage() {
        assertEquals(VaultShape.PAGE_COUNT, NexusVaultLayout.PAGE_BUTTON_SLOTS.length);
    }

    /** The buttons sit BETWEEN the arrows, contiguously, and the row is 0-8. */
    @Test
    void theButtonsSitBetweenTheArrowsAndFillTheRow() {
        assertEquals(0, NexusVaultLayout.PREV_SLOT);
        assertEquals(8, NexusVaultLayout.NEXT_SLOT);

        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            int slot = NexusVaultLayout.PAGE_BUTTON_SLOTS[page];
            assertTrue(slot > NexusVaultLayout.PREV_SLOT && slot < NexusVaultLayout.NEXT_SLOT,
                    "page " + page + "'s button at " + slot + " is outside the arrows");
            assertEquals(NexusVaultLayout.FIRST_PAGE_SLOT + page, slot, "contiguous, in page order");
        }
    }

    /** A page's worth of storage cells, derived from {@code SLOTS_PER_PAGE} rather than written out. */
    @Test
    void thereAreExactlyAPagesWorthOfStorageCells() {
        assertEquals(VaultShape.SLOTS_PER_PAGE, NexusVaultLayout.STORAGE_SLOTS.size());
        assertTrue(NexusVaultLayout.STORAGE_SLOTS.contains(NexusVaultLayout.FIRST_STORAGE_SLOT));
        assertTrue(NexusVaultLayout.STORAGE_SLOTS.contains(
                        NexusVaultLayout.FIRST_STORAGE_SLOT + VaultShape.SLOTS_PER_PAGE - 1),
                "including the LAST one, which an off-by-one in the loop bound drops");
    }

    /**
     * *** STORAGE DOES NOT OVERLAP THE SELECTOR OR THE CHROME. ***
     *
     * <p>The failure this prevents is not cosmetic: a storage cell that is also a button would be
     * painted with an icon by {@code renderSelector} and then ENCODED as the player's item by the
     * next write -- a page that stores glass panes, and eats whatever was there.
     */
    @Test
    void storageAndPaintedCellsDoNotOverlap() {
        Set<Integer> both = new HashSet<>(NexusVaultLayout.STORAGE_SLOTS);
        both.retainAll(NexusVaultLayout.PAINTED_SLOTS);
        assertTrue(both.isEmpty(), "a cell cannot be both storage and chrome: " + both);
    }

    /**
     * The three sets partition the screen exactly once.
     *
     * <p>{@code NexusMenuLayout} shipped an invisible clickable hole at slot 33 because a cell was
     * subtracted from the filler and painted by nothing. This is that invariant, with storage as a
     * third region.
     */
    @Test
    void fillerStorageAndPaintedCoverTheScreenExactlyOnce() {
        for (int slot = 0; slot < NexusVaultLayout.SIZE; slot++) {
            int regions = 0;
            if (NexusVaultLayout.FILLER_SLOTS.contains(slot)) regions++;
            if (NexusVaultLayout.STORAGE_SLOTS.contains(slot)) regions++;
            if (NexusVaultLayout.PAINTED_SLOTS.contains(slot)) regions++;
            assertEquals(1, regions,
                    "slot " + slot + " is in " + regions + " regions; it must be in exactly one");
        }

        assertEquals(NexusVaultLayout.SIZE,
                NexusVaultLayout.FILLER_SLOTS.size() + NexusVaultLayout.STORAGE_SLOTS.size()
                        + NexusVaultLayout.PAINTED_SLOTS.size());

        // AND NO REGION IS EMPTY, without which the partition is satisfied by "everything is filler".
        assertFalse(NexusVaultLayout.PAINTED_SLOTS.isEmpty());
        assertFalse(NexusVaultLayout.STORAGE_SLOTS.isEmpty());
        assertFalse(NexusVaultLayout.FILLER_SLOTS.isEmpty());
    }

    /** Back at 48 and Close at 49, the cells every other screen in this plugin uses. */
    @Test
    void backAndCloseMatchEveryOtherScreen() {
        assertEquals(48, NexusVaultLayout.BACK_SLOT);
        assertEquals(49, NexusVaultLayout.CLOSE_SLOT);
        assertEquals(CraftingMenuLayout.BACK_SLOT, NexusVaultLayout.BACK_SLOT,
                "read from the screen that established it rather than agreeing by coincidence");
        assertEquals(NexusMenuLayout.CLOSE_SLOT, NexusVaultLayout.CLOSE_SLOT);
    }

    /**
     * {@code pageAt} resolves every button and nothing else.
     *
     * <p>The {@code -1} arm matters: a storage cell that resolved to a page would turn a click on
     * the player's own item into a page flip, which repaints the cells it was clicked on.
     */
    @Test
    void pageAtResolvesTheButtonsAndOnlyTheButtons() {
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            assertEquals(page, NexusVaultLayout.pageAt(NexusVaultLayout.PAGE_BUTTON_SLOTS[page]));
        }
        assertEquals(-1, NexusVaultLayout.pageAt(NexusVaultLayout.PREV_SLOT));
        assertEquals(-1, NexusVaultLayout.pageAt(NexusVaultLayout.NEXT_SLOT));
        assertEquals(-1, NexusVaultLayout.pageAt(NexusVaultLayout.CLOSE_SLOT));
        assertEquals(-1, NexusVaultLayout.pageAt(NexusVaultLayout.FIRST_STORAGE_SLOT));
        assertEquals(-1, NexusVaultLayout.pageAt(-1));
    }

    /**
     * *** THE SCREEN-CELL TO VAULT-SLOT CONVERSION, IN BOTH DIRECTIONS. ***
     *
     * <p>Storage cell 9 is vault slot 0, and this is the one place that arithmetic happens. Done at
     * each call site instead, it is how a screen comes to write row 2 into row 3.
     *
     * <p>Checked as a ROUND TRIP over every slot rather than at one point: an off-by-one that
     * shifted both directions equally would survive a single-point check in either direction alone.
     */
    @Test
    void theStorageConversionRoundTripsForEverySlot() {
        assertEquals(0, NexusVaultLayout.storageSlotAt(NexusVaultLayout.FIRST_STORAGE_SLOT));
        assertEquals(NexusVaultLayout.FIRST_STORAGE_SLOT, NexusVaultLayout.screenSlotFor(0));

        for (int vaultSlot = 0; vaultSlot < VaultShape.SLOTS_PER_PAGE; vaultSlot++) {
            int screenSlot = NexusVaultLayout.screenSlotFor(vaultSlot);
            assertTrue(NexusVaultLayout.STORAGE_SLOTS.contains(screenSlot),
                    "vault slot " + vaultSlot + " maps to " + screenSlot + ", which is not storage");
            assertEquals(vaultSlot, NexusVaultLayout.storageSlotAt(screenSlot));
        }
    }

    /** A non-storage cell has no vault slot, and says so rather than returning 0. */
    @Test
    void aNonStorageCellHasNoVaultSlot() {
        assertEquals(-1, NexusVaultLayout.storageSlotAt(NexusVaultLayout.CLOSE_SLOT));
        assertEquals(-1, NexusVaultLayout.storageSlotAt(NexusVaultLayout.PREV_SLOT));
        assertEquals(-1, NexusVaultLayout.storageSlotAt(NexusVaultLayout.BACK_SLOT));
    }

    /** A vault slot outside a page is a programming error, refused rather than mapped. */
    @Test
    void aSlotPastThePageIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> NexusVaultLayout.screenSlotFor(VaultShape.SLOTS_PER_PAGE));
        assertThrows(IllegalArgumentException.class, () -> NexusVaultLayout.screenSlotFor(-1));
    }
}
