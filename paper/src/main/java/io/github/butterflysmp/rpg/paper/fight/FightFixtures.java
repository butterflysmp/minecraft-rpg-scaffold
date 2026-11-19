package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.CrystalRing;
import io.github.butterflysmp.rpg.core.fight.DamageGate;
import io.github.butterflysmp.rpg.core.fight.MageCycle;
import io.github.butterflysmp.rpg.core.fight.RandomLanding;
import io.github.butterflysmp.rpg.core.fight.RingFixture;
import io.github.butterflysmp.rpg.core.fight.StandableColumn;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Supplier;

/**
 * THE DEV FIXTURE FOR STEPS F4 TO F7 (PLAN-dragon-fight.md section 6; GATE-fight-ring.md, GATE-fight-mage.md): the
 * ring, the lightning, the Dark Mage and the crystals, in an ordinary world. No arena, no dragon, no waves. Dev only,
 * like {@code /rpg selftest}: its command refuses off a dev server.
 *
 * <p><b>One fixture at a time</b>, and its state lives in the {@link RingFixture} and the port this holds, never in a
 * static. It changes no blocks. The Mage and the crystals are NOT PERSISTED ({@code setPersistent(false)}; READ, see
 * {@code BukkitFightPort.lightCrystal}), so a stop or a crash leaves nothing in the world file; and a stop, a new start,
 * the end of every selftest run and plugin disable remove them.
 *
 * <p><b>The Mage</b> (step F6): spawned at the landing, on the highest standable block within {@link #STAND_BAND} of
 * C's y in that column, at gear score {@link #MAGE_GEAR_SCORE}; GUARDED by the damage gate while he lives, the rule
 * reading the cycle's {@code mageDamageable}. His real death feeds the cycle on that tick and is not a kill for the ring.
 * He is PINNED each tick to where he landed (x and z; gravity keeps y), because our own pushes set a velocity that the
 * knockback attribute does not stop.
 */
public final class FightFixtures {

    /** The fight file the fixture runs: {@code content/fights/dragon.yml}. */
    public static final String FIGHT_ID = "dragon";

    /** The Mage is spawned at this gear score, so his health is his file's {@code max_health} (Ben's 1500). */
    static final int MAGE_GEAR_SCORE = 100;

    /** The brief's: a standable block within this many blocks above or below C's y. */
    static final int STAND_BAND = 8;

    /** Air blocks above the floor the Mage needs (a wither skeleton is about 2.4 tall). */
    static final int STAND_CLEARANCE = 3;

    /** {@code land random} tries this many uniformly random spots before refusing for want of ground. */
    static final int RANDOM_TRIES = 20;

    private final AdapterContext adapters;
    private final Supplier<Optional<FightContent>> fight;
    private final Supplier<MobRegistry> mobs;
    private final Random random = new Random();

    private RingFixture fixture;
    private BukkitFightPort port;
    private RepeatingTask loop;
    private World world;
    private Location centre;
    private LivingEntity mage;
    private Location magePin;

    /**
     * THE PLAY ROW (GATE-fight-mage.md, MQ8): Ben fights the Mage with his own weapon. While on, every participant is
     * healed to full every {@link #KEEP_ALIVE_TICKS} through the store, the core fixture stops after any participant's
     * {@link #PLAY_STRIKE_LIMIT}-th consecutive strike, and the fixture ends itself at the Mage's death or a failure.
     */
    private boolean play;

    /** Heal every this many ticks in play (the brief's 10). */
    static final int KEEP_ALIVE_TICKS = 10;

    /** In play the fixture stops after a third consecutive strike: 20, 40, 80 before armour, each under a 100 maximum. */
    static final int PLAY_STRIKE_LIMIT = 3;

