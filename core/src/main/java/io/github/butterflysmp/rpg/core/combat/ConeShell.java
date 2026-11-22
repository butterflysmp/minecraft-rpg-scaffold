package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * THE CONE CAST'S GEOMETRY, pure (the seat's brief of 2026-11-22, the Living Lantern). {@code CastExecutor} asks this
 * which bodies one tick of the front reaches, and where to draw the front; the world calls (the query, the line of
 * sight, the strike) stay in the executor.
 *
 * <p>Here and not in {@code core.ability} because {@code core.combat} imports nothing from {@code core.ability}, and
 * this needs only an {@link Aim}, bodies and numbers.
 */
public final class ConeShell {

    private ConeShell() {}

    /** How many points draw the front's ring, beside its centre. A look number, not a rule. */
    public static final int FRONT_RING_POINTS = 8;

    /** A body the front reached this tick, and how far its sight point is from the origin. */
    public record Reached(Combatant combatant, double distance) {}

    /**
     * The bodies whose SIGHT POINT lies inside the cone ({@link Aim#withinArc}, the melee swing's angle test) and in the
     * SHELL {@code (inner, outer]} from the aim's origin, nearest first. The caster ({@code casterId}) and every id in
     * {@code struck} are left out. Line of sight is NOT judged here: it needs the world.
     *
     * <p>{@code inner} is exclusive and is what keeps a body that walked into the cone behind the front from being
     * struck: it is nearer than where the front was last tick. Pass {@code Double.NEGATIVE_INFINITY} for the first
     * tick, or for an instant cone.
     */
    public static List<Reached> reached(Aim aim, double arcDegrees, double inner, double outer,
                                        Collection<Combatant> candidates, UUID casterId, Set<UUID> struck) {
        List<Reached> out = new ArrayList<>();
        for (Combatant c : candidates) {
            if (c.id().equals(casterId) || struck.contains(c.id())) continue;
            Vec3 sight = c.state().sightPoint();
            double distance = sight.subtract(aim.origin()).length();
            if (!(distance > inner && distance <= outer)) continue;
            if (!aim.withinArc(sight, arcDegrees)) continue;
            out.add(new Reached(c, distance));
        }
        out.sort(Comparator.comparingDouble(Reached::distance));
        return out;
    }

    /**
     * Where the front is drawn at {@code distance}: its centre on the aim, then {@link #FRONT_RING_POINTS} points round
     * a ring of radius {@code distance x tan(arc / 2)}, the cone's own width there, in the plane square to the aim.
     *
     * <p>The ring's basis is the aim's RIGHT and {@code direction x right}. A vertical look built by the two-argument
     * {@code Aim} constructor has a ZERO right ({@code Aim}'s javadoc); production aims carry the view's own right
     * ({@code ViewAim}), and for the zero case this falls back to world +x so the ring is still drawn, never collapsed.
     */
    public static List<Vec3> frontPoints(Aim aim, double distance, double arcDegrees) {
        Vec3 centre = aim.pointAt(distance);
        double radius = distance * Math.tan(Math.toRadians(arcDegrees / 2.0));
        Vec3 right = aim.right().lengthSquared() > 0 ? aim.right().normalize() : new Vec3(1, 0, 0);
        Vec3 up = aim.direction().cross(right).normalize();
        List<Vec3> out = new ArrayList<>(1 + FRONT_RING_POINTS);
        out.add(centre);
        for (int i = 0; i < FRONT_RING_POINTS; i++) {
            double a = 2 * Math.PI * i / FRONT_RING_POINTS;
            out.add(centre.add(right.scale(radius * Math.cos(a))).add(up.scale(radius * Math.sin(a))));
        }
        return out;
    }
}
