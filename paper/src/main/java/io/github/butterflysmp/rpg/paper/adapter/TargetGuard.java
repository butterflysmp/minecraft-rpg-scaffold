package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * WHO A MOB MAY NOT TAKE AS A TARGET (the Blink Tome's K7, 2026-11-26): a player in the blinked state is HIDDEN. Asked at
 * the three places a target or a victim is chosen:
 *
 * <ul>
 *   <li>the engine's one targeting seam, {@link MobTargets#set} (the aggro-on-hit in {@code BukkitCombatant} and the
 *       selftest's {@code mob_target}): READ, {@code CraftMob#setTarget} passes a NULL reason, and
 *       {@code Mob.setTarget(target, null)} skips {@code callEntityTargetLivingEvent}, so vanilla's target event never
 *       sees these two;</li>
 *   <li>vanilla's target event ({@code RpgListeners.onHiddenTarget}): every goal and brain behaviour that calls the event
 *       (READ: {@code Mob.setTarget(target, reason)} with a reason, and {@code StartAttacking});</li>
 *   <li>our own mob abilities' victim choice ({@code MobBeamer}, {@code MobShooter}, {@code MobPouncer}), through
 *       {@link #hides}.</li>
 * </ul>
 *
 * He is HIDDEN, NOT INVULNERABLE: nothing here touches damage.
 */
public interface TargetGuard {

    /** Is this entity hidden from every mob now? No side effect: a victim choice asks it per candidate. */
    boolean hides(Entity entity);

    /** Refuse {@code mob} taking {@code target}: true when refused, and the refusal is logged (once per mob per blink). */
    boolean refuses(Entity mob, LivingEntity target);

    /** Bound once at enable (the guard needs the context); until then, and in a unit test, nothing is hidden. */
    final class Late implements TargetGuard {
        private volatile TargetGuard to;

        public void bind(TargetGuard guard) {
            this.to = guard;
        }

        @Override
        public boolean hides(Entity entity) {
            TargetGuard g = to;
            return g != null && g.hides(entity);
        }

        @Override
        public boolean refuses(Entity mob, LivingEntity target) {
            TargetGuard g = to;
            return g != null && g.refuses(mob, target);
        }
    }
}
