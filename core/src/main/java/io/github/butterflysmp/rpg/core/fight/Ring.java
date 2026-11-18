package io.github.butterflysmp.rpg.core.fight;

/**
 * The storm ring at one tick: a horizontal circle. Heights play no part (PLAN-dragon-fight.md section 2).
 *
 * @param centreXBlocks the centre's x, blocks
 * @param centreZBlocks the centre's z, blocks
 * @param radiusBlocks  the radius, blocks; 0 means the ring has closed for good
 */
public record Ring(double centreXBlocks, double centreZBlocks, double radiusBlocks) {

    /**
     * Whether a player standing at these FEET coordinates is inside: the horizontal distance to the centre is at most
     * the radius, so the edge itself counts as inside. Height is not an argument, so it cannot matter.
     *
     * <p><b>A closed ring (radius 0) contains no point, its own centre included</b> (seat reading b: "no point is
     * inside it"). Without that clause the inclusive edge would leave one point, the centre, inside a closed ring.
     */
    public boolean contains(double feetXBlocks, double feetZBlocks) {
        if (radiusBlocks <= 0.0) return false;
        double dx = feetXBlocks - centreXBlocks;
        double dz = feetZBlocks - centreZBlocks;
        return dx * dx + dz * dz <= radiusBlocks * radiusBlocks;
    }
}
