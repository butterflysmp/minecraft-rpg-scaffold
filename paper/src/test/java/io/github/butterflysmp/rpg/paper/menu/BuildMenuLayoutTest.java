package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Build screen's and its picker's slots, pinned as LITERALS (a constant only ever named symbolically
 * has no guard), and each screen's partition of the 54 into painted and filler.
 */
class BuildMenuLayoutTest {

    @Test
    void theSlotsArePinnedToTheirLiterals() {
        assertEquals(54, BuildMenuLayout.SIZE);
        assertEquals(48, BuildMenuLayout.BACK_SLOT, "the house back button");
        assertEquals(49, BuildMenuLayout.CLOSE_SLOT, "the house close button");
        assertEquals(3, BuildMenuLayout.CLASS_SLOT, "row 1: class");
        assertEquals(5, BuildMenuLayout.ELEMENT_SLOT, "row 1: element");
        assertEquals(11, BuildMenuLayout.ULTIMATE_SLOT, "row 2: Q");
        assertEquals(13, BuildMenuLayout.ACTIVE_1_SLOT, "row 2: Left");
        assertEquals(15, BuildMenuLayout.ACTIVE_2_SLOT, "row 2: Right");
        assertEquals(List.of(21, 23), BuildMenuLayout.ASPECT_SLOTS, "row 3: two aspect cells");
        assertEquals(List.of(28, 30, 32, 34), BuildMenuLayout.FRAGMENT_SLOTS, "row 4: four fragment cells");
    }

    /** Ruling 16's bands: row 1 class and element, row 2 the loadout, row 3 aspects, row 4 fragments. */
    @Test
    void eachKindSitsInItsRow() {
        assertEquals(0, BuildMenuLayout.CLASS_SLOT / 9);
        assertEquals(0, BuildMenuLayout.ELEMENT_SLOT / 9);
        for (LoadoutSlot slot : LoadoutSlot.values()) assertEquals(1, BuildMenuLayout.slotOf(slot) / 9, slot.name());
        BuildMenuLayout.ASPECT_SLOTS.forEach(slot -> assertEquals(2, slot / 9, "aspect " + slot));
        BuildMenuLayout.FRAGMENT_SLOTS.forEach(slot -> assertEquals(3, slot / 9, "fragment " + slot));
    }

    /** Q, Left, Right: the order the stone's inputs are named, left to right on screen. */
    @Test
    void theLoadoutRowReadsQLeftRight() {
        assertTrue(BuildMenuLayout.ULTIMATE_SLOT < BuildMenuLayout.ACTIVE_1_SLOT);
        assertTrue(BuildMenuLayout.ACTIVE_1_SLOT < BuildMenuLayout.ACTIVE_2_SLOT);
        assertEquals("Q", BuildMenuLayout.inputLabel(LoadoutSlot.ULTIMATE));
        assertEquals("Left", BuildMenuLayout.inputLabel(LoadoutSlot.ACTIVE_1));
        assertEquals("Right", BuildMenuLayout.inputLabel(LoadoutSlot.ACTIVE_2));
    }

    @Test
    void screenSlotAndLoadoutSlotRoundTrip() {
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            assertEquals(Optional.of(slot), BuildMenuLayout.loadoutSlotAt(BuildMenuLayout.slotOf(slot)));
        }
        assertEquals(Optional.empty(), BuildMenuLayout.loadoutSlotAt(BuildMenuLayout.CLASS_SLOT));
        assertEquals(Optional.empty(), BuildMenuLayout.loadoutSlotAt(BuildMenuLayout.ASPECT_SLOTS.get(0)));
    }

    /** Slice 4: the fragment row maps screen slot to fragment slot 0-3, one per stored slot. */
    @Test
    void theFragmentRowMapsToTheFourStoredSlots() {
        assertEquals(io.github.butterflysmp.rpg.core.build.FragmentSlots.COUNT, BuildMenuLayout.FRAGMENT_SLOTS.size());
        assertEquals(io.github.butterflysmp.rpg.storage.CellLoadout.FRAGMENTS,
                io.github.butterflysmp.rpg.core.build.FragmentSlots.COUNT,
                "the stored slots and the core slots are the same four");
        for (int i = 0; i < BuildMenuLayout.FRAGMENT_SLOTS.size(); i++) {
            assertEquals(OptionalInt.of(i), BuildMenuLayout.fragmentIndexAt(BuildMenuLayout.FRAGMENT_SLOTS.get(i)));
        }
        assertEquals(OptionalInt.empty(), BuildMenuLayout.fragmentIndexAt(BuildMenuLayout.ASPECT_SLOTS.get(0)));
    }

    @Test
    void theBuildScreenIsPaintedOrFillerExactlyOnce() {
        for (int slot = 0; slot < BuildMenuLayout.SIZE; slot++) {
            boolean filler = BuildMenuLayout.FILLER_SLOTS.contains(slot);
            boolean painted = BuildMenuLayout.PAINTED_SLOTS.contains(slot);
            assertTrue(filler ^ painted, "slot " + slot + " must be exactly one of filler or painted");
        }
        assertEquals(13, BuildMenuLayout.PAINTED_SLOTS.size(),
                "class, element, three loadout cells, two aspects, four fragments, back, close");
    }

    @Test
    void thePickerHas28OptionsAndItsChromeIsDisjoint() {
        assertEquals(28, BuildMenuLayout.OPTION_SLOTS.size());
        assertEquals(28, new HashSet<>(BuildMenuLayout.OPTION_SLOTS).size(), "no option cell listed twice");
        assertEquals(4, BuildMenuLayout.PICKER_TITLE_SLOT);
        Set<Integer> chrome = Set.of(BuildMenuLayout.PICKER_TITLE_SLOT, BuildMenuLayout.BACK_SLOT,
                BuildMenuLayout.CLOSE_SLOT);
        for (int slot = 0; slot < BuildMenuLayout.SIZE; slot++) {
            int roles = (BuildMenuLayout.OPTION_SLOTS.contains(slot) ? 1 : 0)
                    + (chrome.contains(slot) ? 1 : 0)
                    + (BuildMenuLayout.PICKER_FILLER_SLOTS.contains(slot) ? 1 : 0);
            assertEquals(1, roles, "picker slot " + slot + " must have exactly one role");
        }
        assertEquals(OptionalInt.of(0), BuildMenuLayout.optionAt(10));
        assertEquals(OptionalInt.of(7), BuildMenuLayout.optionAt(19), "row 3 starts at option 7");
        assertEquals(OptionalInt.of(27), BuildMenuLayout.optionAt(43));
        assertEquals(OptionalInt.empty(), BuildMenuLayout.optionAt(9), "column 0 is filler");
        assertEquals(OptionalInt.empty(), BuildMenuLayout.optionAt(BuildMenuLayout.BACK_SLOT));
    }
}
