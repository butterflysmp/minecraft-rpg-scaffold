package io.github.butterflysmp.rpg.paper.packet;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The unit-testable half of the threading contract: bukkit(player, task) DEFERS the
 * world-touch to the scheduler instead of running it on the calling thread.
 *
 * This does NOT prove the destination thread's identity (region vs Netty) -- that is a
 * runtime property of the real schedulers, witnessed on a real-server boot, not here. What
 * it catches is the regression that matters: someone "simplifying" the listener by running
 * the Bukkit work inline in the packet callback. Inline it, and this test reddens.
 */
class PacketListenerBaseTest {

    /** Records the onEntity hand-off; every other scheduling route is a contract violation. */
    private static final class RecordingScheduler implements Scheduler {
        Entity entity;
        Runnable task;
        int onEntityCalls;

        @Override public void onEntity(Entity entity, Runnable task) {
            this.entity = entity;
            this.task = task;
            this.onEntityCalls++;
        }
        @Override public void onEntityLater(Entity entity, Runnable task, long delayTicks) {
            throw new AssertionError("unexpected onEntityLater");
        }
        @Override public void onRegion(Location location, Runnable task) {
            throw new AssertionError("bukkit(...) must hop onto the entity, not a region");
        }
        @Override public void onRegionLater(Location location, Runnable task, long delayTicks) {
            throw new AssertionError("unexpected onRegionLater");
        }
        @Override public void onGlobal(Runnable task) {
            throw new AssertionError("unexpected onGlobal");
        }
        @Override public void async(Runnable task) {
            throw new AssertionError("unexpected async");
        }
    }

    /** Concrete subclass so the test can reach the protected hop. */
    private static final class Harness extends PacketListenerBase {
        Harness(Scheduler scheduler) { super(scheduler, PacketListenerPriority.NORMAL); }
        void hop(Player player, Runnable task) { bukkit(player, task); }
    }

    /** A Player whose only live method is isOnline() == true -- enough for bukkit()'s guard. */
    private static Player onlinePlayerStub() {
        return (Player) Proxy.newProxyInstance(
                PacketListenerBaseTest.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isOnline" -> true;
                    case "toString" -> "online-player-stub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }

    @Test
    void bukkitHandsTheTaskToTheSchedulerInsteadOfRunningItInline() {
        var scheduler = new RecordingScheduler();
        var harness = new Harness(scheduler);
        boolean[] ran = {false};
        Runnable worldTouch = () -> ran[0] = true;

        harness.hop(onlinePlayerStub(), worldTouch);

        assertFalse(ran[0], "the world-touch must NOT run on the calling (Netty) thread");
        assertEquals(1, scheduler.onEntityCalls, "it must be handed to the entity scheduler exactly once");
        assertSame(worldTouch, scheduler.task, "the exact task must be deferred, unrun");

        // Running it is the scheduler's job, later, on the owning thread.
        scheduler.task.run();
        assertTrue(ran[0], "once the scheduler runs it, the work happens");
    }

    @Test
    void anOfflinePlayerIsNotScheduled() {
        var scheduler = new RecordingScheduler();
        var harness = new Harness(scheduler);

        harness.hop(null, () -> { throw new AssertionError("must not run for a null player"); });

        assertEquals(0, scheduler.onEntityCalls, "no player, no hop");
    }
}
