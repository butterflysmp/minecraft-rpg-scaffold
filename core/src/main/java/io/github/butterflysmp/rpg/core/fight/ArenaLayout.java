package io.github.butterflysmp.rpg.core.fight;

import java.util.List;

/**
 * WHAT THE FIGHT READS FROM ITS ARENA, WHICHEVER SOURCE MADE IT (PLAN-dragon-mobs.md C3; the seat's queue, item 2):
 * the generated stand-in ({@link ArenaShape#layout}) or a template file ({@link TemplateLayout}).
 *
 * @param centreX           C's block column, x
 * @param centreZ           and z
 * @param floorY            the floor's TOP block under C; a player stands at {@link #groundY()}
 * @param crystalSpots      crystal spot n at index n - 1: {x, y, z}, blocks (feet of the crystal)
 * @param scanRadius        how far from C the arena probe looks for fire, blocks
 * @param scanTopAboveFloor how far above the floor it looks
 */
public record ArenaLayout(int centreX, int centreZ, int floorY, List<double[]> crystalSpots, int scanRadius,
                          int scanTopAboveFloor) {

    public ArenaLayout {
        crystalSpots = List.copyOf(crystalSpots);
    }

    /** C's ground: the floor's top plus one. */
    public double groundY() {
        return floorY + 1;
    }
}
