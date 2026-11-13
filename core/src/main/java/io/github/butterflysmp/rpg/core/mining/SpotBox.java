package io.github.butterflysmp.rpg.core.mining;

/**
 * The hit test: is a patch point inside a spot's 5 x 5 box?
 *
 * <p>Ben: <i>"The visible weak spot will be 3x3 but the hit box for it can be 5x5"</i> (units of
 * 1/16 block). A spot centred on pixel {@code cu} has a box covering pixels {@code cu-2..cu+2},
 * which is the half-open fractional range {@code [cu - 2, cu + 3)}. <b>The low edge is in and the
 * high edge is out</b>, so two adjacent boxes never both claim a point (PLAN-mining.md 5.2).
 */
public final class SpotBox {

    /** The hit box's side, in pixels. */
    public static final int BOX_PX = 5;
    /** The visible mark's side, in pixels. */
    public static final int MARK_PX = 3;

    /** Pixels from the centre pixel to the box's low edge: (5 - 1) / 2 = 2. */
    private static final int HALF = (BOX_PX - 1) / 2;

    private SpotBox() {}

    public static boolean contains(Spot spot, PatchPoint p) {
        double lowU = spot.cu() - HALF, highU = spot.cu() + HALF + 1;
        double lowV = spot.cv() - HALF, highV = spot.cv() + HALF + 1;
        return p.u() >= lowU && p.u() < highU
                && p.v() >= lowV && p.v() < highV;
    }
}
