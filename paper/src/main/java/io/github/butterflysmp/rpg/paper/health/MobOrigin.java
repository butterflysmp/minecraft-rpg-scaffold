package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.mob.DimensionGearScore;
import io.github.butterflysmp.rpg.core.mob.MobDimension;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Where a mob's distance is measured FROM, and which curve it is read against -- the only two things
 * about a position that {@code DimensionGearScore} does not decide.
 *
 * <ul>
 *   <li><b>The End measures from (0, 0), always, never its spawn point (M23).</b> This is the only
 *       place that rule lives, and nothing unit-tests it: the M25 End rows are its witness.
 *   <li>The overworld and the Nether still measure from the world's spawn point, but since M25 their
 *       curves have no distance term, so the distance is computed and then ignored. It is kept because
 *       {@code /rpg mobinfo} prints it, and a later boss or activity source (M3's seam) may want it.
 * </ul>
 *
 * <p>A {@code CUSTOM} environment reads the overworld curve: a custom world has no ruling, and the
 * overworld curve is the least surprising.
 */
public final class MobOrigin {

    private MobOrigin() {}

    public static MobDimension dimensionOf(World world) {
        // Exhaustive, no default: a fifth Environment is a compile error rather than a silent guess.
        return switch (world.getEnvironment()) {
            case NORMAL, CUSTOM -> MobDimension.OVERWORLD;
            case NETHER -> MobDimension.NETHER;
            case THE_END -> MobDimension.END;
        };
    }

    /** Horizontal distance from this world's origin to {@code at}. */
    public static double horizontalDistance(Location at) {
        World world = at.getWorld();
        if (dimensionOf(world) == MobDimension.END) {
            return DimensionGearScore.horizontalDistance(at.getX(), at.getZ(), 0.0, 0.0);
        }
        Location spawn = world.getSpawnLocation();
        return DimensionGearScore.horizontalDistance(at.getX(), at.getZ(), spawn.getX(), spawn.getZ());
    }
}
