package io.github.butterflysmp.rpg.core.fight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A TEMPLATE FILE'S ARENA (PLAN-dragon-mobs.md C3; the seat's queue, item 2). The file is a saved structure. The
 * builder marks two things in it with MARKER BLOCKS, which are removed when it is placed (CC's proposal, flagged for
 * Ben):
 * <ul>
 *   <li><b>ONE PODIUM marker</b>, standing ON the floor at the arena's centre. It is C: the file is placed so this
 *       column lands on the fight world's (0, 0) and the block under it on the content's {@code floor_y}.</li>
 *   <li><b>ONE CRYSTAL marker per crystal</b>, where each crystal will stand. Spot 1 is the marker nearest due east of
 *       the podium (+x), the rest anticlockwise seen from above (+z for a positive angle), as the generated arena's
 *       pillars are numbered.</li>
 * </ul>
 * Pure: the Paper side reads the markers from the file and places it at {@link Ok#originX()} and the rest.
 */
public final class TemplateLayout {

    private TemplateLayout() {}

    public enum Kind { PODIUM, CRYSTAL }

    /** A marker block's position in the FILE (0-based from the file's corner) and what it marks. */
    public record Marker(int x, int y, int z, Kind kind) {}

    /** What reading the file came to. */
    public sealed interface Result permits Ok, Refused {}

    /**
     * @param layout       what the fight reads
     * @param originX      where the file's corner goes in the world, x
     * @param originY      y
     * @param originZ      z
     * @param markersWorld every marker's world block {x, y, z}, podium first, then the crystals in spot order
     */
    public record Ok(ArenaLayout layout, int originX, int originY, int originZ, List<int[]> markersWorld)
            implements Result {}

    /** Refused, with the reason in words (the caller names the file). */
    public record Refused(String why) implements Result {}

    /**
     * @param sizeX        the file's size, x
     * @param sizeY        y
     * @param sizeZ        z
     * @param markers      every marker block found in it
     * @param floorY       the content's {@code floor_y}: where the floor under the podium goes
     * @param crystalCount the content's {@code crystal_count}: exactly that many crystal markers
     */
    public static Result read(int sizeX, int sizeY, int sizeZ, List<Marker> markers, int floorY, int crystalCount) {
        List<Marker> podiums = markers.stream().filter(m -> m.kind() == Kind.PODIUM).toList();
        List<Marker> crystals = new ArrayList<>(markers.stream().filter(m -> m.kind() == Kind.CRYSTAL).toList());
        if (podiums.size() != 1) {
            return new Refused(podiums.size() + " podium markers, and a template needs exactly 1");
        }
        if (crystals.size() != crystalCount) {
            return new Refused(crystals.size() + " crystal markers, and the fight has " + crystalCount + " crystals");
        }
        Marker p = podiums.get(0);
        if (p.y() < 1) return new Refused("no floor under the podium (it is on the file's bottom layer)");
        int originX = -p.x();
        int originZ = -p.z();
        int originY = floorY - (p.y() - 1);
        crystals.sort(Comparator.comparingDouble(m -> angle(m.x() - p.x(), m.z() - p.z())));
        List<double[]> spots = new ArrayList<>();
        List<int[]> world = new ArrayList<>();
        world.add(new int[] {originX + p.x(), originY + p.y(), originZ + p.z()});
        for (Marker c : crystals) {
            spots.add(new double[] {originX + c.x() + 0.5, originY + c.y(), originZ + c.z() + 0.5});
            world.add(new int[] {originX + c.x(), originY + c.y(), originZ + c.z()});
        }
        int scanRadius = Math.max(sizeX, sizeZ) / 2;
        int scanTop = (sizeY - 1) - (p.y() - 1);
        ArenaLayout layout = new ArenaLayout(0, 0, floorY, spots, scanRadius, scanTop);
        return new Ok(layout, originX, originY, originZ, List.copyOf(world));
    }

    /** The angle of (dx, dz) from +x towards +z, in [0, 2 pi): due east is 0, the generated arena's pillar 1. */
    static double angle(int dx, int dz) {
        double a = Math.atan2(dz, dx);
        return a < 0 ? a + 2 * Math.PI : a;
    }
}
