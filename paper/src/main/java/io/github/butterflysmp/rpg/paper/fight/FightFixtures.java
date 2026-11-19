package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.FightReturn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * THE FIGHTS THE PLUGIN HOLDS, AND THE DEV COMMAND'S ONE (the F8 brief, section 3; step F8b's arena). Dev only, like
 * {@code /rpg selftest}: its command refuses off a dev server.
 *
 * <p>Each fight is a {@link FightInstance} at a {@link FightSite}, held here BY IDENTITY:
 * <ul>
 *   <li>a death is offered to every fight;</li>
 *   <li>plugin disable ends every fight;</li>
 *   <li>a fight that ends by any route removes itself.</li>
 * </ul>
 * So two fights at once are possible in the code. The dev command drives ONE: {@code start} begins one at an
 * {@link OrdinaryWorldSite}; {@code enter} begins one in its OWN WORLD at an {@link ArenaSite}, or joins the arena fight
 * that runs.
 *
 * <p><b>The arena's people</b> (the brief's section 4 c to e):
 * <ul>
 *   <li>Entering saves his RETURN LOCATION in his profile, through {@code PlayerRepository} (C5).</li>
 *   <li>Leaving, death and every ending send him back there and clear it. A death sends him back by setting his
 *       respawn there; he is out of the fight.</li>
 *   <li>A quit puts him out and keeps the return, so his next login sends him back
 *       ({@link #onJoinSettled}).</li>
 *   <li>Every ending returns everyone inside, then unloads the world and deletes its folder ({@link FightWorlds}).</li>
 * </ul>
 */
public final class FightFixtures {

    /** The fight file the fixture runs: {@code content/fights/dragon.yml}. */
    public static final String FIGHT_ID = "dragon";

    private final AdapterContext adapters;
    private final Supplier<Optional<FightContent>> fight;
    private final Supplier<MobRegistry> mobs;
    private final ProfileService profiles;
    private final FightWorlds worlds;

    /** Every fight running, by its id, in the order started. */
    private final Map<UUID, FightInstance> fights = new LinkedHashMap<>();

    /** The arena fights among them, by the same id: their site, world and folder. */
    private final Map<UUID, ArenaFight> arenas = new LinkedHashMap<>();

    /** The fight the dev command drives, or null. */
    private FightInstance current;

    /** One arena fight: its instance, its site (who is inside), its world, and the folder to delete at the end. */
    private record ArenaFight(FightInstance fight, ArenaSite site, World world, Path folder) {}

    /** {@code fight} and {@code mobs} are read at each start, so a content reload is picked up by the next fight. */
    public FightFixtures(AdapterContext adapters, Supplier<Optional<FightContent>> fight, Supplier<MobRegistry> mobs,
                         ProfileService profiles, FightWorlds worlds) {
        this.adapters = adapters;
        this.fight = fight;
        this.mobs = mobs;
        this.profiles = profiles;
        this.worlds = worlds;
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
        return String.format(Locale.ROOT, "Fight fixture started at %.1f %.1f %.1f (ring %.0f about you).",
                c.getX(), c.getY(), c.getZ(), f.settings().borderRadiusBlocks());
    }

    // --- the arena (step F8b) ----------------------------------------------------------------------------------------

    /**
     * {@code enter} (dev only; SEAT PLACEHOLDER per Ben's "yes"): the caller joins the arena fight that runs, or a new
     * one begins in its own world with him in it. His return location is saved in his profile FIRST; if that cannot
     * be saved (his profile is not loaded) he is not taken in.
     */
    public String enter(Player p) {
        Optional<FightContent> content = fight.get();
        if (content.isEmpty()) return "No fight '" + FIGHT_ID + "' is loaded (see the startup log).";
        if (FightWorlds.isFightWorld(p.getWorld())) return "You are already in a fight world.";
        if (profiles.profile(p.getUniqueId()).isEmpty()) {
            return "Your profile is not loaded yet, so your way back cannot be saved; try again in a moment.";
        }
        FightContent f = content.get();
        ArenaFight a = runningArena();
        if (a == null) {
            World w = worlds.create(f.arena());
            if (w == null) return "The fight's world could not be created (see the log).";
            worlds.build(w, f.arena().shape());
            ArenaSite site = new ArenaSite(w, f.arena().shape());
            site.addEntrant(p.getUniqueId());
            FightInstance started = new FightInstance(adapters, f, mobs.get(), site, p.getName(), this::forget);
            a = new ArenaFight(started, site, w, FightWorlds.folderOf(w));
            fights.put(started.id(), started);
            arenas.put(started.id(), a);
            if (running()) current.stop("replaced by an arena fight");
            current = started;
        } else {
            a.site().addEntrant(p.getUniqueId());
        }
        Location from = p.getLocation();
        FightReturn back = new FightReturn(from.getWorld().getName(), from.getX(), from.getY(), from.getZ(),
                from.getYaw(), from.getPitch());
        if (!profiles.setFightReturn(p.getUniqueId(), back)) {
            a.site().removeEntrant(p.getUniqueId());
            return "Your way back could not be saved; not entering.";
        }
        Location entry = new Location(a.world(), a.site().centre().getX() + f.arena().entryDxBlocks(),
                a.site().centre().getY(), a.site().centre().getZ() + f.arena().entryDzBlocks());
        p.teleportAsync(entry);
        adapters.log().info(String.format(Locale.ROOT, "FIGHTWORLD entered player=%s world=%s return=%s",
                p.getName(), a.world().getName(), where(back)));
        return "Entered the fight in " + a.world().getName() + ".";
    }

    /** {@code leave}: he is out of the fight and sent back. With nobody left inside, the fight then ends. */
    public String leave(Player p) {
        ArenaFight a = arenaHolding(p.getUniqueId());
        if (a == null) return "You are not in a fight.";
        a.site().removeEntrant(p.getUniqueId());
        adapters.log().info("FIGHTWORLD left player=" + p.getName() + " world=" + a.world().getName() + " why=leave");
        returnPlayer(p, "left");
        return "You left the fight.";
    }

    /** A death inside: he is out of the fight (his respawn sends him back: {@link #onPlayerRespawn}). */
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getPlayer();
        ArenaFight a = arenaHolding(p.getUniqueId());
        if (a == null || p.getWorld() != a.world()) return;
        a.site().removeEntrant(p.getUniqueId());
        adapters.log().info("FIGHTWORLD died-inside player=" + p.getName() + " world=" + a.world().getName());
    }

    /** A respawn with a return pending and no fight holding him: he respawns at his return location. */
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        if (arenaHolding(p.getUniqueId()) != null) return;
        Optional<FightReturn> back = profiles.fightReturn(p.getUniqueId());
        if (back.isEmpty()) return;
        Location to = locationOf(back.get());
        event.setRespawnLocation(to);
        profiles.clearFightReturn(p.getUniqueId());
        adapters.log().info("FIGHTWORLD returned player=" + p.getName() + " to=" + where(back.get()) + " why=died");
    }

    /** A quit inside: he is out of the fight; his return stays saved for his next login. */
    public void onQuit(Player p) {
        ArenaFight a = arenaHolding(p.getUniqueId());
        if (a == null) return;
        a.site().removeEntrant(p.getUniqueId());
        adapters.log().info("FIGHTWORLD left player=" + p.getName() + " world=" + a.world().getName()
                + " why=quit return=kept");
    }

    /**
     * THE STALE LOGIN (the brief's section 4 d), on the player's own thread once his profile has settled: a return
     * pending while no fight holds him means he quit inside, or the server stopped while he was inside. Wherever he has
     * spawned, he is sent to his return location and the return is cleared.
     *
     * <p>That covers both a fight world that is gone (UNVERIFIED where vanilla puts him then: the brief says the same x,
     * y, z of the overworld) and one still running, which he is out of either way.
     */
    public void onJoinSettled(Player p) {
        if (!p.isOnline() || arenaHolding(p.getUniqueId()) != null) return;
        Optional<FightReturn> back = profiles.fightReturn(p.getUniqueId());
        if (back.isEmpty()) return;
        Location at = p.getLocation();
        adapters.log().info(String.format(Locale.ROOT, "FIGHTWORLD stale login caught player=%s at=%s %.2f %.2f %.2f"
                + " to=%s", p.getName(), at.getWorld().getName(), at.getX(), at.getY(), at.getZ(), where(back.get())));
        returnPlayer(p, "login");
    }

    /** Sends him to his saved return location, clears it, and logs it. A return to a world that is gone goes to spawn. */
    private void returnPlayer(Player p, String why) {
        Optional<FightReturn> back = profiles.fightReturn(p.getUniqueId());
        Location to = back.map(this::locationOf).orElse(Bukkit.getWorlds().getFirst().getSpawnLocation());
        p.teleportAsync(to);
        profiles.clearFightReturn(p.getUniqueId());
        adapters.log().info("FIGHTWORLD returned player=" + p.getName() + " to="
                + back.map(FightFixtures::where).orElse("spawn (no return saved)") + " why=" + why);
    }

    private Location locationOf(FightReturn r) {
        World w = Bukkit.getWorld(r.worldName());
        if (w == null) return Bukkit.getWorlds().getFirst().getSpawnLocation();
        return new Location(w, r.x(), r.y(), r.z(), r.yaw(), r.pitch());
    }

    private static String where(FightReturn r) {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", r.worldName(), r.x(), r.y(), r.z());
    }

    /** The arena fight that runs, if any (the dev command runs one). */
    private ArenaFight runningArena() {
        for (ArenaFight a : arenas.values()) if (a.fight().running()) return a;
        return null;
    }

    /** The arena fight this player is an entrant of, if any. */
    private ArenaFight arenaHolding(UUID id) {
        for (ArenaFight a : arenas.values()) if (a.fight().running() && a.site().isEntrant(id)) return a;
        return null;
    }

    /** {@code worlds}: the FIGHTWORLDS line. */
    public String worldsLine(Player caller) {
        return worlds.worldsLine(caller);
    }

    /** Startup: the orphan sweep (the brief's section 4 f). */
    public void sweepOrphans() {
        worlds.sweepOrphans();
    }

    // --- every fight -------------------------------------------------------------------------------------------------

    /** A fight ended by any route: it is no longer held, and an arena's people go home and its world goes. */
    private void forget(FightInstance ended) {
        fights.remove(ended.id());
        if (current == ended) current = null;
        ArenaFight a = arenas.remove(ended.id());
        if (a != null) endArena(a, false);
    }

    /**
     * THE END OF AN ARENA FIGHT, BY ANY ROUTE (the brief's section 4 e). Every player still in its world goes home:
     * <ul>
     *   <li>an entrant to his return location;</li>
     *   <li>anyone else to the main world's spawn.</li>
     * </ul>
     * Then the world is unloaded and its folder deleted. {@code now} (plugin disable) teleports at once and tries the
     * unload and delete once, leaving any failure for the next startup's sweep.
     */
    private void endArena(ArenaFight a, boolean now) {
        for (Player p : new ArrayList<>(a.world().getPlayers())) {
            boolean entrant = a.site().removeEntrant(p.getUniqueId());
            if (!p.isOnline()) {
                // Quitting: his return must stay for his next login (onJoinSettled); clearing it here would lose it.
                continue;
            }
            if (p.isDead() && !now) {
                // His respawn sends him back (onPlayerRespawn); clearing his return here would send him to spawn.
                adapters.log().info("FIGHTWORLD end leaves player=" + p.getName() + " to his respawn");
                continue;
            }
            if (now) {
                Optional<FightReturn> back = profiles.fightReturn(p.getUniqueId());
                p.teleport(back.map(this::locationOf).orElse(Bukkit.getWorlds().getFirst().getSpawnLocation()));
                profiles.clearFightReturn(p.getUniqueId());
                adapters.log().info("FIGHTWORLD returned player=" + p.getName() + " why=end-disable");
            } else if (entrant || profiles.fightReturn(p.getUniqueId()).isPresent()) {
                returnPlayer(p, "end");
            } else {
                p.teleportAsync(Bukkit.getWorlds().getFirst().getSpawnLocation());
                adapters.log().info("FIGHTWORLD returned player=" + p.getName() + " to=spawn why=end-not-entrant");
            }
        }
        if (now) worlds.unloadAndDeleteNow(a.world(), a.folder());
        else worlds.unloadAndDelete(a.world(), a.folder());
    }

    public String land(double dxBlocks, double dzBlocks) {
        return running() ? current.land(dxBlocks, dzBlocks) : "No fight fixture is running.";
    }

    public String landRandom() {
        return running() ? current.landRandom() : "No fight fixture is running.";
    }

    public String play(double minDistanceBlocks, boolean safe) {
        return running() ? current.play(minDistanceBlocks, safe) : "No fight fixture is running.";
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

    /** Plugin disable or server stop: every fight ends cleanly; an arena's people go home at once. */
    public void onDisable() {
        for (ArenaFight a : new ArrayList<>(arenas.values())) {
            arenas.remove(a.fight().id());
            endArena(a, true);
        }
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (f.running()) f.stop("plugin-disable");
        }
    }
}
