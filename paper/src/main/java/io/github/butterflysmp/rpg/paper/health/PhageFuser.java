package io.github.butterflysmp.rpg.paper.health;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.github.butterflysmp.rpg.core.mob.MobBlast;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobFuse;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.ImplosionFooting;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * THE PHAGE'S OWN FUSE, WITH THE SWELL SHOWN BY PACKET (the seat's ruling 1 on #204, route (b);
 * PLAN-dragon-mobs.md, *PART C*). The rules are core's ({@link MobFuse}, the distances {@link MobBlast#triggerAt} and
 * {@link MobBlast#cancelAt} by stage); this is the Bukkit half.
 *
 * <h2>WHAT IT DOES</h2>
 * <ol>
 *   <li><b>At every appearance</b> of a creeper of ours whose file authors a blast ({@code EntityAddToWorldEvent}: a
 *       spawn or a chunk load; and the enable sweep), it REMOVES VANILLA'S SWELL GOAL ({@code MobGoals#removeGoal(mob,
 *       VanillaGoal.CREEPER_SWELL)}) and adds {@link Hold}, a goal at vanilla's priority 2 that claims {@code MOVE}
 *       while our fuse is lit: what {@code SwellGoal} did ({@code setFlags(EnumSet.of(Flag.MOVE))}, and
 *       {@code getNavigation().stop()} in its {@code start}; READ). <b>It must be at EVERY appearance</b>: a creeper
 *       read back from disk is constructed anew, and {@code Creeper#registerGoals} adds a fresh {@code SwellGoal}.</li>
 *   <li><b>Each tick</b>, on the Phage's own thread, it ticks the Phage's {@link MobFuse} with the distance to its
 *       target (feet to feet) when that target is a combatant it is hostile to ({@link MobHostility}) and it has AI,
 *       and {@code LivingEntity#hasLineOfSight}. A frozen (NoAI) Phage sees no target, so it never lights, and one
 *       frozen mid-fuse resets.</li>
 *   <li><b>A START or a RESET is SHOWN</b>: an entity-metadata packet setting the creeper's swell direction (index
 *       {@link #SWELL_DIR_INDEX}, type {@code INT}) to +1 or -1, sent to every player in {@code getTrackedBy()}, and one
 *       {@code SWELL} line. A player who starts tracking a lit Phage is sent +1 one tick later ({@link #onTrack}).</li>
 *   <li><b>A DETONATE</b> raises the prime ourselves ({@link #detonate}: {@code Creeper#explode()}), marked, so the
 *       one prime handler runs our blast; any prime NOT raised here is refused there, with its line.</li>
 * </ol>
 *
 * <h2>WHY THE SERVER'S OWN DIRECTION STAYS -1 AND IS NEVER RE-SENT</h2>
 * The server's {@code DATA_SWELL_DIR} starts at -1 ({@code Creeper#defineSynchedData}, READ) and only
 * {@code SwellGoal#tick} or an ignited {@code Creeper#tick} sets it (READ). The goal is removed at every appearance; the
 * ignite is refused ({@code CustomCreeperGuard}); this class never calls {@code setIgnited}, {@code ignite},
 * {@code setFuseTicks} or touches the swell. So the server never marks the value dirty, and never sends it after ours.
 * <b>What would break that, pinned by {@code PhageFuseWiringTest}:</b> a {@code SwellGoal} that survives (a load path
 * that skips {@link #onAppear}), a successful ignite, or any of those four calls in this class. And because the server's
 * direction is -1 and its default is -1, a NEW viewer's spawn packets carry nothing (non-default values only), which is
 * why {@link #onTrack} sends the current state itself.
 *
 * <h2>THE INDEX, READ, AND CHECKED AT STARTUP</h2>
 * {@code ClassTreeIdRegistry#define} numbers each class's synched data after its superclass's last (READ). Counted down
 * the chain in the pinned server jar ({@code javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar
 * net.minecraft.world.entity.<Class>}, each class's {@code static {}}): {@code Entity} 8 ({@code DATA_SHARED_FLAGS_ID}
 * to {@code DATA_TICKS_FROZEN}, 0-7), {@code LivingEntity} 7 (8-14), {@code Mob} 1 ({@code DATA_MOB_FLAGS_ID}, 15),
 * {@code PathfinderMob} and {@code Monster} none, then {@code Creeper.DATA_SWELL_DIR}, the first, an {@code INT}: 16.
 * The type: {@code EntityDataSerializers.INT}, registered second (READ), and PacketEvents 2.13.0's
 * {@code EntityDataTypes.INT}, whose id at V_26_1 is 1 in its own {@code entity_data_serializer} mapping (executed from
 * its pinned jar). {@link #selfCheck} logs the index, the type's id for the running server, and the server version, so
 * a protocol change shows in R0 and not in play.
 */
public final class PhageFuser {

    /** READ: the creeper's swell-direction synched-data index on the pinned 26.1.2 server (class javadoc). */
    public static final int SWELL_DIR_INDEX = 16;

    /** The server version the index was read for: PacketEvents' name for the pinned server's protocol (775). */
    static final String READ_FOR = "V_26_1";

    /** Vanilla's {@code SwellGoal} priority, READ ({@code Creeper#registerGoals}: {@code addGoal(2, new SwellGoal)}). */
    static final int HOLD_PRIORITY = 2;

    private final Supplier<MobRegistry> mobs;
    private final AdapterContext adapters;
    private final GoalKey<Creeper> holdKey;
    private final Map<UUID, Active> active = new ConcurrentHashMap<>();
    private final Set<UUID> raising = ConcurrentHashMap.newKeySet();

    private static final class Active {
        final Creeper creeper;
        final String mobId;
        final MobFuse fuse = new MobFuse();
        RepeatingTask task;
        Active(Creeper creeper, String mobId) {
            this.creeper = creeper;
            this.mobId = mobId;
        }
    }

    public PhageFuser(Plugin plugin, Supplier<MobRegistry> mobs, AdapterContext adapters) {
        this.mobs = mobs;
        this.adapters = adapters;
        this.holdKey = GoalKey.of(Creeper.class, new NamespacedKey(plugin, "phage_fuse_hold"));
    }

    /**
     * THE STARTUP SELF-CHECK: one line naming the index in use, PacketEvents' id for {@code INT} on the running
     * server, and that server's version; a WARN in its place when the version is not the one the index was read for, or
     * PacketEvents cannot answer. The gate's R0 reads it.
     */
    public String selfCheck() {
        try {
            ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
            int typeId = EntityDataTypes.INT.getId(version.toClientVersion());
            String line = "Phage swell packet: index=" + SWELL_DIR_INDEX + " type=int typeId=" + typeId + " server="
                    + version.name() + " readFor=" + READ_FOR;
            return version.name().startsWith(READ_FOR) ? line
                    : "UNVERIFIED " + line + ": the index was READ for " + READ_FOR + "; re-read it before trusting a swell";
        } catch (RuntimeException e) {
            return "UNVERIFIED Phage swell packet: PacketEvents could not answer (" + e + ")";
        }
    }

    /**
     * A living entity appeared (a spawn or a chunk load; the enable sweep). For a creeper of ours whose file authors a
     * blast: vanilla's swell goal goes, our hold goal comes, and its fuse loop starts. Idempotent. Must run on the
     * creeper's own thread.
     */
    public void onAppear(LivingEntity entity) {
        if (!(entity instanceof Creeper creeper)) return;
        String mobId = creeper.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        if (mobId == null) return;
        MobDefinition def = mobs.get().find(mobId).orElse(null);
        if (def == null || def.blast() == null) return;
        var goals = Bukkit.getMobGoals();
        goals.removeGoal(creeper, VanillaGoal.CREEPER_SWELL);
        if (!goals.hasGoal(creeper, holdKey)) goals.addGoal(creeper, HOLD_PRIORITY, new Hold(creeper));
        Active running = active.get(creeper.getUniqueId());
        if (running != null && running.task.isRunning()) return;
        Active a = new Active(creeper, mobId);
        a.task = RepeatingTask.start(new EntityTaskTarget(creeper, adapters.scheduler(), adapters.log()), 1,
                "phage-fuse", () -> step(a), () -> active.remove(a.creeper.getUniqueId(), a));
        active.put(creeper.getUniqueId(), a);
    }

    /** A living entity left the world (a death, a split, a removal, an unload): its fuse goes, nothing is left. */
    public void onRemove(UUID id) {
        Active a = active.remove(id);
        if (a != null) a.task.cancel();
    }

    /**
     * A player began tracking an entity. If it is a lit Phage, the player is shown +1 ONE TICK LATER, on the Phage's
     * thread: READ, {@code ChunkMap$TrackedEntity#updatePlayer} calls this event BEFORE {@code ServerEntity#addPairing}
     * sends the entity, so a packet sent inside the event would reach a client that has no such entity yet.
     */
    public void onTrack(Player viewer, Entity entity) {
        Active a = active.get(entity.getUniqueId());
        if (a == null || !a.fuse.fusing()) return;
        adapters.scheduler().onEntityLater(entity, () -> {
            if (!a.fuse.fusing() || !entity.getTrackedBy().contains(viewer)) return;
            send(viewer, a.creeper.getEntityId(), 1);
            log(a, 1, "track", 1, OptionalDouble.empty(), Double.NaN, Double.NaN);
        }, 1);
    }

    /** The prime handler's question: was this prime raised by our fuse (or the instrument's detonate), just now? */
    public boolean isRaising(UUID id) {
        return raising.contains(id);
    }

    /**
     * RAISE THE PRIME OURSELVES: {@code Creeper#explode()} calls {@code explodeCreeper()}, which fires
     * {@code ExplosionPrimeEvent} synchronously (READ, {@code CraftCreeper#explode}); the handler sees the mark and runs
     * our blast. The fuse's end and the selftest's DETONATE both come here, so there is ONE way a Phage blows.
     */
    public void detonate(Creeper creeper) {
        UUID id = creeper.getUniqueId();
        raising.add(id);
        try {
            creeper.explode();
        } finally {
            raising.remove(id);
        }
    }

    /**
     * THE INSTRUMENT'S LIGHT (2026-11-18; {@link MobFuse#light}): light this Phage's fuse now and hold it to the blast,
     * shown to every viewer as a START is, logged {@code SWELL ... reason=light} with no distance. It then blows by the
     * fuse's own end, the usual {@code FUSED} and {@code detonate}. A frozen Phage's fuse runs too: the loop does not ask
     * for its AI. Must run on the creeper's thread. False when it has no fuse of ours.
     */
    public boolean light(Creeper creeper) {
        Active a = active.get(creeper.getUniqueId());
        if (a == null) return false;
        if (a.fuse.light()) {
            creeper.getPathfinder().stopPathfinding();
            show(a, 1, "light", OptionalDouble.empty(), Double.NaN, Double.NaN);
        }
        return true;
    }

    /** The fuse of one live Phage, for the instrument: swell, direction and the fuse's ticks; empty when none. */
    public java.util.Optional<int[]> state(UUID id) {
        Active a = active.get(id);
        if (a == null) return java.util.Optional.empty();
        MobDefinition def = mobs.get().find(a.mobId).orElse(null);
        int max = def == null || def.blast() == null ? 0 : def.blast().fuseTicks();
        return java.util.Optional.of(new int[] {a.fuse.swell(), a.fuse.direction(), max});
    }

    private boolean step(Active a) {
        Creeper c = a.creeper;
        if (!c.isValid()) return false;
        MobDefinition def = mobs.get().find(a.mobId).orElse(null);
        if (def == null || def.blast() == null) return false;
        MobBlast blast = def.blast();
        int stage = MobBodyApplier.stageOf(c, def, adapters.keys(), adapters.log());
        double trigger = blast.triggerAt(stage, def.stages());
        double cancel = blast.cancelAt(stage, def.stages());
        OptionalDouble distance = OptionalDouble.empty();
        boolean sight = false;
        LivingEntity target = c.hasAI() ? c.getTarget() : null;
        if (target != null && LiveTargets.isLive(target) && target.getWorld().equals(c.getWorld())
                && MobHostility.hostileTo(io.github.butterflysmp.rpg.paper.adapter.CombatSides.of(target, adapters.keys()),
                        ImplosionFooting.exempt(target))) {
            distance = OptionalDouble.of(c.getLocation().distance(target.getLocation()));
            sight = c.hasLineOfSight(target);
        }
        switch (a.fuse.tick(distance, sight, trigger, cancel, blast.fuseTicks())) {
            case START -> {
                c.getPathfinder().stopPathfinding();   // stand still from this tick, as SwellGoal#start does
                show(a, 1, "start", distance, trigger, cancel);
            }
            case RESET -> show(a, -1, "reset", distance, trigger, cancel);
            case DETONATE -> {
                adapters.log().info(fusedLine(a.mobId, c.getUniqueId(), Bukkit.getCurrentTick(), a.fuse.swell(),
                        blast.fuseTicks()));
                detonate(c);
                return false;
            }
            case NONE -> { }
        }
        return true;
    }

    private void show(Active a, int direction, String reason, OptionalDouble d, double trigger, double cancel) {
        Set<Player> viewers = a.creeper.getTrackedBy();
        for (Player p : viewers) send(p, a.creeper.getEntityId(), direction);
        log(a, direction, reason, viewers.size(), d, trigger, cancel);
    }

    private static void send(Player viewer, int entityId, int direction) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, new WrapperPlayServerEntityMetadata(entityId,
                List.of(new EntityData<>(SWELL_DIR_INDEX, EntityDataTypes.INT, direction))));
    }

    private void log(Active a, int direction, String reason, int viewers, OptionalDouble d, double trigger,
                     double cancel) {
        adapters.log().info(line(a.mobId, a.creeper.getUniqueId(), direction, reason, Bukkit.getCurrentTick(), viewers,
                d, trigger, cancel, a.fuse.swell()));
    }

    /**
     * {@code SWELL mob=<id> id=<uuid> dir=<+1|-1> reason=<start|reset|track> tick=<t> viewers=<n> d=<feet to feet|->
     * trigger=<x|-> cancel=<y|-> swell=<n>}: one per packet send, to all its viewers at once.
     */
    static String line(String mobId, UUID id, int direction, String reason, int tick, int viewers, OptionalDouble d,
                       double trigger, double cancel, int swell) {
        return "SWELL mob=" + mobId + " id=" + id + " dir=" + (direction > 0 ? "+1" : "-1") + " reason=" + reason
                + " tick=" + tick + " viewers=" + viewers
                + " d=" + (d.isPresent() ? String.format(Locale.ROOT, "%.3f", d.getAsDouble()) : "-")
                + " trigger=" + (Double.isNaN(trigger) ? "-" : String.format(Locale.ROOT, "%.3f", trigger))
                + " cancel=" + (Double.isNaN(cancel) ? "-" : String.format(Locale.ROOT, "%.3f", cancel))
                + " swell=" + swell;
    }

    /** {@code FUSED mob=<id> id=<uuid> tick=<t> swell=<n>/<fuse>}: the fuse's end, the tick its blast is raised. */
    static String fusedLine(String mobId, UUID id, int tick, int swell, int fuseTicks) {
        return "FUSED mob=" + mobId + " id=" + id + " tick=" + tick + " swell=" + swell + "/" + fuseTicks;
    }

    /**
     * WHAT {@code SwellGoal} DID FOR THE BODY: while our fuse is lit, claim {@code MOVE} (so the lower-priority melee
     * and stroll goals cannot move it) and keep its path stopped.
     */
    private final class Hold implements Goal<Creeper> {
        private final Creeper creeper;

        Hold(Creeper creeper) {
            this.creeper = creeper;
        }

        @Override public boolean shouldActivate() {
            Active a = active.get(creeper.getUniqueId());
            return a != null && a.fuse.fusing();
        }

        @Override public void start() {
            creeper.getPathfinder().stopPathfinding();
        }

        @Override public void tick() {
            creeper.getPathfinder().stopPathfinding();
        }

        @Override public GoalKey<Creeper> getKey() {
            return holdKey;
        }

        @Override public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.MOVE);
        }
    }
}
