package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Where to draw along a segment, at a density measured PER BLOCK.
 *
 * Pure arithmetic, so the one number that decides whether a beam reads as a LINE is testable
 * without a server. The adapter owns the rendering; this owns the spacing.
 *
 * <p><b>DENSITY IS PER BLOCK, NOT PER SEGMENT, AND THAT IS THE WHOLE POINT.</b> A beam is drawn
 * one chunk-column segment at a time (see {@link ChunkTraversal}), and those segments are of
 * VARIABLE LENGTH -- an aim that clips the corner of a column yields a sliver, an aim straight
 * down a column yields sixteen blocks. A fixed count per segment would therefore make the beam
 * dense near chunk boundaries and thin across long spans, from a rule that looks uniform in the
 * code. Deriving the count from the segment's own length is what keeps the spacing constant, and
 * the spacing is what the eye reads.
 */
public final class BeamSamples {

    private BeamSamples() {}

    /**
     * The points to draw at, in increasing distance from {@code from}.
     *
     * <p><b>{@code from} IS EXCLUDED AND {@code to} IS INCLUDED.</b> Both halves are deliberate and
     * neither is the projectile rule:
     *
     * <ul>
     *   <li>Including {@code to} is what lets a beam stop EXACTLY where the ray stopped. The
     *       caller hands the hit point, not the segment's far end, so the last sample lands on the
     *       wall or the body rather than a fraction short of it.
     *   <li>Excluding {@code from} buys two things, and it is worth being precise about which,
     *       because the obvious claim is wrong. It removes the COINCIDENT DOUBLE-DRAW at each
     *       segment joint -- segment k's far end is segment k+1's start, and both would otherwise
     *       be drawn. And it moves the first sample one spacing off the muzzle.
     * </ul>
     *
     * <p><b>THAT SECOND EFFECT IS NOT {@code ProjectileFlight.step}'s {@code elapsed > 0} GUARD,
     * AND MUST NOT BE DESCRIBED AS ONE.</b> That guard skips an entire TICK -- 1.4 blocks at the
     * Flint Staff's speed. This skips {@code 1/samplesPerBlock}, which at 4 per block is 0.25
     * BLOCKS, and note that the figure does not depend on segment length or on aim: the first
     * sample is always exactly one spacing from the muzzle. A quarter of a block is inside the
     * caster's own head, and dust at size 1.2 is a soft coloured blob rather than a spark.
     * cfde822 drew at s = 0 -- literally AT the eye -- and nobody complained, so this may be
     * entirely fine; but the Flint Staff's gate found a FLAME at the eye WAS a problem, and the two
     * particles are not comparable. It is a gate question, not a settled one.
     *
     * <p><b>THERE IS NO PER-SEGMENT FLOOR, AND ADDING ONE WOULD UNDO THE CLASS'S PURPOSE.</b>
     * cfde822 used {@code max(2, distance * 4)} over the WHOLE LINE, where a floor of 2 is a sane
     * minimum for a beam. Re-applying that floor per segment puts 2 samples into a 0.05-block
     * sliver beside a chunk plane -- 40 per block at that one spot. A floor of 1 only BOUNDS that
     * spike (20 per block) rather than removing it. So a sliver draws NOTHING, and the neighbouring
     * segments already cover the space it occupies. That is what actually keeps the spacing even.
     *
     * @param samplesPerBlock authored per beam, on the visual's particle step. Must be > 0.
     */
    public static List<Vec3> along(Vec3 from, Vec3 to, double samplesPerBlock) {
        Vec3 span = to.subtract(from);
        double length = span.length();

        int count = (int) Math.round(length * samplesPerBlock);
        if (count <= 0) return List.of();

        List<Vec3> points = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            points.add(from.add(span.scale((double) i / count)));
        }
        return points;
    }
}
