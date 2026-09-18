package io.github.butterflysmp.rpg.core.vault;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What is not in the file -- the one comparison the screen, the service and the command share. */
class VaultPageDiffTest {

    @Test
    void nothingIsOwedWhenTheFileHasEverythingExactly() {
        Map<Integer, String> both = Map.of(17, "a", 31, "b");

        assertEquals(Set.of(), VaultPageDiff.unpersisted(both, both));
    }

    @Test
    void everythingIsOwedWhenTheFileHasNothing() {
        assertEquals(Set.of(17, 31),
                VaultPageDiff.unpersisted(Map.of(17, "a", 31, "b"), Map.of()));
    }

    /** The common shape of a failed write: one cell changed, thirty-five untouched. */
    @Test
    void onlyTheChangedCellIsOwed() {
        assertEquals(Set.of(24),
                VaultPageDiff.unpersisted(Map.of(17, "a", 24, "new", 31, "b"),
                        Map.of(17, "a", 31, "b")));
    }

    /**
     * *** AN UNPERSISTED SWAP IS OWED, AND PRESENCE ALONE WOULD DESTROY IT. ***
     *
     * <p>The file holds X at cell 17; the screen holds Y there. A comparison that asked only "does
     * the file have this slot" would call the cell persisted, hand back nothing, and leave Y in no
     * file and nobody's hands -- while the player keeps an X they never took.
     */
    @Test
    void aCellWhoseCONTENTSDifferIsOwedEvenThoughTheSlotIsPresent() {
        assertEquals(Set.of(17),
                VaultPageDiff.unpersisted(Map.of(17, "Y"), Map.of(17, "X")));
    }

    /**
     * An empty cell is never owed, however the file disagrees.
     *
     * <p>A cell the player emptied is a cell there is nothing to hand back FROM. The file still
     * holding the old item is the accepted take-out residual, not something a drop can fix -- and
     * dropping "nothing" would be a null into {@code dropItemNaturally}.
     */
    @Test
    void emptyAndBlankCellsAreNeverOwed() {
        Map<Integer, String> live = new HashMap<>();
        live.put(17, null);
        live.put(24, "");
        live.put(31, "   ");

        assertTrue(VaultPageDiff.unpersisted(live, Map.of(17, "was here")).isEmpty());
    }

    /** A file holding cells the screen does not have changes nothing -- they are not ours to hand back. */
    @Test
    void cellsOnlyInTheFileAreIgnored() {
        assertEquals(Set.of(), VaultPageDiff.unpersisted(Map.of(), Map.of(17, "a", 31, "b")));
    }

    /** The answer is immutable, because three callers share it and one of them iterates while dropping. */
    @Test
    void theAnswerIsImmutable() {
        Set<Integer> owed = VaultPageDiff.unpersisted(Map.of(17, "a"), Map.of());

        assertEquals(Set.of(17), owed);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> owed.add(99));
    }
}
