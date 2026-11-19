package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * THE FIGHTS THE PLUGIN HOLDS, AND THE DEV COMMAND'S ONE (the F8 brief, section 3). Dev only, like
 * {@code /rpg selftest}: its command refuses off a dev server.
 *
 * <p>Each fight is a {@link FightInstance} at a {@link FightSite}, held here BY IDENTITY:
 * <ul>
 *   <li>a death is offered to every fight;</li>
 *   <li>plugin disable ends every fight;</li>
 *   <li>a fight that ends by any route removes itself.</li>
 * </ul>
 * So two fights at once are possible in the code. The dev command drives ONE: {@code start} ends the one it started
 * before (if it still runs) and starts a new one at an {@link OrdinaryWorldSite} where the command ran. No fight state
 * lives here beyond the map and which one is the command's (CLAUDE.md invariant 3).
 */
public final class FightFixtures {

    /** The fight file the fixture runs: {@code content/fights/dragon.yml}. */
    public static final String FIGHT_ID = "dragon";

    private final AdapterContext adapters;
    private final Supplier<Optional<FightContent>> fight;
    private final Supplier<MobRegistry> mobs;

    /** Every fight running, by its id, in the order started. */
    private final Map<UUID, FightInstance> fights = new LinkedHashMap<>();

    /** The fight the dev command drives, or null. */
    private FightInstance current;

    /** {@code fight} and {@code mobs} are read at each start, so a content reload is picked up by the next fight. */
    public FightFixtures(AdapterContext adapters, Supplier<Optional<FightContent>> fight, Supplier<MobRegistry> mobs) {
        this.adapters = adapters;
        this.fight = fight;
        this.mobs = mobs;
    }

    /** Whether the dev command's fight runs. */
    public boolean running() {
        return current != null && current.running();
    }

    /** How many fights are held right now. */
    public int count() {
        return fights.size();
    }

    /**
     * Starts the dev command's fight with C at the block position of {@code at} (the command's source location: the
     * caller's feet, or wherever {@code execute positioned} put it), ending the one it started before.
     */
    public String start(Player caller, Location at) {
        Optional<FightContent> content = fight.get();
        if (content.isEmpty()) return "No fight '" + FIGHT_ID + "' is loaded (see the startup log).";
        if (running()) current.stop("replaced by a new start");
        FightContent f = content.get();
        FightSite site = new OrdinaryWorldSite(at, f);
        FightInstance started = new FightInstance(adapters, f, mobs.get(), site, caller.getName(), this::forget);
        fights.put(started.id(), started);
        current = started;
        Location c = site.centre();
        return String.format(java.util.Locale.ROOT, "Fight fixture started at %.1f %.1f %.1f (ring %.0f about you).",
                c.getX(), c.getY(), c.getZ(), f.settings().borderRadiusBlocks());
    }

    /** A fight ended by any route: it is no longer held, and no longer the command's. */
    private void forget(FightInstance ended) {
        fights.remove(ended.id());
        if (current == ended) current = null;
    }

    public String land(double dxBlocks, double dzBlocks) {
        return running() ? current.land(dxBlocks, dzBlocks) : "No fight fixture is running.";
    }

    public String landRandom() {
        return running() ? current.landRandom() : "No fight fixture is running.";
    }

    public String play(double minDistanceBlocks) {
        return running() ? current.play(minDistanceBlocks) : "No fight fixture is running.";
    }

    public String mageDie() {
        return running() ? current.mageDie() : "No fight fixture is running.";
    }

    public String probe() {
        return running() ? current.probe() : "No fight fixture is running.";
    }

    /** Ends the dev command's fight, if any. */
    public String stop(String why) {
        return running() ? current.stop(why) : "No fight fixture is running.";
    }

    /** A mob died: offered to every fight held (each knows its own Mage and its own world). */
    public void onDeath(EntityDeathEvent event) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            f.onDeath(event);
        }
    }

    /** Plugin disable or server stop: every fight ends cleanly. */
    public void onDisable() {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (f.running()) f.stop("plugin-disable");
        }
    }
}
