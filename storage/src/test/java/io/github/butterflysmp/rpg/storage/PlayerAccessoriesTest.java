package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The accessories record: its shape, and the structural faults that refuse a whole file. */
class PlayerAccessoriesTest {

    private static final UUID PLAYER = UUID.fromString("0d1f2e3c-4b5a-6978-8a9b-acbdcedf0f1e");

    @Test
    void anEmptySetHasNoEntries_andEverySlotReadsEmpty() {
        PlayerAccessories empty = PlayerAccessories.empty(PLAYER);
        assertEquals(PlayerAccessories.CURRENT_SCHEMA_VERSION, empty.schemaVersion());
        for (int slot = 0; slot < 4; slot++) {
            assertEquals(Optional.empty(), empty.item(slot));
        }
    }

    @Test
    void withSlotSetsReplacesAndClears_touchingNoOtherSlot() {
        PlayerAccessories a = PlayerAccessories.empty(PLAYER).withSlot(2, "blob-a").withSlot(0, "blob-b");
        assertEquals(Optional.of("blob-a"), a.item(2));
        assertEquals(Optional.of("blob-b"), a.item(0));
        assertEquals(List.of(0, 2), a.entries().stream().map(AccessoryEntry::slot).toList(), "sorted by slot");

        PlayerAccessories replaced = a.withSlot(2, "blob-c");
        assertEquals(Optional.of("blob-c"), replaced.item(2));
        assertEquals(Optional.of("blob-b"), replaced.item(0));

        PlayerAccessories cleared = replaced.withSlot(2, null);
        assertEquals(Optional.empty(), cleared.item(2));
        assertEquals(Optional.of("blob-b"), cleared.item(0));
        assertEquals(Optional.of("blob-a"), a.item(2), "records are values: the original is unchanged");
    }

    @Test
    void twoEntriesForOneSlotRefuseTheWholeRecord() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new PlayerAccessories(1, PLAYER,
                        List.of(new AccessoryEntry(1, "a"), new AccessoryEntry(1, "b"))));
        assertTrue(thrown.getMessage().contains("two entries for slot 1"), thrown.getMessage());
    }

    @Test
    void aSlotOutOfRangeOrABlankItemIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new AccessoryEntry(4, "a"));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryEntry(-1, "a"));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryEntry(0, " "));
        assertThrows(NullPointerException.class, () -> new AccessoryEntry(0, null));
        assertThrows(IllegalArgumentException.class, () -> PlayerAccessories.empty(PLAYER).withSlot(4, "a"));
    }

    @Test
    void migrationStampsAVersionZeroFile_andRefusesANewerOne() {
        PlayerAccessories v0 = new PlayerAccessories(0, PLAYER, List.of(new AccessoryEntry(3, "x")));
        PlayerAccessories migrated = AccessoryMigrations.migrate(v0);
        assertEquals(1, migrated.schemaVersion());
        assertEquals(Optional.of("x"), migrated.item(3), "the stamp touches nothing else");

        IllegalStateException newer = assertThrows(IllegalStateException.class,
                () -> AccessoryMigrations.migrate(new PlayerAccessories(2, PLAYER, List.of())));
        assertTrue(newer.getMessage().contains("Refusing"), newer.getMessage());
    }
}
