package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vault record's invariants.
 *
 * <h2>NO TWO QUANTITIES IN A FIXTURE ARE EQUAL, AND THAT IS DELIBERATE</h2>
 *
 * Pages are staged at 2, 3 and 5; slots at 17, 24 and 31; counts come out at 1, 2 and 3. None of
 * them is 7 (the page count), 36 (the slots per page), or equal to another quantity the same row
 * reads. A row staged on {@code page == slot} cannot detect a transposition between them, and a row
 * staged on a boundary passes whether the bound is checked correctly or not.
 */
class PlayerVaultTest {

    private static final UUID PLAYER = UUID.fromString("0b6f2c41-9d3e-4a7b-8c15-2e6a9f704d38");
    private static final UUID OTHER = UUID.fromString("7a1e5d92-3c48-4f60-b2d7-91c3e8054a6f");

    private static PlayerVault vault(VaultEntry... entries) {
        return new PlayerVault(PlayerVault.CURRENT_SCHEMA_VERSION, PLAYER, List.of(entries));
    }

    @Test
    void anEmptyVaultHoldsNothingAndIsStampedCurrent() {
        PlayerVault empty = PlayerVault.empty(PLAYER);
        assertEquals(0, empty.occupiedSlots());
        assertEquals(PlayerVault.CURRENT_SCHEMA_VERSION, empty.schemaVersion());
        assertEquals(PLAYER, empty.playerId());
        assertTrue(empty.page(3).isEmpty(), "every page of an empty vault is empty");
    }

    /** Gson leaves an absent array null, and the compact constructor is where that is read. */
    @Test
    void aNullEntryListDeserialisesToAnEmptyVaultRatherThanThrowing() {
        PlayerVault fromAbsentField = new PlayerVault(1, PLAYER, null);
        assertEquals(0, fromAbsentField.occupiedSlots());
    }

    @Test
    void entriesAreSortedByPageThenSlotWhateverOrderTheyArriveIn() {
        PlayerVault sorted = vault(
                new VaultEntry(5, 24, "e"),
                new VaultEntry(2, 31, "b"),
                new VaultEntry(5, 17, "d"),
                new VaultEntry(2, 17, "a"),
                new VaultEntry(3, 24, "c"));

        assertEquals(List.of("a", "b", "c", "d", "e"),
                sorted.entries().stream().map(VaultEntry::item).toList(),
                "canonical order makes two saves of the same contents byte-identical");
    }

    /**
     * *** THE PAIR IS THE IDENTITY, NOT THE SLOT. *** Slot 17 exists on all seven pages. A duplicate
     * check keyed on the slot alone would refuse this perfectly legal vault, and the failure would
     * look like corruption to whoever hit it.
     */
    @Test
    void theSameSlotOnDifferentPagesIsNotADuplicate() {
        PlayerVault legal = vault(
                new VaultEntry(2, 17, "one"),
                new VaultEntry(3, 17, "two"),
                new VaultEntry(5, 17, "three"));
        assertEquals(3, legal.occupiedSlots());
    }

