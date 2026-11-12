package io.github.butterflysmp.rpg.core.mining.spike;

import java.util.Locale;

/**
 * The arithmetic of a spot on a block face, in 1/16 of a block. SPIKE CODE (spike/mining-input).
 *
 * <p>Ben: the visible spot is 3x3 and its hit box 5x5, in sixteenths. Both are centred on one
 * PIXEL CENTRE, {@code (k + 0.5) / 16}, so both are pixel-aligned: the 3x3 covers pixels
 * {@code k-1..k+1} and the 5x5 covers {@code k-2..k+2}. {@code k} is clamped to {@code 2..13}, so
 * the 5x5 never overhangs the face.
 *
 * <p>Face axes are WORLD-aligned, not mirrored per facing: NORTH and SOUTH use (u, v) = (x, y),
 * EAST and WEST use (z, y), and UP and DOWN use (x, z). An offset is therefore read along a world
 * axis, which is all the aim line needs.
 */
public final class FacePoint {

    /** Sixteenths per block. */
    public static final int PX = 16;
    /** Half the hit box, in sixteenths (Ben: 5x5). */
    public static final double BOX_HALF_PX = 2.5;
    /** How far the spot sits off the face, in blocks, so it does not z-fight the block. */
    public static final double OUTSET = 0.001;

    private FacePoint() {}

    /** A block face. {@code nx, ny, nz} is the outward normal. */
    public enum Face {
        NORTH(0, 0, -1, 0, 1), SOUTH(0, 0, 1, 0, 1),
        WEST(-1, 0, 0, 2, 1), EAST(1, 0, 0, 2, 1),
        DOWN(0, -1, 0, 0, 2), UP(0, 1, 0, 0, 2);

        public final int nx, ny, nz;
        /** The world axis index (0 = x, 1 = y, 2 = z) of u and of v. */
        final int uAxis, vAxis;

        Face(int nx, int ny, int nz, int uAxis, int vAxis) {
            this.nx = nx; this.ny = ny; this.nz = nz;
            this.uAxis = uAxis; this.vAxis = vAxis;
        }

        int normalAxis() { return nx != 0 ? 0 : ny != 0 ? 1 : 2; }
        int normalSign() { return nx + ny + nz; }
    }

    /**
     * A spot: its face, the pixel indices of its centre, and its world centre (already set
     * {@link #OUTSET} off the face).
     */
    public record Spot(Face face, int pu, int pv, double x, double y, double z) {}

    /** Where a ray met the face, relative to the spot centre, in sixteenths. */
    public record Aim(double du, double dv, boolean inBox) {
        public String text() {
            return String.format(Locale.ROOT, "%+.2f,%+.2f,%s", du, dv, inBox ? "in" : "out");
        }
    }

    /** Snap a hit on block (bx, by, bz)'s {@code face} to the nearest legal spot centre. */
    public static Spot snap(int bx, int by, int bz, Face face, double hx, double hy, double hz) {
        int[] block = {bx, by, bz};
        double[] hit = {hx, hy, hz};
        int pu = pixel(hit[face.uAxis] - block[face.uAxis]);
        int pv = pixel(hit[face.vAxis] - block[face.vAxis]);

        double[] c = new double[3];
        c[face.uAxis] = block[face.uAxis] + (pu + 0.5) / PX;
        c[face.vAxis] = block[face.vAxis] + (pv + 0.5) / PX;
        int n = face.normalAxis();
        double plane = block[n] + (face.normalSign() > 0 ? 1 : 0);
        c[n] = plane + face.normalSign() * OUTSET;
        return new Spot(face, pu, pv, c[0], c[1], c[2]);
    }

    /**
     * The centre of block (bx, by, bz)'s {@code face}, set {@code outset} blocks off it. The patch
     * model sits here, at half the spot's {@link #OUTSET}, so the spot is drawn in front of it.
     */
    public static double[] centre(int bx, int by, int bz, Face face, double outset) {
        int[] block = {bx, by, bz};
        double[] c = {bx + 0.5, by + 0.5, bz + 0.5};
        int n = face.normalAxis();
        c[n] = block[n] + (face.normalSign() > 0 ? 1 : 0) + face.normalSign() * outset;
        return c;
    }

    /** The offset of a hit on the spot's face from the spot centre, and whether it is in the 5x5. */
    public static Aim aim(Spot spot, double hx, double hy, double hz) {
        double[] hit = {hx, hy, hz};
        double[] c = {spot.x(), spot.y(), spot.z()};
        double du = (hit[spot.face().uAxis] - c[spot.face().uAxis]) * PX;
        double dv = (hit[spot.face().vAxis] - c[spot.face().vAxis]) * PX;
        return new Aim(du, dv, Math.abs(du) <= BOX_HALF_PX && Math.abs(dv) <= BOX_HALF_PX);
    }

    /**
     * The unit quaternion {x, y, z, w} that turns +Z (a flat model's facing) onto {@code face}'s
     * normal. Whether a client draws an ITEM_DISPLAY's model facing +Z under that rotation is client
     * behaviour, and gate MS7 reads it.
     */
    public static float[] rotation(Face face) {
        double half = Math.sqrt(0.5);
        return switch (face) {
            case SOUTH -> new float[]{0f, 0f, 0f, 1f};
            case NORTH -> new float[]{0f, 1f, 0f, 0f};                     // 180 about Y
            case EAST -> new float[]{0f, (float) half, 0f, (float) half};   // +90 about Y
            case WEST -> new float[]{0f, (float) -half, 0f, (float) half};  // -90 about Y
            case UP -> new float[]{(float) -half, 0f, 0f, (float) half};    // -90 about X
            case DOWN -> new float[]{(float) half, 0f, 0f, (float) half};   // +90 about X
        };
    }

    private static int pixel(double local) {
        int k = (int) Math.floor(local * PX);
        return Math.max(2, Math.min(13, k));
    }
}
