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

    /**
     * Every point of one redraw at the fight's {@code fightTick}, height by height. Row {@code j} (0 the lowest) starts
     * {@code j x rowShiftBlocks + turnBlocksPerSecond x seconds} along the circumference from angle 0, and goes once
     * round: the shift twists the wall, the turn rotates all of it. Both are lengths ALONG the ring, so a smaller ring
     * turns through a larger angle at the same speed. With {@link RingLook#alternateTurns()}, every odd row turns the
     * other way (Ben: "maybe alternate twists on every other row").
     */
    public static List<double[]> points(Ring ring, double groundYBlocks, RingLook look, long fightTick) {
        int n = pointsPerHeight(ring.radiusBlocks(), look);
        List<double[]> out = new ArrayList<>(n * look.heightOffsetsBlocks().size());
        if (n == 0) return out;
        double turnedBlocks = look.turnBlocksPerSecond() * fightTick / FightSettings.TICKS_PER_SECOND;
        int row = 0;
        for (double offset : look.heightOffsetsBlocks()) {
            double y = groundYBlocks + offset;
            double turnSign = look.alternateTurns() && row % 2 == 1 ? -1.0 : 1.0;
            double startAngle = (row * look.rowShiftBlocks() + turnSign * turnedBlocks) / ring.radiusBlocks();
            row++;
            for (int i = 0; i < n; i++) {
                double a = startAngle + 2.0 * Math.PI * i / n;
                out.add(new double[] {
                        ring.centreXBlocks() + ring.radiusBlocks() * Math.cos(a),
                        y,
                        ring.centreZBlocks() + ring.radiusBlocks() * Math.sin(a)});
            }
        }
        return out;
    }
}
