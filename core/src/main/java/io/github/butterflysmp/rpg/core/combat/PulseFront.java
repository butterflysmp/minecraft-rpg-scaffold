package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * THE PULSE'S PURE DECISIONS (Smolder, the seat's Z1, Z2 and Z7, 2026-11-27): WHEN each pulse fires, HOW FAR its ring
 * has travelled, and WHEN the ring reaches a body. {@code CastExecutor} asks this; the world calls (finding bodies,
 * drawing, striking) stay there.
 *
 * <p><b>ONE RADIUS FUNCTION, USED BY THE HIT AND BY THE DRAWING</b> (Z7): {@link #radiusAt} is what the ring is drawn
 * at and what {@link #reachTick} compares a distance against, so what Ben sees and what strikes cannot disagree.
 *
 * <p><b>THE RULE AND ITS ROUNDING, AS ONE RULE</b> (Z2): {@code t} ticks after its pulse the ring stands at
 * {@code radius x t / travel} blocks ({@code radius} itself from {@code t = travel} on). A body whose HORIZONTAL distance
 * from the pulse's fixed origin is {@code d} is struck on the FIRST whole tick {@code t} in {@code 0..travel} at which
 * {@code radiusAt(t) >= d}: in exact arithmetic {@code ceil(d x travel / radius)}, but computed by walking
 * {@code radiusAt} itself, so the rule cannot drift from the drawing by a floating-point hair. A body at {@code d = 0} is
 * struck on tick 0; a body with {@code d > radius} never ({@code -1}).
 */
public final class PulseFront {

    private PulseFront() {}

    /**
     * How far apart the ring's drawn points stand, in blocks, along its circumference. A LOOK NUMBER, not a rule (as
     * {@code ConeShell.FRONT_RING_POINTS} is): at 12 blocks it draws 101 points.
     */
    public static final double RING_SPACING_BLOCKS = 0.75;

    /** The fewest points a ring is drawn with, however small it is. */
    public static final int RING_MIN_POINTS = 8;

    /** How far above the origin's feet the ring is drawn: at ground level, clear of the block's top face. */
    public static final double RING_LIFT_BLOCKS = 0.1;

    /**
     * How far apart a spiral arm's drawn points stand, ALONG THE ARM, in blocks (M2, CC'S CALL 2026-11-28; the
     * alternative was the ring's 0.75). A LOOK NUMBER: close enough that an arm reads as a line, not dots.
     */
    public static final double SPIRAL_SPACING_BLOCKS = 0.4;

    /**
     * Where arm 0 starts, in degrees from due east (+x), and the angle grows toward +z (south): CLOCKWISE seen from
     * above (M2, CC'S CALL and SEAT PLACEHOLDER 2026-11-28; the alternatives are any start and the other hand).
     */
    public static final double SPIRAL_START_DEGREES = 0.0;

    /**
     * THE ARM'S ANGLE AT FRONT RADIUS {@code r} (M2): {@code a0 + 2 pi x turns x (r / radius) + k x 2 pi / arms}, in
     * radians, {@code a0} = {@link #SPIRAL_START_DEGREES}. So at the edge every arm has wound {@code turns} whole turns.
     */
    public static double spiralAngle(int arm, int arms, double turns, double r, double radius) {
        return Math.toRadians(SPIRAL_START_DEGREES) + 2 * Math.PI * turns * (r / radius) + 2 * Math.PI * arm / arms;
    }

    /**
     * THE SPIRAL'S POINTS FOR ONE TICK OF THE FRONT (M2): along every arm, the stretch the front swept this tick, from
     * radius {@code from} (exclusive; the last tick's {@link #radiusAt}) to {@code to} (inclusive; this tick's), so tick
     * by tick the arms are drawn out with the front and its tip is always AT the radius the hit rule compares. Grouped by
     * arm (arm 0's points, then arm 1's, ...), each arm outward. With {@code to} 0 (the front's tick 0) it is the one
     * centre point. Lifted {@link #RING_LIFT_BLOCKS}, as the ring is.
     *
     * <p>Each arm's stretch is cut into {@code n} equal radius steps, {@code n} the stretch's length along the arm (the
     * arc of {@code r(theta)}, its slope {@code sqrt(1 + (r x dtheta/dr)^2)} taken at the stretch's outer end, where it
     * is steepest) over {@link #SPIRAL_SPACING_BLOCKS}, rounded up: so no two consecutive points are further apart than
     * the spacing.
     */
    public static List<Vec3> spiralPoints(Vec3 origin, double from, double to, int arms, double turns, double radius) {
        double y = origin.y() + RING_LIFT_BLOCKS;
        if (!(to > 0)) return List.of(new Vec3(origin.x(), y, origin.z()));
        double slope = Math.sqrt(1 + Math.pow(to * 2 * Math.PI * turns / radius, 2));
        int n = Math.max(1, (int) Math.ceil((to - from) * slope / SPIRAL_SPACING_BLOCKS));
        List<Vec3> out = new ArrayList<>(arms * n);
        for (int k = 0; k < arms; k++) {
            for (int i = 1; i <= n; i++) {
                double r = i == n ? to : from + (to - from) * i / n;
                double a = spiralAngle(k, arms, turns, r, radius);
                out.add(new Vec3(origin.x() + r * Math.cos(a), y, origin.z() + r * Math.sin(a)));
            }
        }
        return out;
    }

    /** The tick pulse {@code n} (1-based) fires on: {@code castTick + (n - 1) x interval}. */
    public static long pulseTick(long castTick, int n, int intervalTicks) {
        if (n < 1) throw new IllegalArgumentException("pulses are numbered from 1, was " + n);
        return castTick + (long) (n - 1) * intervalTicks;
    }

    /** The ring's radius {@code t} ticks after its pulse: {@code radius x t / travel}, and {@code radius} from {@code travel} on. */
    public static double radiusAt(int t, double radius, int travelTicks) {
        if (t <= 0) return 0.0;
        if (t >= travelTicks) return radius;
        return radius * t / travelTicks;
    }

    /** The distance a pulse's ring measures: horizontal, feet to feet. The vertical reach is the burst's box (the caller's query). */
    public static double horizontal(Vec3 origin, Vec3 at) {
        double dx = at.x() - origin.x();
        double dz = at.z() - origin.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * THE TICK THE RING FIRST REACHES {@code d}: the first {@code t} in {@code 0..travel} with
     * {@code radiusAt(t) >= d}, or {@code -1} when {@code d} is beyond {@code radius} (or not a number).
     */
    public static int reachTick(double d, double radius, int travelTicks) {
        if (!(d <= radius)) return -1;
        for (int t = 0; t <= travelTicks; t++) {
            if (radiusAt(t, radius, travelTicks) >= d) return t;
        }
        return -1;   // unreachable: radiusAt(travel) is radius, and d <= radius
    }

    /**
     * Where the ring is drawn at radius {@code r}: points round a horizontal circle about {@code origin}, lifted
     * {@link #RING_LIFT_BLOCKS}, spaced about {@link #RING_SPACING_BLOCKS} apart and at least {@link #RING_MIN_POINTS};
     * a ring of radius 0 is the one point at its centre.
     */
    public static List<Vec3> ringPoints(Vec3 origin, double r) {
        Vec3 centre = new Vec3(origin.x(), origin.y() + RING_LIFT_BLOCKS, origin.z());
        if (!(r > 0)) return List.of(centre);
        int n = Math.max(RING_MIN_POINTS, (int) Math.ceil(2 * Math.PI * r / RING_SPACING_BLOCKS));
        List<Vec3> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            out.add(new Vec3(centre.x() + r * Math.cos(a), centre.y(), centre.z() + r * Math.sin(a)));
        }
        return out;
    }
}
