package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.DamageGate;
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
        }
        return String.format(Locale.ROOT, "Mage lands at C + (%.2f, %.2f): %s", dxBlocks, dzBlocks, r);
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
            BukkitCombatant.of(player, adapters).handle().applyDamage(max * 10.0, p.id(),
                    io.github.butterflysmp.rpg.core.combat.CritState.NORMAL,
                    io.github.butterflysmp.rpg.core.combat.DefenseRule.BYPASSED, null);
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
        if (killer == null || killer.getWorld() != world) return;
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
                "FIGHTHIT t=%d target=%s amount=%.3f before_defense=%.3f health=%.3f/%.3f dealer=%s element=%s",
                fixture.fightTick(), target, change.amount(), change.beforeDefense(), change.newCurrent(), change.max(),
                change.dealer(), change.element()));
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
        RingFixture ring = fixture;
        adapters.gate().guard(dragon.getUniqueId(), new DamageGate.Rule() {
            @Override public boolean damageable() { return !ring.mageAlive(); }
            @Override public void refused(double amount, String source) { ring.dragonRefused(amount, source); }
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
                            p.getY() - centre.getY(), p.getZ() - centre.getZ()), fixture.mageAlive()));
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
