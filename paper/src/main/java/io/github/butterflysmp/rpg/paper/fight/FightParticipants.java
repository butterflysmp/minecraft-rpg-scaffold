package io.github.butterflysmp.rpg.paper.fight;

import org.bukkit.GameMode;

/**
 * WHO TAKES PART IN THE DEV FIXTURE (GATE-fight-ring.md): a player in C's world, within the fixture's radius of C,
 * in SURVIVAL or ADVENTURE. Creative and spectator are never struck and never drawn to. Pure, so the rule is tested
 * without a server.
 */
public final class FightParticipants {

    private FightParticipants() {}

    /**
     * @param sameWorld      whether he is in C's world
     * @param distanceBlocks his distance from C, blocks (3D, as {@code Location#distance} gives it)
     * @param mode           his game mode
     * @param radiusBlocks   the fixture's radius
     */
    public static boolean takesPart(boolean sameWorld, double distanceBlocks, GameMode mode, double radiusBlocks) {
        if (!sameWorld) return false;
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) return false;
        return distanceBlocks <= radiusBlocks;
    }
}
