package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamBox;
import io.github.butterflysmp.rpg.core.mob.BeamCycle;
import io.github.butterflysmp.rpg.core.mob.BeamHit;
import io.github.butterflysmp.rpg.core.mob.BeamSound;
import io.github.butterflysmp.rpg.core.mob.BeamLine;
import io.github.butterflysmp.rpg.core.mob.MobBeam;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobGearScore;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * THE KNELL'S BEAM, the Bukkit half (PLAN-dragon-mobs.md step R5). The rules are core's: the clock
 * ({@link BeamCycle}), the numbers and the fire's decision ({@link MobBeam#resolve}), the line ({@link BeamLine}) and
 * the predicate ({@link MobHostility}). This class finds the target, supplies positions and bodies, applies the result
 * and draws it.
 *
 * <h2>ONE LOOP PER BEAM MOB, AND IT DIES WITH THE MOB (the brief's ruling 6)</h2>
 * <ul>
 *   <li><b>Started</b> at {@code EntityAddToWorldEvent} ({@link #onAppear}: a spawn, or the mob's chunk loading),
 *       for a mob whose {@code mob_id} names a file with a {@code beam:} block. A second add while one runs starts
 *       nothing.</li>
 *   <li><b>A {@link RepeatingTask} of period 1 on the mob's own entity scheduler</b> ({@link EntityTaskTarget}). Its
 *       {@code isActive} is {@code isValid() && !isDead()}, checked BEFORE every body: <b>the tick a Knell is dead,
 *       removed or unloaded, the loop stops and touches nothing</b>. Paper's entity scheduler also retires a removed
 *       entity's tasks (the {@code retired} callback is null: "if the entity is gone before the delay, do
 *       nothing").</li>
 *   <li><b>Cancelled</b> at {@code EntityRemoveFromWorldEvent} ({@link #onRemove}: a death, a despawn, a removal, a
 *       chunk unload), and dropped from the map in {@code onStop}, which runs on EVERY ending, a throw included.</li>
 *   <li><b>The root's modifier cannot outlive the telegraph:</b> it is TRANSIENT ({@code addTransientModifier}, never
 *       saved with the entity), removed at the fire, and removed in {@code onStop} when the mob is still valid (a
 *       cancel). A removed mob took its attributes with it.</li>
 *   <li><b>A reload re-reads nothing on a running loop:</b> the {@link MobBeam} is captured at the start. A chunk that
 *       unloads and loads again starts a fresh loop with a fresh wait (I3).</li>
 * </ul>
 *
 * <h2>THE TICK ORDER, AND WHY A FORCE ONLY MARKS THE ATTEMPT DUE</h2>
 * Every step of the clock runs inside the loop's own body, one per tick, so the fire is exactly
 * {@code telegraph_ticks} loop steps after the lock. The instrument's {@link #force} runs on the mob's thread too, but
 * only marks the next step's attempt due and FORCED; it never runs the attempt itself, because its order against the
 * loop's step within one tick is the scheduler's, not ours.
 *
 * <h2>THE E5 SEAM</h2>
 * {@link #addListener}: told of every HIT ({@link BeamHit}), on the Knell's thread, after the damage, and of nothing
 * else. Nothing listens in step R5; step R6's teleport will.
 */
public final class MobBeamer {

    /** Told of each hit. Step R6's teleport is the first; none in step R5. */
    public interface Listener {
        /** The hit, on the beam mob's own thread, after the damage: the mob, its locked target, and the report. */
        void onBeamHit(Mob mob, LivingEntity target, BeamHit hit);

        /** The beam mob left the world (death, removal, unload): anything the listener holds for it must end. */
        default void onRemove(UUID id) { }
    }

    /** The warning line: cfde822's purple dust (RGB 95,35,165, size 0.8). READ from the plan's section 3.1. */
    private static final Particle.DustOptions WARNING = new Particle.DustOptions(Color.fromRGB(95, 35, 165), 0.8f);
    /** The beam: cfde822's cyan dust (RGB 40,230,235, size 1.4). READ from the plan's section 3.1. */
    private static final Particle.DustOptions BEAM = new Particle.DustOptions(Color.fromRGB(40, 230, 235), 1.4f);
    /** One dust particle every this many BLOCKS along a drawn line. NOT IN THE RECORD: chosen (I10). */
    private static final double PARTICLE_STEP = 0.5;

    private static final class Locked {
        final BeamLine line;          // the full-range line, fixed at the lock
        final LivingEntity target;
        final String targetKind;
        Locked(BeamLine line, LivingEntity target, String targetKind) {
            this.line = line;
            this.target = target;
            this.targetKind = targetKind;
        }
    }

    private static final class Active {
        final Mob mob;
        final String mobId;
        final MobBeam beam;
        final BeamCycle cycle;
        RepeatingTask task;
        Locked lock;                  // non-null exactly during a telegraph
        boolean forced;               // the instrument marked the next attempt
        Active(Mob mob, String mobId, MobBeam beam, BeamCycle cycle) {
            this.mob = mob;
            this.mobId = mobId;
            this.beam = beam;
            this.cycle = cycle;
        }
    }

    private final Map<UUID, Active> active = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Supplier<MobRegistry> mobs;
    private final AdapterContext adapters;
    private final BeamCycle.Draw draw;

    /**
     * @param mobs read at each mob's appearance, so a reload's registry is the one a newly appearing Knell uses
     * @param draw the cycle's randomness: production passes {@code BeamCycle.uniform(...)}
     */
    public MobBeamer(Supplier<MobRegistry> mobs, AdapterContext adapters, BeamCycle.Draw draw) {
        this.mobs = mobs;
        this.adapters = adapters;
        this.draw = draw;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /** A living entity entered the world. Starts its loop if it is one of ours with a beam. On its own thread. */
    public void onAppear(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        String mobId = mob.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        if (mobId == null) return;
        MobDefinition def = mobs.get().find(mobId).orElse(null);
        if (def == null || def.beam() == null) return;
        Active running = active.get(mob.getUniqueId());
        if (running != null && running.task.isRunning()) return;
        Active a = new Active(mob, mobId, def.beam(), new BeamCycle(def.beam(), draw));
        a.task = RepeatingTask.start(new EntityTaskTarget(mob, adapters.scheduler(), adapters.log()), 1, "knell-beam",
                () -> {
                    step(a);
                    return true;
                },
                () -> {
                    if (a.lock != null) {
                        adapters.log().info(MobBeamLines.cancelled(a.mobId, a.mob.getUniqueId(), a.cycle.elapsed(),
                                a.beam.telegraphTicks()));
                        a.lock = null;
                    }
                    if (a.mob.isValid()) unroot(a.mob);
                    active.remove(a.mob.getUniqueId(), a);
                });
        active.put(mob.getUniqueId(), a);
    }

    /** A living entity left the world (death, despawn, removal, unload). Its loop stops; nothing fires. */
    public void onRemove(UUID id) {
        Active a = active.get(id);
        if (a != null) a.task.cancel();
        for (Listener l : listeners) l.onRemove(id);
    }

    /**
     * THE INSTRUMENT (the brief's ruling 8): make this beam mob's next attempt due NOW and FORCED, at whoever its
     * normal choice finds. Must run on the mob's own thread. Empty when it took; otherwise why not.
     */
    public Optional<String> force(LivingEntity entity) {
        Active a = active.get(entity.getUniqueId());
        if (a == null || !a.task.isRunning()) return Optional.of("no beam loop");
        if (!a.cycle.forceDue()) return Optional.of("busy: telegraph " + a.cycle.elapsed() + "/" + a.beam.telegraphTicks());
        a.forced = true;
        return Optional.empty();
    }

    // --- the loop ------------------------------------------------------------------------------------------------

    private void step(Active a) {
        switch (a.cycle.tick()) {
            case WAIT -> { }
            case ATTEMPT -> attempt(a);
            case ROOT -> {
                a.mob.getPathfinder().stopPathfinding();
                if (a.cycle.flashing()) drawLine(a.mob.getWorld(), a.lock.line, WARNING);
            }
            case FIRE -> {
                unroot(a.mob);
                fire(a);
            }
        }
    }

    private void attempt(Active a) {
        boolean forced = a.forced;
        a.forced = false;
        Mob mob = a.mob;
        // I23: a mob with no AI ("frozen") never begins a beam of its own, as a vanilla NoAI mob attacks nothing. The
        // instrument's forced beam still runs on one.
        if (!forced && !mob.hasAI()) {
            a.cycle.noTarget();
            return;
        }
        Vec3 eye = vec(mob.getEyeLocation());
        LivingEntity target = choose(mob, a.beam, eye);
        if (target == null) {
            a.cycle.noTarget();
            if (forced) adapters.log().info(MobBeamLines.none(a.mobId, mob.getUniqueId(), Bukkit.getCurrentTick(),
                    a.beam.range()));
            return;
        }
        Vec3 aim = a.beam.aimPoint(vec(target.getLocation()), target.getHeight());
        BeamLine line = BeamLine.aimed(eye, aim, a.beam.range());
        a.cycle.begin();
        a.lock = new Locked(line, target, kind(target));
        root(mob);
        if (a.cycle.flashing()) drawLine(mob.getWorld(), line, WARNING);
        // The telegraph's sounds, from content (the follow-up of 2026-11-17: the bell strike plays with the warning).
        playSounds(mob, a, "telegraph", a.beam.telegraphSounds());
        adapters.log().info(MobBeamLines.telegraph(a.mobId, mob.getUniqueId(), gearScore(a), forced,
                Bukkit.getCurrentTick(), mob.getTicksLived(), vec(mob.getLocation()), a.lock.targetKind,
                target.getUniqueId(), eye, aim, line.direction(), a.beam.range(), a.beam.telegraphTicks()));
    }

    /**
     * I4: the mob's vanilla target if the predicate names it and it is in range; else the NEAREST creature the
     * predicate names within range (eye to aim point). Null when there is none.
     */
    private LivingEntity choose(Mob mob, MobBeam beam, Vec3 eye) {
        LivingEntity vanilla = mob.getTarget();
        if (vanilla != null && eligible(mob, vanilla, beam, eye)) return vanilla;
        double r = beam.range();
        List<LivingEntity> found = new ArrayList<>();
        for (Entity e : mob.getWorld().getNearbyEntities(mob.getEyeLocation(), r, r, r)) {
            if (e instanceof LivingEntity l && eligible(mob, l, beam, eye)) found.add(l);
        }
        return found.stream()
                .min(Comparator.comparingDouble(l -> eye.distanceSquared(beam.aimPoint(vec(l.getLocation()),
                        l.getHeight()))))
                .orElse(null);
    }

    private static boolean eligible(Mob mob, LivingEntity l, MobBeam beam, Vec3 eye) {
        if (l.equals(mob) || !LiveTargets.isLive(l) || !l.getWorld().equals(mob.getWorld())) return false;
        if (!MobHostility.hostileTo(side(l), exempt(l))) return false;
        return beam.inRange(eye, beam.aimPoint(vec(l.getLocation()), l.getHeight()));
    }

    private void fire(Active a) {
        Locked k = a.lock;
        a.lock = null;
        Mob mob = a.mob;
        World world = mob.getWorld();
        BeamLine full = k.line;
        // I13: the first block with a collision box stops it; fluids and passable blocks do not.
        RayTraceResult rt = world.rayTraceBlocks(loc(world, full.origin()), vector(full.direction()), full.length(),
                FluidCollisionMode.NEVER, true);
        boolean byBlock = rt != null;
        BeamLine fired = byBlock ? full.clip(vec(rt.getHitPosition()).subtract(full.origin()).length()) : full;

        // The candidates: the locked target if it is still a live body, and every other live body near the segment.
        List<MobBeam.Candidate> candidates = new ArrayList<>();
        List<LivingEntity> bodies = new ArrayList<>();
        if (LiveTargets.isLive(k.target)) {
            candidates.add(candidate(k.target));
            bodies.add(k.target);
        }
        Vec3 o = fired.origin();
        Vec3 e = fired.end();
        double t = a.beam.tolerance() + 1.0;
        BoundingBox around = new BoundingBox(o.x(), o.y(), o.z(), e.x(), e.y(), e.z()).expand(t);
        for (Entity n : world.getNearbyEntities(around)) {
            if (!(n instanceof LivingEntity l) || l.equals(mob) || l.equals(k.target) || !LiveTargets.isLive(l)) continue;
            candidates.add(candidate(l));
            bodies.add(l);
        }
        MobBeam.Shot shot = a.beam.resolve(fired, k.target.getUniqueId(), world.getName(), candidates);

        int gs = gearScore(a);
        double dealt = a.beam.price(MobClassifier.isHostile(mob), gs);
        if (shot.hit()) {
            // The mob-hit path's final call, as the Phage's blast and a mob's melee take it: defence applies, no
            // element, no crit, no knockback, no shield block (no vanilla event is raised).
            LivingEntity victim = k.target;
            UUID dealer = mob.getUniqueId();
            if (Bukkit.isOwnedByCurrentRegion(victim)) {
                BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, dealer);
            } else {
                adapters.scheduler().onEntity(victim, () ->
                        BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, dealer));
            }
        }

        drawLine(world, fired, BEAM);
        Location at = mob.getLocation();
        // The fire's sounds, from content (the follow-up: the resonance starts at the real shot; the chime stays).
        playSounds(mob, a, "fire", a.beam.fireSounds());

        List<String> kinds = new ArrayList<>();
        for (MobBeam.Tested tested : shot.tested()) {
            LivingEntity body = tested.id().equals(k.target.getUniqueId()) ? k.target : find(bodies, tested.id());
            kinds.add(body == null ? k.targetKind : kind(body));
        }
        long tick = Bukkit.getCurrentTick();
        adapters.log().info(MobBeamLines.fire(a.mobId, mob.getUniqueId(), gs, dealt, tick, vec(at), fired.origin(),
                fired.direction(), fired.length(), byBlock, a.beam.tolerance(), shot.tested(), kinds, shot.hit()));

        // E5: the report, on a hit only (MobBeam.report is empty on a miss).
        MobBeam.report(shot, mob.getUniqueId(), a.mobId, k.target.getUniqueId(), tick, fired).ifPresent(hit -> {
            adapters.log().info(MobBeamLines.report(a.mobId, mob.getUniqueId(), k.targetKind, hit.target(), hit.tick(),
                    hit.from(), hit.to()));
            for (Listener l : listeners) l.onBeamHit(mob, k.target, hit);
        });
    }

    /**
     * Play each authored sound at the mob, in order, and log one line for each (the follow-up of 2026-11-17), so the
     * gate reads what was played and when from the server. The key is the sound EVENT key; the client picks the
     * variant, and multiplies our pitch by that variant's, which the server cannot know.
     */
    private void playSounds(Mob mob, Active a, String when, List<BeamSound> sounds) {
        long tick = Bukkit.getCurrentTick();
        for (BeamSound s : sounds) {
            mob.getWorld().playSound(mob.getLocation(), s.key(), SoundCategory.HOSTILE, (float) s.volume(),
                    (float) s.pitch());
            adapters.log().info(MobBeamLines.sound(a.mobId, mob.getUniqueId(), when, tick, s));
        }
    }

    // --- the root (I11) ------------------------------------------------------------------------------------------

    private void root(Mob mob) {
        mob.getPathfinder().stopPathfinding();
        // Once, at the lock: the walk's own momentum would otherwise carry it on for a few ticks after its speed is
        // zero. Vertical motion is kept (gravity); a push during the telegraph is not undone (I18).
        Vector v = mob.getVelocity();
        mob.setVelocity(new Vector(0, v.getY(), 0));
        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(adapters.keys().beamRoot) == null) {
            // x0: MULTIPLY_SCALAR_1 adds (factor - 1) of the total, so -1 is "times zero".
            speed.addTransientModifier(new AttributeModifier(adapters.keys().beamRoot, -1.0,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    private void unroot(Mob mob) {
        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(adapters.keys().beamRoot) != null) {
            speed.removeModifier(adapters.keys().beamRoot);
        }
    }

    // --- helpers -------------------------------------------------------------------------------------------------

    private int gearScore(Active a) {
        Integer stored = a.mob.getPersistentDataContainer().get(adapters.keys().mobGearScore, PersistentDataType.INTEGER);
        boolean valid = stored != null && MobGearScore.isValidStored(stored);
        if (!valid && MobClassifier.isHostile(a.mob)) {
            adapters.warnOnce("BEAM " + a.mobId + " " + a.mob.getUniqueId() + " has no valid gear score (stored "
                    + stored + "), so it is priced at GS " + MobScaling.GS_BASELINE);
        }
        return valid ? stored : MobScaling.GS_BASELINE;
    }

    private static MobBeam.Candidate candidate(LivingEntity l) {
        var b = l.getBoundingBox();
        return new MobBeam.Candidate(l.getUniqueId(), side(l), exempt(l), l.getWorld().getName(),
                new BeamBox(new Vec3(b.getMinX(), b.getMinY(), b.getMinZ()),
                        new Vec3(b.getMaxX(), b.getMaxY(), b.getMaxZ())));
    }

    private static MobHostility.Side side(LivingEntity l) {
        return l instanceof Player ? MobHostility.Side.PLAYER : MobHostility.Side.MOB;
    }

    private static boolean exempt(LivingEntity l) {
        return l instanceof Player p && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
    }

    private String kind(LivingEntity l) {
        String id = l.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        return l.getType().getKey().getKey() + (id == null ? "" : "/" + id);
    }

    private static LivingEntity find(List<LivingEntity> bodies, UUID id) {
        for (LivingEntity l : bodies) if (l.getUniqueId().equals(id)) return l;
        return null;
    }

    private static void drawLine(World world, BeamLine line, Particle.DustOptions dust) {
        for (double s = 0; s <= line.length(); s += PARTICLE_STEP) {
            Vec3 p = line.at(s);
            world.spawnParticle(Particle.DUST, p.x(), p.y(), p.z(), 1, 0, 0, 0, 0, dust);
        }
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }

    private static Vec3 vec(Vector v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }

    private static Location loc(World w, Vec3 v) {
        return new Location(w, v.x(), v.y(), v.z());
    }

    private static Vector vector(Vec3 v) {
        return new Vector(v.x(), v.y(), v.z());
    }
}
