package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * THE ENGINE'S ONE TARGETING SEAM (the Blink Tome's K7, 2026-11-26). Every place OUR code makes a mob target something
 * goes through here: the aggro-on-hit in {@code BukkitCombatant.applyNow} and the selftest's {@code mob_target}
 * ({@code TargetSeamTest} forbids a bare {@code setTarget(} on a non-null target anywhere else in main code). Before this
 * those were the only two {@code setTarget} calls in main code, and neither raises vanilla's target event (READ:
 * {@code CraftMob#setTarget} passes a null reason, and {@code Mob.setTarget} fires the event only with a reason), so the
 * blink's refusal has to be asked here as well as at the event.
 *
 * <p>Clearing a target ({@code null}) is never refused and does not come through here.
 */
public final class MobTargets {

    private MobTargets() {}

    /** Make {@code mob} target {@code target} unless the guard refuses it. True when the target was set. */
    public static boolean set(Mob mob, LivingEntity target, TargetGuard guard) {
        if (target != null && guard.refuses(mob, target)) return false;
        mob.setTarget(target);
        return true;
    }
}
