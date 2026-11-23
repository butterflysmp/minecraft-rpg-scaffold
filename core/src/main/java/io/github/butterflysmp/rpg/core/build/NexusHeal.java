package io.github.butterflysmp.rpg.core.build;

/**
 * THE NEXUS ITEM HEALS ON THE BEAT (the seat's ruling M, 2026-11-22). The decision is pure; {@code NexusHealing} in paper
 * performs it on the stats bar's 10-tick beat, BEFORE the mirror is redrawn, by the same converge call and the same
 * star guard the join path uses.
 *
 * <p><b>WHY.</b> The quiver mirror makes the Nexus item sometimes an ARROW, so any path that removes arrows by type can now
 * remove it. An operator's {@code /clear @s minecraft:arrow} is the one found (INFERRED: vanilla's clear matches the item
 * type); the heal covers the ones not found. Before it, nothing put the item back until his next join, respawn or star
 * toggle.
 *
 * <p><b>BOUNDED.</b> It mints only when NONE is found: a star in another slot is not moved (hold_only moves it on purpose,
 * the seat's ruling G), and duplicates are not this rule's business. A switched-off star is never minted. A dead player
 * is left to the respawn path, which already converges.
 */
public final class NexusHeal {

    private NexusHeal() {}

    /**
     * @param starOn     his star is switched on (an absent profile reads as on, the join path's guard)
     * @param nexusFound how many items carrying the Nexus tag are anywhere in his inventory
     * @param alive      he is alive
     */
    public static boolean heals(boolean starOn, int nexusFound, boolean alive) {
        return starOn && nexusFound == 0 && alive;
    }
}
