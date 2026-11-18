package io.github.butterflysmp.rpg.core.fight;

/**
 * F1, THE FAST CLOSE: the ring moving from (C, border) to (M, mageRing) along one straight interpolation
 * (PLAN-dragon-fight.md section 2, the seat's mechanism ruling). Pure: every answer is a function of the fields and the
 * ticks elapsed since the landing.
 *
 * <p><b>The geometry.</b> At progress {@code p} from 0 to 1 the centre is {@code C + p(M - C)} and the radius is
 * {@code border - p(border - mageRing)}. A point on the ring at a fixed angle moves, per unit of p, by
 * {@code (M - C) - (border - mageRing) u}, {@code u} the unit vector at that angle; the fastest point moves
 * {@code d + (border - mageRing)} blocks per unit of p, {@code d} the distance C to M. So p advances at
 * {@code v_fast / (d + border - mageRing)} per second and no point outruns {@code v_fast}.
 *
 * <p>Only {@link #plan} makes one, so a path whose rings would not nest cannot exist.
 */
public final class FastClosePath {

    private final FightSettings settings;
    private final double centreXBlocks;
    private final double centreZBlocks;
    private final double mageXBlocks;
    private final double mageZBlocks;
    private final double distanceBlocks;

    private FastClosePath(FightSettings settings, double centreXBlocks, double centreZBlocks,
                          double mageXBlocks, double mageZBlocks, double distanceBlocks) {
        this.settings = settings;
        this.centreXBlocks = centreXBlocks;
        this.centreZBlocks = centreZBlocks;
        this.mageXBlocks = mageXBlocks;
        this.mageZBlocks = mageZBlocks;
        this.distanceBlocks = distanceBlocks;
    }

    /**
     * Plans the fast close for a Mage landing at M, or refuses it when M is further from C than
     * {@link FightSettings#maxLandingDistanceBlocks()} (the rings would not nest; PLAN section 2).
     */
    public static Landing plan(FightSettings settings, double centreXBlocks, double centreZBlocks,
                               double mageXBlocks, double mageZBlocks) {
        double d = Math.hypot(mageXBlocks - centreXBlocks, mageZBlocks - centreZBlocks);
        if (d > settings.maxLandingDistanceBlocks()) {
            return new Landing.TooFar(d, settings.maxLandingDistanceBlocks());
        }
        return new Landing.Accepted(new FastClosePath(settings, centreXBlocks, centreZBlocks,
                mageXBlocks, mageZBlocks, d));
    }

    public FightSettings settings() { return settings; }

    /** {@code d}: the horizontal distance from C to M, blocks. */
    public double distanceBlocks() { return distanceBlocks; }

    public double mageXBlocks() { return mageXBlocks; }

    public double mageZBlocks() { return mageZBlocks; }

    /** How far the ring's fastest point travels over the whole fast close, blocks: {@code d + border - mageRing}. */
    public double fastestPointTravelBlocks() {
        return distanceBlocks + settings.borderRadiusBlocks() - settings.mageRingRadiusBlocks();
    }

    /** How long the fast close lasts, ticks (not rounded): {@code (d + border - mageRing) / v_fast} seconds. */
    public double durationTicks() {
        return fastestPointTravelBlocks() * FightSettings.TICKS_PER_SECOND / settings.fastCloseBlocksPerSecond();
    }

    /** Progress, 0 to 1, this many ticks after the landing. Clamped at both ends. */
    public double progressAfter(long elapsedTicks) {
        if (elapsedTicks <= 0) return 0.0;
        double p = elapsedTicks * settings.fastCloseBlocksPerSecond()
                / (fastestPointTravelBlocks() * FightSettings.TICKS_PER_SECOND);
        return Math.min(1.0, p);
    }

    /** The first number of ticks after the landing at which progress has reached 1. */
    public long durationWholeTicks() {
        long e = (long) Math.ceil(durationTicks());
        // Settle any floating-point disagreement between the duration and progressAfter on the boundary.
        while (e > 0 && progressAfter(e - 1) >= 1.0) e--;
        while (progressAfter(e) < 1.0) e++;
        return e;
    }

    /** The ring this many ticks after the landing. */
    public Ring ringAfter(long elapsedTicks) {
        return ringAt(progressAfter(elapsedTicks));
    }

    /** The ring at progress {@code p} (clamped to 0..1). */
    public Ring ringAt(double p) {
        double q = Math.max(0.0, Math.min(1.0, p));
        double x = centreXBlocks + q * (mageXBlocks - centreXBlocks);
        double z = centreZBlocks + q * (mageZBlocks - centreZBlocks);
        double r = settings.borderRadiusBlocks()
                - q * (settings.borderRadiusBlocks() - settings.mageRingRadiusBlocks());
        return new Ring(x, z, r);
    }
}
