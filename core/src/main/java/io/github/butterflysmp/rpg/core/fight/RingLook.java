package io.github.butterflysmp.rpg.core.fight;

import java.util.List;

/**
 * HOW THE RING IS DRAWN (PLAN-dragon-fight.md section 6, step F4). Content, so Ben retunes it without code. The seat's
 * starting look, judged by Ben at the boot: purple dust (cfde822's border colour, {@code StormSystem.STORM_PURPLE_DUST}
 * at {@code cfde822}: RGB 170, 0, 255, size 1.6), a point about every block round the ring, at three heights above the
 * ground at C, redrawn every 4 ticks.
 *
 * <p>The drawing is never read by anything that decides damage: the ring's shape comes from {@link MageCycle}, and
 * this only says how to show it.
 *
 * @param particleKey         the particle, as a key ({@code dust}); the paper side resolves and refuses it
 * @param red                 dust colour, 0 to 255
 * @param green               dust colour, 0 to 255
 * @param blue                dust colour, 0 to 255
 * @param dustSize            dust size, as the particle takes it
 * @param pointSpacingBlocks  the largest distance between neighbouring points round the ring, blocks
 * @param heightOffsetsBlocks the heights to draw at, blocks above the ground at C; one ring of points per height
 * @param redrawPeriodTicks   ticks between redraws
 */
public record RingLook(String particleKey, int red, int green, int blue, double dustSize,
                       double pointSpacingBlocks, List<Double> heightOffsetsBlocks, int redrawPeriodTicks) {

    public RingLook {
        if (particleKey == null || particleKey.isBlank()) throw new IllegalArgumentException("no particle key");
        requireChannel(red, "red");
        requireChannel(green, "green");
        requireChannel(blue, "blue");
        if (!(dustSize > 0.0) || Double.isInfinite(dustSize)) {
            throw new IllegalArgumentException("dustSize must be finite and > 0: " + dustSize);
        }
        if (!(pointSpacingBlocks > 0.0) || Double.isInfinite(pointSpacingBlocks)) {
            throw new IllegalArgumentException("pointSpacingBlocks must be finite and > 0: " + pointSpacingBlocks);
        }
        heightOffsetsBlocks = List.copyOf(heightOffsetsBlocks);
        if (heightOffsetsBlocks.isEmpty()) throw new IllegalArgumentException("heightOffsetsBlocks is empty");
        for (double h : heightOffsetsBlocks) {
            if (!Double.isFinite(h)) throw new IllegalArgumentException("a height offset is not finite: " + h);
        }
        if (redrawPeriodTicks < 1) {
            throw new IllegalArgumentException("redrawPeriodTicks must be >= 1: " + redrawPeriodTicks);
        }
    }

    private static void requireChannel(int value, String name) {
        if (value < 0 || value > 255) throw new IllegalArgumentException(name + " must be 0 to 255: " + value);
    }
}
