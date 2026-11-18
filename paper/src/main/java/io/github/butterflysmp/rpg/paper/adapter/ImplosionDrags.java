package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import io.github.butterflysmp.rpg.core.shape.ImplosionDrag;
import org.bukkit.Bukkit;
import org.bukkit.Input;
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
 * {@link Implosion#dragStep}, {@link Implosion#addStep}); this is the Bukkit half, the one runner both uses share: the
 * Phage's blast ({@code MobBlaster}) and an ability's ({@code PaperCombatWorld#startDrag}).
 *
 * <h2>EACH TICK, ON THE ORIGIN'S REGION</h2>
 * <ol>
 *   <li><b>Who:</b> every living entity in a cube of the reach round the origin that {@link LiveTargets#isLive} and the
 *       use's {@code allowed} pass, and every one the drag already knows, looked up again by id wherever it went. A
 *       known one that is gone, dead, in another world or no longer allowed (a player gone creative) is DROPPED.</li>
 *   <li><b>Its footing,</b> read here ({@link ImplosionFooting}), and its velocity: a mob's is the server's
 *       ({@code getVelocity}); a player's is never read, because the server does not know it (feat/drag-additive).</li>
 *   <li><b>The pushes</b> core answers: a mob's is SET ({@code setVelocity}, which marks the body for the client); a
 *       player's is an ADD his own client adds to its momentum ({@link DragImpulse}, one packet per drag per tick).
 *       Nothing is sent for a victim the drag adds nothing to: inside the margin he is left entirely alone.</li>
 * </ol>
 * Tick 0 runs inline, in the caller's tick (the blast's); each later tick is POINT WORK at the origin through
 * {@link PendingWork}, so the selftest's row-start clean ends it (its LINGER counts it) and a chain whose link is gone
 * never schedules the next. A server stop cancels every task of the plugin's (INFERRED: Bukkit cancels a disabled
 * plugin's tasks); a drag holds nothing that outlives it but a push already sent.
 *
 * <p><b>THE LINE</b>, one per victim when the drag ends ({@link #line}): {@code DRAG source=<phage|ability> id=<the
 * blast's or caster's uuid> victim=<type>[/<mob id>] <uuid> start=<d> end=<d> ticks=<pulled> sent=<n>
 * outcome=<held|left|dropped|pulled>}, distances feet to feet from the fixed origin; with the instrument on, a
 * player's also ends {@code movesMin=<n> movesMax=<n>}, over its pulled ticks. A drag a clean ends logs none.
 *
 * <p><b>THE PER-TICK LINE</b> (the seat's Part D; only while {@link DragTrace#on()}): one per PLAYER victim per drag
 * tick ({@link #tickLine}): {@code DRAGTICK drag=<the blast's or caster's uuid> t=<0..20> victim=<uuid> at=<x y z>
 * d=<to origin> moves=<n|-> add=<vx vz|-> cap=<bound|-> ping=<ms> keys=<held keys|->}. {@code at} and {@code d} are the
 * SERVER's view this tick, the one the add is computed from; {@code moves} his position-bearing move packets since this
 * drag's last tick ({@code -} at his first); {@code add} what was sent, {@code cap} the hold's bound
 * ({@link Implosion#addStep}), each {@code -} when he was not pulled; {@code ping} {@code Player#getPing};
 * {@code keys} {@code Player#getCurrentInput}, the ones held, in the order forward, back, left, right, jump, sneak,
 * sprint.
 *
 * <p><b>Threading.</b> Every entity this reads or moves is owned by the origin's region on Paper (the main thread). On
 * Folia a victim across a region edge is not, and is skipped that tick rather than touched off its thread.
 */
public final class ImplosionDrags {

    private ImplosionDrags() {}

    /** What one drag remembers between its ticks: names for the lines, and the instrument's move counts. */
    private static final class Run {
        final Map<UUID, String> kinds = new HashMap<>();
        final Map<UUID, Long> lastMoves = new HashMap<>();
        final Map<UUID, long[]> movesRange = new HashMap<>();   // {min, max} over pulled ticks
    }

    /**
     * Start a drag at {@code origin} in {@code world} and run its tick 0 now, on the caller's thread (which must own
     * the origin's region). {@code allowed} is the use's predicate; {@code source} and {@code sourceId} name it in the
     * DRAG lines.
     */
    public static void start(AdapterContext ctx, World world, Vec3 origin, Implosion shape,
                             Predicate<LivingEntity> allowed, String source, UUID sourceId) {
        ImplosionDrag drag = new ImplosionDrag(origin, shape, ctx.dragMotion());
        step(ctx, world, drag, allowed, source, sourceId, new Run());
    }

    private static void step(AdapterContext ctx, World world, ImplosionDrag drag, Predicate<LivingEntity> allowed,
                             String source, UUID sourceId, Run run) {
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
            run.kinds.computeIfAbsent(l.getUniqueId(), k -> kind(ctx, l));
            Vector v = l.getVelocity();
            seen.add(new ImplosionDrag.Sighting(l.getUniqueId(), ImplosionFooting.of(l, drag.shape().resistance()),
                    l instanceof Player ? null : new Vec3(v.getX(), v.getY(), v.getZ())));
        }
        Map<UUID, Vec3> sent = new HashMap<>();
        for (ImplosionDrag.Push p : drag.tick(Bukkit.getCurrentTick(), seen, dropped)) {
            LivingEntity l = bodies.get(p.id());
            Vec3 v = p.velocity();
            if (p.mode() == ImplosionDrag.Mode.ADD && l instanceof Player player) {
                DragImpulse.send(player, v);
            } else {
                l.setVelocity(new Vector(v.x(), v.y(), v.z()));
            }
            sent.put(p.id(), v);
        }
        if (ctx.dragTrace().on()) trace(ctx, drag, sourceId, bodies, sent, run);
        if (drag.finished()) {
            for (ImplosionDrag.Summary s : drag.summaries()) {
                long[] range = run.movesRange.get(s.id());
                String kind = run.kinds.getOrDefault(s.id(), "?");
                ctx.log().info(range == null ? line(source, sourceId, kind, s)
                        : line(source, sourceId, kind, s, range[0], range[1]));
            }
            return;
        }
        long id = ctx.work().add(world.getUID(), o.x(), o.y(), o.z());
        ctx.scheduler().onRegionLater(at, () -> {
            if (ctx.work().take(id)) step(ctx, world, drag, allowed, source, sourceId, run);
        }, 1);
    }

    /** The instrument's lines for this tick: one per player victim sighted. */
    private static void trace(AdapterContext ctx, ImplosionDrag drag, UUID sourceId, Map<UUID, LivingEntity> bodies,
                              Map<UUID, Vec3> sent, Run run) {
        for (ImplosionDrag.Step s : drag.steps()) {
            if (!(bodies.get(s.id()) instanceof Player p)) continue;
            long total = ctx.dragTrace().moves(s.id());
            Long last = run.lastMoves.put(s.id(), total);
            String moves = "-";
            if (last != null) {
                long n = total - last;
                moves = Long.toString(n);
                if (s.step() != null) {
                    run.movesRange.merge(s.id(), new long[] {n, n},
                            (a, b) -> new long[] {Math.min(a[0], b[0]), Math.max(a[1], b[1])});
                }
            }
            Location l = p.getLocation();
            Input in = p.getCurrentInput();
            ctx.log().info(tickLine(sourceId, s.tick(), s.id(), l.getX(), l.getY(), l.getZ(), s.distance(), moves,
                    sent.get(s.id()), s.step() == null ? null : s.step().cap(), p.getPing(),
                    keys(in.isForward(), in.isBackward(), in.isLeft(), in.isRight(), in.isJump(), in.isSneak(),
                            in.isSprint())));
        }
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

    /** One victim's DRAG line with the instrument on: the fewest and most moves over its pulled ticks. */
    static String line(String source, UUID sourceId, String kind, ImplosionDrag.Summary s, long movesMin,
                       long movesMax) {
        return line(source, sourceId, kind, s) + " movesMin=" + movesMin + " movesMax=" + movesMax;
    }

    /** One DRAGTICK line (class javadoc). {@code add} and {@code cap} null print {@code -}. */
    static String tickLine(UUID drag, int t, UUID victim, double x, double y, double z, double d, String moves,
                           Vec3 add, Double cap, int ping, String keys) {
        return "DRAGTICK drag=" + drag + " t=" + t + " victim=" + victim
                + String.format(Locale.ROOT, " at=%.3f %.3f %.3f d=%.3f", x, y, z, d) + " moves=" + moves
                + " add=" + (add == null ? "-" : String.format(Locale.ROOT, "%.6f %.6f", add.x(), add.z()))
                + " cap=" + (cap == null ? "-" : String.format(Locale.ROOT, "%.6f", cap))
                + " ping=" + ping + " keys=" + keys;
    }

    /** The held keys, named in the brief's order; {@code -} when none is held. */
    static String keys(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak,
                       boolean sprint) {
        List<String> held = new ArrayList<>();
        if (forward) held.add("forward");
        if (back) held.add("back");
        if (left) held.add("left");
        if (right) held.add("right");
        if (jump) held.add("jump");
        if (sneak) held.add("sneak");
        if (sprint) held.add("sprint");
        return held.isEmpty() ? "-" : String.join(",", held);
    }
}
