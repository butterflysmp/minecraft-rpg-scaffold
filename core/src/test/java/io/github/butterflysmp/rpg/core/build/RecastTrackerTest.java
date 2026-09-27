package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PLAN-build-system.md section 7.4: one player's recast state, fed EVERY stone input. Recall on the left
 * (Active 1) unless a test says otherwise.
 */
class RecastTrackerTest {

    private static final AspectDefinition.Recast UPDRAFT = new AspectDefinition.Recast("recall_updraft", 10, 50);
    private static final Optional<String> NONE = Optional.empty();
    private static final Optional<String> LEAP = Optional.of("recall_updraft");

    /** Recall cast by an input at {@code tick} on {@code slot}, with Updraft active. */
    private static RecastTracker recallAt(LoadoutSlot slot, long tick) {
        RecastTracker t = new RecastTracker();
        assertEquals(NONE, t.input(slot, tick, "recall", true), "the input that casts Recall is not a recast");
        t.castSucceeded("recall", tick, Optional.of(UPDRAFT));
        return t;
    }

    @Test
    void aCleanPressInsideTheWindowRecasts() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        assertEquals(LEAP, t.input(LoadoutSlot.ACTIVE_1, 1020, "recall", true));
    }

    /** ONE RECAST PER CAST: the third press inside the window is Recall again (on cooldown), not a second leap. */
    @Test
    void onlyOneRecastPerCast() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        assertEquals(LEAP, t.input(LoadoutSlot.ACTIVE_1, 1020, "recall", true));
        assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_1, 1040, "recall", true));
    }

    /**
     * THE HELD-INPUT GUARD, end to end: a held right-click through the whole window never recasts. Every held
     * input is refused by Recall's cooldown, and every one of them must still be RECORDED, or the hold reads as
     * released after 8 ticks and the input at 1012 fires the leap by itself.
     */
    @Test
    void aHeldRightClickNeverRecasts() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_2, 1000);
        for (long tick = 1004; tick <= 1060; tick += 4) {
            assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_2, tick, "recall", true), "held input at " + tick);
        }
    }

    /** Released after a short hold, a pause of 9+, pressed again: the leap. */
    @Test
    void releasedThenPressedAgainRecasts() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_2, 1000);
        assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_2, 1004, "recall", true));
        assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_2, 1008, "recall", true));
        assertEquals(LEAP, t.input(LoadoutSlot.ACTIVE_2, 1017, "recall", true));
    }

    /** A held left click on a block: a swing every tick, all window long. */
    @Test
    void aHeldBlockSwingNeverRecasts() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        for (long tick = 1001; tick <= 1060; tick++) {
            assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_1, tick, "recall", true), "held swing at " + tick);
        }
    }

    /** Another button's stream does not end Recall's hold, and another ability's press is never a recast. */
    @Test
    void theHoldIsPerButtonAndTheRecastPerAbility() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_2, 1020, "solar_lance", true));
        assertEquals(LEAP, t.input(LoadoutSlot.ACTIVE_1, 1030, "recall", true));
    }

    @Test
    void aWindowClosesWhenTheAspectIsNoLongerActive() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_1, 1020, "recall", false));
    }

    @Test
    void noRecastWithoutTheAspect() {
        RecastTracker t = new RecastTracker();
        t.input(LoadoutSlot.ACTIVE_1, 1000, "recall", false);
        t.castSucceeded("recall", 1000, Optional.empty());
        assertEquals(NONE, t.input(LoadoutSlot.ACTIVE_1, 1020, "recall", false));
    }

    /** Recall cast again without Updraft closes the old window; another ability's cast leaves it open. */
    @Test
    void onlyTheTargetsOwnCastReplacesTheWindow() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        t.castSucceeded("solar_lance", 1005, Optional.empty());
        assertEquals(LEAP, t.input(LoadoutSlot.ACTIVE_1, 1020, "recall", true));

        RecastTracker u = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        u.castSucceeded("recall", 1005, Optional.empty());
        assertEquals(NONE, u.input(LoadoutSlot.ACTIVE_1, 1020, "recall", true));
    }

    @Test
    void clearForgetsTheWindow() {
        RecastTracker t = recallAt(LoadoutSlot.ACTIVE_1, 1000);
        t.clear();
        assertTrue(t.input(LoadoutSlot.ACTIVE_1, 1020, "recall", true).isEmpty());
    }
}
