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
        assertEquals(List.of("+5 Max Health", "+1 Weapon Damage", "+1 Health Regen/5s", "+1 Defense",
                "+1% Crit Chance"), LevelBonusLines.gainedAt(10), "the first level every stat moves");
        assertEquals(List.of("+5 Max Health", "+1 Weapon Damage", "+1 Health Regen/5s", "+1 Defense"),
                LevelBonusLines.gainedAt(15));
        assertEquals(List.of("+10 Max Health", "+1 Weapon Damage", "+1 Health Regen/5s", "+1 Defense",
                "+1% Crit Chance"), LevelBonusLines.gainedAt(50), "the 49 -> 50 level-up pays +10");
    }

    @Test
    void theTotalsAtFiftyAreBensTotals() {
        assertEquals(List.of("+250 Max Health", "+49 Weapon Damage", "+9 Health Regen/5s", "+10 Defense",
                "+5% Crit Chance"), LevelBonusLines.totals(LevelBonus.at(50)));
    }

    /**
     * U1 (the seat, 2026-11-27): the Level menu's regen line is in the stats sheet's unit with the sheet's suffix,
     * through the sheet's formatter. Swept over EVERY level, gained and total: each regen line is the sheet's amount for
     * that level's per-second figure, and every one of them is whole (the ladder moves in steps of 0.2 per second).
     */
    @Test
    void everyRegenLineIsTheSheetsAmount_atEveryLevel() {
        int seen = 0;
        for (int level = 1; level <= PlayerLevel.ACTIVE_CAP; level++) {
            LevelBonus now = LevelBonus.at(level);
            for (String line : LevelBonusLines.totals(now)) {
                if (!line.contains("Health Regen")) continue;
                seen++;
                assertEquals("+" + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSecondsBonus(
                        now.healthRegenPerSecond()).replace("/5s", " Health Regen/5s"), line, "level " + level);
                org.junit.jupiter.api.Assertions.assertTrue(line.matches("\\+\\d+ Health Regen/5s"), line);
            }
            for (String line : LevelBonusLines.gainedAt(level)) {
                if (line.contains("Health Regen")) org.junit.jupiter.api.Assertions.assertEquals(
                        "+1 Health Regen/5s", line, "every regen step is one per five seconds; level " + level);
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(seen > 0, "the sweep found no regen line at all");
    }

    @Test
    void aLevelOutsideTheRangeIsRefusedAsLevelBonusRefusesIt() {
        assertThrows(IllegalArgumentException.class, () -> LevelBonusLines.gainedAt(0));
        assertThrows(IllegalArgumentException.class, () -> LevelBonusLines.gainedAt(PlayerLevel.ACTIVE_CAP + 1));
    }
}
