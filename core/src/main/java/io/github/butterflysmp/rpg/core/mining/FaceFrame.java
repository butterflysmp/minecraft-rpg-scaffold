package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A slot's wall geometry: its footprint blocks, and where a look ray lands in patch pixels.
 *
 * <p>The anchor is the footprint's BOTTOM-CENTRE block as the player sees the face. The footprint
 * runs {@code width / 2} blocks to each side (integer division) and {@code height - 1} blocks up.
 * For the Small vein (3 x 2) that is the anchor, one block left, one right, and the three above
 * (PLAN-mining.md 5.1).
 *
 * <p>The patch plane is the footprint's OUTER face. Its origin, patch pixel (0, 0), is that face's
 * bottom-left corner as the player sees it.
 *
 * @param ax     anchor block X
 * @param ay     anchor block Y (the footprint's bottom row)
 * @param az     anchor block Z
 * @param face   the wall face the patch sits on
 * @param width  footprint width in blocks, along the face
 * @param height footprint height in blocks
 */
public record FaceFrame(int ax, int ay, int az, WallFace face, int width, int height) {

    public FaceFrame {
        if (face == null) throw new IllegalArgumentException("a face frame needs a face");
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("a face frame is at least 1 x 1, got " + width + " x " + height);
        }
    }

    /** The left-most footprint column's block, as the player sees the face. */
    private int leftX() { return ax - (width / 2) * face.rx; }
    private int leftZ() { return az - (width / 2) * face.rz; }

    /**
     * Every footprint block, row by row from the bottom, each row from left to right as the player
     * sees it. The order is stable so a test can pin it.
     */
    public List<BlockPos> footprint() {
        List<BlockPos> blocks = new ArrayList<>(width * height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                blocks.add(new BlockPos(leftX() + col * face.rx, ay + row, leftZ() + col * face.rz));
            }
        }
        return blocks;
    }

    /** The patch origin (pixel 0, 0) on the plane: the footprint face's bottom-left as the player sees it. */
    private double originX() { return face.rx != 0 ? leftX() + (face.rx > 0 ? 0 : 1) : planeX(); }
    private double originZ() { return face.rz != 0 ? leftZ() + (face.rz > 0 ? 0 : 1) : planeZ(); }
    private double planeX() { return ax + (face.nx > 0 ? 1 : 0); }
    private double planeZ() { return az + (face.nz > 0 ? 1 : 0); }

    /**
     * The world point of patch pixel coordinates {@code (u, v)}, moved {@code outset} blocks off the
     * wall along the outward normal. The inverse of {@link #worldToPatch} on the plane; the round
     * trip is pinned by {@code FaceFrameWorldTest}.
     *
     * @return {@code {x, y, z}}
     */
    public double[] patchToWorld(double u, double v, double outset) {
        double x = originX() + face.rx * u / 16.0 + face.nx * outset;
        double y = ay + v / 16.0;
        double z = originZ() + face.rz * u / 16.0 + face.nz * outset;
        return new double[]{x, y, z};
    }

    /** The middle of the footprint's face, {@code outset} blocks off the wall. */
    public double[] centre(double outset) {
        return patchToWorld(width * 8.0, height * 8.0, outset);
    }

    /**
     * Where the ray from the eye along {@code (dx, dy, dz)} meets the patch plane, in patch pixels.
     * The direction need not be normalised.
     *
     * @return empty when the ray is parallel to the plane, points away from it, or meets it only
     *         behind the eye
     */
    public Optional<PatchPoint> worldToPatch(double ex, double ey, double ez,
                                             double dx, double dy, double dz) {
        // A point on the plane, and the plane's origin corner. Along the normal, the outer face is
        // the block's high side when the normal is positive and its low side when negative. Along
        // the right axis, u = 0 is the left column's edge that is furthest AGAINST the right axis.
        double originX = originX();
        double originZ = originZ();
        double originY = ay;

        double denom = face.nx * dx + face.nz * dz;
        // A ray that reaches the outer face from in front travels AGAINST the normal (denom < 0).
        // Zero is parallel; positive points away from the wall.
        if (denom >= 0) return Optional.empty();

        double t = (face.nx * (originX - ex) + face.nz * (originZ - ez)) / denom;
        if (t <= 0) return Optional.empty();   // the plane is behind the eye

        double hx = ex + t * dx;
        double hy = ey + t * dy;
        double hz = ez + t * dz;
        double u = (face.rx * (hx - originX) + face.rz * (hz - originZ)) * 16.0;
        double v = (hy - originY) * 16.0;
        return Optional.of(new PatchPoint(u, v));
    }
}
