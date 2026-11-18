package io.github.butterflysmp.rpg.core.fight;

/**
 * F1, THE RETURN after the Mage's death: the close run backwards (Ben, 2026-11-18: "an inverse of it's shrinking
 * instead of a snap back"). First, if he died in the slow close, the radius rises back to the Mage ring's radius at
 * {@code v_slow} about M; then the fast close runs in reverse, tick for tick, from wherever it stood, until the ring
 * is back at (C, border).
 *
 * <p><b>Positions are counted in whole ticks of the fast close</b>: the return's fast part at {@code k} ticks in shows
 * exactly the ring the fast close showed {@code k} ticks before the point it is reversing from. So the return visits
 * the close's positions in reverse by construction, at the same fastest-point cap, and its home tick is exact.
 *
 * <p>The return's speeds are a SEAT PLACEHOLDER (the same as the close's), PLAN-dragon-fight.md section 2.
 */
public final class ReturnPath {

    private final FastClosePath path;
    private final long startTick;
    private final double startRadiusBlocks;
    private final long slowPartTicks;
    private final long startFastTicks;

    private ReturnPath(FastClosePath path, long startTick, double startRadiusBlocks, long slowPartTicks,
                       long startFastTicks) {
        this.path = path;
        this.startTick = startTick;
        this.startRadiusBlocks = startRadiusBlocks;
        this.slowPartTicks = slowPartTicks;
        this.startFastTicks = startFastTicks;
    }

    /**
     * The Mage died in the slow close at {@code tick}, the ring at {@code radiusBlocks} about M. The slow part lasts
     * until the first whole tick at which the radius is back at the Mage ring's radius; the fast part then reverses
     * the whole fast close.
     */
    public static ReturnPath fromSlowClose(FastClosePath path, long tick, double radiusBlocks) {
        FightSettings s = path.settings();
        long slow = 0;
        while (radiusAfterSlow(s, radiusBlocks, slow) < s.mageRingRadiusBlocks()) slow++;
        return new ReturnPath(path, tick, radiusBlocks, slow, path.durationWholeTicks());
    }

    /**
     * The Mage died in the fast close at {@code tick}, {@code fastTicksElapsed} ticks after his landing: only the fast
     * part runs, reversing those ticks.
     */
    public static ReturnPath fromFastClose(FastClosePath path, long tick, long fastTicksElapsed) {
        return new ReturnPath(path, tick, Double.NaN, 0L, Math.max(0L, fastTicksElapsed));
    }

    private static double radiusAfterSlow(FightSettings s, double startRadiusBlocks, long ticks) {
        return startRadiusBlocks + s.slowCloseBlocksPerSecond() * ticks / FightSettings.TICKS_PER_SECOND;
    }

    /** The ring at {@code tick}. */
    public Ring ringAt(long tick) {
        long elapsed = Math.max(0L, tick - startTick);
        if (elapsed < slowPartTicks) {
            double r = Math.min(path.settings().mageRingRadiusBlocks(),
                    radiusAfterSlow(path.settings(), startRadiusBlocks, elapsed));
            return new Ring(path.mageXBlocks(), path.mageZBlocks(), r);
        }
        return path.ringAfter(startFastTicks - (elapsed - slowPartTicks));
    }

    /** The first tick at which the ring is back at (C, border). */
    public long homeTick() {
        return startTick + slowPartTicks + startFastTicks;
    }

    /** Whether the ring is back at (C, border) by {@code tick}. */
    public boolean homeAt(long tick) {
        return tick >= homeTick();
    }
}
