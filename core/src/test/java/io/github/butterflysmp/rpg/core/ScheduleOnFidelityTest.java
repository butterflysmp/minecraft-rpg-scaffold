package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.Aim;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two ports a repeating cast needs, proved on the FAKE before anything is built on them.
 *
 * <b>WHY THIS FILE EXISTS AT ALL.</b> {@code FakeTickTarget}'s javadoc states the rule these rows
 * obey: a fake that lies about time makes every green test meaningless. {@code scheduleOn} and
 * {@code aimOf} are about to become the whole basis of a volley's timing and its re-aim, so the
 * fake's fidelity is asserted here rather than assumed by every row that later depends on it.
 *
 * <b>WHAT THIS SUITE CANNOT SEE, stated so a green run is not over-read.</b> It proves the FAKE
 * behaves like the documented contract. It proves nothing about {@code PaperCombatWorld}, whose
 * {@code scheduleOn} hands off to {@code Scheduler.onEntityLater} and whose {@code aimOf} reads a
 * live {@code LivingEntity} under {@code Regions.requireOwned}. Those two are boot-witnessed.
 *
 * Each row names the mutation it forces red.
 */
class ScheduleOnFidelityTest {

    private static final double EPS = 1e-9;

    @Test
    void scheduleOnDEFERSRatherThanRunningInline() {
        var world = new FakeWorld();
        var dummy = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(dummy);
        var ran = new AtomicInteger();

        world.scheduleOn(dummy.id(), 3, ran::incrementAndGet);

        assertEquals(0, ran.get(), "nothing runs on the frame it was scheduled");
        world.advanceTicks(2);
        assertEquals(0, ran.get(), "the delay is 3 ticks -- not 2");
        world.advanceTicks(1);
        assertEquals(1, ran.get(), "it lands on tick 3 exactly");
        // Mutation: run the task inline, or enqueue it at `now` rather than `now + delayTicks` ->
        // one of the two assertions either side of the boundary reddens. Asserting BOTH sides is
        // what makes this insensitive to direction, exactly as IgniteTest's fuse row is.
    }

    @Test
    void scheduleOnREFUSESADelayBelowOne() {
        var world = new FakeWorld();
        var dummy = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(dummy);

        assertThrows(IllegalArgumentException.class,
                () -> world.scheduleOn(dummy.id(), 0, () -> { }),
                "PaperScheduler.onEntityLater clamps 0 up to 1, so a fake that ran it on the "
                        + "current frame would be MORE PERMISSIVE than the server");
        assertThrows(IllegalArgumentException.class, () -> world.scheduleOn(dummy.id(), -1, () -> { }));
    }

    @Test
    void aTaskWhoseCOMBATANTIsGoneNEVERRuns() {
        var world = new FakeWorld();
        var leaving = new FakeWorld.Dummy(Vec3.ZERO);
        var staying = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(leaving);
        world.entities.add(staying);
        var leaverRan = new AtomicInteger();
        var stayerRan = new AtomicInteger();

        world.scheduleOn(leaving.id(), 2, leaverRan::incrementAndGet);
        world.scheduleOn(staying.id(), 2, stayerRan::incrementAndGet);
        world.entities.remove(leaving);
        world.advanceTicks(2);

        assertEquals(0, leaverRan.get(),
                "onEntityLater passes a null retired-runnable, so a departed entity's task simply "
                        + "never runs");
        assertEquals(1, stayerRan.get(),
                "THE CONTROL: without this the row passes just as well against a scheduleOn that "
                        + "drops EVERY task, which is the opposite defect and looks identical here");
        // Mutation: capture the Dummy instead of its id, or drop the isEmpty() guard -> the first
        // assertion reddens. Delete the enqueue entirely -> the control reddens instead.
    }

    @Test
    void aimOfReadsTheLIVEEyeAndFacingRatherThanAFrozenOne() {
        var world = new FakeWorld();
        var dummy = new FakeWorld.Dummy(Vec3.ZERO);
        dummy.eyeHeight = 1.62;
        world.entities.add(dummy);

        Aim before = world.aimOf(dummy.id()).orElseThrow();
        assertEquals(1.62, before.origin().y(), EPS, "the eye sits eyeHeight above the feet");
        assertEquals(1, before.direction().x(), EPS, "and looks down +X by default");

        dummy.moveTo(new Vec3(10, 0, 0));
        dummy.facing = new Vec3(0, 0, 3);   // deliberately NOT a unit vector

        Aim after = world.aimOf(dummy.id()).orElseThrow();
        assertEquals(10, after.origin().x(), EPS, "it followed the body");
        assertEquals(1, after.direction().z(), EPS,
                "and the turn, normalised -- Aim's compact constructor owns that, so a caller "
                        + "never has to");
        assertEquals(0, after.direction().x(), EPS, "the old facing is gone, not blended");
        // Mutation: cache the Aim on the Dummy at construction, or read `pos` once into a field ->
        // the `after` assertions redden. THIS IS THE ROW THAT MAKES RE-AIM TESTABLE AT ALL: if the
        // fake cannot show a caster turning, no volley test can tell re-aiming from not.
    }

    @Test
    void aimOfIsEMPTYForACombatantThatIsNotHere() {
        var world = new FakeWorld();
        assertTrue(world.aimOf(UUID.randomUUID()).isEmpty(),
                "an absent combatant has no aim -- and the volley that asks for one must STOP "
                        + "rather than fall back to a stale line");
    }
}
