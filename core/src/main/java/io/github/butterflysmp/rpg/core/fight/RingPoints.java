package io.github.butterflysmp.rpg.core.fight;

import java.util.ArrayList;
import java.util.List;

/**
 * F4: WHERE THE RING'S PARTICLES GO. Pure: a ring, the ground height at C and a {@link RingLook} in; points out. Each
 * point is {@code {x, y, z}}, blocks, in the world's coordinates.
 */
public final class RingPoints {

    private RingPoints() {}

    /**
     * Points at one height: enough that neighbours are at most {@code pointSpacingBlocks} apart along the
     * circumference, i.e. {@code ceil(2 pi r / spacing)}. A closed ring (radius 0) has none.
     */
    public static int pointsPerHeight(double radiusBlocks, RingLook look) {
        if (radiusBlocks <= 0.0) return 0;
        return (int) Math.ceil(2.0 * Math.PI * radiusBlocks / look.pointSpacingBlocks());
    }

    /** Points in one redraw: {@link #pointsPerHeight} at each of the look's heights. */
    public static int pointsPerRedraw(double radiusBlocks, RingLook look) {
        return pointsPerHeight(radiusBlocks, look) * look.heightOffsetsBlocks().size();
    }

    /** Every point of one redraw, height by height, each height going once round from angle 0. */
    public static List<double[]> points(Ring ring, double groundYBlocks, RingLook look) {
        int n = pointsPerHeight(ring.radiusBlocks(), look);
        List<double[]> out = new ArrayList<>(n * look.heightOffsetsBlocks().size());
        for (double offset : look.heightOffsetsBlocks()) {
            double y = groundYBlocks + offset;
            for (int i = 0; i < n; i++) {
                double a = 2.0 * Math.PI * i / n;
                out.add(new double[] {
                        ring.centreXBlocks() + ring.radiusBlocks() * Math.cos(a),
                        y,
                        ring.centreZBlocks() + ring.radiusBlocks() * Math.sin(a)});
            }
        }
        return out;
    }
}
