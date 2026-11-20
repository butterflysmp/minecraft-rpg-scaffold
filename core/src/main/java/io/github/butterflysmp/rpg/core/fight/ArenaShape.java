package io.github.butterflysmp.rpg.core.fight;

import java.util.ArrayList;
import java.util.List;

/**
 * THE GENERATED STAND-IN ARENA (the F8 brief, section 4 b; Ben, 2026-11-18: "Yes that's fine for now"). Pure: the
 * shape, its blocks and its crystal spots, so the counts the gate predicts are executed, not worked out by hand. Every
 * number is the content's ({@code content/fights/dragon.yml}, {@code arena:}; SEAT PLACEHOLDERS).
 * <ul>
 *   <li><b>The floor:</b> a disc of {@code discRadius} about the centre column, {@code discThickness} blocks thick, its
 *       TOP block at {@code floorY}. A column is in the disc when {@code dx^2 + dz^2 <= discRadius^2} (block
 *       offsets from the centre column).</li>
 *   <li><b>The pillars:</b> {@code pillarCount} square pillars {@code pillarWidth} on a side (odd, so each has a centre
 *       block), their centre columns on a circle of {@code pillarRingRadius}, rounded to the nearest block; pillar 1
 *       due east (+x), the rest anticlockwise seen from above (+z for a positive angle). Each rises from the block above
 *       the floor to {@code floorY + pillarTopAboveFloor}.</li>
 *   <li><b>Crystal spot n</b> stands on pillar n: its centre column's middle, on the block above its top.</li>
 * </ul>
 *
 * <p>The template-file source (PLAN-dragon-mobs.md C3) is NOT built: no file exists to test it. This is the generated
 * source only; the seam is the paper side's.
 */
public record ArenaShape(int centreX, int centreZ, int floorY, int discRadius, int discThickness, int pillarCount,
                         double pillarRingRadius, int pillarWidth, int pillarTopAboveFloor) {

    /** What a block is. */
    public enum Kind { FLOOR, PILLAR }

    /** Receives each block of the shape. */
    @FunctionalInterface
    public interface Sink {
        void block(int x, int y, int z, Kind kind);
    }

    public ArenaShape {
        if (discRadius < 1) throw new IllegalArgumentException("discRadius must be >= 1: " + discRadius);
        if (discThickness < 1) throw new IllegalArgumentException("discThickness must be >= 1: " + discThickness);
        if (pillarCount < 1) throw new IllegalArgumentException("pillarCount must be >= 1: " + pillarCount);
        if (pillarWidth < 1 || pillarWidth % 2 == 0) {
            throw new IllegalArgumentException("pillarWidth must be odd and >= 1: " + pillarWidth);
        }
        if (pillarTopAboveFloor < 1) {
            throw new IllegalArgumentException("pillarTopAboveFloor must be >= 1: " + pillarTopAboveFloor);
        }
        if (!(pillarRingRadius > 0.0) || !Double.isFinite(pillarRingRadius)) {
            throw new IllegalArgumentException("pillarRingRadius must be finite and > 0: " + pillarRingRadius);
        }
        // Every column of every pillar must stand on the disc: the farthest corner of a pillar is its rounded centre's
        // distance plus the half-width's diagonal, checked block by block below.
        int half = pillarWidth / 2;
        for (int[] c : centres(pillarCount, pillarRingRadius)) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    int x = c[0] + dx;
                    int z = c[1] + dz;
                    if ((long) x * x + (long) z * z > (long) discRadius * discRadius) {
                        throw new IllegalArgumentException("a pillar column at (" + x + ", " + z
                                + ") from the centre hangs off a disc of radius " + discRadius);
                    }
                }
            }
        }
    }

    private static List<int[]> centres(int count, double ringRadius) {
        List<int[]> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = 2.0 * Math.PI * i / count;
            out.add(new int[] {(int) Math.round(ringRadius * Math.cos(a)), (int) Math.round(ringRadius * Math.sin(a))});
        }
        return out;
    }

    /** Is this column (world block x, z) part of the disc? */
    public boolean isFloorColumn(int x, int z) {
        long dx = x - centreX;
        long dz = z - centreZ;
        return dx * dx + dz * dz <= (long) discRadius * discRadius;
    }

    public int floorTopY() { return floorY; }

    public int floorBottomY() { return floorY - discThickness + 1; }

    /** The pillars' top block's y. */
    public int pillarTopY() { return floorY + pillarTopAboveFloor; }

    /** C's ground for the ring and the Mage: the block a player's feet stand in on the floor. */
    public double groundY() { return floorY + 1; }

    /**
     * The layout the fight reads from this GENERATED arena (the seat's queue, item 2): exactly the numbers it read from
     * the shape before, so no row predicted against the generated arena moves. The probe scans the disc, up to three
     * blocks above the pillars' top.
     */
    public ArenaLayout layout() {
        List<double[]> spots = new ArrayList<>(pillarCount);
        for (int n = 1; n <= pillarCount; n++) spots.add(crystalSpot(n));
        return new ArenaLayout(centreX, centreZ, floorY, spots, discRadius, pillarTopAboveFloor + 3);
    }

    /** The pillars' centre columns, world block x and z, pillar 1 first. */
    public List<int[]> pillarCentres() {
        List<int[]> out = new ArrayList<>(pillarCount);
        for (int[] c : centres(pillarCount, pillarRingRadius)) out.add(new int[] {centreX + c[0], centreZ + c[1]});
        return out;
    }

    /** Crystal spot n (1-based) on pillar n: {x, y, z}, blocks. */
    public double[] crystalSpot(int n) {
        if (n < 1 || n > pillarCount) throw new IllegalArgumentException("no pillar " + n + " of " + pillarCount);
        int[] c = pillarCentres().get(n - 1);
        return new double[] {c[0] + 0.5, pillarTopY() + 1, c[1] + 0.5};
    }

    public long floorColumns() {
        long n = 0;
        for (int dx = -discRadius; dx <= discRadius; dx++) {
            for (int dz = -discRadius; dz <= discRadius; dz++) {
                if ((long) dx * dx + (long) dz * dz <= (long) discRadius * discRadius) n++;
            }
        }
        return n;
    }

    public long floorBlocks() { return floorColumns() * discThickness; }

    public long pillarBlocks() { return (long) pillarCount * pillarWidth * pillarWidth * pillarTopAboveFloor; }

    public long totalBlocks() { return floorBlocks() + pillarBlocks(); }

    /** Every block of the shape: the disc, then the pillars. */
    public void forEachBlock(Sink sink) {
        for (int dx = -discRadius; dx <= discRadius; dx++) {
            for (int dz = -discRadius; dz <= discRadius; dz++) {
                if ((long) dx * dx + (long) dz * dz > (long) discRadius * discRadius) continue;
                for (int y = floorBottomY(); y <= floorY; y++) sink.block(centreX + dx, y, centreZ + dz, Kind.FLOOR);
            }
        }
        int half = pillarWidth / 2;
        for (int[] c : pillarCentres()) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    for (int y = floorY + 1; y <= pillarTopY(); y++) sink.block(c[0] + dx, y, c[1] + dz, Kind.PILLAR);
                }
            }
        }
    }
}
