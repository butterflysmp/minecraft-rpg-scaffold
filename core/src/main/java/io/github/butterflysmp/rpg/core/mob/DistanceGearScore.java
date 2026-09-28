package io.github.butterflysmp.rpg.core.mob;

/**
 * A mob's gear score from how far it is from its world's origin: the blanket rule (M3), the three
 * curves (M11, M12), the circle (M19) and the cap (M20). Pure, so the whole grid is pinned in the
 * 2-second loop.
 *
 * <pre>
 *   OVERWORLD   min(500, 100 + d / 25)       2,500 blocks -> 200    caps at 10,000
 *   NETHER      min(500, 200 + d * 8 / 25)   2,500 blocks -> 500    caps at 937.5
 *   END         min(500, 300 + d / 25)       2,500 blocks -> 400    caps at 5,000
 * </pre>
 *
 * <p><b>Multiply before dividing, as the ruling is written.</b> The Nether is {@code d * 8 / 25}, not
 * {@code d * 0.32}: 0.32 has no exact double, and the two can round differently.
 *
 * <p>Continuous, not stepped, and rounded to a whole number -- both are the seat's defaults, not
 * rulings. The order of the rounding and the clamp cannot change the result, because 500 is a whole
 * number; this rounds first.
 *
 * <p><b>Where the distance is measured FROM is not decided here.</b> The overworld and the Nether use
 * the world's spawn point and the End uses (0, 0) (M23); paper's {@code MobOrigin} chooses, and this
 * only ever sees the distance.
 */
public final class DistanceGearScore {

    private DistanceGearScore() {}

    /** Blocks per +1 GS on the normal rate: 2,500 blocks is +100. */
    private static final double BLOCKS_PER_POINT = 25.0;

    public static int of(MobDimension dimension, double horizontalDistance) {
        // A NaN or negative distance is a caller bug; it FAILS SOFT to the curve's start rather than
        // to Math.round(NaN) == 0, which would seed a GS 0 mob -- zero health, born dead.
        double d = horizontalDistance >= 0 ? horizontalDistance : 0.0;
        double raw = switch (dimension) {
            case OVERWORLD -> 100 + d / BLOCKS_PER_POINT;
            case NETHER -> 200 + d * 8 / BLOCKS_PER_POINT;
            case END -> 300 + d / BLOCKS_PER_POINT;
        };
        return (int) Math.min(MobGearScore.CAP, Math.round(raw));
    }

    /** Horizontal Euclidean distance -- a circle around the origin (M19). Y is ignored. */
    public static double horizontalDistance(double x, double z, double originX, double originZ) {
        return Math.hypot(x - originX, z - originZ);
    }
}
