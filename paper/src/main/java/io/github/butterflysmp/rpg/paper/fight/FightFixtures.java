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
        return enter(p, true);
    }

    /**
     * {@code enter} or {@code enter nodragon}. A NEW arena fight spawns the dragon unless {@code withDragon} is false
     * (step F9). {@code nodragon} exists so the F8b gate's arena rows, which predate the dragon, still read the fight
     * they were predicted against. Joining a fight that already runs ignores it.
     */
    public String enter(Player p, boolean withDragon) {
        Optional<FightContent> content = fight.get();
        if (content.isEmpty()) return "No fight '" + FIGHT_ID + "' is loaded (see the startup log).";
        if (FightWorlds.isFightWorld(p.getWorld())) return "You are already in a fight world.";
        if (profiles.profile(p.getUniqueId()).isEmpty()) {
            return "Your profile is not loaded yet, so your way back cannot be saved; try again in a moment.";
        }
        FightContent f = content.get();
        ArenaFight a = runningArena();
        if (a == null) {
            if (preparing != null) return "The fight's world is being prepared; try again in a moment.";
            World w = worlds.create(f.arena());
            if (w == null) return "The fight's world could not be created (see the log).";
            if (w.getEnvironment() == World.Environment.THE_END) {
                // F9c: an END world is checked before anything is built in it or anyone enters it (section 3 c).
                preparing = w;
                UUID who = p.getUniqueId();
                adapters.scheduler().onGlobalLater(() -> finishEndStart(who, f, w, withDragon), END_CHECK_DELAY_TICKS);
                return "Preparing an End world for the fight; you enter once vanilla's dragon fight is checked inert.";
            }
            a = startArena(p, f, w, withDragon);
        } else {
            a.site().addEntrant(p.getUniqueId());
        }
        return join(p, a, f);
    }

    /** F9c: ticks between an END world's creation and its self-check (READ: its fight ticks from the first tick). */
    static final int END_CHECK_DELAY_TICKS = 4;

    /** F9c: an END world made and not yet checked; no second arena is started meanwhile. */
    private World preparing;

    /**
     * F9c, section 3 c: THE END WORLD'S SELF-CHECK, then the fight. Inert: logged, and the arena is built and he
     * enters. NOT inert: a loud line, that world ended through the normal unload-and-delete, and a NORMAL world made
     * instead, so a fight never runs beside vanilla's. Gone or already fighting meanwhile: the world is ended unused.
     */
    private void finishEndStart(UUID who, FightContent f, World w, boolean withDragon) {
        preparing = null;
        io.github.butterflysmp.rpg.core.fight.EndFightCheck.Reading r = worlds.endSelfCheck(w, f.arena().shape());
        String line = io.github.butterflysmp.rpg.core.fight.EndFightCheck.line(w.getName(), r);
        World use = w;
        if (io.github.butterflysmp.rpg.core.fight.EndFightCheck.inert(r)) {
            adapters.log().info(line);
        } else {
            adapters.log().warning(line + " -- vanilla's fight is NOT inert: this world is ended and a NORMAL world made"
                    + " instead, so the fight never runs beside vanilla's");
            worlds.unloadAndDeleteNow(w, FightWorlds.folderOf(w));
            use = worlds.create(f.arena(), FightContent.Environment.NORMAL);
            if (use == null) return;
        }
        Player p = Bukkit.getPlayer(who);
        if (p == null || !p.isOnline() || runningArena() != null || FightWorlds.isFightWorld(p.getWorld())) {
            adapters.log().info("FIGHTWORLD prepared but unused name=" + use.getName()
                    + " (the player left or a fight began meanwhile)");
            worlds.unloadAndDeleteNow(use, FightWorlds.folderOf(use));
            return;
        }
        ArenaFight a = startArena(p, f, use, withDragon);
        p.sendMessage(net.kyori.adventure.text.Component.text(join(p, a, f),
                net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    /** Builds the arena in {@code w} and starts its fight with {@code p} its first entrant. */
    private ArenaFight startArena(Player p, FightContent f, World w, boolean withDragon) {
        worlds.build(w, f.arena().shape());
        ArenaSite site = new ArenaSite(w, f.arena().shape(), f.arena().allDownSeconds() * 20);
        site.addEntrant(p.getUniqueId());
        FightInstance started = new FightInstance(adapters, f, mobs.get(), site, p.getName(), withDragon,
                this::forget);
        ArenaFight a = new ArenaFight(started, site, w, FightWorlds.folderOf(w));
        fights.put(started.id(), started);
        arenas.put(started.id(), a);
        if (running()) current.stop("replaced by an arena fight");
        current = started;
        return a;
    }

    /** Saves his way back and sends him in. */
    private String join(Player p, ArenaFight a, FightContent f) {
        Location from = p.getLocation();
        // F9d d: the mode he entered in travels with his way back.
        FightReturn back = new FightReturn(from.getWorld().getName(), from.getX(), from.getY(), from.getZ(),
                from.getYaw(), from.getPitch(), p.getGameMode().name());
        if (!profiles.setFightReturn(p.getUniqueId(), back)) {
            a.site().removeEntrant(p.getUniqueId());
            return "Your way back could not be saved; not entering.";
        }
        Location entry = new Location(a.world(), a.site().centre().getX() + f.arena().entryDxBlocks(),
                a.site().centre().getY(), a.site().centre().getZ() + f.arena().entryDzBlocks());
        p.teleportAsync(entry);
        adapters.log().info(String.format(Locale.ROOT, "FIGHTWORLD entered player=%s world=%s return=%s mode=%s",
                p.getName(), a.world().getName(), where(back), back.entryMode()));
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

    /**
     * F9d a: A DEATH INSIDE MAKES HIM DOWN, NOT OUT. The death is CANCELLED (READ, {@code ServerPlayer.die}: a cancelled
     * {@code PlayerDeathEvent} restores his health to the event's revive health and returns before the death game event,
     * the drops and the death screen), so he keeps his inventory and has no screen to click through. On his own thread
     * a tick later he is put in SPECTATOR for the rest of the fight (until revived, step F9e), his effects and fire
     * cleared, and lifted above the floor if he died below it (READ: {@code out_of_world} bypasses a spectator's
     * invulnerability, so he would be hurt there). He stays an ENTRANT, so the fight returns him at its end, but is no
     * participant ({@code ArenaSite.takesPart}).
     *
     * @return true when the death was inside an arena fight (and refused); the caller then resets his combat state as
     *         a respawn would
     */
    public boolean onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getPlayer();
        ArenaFight a = arenaHolding(p.getUniqueId());
        if (a == null || p.getWorld() != a.world()) return false;
        event.setCancelled(true);
        org.bukkit.attribute.AttributeInstance max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        event.setReviveHealth(max == null ? 20.0 : max.getValue());
        if (!a.site().markDown(p.getUniqueId())) return true; // already down: the death is refused again, nothing more
        Location at = p.getLocation();
        org.bukkit.event.entity.EntityDamageEvent last = p.getLastDamageCause();
        adapters.log().info(String.format(Locale.ROOT, "FIGHT down player=%s t=%d cause=%s at=%.2f %.2f %.2f",
                p.getName(), a.fight().fightTick(), last == null ? "none" : last.getCause().name(),
                at.getX() - a.site().centre().getX(), at.getY() - a.site().centre().getY(),
                at.getZ() - a.site().centre().getZ()));
        adapters.scheduler().onEntity(p, () -> spectate(p, a));
        return true;
    }

    /** F9d a: the down player's view: spectator, no effects, no fire, above the floor. */
    private void spectate(Player p, ArenaFight a) {
        if (!p.isOnline() || p.getWorld() != a.world() || !a.site().isDown(p.getUniqueId())) return;
        p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        for (org.bukkit.potion.PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(e.getType());
        }
        p.setFireTicks(0);
        p.setFallDistance(0f);
        if (p.getLocation().getY() < a.site().centre().getY() - 1.0) {
            Location up = a.site().centre().add(0.0, DOWN_LIFT_BLOCKS, 0.0);
            p.teleport(up);
            adapters.log().info("FIGHT down lifted player=" + p.getName() + " (below the floor) to C +"
                    + DOWN_LIFT_BLOCKS);
        }
        adapters.log().info("FIGHT down spectating player=" + p.getName() + " mode=" + p.getGameMode());
    }

    /** F9d a: how far above C a player who went down below the floor is put (CC's choice, SEAT PLACEHOLDER). */
    static final double DOWN_LIFT_BLOCKS = 8.0;

    /** F9d: whether he is DOWN in an arena fight that runs (every damage on him is then refused). */
    public boolean isDown(Player p) {
        ArenaFight a = arenaHolding(p.getUniqueId());
        return a != null && a.site().isDown(p.getUniqueId());
    }

    /**
     * F9d e: A DOWN SPECTATOR WHO LEAVES THE FIGHT WORLD BY ANY MEANS (a spectator teleport, a command) is out of the
     * fight and is returned, in the mode he entered in. A living entrant's world change is not this rule's.
     */
    public void onWorldChange(Player p, World from) {
        for (ArenaFight a : arenas.values()) {
            if (a.world() != from || !a.fight().running() || !a.site().isDown(p.getUniqueId())) continue;
            a.site().removeEntrant(p.getUniqueId());
            adapters.log().info("FIGHTWORLD left player=" + p.getName() + " world=" + from.getName()
                    + " why=spectator-left to=" + p.getWorld().getName());
            returnPlayer(p, "spectator-left");
            return;
        }
    }

    /** A respawn with a return pending and no fight holding him: he respawns at his return location. */
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        if (arenaHolding(p.getUniqueId()) != null) return;
        Optional<FightReturn> back = profiles.fightReturn(p.getUniqueId());
        if (back.isEmpty()) return;
        Location to = locationOf(back.get());
        event.setRespawnLocation(to);
        restoreMode(p, back, "died");
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
        restoreMode(p, back, why);
        p.teleportAsync(to);
        profiles.clearFightReturn(p.getUniqueId());
        adapters.log().info("FIGHTWORLD returned player=" + p.getName() + " to="
                + back.map(FightFixtures::where).orElse("spawn (no return saved)") + " why=" + why);
    }

    /**
     * F9d d: HIS MODE ON THE WAY HOME. With a return saved, the mode he entered in ({@link FightReturn#restoreMode}:
     * never spectator, and survival when none was saved). With none saved, only a spectator is changed, to survival: he
     * must never be left in spectator in the normal world. Logged when it changes anything.
     */
    private void restoreMode(Player p, Optional<FightReturn> back, String why) {
        org.bukkit.GameMode want;
        if (back.isPresent()) want = org.bukkit.GameMode.valueOf(back.get().restoreMode());
        else if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) want = org.bukkit.GameMode.SURVIVAL;
        else return;
        org.bukkit.GameMode was = p.getGameMode();
        if (was == want) return;
        p.setGameMode(want);
        adapters.log().info("FIGHTWORLD mode player=" + p.getName() + " from=" + was + " to=" + want + " why=" + why);
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
        probeArena(a, now ? "end-disable" : "end");
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
                restoreMode(p, back, "end-disable"); // F9d d: a server stop sends him home in his entry mode too
                p.teleport(back.map(this::locationOf).orElse(Bukkit.getWorlds().getFirst().getSpawnLocation()));
                profiles.clearFightReturn(p.getUniqueId());
                adapters.log().info("FIGHTWORLD returned player=" + p.getName() + " why=end-disable");
            } else if (entrant || profiles.fightReturn(p.getUniqueId()).isPresent()) {
                returnPlayer(p, "end");
            } else {
                restoreMode(p, Optional.empty(), "end-not-entrant"); // F9d d: never left in spectator at home
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

    // --- the dragon (step F9) ------------------------------------------------------------------------------------------

    public String dragonHit(Player by, double amount) {
        return running() ? current.dragonHit(by, amount) : "No fight fixture is running.";
    }

    public String dragonStrike(Player by, String part) {
        return running() ? current.dragonStrike(by, part) : "No fight fixture is running.";
    }

    public String dragonPhase(String phase) {
        return running() ? current.dragonPhase(phase) : "No fight fixture is running.";
    }

    public String dragonTouch(Player p, String which) {
        return running() ? current.dragonTouch(p, which) : "No fight fixture is running.";
    }

    public String dragonMove(double dx, double dy, double dz) {
        return running() ? current.dragonMove(dx, dy, dz) : "No fight fixture is running.";
    }

    public String dragonToBreath(Player p) {
        return running() ? current.dragonToBreath(p) : "No fight fixture is running.";
    }

    public String dragonKill() {
        return running() ? current.dragonKill() : "No fight fixture is running.";
    }

    public String dragonProbe() {
        return running() ? current.dragonProbe() : "No fight fixture is running.";
    }

    /** Every write to the store: each fight logs the landed hits on its own Mage and dragon (step F9). */
    public void onHealthChange(io.github.butterflysmp.rpg.core.combat.stat.HealthChange change) {
        if (change.kind() == io.github.butterflysmp.rpg.core.combat.stat.HealthChange.Kind.DAMAGE && change.dealerIsPlayer()
                && change.dealer() != null && !fights.isEmpty()) {
            lastDealtTick.put(change.dealer(), Bukkit.getCurrentTick()); // F9b fix D: the cast's "dealt nothing" line
        }
        for (FightInstance f : new ArrayList<>(fights.values())) f.onHealthChange(change);
    }

    /** A dragon is about to shoot a fireball: cancelled if it is any fight's dragon (step F9); others are untouched. */
    public void onFireball(com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent event) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (f.onFireball(event)) return;
        }
    }

    /** A mob's melee hit on a player: priced if it is any fight's dragon (F9b, section 4); empty for any other mob. */
    public FightInstance.Contact dragonMelee(org.bukkit.entity.LivingEntity attacker, Player victim,
                                             double vanillaAmount) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            FightInstance.Contact c = f.dragonMelee(attacker, victim, vanillaAmount);
            if (c.ours()) return c;
        }
        return FightInstance.Contact.NOT_OURS;
    }

    /**
     * F9b fix B: whether {@code victim} still takes part in the fight whose dragon is {@code dragonId}, asked when the
     * dragon's priced hit is APPLIED. False once that fight has ended, or he has left it.
     */
    public boolean dragonVictimInside(UUID dragonId, Player victim) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (dragonId.equals(f.dragonId())) return f.running() && f.holds(victim);
        }
        return false;
    }

    /** F9b fix B: the refusal's line, for the fight whose dragon priced the hit (or a bare line, the fight gone). */
    public void dragonHitRefused(UUID dragonId, Player victim, String source, double amount) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (dragonId.equals(f.dragonId())) {
                f.refusedOutside(victim, source, amount);
                return;
            }
        }
        adapters.log().info(String.format(Locale.ROOT, "FIGHTDAMAGE refused player=%s source=%s amount=%.3f"
                + " why=fight-gone", victim.getName(), source, amount));
    }

    /** A cloud's hit on a player: priced if it is any fight's dragon's breath (F9b, section 4); empty otherwise. */
    public java.util.OptionalDouble dragonBreath(org.bukkit.entity.AreaEffectCloud cloud, Player victim,
                                                  double vanillaAmount) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            java.util.OptionalDouble hit = f.dragonBreath(cloud, victim, vanillaAmount);
            if (hit.isPresent()) return hit;
        }
        return java.util.OptionalDouble.empty();
    }

    /**
     * The name of the dragon's attack behind a HURT on {@code victim} dealt by {@code dealer}, or null when the dealer is
     * no fight's dragon (F9b, section 4: "HURT lines must name which attack"). Each name is given once, oldest first.
     */
    public String attackNameFor(UUID victim, UUID dealer) {
        if (dealer == null) return null;
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (dealer.equals(f.dragonId())) return f.takeAttackName(victim);
        }
        return null;
    }

    /** F9b fix D: the server tick of each player's last landed hit (any target), to say when a cast dealt nothing. */
    private final Map<UUID, Integer> lastDealtTick = new java.util.HashMap<>();

    /** Ticks a cast is given to land a hit before its line says it dealt nothing. */
    static final int CAST_DEALT_WITHIN_TICKS = 40;

    /**
     * F9b fix D: A WEAPON'S RIGHT-CLICK IN A FIGHT WORLD (the seat's brief: "my cursed emerald isn't working").
     * READ first, our code: nothing in weapons, mana or casting reads a world's environment or whether it is a fight
     * world (the only readers are {@code MobOrigin}, gear score, and the selftest's guard). So the line is how a
     * refusal or a silent cast is read: its result ({@code no-weapon} if the held item binds no right-click), the item
     * held, and, for a cast that went off, a second line if no hit of his landed within 40 ticks.
     */
    public void noteCast(Player p, String trigger, io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult result) {
        if (!FightWorlds.isFightWorld(p.getWorld())) return;
        String held = p.getInventory().getItemInMainHand().getType().name();
        String r = result == null ? "no-weapon" : result.getClass().getSimpleName();
        adapters.log().info(String.format(Locale.ROOT, "FIGHTCAST player=%s trigger=%s result=%s held=%s", p.getName(),
                trigger, r, held));
        if (!(result instanceof io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult.Success)) return;
        int castTick = Bukkit.getCurrentTick();
        UUID id = p.getUniqueId();
        adapters.scheduler().onGlobalLater(() -> {
            Integer last = lastDealtTick.get(id);
            if (last == null || last < castTick) {
                adapters.log().info(String.format(Locale.ROOT, "FIGHTCAST player=%s trigger=%s dealt=nothing within=%d"
                        + " held=%s", p.getName(), trigger, CAST_DEALT_WITHIN_TICKS, held));
            }
        }, CAST_DEALT_WITHIN_TICKS);
    }

    /** F9b fix D: a breath cloud applied; each fight logs it if the cloud is its dragon's. */
    public void onCloudApply(org.bukkit.entity.AreaEffectCloud cloud, java.util.List<org.bukkit.entity.LivingEntity> affected) {
        for (FightInstance f : new ArrayList<>(fights.values())) f.onCloudApply(cloud, affected);
    }

    /** F9c: the crystals whose fire was cancelled, so each is logged once (bounded). */
    private final java.util.Set<UUID> crystalFireLogged = new java.util.LinkedHashSet<>();

    /**
     * F9c, section 3 d: an End crystal's fire in a fight world was cancelled. READ: {@code EndCrystal.tick} sets fire on
     * its own block whenever its level has a dragon fight object (which an END world has, inert or not), through
     * {@code CraftEventFactory.callBlockIgniteEvent} with the crystal as the igniting entity. Logged once per crystal.
     */
    public void noteCrystalFire(UUID crystal, org.bukkit.block.Block at) {
        if (!crystalFireLogged.add(crystal)) return;
        if (crystalFireLogged.size() > 64) crystalFireLogged.remove(crystalFireLogged.iterator().next());
        adapters.log().info(String.format(Locale.ROOT, "FIGHT crystal-fire cancelled crystal=%s at=%d %d %d world=%s",
                crystal, at.getX(), at.getY(), at.getZ(), at.getWorld().getName()));
    }

    /**
     * {@code arena-probe} (dev; F9c): the running arena's world, its environment, the centre column's top block and its
     * bedrock (vanilla's exit portal is bedrock), fire anywhere a crystal could have set it, crystals and dragons.
     */
    public String arenaProbe() {
        ArenaFight a = runningArena();
        if (a == null) return "No arena fight is running.";
        return probeArena(a, "command");
    }

    /**
     * The arena's probe (F9c, section 3 e): its environment, its centre column, any fire, its crystals and dragons. Run by
     * the command, and (F9b fix D, the seat's brief of 2026-11-19) at EVERY arena fight's end, before anyone is moved.
     */
    private String probeArena(ArenaFight a, String when) {
        World w = a.world();
        io.github.butterflysmp.rpg.core.fight.ArenaShape s = a.site().shape();
        int bedrock = 0;
        for (int y = w.getMinHeight(); y < w.getMaxHeight(); y++) {
            if (w.getBlockAt(s.centreX(), y, s.centreZ()).getType() == org.bukkit.Material.BEDROCK) bedrock++;
        }
        org.bukkit.Material top = w.getBlockAt(s.centreX(), s.floorY(), s.centreZ()).getType();
        int fire = 0;
        int r = s.discRadius();
        int fromY = s.floorY() + 1;
        int toY = s.floorY() + s.pillarTopAboveFloor() + 3;
        for (int x = s.centreX() - r; x <= s.centreX() + r; x++) {
            for (int z = s.centreZ() - r; z <= s.centreZ() + r; z++) {
                for (int y = fromY; y <= toY; y++) {
                    org.bukkit.Material m = w.getBlockAt(x, y, z).getType();
                    if (m == org.bukkit.Material.FIRE || m == org.bukkit.Material.SOUL_FIRE) fire++;
                }
            }
        }
        int crystals = w.getEntitiesByClass(org.bukkit.entity.EnderCrystal.class).size();
        int dragons = w.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).size();
        String line = String.format(Locale.ROOT, "ARENAPROBE when=%s world=%s environment=%s centre_top=%s bedrock_in_column=%d"
                        + " fire=%d (y %d..%d) crystals=%d dragons=%d", when, w.getName(), w.getEnvironment(), top, bedrock,
                fire, fromY, toY, crystals, dragons);
        adapters.log().info(line);
        return line;
    }

    /** A vanilla hit: refused if it is a fight's dragon hitting that fight's own Mage (F9b, section 7). */
    public boolean refusesDragonOnMage(org.bukkit.entity.Entity damager, org.bukkit.entity.Entity victim) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (f.refusesDragonOnMage(damager, victim)) return true;
        }
        return false;
    }

    /** A crystal heal on a mob: refused if it is any fight's dragon (F9b, section 2); any other mob's is untouched. */
    public boolean refusesCrystalHeal(UUID healed, double vanillaAmount) {
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (f.refusesCrystalHeal(healed, vanillaAmount)) return true;
        }
        return false;
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
        if (preparing != null) {
            // F9c: an END world made and not yet checked has nobody in it: ended at once.
            worlds.unloadAndDeleteNow(preparing, FightWorlds.folderOf(preparing));
            preparing = null;
        }
        for (ArenaFight a : new ArrayList<>(arenas.values())) {
            arenas.remove(a.fight().id());
            endArena(a, true);
        }
        for (FightInstance f : new ArrayList<>(fights.values())) {
            if (f.running()) f.stop("plugin-disable");
        }
    }
}
