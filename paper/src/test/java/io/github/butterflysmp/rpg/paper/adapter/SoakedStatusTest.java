package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Soaked is the first status to carry removable Bukkit state (a speed modifier) through the
 * death/expiry/refresh lifecycle, so the cleanup hazards the primitive exists to prevent come
 * due here. The acceptance is the cleanup, not the slow: base speed exactly restored, exactly
 * one keyed modifier, and nothing touched on a dead entity. Each test names the mutation it
 * forces to redden; the fake speed attribute reads modifier state back so the leak cases are
 * unit-observable rather than boot-only.
 */
class SoakedStatusTest {

    private static final double EPS = 1e-9;

    @Test
    void eachStackMultipliesSpeedByNineTenths() {
        var soaked = new SoakedStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        soaked.apply(id, target, speed, 200);
        assertEquals(0.9, speed.speedFactor(), EPS, "one stack -> 0.9");
        soaked.apply(id, target, speed, 200);
        assertEquals(0.81, speed.speedFactor(), EPS, "two stacks -> 0.81, multiplicative");
        soaked.apply(id, target, speed, 200);
        assertEquals(0.729, speed.speedFactor(), EPS, "three stacks -> 0.729");
        // Mutation: additive slow, or dropping Math.pow, gives the wrong sequence -> reddens.
    }

    @Test
    void theSlowIsFlooredAtSixtyPercent() {
        var soaked = new SoakedStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        for (int i = 0; i < 6; i++) soaked.apply(id, target, speed, 200); // 0.9^6 = 0.531 < 0.6

        assertEquals(0.6, speed.speedFactor(), EPS, "never slower than 0.6x base");
        // Mutation: drop Math.max(SPEED_FLOOR, ...) -> 0.9^6 < 0.6 -> reddens.
    }

    @Test
    void exactlyOneKeyedModifierEverPresentNotN() {
        var soaked = new SoakedStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        // Across many ticks with a single application: the modifier is set once, never per tick.
        soaked.apply(id, target, speed, 200);
        target.advance(50);
        assertEquals(1, speed.modifierCount(), "one modifier after 50 ticks, not 50");

        // Across many applications: each stack change replaces, never appends.
        soaked.apply(id, target, speed, 200);
        soaked.apply(id, target, speed, 200);
        assertEquals(1, speed.modifierCount(), "still one modifier after three applications, not three");
        assertEquals(0.729, speed.speedFactor(), EPS, "and it reflects the current stack count");
        // Mutation A: apply the modifier in the per-tick body -> count climbs with ticks -> reddens.
        // Mutation B: drop the "if hasSoakModifier removeSoakModifier" replace-guard in setSlow
        //             -> count climbs with applications -> reddens. A mob N-stacked below the floor.
    }

    @Test
    void reSoakingRefreshesTheOneTaskAndAddsAStack() {
        var soaked = new SoakedStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        soaked.apply(id, target, speed, 40);
        target.advance(30);                          // 10 ticks left
        soaked.apply(id, target, speed, 40);         // re-soak the already-soaked mob

        assertEquals(1, target.pending(), "still ONE task, not two");
        assertEquals(2, soaked.stacks(id), "the stack was added");
        assertEquals(0.81, speed.speedFactor(), EPS, "and the slow deepened");

        // All-at-once decay: the whole timer refreshed, so it outlives the original window.
        target.advance(35);
        assertTrue(soaked.isSoaked(id), "duration was refreshed to full, not left at 10");
        // Mutation: remove the find-existing branch -> the second apply starts a second task
        //           -> pending() == 2 -> reddens.
    }

    @Test
    void whenTheDurationExpiresTheModifierIsRemovedAndBaseRestoredExactly() {
        var soaked = new SoakedStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        soaked.apply(id, target, speed, 3);
        assertEquals(0.9, speed.speedFactor(), EPS, "slowed while active");

        target.advance(5);                           // past the 3-tick duration

        assertEquals(0, speed.modifierCount(), "the modifier was removed");
        assertEquals(1.0, speed.speedFactor(), EPS, "base speed restored EXACTLY -- no leaked modifier");
        assertEquals(0, target.pending(), "no task still scheduled");
        assertFalse(soaked.isSoaked(id), "registry entry gone");
        // Mutation: body never returns false, OR skip removeSoakModifier at expiry
        //           -> speedFactor stays < 1.0 (a permanently-slow mob) / task still scheduled -> reddens.
    }

    @Test
    void whenTheMobDiesCleanupRunsWithoutTouchingItAndLeavesNoState() {
        var soaked = new SoakedStatus();
        var target = new FakeTickTarget();
        var speed = new FakeSpeedAttribute(target);
        UUID id = UUID.randomUUID();

        soaked.apply(id, target, speed, 200);
        target.advance(1);                           // one tick while alive
        target.active = false;                       // the mob dies mid-soak -- the common case

        assertDoesNotThrow(() -> target.advance(1),
                "cleanup must not touch the removed entity (the fake throws if it does)");
        assertEquals(0, target.pending(), "the task self-cancelled: nothing still ticking the dead mob");
        assertFalse(soaked.isSoaked(id), "and no leaked per-target state reference");
        // The modifier died with the entity; removing it would mean touching a gone entity.
        // Mutation A: delete the isActive guard in RepeatingTask.step -> the task keeps ticking the
        //             dead mob -> pending() stays 1 -> reddens.
        // Mutation B: move removeSoakModifier into onStop (clean up on EVERY stop) -> on the death
        //             path it runs while inactive -> the fake throws "touched a removed entity" -> reddens.
    }
}
