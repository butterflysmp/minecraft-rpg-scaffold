package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The acceptance is the cleanup, not the effect. Rooted now carries removable state -- a
 * MOVEMENT_SPEED=0 modifier that kills the mob's AI drive -- alongside the per-tick velocity-zero
 * that kills imparted movement (knockback, jumps). So the same modifier-cleanup hazard Soaked
 * hardened applies here: a leaked 0-modifier is a permanently-frozen mob. Each test names the
 * mutation it forces to redden. (The fakes are proved faithful in RepeatingTaskFidelityTest.)
 *
 * ONE class, TWO instances: ctx.immobilize() (Rooted) and ctx.freeze() (Freeze) are both an
 * ImmobilizeStatus, so these tests are the lifecycle for both. In particular, the expiry and
 * death tests assert isImmobilized(id) goes false on BOTH paths -- which is exactly the flag the
 * Freeze listeners gate on, so this is also the proof that Freeze's attack-suppression lifts on
 * expiry and on death (no dangling suppression). A separate Freeze lifecycle test would re-run
 * this same class; the difference (attack-event cancellation, creeper fuse-reset) is boot-witnessed.
 *
 * These are server-free unit tests of the LIFECYCLE. That MOVEMENT_SPEED=0 + velocity-zero
 * actually stops a mob dead in the world -- zero creep, even when aggro'd -- is the boot gate.
 */
class ImmobilizeStatusTest {

    private static final double EPS = 1e-9;

    @Test
    void applyingRootRegistersOneTaskAndSetsSpeedToZero() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        status.apply(id, target, speed, 100, () -> {});

        assertTrue(status.isImmobilized(id), "the mob is rooted");
        assertEquals(1, target.pending(), "exactly one repeating task is scheduled");
        assertEquals(1, speed.modifierCount(), "one speed modifier, set on apply");
        assertEquals(0.0, speed.speedFactor(), EPS, "MOVEMENT_SPEED driven to zero -- the AI-drive kill");
        // Mutation: an apply() that never calls RepeatingTask.start leaves pending() == 0.
    }

    @Test
    void theTaskExpiresAndRestoresBaseSpeedExactly() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();
        int[] zeroed = {0};

        status.apply(id, target, speed, 3, () -> zeroed[0]++);
        target.advance(10);

        assertEquals(3, zeroed[0], "velocity-zero ran once per tick for the duration");
        assertEquals(0, target.pending(), "no task is still scheduled after expiry");
        assertFalse(status.isImmobilized(id), "and the registry entry is gone");
        assertEquals(0, speed.modifierCount(), "the speed modifier was removed");
        assertEquals(1.0, speed.speedFactor(), EPS, "base speed restored EXACTLY -- no permanently-frozen mob");
        // Mutation: drop removeSpeedModifier() at expiry -> speedFactor stuck at 0 -> reddens.
    }

    @Test
    void whenTheMobDiesCleanupTouchesNothingAndLeavesNoState() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();
        int[] zeroed = {0};

        status.apply(id, target, speed, 100, () -> zeroed[0]++);
        target.advance(1);
        assertEquals(1, zeroed[0], "one tick happened while alive");

        target.active = false; // the mob dies / is removed -- the common case

        assertDoesNotThrow(() -> target.advance(1),
                "cleanup must not touch the removed entity (the fake throws if it does)");
        assertEquals(0, target.pending(), "the task self-cancelled: nothing still scheduled at the dead mob");
        assertFalse(status.isImmobilized(id), "the registry entry is gone");
        assertEquals(1, zeroed[0], "velocity-zero did NOT run against the removed mob");
        // The 0-modifier died with the entity; removing it would mean touching a gone entity.
        // Mutation A: delete the isActive() guard in RepeatingTask.step -> ticks the dead mob ->
        //             pending() stays 1 -> reddens.
        // Mutation B: move removeSpeedModifier into onStop -> touches the dead entity -> the fake
        //             throws "touched a removed entity" -> reddens.
    }

    @Test
    void reRootingRefreshesTheOneTaskInsteadOfStackingASecond() {
        var status = new ImmobilizeStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        status.apply(id, target, speed, 40, () -> {});
        target.advance(10);                              // 30 ticks left
        assertEquals(30, status.remainingTicks(id));

        status.apply(id, target, speed, 40, () -> {});   // re-root the already-rooted mob

        assertEquals(1, target.pending(), "still ONE task, not two");
        assertEquals(40, status.remainingTicks(id), "its duration was refreshed, not stacked");
        assertEquals(1, speed.modifierCount(), "still one speed modifier, not re-added");
        // Mutation: remove the find-existing-and-refresh branch so the second apply() starts a
        //           second task -> pending() == 2 -> reddens.
    }
}
