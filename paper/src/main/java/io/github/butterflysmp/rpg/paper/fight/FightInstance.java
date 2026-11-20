package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.ContactHitWindow;
import io.github.butterflysmp.rpg.core.fight.DamageGate;
import io.github.butterflysmp.rpg.core.fight.DragonAttacks;
import io.github.butterflysmp.rpg.core.fight.DragonCrossings;
import io.github.butterflysmp.rpg.core.fight.DragonParts;
import io.github.butterflysmp.rpg.core.fight.MageCycle;
import io.github.butterflysmp.rpg.core.fight.RandomLanding;
import io.github.butterflysmp.rpg.core.fight.RingFixture;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import io.github.butterflysmp.rpg.paper.health.MobSpawner;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ONE FIGHT (the F8 brief, section 3: one runtime, two sites). The ring, the lightning, the Dark Mage and the crystals,
 * at a {@link FightSite}. Every decision is the core {@link RingFixture}'s; this holds one fight's state and performs its
 * acts. <b>All of the fight's state lives here</b> (CLAUDE.md invariant 3; PLAN-dragon-mobs.md C4): two instances never
 * share anything but the plugin's services, so two fights can run at once.
 *
 * <p><b>The Mage</b> (step F6): spawned at the landing, on the floor the site allows in that column, at gear score
 * {@link #MAGE_GEAR_SCORE}. While he lives he is GUARDED by the damage gate, whose rule reads the cycle's
 * {@code mageDamageable}. His real death feeds the cycle on that tick and is not a kill for the ring. He is PINNED each
 * tick to where he landed (x and z; gravity keeps y), because our own pushes set a velocity that the knockback
 * attribute does not stop. He and the crystals are NOT PERSISTED ({@code setPersistent(false)}; READ, see
 * {@code BukkitFightPort.lightCrystal}).
 *
 * <p><b>The play row</b> (GATE-fight-mage.md, MQ7): while {@link #play} is on:
 * <ul>
 *   <li>every participant is healed to full every {@link #KEEP_ALIVE_TICKS} through the store;</li>
 *   <li>the core fight stops after any participant's {@link #PLAY_STRIKE_LIMIT}-th consecutive strike;</li>
 *   <li>the fight ends itself at the Mage's death or at a failure.</li>
 * </ul>
 */
public final class FightInstance {

    /** The Mage is spawned at this gear score, so his health is his file's {@code max_health}. */
    static final int MAGE_GEAR_SCORE = 100;

    /** {@code land random} tries this many uniformly random spots before refusing for want of ground. */
    static final int RANDOM_TRIES = 20;

    /** Heal every this many ticks in play (GATE-fight-mage.md's 10). */
    static final int KEEP_ALIVE_TICKS = 10;

    /** In play the fight stops after a third consecutive strike: 20, 40, 80 before armour, each under a 100 maximum. */
    static final int PLAY_STRIKE_LIMIT = 3;

    private final UUID id = UUID.randomUUID();
    private final AdapterContext adapters;
    private final FightContent fight;
    private final MobRegistry mobs;
    private final FightSite site;
    private final World world;
    private final Location centre;
    private final RingFixture fixture;
    private final BukkitFightPort port;
    private final Consumer<FightInstance> onFinished;
    private final Random random = new Random();

    /** The dragon is spawned this far above the arena's floor at its centre, blocks (CC's choice; recorded in the gate). */
    static final double DRAGON_SPAWN_HEIGHT_BLOCKS = 20.0;

    private RepeatingTask loop;
    private LivingEntity mage;
    private Location magePin;
    /** The arena fight's dragon (step F9); null in an ordinary-world fight, before its spawn and after its removal. */
    private org.bukkit.entity.EnderDragon dragon;
    /** Ben's thresholds, queued (core's {@link DragonCrossings}); null without a dragon. */
    private DragonCrossings crossings;
    private net.kyori.adventure.bossbar.BossBar bossBar;
    private final java.util.Set<UUID> barViewers = new java.util.HashSet<>();
    /** F9b, section 2: when a refused crystal heal on the dragon is logged (once per crystal lit). */
    private final io.github.butterflysmp.rpg.core.fight.CrystalHealLog healLog =
            new io.github.butterflysmp.rpg.core.fight.CrystalHealLog();
    /** F9b fix A: one contact hit (bite or wing) per player per window; made with the dragon. */
    private ContactHitWindow contacts;
    /** The dragon died: the fight is won, and ends {@code win_return_ticks} later. */
    private boolean won;
    private MageCycle.Phase lastPhase = MageCycle.Phase.IDLE;
    private boolean play;
    /** The play row's safety (heal, strike limit); off for the arena's play row, which has none (the brief's row 8). */
    private boolean safe = true;
    private boolean finished;
    private boolean poweredKilled;

    /**
     * Builds the fight at the site and starts its loop on C's region thread. {@code onFinished} is told once, when
     * the fight ends by any route, so whoever holds it can forget it.
     */
    FightInstance(AdapterContext adapters, FightContent fight, MobRegistry mobs, FightSite site, String startedBy,
                  Consumer<FightInstance> onFinished) {
        this(adapters, fight, mobs, site, startedBy, false, onFinished);
    }

    /** As above; {@code withDragon} spawns the fight's dragon at its start (step F9: the arena fight only). */
    FightInstance(AdapterContext adapters, FightContent fight, MobRegistry mobs, FightSite site, String startedBy,
                  boolean withDragon, Consumer<FightInstance> onFinished) {
        this.adapters = adapters;
        this.fight = fight;
        this.mobs = mobs;
        this.site = site;
        this.world = site.world();
        this.centre = site.centre();
        this.onFinished = onFinished;
        this.port = new BukkitFightPort(site, fight, adapters, adapters.log());
        this.port.bind(() -> mage);
        this.fixture = new RingFixture(fight.settings(), fight.look(), centre.getX(), centre.getY(), centre.getZ(),
                port);
        adapters.log().info(String.format(Locale.ROOT, "FIGHT start world=%s centre=%.1f %.1f %.1f by=%s site=%s",
                world.getName(), centre.getX(), centre.getY(), centre.getZ(), startedBy, site.kind()));
        RingFixture ring = fixture;
        if (withDragon) spawnDragon();
        loop = RepeatingTask.start(new RegionTarget(centre, ring), 1, "fight-fixture", () -> {
            if (won) return true; // the win's wait: the ring is stopped, the return is scheduled (onDragonDeath)
            ring.tick();
            notePhaseForMage(ring);
            pinMage();
            if (dragon != null) {
                trackBossBar();
                releaseQueuedMage(ring);
                healLog.crystalsNow(ring.crystalsLit());
                noteFireballs();
            }
            if (play && safe) keepAlive();
            if (site.failureKills()) {
                // FAILURE IS REAL HERE (the F8 brief, section 5 i). POWERED kills every participant through the store,
                // once; CLOSED leaves the lightning to do it; and the fight fails once nobody who entered is left in it
                // (every death, leave and quit takes one out).
                if (!poweredKilled && ring.outcome() == MageCycle.Outcome.FAILED_POWERED) {
                    poweredKilled = true;
                    killParticipants(ring);
                }
                if (site.nobodyInside()) {
                    adapters.log().info("FIGHT nobody inside t=" + ring.fightTick() + " outcome=" + ring.outcome());
                    ring.stop();
                    finish("nobody-inside-" + ring.outcome());
                    return false;
                }
                // F9d c: ALL DOWN. Nobody alive inside and at least one down: the fight has FAILED, and everyone goes
                // home all_down_seconds later (POWERED, which downs every participant, ends here too).
                io.github.butterflysmp.rpg.core.fight.DownRoster.Check down =
                        site.allDownCheck(ring.fightTick(), port.participants().size());
                if (down == io.github.butterflysmp.rpg.core.fight.DownRoster.Check.FAILED) {
                    adapters.log().info(String.format(Locale.ROOT, "FIGHT all-down t=%d down=%s outcome=%s returns_in=%d",
                            ring.fightTick(), names(site.downed()), ring.outcome(), fight.arena().allDownSeconds() * 20));
                    port.tell("Everyone is down. The fight has failed; you go home in "
                            + fight.arena().allDownSeconds() + " seconds.");
                } else if (down == io.github.butterflysmp.rpg.core.fight.DownRoster.Check.RETURN_DUE) {
                    adapters.log().info("FIGHT all-down return t=" + ring.fightTick() + " outcome=" + ring.outcome());
                    ring.stop();
                    finish("all-down-FAILED");
                    return false;
                }
            }
            if (ring.stopped()) {
                finish("the fixture stopped itself");
                return false;
            }
            // Where failure kills, a failure ends the fight through "nobody inside" instead: the kill lands a tick
            // later (applyDamage is deferred), and ending here would return the players before it, to die at home.
            if (play && !site.failureKills() && ring.outcome() != MageCycle.Outcome.RUNNING) {
                adapters.log().info("FIGHT play ended t=" + ring.fightTick() + " reason=" + ring.outcome());
                probe();
                finish("play-" + ring.outcome());
                return false;
            }
            return true;
        }, () -> { });
    }

    public UUID id() { return id; }

    public FightSite site() { return site; }

    /** The fight's own tick, as its lines print it. */
    public long fightTick() { return fixture.fightTick(); }

    /** Players' names (an id when he is offline), in the set's order, for a line. */
    private static String names(java.util.Set<UUID> ids) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (UUID id : ids) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            out.add(p == null ? id.toString() : p.getName());
        }
        return out.toString();
    }

    /** Running until it finishes. A won fight still runs through the win's wait, until its return (step F9). */
    public boolean running() {
        return !finished && (won || !fixture.stopped());
    }

    /** C, as the fight's lines measure from it. */
    Location centre() { return centre.clone(); }

    /** {@code land <dx> <dz>}: the Mage lands at C + (dx, dz), on the floor the site allows there. */
    public String land(double dxBlocks, double dzBlocks) {
        if (!running()) return "No fight fixture is running.";
        OptionalInt floor = site.mageFloorAt(centre.getX() + dxBlocks, centre.getZ() + dzBlocks);
        if (floor.isEmpty()) {
            adapters.log().info(String.format(Locale.ROOT, "FIGHT land t=%d dx=%.3f dz=%.3f result=REFUSED_NO_GROUND",
                    fixture.fightTick(), dxBlocks, dzBlocks));
            return String.format(Locale.ROOT, "Mage lands at C + (%.2f, %.2f): REFUSED_NO_GROUND (nowhere to stand"
                    + " there)", dxBlocks, dzBlocks);
        }
        MageCycle.LandingResult r = fixture.land(dxBlocks, dzBlocks);
        if (r == MageCycle.LandingResult.STARTED || r == MageCycle.LandingResult.HELD) {
            spawnMage(centre.getX() + dxBlocks, floor.getAsInt() + 1, centre.getZ() + dzBlocks);
            if (fight.mageLandingLightning()) landingLightning(dxBlocks, floor.getAsInt() + 1, dzBlocks);
        }
        return String.format(Locale.ROOT, "Mage lands at C + (%.2f, %.2f): %s", dxBlocks, dzBlocks, r);
    }

    /**
     * THE LANDING'S LIGHTNING (F9b, section 6; Ben: "Add a lightning strike to the mage landing"). An EFFECT ONLY:
     * {@code World.strikeLightningEffect}, which the API documents as doing no damage and lighting nothing (the SEAT
     * READING: effect only), at the spot he lands on, on the landing's own tick. Both sites; a content switch.
     */
    private void landingLightning(double dxBlocks, double yBlocks, double dzBlocks) {
        Location at = new Location(world, centre.getX() + dxBlocks, yBlocks, centre.getZ() + dzBlocks);
        org.bukkit.entity.LightningStrike bolt = world.strikeLightningEffect(at);
        adapters.log().info(String.format(Locale.ROOT, "FIGHT lightning t=%d at=%.2f %.2f %.2f effect=%s",
                fixture.fightTick(), dxBlocks, yBlocks - centre.getY(), dzBlocks, bolt != null && bolt.isEffect()));
    }

    /** {@code land random}: a uniformly random standable spot within the maximum landing distance. */
    public String landRandom() {
        return landRandom(0.0);
    }

    /**
     * {@code play <min>}: the play row. The Mage lands at a random standable spot at least {@code min} blocks from C;
     * then the keep-alive heal, the strike limit and the self-ending (see {@link #play}).
     */
    public String play(double minDistanceBlocks) {
        return play(minDistanceBlocks, true);
    }

    /**
     * {@code play <min> [unsafe]}. With {@code safe} false (the arena's play row: "no safety"), there is no heal and no
     * strike limit: a death is a death, and he is returned to where he entered.
     */
    public String play(double minDistanceBlocks, boolean safe) {
        if (!running()) return "No fight fixture is running.";
        String landed = landRandom(minDistanceBlocks);
        if (mage == null) return landed;
        play = true;
        this.safe = safe;
        if (safe) fixture.strikeLimit(PLAY_STRIKE_LIMIT);
        adapters.log().info("FIGHT play on t=" + fixture.fightTick() + (safe ? " keepalive=" + KEEP_ALIVE_TICKS
                + " strike_limit=" + PLAY_STRIKE_LIMIT : " unsafe"));
        return landed + " PLAY: fight him with your own weapon.";
    }

    /**
     * POWERED in a site where failure is real: every participant is killed through the store's one entry (the same
     * entry the lightning uses), his defence bypassed, by ten times his maximum. Each is logged.
     */
    private void killParticipants(RingFixture ring) {
        for (RingFixture.Participant p : port.participants()) {
            Player player = org.bukkit.Bukkit.getPlayer(p.id());
            if (player == null) continue;
            double max = adapters.stats().tracks(p.id()) ? adapters.stats().max(p.id()) : 20.0;
            adapters.log().info(String.format(Locale.ROOT, "FIGHT powered-kill t=%d player=%s amount=%.3f",
                    ring.fightTick(), p.name(), max * 10.0));
            // F9b fix B: applied only if he still takes part in this fight when the kill lands.
            BukkitCombatant.of(player, adapters).handle().applyDamageIf(() -> site.takesPart(player),
                    () -> refusedOutside(player, "powered-kill", max * 10.0), max * 10.0, p.id(),
                    io.github.butterflysmp.rpg.core.combat.CritState.NORMAL,
                    io.github.butterflysmp.rpg.core.combat.DefenseRule.BYPASSED);
        }
    }

    private String landRandom(double minDistanceBlocks) {
        if (!running()) return "No fight fixture is running.";
        double max = fight.settings().maxLandingDistanceBlocks();
        for (int i = 0; i < RANDOM_TRIES; i++) {
            double[] p = RandomLanding.pick(max, random);
            if (Math.hypot(p[0], p[1]) < minDistanceBlocks) continue;
            if (site.mageFloorAt(centre.getX() + p[0], centre.getZ() + p[1]).isPresent()) return land(p[0], p[1]);
        }
        adapters.log().info("FIGHT land t=" + fixture.fightTick() + " random result=REFUSED_NO_GROUND after "
                + RANDOM_TRIES + " tries");
        return "Mage lands at a random spot: REFUSED_NO_GROUND after " + RANDOM_TRIES + " tries.";
    }

    /** {@code mage-die}: the Mage is killed through the store (the gate lets this one hit through). */
    public String mageDie() {
        if (!running()) return "No fight fixture is running.";
        if (mage == null || !mage.isValid()) return "There is no Mage.";
        adapters.gate().allowNext(mage.getUniqueId());
        double max = adapters.stats().tracks(mage.getUniqueId()) ? adapters.stats().max(mage.getUniqueId()) : 1.0;
        BukkitCombatant.of(mage, adapters).handle().applyDamage(max * 10.0, mage.getUniqueId());
        return "The Mage is killed.";
    }

    /**
     * {@code probe}: one FIGHTPROBE line:
     * <ul>
     *   <li>the Mage and his health;</li>
     *   <li>the live crystals, and how many beams point at him;</li>
     *   <li>the WALL WITNESS: each row's first point as an arc along the ring from angle 0, blocks, lowest row first
     *       ({@link RingFixture#wallStartArcs}). Two probes a few ticks apart show which way each row turns.</li>
     * </ul>
     */
    public String probe() {
        if (!running()) return "No fight fixture is running.";
        StringBuilder line = new StringBuilder("FIGHTPROBE t=" + fixture.fightTick());
        Location at = null;
        if (mage != null && mage.isValid()) {
            at = mage.getLocation();
            UUID m = mage.getUniqueId();
            boolean tracked = adapters.stats().tracks(m);
            line.append(String.format(Locale.ROOT,
                    " mage=%s at=%.2f %.2f %.2f glowing=%s ai=%s health=%.3f/%.3f defense=%.3f",
                    m, at.getX() - centre.getX(), at.getY() - centre.getY(), at.getZ() - centre.getZ(),
                    mage.isGlowing(), mage.hasAI(),
                    tracked ? adapters.stats().current(m) : -1.0,
                    tracked ? adapters.stats().max(m) : -1.0,
                    tracked ? adapters.stats().defenseValue(m) : -1.0));
        } else {
            line.append(" mage=none");
        }
        List<EnderCrystal> live = port.crystals();
        int onMage = 0;
        for (EnderCrystal c : live) {
            Location beam = c.getBeamTarget();
            if (at != null && beam != null && beam.getBlockX() == at.getBlockX() && beam.getBlockY() == at.getBlockY()
                    && beam.getBlockZ() == at.getBlockZ()) onMage++;
        }
        line.append(" crystals=").append(live.size()).append(" beamsOnMage=").append(onMage);
        line.append(" wall=").append(fixture.wallStartArcs().stream()
                .map(a -> String.format(Locale.ROOT, "%.3f", a)).collect(Collectors.joining(",")));
        adapters.log().info(line.toString());
        return line.toString();
    }

    /** Ends the fight, removing the Mage and every crystal. Returns what it did. */
    public String stop(String why) {
        if (!running()) return "No fight fixture is running.";
        if (play) probe();
        fixture.stop();
        if (loop != null) loop.cancel();
        finish(why);
        return "Fight fixture stopped.";
    }

    /** The one cleanup every ending shares: the Mage and every crystal removed, the holder told. Runs once. */
    private void finish(String why) {
        if (finished) return;
        finished = true;
        logMageHealth("end");
        if (contacts != null) {
            // F9b fix A: an open window's suppressed count is told once, at the end, as every closed one was.
            for (UUID p : contacts.players()) {
                ContactHitWindow.Result w = contacts.flush(p);
                if (w.suppressedBefore() > 0) {
                    Player pl = org.bukkit.Bukkit.getPlayer(p);
                    logSuppressed(pl == null ? p.toString() : pl.getName(), w);
                }
            }
        }
        removeMage();
        removeDragon(why);
        port.clearCrystals();
        adapters.log().info("FIGHT ended reason=" + why);
        play = false;
        loop = null;
        onFinished.accept(this);
    }

    /** Every participant healed to full through the store (the play row's keep-alive). */
    private void keepAlive() {
        if (fixture.fightTick() % KEEP_ALIVE_TICKS != 0) return;
        for (RingFixture.Participant p : port.participants()) {
            if (adapters.stats().tracks(p.id())) {
                adapters.stats().heal(p.id(), adapters.stats().max(p.id()), p.id(), true);
            }
        }
    }

    /**
     * A mob died. This fight's own Mage: his death feeds the cycle on this tick, and is NOT a kill for the ring. Any
     * other mob a player killed in this fight's world: the kill is fed in at the killer's feet (the seat's
     * placeholder: any mob counts; PLAN-dragon-fight.md section 7 item 7 is open).
     */
    public void onDeath(EntityDeathEvent event) {
        if (!running()) return;
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
        if (dragon != null && dead.getUniqueId().equals(dragon.getUniqueId())) {
            onDragonDeath(event);
            return;
        }
        if (mage != null && dead.getUniqueId().equals(mage.getUniqueId())) {
            adapters.gate().release(mage.getUniqueId());
            mage = null;
            magePin = null;
            fixture.mageDie();
            if (play) {
                adapters.log().info("FIGHT play ended t=" + fixture.fightTick() + " reason=mage-died");
                fixture.stop();
                if (loop != null) loop.cancel();
                finish("play-mage-died");
            }
            return;
        }
        Player killer = dead.getKiller();
        // F9d b: a kill counts only for a participant (a down player's kills are impossible).
        if (killer == null || killer.getWorld() != world || !site.takesPart(killer)) return;
        Location feet = killer.getLocation();
        fixture.kill(killer.getUniqueId(), feet.getX(), feet.getZ());
    }

    // --- the landed hit, the Mage's health ----------------------------------------------------------------------------

    /**
     * EVERY HIT THAT LANDS on this fight's Mage or dragon (the F9 brief, section 1: BA3R2 could not say whether Ben
     * damaged the Mage). Fed from the store's one change seam, so every damage path is here without being named: one
     * {@code FIGHTHIT} line with the tick, what landed after defence, what it carried before, and the health after. A
     * landed hit on the dragon is also told to the crossing queue. A refused hit never reaches the store, so it is the
     * gate's line ({@code MAGEGATE} or {@code DRAGONGATE}), never this one.
     */
    public void onHealthChange(io.github.butterflysmp.rpg.core.combat.stat.HealthChange change) {
        if (finished || change.kind() != io.github.butterflysmp.rpg.core.combat.stat.HealthChange.Kind.DAMAGE) return;
        String target;
        if (dragon != null && change.target().equals(dragon.getUniqueId())) target = "dragon";
        else if (mage != null && change.target().equals(mage.getUniqueId())) target = "mage";
        else return;
        adapters.log().info(String.format(Locale.ROOT,
                "FIGHTHIT t=%d target=%s amount=%.3f before_defense=%.3f health=%.3f/%.3f dealer=%s element=%s%s",
                fixture.fightTick(), target, change.amount(), change.beforeDefense(), change.newCurrent(), change.max(),
                change.dealer(), change.element(),
                // F9b, section 7: the dragon's vanilla phase at the hit, appended so the Mage's line is unchanged.
                target.equals("dragon") ? " phase=" + dragon.getPhase() : ""));
        if (target.equals("dragon") && crossings != null && change.max() > 0.0) {
            for (double t : crossings.observe(change.newCurrent() / change.max())) {
                adapters.log().info(String.format(Locale.ROOT,
                        "DRAGONCROSS t=%d threshold=%.2f health=%.3f/%.3f pending=%d", fixture.fightTick(), t,
                        change.newCurrent(), change.max(), crossings.pending()));
            }
        }
    }

    /** The Mage's health at each phase change, on the loop tick that sees it, and at the fight's end. */
    private void notePhaseForMage(RingFixture ring) {
        MageCycle.Phase now = ring.phase();
        if (now == lastPhase) return;
        logMageHealth(lastPhase + "->" + now);
        lastPhase = now;
    }

    private void logMageHealth(String when) {
        StringBuilder line = new StringBuilder("FIGHT mage-health t=" + fixture.fightTick() + " at=" + when);
        if (mage != null && mage.isValid() && adapters.stats().tracks(mage.getUniqueId())) {
            line.append(String.format(Locale.ROOT, " health=%.3f/%.3f", adapters.stats().current(mage.getUniqueId()),
                    adapters.stats().max(mage.getUniqueId())));
        } else {
            line.append(" mage=none");
        }
        adapters.log().info(line.toString());
    }

    // --- the dragon (step F9) ------------------------------------------------------------------------------------------

    /**
     * The dragon, made ours in the spawn's own pre-add step as the Mage is: the mob file stamped, gear score 100 (so its
     * maximum is the file's {@code max_health}), tagged, NOT PERSISTED, its podium the arena's centre floor. Guarded at
     * once: while a Mage is alive every hit on it is refused. Its boss bar is made here and shown from the first tick.
     */
    private void spawnDragon() {
        FightContent.Dragon d = fight.dragon();
        MobDefinition def = mobs.find(d.mob()).orElse(null);
        if (def == null) {
            adapters.log().warning("FIGHT dragon file '" + d.mob() + "' is not loaded; no dragon spawned");
            return;
        }
        if (!"ender_dragon".equalsIgnoreCase(def.baseEntity())) {
            adapters.log().warning("FIGHT dragon file '" + d.mob() + "' is not an ender_dragon (" + def.baseEntity()
                    + "); no dragon spawned");
            return;
        }
        Location podium = new Location(world, centre.getX(), centre.getY(), centre.getZ());
        Location at = podium.clone().add(0.0, DRAGON_SPAWN_HEIGHT_BLOCKS, 0.0);
        dragon = world.spawn(at, org.bukkit.entity.EnderDragon.class, CreatureSpawnEvent.SpawnReason.CUSTOM, false, e -> {
            MobSpawner.stampCustom(e, def, 1, adapters.keys());
            e.getPersistentDataContainer().set(adapters.keys().mobGearScore, PersistentDataType.INTEGER,
                    MAGE_GEAR_SCORE);
            e.addScoreboardTag("fight_dragon");
            e.setPersistent(false);
            e.setRemoveWhenFarAway(false);
            e.setPodium(podium);
        });
        // IT MUST BE TOLD TO FLY. READ, the pinned server jar: EnderDragonPhaseManager's constructor sets HOVERING, and
        // EnderDragon.aiStep skips all movement while the phase is HOVERING; only vanilla's own fight moves a new dragon
        // on. Ours has none, so without this it hangs where it spawned for the whole fight. CIRCLING is vanilla's
        // holding pattern (Bukkit's name for HOLDING_PATTERN), from which it strafes, charges and lands by itself.
        dragon.setPhase(org.bukkit.entity.EnderDragon.Phase.CIRCLING);
        crossings = new DragonCrossings(d.mageThresholds());
        contacts = new ContactHitWindow(d.attacks().contactHitIntervalTicks());
        RingFixture ring = fixture;
        DragonCrossings queue = crossings;
        // F9b, section 1 (Ben: "No it can't be hurt while a mage is owed"): refused while a Mage is alive OR owed. The
        // gate asks before the hit, so the hit that crosses lands whole; core's one rule names the reason.
        adapters.gate().guard(dragon.getUniqueId(), new DamageGate.Rule() {
            @Override public boolean damageable() {
                return queue.refusal(ring.mageAlive()) == DragonCrossings.Refusal.NONE;
            }
            @Override public void refused(double amount, String source) {
                ring.dragonRefused(amount, source, queue.refusal(ring.mageAlive()).logName());
            }
        });
        bossBar = net.kyori.adventure.bossbar.BossBar.bossBar(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(def.displayName()), 1.0f,
                java.util.Objects.requireNonNull(net.kyori.adventure.bossbar.BossBar.Color.NAMES.value(d.bossBarColor())),
                net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS);
        Location p = dragon.getPodium();
        adapters.log().info(String.format(Locale.ROOT,
                "FIGHT dragon spawned t=%d id=%s at=%.2f %.2f %.2f podium=%s phase=" + dragon.getPhase()
                        + " mob=%s thresholds=%s",
                fixture.fightTick(), dragon.getUniqueId(), at.getX() - centre.getX(), at.getY() - centre.getY(),
                at.getZ() - centre.getZ(), p == null ? "none" : String.format(Locale.ROOT, "%.2f %.2f %.2f",
                        p.getX() - centre.getX(), p.getY() - centre.getY(), p.getZ() - centre.getZ()),
                d.mob(), d.mageThresholds()));
    }

    /**
     * The boss bar follows the participants each tick: shown to whoever is in the fight, hidden from whoever has left,
     * its progress the store's fraction (the custom health, not vanilla's). Each show and hide is logged once.
     */
    private void trackBossBar() {
        if (bossBar == null) return;
        UUID id = dragon.getUniqueId();
        if (adapters.stats().tracks(id) && adapters.stats().max(id) > 0.0) {
            float progress = (float) Math.max(0.0, Math.min(1.0, adapters.stats().current(id) / adapters.stats().max(id)));
            if (bossBar.progress() != progress) bossBar.progress(progress);
        }
        java.util.Set<UUID> now = new java.util.HashSet<>();
        for (RingFixture.Participant p : port.participants()) now.add(p.id());
        now.addAll(port.downWatchers()); // F9d b: a down player still sees the bar
        for (UUID v : now) {
            if (barViewers.add(v)) {
                Player pl = org.bukkit.Bukkit.getPlayer(v);
                if (pl != null) pl.showBossBar(bossBar);
                adapters.log().info("BOSSBAR shown t=" + fixture.fightTick() + " player=" + (pl == null ? v : pl.getName()));
            }
        }
        for (UUID v : new java.util.ArrayList<>(barViewers)) {
            if (now.contains(v)) continue;
            hideBar(v, "left");
        }
    }

    private void hideBar(UUID v, String why) {
        barViewers.remove(v);
        Player pl = org.bukkit.Bukkit.getPlayer(v);
        if (pl != null) pl.hideBossBar(bossBar);
        adapters.log().info("BOSSBAR hidden t=" + fixture.fightTick() + " player=" + (pl == null ? v : pl.getName())
                + " why=" + why);
    }

    /**
     * THE QUEUE'S RELEASE (the seat's mechanism ruling): a Mage owed by a crossing lands only when the cycle is IDLE, no
     * Mage alive and the ring home, at a random standable spot within the maximum landing distance. It is paid only if
     * it actually landed (the fast close began); a landing refused for want of ground stays owed.
     */
    private void releaseQueuedMage(RingFixture ring) {
        if (crossings == null || won || !dragon.isValid()) return;
        if (!crossings.due(ring.phase() == MageCycle.Phase.IDLE && !ring.mageAlive())) return;
        adapters.log().info("DRAGON mage-due t=" + ring.fightTick() + " pending=" + crossings.pending());
        landRandom(0.0);
        if (ring.phase() == MageCycle.Phase.FAST_CLOSE) {
            crossings.landed();
            adapters.log().info("DRAGON mage-landed t=" + ring.fightTick() + " pending=" + crossings.pending());
        }
    }

    /**
     * ITS DEATH IS THE WIN (the F9 brief, section 3 h). The participants are told at once; the ring stops (no strike, no
     * Mage, no crystal after it); the Mage, if one is alive, is removed; vanilla's drops and experience are suppressed
     * (SEAT PLACEHOLDER); and {@code win_return_ticks} later the arena's normal end: everyone returned, the world
     * unloaded, its folder deleted.
     */
    private void onDragonDeath(EntityDeathEvent event) {
        if (won) return;
        won = true;
        int dropped = event.getDroppedExp();
        int drops = event.getDrops().size();
        event.setDroppedExp(0);
        event.getDrops().clear();
        adapters.gate().release(dragon.getUniqueId());
        int after = fight.dragon().winReturnTicks();
        adapters.log().info(String.format(Locale.ROOT, "FIGHT win t=%d dragon=%s exp_suppressed=%d drops_suppressed=%d"
                + " return_in=%d", fixture.fightTick(), dragon.getUniqueId(), dropped, drops, after));
        port.tell("The Ender Dragon is dead. You win.");
        logMageHealth("win");
        removeMage();
        port.clearCrystals();
        fixture.stop();
        adapters.scheduler().onRegionLater(centre, () -> {
            adapters.log().info("FIGHT win-return t=" + fixture.fightTick() + " after=" + after);
            finish("win");
        }, Math.max(1, after));
    }

    /**
     * NO FIREBALL (the F9 brief, section 3 e; Ben: "Only the fireball"). The event is cancelled for THIS fight's dragon
     * only, and each cancel is logged. The perched breath is not a fireball and stays vanilla.
     *
     * @return whether the event was this fight's dragon's
     */
    public boolean onFireball(com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent event) {
        if (dragon == null || !event.getEntity().getUniqueId().equals(dragon.getUniqueId())) return false;
        event.setCancelled(true);
        adapters.log().info("FIGHT fireball-cancelled t=" + fixture.fightTick() + " dragon=" + dragon.getUniqueId()
                + " phase=" + dragon.getPhase());
        return true;
    }

    /**
     * NO CRYSTAL HEALS THE FIGHT'S DRAGON (F9b, section 2; Ben: "No crystals should not heal the dragon"). READ, the
     * pinned server jar: {@code EnderDragon.checkCrystals} raises {@code EntityRegainHealthEvent} with
     * {@code ENDER_CRYSTAL} every ten ticks near a crystal and calls {@code setHealth} only if it is not cancelled; our
     * {@code MobHealPolicy} would also reroute it into the store (100 a heal, read at the F9 boot). The listener cancels
     * the event when this says so, which stops BOTH. Only THIS fight's dragon: every other mob's heal is untouched.
     *
     * @return whether the heal was this fight's dragon's (and is to be refused)
     */
    public boolean refusesCrystalHeal(UUID healed, double vanillaAmount) {
        if (dragon == null || !healed.equals(dragon.getUniqueId())) return false;
        int lit = fixture.crystalsLit();
        if (healLog.shouldLog(lit)) {
            adapters.log().info(String.format(Locale.ROOT, "DRAGONHEAL t=%d refused reason=ENDER_CRYSTAL vanilla=%.3f"
                    + " crystals_lit=%d (once per crystal lit)", fixture.fightTick(), vanillaAmount, lit));
        }
        return true;
    }

    /** The dragon is gone with its fight, at every ending: the gate released, the boss bar hidden from all, removed. */
    private void removeDragon(String why) {
        if (bossBar != null) {
            for (UUID v : new java.util.ArrayList<>(barViewers)) hideBar(v, "fight-ended");
        }
        if (dragon == null) return;
        adapters.gate().release(dragon.getUniqueId());
        boolean was = dragon.isValid();
        if (was) dragon.remove();
        adapters.log().info("FIGHT dragon removed reason=" + why + " was_valid=" + was + " id=" + dragon.getUniqueId());
        dragon = null;
    }

    // --- the dragon's dev handles (the gate's AUTO rows) ---------------------------------------------------------------

    /** {@code dragon hit <amount>}: one store hit on the dragon from {@code by}, through the pipeline (defence applies). */
    public String dragonHit(Player by, double amount) {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        BukkitCombatant.of(dragon, adapters).handle().applyDamage(amount, by.getUniqueId(),
                io.github.butterflysmp.rpg.core.combat.CritState.NORMAL,
                io.github.butterflysmp.rpg.core.combat.DefenseRule.APPLIES, null);
        return String.format(Locale.ROOT, "Dragon hit for %.3f.", amount);
    }

    /** {@code dragon strike <part>}: {@code by} attacks the named part with the held weapon (the melee path). */
    public String dragonStrike(Player by, String partName) {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        org.bukkit.entity.ComplexEntityPart chosen = null;
        java.util.List<String> names = new java.util.ArrayList<>();
        for (org.bukkit.entity.ComplexEntityPart part : dragon.getParts()) {
            String n = partName(part);
            names.add(n);
            if (n.equalsIgnoreCase(partName)) chosen = part;
        }
        if (chosen == null) return "No part '" + partName + "'; the parts are " + names + ".";
        adapters.log().info("DRAGON strike t=" + fixture.fightTick() + " part=" + partName(chosen) + " part_id="
                + chosen.getUniqueId() + " by=" + by.getName());
        by.attack(chosen);
        return "Struck the dragon's " + partName(chosen) + ".";
    }

    // --- the dragon's damage (F9b, section 4) -------------------------------------------------------------------------

    /** The attack names priced on each player, oldest first, for the HURT line to name (bounded; gone with the fight). */
    private final java.util.Map<UUID, java.util.ArrayDeque<String>> attackNames = new java.util.HashMap<>();
    private static final int ATTACK_NAMES_KEPT = 8;

    /**
     * A MELEE HIT FROM THIS FIGHT'S DRAGON on a player (Ben: "Yes those numbers are fine to start with").
     * <ul>
     *   <li><b>ONE CONTACT HIT PER WINDOW (F9b fix A):</b> the bite and the wing together land at most once per
     *       {@code contact_hit_interval_ticks} for each player ({@link ContactHitWindow}). A hit inside the window is
     *       not priced and the caller CANCELS vanilla's event, so it does no vanilla damage either; the wing's push is
     *       not an event and stays. READ why it is needed: the head's and the neck's hurt are two calls a tick, and our
     *       token's 0.01 is what {@code lastHurt} then holds, so vanilla's own window passes every later hit.</li>
     *   <li><b>THE NAME FOLLOWS THE PART THAT DEALT IT (the addendum):</b> vanilla's own amount first (READ: 10 for the
     *       head and the neck, 5 for a wing, scaled by difficulty), then the boxes he is in only if it names neither.</li>
     *   <li>Its content number is scaled by the dragon's gear score as any mob's hit is ({@code MobScaling}).</li>
     * </ul>
     *
     * @return whether it was this fight's dragon's, and if so whether it lands and for how much
     */
    public Contact dragonMelee(LivingEntity attacker, Player victim, double vanillaAmount) {
        if (dragon == null || !attacker.getUniqueId().equals(dragon.getUniqueId())) return Contact.NOT_OURS;
        ContactHitWindow.Result w = contacts.hit(victim.getUniqueId(), fixture.fightTick());
        if (w.suppressedBefore() > 0) logSuppressed(victim.getName(), w);
        if (!w.lands()) return Contact.SUPPRESSED;
        DragonAttacks.Box head = null;
        DragonAttacks.Box neck = null;
        java.util.List<DragonAttacks.Box> wings = new java.util.ArrayList<>();
        for (org.bukkit.entity.ComplexEntityPart part : dragon.getParts()) {
            DragonAttacks.Box b = box(part.getBoundingBox());
            switch (partName(part)) {
                case "head" -> head = b;
                case "neck" -> neck = b;
                case "wing" -> wings.add(b);
                default -> { }
            }
        }
        DragonAttacks.Difficulty difficulty = DragonAttacks.Difficulty.valueOf(world.getDifficulty().name());
        java.util.Optional<DragonAttacks.Attack> read = head == null || neck == null
                ? DragonAttacks.byVanilla(vanillaAmount, difficulty)
                : DragonAttacks.name(vanillaAmount, difficulty, box(victim.getBoundingBox()), head, neck, wings);
        // A hit named by neither its amount nor a box (INFERRED rare) is priced as the lesser attack, and its line says
        // so, so a gate can count them.
        DragonAttacks.Attack attack = read.orElse(DragonAttacks.Attack.WING);
        return new Contact(true, true, priceAttack(attack, fight.dragon().attacks().base(attack), victim,
                read.isPresent() ? attack.logName() : "unknown->wing", vanillaAmount));
    }

    /**
     * What the fight says of a dragon's contact hit: not its dragon ({@link #NOT_OURS}); its dragon, inside the window
     * ({@link #SUPPRESSED}: cancel vanilla's event, deal nothing); or its dragon, landing for {@code amount}.
     */
    public record Contact(boolean ours, boolean lands, double amount) {
        public static final Contact NOT_OURS = new Contact(false, false, 0.0);
        public static final Contact SUPPRESSED = new Contact(true, false, 0.0);
    }

    /** One line per window that suppressed anything: how many, and the window. */
    private void logSuppressed(String player, ContactHitWindow.Result w) {
        adapters.log().info(String.format(Locale.ROOT, "DRAGONATTACK suppressed player=%s count=%d window=%d..%d",
                player, w.suppressedBefore(), w.windowFrom(), w.windowFrom() + contacts.intervalTicks() - 1));
    }

    /**
     * F9b fix B: the fight's damage lands only on a participant of THIS fight, in its world, at the moment it is
     * applied ({@code ArenaSite.takesPart}: an entrant, in this world, alive, in survival or adventure).
     */
    public boolean holds(Player victim) {
        return site.takesPart(victim);
    }

    /** The refusal's line: a fight's damage priced for him, not applied, because he was no longer in the fight. */
    void refusedOutside(Player victim, String source, double amount) {
        adapters.log().info(String.format(Locale.ROOT, "FIGHTDAMAGE refused t=%d player=%s source=%s amount=%.3f"
                + " why=not-in-this-fight", fixture.fightTick(), victim.getName(), source, amount));
    }

    /**
     * ONE HIT OF THE PERCHED BREATH from this fight's dragon. READ: the sitting dragon's cloud ({@code
     * DragonSittingFlamingPhase}) is owned by the dragon, radius 5, 200 ticks, Instant Damage; Bukkit raises its hit as
     * {@code ENTITY_ATTACK} with the CLOUD as the damager, which no rider took before (the cloud is not living, and
     * {@code VanillaDamagePolicy} passes {@code ENTITY_ATTACK}). Each hit costs the content's figure a second times the
     * cloud's interval over 20, so standing in it costs that a second. The cloud's size and life are not touched.
     *
     * @return the hit to deal, or empty when the cloud is not this fight's dragon's
     */
    public java.util.OptionalDouble dragonBreath(org.bukkit.entity.AreaEffectCloud cloud, Player victim,
                                                  double vanillaAmount) {
        if (dragon == null || !(cloud.getSource() instanceof org.bukkit.entity.EnderDragon source)
                || !source.getUniqueId().equals(dragon.getUniqueId())) {
            return java.util.OptionalDouble.empty();
        }
        double each = DragonAttacks.breathPerApplication(fight.dragon().attacks().breathPerSecond(),
                cloud.getReapplicationDelay());
        double priced = priceAttack(DragonAttacks.Attack.BREATH, each, victim, DragonAttacks.Attack.BREATH.logName(),
                vanillaAmount);
        // F9b fix F's witness: his server-side velocity at the hit and a tick later. INFERRED: a vanilla knockback sets
        // the server's velocity for him, so with the push removed the two should not show one. His eyes confirm it.
        org.bukkit.util.Vector at = victim.getVelocity();
        long hitTick = fixture.fightTick();
        adapters.scheduler().onEntityLater(victim, () -> {
            org.bukkit.util.Vector later = victim.getVelocity();
            adapters.log().info(String.format(Locale.ROOT, "DRAGONBREATH velocity t=%d player=%s at_hit=%.3f %.3f %.3f"
                            + " next_tick=%.3f %.3f %.3f", hitTick, victim.getName(), at.getX(), at.getY(), at.getZ(),
                    later.getX(), later.getY(), later.getZ()));
        }, 1);
        return java.util.OptionalDouble.of(priced);
    }

    /**
     * THE DRAGON DOES NOT HURT ITS OWN MAGE IN VANILLA (F9b, section 7). WHY the Mage's death message credited the
     * dragon at the F9 boot ("Withered Dark Mage was slain by Ender Dragon", while our store credited Ben): READ, the
     * pinned server jar, the dragon's wings and bite hurt EVERY living thing in their boxes ({@code EnderDragon.knockBack}
     * and {@code hurt}), our Mage included; that vanilla hit lands on his vanilla health, which our store ignores, and
     * vanilla's combat record keeps it. Our kill is {@code setKiller} then {@code setHealth(0)} ({@code MobDeathSystem}),
     * which raises no damage event, so the message is built from that record: the dragon's last hit. Ours to fix: the
     * event is cancelled for this fight's dragon on this fight's Mage. The wing's push is not an event and stays.
     *
     * @return whether it was (and is to be cancelled)
     */
    public boolean refusesDragonOnMage(org.bukkit.entity.Entity damager, org.bukkit.entity.Entity victim) {
        if (dragon == null || mage == null) return false;
        if (!damager.getUniqueId().equals(dragon.getUniqueId()) || !victim.getUniqueId().equals(mage.getUniqueId())) {
            return false;
        }
        adapters.log().info("DRAGONMAGE t=" + fixture.fightTick() + " cancelled (the dragon's vanilla hit on its Mage)");
        return true;
    }

    /**
     * THE FIREBALL PROBE IN THE PLAY ROWS (F9b, section 7): DB1 could not say whether a fireball entity ever existed.
     * Each loop tick counts dragon fireballs in the fight's world and logs the count whenever it changes.
     */
    private void noteFireballs() {
        int now = world.getEntitiesByClass(org.bukkit.entity.DragonFireball.class).size();
        if (now == lastFireballs) return;
        adapters.log().info("FIGHT fireballs t=" + fixture.fightTick() + " count=" + now);
        lastFireballs = now;
    }

    private int lastFireballs;

    /** The dragon's uuid, while it lives; the HURT line asks whose hit it was. */
    UUID dragonId() {
        return dragon == null ? null : dragon.getUniqueId();
    }

    /** The oldest attack name priced on this player and not yet named, or null. */
    String takeAttackName(UUID victim) {
        java.util.ArrayDeque<String> q = attackNames.get(victim);
        return q == null ? null : q.pollFirst();
    }

    private double priceAttack(DragonAttacks.Attack attack, double base, Player victim, String name,
                               double vanillaAmount) {
        Integer gs = dragon.getPersistentDataContainer().get(adapters.keys().mobGearScore, PersistentDataType.INTEGER);
        int score = gs == null ? MAGE_GEAR_SCORE : gs;
        double amount = io.github.butterflysmp.rpg.core.mob.MobScaling.attackDamage(base, true,
                dragon instanceof org.bukkit.entity.Enemy, score);
        java.util.ArrayDeque<String> q = attackNames.computeIfAbsent(victim.getUniqueId(), k -> new java.util.ArrayDeque<>());
        if (q.size() >= ATTACK_NAMES_KEPT) q.pollFirst();
        q.addLast(name);
        adapters.log().info(String.format(Locale.ROOT, "DRAGONATTACK t=%d attack=%s player=%s base=%.3f gs=%d"
                + " amount=%.3f vanilla=%.3f phase=%s", fixture.fightTick(), name, victim.getName(), base, score, amount,
                vanillaAmount, dragon.getPhase()));
        return amount;
    }

    /** A box as the log prints it, relative to nothing: min x y z .. max x y z, two places. */
    private static String boxText(DragonAttacks.Box b) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f..%.2f,%.2f,%.2f", b.minX(), b.minY(), b.minZ(), b.maxX(),
                b.maxY(), b.maxZ());
    }

    /**
     * F9b fix D: THE BREATH CLOUD APPLIED (READ: {@code AreaEffectCloud.serverTick} raises
     * {@code AreaEffectCloudApplyEvent} at each application, every 5 ticks of its age past its wait). For THIS fight's
     * dragon's cloud only: its age, whom it applied to, and every entrant in this world with his distance from it, so a
     * miss can be read.
     */
    public void onCloudApply(org.bukkit.entity.AreaEffectCloud cloud, java.util.List<LivingEntity> affected) {
        if (dragon == null || !(cloud.getSource() instanceof org.bukkit.entity.EnderDragon d)
                || !d.getUniqueId().equals(dragon.getUniqueId())) {
            return;
        }
        StringBuilder near = new StringBuilder();
        for (Player p : world.getPlayers()) {
            Location c = cloud.getLocation();
            Location at = p.getLocation();
            near.append(String.format(Locale.ROOT, "%s%s(xz=%.2f dy=%.2f)", near.length() == 0 ? "" : " ", p.getName(),
                    Math.hypot(at.getX() - c.getX(), at.getZ() - c.getZ()), at.getY() - c.getY()));
        }
        adapters.log().info(String.format(Locale.ROOT, "DRAGONBREATH apply t=%d cloud=%s age=%d radius=%.2f"
                        + " affected=%s players=[%s]", fixture.fightTick(), cloud.getUniqueId(), cloud.getTicksLived(),
                cloud.getRadius(), affected.stream().map(e -> e instanceof Player pl ? pl.getName() : e.getType().name())
                        .toList(), near));
    }

    private static DragonAttacks.Box box(org.bukkit.util.BoundingBox b) {
        return new DragonAttacks.Box(b.getMinX(), b.getMinY(), b.getMinZ(), b.getMaxX(), b.getMaxY(), b.getMaxZ());
    }

    /**
     * {@code dragon touch <head|wing>} (dev; F9b's staging of the dragon's attacks): puts the sender inside that part's
     * ATTACK box as the server builds it (READ: head inflated by 1; a wing inflated by (4, 2, 4) and moved down 2), so
     * the dragon's next tick bites or strikes him. The rows hold the dragon in HOVER, which is not a sitting phase, so
     * the wing hurts (READ: {@code knockBack} hurts only when the phase is not sitting).
     */
    public String dragonTouch(Player p, String which) {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        org.bukkit.entity.ComplexEntityPart chosen = null;
        for (org.bukkit.entity.ComplexEntityPart part : dragon.getParts()) {
            if (partName(part).equalsIgnoreCase(which)) {
                chosen = part;
                break;
            }
        }
        if (chosen == null || !("head".equalsIgnoreCase(which) || "wing".equalsIgnoreCase(which))) {
            return "Touch 'head' or 'wing'.";
        }
        org.bukkit.util.BoundingBox b = chosen.getBoundingBox();
        // Feet below the part's box: inside the head's box inflated by 1 (from minY - 1), or a wing's moved down 2 and
        // inflated by 2 (from minY - 4).
        double feetY = "head".equalsIgnoreCase(which) ? b.getMinY() - 0.5 : b.getMinY() - 3.0;
        Location to = new Location(world, b.getCenterX(), feetY, b.getCenterZ(), p.getLocation().getYaw(), 0.0f);
        p.teleport(to);
        // F9b fix D: his box and the part's ATTACK box as the server builds it (READ: head inflated by 1; a wing inflated
        // by (4, 2, 4) and moved down 2), and whether they overlap, so a missed staging can be read.
        DragonAttacks.Box attackBox = "head".equalsIgnoreCase(which)
                ? box(b).inflate(1.0, 1.0, 1.0)
                : box(b).inflate(4.0, 2.0, 4.0).down(2.0);
        DragonAttacks.Box his = box(p.getBoundingBox());
        adapters.log().info(String.format(Locale.ROOT, "DRAGON touch t=%d part=%s at=%.2f %.2f %.2f phase=%s box=%s"
                        + " attack_box=%s inside=%s", fixture.fightTick(), partName(chosen), to.getX() - centre.getX(),
                to.getY() - centre.getY(), to.getZ() - centre.getZ(), dragon.getPhase(), boxText(his),
                boxText(attackBox), his.intersects(attackBox)));
        return "In the dragon's " + partName(chosen) + ".";
    }

    /**
     * {@code dragon move <dx> <dy> <dz>} (dev; F9b's crystal row): moves the dragon to C + (dx, dy, dz). READ: vanilla
     * looks for a crystal within its box inflated by 32 ({@code EnderDragon.checkCrystals}); hovering over the centre, a
     * crystal on the 40-block pillar ring sits at the search's very edge, so the row moves the dragon beside one.
     */
    public String dragonMove(double dx, double dy, double dz) {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        Location to = new Location(world, centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
        boolean moved = dragon.teleport(to);
        adapters.log().info(String.format(Locale.ROOT, "DRAGON moved t=%d to=%.2f %.2f %.2f ok=%s phase=%s",
                fixture.fightTick(), dx, dy, dz, moved, dragon.getPhase()));
        return "Dragon moved: " + moved + ".";
    }

    /**
     * {@code dragon to-breath} (dev): puts the sender on this fight's dragon's breath cloud, if one exists. READ: the
     * cloud spawns 10 ticks into the sitting-flaming phase, on the floor below a point 5 blocks ahead of the head.
     */
    public String dragonToBreath(Player p) {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        for (org.bukkit.entity.AreaEffectCloud c : world.getEntitiesByClass(org.bukkit.entity.AreaEffectCloud.class)) {
            if (c.getSource() instanceof org.bukkit.entity.EnderDragon d && d.getUniqueId().equals(dragon.getUniqueId())) {
                Location at = c.getLocation();
                p.teleport(at.clone().add(0.0, 0.1, 0.0));
                adapters.log().info(String.format(Locale.ROOT, "DRAGON to-breath t=%d cloud=%s at=%.2f %.2f %.2f"
                                + " radius=%.2f interval=%d", fixture.fightTick(), c.getUniqueId(), at.getX() - centre.getX(),
                        at.getY() - centre.getY(), at.getZ() - centre.getZ(), c.getRadius(), c.getReapplicationDelay()));
                return "On the dragon's breath.";
            }
        }
        adapters.log().info("DRAGON to-breath t=" + fixture.fightTick() + " cloud=none phase=" + dragon.getPhase());
        return "The dragon has no breath cloud.";
    }

    /** A part's name, read from its box through the API alone ({@link DragonParts}); "part" when the box is no known size. */
    private static String partName(org.bukkit.entity.ComplexEntityPart part) {
        org.bukkit.util.BoundingBox box = part.getBoundingBox();
        return DragonParts.nameOf(box.getWidthX(), box.getHeight());
    }

    /** {@code dragon phase <name>}: sets the dragon's vanilla phase by the API (the fireball row's staging). */
    public String dragonPhase(String phase) {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        org.bukkit.entity.EnderDragon.Phase p;
        try {
            p = org.bukkit.entity.EnderDragon.Phase.valueOf(phase.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return "No phase '" + phase + "'; one of " + java.util.Arrays.toString(org.bukkit.entity.EnderDragon.Phase.values());
        }
        dragon.setPhase(p);
        adapters.log().info("DRAGON phase-set t=" + fixture.fightTick() + " phase=" + p + " now=" + dragon.getPhase());
        return "Dragon phase " + dragon.getPhase() + ".";
    }

    /** {@code dragon kill}: the dragon is killed through the store (the gate lets this one hit through). */
    public String dragonKill() {
        if (!running() || dragon == null || !dragon.isValid()) return "There is no dragon.";
        adapters.gate().allowNext(dragon.getUniqueId());
        double max = adapters.stats().tracks(dragon.getUniqueId()) ? adapters.stats().max(dragon.getUniqueId()) : 1.0;
        BukkitCombatant.of(dragon, adapters).handle().applyDamage(max * 10.0, dragon.getUniqueId(),
                io.github.butterflysmp.rpg.core.combat.CritState.NORMAL,
                io.github.butterflysmp.rpg.core.combat.DefenseRule.BYPASSED, null);
        return "The dragon is killed.";
    }

    /**
     * {@code dragon probe}: one DRAGONPROBE line, the dragon's store health, its vanilla phase, the podium, the boss
     * bar's colour, progress and viewers, the queue, and how many dragons and fireballs are in the fight's world.
     */
    public String dragonProbe() {
        StringBuilder line = new StringBuilder("DRAGONPROBE t=" + fixture.fightTick());
        int dragons = world.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).size();
        int fireballs = world.getEntitiesByClass(org.bukkit.entity.DragonFireball.class).size();
        if (dragon != null && dragon.isValid()) {
            UUID id = dragon.getUniqueId();
            boolean tracked = adapters.stats().tracks(id);
            Location p = dragon.getPodium();
            line.append(String.format(Locale.ROOT,
                    " dragon=%s health=%.3f/%.3f defense=%.3f phase=%s podium=%s immune=%s",
                    id, tracked ? adapters.stats().current(id) : -1.0, tracked ? adapters.stats().max(id) : -1.0,
                    tracked ? adapters.stats().defenseValue(id) : -1.0, dragon.getPhase(),
                    p == null ? "none" : String.format(Locale.ROOT, "%.2f %.2f %.2f", p.getX() - centre.getX(),
                            p.getY() - centre.getY(), p.getZ() - centre.getZ()),
                    crossings == null ? fixture.mageAlive()
                            : crossings.refusal(fixture.mageAlive()) != DragonCrossings.Refusal.NONE));
        } else {
            line.append(" dragon=none");
        }
        line.append(bossBar == null ? " bar=none" : String.format(Locale.ROOT, " bar=%s progress=%.4f viewers=%d",
                bossBar.color(), bossBar.progress(), barViewers.size()));
        line.append(crossings == null ? "" : " crossed=" + crossings.crossed() + " pending=" + crossings.pending());
        line.append(" dragons_in_world=").append(dragons).append(" fireballs_in_world=").append(fireballs);
        adapters.log().info(line.toString());
        return line.toString();
    }

    // --- the Mage ----------------------------------------------------------------------------------------------------

    /**
     * The Mage, made ours in the spawn's own pre-add step (as {@code /rpg spawn} does: MobSpawner.stampCustom, then the
     * gear score), tagged and marked in the same step, not persisted, and guarded at once.
     */
    private void spawnMage(double x, double y, double z) {
        removeMage();
        String mobId = fight.mageMob();
        MobDefinition def = mobs.find(mobId).orElse(null);
        if (def == null) {
            adapters.log().warning("FIGHT mage file '" + mobId + "' is not loaded; no Mage spawned");
            return;
        }
        EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(def.baseEntity().toLowerCase(Locale.ROOT)));
        if (type == null || !type.isAlive()) {
            adapters.log().warning("FIGHT mage file '" + mobId + "' names no living entity; no Mage spawned");
            return;
        }
        Location at = new Location(world, x, y, z);
        LivingEntity spawned = world.spawn(at, type.getEntityClass().asSubclass(LivingEntity.class),
                CreatureSpawnEvent.SpawnReason.CUSTOM, false, e -> {
                    MobSpawner.stampCustom(e, def, 1, adapters.keys());
                    e.getPersistentDataContainer().set(adapters.keys().mobGearScore, PersistentDataType.INTEGER,
                            MAGE_GEAR_SCORE);
                    e.getPersistentDataContainer().set(adapters.keys().fightMage, PersistentDataType.BYTE, (byte) 1);
                    e.addScoreboardTag("fight_mage");
                    e.setPersistent(false);
                    e.setRemoveWhenFarAway(false);
                });
        mage = spawned;
        magePin = at.clone();
        RingFixture ring = fixture;
        adapters.gate().guard(spawned.getUniqueId(), new DamageGate.Rule() {
            @Override public boolean damageable() { return ring.mageDamageable(); }
            @Override public void refused(double amount, String source) { ring.refused(amount, source); }
        });
        adapters.log().info(String.format(Locale.ROOT, "FIGHT mage spawned t=%d id=%s at=%.2f %.2f %.2f",
                fixture.fightTick(), spawned.getUniqueId(), x - centre.getX(), y - centre.getY(), z - centre.getZ()));
    }

    /** Holds the Mage at the x and z he landed on; gravity keeps y. A push leaves him where he was. */
    private void pinMage() {
        if (mage == null || !mage.isValid() || magePin == null) return;
        Location now = mage.getLocation();
        if (Math.abs(now.getX() - magePin.getX()) > 1e-3 || Math.abs(now.getZ() - magePin.getZ()) > 1e-3) {
            Location back = now.clone();
            back.setX(magePin.getX());
            back.setZ(magePin.getZ());
            mage.teleport(back);
        }
        Vector v = mage.getVelocity();
        if (v.getX() != 0.0 || v.getZ() != 0.0) mage.setVelocity(new Vector(0.0, v.getY(), 0.0));
    }

    private void removeMage() {
        if (mage == null) return;
        adapters.gate().release(mage.getUniqueId());
        if (mage.isValid()) mage.remove();
        mage = null;
        magePin = null;
    }

    /** The loop runs on C's region thread, and only while this fight runs. */
    private final class RegionTarget implements RepeatingTaskTarget {
        private final Location centre;
        private final RingFixture owner;

        RegionTarget(Location centre, RingFixture owner) {
            this.centre = centre;
            this.owner = owner;
        }

        @Override public boolean isActive() {
            return !finished && !owner.stopped();
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
