package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * The selftest pad's stand point for a world, for code outside this package (S1: the MOBSEED line's distance). One
 * source: {@link SelfTestPad#standLocation} of {@link SelfTestPad#standOf}, derived from the world's spawn.
 */
public final class SelfTestStandPoint {

    private SelfTestStandPoint() {}

    public static Location of(World world) {
        return SelfTestPad.standLocation(world, SelfTestPad.standOf(world));
    }
}
