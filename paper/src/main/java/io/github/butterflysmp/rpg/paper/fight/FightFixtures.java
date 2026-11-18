package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.MageCycle;
import io.github.butterflysmp.rpg.core.fight.RingFixture;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * THE DEV FIXTURE FOR STEPS F4 + F5 (PLAN-dragon-fight.md section 6; GATE-fight-ring.md): the ring drawn and the
 * lightning, in an ordinary world. No Mage mob, no crystals, no arena, no dragon. Dev only, like {@code /rpg selftest}:
 * its command refuses off a dev server.
 *
 * <p><b>One fixture at a time</b>, and its state lives in the {@link RingFixture} this holds, never in a static. It
 * changes no blocks and spawns nothing that outlives it (forced particles and effect-only bolts), so a stop leaves
 * nothing behind. It ends on {@code stop}, on a new {@code start} (which replaces it), at the end of every selftest
 * run, and on plugin disable.
 */
public final class FightFixtures {

    /** The fight file the fixture runs: {@code content/fights/dragon.yml}. */
    public static final String FIGHT_ID = "dragon";

    private final AdapterContext adapters;
    private final Supplier<Optional<FightContent>> fight;

    private RingFixture fixture;
    private RepeatingTask loop;
    private World world;

    /** {@code fight} is read at each start, so a content reload is picked up by the next fixture. */
    public FightFixtures(AdapterContext adapters, Supplier<Optional<FightContent>> fight) {
        this.adapters = adapters;
        this.fight = fight;
    }

    public boolean running() {
        return fixture != null && !fixture.stopped();
    }

    /**
     * Starts a fixture with C at the block position of {@code at} (the command's source location: the caller's feet,
     * or wherever {@code execute positioned} put it), replacing any running one.
     */
    public String start(Player caller, Location at) {
        Optional<FightContent> content = fight.get();
        if (content.isEmpty()) return "No fight '" + FIGHT_ID + "' is loaded (see the startup log).";
        if (running()) stop("replaced by a new start");
        FightContent f = content.get();
        Location block = at.getBlock().getLocation();
        world = block.getWorld();
        double cx = block.getBlockX() + 0.5;
        double cz = block.getBlockZ() + 0.5;
        double groundY = block.getBlockY();
        Location centre = new Location(world, cx, groundY, cz);
        BukkitFightPort port = new BukkitFightPort(world, centre, f, adapters, adapters.log());
        RingFixture started = new RingFixture(f.settings(), f.look(), cx, groundY, cz, port);
        fixture = started;
        adapters.log().info(String.format(Locale.ROOT, "FIGHT start world=%s centre=%.1f %.1f %.1f by=%s",
                world.getName(), cx, groundY, cz, caller.getName()));
        loop = RepeatingTask.start(new RegionTarget(centre, started), 1, "fight-fixture", () -> {
            started.tick();
            return !started.stopped();
        }, () -> { });
        return String.format(Locale.ROOT, "Fight fixture started at %.1f %.1f %.1f (ring 48 about you).",
                cx, groundY, cz);
    }

    public String land(double dxBlocks, double dzBlocks) {
        if (!running()) return "No fight fixture is running.";
        MageCycle.LandingResult r = fixture.land(dxBlocks, dzBlocks);
        return String.format(Locale.ROOT, "Mage lands at C + (%.2f, %.2f): %s", dxBlocks, dzBlocks, r);
    }

    public String mageDie() {
        if (!running()) return "No fight fixture is running.";
        fixture.mageDie();
        return "The Mage dies.";
    }

    /** Ends the running fixture, if any. Returns what it did. */
    public String stop(String why) {
        if (!running()) return "No fight fixture is running.";
        fixture.stop();
        if (loop != null) loop.cancel();
        adapters.log().info("FIGHT ended reason=" + why);
        fixture = null;
        loop = null;
        world = null;
        return "Fight fixture stopped.";
    }

    /**
     * A mob died. If a player killed it while a fixture runs in that world, the kill is fed in at the killer's feet
     * (the seat's placeholder: any mob counts; PLAN-dragon-fight.md section 7 item 7 is open).
     */
    public void onDeath(EntityDeathEvent event) {
        if (!running()) return;
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
        Player killer = dead.getKiller();
        if (killer == null || killer.getWorld() != world) return;
        Location feet = killer.getLocation();
        fixture.kill(killer.getUniqueId(), feet.getX(), feet.getZ());
    }

    /** Plugin disable or server stop: the fixture ends cleanly. */
    public void onDisable() {
        if (running()) stop("plugin-disable");
    }

    /** The loop runs on C's region thread, and only while this fixture is the one running. */
    private final class RegionTarget implements RepeatingTaskTarget {
        private final Location centre;
        private final RingFixture owner;

        RegionTarget(Location centre, RingFixture owner) {
            this.centre = centre;
            this.owner = owner;
        }

        @Override public boolean isActive() {
            return fixture == owner && !owner.stopped();
        }

        @Override public void scheduleTick(int delayTicks, Runnable run) {
            adapters.scheduler().onRegionLater(centre, run, delayTicks);
        }

        @Override public void loopFailed(String loopName, RuntimeException cause) {
            adapters.log().severe("FIGHT loop '" + loopName + "' threw and stopped: " + cause);
            owner.stop();
        }
    }
}
