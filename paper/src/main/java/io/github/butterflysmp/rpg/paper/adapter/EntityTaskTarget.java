package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.LivingEntity;

/**
 * Binds a real Bukkit LivingEntity to the Bukkit-free {@link RepeatingTaskTarget} seam.
 *
 * Thin on purpose: the repeating-task lifecycle lives in RepeatingTask and ImmobilizeStatus,
 * which are tested against a fake target with no server. All that is left here -- "is the
 * entity still alive?" and "schedule my next tick on its thread" -- is boot-witnessed.
 */
public final class EntityTaskTarget implements RepeatingTaskTarget {

    private final LivingEntity entity;
    private final Scheduler scheduler;

    public EntityTaskTarget(LivingEntity entity, Scheduler scheduler) {
        this.entity = entity;
        this.scheduler = scheduler;
    }

    @Override public boolean isActive() {
        return entity.isValid() && !entity.isDead();
    }

    @Override public void scheduleTick(int delayTicks, Runnable run) {
        scheduler.onEntityLater(entity, run, delayTicks);
    }
}
