package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;

/**
 * Hostile or passive: M21's line. A HOSTILE mob gets a gear score; a PASSIVE one gets the 5x health
 * and no score (M7).
 *
 * <p><b>The rule is one interface, {@link Enemy}, and nothing else</b>, as ruled. The full closure
 * measured from the pinned API jar, and every edge case (neutral mobs inside and outside it, split
 * slimes, tamed mobs, undead mounts, the Hoglin), is the account in {@code PLAN-mob-scaling.md} §0
 * under M21. This is the pointer.
 *
 * <p>Two edges worth knowing without opening the plan: an enderman, spider or piglin IS an
 * {@code Enemy}, so it scales; a wolf, bee, iron golem or llama is NOT, so it does not. Neither is an
 * accident of this class -- they are the ruled line.
 */
public final class MobClassifier {

    private MobClassifier() {}

    public static boolean isHostile(LivingEntity mob) {
        return mob instanceof Enemy;
    }
}
