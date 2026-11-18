package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * AN INWARD SPIRAL, AS PURE GEOMETRY: the void implosion's look (the 2026-11-17 brief, Part D 5: "black and dark purple
 * particles spiralling INWARD from the reach to the origin over about 10 ticks"). Paper's {@code spiral} visual step
 * draws what this returns; the radius is handed in at draw time (the use's reach), so a stage-1 swirl is its own reach
 * wide and a stage-5 swirl its own.
 *
 * <p><b>THE CURVE.</b> Each of {@code arms} arms is ONE curve from the radius to the origin, parameterised by {@code s}
 * from 0 to 1: its radius is {@code R x (1 - s)} and its angle {@code phase + 360 x arm / arms + 360 x turns x s}
 * DEGREES (counter-clockwise seen from above with x east and z south, i.e. from +x toward +z). Over
 * {@code durationTicks} ticks the whole curve is drawn once, {@code pointsPerTick} points per arm per tick, in order:
 * tick 0 draws the outermost stretch and the last tick ends exactly at the origin. Offsets are FLAT (y 0); the caller
 * places them.
 *
 * <p><b>NAME THE QUANTITY:</b> {@code durationTicks} TICKS (at least 2); {@code arms} a whole number of arms;
 * {@code turns} FULL TURNS each arm sweeps from the reach to the origin; {@code pointsPerTick} particles per arm per
 * tick; {@code phaseDegrees} DEGREES every arm is turned by, so a second colour's arms can sit between the first's.
 */
public record Spiral(int durationTicks, int arms, double turns, int pointsPerTick, double phaseDegrees) {

    public Spiral {
        if (durationTicks < 2) throw new IllegalArgumentException("spiral duration_ticks must be at least 2");
        if (arms < 1) throw new IllegalArgumentException("spiral arms must be at least 1");
        if (!(turns >= 0 && Double.isFinite(turns))) throw new IllegalArgumentException("spiral turns must be 0 or more");
        if (pointsPerTick < 1) throw new IllegalArgumentException("spiral points_per_tick must be at least 1");
        if (!Double.isFinite(phaseDegrees)) throw new IllegalArgumentException("spiral phase_degrees must be a number");
    }

    /** RADIANS: arm {@code arm}'s angle at curve parameter {@code s} (0 at the reach, 1 at the origin). */
    public double angle(int arm, double s) {
        return Math.toRadians(phaseDegrees + 360.0 * arm / arms + 360.0 * turns * s);
    }

    /**
     * The FLAT offsets from the origin arm {@code arm} draws on tick {@code tick} (0-based), at {@code radius}:
     * {@code pointsPerTick} of them, outermost first. Point i of the whole curve (i from 0 to
     * {@code durationTicks x pointsPerTick - 1}) sits at {@code s = i / (that - 1)}, so the first is at the radius and
     * the last at the origin.
     */
    public List<Vec3> arm(double radius, int tick, int arm) {
        if (tick < 0 || tick >= durationTicks) {
            throw new IllegalArgumentException("tick " + tick + " is outside 0.." + (durationTicks - 1));
        }
        if (arm < 0 || arm >= arms) throw new IllegalArgumentException("arm " + arm + " is outside 0.." + (arms - 1));
        int last = durationTicks * pointsPerTick - 1;
        List<Vec3> out = new ArrayList<>(pointsPerTick);
        for (int k = 0; k < pointsPerTick; k++) {
            double s = (double) (tick * pointsPerTick + k) / last;
            double r = radius * (1.0 - s);
            double a = angle(arm, s);
            out.add(new Vec3(r * Math.cos(a), 0.0, r * Math.sin(a)));
        }
        return out;
    }
}
