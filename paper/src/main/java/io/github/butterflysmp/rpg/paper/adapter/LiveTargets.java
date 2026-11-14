package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * WHICH BODIES CAN TAKE A HIT. Every first-hit and every nearby pick on the Paper side goes through here, so a mob in
 * its death animation can never steal a hit meant for the live mob behind or beside it.
 *
 * <p><b>The defect this exists for (the {@code 61e722f2} boot, 2026-11-10, seat-accepted).</b> {@code castRay}'s
 * filter was {@code entity instanceof LivingEntity && !caster}. A mob that has just been killed stays a
 * {@code LivingEntity} for its death animation, about a second, until the server removes it. In that window it still
 * passed the filter. So a projectile ended on the corpse ({@code ProjectileFlight.step} stops at the first
 * {@code castRay} hit), and the impact on an already-dead body dealt nothing. Three gate rows missed this way:
 * GATE-legacy-b.md LB2 and LB5 (a), and GATE-withered-shortbow.md WB2. The dev look-rays had the same filter, so
 * {@code /rpg apply} and {@code /rpg mobdamage} landed on corpses (GATE-wither-status.md WS2b, WS2c, WS3). In play, a
 * shot through a mob that has just died is used up on the body.
 *
 * <p><b>Why {@code isDead()} and {@code isValid()} both.</b> {@code isDead()} is the death animation: in that boot,
 * the selftest's PROBE, which reads exactly {@code LivingEntity.isDead()}, printed {@code dead=true} for every corpse
 * that took a hit and {@code dead=false} for every live target. {@code isValid()} is a body already removed but still
 * held in a stale reference. Each check guards a different window, so neither replaces the other.
 */
public final class LiveTargets {

    private LiveTargets() {}

    /** A body that can still take a hit: a {@code LivingEntity} that is neither dying nor removed. */
    public static boolean isLive(Entity entity) {
        return entity instanceof LivingEntity living && !living.isDead() && living.isValid();
    }

    /** {@code castRay}'s filter: any live body except the caster. */
    public static Predicate<Entity> except(UUID casterId) {
        return entity -> isLive(entity) && !entity.getUniqueId().equals(casterId);
    }

    /** The dev look-rays' filter ({@code /rpg apply}, {@code /rpg mobdamage}, {@code /rpg mobinfo}): any live body that is not a player. */
    public static boolean isLiveMob(Entity entity) {
        return isLive(entity) && !(entity instanceof Player);
    }
}
