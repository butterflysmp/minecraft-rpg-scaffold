package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ONE IMPLOSION'S DRAG: the second after the blast (Ben, 2026-11-18, the seat's four points: "1. yes 2. yes 3. yes 4.
 * yes"). Pure: the caller hands it, each tick, who the use allows near the origin; it answers whom to push and how
 * hard, and at the end one summary per victim. The ORIGIN is fixed at the blast.
 *
 * <p><b>ITS TICKS.</b> Tick 0 is the blast's own tick: it only LOOKS (it is the tick of the damage). Ticks 1 to
 * {@link Implosion#durationTicks()} pull. So the Phage's 20 pull for the twenty ticks after the blast, one second.
 *
 * <p><b>EACH TICK, EACH VICTIM</b> the caller names (every combatant the use's predicate allows): its footing is read;
 * if it is INSIDE the reach this tick it is pulled, two ways (feat/drag-additive, 2026-11-18):
 * <ul>
 *   <li><b>A MOB</b> (its {@link Sighting#velocity} is the server's, which owns a mob's motion):
 *       {@link Implosion#dragStep} with that velocity, the add tallied with every other drag's on that body this tick
 *       ({@link DragMotion#tally}), returned as a {@link Mode#SET} push: the velocity to set.</li>
 *   <li><b>A PLAYER</b> (velocity {@code null}: his client owns his motion, and the server never knows it):
 *       {@link Implosion#addStep}, from his distance and footing alone, returned as a {@link Mode#ADD} push: the
 *       velocity his client ADDS to its own. Never tallied: each drag's add is its own packet, and both arrive.</li>
 * </ul>
 * An add of zero sends nothing. One outside the reach is not pulled; one who enters during the second is, from its
 * first sighting inside.
 *
 * <p><b>IT ENDS CLEANLY:</b> a victim the caller names in {@code dropped} (dead, gone, in another world, no longer one
 * the use allows: a player gone creative) is never pushed again and is summarised as {@link Outcome#DROPPED}.
 */
public final class ImplosionDrag {

    /**
     * One combatant the use allows, as the world sees it this tick: its id, its footing, and the server's velocity for
     * a mob (the server owns a mob's motion) or null for a player (his client owns his).
     */
    public record Sighting(UUID id, Implosion.Footing footing, Vec3 velocity) {}

    /** How a push reaches its body: {@code SET} the server's velocity (a mob), {@code ADD} to the client's (a player). */
    public enum Mode { SET, ADD }

    /**
     * Push {@code id}: with {@link Mode#SET}, set its velocity to {@code velocity} (its own plus every drag's add this
     * tick); with {@link Mode#ADD}, have its client add {@code velocity} (this drag's add alone) to its own.
     */
    public record Push(UUID id, Vec3 velocity, Mode mode) {}

    /**
     * One sighting of the tick just run, for the per-tick instrument: the victim, the drag tick, its distance feet to
     * feet from the origin, and its step ({@code null} when it was not pulled: tick 0, or outside the reach).
     */
    public record Step(UUID id, int tick, double distance, Implosion.DragStep step) {}

    /**
     * How a victim's drag ended. {@code HELD}: on its last pulled tick the margin limited the add (it reached the
     * margin, or stood inside it). {@code LEFT}: it was last seen outside the reach. {@code DROPPED}: the caller dropped
     * it. {@code PULLED}: pulled to the end, never limited.
     */
    public enum Outcome { HELD, LEFT, DROPPED, PULLED }

    /**
     * One victim's second: its distance feet to feet when first seen inside the reach ({@code start}) and when last
     * seen ({@code end}); the ticks it was PULLED (inside the reach on a pulling tick), the ticks a push was SENT (an
     * add above zero), and its {@link Outcome}.
     */
    public record Summary(UUID id, double start, double end, int ticks, int sent, Outcome outcome) {}

    private static final class Victim {
        double start = Double.NaN;
        double end = Double.NaN;
        int ticks;
        int sent;
        boolean inReach;
        boolean held;
        boolean dropped;
    }

    private final Vec3 origin;
    private final Implosion shape;
    private final DragMotion motion;
    private final Map<UUID, Victim> victims = new LinkedHashMap<>();
    private List<Step> steps = List.of();
    private int next;   // the next tick to run, 0 first

    public ImplosionDrag(Vec3 origin, Implosion shape, DragMotion motion) {
        if (shape.durationTicks() < 2) {
            throw new IllegalArgumentException("a drag needs a duration of 2 ticks or more; 1 is a single pull");
        }
        this.origin = origin;
        this.shape = shape;
        this.motion = motion;
    }

    public Vec3 origin() {
        return origin;
    }

    public Implosion shape() {
        return shape;
    }

    /** The tick {@link #tick} runs next: 0 (the blast's), then 1 to the duration. */
    public int nextTick() {
        return next;
    }

    /** True once every pulling tick has run. */
    public boolean finished() {
        return next > shape.durationTicks();
    }

    /** The ids this drag has seen and not dropped: the caller looks each up again next tick, wherever it went. */
    public Set<UUID> known() {
        Set<UUID> out = new java.util.LinkedHashSet<>();
        victims.forEach((id, v) -> {
            if (!v.dropped) out.add(id);
        });
        return out;
    }

    /** Every victim's sighting in the tick last run, in sighting order: the per-tick instrument's window. */
    public List<Step> steps() {
        return steps;
    }

    /**
     * RUN THE NEXT TICK. {@code serverTick} is the server's tick counter (the mob tally's clock); {@code sightings}
     * every combatant the use allows that the caller found near the origin, and every known one it looked up again;
     * {@code dropped} the known ones it could not, or that the use no longer allows. Returns the pushes, in sighting
     * order.
     */
    public List<Push> tick(long serverTick, List<Sighting> sightings, Set<UUID> dropped) {
        if (finished()) throw new IllegalStateException("the drag has finished");
        int t = next++;
        for (UUID id : dropped) {
            Victim v = victims.get(id);
            if (v != null) v.dropped = true;
        }
        List<Push> out = new ArrayList<>();
        List<Step> seen = new ArrayList<>();
        for (Sighting s : sightings) {
            Victim known = victims.get(s.id());
            if (known != null && known.dropped) continue;
            boolean in = shape.inReach(origin, s.footing().feet());
            double d = Math.sqrt(origin.distanceSquared(s.footing().feet()));
            if (known == null) {
                if (!in) continue;   // never inside the reach: not a victim of this drag
                known = new Victim();
                known.start = d;
                victims.put(s.id(), known);
            }
            known.end = d;
            known.inReach = in;
            if (t == 0 || !in) {     // tick 0 only looks; outside the reach nothing pulls
                seen.add(new Step(s.id(), t, d, null));
                continue;
            }
            Implosion.DragStep step = s.velocity() == null
                    ? shape.addStep(origin, s.footing()).orElseThrow()
                    : shape.dragStep(origin, s.footing(), s.velocity()).orElseThrow();
            seen.add(new Step(s.id(), t, d, step));
            known.ticks++;
            known.held = step.held();
            if (step.added() > 0) {
                known.sent++;
                out.add(s.velocity() == null
                        ? new Push(s.id(), step.add(), Mode.ADD)
                        : new Push(s.id(), motion.tally(s.id(), serverTick, s.velocity(), step.add()), Mode.SET));
            }
        }
        steps = List.copyOf(seen);
        motion.prune(serverTick);
        return out;
    }

    /** One summary per victim, in the order each was first seen inside the reach. */
    public List<Summary> summaries() {
        List<Summary> out = new ArrayList<>();
        victims.forEach((id, v) -> out.add(new Summary(id, v.start, v.end, v.ticks, v.sent,
                v.dropped ? Outcome.DROPPED : !v.inReach ? Outcome.LEFT : v.held ? Outcome.HELD : Outcome.PULLED)));
        return out;
    }
}
