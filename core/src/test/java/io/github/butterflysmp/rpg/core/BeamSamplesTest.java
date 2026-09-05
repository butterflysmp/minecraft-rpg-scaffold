package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.BeamSamples;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The spacing of a beam. The one number that decides whether it reads as a line. */
class BeamSamplesTest {

    private static final double PER_BLOCK = 4.0;

    private static List<Vec3> along(double fromX, double toX) {
        return BeamSamples.along(new Vec3(fromX, 0, 0), new Vec3(toX, 0, 0), PER_BLOCK);
    }

    /** Distance between consecutive samples, which is what the eye actually reads. */
    private static double spacing(List<Vec3> points) {
        assertTrue(points.size() >= 2, "need two samples to measure a spacing");
        return points.get(1).subtract(points.get(0)).length();
    }

    /**
     * DENSITY IS PER BLOCK, SO SEGMENT LENGTH DOES NOT CHANGE THE SPACING.
     *
     * <p>This is the defect the whole class exists to prevent. A beam is drawn one CHUNK-COLUMN
     * segment at a time and those segments are of variable length -- an aim clipping the corner of
     * a column yields a sliver, an aim straight down one yields sixteen blocks. A fixed count per
     * segment would make the beam dense near chunk boundaries and thin across long spans, from a
     * rule that looks perfectly uniform in the code.
     *
     * <p>Mutation, run rather than reasoned: replace the count with a constant (say 8) -> the
     * 16-block segment's spacing becomes 2.0 while the 3-block segment's becomes 0.375, and the
     * equality below reddens with "expected: <0.25> but was: <2.0>".
     */
    @Test
    void densityIsPerBlockSoSegmentLengthDoesNotChangeSpacing() {
        List<Vec3> longSegment = along(0, 16);
        List<Vec3> shortSegment = along(0, 3);

        assertEquals(64, longSegment.size(), "16 blocks at 4 per block");
        assertEquals(12, shortSegment.size(), "3 blocks at 4 per block");

        assertEquals(1.0 / PER_BLOCK, spacing(longSegment), 1e-9,
                "spacing is 1/samplesPerBlock, whatever the segment's length");
        assertEquals(spacing(longSegment), spacing(shortSegment), 1e-9,
                "a sixteen-block span and a three-block one must draw at the same density");
    }

    /**
     * A SLIVER DRAWS NOTHING RATHER THAN SPIKING THE DENSITY, AND THERE IS NO PER-SEGMENT FLOOR.
     *
     * <p>cfde822 used {@code max(2, distance * 4)} over the WHOLE LINE, where a floor of 2 is a
     * sane minimum for a beam. Re-applying that floor PER SEGMENT is the trap: a 0.05-block sliver
     * beside a chunk plane would get 2 samples, which is 40 per block at that one spot -- density
     * spikes at exactly the boundaries the per-block rule exists to keep even.
     *
     * <p><b>A floor of 1 does not fix that; it only bounds it</b> at 20 per block. So there is no
     * floor at all. The space a sliver occupies is already covered by the segments on either side
     * of it, and the far end of the whole beam is marked by the impact visual regardless.
     *
     * <p>Mutation: restore {@code max(1, ...)} -> this reddens with "expected: <0> but was: <1>".
     * Restore {@code max(2, ...)} -> it reddens with 2.
     */
    @Test
    void aSliverSegmentDrawsNothingRatherThanSpikingTheDensity() {
        assertEquals(0, along(0, 0.05).size(),
                "a sliver beside a chunk plane draws nothing -- its neighbours cover the space");
        assertEquals(0, along(0, 0).size(), "and a zero-length segment certainly draws nothing");
    }

    /**
     * THE FIRST SAMPLE IS ONE SPACING OFF THE START; THE LAST IS EXACTLY THE END.
     *
     * <p>Both halves are load-bearing and they guard different things.
     *
     * <p>INCLUDING THE END is what lets a beam stop exactly where the ray stopped. CastExecutor
     * hands this the HIT POINT rather than the segment's far end, so a beam that fell one sample
     * short would visibly stop before the wall it hit.
     *
     * <p>EXCLUDING THE START removes the coincident double-draw at each segment joint (segment k's
     * far end IS segment k+1's start). It also moves the first sample one spacing off the muzzle --
     * and <b>this test pins that figure at 0.25 blocks</b>, which is what gate row L0 judges.
     * <b>It is NOT ProjectileFlight's {@code elapsed > 0} guard</b>, which skips an entire tick --
     * 1.4 blocks at the Flint Staff's speed. 0.25 blocks is inside the caster's own head.
     *
     * <p>Mutation: start the loop at {@code i = 0} -> the first assertion reddens, the origin
     * reappears, and the joint double-draw returns. Stop at {@code i = count - 1} -> the last
     * assertion reddens by one spacing and every beam falls short of what it hit.
     */
    @Test
    void theFirstSampleIsOneSpacingOffTheStartAndTheLastIsExactlyTheEnd() {
        Vec3 eye = new Vec3(0, 1.62, 0);
        Vec3 end = new Vec3(4, 1.62, 0);
        List<Vec3> points = BeamSamples.along(eye, end, PER_BLOCK);

        assertNotEquals(eye, points.get(0), "the muzzle itself is never drawn at");
        assertEquals(0.25, points.get(0).subtract(eye).length(), 1e-9,
                "the first sample sits one spacing -- 1/4 block -- off the eye, whatever the aim");
        assertEquals(end, points.get(points.size() - 1),
                "the last sample IS the end, so a beam stops exactly where the ray did");
    }

    /**
     * The spacing figure above does not depend on the aim, which is worth its own assertion
     * because L0 is judged once and then trusted for every shot. A diagonal aim crosses more chunk
     * planes and so yields different segments, but the distance from a segment's start to its
     * first sample is 1/samplesPerBlock either way.
     */
    @Test
    void theFirstSamplesDistanceIsTheSameOnADiagonalAsDownAnAxis() {
        Vec3 origin = new Vec3(0, 1.62, 0);
        double d = 1 / Math.sqrt(2);
        List<Vec3> diagonal = BeamSamples.along(origin, origin.add(new Vec3(d * 8, 0, d * 8)), PER_BLOCK);

        assertEquals(0.25, diagonal.get(0).subtract(origin).length(), 1e-9,
                "one spacing off the muzzle on a diagonal too -- L0's figure is aim-independent");
    }
}
