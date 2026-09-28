package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.mob.DistanceGearScore;
import io.github.butterflysmp.rpg.core.mob.MobDimension;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Where a mob's distance is measured FROM, and which curve it is read against -- the only two things
 * about a position that {@code DistanceGearScore} does not decide.
 *
 * <ul>
 *   <li>The overworld and the Nether measure from the world's spawn point (the brief's curves).
 *   <li><b>The End measures from (0, 0), always, never its spawn point (M23).</b> This is the only
 *       place that rule lives, and nothing unit-tests it: gate row G7 is its witness.
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
            return DistanceGearScore.horizontalDistance(at.getX(), at.getZ(), 0.0, 0.0);
        }
        Location spawn = world.getSpawnLocation();
        return DistanceGearScore.horizontalDistance(at.getX(), at.getZ(), spawn.getX(), spawn.getZ());
    }
}
