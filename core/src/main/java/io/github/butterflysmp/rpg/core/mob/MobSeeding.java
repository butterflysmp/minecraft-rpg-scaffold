package io.github.butterflysmp.rpg.core.mob;

/**
 * Deciding what a mob's custom stats seed FROM: its content definition when it carries a mob tag, or
 * its vanilla attributes when it does not.
 *
 * This is the separation guarantee in one pure function, deliberately Bukkit-free so it can be
 * reddening-tested in the 2-second loop instead of by spawning things on a server. The property that
 * matters is symmetric, and both halves have to be tested:
 *
 *  - a TAGGED mob takes its definition's numbers (the Knell has 360 HP);
 *  - an UNTAGGED mob takes the vanilla numbers, unchanged, on exactly the path it took before this
 *    feature existed (an ordinary wither skeleton is still an ordinary wither skeleton).
 *
 * The second half is the one worth guarding hardest. A bug that scales every mob of a type would look
 * like the feature working -- the Knell would be right -- while quietly changing every wither skeleton
 * on the server.
 */
public final class MobSeeding {

    private MobSeeding() {}

    /**
     * The max HP to seed a mob at.
     *
     * @param mobs      the loaded custom-mob registry
     * @param mobId     the mob's {@code mob_id} PDC tag, or {@code null} when it carries none
     * @param vanillaMax the value the vanilla path would have used (its MAX_HEALTH attribute)
     * @return the definition's {@code max_health} when {@code mobId} is tagged AND known, else
     *         {@code vanillaMax}
     *
     * A tag naming a mob the registry does not know FALLS BACK to vanilla rather than throwing or
     * yielding 0. That case is real -- a content file renamed or deleted while a tagged mob is still
     * alive in a loaded chunk -- and the fail-soft answer is the safe one: an unkillable mob (0 max,
     * never draining) or a crashed seed on an entity add event are both worse than a mob that quietly
     * reverts to its vanilla numbers.
     */
    public static double maxHealth(MobRegistry mobs, String mobId, double vanillaMax) {
        if (mobId == null) return vanillaMax;
        return mobs.find(mobId)
                .map(MobDefinition::maxHealth)
                .orElse(vanillaMax);
    }
}
