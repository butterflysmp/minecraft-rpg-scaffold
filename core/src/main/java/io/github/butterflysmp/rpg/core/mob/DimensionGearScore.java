package io.github.butterflysmp.rpg.core.mob;

/**
 * A mob's gear score from its dimension (M25, Ben, 2026-09-28), with the one distance term M25 keeps, the
 * End's. Pure, so the whole grid is pinned in the 2-second loop.
 *
 * <pre>
 *   OVERWORLD   100                                    at every distance
 *   NETHER      200                                    at every distance
 *   END         300 + max(0, d - 1000) / 100, <= 500   300 to 1,000 blocks; 350 at 6,000; 400 at 11,000;
 *                                                      500 from 21,000
 * </pre>
 *
 * <p><b>M25 REPLACES the old distance curves</b> (M3's +100 per 2,500 blocks, M11's 8x Nether rate, M12's
 * End rate). They lived in this class under its old name, {@code DistanceGearScore}. It was renamed
 * because two of the three dimensions no longer have a distance term; the dimension decides.
 *
 * <p>The End curve being CONTINUOUS, rounded to a whole number and clamped at {@link MobGearScore#CAP} is
 * the seat's default, not a ruling. The rounding and the clamp commute, because 500 is a whole number;
 * this rounds first.
 *
 * <p><b>Where the End's distance is measured FROM is not decided here.</b> It is (0, 0) (M23), chosen by
 * paper's {@code MobOrigin}; this only ever sees the distance. The overworld and Nether ignore theirs.
 */
public final class DimensionGearScore {

    private DimensionGearScore() {}

    /** M25: every hostile overworld mob. */
    static final int OVERWORLD = 100;
    /** M25: every hostile Nether mob. */
    static final int NETHER = 200;
    /** M25: the End's start, held out to {@link #END_FLAT_RADIUS}. */
    static final int END_START = 300;
    /** M25: blocks from (0, 0) within which the End stays at its start. */
    static final double END_FLAT_RADIUS = 1_000.0;
    /** M25: blocks per +1 GS past the flat radius (+100 per 10,000). */
    static final double END_BLOCKS_PER_POINT = 100.0;

    public static int of(MobDimension dimension, double horizontalDistance) {
        return switch (dimension) {
            case OVERWORLD -> OVERWORLD;
            case NETHER -> NETHER;
            case END -> end(horizontalDistance);
        };
    }

    private static int end(double horizontalDistance) {
        // A NaN or negative distance is a caller bug; it FAILS SOFT to the start rather than to
        // Math.round(NaN) == 0, which would seed a GS 0 mob -- zero health, born dead.
        double d = horizontalDistance >= 0 ? horizontalDistance : 0.0;
        double raw = END_START + Math.max(0.0, d - END_FLAT_RADIUS) / END_BLOCKS_PER_POINT;
        return (int) Math.min(MobGearScore.CAP, Math.round(raw));
    }

    /** Horizontal Euclidean distance -- a circle around the origin (M19). Y is ignored. */
    public static double horizontalDistance(double x, double z, double originX, double originZ) {
        return Math.hypot(x - originX, z - originZ);
    }
}
