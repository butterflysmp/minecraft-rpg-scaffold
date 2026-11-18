package io.github.butterflysmp.rpg.core.fight;

/**
 * The answer to "may the Mage land here?" ({@link FastClosePath#plan}). It is one of exactly two shapes, and the caller
 * must handle both, so a refusal cannot be ignored by accident.
 *
 * <p>A {@code sealed interface} lists every class allowed to implement it ({@code permits}), so a {@code switch} over
 * a {@code Landing} that forgets a case does not compile.
 */
public sealed interface Landing permits Landing.Accepted, Landing.TooFar {

    /** The landing is allowed; {@code path} is the fast close it starts. */
    record Accepted(FastClosePath path) implements Landing {}

    /**
     * Refused: the Mage is further from C than {@link FightSettings#maxLandingDistanceBlocks()}, so the rings would
     * not nest.
     *
     * @param distanceBlocks    how far from C he tried to land, horizontally
     * @param maxDistanceBlocks the furthest allowed
     */
    record TooFar(double distanceBlocks, double maxDistanceBlocks) implements Landing {}
}
