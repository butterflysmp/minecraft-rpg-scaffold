package io.github.butterflysmp.rpg.core.fight;

/**
 * EVERY NUMBER THE DRAGON FIGHT'S RING, LIGHTNING AND MAGE CYCLE RUN ON (PLAN-dragon-fight.md section 2, the quantity
 * table). Nothing in this package holds one of these as a constant; each arrives here, so the first boot can set them
 * from content without touching the logic.
 *
 * <p>A {@code record} is an immutable data class: Java writes the constructor, the getters (named after the fields,
 * e.g. {@code borderRadiusBlocks()}), {@code equals} and {@code toString}. The block below the header is the
 * "compact constructor", which runs before the fields are set and is where the values are checked.
 *
 * <p>Whose each number is, as of 2026-11-18: {@code borderRadiusBlocks} 48 and {@code mageRingRadiusBlocks} 12 are
 * Ben's; {@code strikeIntervalTicks} 40 (every 2 s) and {@code firstStrikeDamage} 20 are Ben's; the speeds, the kill
 * expansion and the crystals are SEAT PLACEHOLDERS that Ben sets at the first boot.
 *
 * @param borderRadiusBlocks       the border ring's radius about the arena centre C, blocks
 * @param mageRingRadiusBlocks     the ring's radius about the Mage M when the fast close ends, blocks
 * @param fastCloseBlocksPerSecond {@code v_fast}: the speed of the ring's FASTEST point in the fast close and in the
 *                                 return's fast part, blocks per second
 * @param slowCloseBlocksPerSecond {@code v_slow}: how fast the radius falls in the slow close (and rises in the
 *                                 return's slow part), blocks per second
 * @param killExpansionBlocks      {@code e_kill}: the radius one counted kill adds, blocks
 * @param crystalCount             how many crystals light before the Mage is powered
 * @param crystalIntervalTicks     ticks between crystals, counted from the start of the slow close
 * @param strikeIntervalTicks      ticks between lightning strikes outside the ring
 * @param firstStrikeDamage        the damage of a player's first consecutive strike; each further one doubles
 */
public record FightSettings(
        double borderRadiusBlocks,
        double mageRingRadiusBlocks,
        double fastCloseBlocksPerSecond,
        double slowCloseBlocksPerSecond,
        double killExpansionBlocks,
        int crystalCount,
        long crystalIntervalTicks,
        long strikeIntervalTicks,
        double firstStrikeDamage) {

    /** Minecraft's tick rate, ticks per second. A fact about the game, not a ruled number. */
    public static final double TICKS_PER_SECOND = 20.0;

    public FightSettings {
        requirePositive(borderRadiusBlocks, "borderRadiusBlocks");
        requirePositive(mageRingRadiusBlocks, "mageRingRadiusBlocks");
        if (mageRingRadiusBlocks >= borderRadiusBlocks) {
            throw new IllegalArgumentException("mageRingRadiusBlocks " + mageRingRadiusBlocks
                    + " must be smaller than borderRadiusBlocks " + borderRadiusBlocks);
        }
        requirePositive(fastCloseBlocksPerSecond, "fastCloseBlocksPerSecond");
        requirePositive(slowCloseBlocksPerSecond, "slowCloseBlocksPerSecond");
        // The return's slow part moves every point at v_slow; it must not outrun the fast close's cap.
        if (slowCloseBlocksPerSecond > fastCloseBlocksPerSecond) {
            throw new IllegalArgumentException("slowCloseBlocksPerSecond " + slowCloseBlocksPerSecond
                    + " must not exceed fastCloseBlocksPerSecond " + fastCloseBlocksPerSecond);
        }
        if (!(killExpansionBlocks >= 0.0) || Double.isInfinite(killExpansionBlocks)) {
            throw new IllegalArgumentException("killExpansionBlocks must be finite and >= 0: " + killExpansionBlocks);
        }
        if (crystalCount < 1) throw new IllegalArgumentException("crystalCount must be >= 1: " + crystalCount);
        if (crystalIntervalTicks < 1) {
            throw new IllegalArgumentException("crystalIntervalTicks must be >= 1: " + crystalIntervalTicks);
        }
        if (strikeIntervalTicks < 1) {
            throw new IllegalArgumentException("strikeIntervalTicks must be >= 1: " + strikeIntervalTicks);
        }
        requirePositive(firstStrikeDamage, "firstStrikeDamage");
    }

    /**
     * How far from C the Mage may land, blocks: {@code border - mageRing} (36 with Ben's numbers). Past it, the
     * 12-block ring would poke outside the 48 border and the rings would stop nesting (PLAN section 2).
     */
    public double maxLandingDistanceBlocks() {
        return borderRadiusBlocks - mageRingRadiusBlocks;
    }

    private static void requirePositive(double value, String name) {
        if (!(value > 0.0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
        }
    }
}
