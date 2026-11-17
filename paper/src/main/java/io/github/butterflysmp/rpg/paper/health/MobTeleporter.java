package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamHit;
import io.github.butterflysmp.rpg.core.mob.BeamTeleport;
import io.github.butterflysmp.rpg.core.mob.MobBeam;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobTeleport;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * THE KNELL'S TELEPORT AFTER ITS BEAM LANDS, the Bukkit half (PLAN-dragon-mobs.md step R6; §4.2 N8). The decision is
 * core's ({@link BeamTeleport#decide}); this class times it, answers its four questions through the collision API
 * (section 7 F-T), moves the mob, and logs one line.
 *
 * <h2>THE TRIGGER IS THE BEAM'S REPORT AND NOTHING ELSE (the brief's ruling 1)</h2>
 * It is a {@link MobBeamer.Listener}, told only of a HIT ({@code MobBeam.report} is empty on a miss). Nothing else in
 * the plugin moves a Knell: no damage listener, no projectile listener (cfde822's arrow dodge was never ported).
 *
 * <h2>THE DELAY DIES WITH THE MOB (ruling 2)</h2>
 * A {@link RepeatingTask} on the Knell's OWN entity scheduler, period {@code delay_ticks}, that runs its body once.
 * Its {@code isActive} check ({@code isValid() && !isDead()}) runs before the body, so a Knell dead at the end of the
 * delay stops the task WITHOUT moving, and {@code onStop} logs {@code TELEPORT skipped ... reason=knell-gone}. A
 * removal or unload first ({@code EntityRemoveFromWorldEvent}) reaches {@link #onRemove} through the beamer, which
 * cancels the task: the same line. The target is re-checked at the end of the delay ({@link BeamTeleport#skip}).
 *
 * <h2>THE MOVE (ruling 5), READ from the server jar (build 74)</h2>
 * {@code Entity#teleport(Location)} reaches {@code CraftEntity.teleport0}, which builds a {@code TeleportTransition}
 * with motion {@code Vec3.ZERO} and no relative flags, then {@code Entity.teleport} → {@code teleportSameDimension} →
 * {@code teleportSetPosition}: position, yaw, head yaw, pitch, {@code setDeltaMovement(ZERO)},
 * {@code clearMovementThisTick}. <b>Nothing there touches the mob's navigation or its target</b>, so the path toward
 * the target is stopped here after the move ({@code stopPathfinding}), and the target is kept. Health, tags, gear score
 * and the beam's clock are untouched: they live elsewhere and the move writes none of them.
 */
public final class MobTeleporter implements MobBeamer.Listener {

    private static final class Pending {
        RepeatingTask task;
        boolean done;
    }

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, BeamTeleport.Draw> fixed = new ConcurrentHashMap<>();
    private final Supplier<MobRegistry> mobs;
    private final AdapterContext adapters;
    private final BeamTeleport.Source source;

    public MobTeleporter(Supplier<MobRegistry> mobs, AdapterContext adapters, BeamTeleport.Source source) {
        this.mobs = mobs;
        this.adapters = adapters;
        this.source = source;
    }

    /** THE INSTRUMENT (ruling 8): this Knell's NEXT decision uses {@code draw}, not the random source. */
    public void fix(UUID knell, BeamTeleport.Draw draw) {
        fixed.put(knell, draw);
    }

    @Override
    public void onBeamHit(Mob mob, LivingEntity target, BeamHit hit) {
        MobDefinition def = mobs.get().find(hit.mobId()).orElse(null);
        if (def == null || def.teleport() == null) return;
        UUID id = mob.getUniqueId();
        Pending running = pending.get(id);
        if (running != null && running.task.isRunning()) {
            adapters.log().info(MobTeleportLines.ignored(hit.mobId(), id));
            return;
        }
        MobTeleport t = def.teleport();
        MobBeam beam = def.beam();
        Pending p = new Pending();
        p.task = RepeatingTask.start(new EntityTaskTarget(mob, adapters.scheduler(), adapters.log()), t.delayTicks(),
                "knell-teleport",
                () -> {
                    p.done = true;
                    decideAndMove(mob, hit.mobId(), target, t, beam);
                    return false;   // once
                },
                () -> {
                    if (!p.done) {
                        adapters.log().info(MobTeleportLines.skipped(hit.mobId(), id, target.getUniqueId(),
                                BeamTeleport.Skip.KNELL_GONE));
                    }
                    pending.remove(id, p);
                });
        pending.put(id, p);
    }

    @Override
    public void onRemove(UUID id) {
        Pending p = pending.get(id);
        if (p != null) p.task.cancel();
    }

    private void decideAndMove(Mob mob, String mobId, LivingEntity target, MobTeleport t, MobBeam beam) {
        boolean exempt = target instanceof Player pl
                && (pl.getGameMode() == GameMode.CREATIVE || pl.getGameMode() == GameMode.SPECTATOR);
        MobHostility.Side side = target instanceof Player ? MobHostility.Side.PLAYER : MobHostility.Side.MOB;
        Optional<BeamTeleport.Skip> skip = BeamTeleport.skip(true, LiveTargets.isLive(target),
                target.getWorld().equals(mob.getWorld()), side, exempt);
        if (skip.isPresent()) {
            adapters.log().info(MobTeleportLines.skipped(mobId, mob.getUniqueId(), target.getUniqueId(), skip.get()));
            return;
        }
        Vec3 from = vec(mob.getLocation());
        Vec3 center = vec(target.getLocation());
        BeamTeleport.Draw fixedDraw = fixed.remove(mob.getUniqueId());
        BeamTeleport.Draw draw = fixedDraw != null ? fixedDraw : source.draw(t.arcMinDegrees(), t.arcMaxDegrees());
        BeamTeleport.Decision d = BeamTeleport.decide(t, center, from, draw, footing(mob, target, beam));

        String moved = "-";
        if (d.landing().isPresent()) {
            Vec3 l = d.landing().get();
            World world = mob.getWorld();
            Location to = new Location(world, l.x(), l.y(), l.z());
            to.setDirection(new Vector(center.x() - l.x(), 0, center.z() - l.z()));   // facing the target, level
            puff(world, mob.getLocation());
            boolean ok = mob.teleport(to);
            moved = String.valueOf(ok);
            if (ok) {
                mob.getPathfinder().stopPathfinding();   // the move leaves the old path; nothing in the jar stops it
                puff(world, to);
            }
        }
        adapters.log().info(MobTeleportLines.decision(mobId, mob.getUniqueId(), kind(target), target.getUniqueId(),
                Bukkit.getCurrentTick(), from, d, center, draw.firstAngle(), moved));
    }

    /**
     * THE PORT, on the collision API (section 7 F-T), each READ in the server jar (build 74; {@code javap -c -p -cp
     * run/versions/26.1.2/paper-26.1.2.jar org.bukkit.craftbukkit.entity.CraftEntity} and {@code
     * org.bukkit.craftbukkit.CraftRegionAccessor}, {@code net.minecraft.world.level.CollisionGetter}):
     * <ul>
     *   <li>{@code loaded}: {@code World#isChunkLoaded} for the column's chunk. The collision calls do not load chunks,
     *       and an unloaded point would read as free (F-T), so this is asked first.</li>
     *   <li>{@code fits}: {@code Entity#wouldCollideUsing(box)} is {@code !level.noCollision(thisEntity, aabb)}: block
     *       shapes, hard-collision entities other than the Knell itself, and the world border. The box is the Knell's
     *       OWN scaled width and height ({@code getWidth}, {@code getHeight}) at the feet, shrunk by
     *       {@link #INSET} on every face so a face resting exactly on the floor is not counted as a collision.</li>
     *   <li>{@code grounded}: {@code RegionAccessor#hasCollisionsIn(box)} is {@code !level.noCollision(aabb)} (block
     *       collision shapes, then entities, then the border). The box is the Knell's footprint (inset as above), from
     *       {@link #GROUND_DEPTH} to {@link #INSET} below the feet: a thin slab inside whatever block the feet stand
     *       on. A hit there is ground. INFERRED, not read: that a slab, a fence or a boat counts too (any collision).</li>
     *   <li>{@code sees}: {@code World#rayTraceBlocks} from the Knell's EYE standing there ({@code getEyeHeight}, which
     *       follows the scale) to the target's AIM POINT (the beam's own, {@code aim_height} of its height), with
     *       {@code FluidCollisionMode.NEVER} and passable blocks ignored: <b>the beam's block rule</b> (I13), so a spot
     *       it sees from is one it could beam from.</li>
     * </ul>
     */
    private static BeamTeleport.Footing footing(Mob mob, LivingEntity target, MobBeam beam) {
        World world = mob.getWorld();
        double half = mob.getWidth() / 2.0 - INSET;
        double height = mob.getHeight();
        double eye = mob.getEyeHeight();
        Vec3 aim = beam.aimPoint(vec(target.getLocation()), target.getHeight());
        return new BeamTeleport.Footing() {
            @Override public boolean loaded(Vec3 f) {
                return world.isChunkLoaded((int) Math.floor(f.x()) >> 4, (int) Math.floor(f.z()) >> 4);
            }

            @Override public boolean fits(Vec3 f) {
                return !mob.wouldCollideUsing(new BoundingBox(f.x() - half, f.y() + INSET, f.z() - half,
                        f.x() + half, f.y() + height - INSET, f.z() + half));
            }

            @Override public boolean grounded(Vec3 f) {
                return world.hasCollisionsIn(new BoundingBox(f.x() - half, f.y() - GROUND_DEPTH, f.z() - half,
                        f.x() + half, f.y() - INSET, f.z() + half));
            }

            @Override public boolean sees(Vec3 f) {
                Vec3 from = new Vec3(f.x(), f.y() + eye, f.z());
                Vec3 d = aim.subtract(from);
                double len = d.length();
                if (len == 0) return true;
                Vec3 u = d.scale(1.0 / len);
                return world.rayTraceBlocks(new Location(world, from.x(), from.y(), from.z()),
                        new Vector(u.x(), u.y(), u.z()), len, FluidCollisionMode.NEVER, true) == null;
            }
        };
    }

    /** BLOCKS each face of a tested box is pulled in by, so resting on a face is not a collision. NOT IN THE RECORD. */
    static final double INSET = 1.0E-3;
    /** BLOCKS below the feet the ground slab reaches down to. NOT IN THE RECORD: chosen, well inside any block. */
    static final double GROUND_DEPTH = 0.05;

    /**
     * cfde822's sidestep showed "Soul and soul-fire particles and an amethyst step at both ends" (READ, the plan's
     * section 3.1). The counts, spread, volume and pitch are NOT IN THE RECORD: 12 of each, spread over the body, 1.0
     * and 1.0, chosen.
     */
    private static void puff(World world, Location at) {
        Location mid = at.clone().add(0, 1.0, 0);
        world.spawnParticle(Particle.SOUL, mid, 12, 0.4, 0.8, 0.4, 0.02);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, mid, 12, 0.4, 0.8, 0.4, 0.02);
        world.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private String kind(LivingEntity l) {
        String id = l.getPersistentDataContainer().get(adapters.keys().mobId, PersistentDataType.STRING);
        return l.getType().getKey().getKey() + (id == null ? "" : "/" + id);
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
