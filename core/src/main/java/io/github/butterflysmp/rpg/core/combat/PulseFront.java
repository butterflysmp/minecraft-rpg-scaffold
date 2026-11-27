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
