package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.LivingEntity;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Binds a real Bukkit LivingEntity to the Bukkit-free {@link RepeatingTaskTarget} seam.
 *
 * Thin on purpose: the repeating-task lifecycle lives in RepeatingTask and ImmobilizeStatus,
 * which are tested against a fake target with no server. All that is left here -- "is the
 * entity still alive?", "schedule my next tick on its thread" and "say who a failed loop
 * belonged to" -- is boot-witnessed.
 */
public final class EntityTaskTarget implements RepeatingTaskTarget {

    private final LivingEntity entity;
    private final Scheduler scheduler;
    /**
     * Where a crashed loop is reported. <b>A {@code Logger} rather than an {@code AdapterContext}</b>,
     * and the reason is construction ORDER rather than taste: {@code PlayerHealthSystem} and
     * {@code MobNameplateManager} are built at {@code RpgPlugin} lines 430-431, <b>fourteen lines
     * before the AdapterContext exists</b>, while {@code getLogger()} is available throughout
     * {@code onEnable}. Threading the context would have meant reordering the plugin bootstrap to
     * fix a logging line.
     */
    private final Logger log;

    public EntityTaskTarget(LivingEntity entity, Scheduler scheduler, Logger log) {
        this.entity = entity;
        this.scheduler = scheduler;
        this.log = log;
    }

    @Override public boolean isActive() {
        return entity.isValid() && !entity.isDead();
    }

    @Override public void scheduleTick(int delayTicks, Runnable run) {
        scheduler.onEntityLater(entity, run, delayTicks);
    }

    /**
     * Name the loop, name who it was for, and carry the stack trace.
     *
     * <p><b>SEVERE with the throwable attached</b>, not a {@code warning(message)}: the cause is the
     * only thing that says WHERE in the body it broke, and a loop dying silently for one player
     * among forty is precisely the failure that never gets reported by a human.
     *
     * <p><b>The entity is named by type AND uuid.</b> A player's name is the useful half and a mob
     * has none worth printing, so both are written and neither is assumed -- {@code getName()} on a
     * mob returns its type, which is still the right thing to see.
     *
     * <p><b>It reads the entity but does not TOUCH it</b>, which is what makes it safe here: this
     * runs on whatever thread the failing tick ran on, and on Folia that is the entity's own
     * thread, so even the reads are on-thread by construction rather than by luck.
     */
    @Override public void loopFailed(String loopName, RuntimeException cause) {
        log.log(Level.SEVERE,
                "repeating loop '" + loopName + "' threw and has been STOPPED for "
                        + entity.getName() + " (" + entity.getUniqueId() + "). Anything the loop"
                        + " owned is released by its onStop; a later start() can replace it.",
                cause);
    }
}