    /** {@code fight} and {@code mobs} are read at each start, so a content reload is picked up by the next fixture. */
    public FightFixtures(AdapterContext adapters, Supplier<Optional<FightContent>> fight, Supplier<MobRegistry> mobs) {
        this.adapters = adapters;
        this.fight = fight;
        this.mobs = mobs;
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
        centre = new Location(world, cx, groundY, cz);
        BukkitFightPort started = new BukkitFightPort(world, centre, f, adapters, adapters.log());
        List<Location> spots = new ArrayList<>();
        for (double[] o : CrystalRing.offsets(f.settings().crystalCount(), f.crystalRingRadiusBlocks(),
                f.crystalHeightBlocks())) {
            spots.add(new Location(world, cx + o[0], groundY + o[1], cz + o[2]));
        }
        started.bind(() -> mage, spots);
        port = started;
        RingFixture ring = new RingFixture(f.settings(), f.look(), cx, groundY, cz, started);
        fixture = ring;
        adapters.log().info(String.format(Locale.ROOT, "FIGHT start world=%s centre=%.1f %.1f %.1f by=%s",
                world.getName(), cx, groundY, cz, caller.getName()));
        play = false;
        loop = RepeatingTask.start(new RegionTarget(centre, ring), 1, "fight-fixture", () -> {
            ring.tick();
            pinMage();
            if (play) keepAlive(ring);
            if (ring.stopped()) {
                finish("the fixture stopped itself");
                return false;
            }
            if (play && ring.outcome() != MageCycle.Outcome.RUNNING) {
                adapters.log().info("FIGHT play ended t=" + ring.fightTick() + " reason=" + ring.outcome());
                probe();
                finish("play-" + ring.outcome());
                return false;
            }
            return true;
        }, () -> { });
        return String.format(Locale.ROOT, "Fight fixture started at %.1f %.1f %.1f (ring 48 about you).",
                cx, groundY, cz);
    }

