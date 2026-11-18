package io.github.butterflysmp.rpg.core.fight;

/**
 * F1, THE SLOW CLOSE: centred on M, the radius falling from the Mage ring's radius at {@code v_slow}; a counted kill
 * adds {@code e_kill}, never past the Mage ring's radius; at 0 it is closed for good (Ben, 2026-11-18: "if the ring
 * fully closes they it cannot be reopened").
 *
 * <p>Immutable: a kill returns a NEW {@code SlowClose} anchored at the kill's tick. Between kills the radius is
 * {@code anchorRadius - v_slow x seconds since the anchor}, floored at 0.
 *
 * @param settings           the fight's numbers
 * @param centreXBlocks      M's x, blocks
 * @param centreZBlocks      M's z, blocks
 * @param anchorTick         the tick the radius was last set (the start, or the last counted kill)
 * @param anchorRadiusBlocks the radius at {@code anchorTick}, blocks
 */
public record SlowClose(FightSettings settings, double centreXBlocks, double centreZBlocks,
                        long anchorTick, double anchorRadiusBlocks) {

    /** The slow close starting at {@code startTick}, centred on M, at the Mage ring's radius. */
    public static SlowClose start(FightSettings settings, double mageXBlocks, double mageZBlocks, long startTick) {
        return new SlowClose(settings, mageXBlocks, mageZBlocks, startTick, settings.mageRingRadiusBlocks());
    }

    /** The radius at {@code tick}, blocks, never below 0. */
    public double radiusAt(long tick) {
        double seconds = (tick - anchorTick) / FightSettings.TICKS_PER_SECOND;
        return Math.max(0.0, anchorRadiusBlocks - settings.slowCloseBlocksPerSecond() * seconds);
    }

    /** Whether the ring has closed by {@code tick}. Once true it stays true: the radius only falls between kills. */
    public boolean closedAt(long tick) {
        return radiusAt(tick) <= 0.0;
    }

    /** The first tick at which the ring is closed, with no further kills. */
    public long closeTick() {
        double ticks = anchorRadiusBlocks / settings.slowCloseBlocksPerSecond() * FightSettings.TICKS_PER_SECOND;
        long t = anchorTick + (long) Math.ceil(ticks);
        // Settle any floating-point disagreement between this estimate and radiusAt on the boundary.
        while (t > anchorTick && closedAt(t - 1)) t--;
        while (!closedAt(t)) t++;
        return t;
    }

    /**
     * After a counted kill at {@code tick}: the radius grows by {@code e_kill}, capped at the Mage ring's radius.
     * <b>A closed ring stays closed:</b> a kill at or after the close tick returns this same object.
     */
    public SlowClose afterKill(long tick) {
        if (closedAt(tick)) return this;
        double grown = Math.min(settings.mageRingRadiusBlocks(), radiusAt(tick) + settings.killExpansionBlocks());
        return new SlowClose(settings, centreXBlocks, centreZBlocks, tick, grown);
    }

    /** The ring at {@code tick}. */
    public Ring ringAt(long tick) {
        return new Ring(centreXBlocks, centreZBlocks, radiusAt(tick));
    }
}