    @Test
    void twoEntriesForOneCellAreRefusedAndTheMessageNamesTheCell() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> vault(new VaultEntry(3, 24, "first"), new VaultEntry(3, 24, "second")));

        assertTrue(thrown.getMessage().contains("page 3"), "name the page");
        assertTrue(thrown.getMessage().contains("slot 24"), "and the slot, so the file can be read");
        assertTrue(thrown.getMessage().contains("refusing"),
                "and say it refused rather than picked a winner -- last-write-wins would silently"
                        + " delete one of two items claiming one cell");
    }

    @Test
    void pageReturnsOnlyThatPageKeyedBySlot() {
        PlayerVault mixed = vault(
                new VaultEntry(2, 17, "on two"),
                new VaultEntry(5, 24, "on five"),
                new VaultEntry(5, 31, "also on five"));

        Map<Integer, String> five = mixed.page(5);
        assertEquals(2, five.size());
        assertEquals("on five", five.get(24));
        assertEquals("also on five", five.get(31));
        assertNull(five.get(17), "an empty slot is ABSENT from the map, not mapped to null");
    }

    @Test
    void pageIsUnmodifiableSoNobodyCanEditTheVaultThroughAReadAccessor() {
        Map<Integer, String> page = vault(new VaultEntry(2, 17, "x")).page(2);
        assertThrows(UnsupportedOperationException.class, () -> page.put(24, "sneaked in"));
    }

    /**
     * The accessor's javadoc claims slot order, so it is asserted. {@code Map.copyOf} would NOT have
     * provided it -- its iteration order is unspecified -- and the claim would have been false while
     * every other row here still passed.
     */
    @Test
    void pageIteratesInSlotOrder() {
        PlayerVault unordered = vault(
                new VaultEntry(3, 31, "last"),
                new VaultEntry(3, 17, "first"),
                new VaultEntry(3, 24, "middle"));

        assertEquals(List.of(17, 24, 31), List.copyOf(unordered.page(3).keySet()));
    }

    @Test
    void withPageReplacesThatPageWholesaleAndLeavesTheOthersAlone() {
        PlayerVault before = vault(
                new VaultEntry(2, 17, "keep me"),
                new VaultEntry(3, 17, "replace me"),
                new VaultEntry(3, 24, "and me"));

        Map<Integer, String> replacement = new LinkedHashMap<>();
        replacement.put(31, "the only thing on page three now");

        PlayerVault after = before.withPage(3, replacement);

        assertEquals(1, after.page(2).size(), "page two is untouched");
        assertEquals("keep me", after.page(2).get(17));
        assertEquals(1, after.page(3).size(), "page three was replaced, not merged");
        assertEquals("the only thing on page three now", after.page(3).get(31));
        assertEquals(2, after.occupiedSlots());
    }

    /** Writing an emptied page must EMPTY it. A merge here would make an item impossible to remove. */
    @Test
    void withPageAnEmptyMapClearsThePage() {
        PlayerVault cleared = vault(new VaultEntry(3, 17, "gone"), new VaultEntry(5, 24, "stays"))
                .withPage(3, Map.of());
        assertTrue(cleared.page(3).isEmpty());
        assertEquals(1, cleared.occupiedSlots());
    }

    /**
     * The caller hands over a snapshot of thirty-six inventory cells, most of them empty. Empty cells
     * are the normal case, so they are DROPPED rather than rejected -- a throw here would mean every
     * caller had to filter first, and the one that forgot would lose a page.
     */
    @Test
    void withPageDropsEmptyCellsRatherThanRejectingThem() {
        Map<Integer, String> withHoles = new LinkedHashMap<>();
        withHoles.put(17, "real");
        withHoles.put(24, null);
        withHoles.put(31, "   ");

        PlayerVault written = PlayerVault.empty(PLAYER).withPage(5, withHoles);
        assertEquals(1, written.occupiedSlots());
        assertEquals("real", written.page(5).get(17));
    }

    @Test
    void withPageValidatesTheSlotsItIsGiven() {
        assertThrows(IllegalArgumentException.class,
                () -> PlayerVault.empty(PLAYER).withPage(2, Map.of(36, "one past the end")));
    }

    @Test
    void anEntryOutsideTheVaultsShapeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new VaultEntry(7, 17, "no such page"));
        assertThrows(IllegalArgumentException.class, () -> new VaultEntry(2, 36, "no such slot"));
        assertThrows(IllegalArgumentException.class, () -> new VaultEntry(-1, 17, "negative page"));
    }

    /**
     * An empty slot is the ABSENCE of an entry. A blank one would be a third state meaning the same
     * thing, and two representations of one fact drift.
     */
    @Test
    void aBlankItemIsRefusedBecauseAbsenceAlreadyMeansEmpty() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new VaultEntry(3, 24, "  "));
        assertTrue(thrown.getMessage().contains("NO entry"),
                "the message must say what the right representation IS, not only that this is wrong");
    }

    @Test
    void withSchemaVersionCarriesTheOwnerAndTheContents() {
        PlayerVault stamped = vault(new VaultEntry(2, 17, "x")).withSchemaVersion(4);
        assertEquals(4, stamped.schemaVersion());
        assertEquals(PLAYER, stamped.playerId());
        assertEquals(1, stamped.occupiedSlots());
    }

    @Test
    void aVaultKnowsWhoseItIs() {
        assertEquals(OTHER, PlayerVault.empty(OTHER).playerId());
    }
}
