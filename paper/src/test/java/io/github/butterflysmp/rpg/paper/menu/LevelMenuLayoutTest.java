package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The level screen's slot map and page arithmetic, pinned as literals. */
class LevelMenuLayoutTest {

    @Test
    void theLevelBlockIsTheInnerSevenByFourInReadingOrder() {
        assertEquals(28, LevelMenuLayout.PAGE_SIZE);
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16), LevelMenuLayout.LEVEL_SLOTS.subList(0, 7));
        assertEquals(37, LevelMenuLayout.LEVEL_SLOTS.get(21), "row 4 starts at 37");
        assertEquals(43, LevelMenuLayout.LEVEL_SLOTS.get(27), "and ends at 43");
    }

    @Test
    void everySlotHasExactlyOneRole() {
        Set<Integer> seen = new HashSet<>();
        for (int slot : LevelMenuLayout.LEVEL_SLOTS) assertTrue(seen.add(slot), "level slot " + slot);
        for (int slot : List.of(LevelMenuLayout.SUMMARY_SLOT, LevelMenuLayout.PREV_SLOT, LevelMenuLayout.BACK_SLOT,
                LevelMenuLayout.CLOSE_SLOT, LevelMenuLayout.NEXT_SLOT)) {
            assertTrue(seen.add(slot), "button " + slot);
        }
        for (int slot : LevelMenuLayout.FILLER_SLOTS) assertTrue(seen.add(slot), "filler " + slot);
        assertEquals(LevelMenuLayout.SIZE, seen.size(), "and every one of the 54 is something");
    }

    @Test
    void backAndCloseSitWhereTheOtherNexusScreensPutThem() {
        assertEquals(SettingsMenuLayout.BACK_SLOT, LevelMenuLayout.BACK_SLOT);
        assertEquals(SettingsMenuLayout.CLOSE_SLOT, LevelMenuLayout.CLOSE_SLOT);
    }

    /** Levels 1-28 on page 1, 29-50 on page 2, and the six cells after 50 are empty. */
    @Test
    void fiftyLevelsFillTwoPagesAndNothingPastTheCapIsShown() {
        assertEquals(2, LevelMenuLayout.PAGE_COUNT);
        assertEquals(1, LevelMenuLayout.levelAt(0, 0));
        assertEquals(28, LevelMenuLayout.levelAt(0, 27));
        assertEquals(29, LevelMenuLayout.levelAt(1, 0));
        assertEquals(50, LevelMenuLayout.levelAt(1, 21));
        assertEquals(0, LevelMenuLayout.levelAt(1, 22), "51 is past the active cap");
        assertEquals(0, LevelMenuLayout.levelAt(1, 27));
    }

    @Test
    void theScreenOpensOnThePageThePlayersLevelIsOn() {
        assertEquals(0, LevelMenuLayout.pageOf(1));
        assertEquals(0, LevelMenuLayout.pageOf(28));
        assertEquals(1, LevelMenuLayout.pageOf(29));
        assertEquals(1, LevelMenuLayout.pageOf(50));
        assertFalse(LevelMenuLayout.pageOf(99) >= LevelMenuLayout.PAGE_COUNT, "a curve level past the cap clamps");
    }
}
