package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the level screen lists as each level's unlocks. The expected levels are LITERALS -- Ben's numbers
 * as ruled -- so a retune of a gate reddens this row and the screen's claim is re-read, rather than the
 * test quietly following the gate it is meant to check.
 */
class LevelUnlocksTest {

    @Test
    void theFiveStationsUnlockAtTheirRuledLevels() {
        assertEquals(List.of("Unlocks the Crafting station"), LevelUnlocks.at(3));
        assertEquals(List.of("Unlocks the Anvil station"), LevelUnlocks.at(7));
        assertEquals(List.of("Unlocks the Enchanting station"), LevelUnlocks.at(10));
        assertEquals(List.of("Unlocks the Grindstone station"), LevelUnlocks.at(13));
        assertEquals(List.of("Unlocks the Vault station"), LevelUnlocks.at(20), "the hub shortcut");
    }

    @Test
    void vaultPagesTwoToSevenUnlockEveryFiveFromTwentyFive() {
        assertEquals(List.of("Unlocks Vault page 2"), LevelUnlocks.at(25));
        assertEquals(List.of("Unlocks Vault page 3"), LevelUnlocks.at(30));
        assertEquals(List.of("Unlocks Vault page 7"), LevelUnlocks.at(50), "the last page, exactly at the cap");
    }

    /** Page 1 is free, so no level claims it -- and most levels unlock nothing at all. */
    @Test
    void noLevelClaimsTheFreePageAndMostLevelsUnlockNothing() {
        assertEquals(List.of(), LevelUnlocks.at(1));
        assertEquals(List.of(), LevelUnlocks.at(2));
        assertEquals(List.of(), LevelUnlocks.at(49));
        int levelsWithAnUnlock = 0;
        for (int level = 1; level <= PlayerLevel.ACTIVE_CAP; level++) {
            if (!LevelUnlocks.at(level).isEmpty()) levelsWithAnUnlock++;
        }
        assertEquals(11, levelsWithAnUnlock, "five stations and six vault pages, none sharing a level");
    }
}
