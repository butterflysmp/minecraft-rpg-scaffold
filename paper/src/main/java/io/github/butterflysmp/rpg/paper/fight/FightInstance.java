package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.DamageGate;
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

    private RepeatingTask loop;
    private LivingEntity mage;
    private Location magePin;
    private boolean play;
    private boolean finished;

    /**
     * Builds the fight at the site and starts its loop on C's region thread. {@code onFinished} is told once, when
     * the fight ends by any route, so whoever holds it can forget it.
     */
    FightInstance(AdapterContext adapters, FightContent fight, MobRegistry mobs, FightSite site, String startedBy,
                  Consumer<FightInstance> onFinished) {
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
        loop = RepeatingTask.start(new RegionTarget(centre, ring), 1, "fight-fixture", () -> {
            ring.tick();
            pinMage();
            if (play) keepAlive();
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
    }

    public UUID id() { return id; }

    public FightSite site() { return site; }

    public boolean running() {
        return !finished && !fixture.stopped();
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
        removeMage();
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
