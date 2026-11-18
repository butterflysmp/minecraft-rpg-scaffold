package io.github.butterflysmp.rpg.core.fight;

import java.util.Random;

/**
 * F6: {@code land random}, a point uniformly random over the DISC of the maximum landing distance about C. Uniform over
 * the area, not the radius: the radius is the maximum times the square root of a uniform number.
 */
public final class RandomLanding {

    private RandomLanding() {}

    /** An offset {@code {dx, dz}} from C, blocks, at most {@code maxDistanceBlocks} from it. */
    public static double[] pick(double maxDistanceBlocks, Random rng) {
        double r = maxDistanceBlocks * Math.sqrt(rng.nextDouble());
        double a = 2.0 * Math.PI * rng.nextDouble();
        return new double[] {r * Math.cos(a), r * Math.sin(a)};
    }
}
