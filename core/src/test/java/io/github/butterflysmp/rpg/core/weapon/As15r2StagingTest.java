package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AS15R2 (GATE-armor-sets.md, 2026-11-26): the two numbers its trace rests on, EXECUTED rather than derived. AS15R missed on
 * staging twice; these are the steps a third miss would hide in.
 */
class As15r2StagingTest {

    /**
     * `rpg playerxp set @s 3 levels` writes {@code PlayerLevel.totalForLevel(3)} (READ: XpGrant), and the recipe screen asks
     * {@code effectiveLevel(lifetimeXp) >= 3} (READ: NexusScreens.refusal, NexusStationGate.unlocked). Level 1, AS15R's, is
     * the control that fails the same gate.
     */
    @Test
    void threeLevelsOpenTheRecipeScreenAndOneDoesNot() {
        assertEquals(3, PlayerLevel.effectiveLevel(PlayerLevel.totalForLevel(3)));
        assertTrue(PlayerLevel.effectiveLevel(PlayerLevel.totalForLevel(3)) >= 3);
        assertTrue(PlayerLevel.effectiveLevel(PlayerLevel.totalForLevel(1)) < 3, "control: AS15R's level 1 is locked out");
    }

    /**
     * The crafted helmet's score is ROLLED from his six slots' average (READ: GearScoreItems.stampOnAcquire). With all six
     * empty (the row clears his armour and holds only ingots) the average is 0, and EVERY draw floors to 100. EXECUTED: one
     * worn piece at 200 does NOT move it (an average of 33 still floors), but four at 200 do, which is why the row clears
     * the four armour slots rather than trusting that he wears little.
     */
    @Test
    void aBarePlayersCraftedPieceScoresOneHundredOnEveryDraw() {
        int average = GearScore.averageOf(GearScore.sixSlots(new int[] {0, 0, 0, 0}, new int[] {}));
        assertEquals(0, average);
        for (int i = 0; i < 1000; i++) {
            assertEquals(100, GearScore.roll(average, GearScoreBand.SPREAD, GearScoreBand.SKEW, i / 1000.0), "draw " + i);
        }
        assertEquals(100, GearScore.roll(GearScore.averageOf(GearScore.sixSlots(new int[] {200, 0, 0, 0}, new int[] {})),
                GearScoreBand.SPREAD, GearScoreBand.SKEW, 0.999), "one worn piece at 200: still 100");
        assertTrue(GearScore.roll(GearScore.averageOf(GearScore.sixSlots(new int[] {200, 200, 200, 200}, new int[] {})),
                GearScoreBand.SPREAD, GearScoreBand.SKEW, 0.999) > 100, "control: four worn pieces at 200 move the roll");
    }
}
