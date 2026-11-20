package io.github.butterflysmp.rpg.core.fight;

/**
 * WHERE A DOWN PLAYER'S COPY LIES (step F9e b; Ben, PLAN-dragon-fight.md section 1, 0e: "The copy should be placed just
 * inside the ring in both cases"). Horizontal only: the Paper side lays it on the arena's floor, so a death in the air
 * or in the void comes down to the floor below the point given here.
 * <ul>
 *   <li>At least {@link #INSIDE_BLOCKS} inside the ring: where he died.</li>
 *   <li>Otherwise (he died outside the ring, or nearer its edge than that): the nearest point that far inside, on the
 *       line from the ring's centre to where he died.</li>
 *   <li>A CLOSED ring (no point that far inside: radius {@link #INSIDE_BLOCKS} or less) contains no place for it: the
 *       copy stays where it is and CANNOT be revived.</li>
 * </ul>
 * THE SEAT'S MECHANISM READING (flagged): on every ring tick each copy is placed again against the ring of that tick,
 * so a closing or moving edge that would pass it carries it along, one block inside. Pure.
 */
public final class CopyPlace {

    private CopyPlace() {}

    /** How far inside the ring's edge a copy lies, blocks (Ben's "just inside"; SEAT PLACEHOLDER, one block). */
    public static final double INSIDE_BLOCKS = 1.0;

    /**
     * @param xBlocks   where it lies, x
     * @param zBlocks   and z
     * @param revivable false only when the ring is closed (no place is inside)
     * @param moved     whether this differs from the point it was asked about
     */
    public record Place(double xBlocks, double zBlocks, boolean revivable, boolean moved) {}

    /** Where a copy at (x, z) lies against this ring. */
    public static Place place(double xBlocks, double zBlocks, Ring ring) {
        double inner = ring.radiusBlocks() - INSIDE_BLOCKS;
        if (inner <= 0.0) return new Place(xBlocks, zBlocks, false, false);
        double dx = xBlocks - ring.centreXBlocks();
        double dz = zBlocks - ring.centreZBlocks();
        double d = Math.hypot(dx, dz);
        if (d <= inner) return new Place(xBlocks, zBlocks, true, false);
        double k = inner / d;
        return new Place(ring.centreXBlocks() + dx * k, ring.centreZBlocks() + dz * k, true, true);
    }
}
