package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The acceptance is the cleanup, not the effect. Rooted is the simplest per-tick status --
 * velocity-zero, nothing to revert -- so a lifecycle leak surfaces here, not tangled in
 * stacking. Each test names the mutation it forces to redden, so a green here means the test
 * SEES the lifecycle, not just the happy path. (The fake it runs on is proved faithful in
 * RepeatingTaskFidelityTest first.)
 *
 * The perTick action is a counter, never a real velocity write, so this is a pure unit test:
 * `./mvnw -pl paper test`, no server. That the velocity-zero actually holds a mob in place is
 * the one boot observation.
 */
class ImmobilizeStatusTest {

    @Test
    void applyingRootRegistersExactlyOneRepeatingTask() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        UUID id = UUID.randomUUID();

        status.apply(id, target, 100, () -> {});

        assertTrue(status.isRooted(id), "the mob is rooted");
        assertEquals(1, target.pending(), "exactly one repeating task is scheduled");
        // Mutation: an apply() that never calls RepeatingTask.start leaves pending() == 0.
    }

    @Test
    void theTaskSelfCancelsWhenTheDurationExpires() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        UUID id = UUID.randomUUID();
        int[] zeroed = {0};

        status.apply(id, target, 3, () -> zeroed[0]++);
        target.advance(10);

        assertEquals(3, zeroed[0], "the effect ran once per tick for the duration, then stopped");
        assertEquals(0, target.pending(), "no task is still scheduled after expiry");
        assertFalse(status.isRooted(id), "and the registry entry is gone");
        // Mutation: a body that never returns false leaves the task re-arming -> pending() stays 1
        //           (or the fake's due-order clock keeps firing) -> reddens.
    }

    @Test
    void theTaskSelfCancelsWhenTheMobIsRemovedAndNeverTouchesIt() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        UUID id = UUID.randomUUID();
        int[] zeroed = {0};

        status.apply(id, target, 100, () -> zeroed[0]++);
        target.advance(1);
        assertEquals(1, zeroed[0], "one tick happened while alive");

        target.active = false; // the mob dies / is removed -- the common case
        target.advance(1);

        assertEquals(0, target.pending(), "the task self-cancelled: nothing still scheduled at the dead mob");
        assertFalse(status.isRooted(id), "the registry entry is gone");
        assertEquals(1, zeroed[0], "the effect did NOT run against the removed mob");
        // Mutation: delete the isActive() guard in RepeatingTask.step -> the loop re-arms,
        //           pending() stays 1 and zeroed[0] becomes 2 (touching a removed mob) -> reddens.
    }

    @Test
    void reRootingRefreshesTheOneTaskInsteadOfStackingASecond() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        UUID id = UUID.randomUUID();

        status.apply(id, target, 40, () -> {});
        target.advance(10);                       // 30 ticks left
        assertEquals(30, status.remainingTicks(id));

        status.apply(id, target, 40, () -> {});   // re-root the already-rooted mob

        assertEquals(1, target.pending(), "still ONE task, not two");
        assertEquals(40, status.remainingTicks(id), "its duration was refreshed, not stacked");
        // Mutation: remove the find-existing-and-refresh branch so the second apply() starts a
        //           second task -> pending() == 2 -> reddens. Without this mutation, "one not two"
        //           could pass merely because the second-application path was never exercised.
    }
}