    /** {@code land <dx> <dz>}: the Mage lands at C + (dx, dz), on the standable block there. */
    public String land(double dxBlocks, double dzBlocks) {
        if (!running()) return "No fight fixture is running.";
        OptionalInt floor = floorAt(centre.getX() + dxBlocks, centre.getZ() + dzBlocks);
        if (floor.isEmpty()) {
            adapters.log().info(String.format(Locale.ROOT, "FIGHT land t=%d dx=%.3f dz=%.3f result=REFUSED_NO_GROUND",
                    fixture.fightTick(), dxBlocks, dzBlocks));
            return String.format(Locale.ROOT, "Mage lands at C + (%.2f, %.2f): REFUSED_NO_GROUND (nothing to stand on"
                    + " within %d of C's y)", dxBlocks, dzBlocks, STAND_BAND);
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
        if (!running()) return "No fight fixture is running.";
        String landed = landRandom(minDistanceBlocks);
        if (mage == null) return landed;
        play = true;
        fixture.strikeLimit(PLAY_STRIKE_LIMIT);
        adapters.log().info("FIGHT play on t=" + fixture.fightTick() + " keepalive=" + KEEP_ALIVE_TICKS
                + " strike_limit=" + PLAY_STRIKE_LIMIT);
        return landed + " PLAY: fight him with your own weapon.";
    }

    private String landRandom(double minDistanceBlocks) {
        if (!running()) return "No fight fixture is running.";
        double max = fight.get().map(f -> f.settings().maxLandingDistanceBlocks()).orElse(0.0);
        for (int i = 0; i < RANDOM_TRIES; i++) {
            double[] p = RandomLanding.pick(max, random);
            if (Math.hypot(p[0], p[1]) < minDistanceBlocks) continue;
            if (floorAt(centre.getX() + p[0], centre.getZ() + p[1]).isPresent()) return land(p[0], p[1]);
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

    /** {@code probe}: one FIGHTPROBE line: the Mage, his health, the live crystals and where each beam points. */
    public String probe() {
        if (!running()) return "No fight fixture is running.";
        StringBuilder line = new StringBuilder("FIGHTPROBE t=" + fixture.fightTick());
        Location at = null;
        if (mage != null && mage.isValid()) {
            at = mage.getLocation();
            var id = mage.getUniqueId();
            boolean tracked = adapters.stats().tracks(id);
            line.append(String.format(Locale.ROOT,
                    " mage=%s at=%.2f %.2f %.2f glowing=%s ai=%s health=%.3f/%.3f defense=%.3f",
                    id, at.getX() - centre.getX(), at.getY() - centre.getY(), at.getZ() - centre.getZ(),
                    mage.isGlowing(), mage.hasAI(),
                    tracked ? adapters.stats().current(id) : -1.0,
                    tracked ? adapters.stats().max(id) : -1.0,
                    tracked ? adapters.stats().defenseValue(id) : -1.0));
        } else {
            line.append(" mage=none");
        }
        List<org.bukkit.entity.EnderCrystal> live = port.crystals();
        int onMage = 0;
        for (org.bukkit.entity.EnderCrystal c : live) {
            Location beam = c.getBeamTarget();
            if (at != null && beam != null && beam.getBlockX() == at.getBlockX() && beam.getBlockY() == at.getBlockY()
                    && beam.getBlockZ() == at.getBlockZ()) onMage++;
        }
        line.append(" crystals=").append(live.size()).append(" beamsOnMage=").append(onMage);
        adapters.log().info(line.toString());
        return line.toString();
    }

    /** Ends the running fixture, if any, removing the Mage and every crystal. Returns what it did. */
    public String stop(String why) {
        if (!running()) return "No fight fixture is running.";
        if (play) probe();
        fixture.stop();
        if (loop != null) loop.cancel();
        finish(why);
        return "Fight fixture stopped.";
    }

    /** The one cleanup every ending shares: the Mage and every crystal removed, the state forgotten. */
    private void finish(String why) {
        if (fixture == null) return;
        removeMage();
        if (port != null) port.clearCrystals();
        adapters.log().info("FIGHT ended reason=" + why);
        fixture = null;
        port = null;
        loop = null;
        world = null;
        centre = null;
        play = false;
    }

    /** Every participant healed to full through the store (the play row's keep-alive). */
    private void keepAlive(RingFixture ring) {
        if (ring.fightTick() % KEEP_ALIVE_TICKS != 0 || port == null) return;
        for (RingFixture.Participant p : port.participants()) {
            if (adapters.stats().tracks(p.id())) {
                adapters.stats().heal(p.id(), adapters.stats().max(p.id()), p.id(), true);
            }
        }
    }

    /**
     * A mob died. The fixture's own Mage: his death feeds the cycle on this tick, and is NOT a kill for the ring. Any
     * other mob a player killed while the fixture runs in that world: the kill is fed in at the killer's feet (the
     * seat's placeholder: any mob counts; PLAN-dragon-fight.md section 7 item 7 is open).
     */
    public void onDeath(EntityDeathEvent event) {
        if (!running()) return;
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
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

    /** Plugin disable or server stop: the fixture ends cleanly. */
    public void onDisable() {
        if (running()) stop("plugin-disable");
    }

    // --- the Mage ----------------------------------------------------------------------------------------------------

    private OptionalInt floorAt(double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        return StandableColumn.highest(centre.getBlockY(), STAND_BAND, STAND_CLEARANCE,
                y -> world.getBlockAt(bx, y, bz).isSolid());
    }

    /**
     * The Mage, made ours in the spawn's own pre-add step (as {@code /rpg spawn} does: MobSpawner.stampCustom, then the
     * gear score), tagged and marked in the same step, not persisted, and guarded at once.
     */
    private void spawnMage(double x, double y, double z) {
        removeMage();
        String mobId = fight.get().map(FightContent::mageMob).orElse(null);
        MobDefinition def = mobId == null ? null : mobs.get().find(mobId).orElse(null);
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
