package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * THE PHAGE'S DISTANCES BY STAGE (the seat's ruling on #204: Part C item 3). Ben: "make the size of the Phage affect the
 * Fuse radius, explosion radius, etc. The Large phage being the current distances and scaling everything down to the
 * small phage." The file authors the LAST stage's distance; each earlier stage's is that times its HEIGHT over the
 * last stage's. The Phage's heights are 1, 1.5, 2, 2.5, 3 (phage.yml), so the factors are 1/3, 1/2, 2/3, 5/6, 1.
 * Every expected value below is that product, executed by the test's own arithmetic, never typed as a rounded figure.
 */
class StagedDistanceTest {

    private static final MobStages PHAGE =
            new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160));

    @Test
    void theTriggerScalesFromOneToThreeByHeight() {
        StagedDistance trigger = StagedDistance.byHeight(3);
        double[] want = {1.0, 1.5, 2.0, 2.5, 3.0};
        for (int s = 1; s <= 5; s++) assertEquals(want[s - 1], trigger.at(s, PHAGE), 1e-12, "stage " + s);
    }

    @Test
    void theCancelScalesFromSevenThirdsToSevenByHeight() {
        StagedDistance cancel = StagedDistance.byHeight(7);
        double[] want = {7.0 / 3, 3.5, 14.0 / 3, 35.0 / 6, 7.0};
        for (int s = 1; s <= 5; s++) assertEquals(want[s - 1], cancel.at(s, PHAGE), 1e-12, "stage " + s);
    }

    @Test
    void theReachScalesFromTwoToSixByHeight() {
        StagedDistance reach = StagedDistance.byHeight(6);
        double[] want = {2.0, 3.0, 4.0, 5.0, 6.0};
        for (int s = 1; s <= 5; s++) assertEquals(want[s - 1], reach.at(s, PHAGE), 1e-12, "stage " + s);
    }

    @Test
    void itFollowsTheHeightsNotTheStageNumbers() {
        // A body whose heights are NOT evenly spaced from zero: 2 to 3. By height stage 1 is 2/3 of the last, which
        // is what the rule says; a rule by stage number (1/5) would give 1.2 here.
        MobStages tall = new MobStages(5, new StageBody(2.0, 1, 80), new StageBody(3.0, 1, 160));
        assertEquals(6.0 * 2.0 / 3.0, StagedDistance.byHeight(6).at(1, tall), 1e-12);
    }

    @Test
    void anAuthoredFirstStageIsLinearToTheLastInsteadOfByHeight() {
        // The content-only change the brief asked for: author the small one apart. 1.5 to 6, linear by stage.
        StagedDistance tuned = new StagedDistance(1.5, 6);
        double[] want = {1.5, 2.625, 3.75, 4.875, 6.0};
        for (int s = 1; s <= 5; s++) assertEquals(want[s - 1], tuned.at(s, PHAGE), 1e-12, "stage " + s);
    }

    @Test
    void aStageOutsideTheTableIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> StagedDistance.byHeight(6).at(0, PHAGE));
        assertThrows(IllegalArgumentException.class, () -> StagedDistance.byHeight(6).at(6, PHAGE));
    }

    @Test
    void aBadDistanceIsRefusedByName() {
        assertThrows(IllegalArgumentException.class, () -> StagedDistance.byHeight(0));
        assertThrows(IllegalArgumentException.class, () -> StagedDistance.byHeight(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new StagedDistance(-1.0, 6));
    }
}
