package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import io.github.butterflysmp.rpg.core.shape.ImplosionDrag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * THE IMPLOSION'S DRAG, RUN IN THE WORLD (2026-11-18; Ben's four yeses). The rules are core's ({@link ImplosionDrag},
 * {@link Implosion#dragStep}, {@link io.github.butterflysmp.rpg.core.shape.DragMotion}); this is the Bukkit half, the
 * one runner both uses share: the Phage's blast ({@code MobBlaster}) and an ability's ({@code PaperCombatWorld
 * #startDrag}).
 *
 * <h2>EACH TICK, ON THE ORIGIN'S REGION</h2>
 * <ol>
 *   <li><b>Who:</b> every living entity in a cube of the reach round the origin that {@link LiveTargets#isLive} and the
 *       use's {@code allowed} pass, and every one the drag already knows, looked up again by id wherever it went. A
 *       known one that is gone, dead, in another world or no longer allowed (a player gone creative) is DROPPED.</li>
 *   <li><b>Its footing,</b> read here ({@link ImplosionFooting}), and its velocity: a mob's is the server's
 *       ({@code getVelocity}), a player's is never read: core estimates it from his positions (DragMotion's javadoc:
 *       the server's copy is not his, D1-D3 of the brief).</li>
 *   <li><b>The pushes</b> core answers are SET ({@code setVelocity}, which marks the body for the client). Nothing is
 *       set for a victim the drag adds nothing to: inside the margin he is left entirely alone.</li>
 * </ol>
 * Tick 0 runs inline, in the caller's tick (the blast's); each later tick is POINT WORK at the origin through
 * {@link PendingWork}, so the selftest's row-start clean ends it (its LINGER counts it) and a chain whose link is gone
 * never schedules the next. A server stop cancels every task of the plugin's (INFERRED: Bukkit cancels a disabled
 * plugin's tasks); a drag holds nothing that outlives it but a velocity already sent.
 *
 * <p><b>THE LINE</b>, one per victim when the drag ends ({@link #line}): {@code DRAG source=<phage|ability> id=<the
 * blast's or caster's uuid> victim=<type>[/<mob id>] <uuid> start=<d> end=<d> ticks=<pulled> sent=<n>
 * outcome=<held|left|dropped|pulled>}, distances feet to feet from the fixed origin. A drag a clean ends logs none.
 *
 * <p><b>Threading.</b> Every entity this reads or moves is owned by the origin's region on Paper (the main thread). On
 * Folia a victim across a region edge is not, and is skipped that tick rather than touched off its thread.
 */
public final class ImplosionDrags {

    private ImplosionDrags() {}

    /**
     * Start a drag at {@code origin} in {@code world} and run its tick 0 now, on the caller's thread (which must own
     * the origin's region). {@code allowed} is the use's predicate; {@code source} and {@code sourceId} name it in the
     * DRAG lines.
     */
    public static void start(AdapterContext ctx, World world, Vec3 origin, Implosion shape,
                             Predicate<LivingEntity> allowed, String source, UUID sourceId) {
        ImplosionDrag drag = new ImplosionDrag(origin, shape, ctx.dragMotion());
        Map<UUID, String> kinds = new HashMap<>();
        step(ctx, world, drag, allowed, source, sourceId, kinds);
    }

    private static void step(AdapterContext ctx, World world, ImplosionDrag drag, Predicate<LivingEntity> allowed,
                             String source, UUID sourceId, Map<UUID, String> kinds) {
        Vec3 o = drag.origin();
        Location at = new Location(world, o.x(), o.y(), o.z());
        double r = drag.shape().reach();
        Map<UUID, LivingEntity> bodies = new HashMap<>();
        for (Entity e : world.getNearbyEntities(at, r, r, r)) {
            if (e instanceof LivingEntity l && LiveTargets.isLive(l) && allowed.test(l)) bodies.put(l.getUniqueId(), l);
        }
        Set<UUID> dropped = new HashSet<>();
        for (UUID id : drag.known()) {
            if (bodies.containsKey(id)) continue;
            Entity e = Bukkit.getEntity(id);
            if (e instanceof LivingEntity l && LiveTargets.isLive(l) && l.getWorld().equals(world) && allowed.test(l)) {
                bodies.put(id, l);
            } else {
                dropped.add(id);
            }
        }
        List<ImplosionDrag.Sighting> seen = new ArrayList<>();
        for (LivingEntity l : bodies.values()) {
            if (!Bukkit.isOwnedByCurrentRegion(l)) continue;   // Folia: not this thread's to read or move
            kinds.computeIfAbsent(l.getUniqueId(), k -> kind(ctx, l));
            Vector v = l.getVelocity();
            seen.add(new ImplosionDrag.Sighting(l.getUniqueId(), ImplosionFooting.of(l, drag.shape().resistance()),
                    l instanceof Player ? null : new Vec3(v.getX(), v.getY(), v.getZ())));
        }
        for (ImplosionDrag.Push p : drag.tick(Bukkit.getCurrentTick(), seen, dropped)) {
            LivingEntity l = bodies.get(p.id());
            Vec3 v = p.velocity();
            l.setVelocity(new Vector(v.x(), v.y(), v.z()));
        }
        if (drag.finished()) {
            for (ImplosionDrag.Summary s : drag.summaries()) {
                ctx.log().info(line(source, sourceId, kinds.getOrDefault(s.id(), "?"), s));
            }
            return;
        }
        long id = ctx.work().add(world.getUID(), o.x(), o.y(), o.z());
        ctx.scheduler().onRegionLater(at, () -> {
            if (ctx.work().take(id)) step(ctx, world, drag, allowed, source, sourceId, kinds);
        }, 1);
    }

    /** {@code <type>[/<mob id>]}, as the BLAST line names a creature. */
    private static String kind(AdapterContext ctx, LivingEntity e) {
        String mob = e.getPersistentDataContainer().get(ctx.keys().mobId, PersistentDataType.STRING);
        return e.getType().getKey().getKey() + (mob == null ? "" : "/" + mob);
    }

    /** One victim's DRAG line (class javadoc). */
    static String line(String source, UUID sourceId, String kind, ImplosionDrag.Summary s) {
        return "DRAG source=" + source + " id=" + sourceId + " victim=" + kind + " " + s.id()
                + String.format(Locale.ROOT, " start=%.3f end=%.3f ticks=%d sent=%d outcome=%s", s.start(), s.end(),
                s.ticks(), s.sent(), s.outcome().name().toLowerCase(Locale.ROOT));
    }
}
