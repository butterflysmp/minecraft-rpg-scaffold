package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.build.LandingHook;
import io.github.butterflysmp.rpg.core.build.SafeLanding;
import io.github.butterflysmp.rpg.core.build.Touchdown;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sunder's slam on the paper side (PLAN-melee-class.md section 8.2, S1; the seat's rulings 4 and 5): the hook is
 * called EXACTLY ONCE per arming, by whichever of the per-tick step and the FALL event sees the landing first, and
 * not at all on death, quit or a re-arm. The entity is a {@link Proxy} stub (the {@code PacketListenerBaseTest}
 * pattern) whose ground, fall distance and position each row sets; the scheduler runs its queue when told.
 */
class SafeLandingsTest {

    /** A leaping entity whose server readings the row controls. */
    private static final class Leaper {
        final UUID id = UUID.randomUUID();
        boolean onGround = true;
        float fallDistance = 0;
        double x, y = 64, z;

        LivingEntity entity() {
            return (LivingEntity) Proxy.newProxyInstance(SafeLandingsTest.class.getClassLoader(),
                    new Class<?>[]{LivingEntity.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getUniqueId" -> id;
                        case "isOnGround" -> onGround;
                        case "getFallDistance" -> fallDistance;
                        case "getLocation" -> new Location(null, x, y, z);
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    });
        }
    }

    /** {@code onEntityLater} queues; {@link #tick} runs everything queued for the next tick. */
    private static final class TickScheduler implements Scheduler {
        private List<Runnable> next = new ArrayList<>();

        void tick() {
            List<Runnable> now = next;
            next = new ArrayList<>();
            now.forEach(Runnable::run);
        }

        @Override public void onEntity(Entity entity, Runnable task) { task.run(); }
        @Override public void onEntityLater(Entity entity, Runnable task, long delayTicks) { next.add(task); }
        @Override public void onRegion(Location location, Runnable task) { task.run(); }
        @Override public void onRegionLater(Location location, Runnable task, long delayTicks) { next.add(task); }
        @Override public void onGlobal(Runnable task) { task.run(); }
        @Override public void async(Runnable task) { task.run(); }
    }

    /** Every call the hook received, in order. */
    private static final class Recorder implements LandingHook {
        final List<String> calls = new ArrayList<>();
        final List<Vec3> feet = new ArrayList<>();

        @Override public void landed(Vec3 at, Touchdown how) { calls.add("landed " + how); feet.add(at); }
        @Override public void ended(Touchdown how) { calls.add("ended " + how); }
    }

    private final SafeLandings landings = new SafeLandings();
    private final TickScheduler scheduler = new TickScheduler();
    private final Leaper leaper = new Leaper();
    private final LivingEntity entity = leaper.entity();
    private final Recorder hook = new Recorder();

    private void armAndTakeOff() {
        landings.arm(entity, scheduler, hook);
        leaper.onGround = false;
    }

    @Test
    void aLowLeapIsSlammedByTheStepAtTheLiveFeetOnceAndTheMarkStaysImmuneForTheLandingTick() {
        armAndTakeOff();
        scheduler.tick();                                  // AIRBORNE
        leaper.fallDistance = 1.2f;
        scheduler.tick();
        leaper.onGround = true;
        leaper.fallDistance = 0;
        leaper.x = 5.5;
        scheduler.tick();                                  // LANDED: the slam
        assertEquals(List.of("landed LANDED"), hook.calls);
        assertEquals(new Vec3(5.5, 64, 0), hook.feet.getFirst());
        // A FALL event on the landing tick (if one comes) only cancels: the slam does not go off twice.
        assertTrue(landings.consumeFall(entity), "the landing tick is still immune");
        scheduler.tick();
        assertEquals(List.of("landed LANDED"), hook.calls);
    }

    /** THE ROW THE FALL ARM EXISTS FOR: the FALL event beats the step, and it is the one that slams. */
    @Test
    void aFallEventBeforeTheStepSeesTheGroundSlamsOnceAndCancels() {
        armAndTakeOff();
        scheduler.tick();
        leaper.fallDistance = 6;
        scheduler.tick();
        leaper.x = 4;
        assertTrue(landings.consumeFall(entity), "Sunder never deals fall damage (ruling 5, F-d)");
        assertEquals(List.of("landed LANDED"), hook.calls);
        leaper.onGround = true;
        scheduler.tick();                                  // the step's task finds no mark
        assertEquals(List.of("landed LANDED"), hook.calls);
        assertFalse(landings.armed(leaper.id), "the mark is spent; a later fall is an ordinary fall");
        assertFalse(landings.consumeFall(entity));
    }

    @Test
    void waterAbsorbingTheFallSlamsAtTheLiveFeet() {
        armAndTakeOff();
        scheduler.tick();
        leaper.fallDistance = 1.5f;
        scheduler.tick();
        leaper.fallDistance = 0;                           // still not on the ground: absorbed
        leaper.y = 61.2;
        scheduler.tick();
        assertEquals(List.of("landed ABSORBED"), hook.calls);
        assertEquals(new Vec3(0, 61.2, 0), hook.feet.getFirst());
        assertFalse(landings.armed(leaper.id));
    }

    @Test
    void aFailedTakeOffSlamsInPlace() {
        landings.arm(entity, scheduler, hook);             // the ceiling: never leaves the ground
        for (int i = 0; i <= SafeLanding.TAKE_OFF_TICKS; i++) scheduler.tick();
        assertEquals(List.of("landed NO_TAKE_OFF"), hook.calls);
        assertEquals(new Vec3(0, 64, 0), hook.feet.getFirst());
    }

    @Test
    void theBackstopEndsTheMarkWithoutASlam() {
        armAndTakeOff();
        for (int i = 0; i <= SafeLanding.BACKSTOP_TICKS; i++) {
            leaper.fallDistance += 1;                      // falling the whole time, never absorbed
            scheduler.tick();
        }
        assertEquals(List.of("ended BACKSTOP"), hook.calls);
    }

    /** Ruling 4: death or quit mid-leap -- no slam, no posthumous burst. */
    @Test
    void forgettingMidLeapEndsItWithoutASlamAndTheStepNeverFiresLater() {
        armAndTakeOff();
        scheduler.tick();
        landings.forget(leaper.id);
        leaper.onGround = true;
        scheduler.tick();
        scheduler.tick();
        assertEquals(List.of("ended FORGOTTEN"), hook.calls);
    }

    @Test
    void aReArmEndsTheFirstSlamAsSupersededAndOnlyTheSecondLands() {
        armAndTakeOff();
        scheduler.tick();
        var second = new Recorder();
        landings.arm(entity, scheduler, second);
        assertEquals(List.of("ended SUPERSEDED"), hook.calls);
        scheduler.tick();                                  // the first leap's stale task and the second's first
        leaper.onGround = true;
        scheduler.tick();
        assertEquals(List.of("ended SUPERSEDED"), hook.calls, "the first leap never lands");
        assertEquals(List.of("landed LANDED"), second.calls);
    }

    /** Ruling 31 unchanged: a bare mark (no hook) still makes the landing free, and calls nothing. */
    @Test
    void aBareMarkIsStillImmuneAndHasNoHookToCall() {
        landings.arm(entity, scheduler);
        leaper.onGround = false;
        scheduler.tick();
        assertTrue(landings.consumeFall(entity));
        assertFalse(landings.consumeFall(entity), "one landing each");
    }
}
