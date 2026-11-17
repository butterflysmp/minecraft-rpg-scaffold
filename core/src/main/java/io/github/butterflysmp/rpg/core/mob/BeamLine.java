package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * THE BEAM'S LINE: a SEGMENT from {@code origin} along the unit {@code direction} for {@code length} BLOCKS
 * (PLAN-dragon-mobs.md step R5). The shape the Knell's beam and, later, the Knell Beam weapon share
 * ({@code PLAN-legacy-port.md} section 2.5, E5: the aim is locked at the start, then a segment is tested at the end).
 *
 * <p><b>NAME THE QUANTITY:</b> {@code length} is BLOCKS along the line from the origin (the Knell's EYE): the range,
 * or less where a block clips it. A body is MET when its box, grown by the tolerance on every side, shares at least
 * one point with the segment, the segment's two ends and the box's faces included. The {@link #gap} is the smallest
 * growth that would make it meet: the beam hits exactly when {@code gap <= tolerance}. Because the growth is a BOX's
 * (every face out by the same amount), the gap is the largest of the three axis distances at the segment's closest
 * point, never a Euclidean distance: a body 1.0 off on two axes has a gap of 1.0, not 1.414.
 */
public record BeamLine(Vec3 origin, Vec3 direction, double length) {

    /** The bisection's steps for {@link #gap}: 2^-60 of the starting bracket, far below the log's three places. */
    private static final int GAP_STEPS = 60;

    public BeamLine {
        if (!(length >= 0 && Double.isFinite(length))) {
            throw new IllegalArgumentException("a beam's length must be 0 or more blocks, was " + length);
        }
        if (Math.abs(direction.length() - 1.0) > 1e-9) {
            throw new IllegalArgumentException("a beam's direction must be a unit vector, was " + direction);
        }
    }

    /** The line from {@code origin} through {@code toward}, running on for {@code length} blocks. */
    public static BeamLine aimed(Vec3 origin, Vec3 toward, double length) {
        Vec3 d = toward.subtract(origin);
        if (d.lengthSquared() == 0) {
            throw new IllegalArgumentException("a beam cannot be aimed at its own origin " + origin);
        }
        return new BeamLine(origin, d.normalize(), length);
    }

    /** The same line, cut to at most {@code atMost} blocks (where a block stops it). Never longer, never negative. */
    public BeamLine clip(double atMost) {
        return new BeamLine(origin, direction, Math.max(0.0, Math.min(length, atMost)));
    }

    public Vec3 at(double s) {
        return origin.add(direction.scale(s));
    }

    public Vec3 end() {
        return at(length);
    }

    /**
     * Does the segment meet {@code box} grown by {@code tolerance} blocks on every side? Inclusive: touching a face, or
     * reaching it with the segment's very end, counts. The slab test: the segment's parameter range inside each axis's
     * slab, intersected over the three axes, must not be empty.
     */
    public boolean meets(BeamBox box, double tolerance) {
        BeamBox g = box.grow(tolerance);
        double lo = 0.0;
        double hi = length;
        double[] o = {origin.x(), origin.y(), origin.z()};
        double[] d = {direction.x(), direction.y(), direction.z()};
        double[] min = {g.min().x(), g.min().y(), g.min().z()};
        double[] max = {g.max().x(), g.max().y(), g.max().z()};
        for (int i = 0; i < 3; i++) {
            if (d[i] == 0.0) {
                // Parallel to this slab: inside it everywhere or nowhere.
                if (o[i] < min[i] || o[i] > max[i]) return false;
                continue;
            }
            double t1 = (min[i] - o[i]) / d[i];
            double t2 = (max[i] - o[i]) / d[i];
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
            lo = Math.max(lo, t1);
            hi = Math.min(hi, t2);
            if (lo > hi) return false;
        }
        return true;
    }

    /**
     * The smallest growth of {@code box}, in blocks, at which the segment meets it: 0 when it already does. Found by
     * bisecting {@link #meets}, so the gap and the decision can never disagree by more than the bisection's 2^-60 of
     * its bracket: the LOG's number, never the decision's. The bracket's top is the distance from the origin to the
     * box's far corner, at which the grown box contains the origin.
     */
    public double gap(BeamBox box) {
        if (meets(box, 0.0)) return 0.0;
        double lo = 0.0;
        double hi = Math.max(origin.subtract(box.min()).length(), origin.subtract(box.max()).length())
                + box.max().subtract(box.min()).length();
        for (int i = 0; i < GAP_STEPS; i++) {
            double mid = (lo + hi) / 2.0;
            if (meets(box, mid)) hi = mid; else lo = mid;
        }
        return hi;
    }

    /** The point of the segment nearest {@code p}: its projection onto the line, clamped to the segment's ends. */
    public Vec3 nearestTo(Vec3 p) {
        double s = p.subtract(origin).dot(direction);
        return at(Math.max(0.0, Math.min(length, s)));
    }
}
