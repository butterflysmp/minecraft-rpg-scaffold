package io.github.butterflysmp.rpg.core.progression;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The level screen's bonus lines, pinned as literals at the rungs where each stat first moves. */
class LevelBonusLinesTest {

    @Test
    void levelOneGrantsNothingAndSaysNothing() {
        assertEquals(List.of(), LevelBonusLines.gainedAt(1));
        assertEquals(List.of(), LevelBonusLines.totals(LevelBonus.at(1)));
    }

    @Test
    void eachLevelUpNamesOnlyWhatMoved() {
        assertEquals(List.of("+5 Max Health", "+1 Weapon Damage"), LevelBonusLines.gainedAt(2));
        assertEquals(List.of("+5 Max Health", "+1 Weapon Damage", "+1 Defense"), LevelBonusLines.gainedAt(5));
        assertEquals(List.of("+5 Max Health", "+1 Weapon Damage", "+1 Health Regen per 5s", "+1 Defense",
                "+1% Crit Chance"), LevelBonusLines.gainedAt(10), "the first level every stat moves");
        assertEquals(List.of("+5 Max Health", "+1 Weapon Damage", "+1 Health Regen per 5s", "+1 Defense"),
                LevelBonusLines.gainedAt(15));
        assertEquals(List.of("+10 Max Health", "+1 Weapon Damage", "+1 Health Regen per 5s", "+1 Defense",
                "+1% Crit Chance"), LevelBonusLines.gainedAt(50), "the 49 -> 50 level-up pays +10");
    }

    @Test
    void theTotalsAtFiftyAreBensTotals() {
        assertEquals(List.of("+250 Max Health", "+49 Weapon Damage", "+9 Health Regen per 5s", "+10 Defense",
                "+5% Crit Chance"), LevelBonusLines.totals(LevelBonus.at(50)));
    }

    @Test
    void aLevelOutsideTheRangeIsRefusedAsLevelBonusRefusesIt() {
        assertThrows(IllegalArgumentException.class, () -> LevelBonusLines.gainedAt(0));
        assertThrows(IllegalArgumentException.class, () -> LevelBonusLines.gainedAt(PlayerLevel.ACTIVE_CAP + 1));
    }
}
