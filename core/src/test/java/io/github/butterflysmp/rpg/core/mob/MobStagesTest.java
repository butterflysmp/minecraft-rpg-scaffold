package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phage's five stage bodies (PLAN-dragon-mobs.md, RULINGS: Ben's answer 6 and G3). Content gives stage 1 and
 * stage 5; every stage between is LINEAR. The whole table is asserted, cell by cell, because an interpolation error
 * lands in one cell and a spot check of the two ends cannot see it.
 */
class MobStagesTest {

    /** Ben's numbers: 1 to 3 blocks tall, 1.5x to 0.5x speed, 80 to 160 health. */
    private static MobStages phage() {
        return new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160));
    }

    @Test
    void theWholeTableIsLinearBetweenTheTwoAuthoredEnds() {
        double[] height = {1.0, 1.5, 2.0, 2.5, 3.0};
        double[] speed = {1.5, 1.25, 1.0, 0.75, 0.5};
        double[] health = {80, 100, 120, 140, 160};
        MobStages stages = phage();
        for (int stage = 1; stage <= 5; stage++) {
            StageBody body = stages.at(stage);
            // Exact, not within a tolerance: every value here is a short binary fraction, and the test is the
            // measurement of whether the arithmetic produced it (it was executed, not predicted).
            assertEquals(height[stage - 1], body.height(), 0.0, "height at stage " + stage);
            assertEquals(speed[stage - 1], body.speedMultiplier(), 0.0, "speed at stage " + stage);
            assertEquals(health[stage - 1], body.maxHealth(), 0.0, "health at stage " + stage);
        }
    }

    @Test
    void stageZeroAndStageSixAreRefused() {
        MobStages stages = phage();
        assertThrows(IllegalArgumentException.class, () -> stages.at(0));
        assertThrows(IllegalArgumentException.class, () -> stages.at(6));
        assertFalse(stages.isValid(0));
        assertFalse(stages.isValid(6));
        assertTrue(stages.isValid(1));
        assertTrue(stages.isValid(5));
    }

    /** One stage has nothing to interpolate between, and a body of 0 or less is not a body. */
    @Test
    void aStageCountBelowTwoAndANonPositiveBodyAreRefused() {
        StageBody ok = new StageBody(1.0, 1.0, 10);
        assertThrows(IllegalArgumentException.class, () -> new MobStages(1, ok, ok));
        assertThrows(IllegalArgumentException.class, () -> new StageBody(0.0, 1.0, 10));
        assertThrows(IllegalArgumentException.class, () -> new StageBody(1.0, 0.0, 10));
        assertThrows(IllegalArgumentException.class, () -> new StageBody(1.0, 1.0, 0));
    }
}
